import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError } from '../lib/db.js';

const TABLE = 'Depot';
const PK = 'DepotId';

interface DepotFields {
  Name: string;
  Address: string;
  Latitude: number;
  Longitude: number;
  ContactNumber: string | null;
  IsActive: boolean;
}

type ParseResult =
  | { ok: true; value: Partial<DepotFields> }
  | { ok: false; error: string };

/**
 * Validate the writable Depot fields. With `partial: false` all NOT NULL
 * columns without a default (Name, Address, Latitude, Longitude) are required;
 * with `partial: true` every field is optional but still type-checked.
 */
function parseDepotInput(body: any, partial: boolean): ParseResult {
  const out: Partial<DepotFields> = {};
  const has = (k: string) => body != null && body[k] !== undefined;

  if (has('Name')) {
    if (typeof body.Name !== 'string' || !body.Name.trim())
      return { ok: false, error: 'Name must be a non-empty string' };
    out.Name = body.Name.trim();
  } else if (!partial) return { ok: false, error: 'Name is required' };

  if (has('Address')) {
    if (typeof body.Address !== 'string' || !body.Address.trim())
      return { ok: false, error: 'Address must be a non-empty string' };
    out.Address = body.Address.trim();
  } else if (!partial) return { ok: false, error: 'Address is required' };

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

  if (has('ContactNumber')) {
    if (body.ContactNumber !== null && typeof body.ContactNumber !== 'string')
      return { ok: false, error: 'ContactNumber must be a string or null' };
    out.ContactNumber = body.ContactNumber;
  }

  if (has('IsActive')) {
    if (typeof body.IsActive !== 'boolean')
      return { ok: false, error: 'IsActive must be a boolean' };
    out.IsActive = body.IsActive;
  }

  return { ok: true, value: out };
}

//GET /api/depots?active=true|false — any authenticated user. */
export async function listDepots(req: Request, res: Response) {
  let query = supabase.from(TABLE).select('*').order(PK, { ascending: true });

  if (req.query.active === 'true') query = query.eq('IsActive', true);
  else if (req.query.active === 'false') query = query.eq('IsActive', false);

  const { data, error } = await query;
  if (error) return writeError(res, error, 'Database error');
  return res.json({ depots: data });
}

//GET /api/depots/:id — any authenticated user. */
export async function getDepot(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid depot id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'Depot not found' });
  return res.json({ depot: data });
}

//POST /api/depots — admin only. */
export async function createDepot(req: Request, res: Response) {
  const parsed = parseDepotInput(req.body, false);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });

  const { data, error } = await supabase.from(TABLE).insert(parsed.value).select().single();
  if (error) return writeError(res, error, 'Could not create depot');
  return res.status(201).json({ depot: data });
}

//PATCH /api/depots/:id — admin only. */
export async function updateDepot(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid depot id' });

  const parsed = parseDepotInput(req.body, true);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  if (Object.keys(parsed.value).length === 0)
    return res.status(400).json({ error: 'No valid fields to update' });

  const { data, error } = await supabase
    .from(TABLE)
    .update(parsed.value)
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update depot');
  if (!data) return res.status(404).json({ error: 'Depot not found' });
  return res.json({ depot: data });
}

//DELETE /api/depots/:id — admin only. Soft delete (IsActive = false) to keep FKs intact. */
export async function deactivateDepot(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid depot id' });

  const { data, error } = await supabase
    .from(TABLE)
    .update({ IsActive: false })
    .eq(PK, id)
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not deactivate depot');
  if (!data) return res.status(404).json({ error: 'Depot not found' });
  return res.json({ depot: data });
}
