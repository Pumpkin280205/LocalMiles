import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, posInt, isIsoDate, driverIdForUser } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';

const TABLE = 'DeliveryJob';
const PK = 'JobId';

/**
 * Free-text in the DB, but treated as enums here. Adjust these to match your
 * domain vocabulary (and any CHECK constraint on the column).
 */
export const JOB_STATUSES = ['Pending', 'Assigned', 'EnRoute', 'Completed', 'Cancelled'] as const;
export const JOB_PRIORITIES = ['Low', 'Normal', 'High', 'Urgent'] as const;

// Statuses a driver (not admin) is allowed to move their own job into. */
const DRIVER_SETTABLE_STATUSES = ['EnRoute', 'Completed'] as const;

/**
 * Valid next statuses for a job, keyed by its current status. Completed and
 * Cancelled are terminal.
 */
const JOB_TRANSITIONS: Record<string, readonly string[]> = {
  Pending: ['Assigned', 'Cancelled'],
  Assigned: ['EnRoute', 'Pending', 'Cancelled'],
  EnRoute: ['Completed', 'Cancelled'],
  Completed: [],
  Cancelled: [],
};

//A same-status call is always a no-op; anything else must be a listed transition. */
export function isValidJobTransition(from: string, to: string): boolean {
  return from === to || (JOB_TRANSITIONS[from]?.includes(to) ?? false);
}

interface JobFields {
  ClientId: number;
  DriverId: number | null;
  VehicleId: number | null;
  DepotId: number;
  ScheduledDate: string;
  Status: string;
  Priority: string;
  Notes: string | null;
  EstimatedDistanceKm: number | null;
  EstimatedDurationMinutes: number | null;
}

type ParseResult =
  | { ok: true; value: Partial<JobFields> }
  | { ok: false; error: string };
  
function parseJobInput(body: any, partial: boolean): ParseResult {
  const out: Partial<JobFields> = {};
  const has = (k: string) => body != null && body[k] !== undefined;

  for (const k of ['CreatedByUserId', 'CreatedAt', 'JobId', 'ReferenceNumber'] as const) {
    if (has(k)) return { ok: false, error: `${k} cannot be set` };
  }

  if (has('ClientId')) {
    if (!posInt(body.ClientId)) return { ok: false, error: 'ClientId must be a positive integer' };
    out.ClientId = body.ClientId;
  } else if (!partial) return { ok: false, error: 'ClientId is required' };

  if (has('DepotId')) {
    if (!posInt(body.DepotId)) return { ok: false, error: 'DepotId must be a positive integer' };
    out.DepotId = body.DepotId;
  } else if (!partial) return { ok: false, error: 'DepotId is required' };

  if (has('ScheduledDate')) {
    if (!isIsoDate(body.ScheduledDate))
      return { ok: false, error: 'ScheduledDate must be a valid date/time string' };
    out.ScheduledDate = new Date(body.ScheduledDate).toISOString();
  } else if (!partial) return { ok: false, error: 'ScheduledDate is required' };

  for (const k of ['DriverId', 'VehicleId'] as const) {
    if (has(k)) {
      if (body[k] !== null && !posInt(body[k]))
        return { ok: false, error: `${k} must be a positive integer or null` };
      out[k] = body[k];
    }
  }

  if (has('Status')) {
    if (!JOB_STATUSES.includes(body.Status))
      return { ok: false, error: `Status must be one of: ${JOB_STATUSES.join(', ')}` };
    out.Status = body.Status;
  }

  if (has('Priority')) {
    if (!JOB_PRIORITIES.includes(body.Priority))
      return { ok: false, error: `Priority must be one of: ${JOB_PRIORITIES.join(', ')}` };
    out.Priority = body.Priority;
  }

  if (has('Notes')) {
    if (body.Notes !== null && typeof body.Notes !== 'string')
      return { ok: false, error: 'Notes must be a string or null' };
    out.Notes = body.Notes;
  }

  if (has('EstimatedDistanceKm')) {
    if (body.EstimatedDistanceKm !== null &&
        (typeof body.EstimatedDistanceKm !== 'number' || !Number.isFinite(body.EstimatedDistanceKm) || body.EstimatedDistanceKm < 0))
      return { ok: false, error: 'EstimatedDistanceKm must be a non-negative number or null' };
    out.EstimatedDistanceKm = body.EstimatedDistanceKm;
  }

  if (has('EstimatedDurationMinutes')) {
    if (body.EstimatedDurationMinutes !== null &&
        (!Number.isInteger(body.EstimatedDurationMinutes) || body.EstimatedDurationMinutes < 0))
      return { ok: false, error: 'EstimatedDurationMinutes must be a non-negative integer or null' };
    out.EstimatedDurationMinutes = body.EstimatedDurationMinutes;
  }

  return { ok: true, value: out };
}

export async function listJobs(req: Request, res: Response) {
  const q = req.query;
  let query = supabase.from(TABLE).select('*').order('ScheduledDate', { ascending: true });

  if (q.status !== undefined) {
    if (!JOB_STATUSES.includes(q.status as any))
      return res.status(400).json({ error: `status must be one of: ${JOB_STATUSES.join(', ')}` });
    query = query.eq('Status', q.status as string);
  }
  if (q.priority !== undefined) {
    if (!JOB_PRIORITIES.includes(q.priority as any))
      return res.status(400).json({ error: `priority must be one of: ${JOB_PRIORITIES.join(', ')}` });
    query = query.eq('Priority', q.priority as string);
  }

  for (const [param, column] of [
    ['clientId', 'ClientId'],
    ['driverId', 'DriverId'],
    ['vehicleId', 'VehicleId'],
    ['depotId', 'DepotId'],
  ] as const) {
    if (q[param] !== undefined) {
      const id = parseId(q[param] as string);
      if (id === null) return res.status(400).json({ error: `Invalid ${param}` });
      query = query.eq(column, id);
    }
  }

  if (q.scheduledFrom !== undefined) {
    if (Number.isNaN(Date.parse(q.scheduledFrom as string)))
      return res.status(400).json({ error: 'Invalid scheduledFrom' });
    query = query.gte('ScheduledDate', new Date(q.scheduledFrom as string).toISOString());
  }
  if (q.scheduledTo !== undefined) {
    if (Number.isNaN(Date.parse(q.scheduledTo as string)))
      return res.status(400).json({ error: 'Invalid scheduledTo' });
    query = query.lte('ScheduledDate', new Date(q.scheduledTo as string).toISOString());
  }

  if (q.mine === 'true') {
    const driverId = await driverIdForUser(Number(req.user!.id));
    if (driverId === null) return res.json({ jobs: [] });
    query = query.eq('DriverId', driverId);
  }

  const limit = Math.min(Math.max(Number(q.limit) || 50, 1), 200);
  const offset = Math.max(Number(q.offset) || 0, 0);
  query = query.range(offset, offset + limit - 1);

  const { data, error } = await query;
  if (error) return writeError(res, error, 'Database error');
  return res.json({ jobs: data, limit, offset });
}

//GET /api/jobs/:id — any authenticated user. */
export async function getJob(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid job id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'Job not found' });
  return res.json({ job: data });
}

export async function createJob(req: Request, res: Response) {
  const parsed = parseJobInput(req.body, false);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });

  const row = {
    ...parsed.value,
    Status: parsed.value.Status ?? 'Pending',
    Priority: parsed.value.Priority ?? 'Normal',
    CreatedByUserId: Number(req.user!.id),
    CreatedAt: new Date().toISOString(),
  };

  const { data: created, error } = await supabase.from(TABLE).insert(row).select().single();
  if (error) return writeError(res, error, 'Could not create job');

  const referenceNumber = `LM-${String(created.JobId).padStart(6, '0')}`;
  const { data, error: refErr } = await supabase
    .from(TABLE)
    .update({ ReferenceNumber: referenceNumber })
    .eq(PK, created.JobId)
    .select()
    .single();
  if (refErr) return writeError(res, refErr, 'Job created but could not assign a reference number');

  return res.status(201).json({ job: data });
}

//PATCH /api/jobs/:id — admin only. Full update incl. driver/vehicle assignment. */
export async function updateJob(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid job id' });

  const parsed = parseJobInput(req.body, true);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  if (Object.keys(parsed.value).length === 0)
    return res.status(400).json({ error: 'No valid fields to update' });

  const { data, error } = await supabase
    .from(TABLE)
    .update(parsed.value)
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update job');
  if (!data) return res.status(404).json({ error: 'Job not found' });
  return res.json({ job: data });
}

export async function updateJobStatus(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid job id' });

  const { Status } = req.body ?? {};
  if (!JOB_STATUSES.includes(Status))
    return res.status(400).json({ error: `Status must be one of: ${JOB_STATUSES.join(', ')}` });

  const { data: job, error: getErr } = await supabase
    .from(TABLE).select('JobId, DriverId, Status').eq(PK, id).maybeSingle();
  if (getErr) return writeError(res, getErr, 'Database error');
  if (!job) return res.status(404).json({ error: 'Job not found' });

  if (req.user!.role !== ROLE.ADMIN) {
    const driverId = await driverIdForUser(Number(req.user!.id));
    if (driverId === null || job.DriverId !== driverId)
      return res.status(403).json({ error: 'Not your job' });
    if (!DRIVER_SETTABLE_STATUSES.includes(Status))
      return res.status(403).json({ error: `Drivers may only set: ${DRIVER_SETTABLE_STATUSES.join(', ')}` });
  }

  if (!isValidJobTransition(job.Status, Status))
    return res.status(409).json({ error: `Cannot transition job from ${job.Status} to ${Status}` });

  const { data, error } = await supabase
    .from(TABLE).update({ Status }).eq(PK, id).select().maybeSingle();
  if (error) return writeError(res, error, 'Could not update job status');
  return res.json({ job: data });
}

//DELETE /api/jobs/:id — admin only. No IsActive column, so this sets Status = 'Cancelled'. */
export async function cancelJob(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid job id' });

  const { data, error } = await supabase
    .from(TABLE)
    .update({ Status: 'Cancelled' })
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not cancel job');
  if (!data) return res.status(404).json({ error: 'Job not found' });
  return res.json({ job: data });
}
