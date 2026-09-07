import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, isIsoDate, driverIdForUser } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';

const TABLE = 'DeliveryStop';
const PK = 'StopId';

//Free-text in the DB, treated as enums here. Adjust to match your domain. */
export const STOP_STATUSES = ['Pending', 'Arrived', 'Completed', 'Failed', 'Cancelled'] as const;

//Statuses a driver (not admin) may set via the status endpoint. */
const DRIVER_SETTABLE_STATUSES = ['Arrived', 'Completed', 'Failed'] as const;

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
}

type ParseResult =
  | { ok: true; value: Partial<StopFields> }
  | { ok: false; error: string };

/**
 * Validate writable DeliveryStop fields. JobId comes from the route, never the
 * body. On create, SequenceNumber/RecipientName/Address/Latitude/Longitude are
 * required.
 */
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

  return { ok: true, value: out };
}

/** Fetch a stop plus its parent job's DriverId. */
async function getStopWithDriver(stopId: number) {
  const { data, error } = await supabase
    .from(TABLE)
    .select('*, DeliveryJob(DriverId)')
    .eq(PK, stopId)
    .maybeSingle();
  return { data, error };
}

//GET /api/jobs/:jobId/stops — any authenticated user. */
export async function listStopsForJob(req: Request, res: Response) {
  const jobId = parseId(req.params.jobId);
  if (jobId === null) return res.status(400).json({ error: 'Invalid job id' });

  const { data, error } = await supabase
    .from(TABLE)
    .select('*')
    .eq('JobId', jobId)
    .order('SequenceNumber', { ascending: true });
  if (error) return writeError(res, error, 'Database error');
  return res.json({ stops: data });
}

//GET /api/stops/:id — any authenticated user. */
export async function getStop(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'Stop not found' });
  return res.json({ stop: data });
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

//PATCH /api/stops/:id — admin only. Edit planning fields (incl. Status / timestamps). */
export async function updateStop(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

  const parsed = parseStopInput(req.body, true);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  if (Object.keys(parsed.value).length === 0)
    return res.status(400).json({ error: 'No valid fields to update' });

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

/**
 * PATCH /api/stops/:id/status — the driver assigned to the parent job, or an admin.
 * Sets ArrivedAt/CompletedAt automatically on the matching transitions.
 */
export async function updateStopStatus(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

  const { Status } = req.body ?? {};
  if (!STOP_STATUSES.includes(Status))
    return res.status(400).json({ error: `Status must be one of: ${STOP_STATUSES.join(', ')}` });

  const { data: stop, error: getErr } = await getStopWithDriver(id);
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

  const now = new Date().toISOString();
  const patch: Record<string, unknown> = { Status };
  if (Status === 'Arrived' && !(stop as any).ArrivedAt) patch.ArrivedAt = now;
  if (Status === 'Completed') {
    if (!(stop as any).ArrivedAt) patch.ArrivedAt = now;
    patch.CompletedAt = now;
  }

  const { data, error } = await supabase
    .from(TABLE)
    .update(patch)
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update stop status');
  return res.json({ stop: data });
}

//DELETE /api/stops/:id — admin only. No IsActive column → sets Status = 'Cancelled'. */
export async function cancelStop(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid stop id' });

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
