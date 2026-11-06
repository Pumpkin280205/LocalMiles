import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, isIsoDate, driverIdForUser, userCanAccessJob } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';
import { notifyDeliveryFailed } from '../lib/notifications.js';

const TABLE = 'DeliveryStop';
const PK = 'StopId';

//Free-text in the DB, treated as enums here. Adjust to match your domain. */
export const STOP_STATUSES = ['Pending', 'Arrived', 'Completed', 'Failed', 'Cancelled'] as const;

//Statuses a driver (not admin) may set via the status endpoint. */
const DRIVER_SETTABLE_STATUSES = ['Arrived', 'Completed', 'Failed'] as const;

/**
 * Valid next statuses for a stop, keyed by its current status. Completed and
 * Cancelled are terminal. Failed can go back to Arrived/Pending to allow a retry.
 */
const STOP_TRANSITIONS: Record<string, readonly string[]> = {
  Pending: ['Arrived', 'Cancelled'],
  Arrived: ['Completed', 'Failed', 'Cancelled'],
  Failed: ['Arrived', 'Pending', 'Cancelled'],
  Completed: [],
  Cancelled: [],
};

//A same-status call is always a no-op; anything else must be a listed transition. */
export function isValidStopTransition(from: string, to: string): boolean {
  return from === to || (STOP_TRANSITIONS[from]?.includes(to) ?? false);
}

interface StopFields {
  SequenceNumber: number;
  RecipientName: string;
  Address: string;
  Latitude: number;
  Longitude: number;
  DeliveryWindowStart: string | null;
  DeliveryWindowEnd: string | null;
  Status: string;
  Instructions: string | null;
  ArrivedAt: string | null;
  CompletedAt: string | null;
  FailureReason: string | null;
}

type ParseResult =
  | { ok: true; value: Partial<StopFields> }
  | { ok: false; error: string };

function parseStopInput(body: any, partial: boolean): ParseResult {
  const out: Partial<StopFields> = {};
  const has = (k: string) => body != null && body[k] !== undefined;

  if (has('JobId')) return { ok: false, error: 'JobId is taken from the route, not the body' };

  if (has('SequenceNumber')) {
    if (!Number.isInteger(body.SequenceNumber) || body.SequenceNumber < 0)
      return { ok: false, error: 'SequenceNumber must be a non-negative integer' };
    out.SequenceNumber = body.SequenceNumber;
  } else if (!partial) return { ok: false, error: 'SequenceNumber is required' };

  for (const key of ['RecipientName', 'Address'] as const) {
    if (has(key)) {
      if (typeof body[key] !== 'string' || !body[key].trim())
        return { ok: false, error: `${key} must be a non-empty string` };
      out[key] = body[key].trim();
    } else if (!partial) return { ok: false, error: `${key} is required` };
  }

  if (has('Latitude')) {
    if (typeof body.Latitude !== 'number' || !Number.isFinite(body.Latitude) || body.Latitude < -90 || body.Latitude > 90)
      return { ok: false, error: 'Latitude must be a number between -90 and 90' };
    out.Latitude = body.Latitude;
  } else if (!partial) return { ok: false, error: 'Latitude is required' };

  if (has('Longitude')) {
    if (typeof body.Longitude !== 'number' || !Number.isFinite(body.Longitude) || body.Longitude < -180 || body.Longitude > 180)
      return { ok: false, error: 'Longitude must be a number between -180 and 180' };
    out.Longitude = body.Longitude;
  } else if (!partial) return { ok: false, error: 'Longitude is required' };

  for (const key of ['DeliveryWindowStart', 'DeliveryWindowEnd', 'ArrivedAt', 'CompletedAt'] as const) {
    if (has(key)) {
      if (body[key] === null) out[key] = null;
      else if (isIsoDate(body[key])) out[key] = new Date(body[key]).toISOString();
      else return { ok: false, error: `${key} must be a valid date/time string or null` };
    }
  }

  if (has('Status')) {
    if (!STOP_STATUSES.includes(body.Status))
      return { ok: false, error: `Status must be one of: ${STOP_STATUSES.join(', ')}` };
    out.Status = body.Status;
  }

  if (has('Instructions')) {
    if (body.Instructions !== null && typeof body.Instructions !== 'string')
      return { ok: false, error: 'Instructions must be a string or null' };
    out.Instructions = body.Instructions;
  }

  if (has('FailureReason')) {
    if (body.FailureReason !== null && typeof body.FailureReason !== 'string')
      return { ok: false, error: 'FailureReason must be a string or null' };
    out.FailureReason = body.FailureReason;
  }

  return { ok: true, value: out };
}

/** Fetch a stop plus its parent job's DriverId/ClientId/ReferenceNumber. */
async function getStopWithJob(stopId: number) {
  const { data, error } = await supabase
    .from(TABLE)
    .select('*, DeliveryJob(DriverId, ClientId, ReferenceNumber)')
    .eq(PK, stopId)
    .maybeSingle();
  return { data, error };
}

//GET /api/jobs/:jobId/stops — admin, or the job's own driver/client. */
export async function listStopsForJob(req: Request, res: Response) {
  const jobId = parseId(req.params.jobId);
  if (jobId === null) return res.status(400).json({ error: 'Invalid job id' });

  const { data: job, error: jobErr } = await supabase
    .from('DeliveryJob')
    .select('DriverId, ClientId')
    .eq('JobId', jobId)
    .maybeSingle();
  if (jobErr) return writeError(res, jobErr, 'Database error');
  if (!job || !(await userCanAccessJob(req, job))) return res.status(404).json({ error: 'Job not found' });

  const { data, error } = await supabase
    .from(TABLE)
    .select('*')
    .eq('JobId', jobId)
    .order('SequenceNumber', { ascending: true });
  if (error) return writeError(res, error, 'Database error');
  return res.json({ stops: data });
}

//GET /api/stops/:id — admin, or the parent job's own driver/client. */
export async function getStop(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

  const { data, error } = await getStopWithJob(id);
  if (error) return writeError(res, error, 'Database error');
  const job = (data as any)?.DeliveryJob;
  if (!data || !job || !(await userCanAccessJob(req, job)))
    return res.status(404).json({ error: 'Stop not found' });

  const { DeliveryJob, ...stop } = data as any;
  return res.json({ stop });
}

//POST /api/jobs/:jobId/stops — admin only. */
export async function createStop(req: Request, res: Response) {
  const jobId = parseId(req.params.jobId);
  if (jobId === null) return res.status(400).json({ error: 'Invalid job id' });

  const parsed = parseStopInput(req.body, false);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });

  const row = {
    ...parsed.value,
    JobId: jobId,
    Status: parsed.value.Status ?? 'Pending',
  };

  const { data, error } = await supabase.from(TABLE).insert(row).select().single();
  if (error) return writeError(res, error, 'Could not create stop');
  return res.status(201).json({ stop: data });
}

export async function updateStop(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

  const parsed = parseStopInput(req.body, true);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  if (Object.keys(parsed.value).length === 0)
    return res.status(400).json({ error: 'No valid fields to update' });

  if (parsed.value.Status !== undefined) {
    const { data: current, error: curErr } = await supabase
      .from(TABLE).select('Status').eq(PK, id).maybeSingle();
    if (curErr) return writeError(res, curErr, 'Database error');
    if (!current) return res.status(404).json({ error: 'Stop not found' });
    if (!isValidStopTransition(current.Status, parsed.value.Status))
      return res.status(409).json({ error: `Cannot transition stop from ${current.Status} to ${parsed.value.Status}` });
  }

  const { data, error } = await supabase
    .from(TABLE)
    .update(parsed.value)
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update stop');
  if (!data) return res.status(404).json({ error: 'Stop not found' });
  return res.json({ stop: data });
}

export async function updateStopStatus(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

  const { Status, FailureReason } = req.body ?? {};
  if (!STOP_STATUSES.includes(Status))
    return res.status(400).json({ error: `Status must be one of: ${STOP_STATUSES.join(', ')}` });

  if (Status === 'Failed' && (typeof FailureReason !== 'string' || !FailureReason.trim()))
    return res.status(400).json({ error: 'FailureReason is required when Status is Failed' });

  const { data: stop, error: getErr } = await getStopWithJob(id);
  if (getErr) return writeError(res, getErr, 'Database error');
  if (!stop) return res.status(404).json({ error: 'Stop not found' });

  if (req.user!.role !== ROLE.ADMIN) {
    const driverId = await driverIdForUser(Number(req.user!.id));
    const jobDriverId = (stop as any).DeliveryJob?.DriverId ?? null;
    if (driverId === null || jobDriverId !== driverId)
      return res.status(403).json({ error: 'Not your delivery' });
    if (!DRIVER_SETTABLE_STATUSES.includes(Status))
      return res.status(403).json({ error: `Drivers may only set: ${DRIVER_SETTABLE_STATUSES.join(', ')}` });
  }

  if (!isValidStopTransition((stop as any).Status, Status))
    return res.status(409).json({ error: `Cannot transition stop from ${(stop as any).Status} to ${Status}` });

  const now = new Date().toISOString();
  const patch: Record<string, unknown> = { Status };
  if (Status === 'Arrived' && !(stop as any).ArrivedAt) patch.ArrivedAt = now;
  if (Status === 'Completed') {
    if (!(stop as any).ArrivedAt) patch.ArrivedAt = now;
    patch.CompletedAt = now;
  }
  if (Status === 'Failed') patch.FailureReason = (FailureReason as string).trim();

  const { data, error } = await supabase
    .from(TABLE)
    .update(patch)
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update stop status');

  if (data && Status === 'Failed' && (stop as any).Status !== 'Failed') {
    const job = (stop as any).DeliveryJob;
    await notifyDeliveryFailed(
      { JobId: (stop as any).JobId, ClientId: job.ClientId, DriverId: job.DriverId, ReferenceNumber: job.ReferenceNumber },
      id,
      (FailureReason as string).trim()
    );
  }

  return res.json({ stop: data });
}

//DELETE /api/stops/:id — admin only. No IsActive column → sets Status = 'Cancelled'. */
export async function cancelStop(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

  const { data: current, error: curErr } = await supabase
    .from(TABLE).select('Status').eq(PK, id).maybeSingle();
  if (curErr) return writeError(res, curErr, 'Database error');
  if (!current) return res.status(404).json({ error: 'Stop not found' });
  if (!isValidStopTransition(current.Status, 'Cancelled'))
    return res.status(409).json({ error: `Cannot cancel a stop with status ${current.Status}` });

  const { data, error } = await supabase
    .from(TABLE)
    .update({ Status: 'Cancelled' })
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not cancel stop');
  if (!data) return res.status(404).json({ error: 'Stop not found' });
  return res.json({ stop: data });
}
