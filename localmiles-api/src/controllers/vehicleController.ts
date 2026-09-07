import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError } from '../lib/db.js';

const TABLE = 'Vehicle';
const PK = 'VehicleId';

interface VehicleFields {
  DepotId: number;
  RegistrationNumber: string;
  Make: string;
  Model: string;
  VehicleType: string;
  CapacityKg: number;
  IsAvailable: boolean;
  IsActive: boolean;
}

type ParseResult =
  | { ok: true; value: Partial<VehicleFields> }
  | { ok: false; error: string };

function parseVehicleInput(body: any, partial: boolean): ParseResult {
  const out: Partial<VehicleFields> = {};
  const has = (k: string) => body != null && body[k] !== undefined;

  if (has('DepotId')) {
    const v = body.DepotId;
    if (typeof v !== 'number' || !Number.isInteger(v) || v <= 0)
      return { ok: false, error: 'DepotId must be a positive integer' };
    out.DepotId = v;
  } else if (!partial) return { ok: false, error: 'DepotId is required' };

  for (const key of ['RegistrationNumber', 'Make', 'Model', 'VehicleType'] as const) {
    if (has(key)) {
      if (typeof body[key] !== 'string' || !body[key].trim())
        return { ok: false, error: `${key} must be a non-empty string` };
      out[key] = body[key].trim();
    } else if (!partial) return { ok: false, error: `${key} is required` };
  }

  if (has('CapacityKg')) {
    const v = body.CapacityKg;
    if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0)
      return { ok: false, error: 'CapacityKg must be a positive number' };
    out.CapacityKg = v;
  } else if (!partial) return { ok: false, error: 'CapacityKg is required' };

  for (const key of ['IsAvailable', 'IsActive'] as const) {
    if (has(key)) {
      if (typeof body[key] !== 'boolean') return { ok: false, error: `${key} must be a boolean` };
      out[key] = body[key];
    }
  }

  return { ok: true, value: out };
}

//GET /api/vehicles?depotId=&available=true|false&active=true|false — any authenticated user. */
export async function listVehicles(req: Request, res: Response) {
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
  return res.json({ vehicles: data });
}

//GET /api/vehicles/:id — any authenticated user. */
export async function getVehicle(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid vehicle id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'Vehicle not found' });
  return res.json({ vehicle: data });
}

/** POST /api/vehicles — admin only. */
export async function createVehicle(req: Request, res: Response) {
  const parsed = parseVehicleInput(req.body, false);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });

  const { data, error } = await supabase.from(TABLE).insert(parsed.value).select().single();
  if (error) return writeError(res, error, 'Could not create vehicle');
  return res.status(201).json({ vehicle: data });
}

//PATCH /api/vehicles/:id — admin only. */
export async function updateVehicle(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid vehicle id' });

  const parsed = parseVehicleInput(req.body, true);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  if (Object.keys(parsed.value).length === 0)
    return res.status(400).json({ error: 'No valid fields to update' });

  const { data, error } = await supabase
    .from(TABLE)
    .update(parsed.value)
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update vehicle');
  if (!data) return res.status(404).json({ error: 'Vehicle not found' });
  return res.json({ vehicle: data });
}

//DELETE /api/vehicles/:id — admin only. Soft delete (IsActive = false). */
export async function deactivateVehicle(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid vehicle id' });

  const { data, error } = await supabase
    .from(TABLE)
    .update({ IsActive: false })
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not deactivate vehicle');
  if (!data) return res.status(404).json({ error: 'Vehicle not found' });
  return res.json({ vehicle: data });
}
