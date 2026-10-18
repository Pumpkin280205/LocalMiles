import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, posInt, isIsoDate, writeError, driverIdForUser, userCanAccessJob } from '../lib/db.js';
import { getFirestoreDb, isFirebaseConfigured } from '../lib/firebase.js';
import { ROLE } from '../middleware/auth.js';
import type { DocumentData, Query } from 'firebase-admin/firestore';

/**
 * Driver live location, stored in Firestore (NoSQL) rather than Postgres:
 * it's a high-frequency, append-only stream of small documents that's never
 * joined against, so it suits a document store better than a relational table.
 *
 *   driverLocations/{driverId}               latest known position (one doc per driver)
 *   driverLocations/{driverId}/pings/{auto}  full history, one doc per ping
 */
const COLLECTION = 'driverLocations';
const PINGS = 'pings';
// Jobs during which the client may see their driver's position.
const TRACKABLE_JOB_STATUSES = ['Assigned', 'EnRoute'];
// Pings older than this are rejected (also stops a stale offline replay moving the "latest" marker).
const MAX_PING_AGE_MS = 24 * 60 * 60 * 1000;
const MAX_HISTORY_LIMIT = 500;

export interface LocationPing {
  Latitude: number;
  Longitude: number;
  AccuracyMeters: number | null;
  SpeedMps: number | null;
  Heading: number | null;
  RecordedAt: Date;
  JobId: number | null;
}

function optionalNonNegative(v: unknown): number | null | undefined {
  if (v === undefined || v === null || v === '') return null;
  const n = Number(v);
  return Number.isFinite(n) && n >= 0 ? n : undefined;
}

/** Validate a location ping body. Pure, so it's unit-testable without Firestore. */
export function parseLocationBody(
  body: any,
  now = new Date()
): { ok: true; value: LocationPing } | { ok: false; error: string } {
  const b = body ?? {};

  const Latitude = Number(b.Latitude);
  const Longitude = Number(b.Longitude);
  if (b.Latitude === undefined || !Number.isFinite(Latitude) || Latitude < -90 || Latitude > 90)
    return { ok: false, error: 'Latitude is required and must be between -90 and 90' };
  if (b.Longitude === undefined || !Number.isFinite(Longitude) || Longitude < -180 || Longitude > 180)
    return { ok: false, error: 'Longitude is required and must be between -180 and 180' };

  const AccuracyMeters = optionalNonNegative(b.AccuracyMeters);
  if (AccuracyMeters === undefined) return { ok: false, error: 'AccuracyMeters must be a non-negative number' };
  const SpeedMps = optionalNonNegative(b.SpeedMps);
  if (SpeedMps === undefined) return { ok: false, error: 'SpeedMps must be a non-negative number' };
  const Heading = optionalNonNegative(b.Heading);
  if (Heading === undefined || (Heading !== null && Heading >= 360))
    return { ok: false, error: 'Heading must be a number from 0 to less than 360' };

  let RecordedAt = now;
  if (b.RecordedAt !== undefined && b.RecordedAt !== '') {
    if (!isIsoDate(b.RecordedAt)) return { ok: false, error: 'RecordedAt must be a valid date/time string' };
    RecordedAt = new Date(b.RecordedAt);
    // Small allowance for phone clock drift.
    if (RecordedAt.getTime() > now.getTime() + 5 * 60 * 1000)
      return { ok: false, error: 'RecordedAt cannot be in the future' };
    if (now.getTime() - RecordedAt.getTime() > MAX_PING_AGE_MS)
      return { ok: false, error: 'RecordedAt is older than 24 hours' };
  }

  let JobId: number | null = null;
  if (b.JobId !== undefined && b.JobId !== null && b.JobId !== '') {
    if (!posInt(Number(b.JobId))) return { ok: false, error: 'JobId must be a positive integer' };
    JobId = Number(b.JobId);
  }

  return { ok: true, value: { Latitude, Longitude, AccuracyMeters, SpeedMps, Heading, RecordedAt, JobId } };
}

/** Firestore doc -> API JSON (Timestamps become ISO strings). */
function toApi(doc: DocumentData) {
  return {
    ...doc,
    RecordedAt: doc.RecordedAt?.toDate?.().toISOString() ?? null,
    ReceivedAt: doc.ReceivedAt?.toDate?.().toISOString() ?? null,
  };
}

function firestoreUnavailable(res: Response) {
  return res.status(503).json({ error: 'Location tracking is not configured on this server' });
}

/**
 * POST /api/drivers/me/location — driver only.
 * Body: Latitude*, Longitude*, AccuracyMeters?, SpeedMps?, Heading?, RecordedAt? (ISO), JobId?
 * Appends a ping to the history and moves the driver's "latest" doc forward
 * (only if this ping is newer, so out-of-order offline replays don't rewind it).
 */
export async function postMyLocation(req: Request, res: Response) {
  if (req.user!.role !== ROLE.DRIVER) return res.status(403).json({ error: 'Only drivers can report a location' });

  const parsed = parseLocationBody(req.body);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });
  const ping = parsed.value;

  const driverId = await driverIdForUser(Number(req.user!.id));
  if (driverId === null) return res.status(404).json({ error: 'No driver record for this user' });

  if (ping.JobId !== null) {
    const { data: job, error } = await supabase
      .from('DeliveryJob').select('JobId, DriverId').eq('JobId', ping.JobId).maybeSingle();
    if (error) return writeError(res, error, 'Database error');
    if (!job || job.DriverId !== driverId) return res.status(403).json({ error: 'Not your job' });
  }

  if (!isFirebaseConfigured()) return firestoreUnavailable(res);

  try {
    const db = await getFirestoreDb();
    const { Timestamp, FieldValue } = await import('firebase-admin/firestore');
    const doc = {
      DriverId: driverId,
      JobId: ping.JobId,
      Latitude: ping.Latitude,
      Longitude: ping.Longitude,
      AccuracyMeters: ping.AccuracyMeters,
      SpeedMps: ping.SpeedMps,
      Heading: ping.Heading,
      RecordedAt: Timestamp.fromDate(ping.RecordedAt),
      ReceivedAt: FieldValue.serverTimestamp(),
    };

    const latestRef = db.collection(COLLECTION).doc(String(driverId));
    const pingRef = latestRef.collection(PINGS).doc();
    let movedLatest = false;

    await db.runTransaction(async (tx) => {
      const current = await tx.get(latestRef);
      const currentAt: number | undefined = current.data()?.RecordedAt?.toMillis?.();
      tx.create(pingRef, doc);
      if (currentAt === undefined || ping.RecordedAt.getTime() >= currentAt) {
        tx.set(latestRef, doc);
        movedLatest = true;
      }
    });

    console.log(`Location ping stored for driver ${driverId} (latest updated: ${movedLatest})`);
    return res.status(201).json({ pingId: pingRef.id, latestUpdated: movedLatest });
  } catch (e) {
    console.error('Failed to store location ping:', e);
    return res.status(502).json({ error: 'Could not store location' });
  }
}

/**
 * GET /api/jobs/:id/driver-location — admin, or the job's own driver/client.
 * Clients only see a position while the job is Assigned/EnRoute.
 */
export async function getJobDriverLocation(req: Request, res: Response) {
  const jobId = parseId(req.params.id);
  if (jobId === null) return res.status(400).json({ error: 'Invalid job id' });

  const { data: job, error } = await supabase
    .from('DeliveryJob').select('JobId, DriverId, ClientId, Status').eq('JobId', jobId).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!job || !(await userCanAccessJob(req, job))) return res.status(404).json({ error: 'Job not found' });
  if (job.DriverId === null) return res.status(409).json({ error: 'Job has no driver assigned' });
  if (req.user!.role !== ROLE.ADMIN && !TRACKABLE_JOB_STATUSES.includes(job.Status))
    return res.status(409).json({ error: `Driver location is only shared while a job is ${TRACKABLE_JOB_STATUSES.join(' or ')}` });

  if (!isFirebaseConfigured()) return firestoreUnavailable(res);

  try {
    const db = await getFirestoreDb();
    const snap = await db.collection(COLLECTION).doc(String(job.DriverId)).get();
    if (!snap.exists) return res.status(404).json({ error: 'No location reported yet' });
    const location = toApi(snap.data()!);
    const ageSeconds = location.RecordedAt
      ? Math.round((Date.now() - Date.parse(location.RecordedAt)) / 1000)
      : null;
    return res.json({ jobId, location, ageSeconds });
  } catch (e) {
    console.error('Failed to read driver location:', e);
    return res.status(502).json({ error: 'Could not read location' });
  }
}

/**
 * GET /api/drivers/:id/location-history — admin only.
 * Query: limit (<=500, def 100), since (ISO), jobId. Newest first.
 */
export async function getDriverLocationHistory(req: Request, res: Response) {
  const driverId = parseId(req.params.id);
  if (driverId === null) return res.status(400).json({ error: 'Invalid driver id' });

  const q = req.query;
  const limit = Math.min(Math.max(Number(q.limit) || 100, 1), MAX_HISTORY_LIMIT);
  if (q.since !== undefined && !isIsoDate(q.since)) return res.status(400).json({ error: 'since must be a valid date/time string' });
  const jobId = q.jobId !== undefined ? parseId(q.jobId as string) : undefined;
  if (jobId === null) return res.status(400).json({ error: 'Invalid jobId filter' });

  if (!isFirebaseConfigured()) return firestoreUnavailable(res);

  try {
    const db = await getFirestoreDb();
    const { Timestamp } = await import('firebase-admin/firestore');
    let query: Query = db.collection(COLLECTION).doc(String(driverId)).collection(PINGS);
    if (jobId !== undefined) query = query.where('JobId', '==', jobId);
    if (q.since !== undefined) query = query.where('RecordedAt', '>=', Timestamp.fromDate(new Date(q.since as string)));
    const snap = await query.orderBy('RecordedAt', 'desc').limit(limit).get();
    const pings = snap.docs.map((d) => ({ PingId: d.id, ...toApi(d.data()) }));
    return res.json({ driverId, pings, limit });
  } catch (e) {
    console.error('Failed to read location history:', e);
    return res.status(502).json({ error: 'Could not read location history' });
  }
}
