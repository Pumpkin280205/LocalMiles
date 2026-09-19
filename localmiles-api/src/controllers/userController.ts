import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { writeError, parseId } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';

const TABLE = 'User';
const PK = 'UserId';
const ASSIGNABLE_ROLES: string[] = Object.values(ROLE);

interface ProfileFields {
  FirstName: string;
  LastName: string;
}

type ParseResult =
  | { ok: true; value: Partial<ProfileFields> }
  | { ok: false; error: string };

function parseProfileInput(body: any): ParseResult {
  const out: Partial<ProfileFields> = {};
  const has = (k: string) => body != null && body[k] !== undefined;

  for (const k of ['UserId', 'Email', 'SsoProviderId', 'Role', 'IsActive', 'CreatedAt', 'LastLoginAt'] as const) {
    if (has(k)) return { ok: false, error: `${k} cannot be set` };
  }

  for (const key of ['FirstName', 'LastName'] as const) {
    if (has(key)) {
      if (typeof body[key] !== 'string' || !body[key].trim())
        return { ok: false, error: `${key} must be a non-empty string` };
      out[key] = body[key].trim();
    }
  }

  return { ok: true, value: out };
}

//PATCH /api/users/me — any authenticated user, edits their own profile only. */
export async function updateMe(req: Request, res: Response) {
  const parsed = parseProfileInput(req.body);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  if (Object.keys(parsed.value).length === 0)
    return res.status(400).json({ error: 'No valid fields to update' });

  const { data, error } = await supabase
    .from(TABLE)
    .update(parsed.value)
    .eq(PK, Number(req.user!.id))
    .select('UserId, Email, FirstName, LastName, Role, IsActive, CreatedAt, LastLoginAt')
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update profile');
  if (!data) return res.status(404).json({ error: 'User not found' });
  return res.json({ user: data });
}

//GET /api/users — admin only. Lets an admin see every account and its role. */
export async function listUsers(_req: Request, res: Response) {
  const { data, error } = await supabase
    .from(TABLE)
    .select('UserId, Email, FirstName, LastName, Role, IsActive, CreatedAt, LastLoginAt')
    .order(PK, { ascending: true });
  if (error) return writeError(res, error, 'Database error');
  return res.json({ users: data });
}

//PATCH /api/users/:id/role — admin only. Body: { Role: "admin" | "driver" | "client" }. */
export async function updateUserRole(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid user id' });

  const { Role } = req.body ?? {};
  if (typeof Role !== 'string' || !ASSIGNABLE_ROLES.includes(Role))
    return res.status(400).json({ error: `Role must be one of: ${ASSIGNABLE_ROLES.join(', ')}` });

  const { data, error } = await supabase
    .from(TABLE)
    .update({ Role })
    .eq(PK, id)
    .select('UserId, Email, FirstName, LastName, Role, IsActive, CreatedAt, LastLoginAt')
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update role');
  if (!data) return res.status(404).json({ error: 'User not found' });
  return res.json({ user: data });
}
