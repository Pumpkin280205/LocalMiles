import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { writeError, driverIdForUser } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';
import { JOB_STATUSES, isValidJobTransition } from './jobController.js';
import { STOP_STATUSES, isValidStopTransition } from './stopController.js';
import { notifySyncIssue } from '../lib/notifications.js';

const TABLE = 'OfflineSyncAction';
const PK = 'SyncActionId';
const MAX_BATCH = 50;

//Kept in sync with the private constants in jobController / stopController. */
const DRIVER_JOB_STATUSES = ['EnRoute', 'Completed'];
const DRIVER_STOP_STATUSES = ['Arrived', 'Completed', 'Failed'];

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const PG_UNIQUE_VIOLATION = '23505';

const callerId = (req: Request) => Number(req.user!.id);
const isAdmin = (req: Request) => req.user!.role === ROLE.ADMIN;

interface SyncAction {
  SyncActionId: string;
  ActionType: string;
  EntityType: string;
  EntityId: string;
  Payload: Record<string, unknown>;
}

type ActionOutcome =
  | { SyncActionId: string; outcome: 'invalid'; error: string }
  | { SyncActionId: string; outcome: 'duplicate' | 'queued' | 'applied' | 'failed'; status: string; error?: string | undefined };

//Validate one raw action object from the request. */
function parseAction(raw: any): { ok: true; value: SyncAction } | { ok: false; error: string } {
  if (raw == null || typeof raw !== 'object') return { ok: false, error: 'action must be an object' };
  if (typeof raw.SyncActionId !== 'string' || !UUID_RE.test(raw.SyncActionId))
    return { ok: false, error: 'SyncActionId must be a UUID' };
  if (typeof raw.ActionType !== 'string' || !raw.ActionType.trim())
    return { ok: false, error: 'ActionType is required' };
  if (typeof raw.EntityType !== 'string' || !raw.EntityType.trim())
    return { ok: false, error: 'EntityType is required' };
  if (raw.EntityId === undefined || raw.EntityId === null ||
      (typeof raw.EntityId !== 'string' && typeof raw.EntityId !== 'number'))
    return { ok: false, error: 'EntityId is required (string or number)' };
  if (raw.Payload == null || typeof raw.Payload !== 'object' || Array.isArray(raw.Payload))
    return { ok: false, error: 'Payload must be an object' };
  return {
    ok: true,
    value: {
      SyncActionId: raw.SyncActionId.toLowerCase(),
      ActionType: raw.ActionType.trim(),
      EntityType: raw.EntityType.trim(),
      EntityId: String(raw.EntityId),
      Payload: raw.Payload,
    },
  };
}

/**
 * Apply a known action. Returns the resulting SyncStatus:
 *  - 'Completed' — applied
 *  - 'Failed'    — a handler ran but rejected (error is set)
 *  - 'Pending'   — no handler for this ActionType; left for a worker
 */
async function dispatch(
  action: SyncAction,
  req: Request,
): Promise<{ status: 'Completed' | 'Failed' | 'Pending'; error: string | null }> {
  const entityId = Number(action.EntityId);
  const wantInt = Number.isInteger(entityId) && entityId > 0;

  switch (action.ActionType) {
    case 'UpdateJobStatus': {
      if (!wantInt) return { status: 'Failed', error: 'EntityId must be a job id' };
      const Status = (action.Payload as any).Status;
      if (!JOB_STATUSES.includes(Status))
        return { status: 'Failed', error: `Payload.Status must be one of: ${JOB_STATUSES.join(', ')}` };

      const { data: job, error } = await supabase
        .from('DeliveryJob').select('JobId, DriverId, Status').eq('JobId', entityId).maybeSingle();
      if (error) return { status: 'Failed', error: error.message };
      if (!job) return { status: 'Failed', error: 'Job not found' };

      if (!isAdmin(req)) {
        const driverId = await driverIdForUser(callerId(req));
        if (driverId === null || job.DriverId !== driverId)
          return { status: 'Failed', error: 'Not your job' };
        if (!DRIVER_JOB_STATUSES.includes(Status))
          return { status: 'Failed', error: `Drivers may only set: ${DRIVER_JOB_STATUSES.join(', ')}` };
      }

      if (!isValidJobTransition(job.Status, Status))
        return { status: 'Failed', error: `Cannot transition job from ${job.Status} to ${Status}` };

      const upd = await supabase.from('DeliveryJob').update({ Status }).eq('JobId', entityId);
      if (upd.error) return { status: 'Failed', error: upd.error.message };
      return { status: 'Completed', error: null };
    }

    case 'UpdateStopStatus': {
      if (!wantInt) return { status: 'Failed', error: 'EntityId must be a stop id' };
      const Status = (action.Payload as any).Status;
      const FailureReason = (action.Payload as any).FailureReason;
      if (!STOP_STATUSES.includes(Status))
        return { status: 'Failed', error: `Payload.Status must be one of: ${STOP_STATUSES.join(', ')}` };
      if (Status === 'Failed' && (typeof FailureReason !== 'string' || !FailureReason.trim()))
        return { status: 'Failed', error: 'Payload.FailureReason is required when Status is Failed' };

      const { data: stop, error } = await supabase
        .from('DeliveryStop').select('StopId, Status, ArrivedAt, DeliveryJob(DriverId)').eq('StopId', entityId).maybeSingle();
      if (error) return { status: 'Failed', error: error.message };
      if (!stop) return { status: 'Failed', error: 'Stop not found' };

      if (!isAdmin(req)) {
        const driverId = await driverIdForUser(callerId(req));
        const jobDriverId = (stop as any).DeliveryJob?.DriverId ?? null;
        if (driverId === null || jobDriverId !== driverId)
          return { status: 'Failed', error: 'Not your delivery' };
        if (!DRIVER_STOP_STATUSES.includes(Status))
          return { status: 'Failed', error: `Drivers may only set: ${DRIVER_STOP_STATUSES.join(', ')}` };
      }

      if (!isValidStopTransition((stop as any).Status, Status))
        return { status: 'Failed', error: `Cannot transition stop from ${(stop as any).Status} to ${Status}` };

      const now = new Date().toISOString();
      const patch: Record<string, unknown> = { Status };
      if (Status === 'Arrived' && !(stop as any).ArrivedAt) patch.ArrivedAt = now;
      if (Status === 'Completed') {
        if (!(stop as any).ArrivedAt) patch.ArrivedAt = now;
        patch.CompletedAt = now;
      }
      if (Status === 'Failed') patch.FailureReason = FailureReason.trim();
      const upd = await supabase.from('DeliveryStop').update(patch).eq('StopId', entityId);
      if (upd.error) return { status: 'Failed', error: upd.error.message };
      return { status: 'Completed', error: null };
    }

    default:
      // Unknown action type: persisted as Pending for an out-of-band worker.
      return { status: 'Pending', error: null };
  }
}

//Process one already-validated action against the ledger. */
async function processAction(action: SyncAction, req: Request): Promise<ActionOutcome> {
  // 1. Check the processed-actions ledger.
  const { data: existing, error: checkErr } = await supabase
    .from(TABLE).select('SyncActionId, SyncStatus').eq(PK, action.SyncActionId).maybeSingle();
  if (checkErr) return { SyncActionId: action.SyncActionId, outcome: 'failed', status: 'Unknown', error: checkErr.message };
  if (existing)
    return { SyncActionId: action.SyncActionId, outcome: 'duplicate', status: existing.SyncStatus };

  // 2. Insert the action as Pending. PK conflict => another request beat us here.
  const ins = await supabase.from(TABLE).insert({
    SyncActionId: action.SyncActionId,
    UserId: callerId(req),
    ActionType: action.ActionType,
    EntityType: action.EntityType,
    EntityId: action.EntityId,
    Payload: action.Payload,
    SyncStatus: 'Pending',
  }).select('SyncStatus').single();

  if (ins.error) {
    if (ins.error.code === PG_UNIQUE_VIOLATION) {
      const { data: row } = await supabase.from(TABLE).select('SyncStatus').eq(PK, action.SyncActionId).maybeSingle();
      return { SyncActionId: action.SyncActionId, outcome: 'duplicate', status: row?.SyncStatus ?? 'Pending' };
    }
    if (ins.error.code === '23503')
      return { SyncActionId: action.SyncActionId, outcome: 'failed', status: 'Failed', error: `Invalid reference: ${ins.error.details ?? ins.error.message}` };
    return { SyncActionId: action.SyncActionId, outcome: 'failed', status: 'Failed', error: ins.error.message };
  }

  // 3. Apply it.
  const { status, error } = await dispatch(action, req);
  await supabase.from(TABLE).update({
    SyncStatus: status,
    LastAttemptAt: new Date().toISOString(),
    ErrorMessage: error,
  }).eq(PK, action.SyncActionId);

  if (status === 'Pending')
    return { SyncActionId: action.SyncActionId, outcome: 'queued', status };
  if (status === 'Failed') {
    await notifySyncIssue(callerId(req), action.SyncActionId, error);
    return { SyncActionId: action.SyncActionId, outcome: 'failed', status, error: error ?? undefined };
  }
  return { SyncActionId: action.SyncActionId, outcome: 'applied', status };
}

export async function submitSync(req: Request, res: Response) {
  const actions = req.body?.actions;
  if (!Array.isArray(actions) || actions.length === 0)
    return res.status(400).json({ error: 'Body must be { actions: [...] } with at least one action' });
  if (actions.length > MAX_BATCH)
    return res.status(400).json({ error: `Too many actions (max ${MAX_BATCH})` });

  const seen = new Set<string>();
  const results: ActionOutcome[] = [];

  for (const raw of actions) {
    const parsed = parseAction(raw);
    if (!parsed.ok) {
      results.push({ SyncActionId: typeof raw?.SyncActionId === 'string' ? raw.SyncActionId : '', outcome: 'invalid', error: parsed.error });
      continue;
    }
    if (seen.has(parsed.value.SyncActionId)) {
      results.push({ SyncActionId: parsed.value.SyncActionId, outcome: 'duplicate', status: 'Pending', error: 'repeated in this batch' });
      continue;
    }
    seen.add(parsed.value.SyncActionId);
    results.push(await processAction(parsed.value, req));
  }

  return res.json({ results });
}

//GET /api/sync?status=&entityType=&limit=&offset= — the caller's own queued actions. */
export async function listSync(req: Request, res: Response) {
  const q = req.query;
  let query = supabase
    .from(TABLE).select('*').eq('UserId', callerId(req)).order('CreatedAt', { ascending: false });

  if (q.status !== undefined) query = query.eq('SyncStatus', String(q.status));
  if (q.entityType !== undefined) query = query.eq('EntityType', String(q.entityType));

  const limit = Math.min(Math.max(Number(q.limit) || 50, 1), 200);
  const offset = Math.max(Number(q.offset) || 0, 0);
  query = query.range(offset, offset + limit - 1);

  const { data, error } = await query;
  if (error) return writeError(res, error, 'Database error');
  return res.json({ actions: data, limit, offset });
}

//GET /api/sync/:id — one action's status by its UUID (owner or admin). */
export async function getSync(req: Request, res: Response) {
  const id = String(req.params.id ?? '');
  if (!UUID_RE.test(id)) return res.status(400).json({ error: 'Invalid sync action id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id.toLowerCase()).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data || (data.UserId !== callerId(req) && !isAdmin(req)))
    return res.status(404).json({ error: 'Sync action not found' });
  return res.json({ action: data });
}

//POST /api/sync/:id/retry — re-attempt a Failed action (owner or admin). */
export async function retrySync(req: Request, res: Response) {
  const id = String(req.params.id ?? '');
  if (!UUID_RE.test(id)) return res.status(400).json({ error: 'Invalid sync action id' });

  const { data: row, error } = await supabase.from(TABLE).select('*').eq(PK, id.toLowerCase()).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!row || (row.UserId !== callerId(req) && !isAdmin(req)))
    return res.status(404).json({ error: 'Sync action not found' });
  if (row.SyncStatus !== 'Failed')
    return res.status(409).json({ error: `Only Failed actions can be retried (this one is ${row.SyncStatus})` });

  const action: SyncAction = {
    SyncActionId: row.SyncActionId,
    ActionType: row.ActionType,
    EntityType: row.EntityType,
    EntityId: String(row.EntityId),
    Payload: row.Payload ?? {},
  };
  const { status, error: applyErr } = await dispatch(action, req);

  const { data: updated, error: updErr } = await supabase.from(TABLE).update({
    SyncStatus: status,
    RetryCount: (Number(row.RetryCount) || 0) + 1,
    LastAttemptAt: new Date().toISOString(),
    ErrorMessage: applyErr,
  }).eq(PK, row.SyncActionId).select().single();
  if (updErr) return writeError(res, updErr, 'Could not update sync action');

  if (status === 'Failed') await notifySyncIssue(row.UserId, row.SyncActionId, applyErr);

  return res.json({ action: updated });
}
