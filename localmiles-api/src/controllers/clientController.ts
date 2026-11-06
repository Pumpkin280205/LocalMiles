import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { writeError } from '../lib/db.js';

const TABLE = 'Client';

interface ClientFields {
  UserId: number;
  CompanyName: string;
  ContactPerson: string;
  Email: string;
  PhoneNumber: string | null;
  Address: string;
}

type ParseResult =
  | { ok: true; value: Partial<ClientFields> }
  | { ok: false; error: string };

function reqInt(body: any, key: string): number | null {
  const v = body?.[key];
  return typeof v === 'number' && Number.isInteger(v) && v > 0 ? v : null;
}
function reqStr(body: any, key: string): string | null {
  const v = body?.[key];
  return typeof v === 'string' && v.trim() ? v.trim() : null;
}

function parseClientInput(body: any): ParseResult {
  const out: Partial<ClientFields> = {};

  const UserId = reqInt(body, 'UserId');
  if (UserId === null) return { ok: false, error: 'UserId is required and must be a positive integer' };
  out.UserId = UserId;

  for (const key of ['CompanyName', 'ContactPerson', 'Email', 'Address'] as const) {
    const v = reqStr(body, key);
    if (v === null) return { ok: false, error: `${key} is required` };
    out[key] = v;
  }

  if (body?.PhoneNumber !== undefined) {
    if (body.PhoneNumber !== null && typeof body.PhoneNumber !== 'string')
      return { ok: false, error: 'PhoneNumber must be a string or null' };
    out.PhoneNumber = body.PhoneNumber;
  }

  return { ok: true, value: out };
}

//GET /api/clients — admin only. Used to populate client pickers (e.g. job creation). */
export async function listClients(req: Request, res: Response) {
  const { data, error } = await supabase
    .from(TABLE)
    .select('*')
    .order('ClientId', { ascending: true });
  if (error) return writeError(res, error, 'Database error');
  return res.json({ clients: data });
}

//POST /api/clients — admin only. Links a User to a new Client (company) record. */
export async function createClient(req: Request, res: Response) {
  const parsed = parseClientInput(req.body);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });

  const { data: user, error: userErr } = await supabase
    .from('User').select('UserId').eq('UserId', parsed.value.UserId).maybeSingle();
  if (userErr) return writeError(res, userErr, 'Database error');
  if (!user) return res.status(400).json({ error: 'Invalid reference: no user with that UserId' });

  const { data: existing, error: existingErr } = await supabase
    .from(TABLE).select('ClientId').eq('UserId', parsed.value.UserId).maybeSingle();
  if (existingErr) return writeError(res, existingErr, 'Database error');
  if (existing) return res.status(409).json({ error: `Conflict: user is already linked to ClientId ${existing.ClientId}` });

  const { data, error } = await supabase.from(TABLE).insert(parsed.value).select().single();
  if (error) return writeError(res, error, 'Could not create client');
  return res.status(201).json({ client: data });
}
