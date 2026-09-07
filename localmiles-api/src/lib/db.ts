import type { Response } from 'express';
import { supabase } from '../supabaseClient.js';

/** Parse a positive-integer path param (bigint identity PK). Returns null if not valid. */
export function parseId(raw: string | string[] | undefined): number | null {
  if (typeof raw !== 'string') return null;
  const n = Number(raw);
  return Number.isInteger(n) && n > 0 ? n : null;
}

/** True if v is a positive integer. */
export function posInt(v: unknown): v is number {
  return typeof v === 'number' && Number.isInteger(v) && v > 0;
}

/** True if v is a string that Date can parse. */
export function isIsoDate(v: unknown): v is string {
  return typeof v === 'string' && !Number.isNaN(Date.parse(v));
}

/** Resolve the Driver.DriverId linked to a given User, or null if the user isn't a driver. */
export async function driverIdForUser(userId: number): Promise<number | null> {
  const { data } = await supabase
    .from('Driver')
    .select('DriverId')
    .eq('UserId', userId)
    .maybeSingle();
  return data ? data.DriverId : null;
}

/** Postgres SQLSTATEs surfaced by PostgREST that we want to turn into 4xx, not 500. */
const PG = {
  UNIQUE_VIOLATION: '23505',
  FK_VIOLATION: '23503',
  CHECK_VIOLATION: '23514',
  NOT_NULL_VIOLATION: '23502',
} as const;

/**
 * Translate a Supabase/PostgREST write error into an HTTP response.
 * Constraint violations become 409/400 with the DB detail; anything else is a 500.
 */
export function writeError(res: Response, error: any, fallbackMessage: string) {
  const code: string | undefined = error?.code;
  const detail: string = error?.details || error?.message || 'unknown error';

  switch (code) {
    case PG.UNIQUE_VIOLATION:
      return res.status(409).json({ error: `Conflict: ${detail}` });
    case PG.FK_VIOLATION:
      return res.status(400).json({ error: `Invalid reference: ${detail}` });
    case PG.CHECK_VIOLATION:
    case PG.NOT_NULL_VIOLATION:
      return res.status(400).json({ error: `Invalid data: ${detail}` });
    default:
      console.error(`${fallbackMessage}:`, error);
      return res.status(500).json({ error: fallbackMessage });
  }
}
