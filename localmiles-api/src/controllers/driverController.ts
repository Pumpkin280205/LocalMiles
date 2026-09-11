import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError } from '../lib/db.js';

const TABLE = 'Driver';
const PK = 'DriverId';

interface DriverFields {
  UserId: number;
  DepotId: number;
  EmployeeNumber: string;
  PhoneNumber: string;
  LicenceNumber: string;
  IsAvailable: boolean;
  IsActive: boolean;
  Rating: number | null;
}

type ParseResult =
  | { ok: true; value: Partial<DriverFields> }
  | { ok: false; error: string };

function reqInt(body: any, key: string): number | null {
  const v = body?.[key];
  return typeof v === 'number' && Number.isInteger(v) && v > 0 ? v : null;
}
function reqStr(body: any, key: string): string | null {
  const v = body?.[key];
  return typeof v === 'string' && v.trim() ? v.trim() : null;
}

function parseDriverInput(body: any, partial: boolean): ParseResult {
  const out: Partial<DriverFields> = {};

  if (!partial) {
    const UserId = reqInt(body, 'UserId');
    if (UserId === null) return { ok: false, error: 'UserId is required and must be a positive integer' };
    out.UserId = UserId;
  } else if (body?.UserId !== undefined) {
    return { ok: false, error: 'UserId cannot be changed' };
  }

  const has = (k: string) => body != null && body[k] !== undefined;

  if (has('DepotId')) {
    const DepotId = reqInt(body, 'DepotId');
    if (DepotId === null) return { ok: false, error: 'DepotId must be a positive integer' };
    out.DepotId = DepotId;
  } else if (!partial) return { ok: false, error: 'DepotId is required' };

  for (const key of ['EmployeeNumber', 'PhoneNumber', 'LicenceNumber'] as const) {
    if (has(key)) {
      const v = reqStr(body, key);
      if (v === null) return { ok: false, error: `${key} must be a non-empty string` };
      out[key] = v;
    } else if (!partial) return { ok: false, error: `${key} is required` };
  }

  for (const key of ['IsAvailable', 'IsActive'] as const) {
    if (has(key)) {
      if (typeof body[key] !== 'boolean') return { ok: false, error: `${key} must be a boolean` };
      out[key] = body[key];
    }
  }

  if (has('Rating')) {
    if (body.Rating !== null &&
        (typeof body.Rating !== 'number' || !Number.isFinite(body.Rating) || body.Rating < 0 || body.Rating > 5))
      return { ok: false, error: 'Rating must be a number between 0 and 5, or null' };
    out.Rating = body.Rating;
  }

  return { ok: true, value: out };
}

//GET /api/drivers?depotId=&available=true|false&active=true|false — any authenticated user. */
export async function listDrivers(req: Request, res: Response) {
  let query = supabase.from(TABLE).select('*').order(PK, { ascending: true });

  const depotId = parseId(req.query.depotId as string | undefined);
  if (req.query.depotId !== undefined && depotId === null)
    return res.status(400).json({ error: 'Invalid depotId' });
  if (depotId !== null) query = query.eq('DepotId', depotId);

  if (req.query.available === 'true') query = query.eq('IsAvailable', true);
  else if (req.query.available === 'false') query = query.eq('IsAvailable', false);

  if (req.query.active === 'true') query = query.eq('IsActive', true);
  else if (req.query.active === 'false') query = query.eq('IsActive', false);

  const { data, error } = await query;
  if (error) return writeError(res, error, 'Database error');
  return res.json({ drivers: data });
}

//GET /api/drivers/me — the Driver row linked to the caller's User. */
export async function getMyDriver(req: Request, res: Response) {
  const { data, error } = await supabase
    .from(TABLE)
    .select('*')
    .eq('UserId', Number(req.user!.id))
    .maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'No driver record for this user' });
  return res.json({ driver: data });
}

// GET /api/drivers/:id — any authenticated user. */
export async function getDriver(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid driver id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'Driver not found' });
  return res.json({ driver: data });
}

// POST /api/drivers — admin only. */
export async function createDriver(req: Request, res: Response) {
  const parsed = parseDriverInput(req.body, false);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });

  const { data, error } = await supabase.from(TABLE).insert(parsed.value).select().single();
  if (error) return writeError(res, error, 'Could not create driver');
  return res.status(201).json({ driver: data });
}

// PATCH /api/drivers/:id — admin only. */
export async function updateDriver(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid driver id' });

  const parsed = parseDriverInput(req.body, true);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  if (Object.keys(parsed.value).length === 0)
    return res.status(400).json({ error: 'No valid fields to update' });

  const { data, error } = await supabase
    .from(TABLE)
    .update(parsed.value)
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update driver');
  if (!data) return res.status(404).json({ error: 'Driver not found' });
  return res.json({ driver: data });
}

// DELETE /api/drivers/:id — admin only. Soft delete (IsActive = false). */
export async function deactivateDriver(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid driver id' });

  const { data, error } = await supabase
    .from(TABLE)
    .update({ IsActive: false })
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not deactivate driver');
  if (!data) return res.status(404).json({ error: 'Driver not found' });
  return res.json({ driver: data });
}
