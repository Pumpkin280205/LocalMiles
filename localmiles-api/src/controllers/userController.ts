import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { writeError } from '../lib/db.js';

const TABLE = 'User';
const PK = 'UserId';

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
