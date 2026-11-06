import type { Request, Response } from 'express';
import multer from 'multer';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, posInt, isIsoDate, driverIdForUser, userCanAccessJob } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';
import { haversineKm } from '../lib/geo.js';
import { notifyJobChange } from './jobController.js';

const TABLE = 'ProofOfDelivery';
const PK = 'PodId';
//Storage bucket. The brief specifies this exact (lower-case) name. */
const BUCKET = 'pod-uploads';
const SIGNED_URL_TTL = 3600; // seconds
// How far a driver's submitted GPS position may be from the stop's own coordinates.
// Generous enough for normal phone-GPS drift, tight enough to catch a wrong location.
const MAX_POD_DISTANCE_METERS = 200;

//multipart/form-data middleware: optional `photo` and `signature` image files. */
export const podUpload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024, files: 2 },
}).fields([
  { name: 'photo', maxCount: 1 },
  { name: 'signature', maxCount: 1 },
]);

function extFromMime(mime: string): string {
  return { 'image/jpeg': 'jpg', 'image/png': 'png', 'image/webp': 'webp', 'image/heic': 'heic' }[mime] ?? 'bin';
}

//Add short-lived signed URLs for any stored files on a POD row. */
async function withSignedUrls(pod: any) {
  const out = { ...pod };
  for (const [field, target] of [
    ['PhotoUrl', 'PhotoSignedUrl'],
    ['SignatureUrl', 'SignatureSignedUrl'],
  ] as const) {
    if (pod[field]) {
      const { data } = await supabase.storage.from(BUCKET).createSignedUrl(pod[field], SIGNED_URL_TTL);
      out[target] = data?.signedUrl ?? null;
    }
  }
  return out;
}

export interface PodInput {
  RecipientName: string;
  Latitude: number;
  Longitude: number;
  CapturedAt?: string;
  Notes: string | null;
  DriverId?: unknown; // only read when an admin submits
}

export interface PodFile {
  buffer: Buffer;
  mimetype: string;
}

//Validate the POD fields. Shared by POST /stops/:stopId/pod and the offline SubmitPod sync action. */
export function parsePodInput(body: any): { ok: true; value: PodInput } | { ok: false; error: string } {
  const b = body ?? {};

  const RecipientName = typeof b.RecipientName === 'string' ? b.RecipientName.trim() : '';
  if (!RecipientName) return { ok: false, error: 'RecipientName is required' };

  const Latitude = Number(b.Latitude);
  const Longitude = Number(b.Longitude);
  if (b.Latitude === undefined || b.Latitude === '' || !Number.isFinite(Latitude) || Latitude < -90 || Latitude > 90)
    return { ok: false, error: 'Latitude is required and must be between -90 and 90' };
  if (b.Longitude === undefined || b.Longitude === '' || !Number.isFinite(Longitude) || Longitude < -180 || Longitude > 180)
    return { ok: false, error: 'Longitude is required and must be between -180 and 180' };

  const value: PodInput = {
    RecipientName,
    Latitude,
    Longitude,
    Notes: typeof b.Notes === 'string' && b.Notes.trim() ? b.Notes.trim() : null,
    DriverId: b.DriverId,
  };

  if (b.CapturedAt !== undefined && b.CapturedAt !== '') {
    if (!isIsoDate(b.CapturedAt)) return { ok: false, error: 'CapturedAt must be a valid date/time string' };
    value.CapturedAt = new Date(b.CapturedAt).toISOString();
  }

  return { ok: true, value };
}

export type PodResult =
  | { ok: true; pod: any }
  | { ok: false; status: number; error: string; dbError?: unknown };

/**
 * Create a proof of delivery for a stop: ownership + GPS checks, upload any
 * files to blob storage, insert the row, mark the stop Completed. Used by the
 * live endpoint and by offline sync, so both enforce exactly the same rules.
 */
//When no stop is left to do, an EnRoute job becomes Completed (and the client is told). Best-effort. */
export async function completeJobIfDone(jobId: number, stopId: number): Promise<void> {
  const { data: stops } = await supabase.from('DeliveryStop').select('Status').eq('JobId', jobId);
  if (!stops?.length || !stops.some((s) => s.Status === 'Completed')) return;
  if (stops.some((s) => !['Completed', 'Failed', 'Cancelled'].includes(s.Status))) return;

  const { data: job } = await supabase
    .from('DeliveryJob').select('JobId, ClientId, DriverId, Status, ReferenceNumber').eq('JobId', jobId).maybeSingle();
  if (!job || job.Status !== 'EnRoute') return;

  const { error } = await supabase.from('DeliveryJob').update({ Status: 'Completed' }).eq('JobId', jobId).eq('Status', 'EnRoute');
  if (error) { console.error(`Could not complete job ${jobId} after POD for stop ${stopId}:`, error); return; }
  await notifyJobChange(job, { ...job, Status: 'Completed' });
}

export async function createPod(
  user: { id: string; role: string },
  stopId: number,
  input: PodInput,
  files: { photo?: PodFile | undefined; signature?: PodFile | undefined }
): Promise<PodResult> {
  // Look up the stop and the driver assigned to its parent job.
  const { data: stop, error: stopErr } = await supabase
    .from('DeliveryStop')
    .select('StopId, JobId, Latitude, Longitude, DeliveryJob(DriverId)')
    .eq('StopId', stopId)
    .maybeSingle();
  if (stopErr) return { ok: false, status: 500, error: 'Database error', dbError: stopErr };
  if (!stop) return { ok: false, status: 404, error: 'Stop not found' };

  // Resolve which DriverId this POD is attributed to.
  let driverId: number | null;
  if (user.role === ROLE.ADMIN) {
    driverId = posInt(Number(input.DriverId)) ? Number(input.DriverId) : null;
    if (driverId === null)
      return { ok: false, status: 400, error: 'DriverId is required when an admin submits a POD' };
  } else {
    driverId = await driverIdForUser(Number(user.id));
    const jobDriverId = (stop as any).DeliveryJob?.DriverId ?? null;
    if (driverId === null || jobDriverId !== driverId)
      return { ok: false, status: 403, error: 'Not your delivery' };

    // GPS-proximity check: only enforced for the driver's own submission, not
    // an admin's back-office override (an admin isn't physically at the stop).
    const distanceMeters =
      haversineKm(
        { lat: (stop as any).Latitude, lng: (stop as any).Longitude },
        { lat: input.Latitude, lng: input.Longitude }
      ) * 1000;
    if (distanceMeters > MAX_POD_DISTANCE_METERS) {
      return {
        ok: false,
        status: 400,
        error: `Submitted location is ${Math.round(distanceMeters)}m from the stop (max ${MAX_POD_DISTANCE_METERS}m)`,
      };
    }
  }

  for (const f of [files.photo, files.signature]) {
    if (f && !f.mimetype.startsWith('image/'))
      return { ok: false, status: 400, error: 'photo and signature must be image files' };
  }

  // Upload files to storage first; keep the returned paths for the DB row.
  const uploadOne = async (kind: string, f?: PodFile): Promise<string | null> => {
    if (!f) return null;
    const path = `stop-${stopId}/${kind}-${Date.now()}.${extFromMime(f.mimetype)}`;
    const { data, error } = await supabase.storage
      .from(BUCKET)
      .upload(path, f.buffer, { contentType: f.mimetype, upsert: false });
    if (error) throw error;
    return data.path;
  };

  let PhotoUrl: string | null = null;
  let SignatureUrl: string | null = null;
  try {
    PhotoUrl = await uploadOne('photo', files.photo);
    SignatureUrl = await uploadOne('signature', files.signature);
  } catch (e) {
    console.error('POD file upload failed:', e);
    const uploaded = [PhotoUrl].filter(Boolean) as string[];
    if (uploaded.length) await supabase.storage.from(BUCKET).remove(uploaded);
    return { ok: false, status: 502, error: 'File upload failed' };
  }

  const row: Record<string, unknown> = {
    StopId: stopId,
    DriverId: driverId,
    RecipientName: input.RecipientName,
    Latitude: input.Latitude,
    Longitude: input.Longitude,
    PhotoUrl,
    SignatureUrl,
    Notes: input.Notes,
  };
  if (input.CapturedAt) row.CapturedAt = input.CapturedAt;

  const { data, error } = await supabase.from(TABLE).insert(row).select().single();
  if (error) {
    const paths = [PhotoUrl, SignatureUrl].filter(Boolean) as string[];
    if (paths.length) await supabase.storage.from(BUCKET).remove(paths);
    return { ok: false, status: 500, error: 'Could not save proof of delivery', dbError: error };
  }

  // Best-effort: a POD means the stop is delivered.
  await supabase
    .from('DeliveryStop')
    .update({ Status: 'Completed', CompletedAt: new Date().toISOString() })
    .eq('StopId', stopId);

  await completeJobIfDone((stop as any).JobId, stopId);

  return { ok: true, pod: data };
}

//POST /api/stops/:stopId/pod — assigned driver or admin. multipart/form-data with optional photo/signature. */
export async function submitPod(req: Request, res: Response) {
  const stopId = parseId(req.params.stopId);
  if (stopId === null) return res.status(400).json({ error: 'Invalid stop id' });

  const parsed = parsePodInput(req.body);
  if (!parsed.ok) return res.status(400).json({ error: parsed.error });

  const files = req.files as Record<string, Express.Multer.File[]> | undefined;
  const result = await createPod(req.user!, stopId, parsed.value, {
    photo: files?.photo?.[0],
    signature: files?.signature?.[0],
  });
  if (!result.ok) {
    if (result.dbError) return writeError(res, result.dbError, result.error);
    return res.status(result.status).json({ error: result.error });
  }
  return res.status(201).json({ pod: await withSignedUrls(result.pod) });
}

//GET /api/stops/:stopId/pod — admin, or the stop's parent job's own driver/client. */
export async function getPodForStop(req: Request, res: Response) {
  const stopId = parseId(req.params.stopId);
  if (stopId === null) return res.status(400).json({ error: 'Invalid stop id' });

  const { data: stop, error: stopErr } = await supabase
    .from('DeliveryStop')
    .select('DeliveryJob(DriverId, ClientId)')
    .eq('StopId', stopId)
    .maybeSingle();
  if (stopErr) return writeError(res, stopErr, 'Database error');
  const job = (stop as any)?.DeliveryJob;
  if (!stop || !job || !(await userCanAccessJob(req, job)))
    return res.status(404).json({ error: 'No proof of delivery for this stop' });

  const { data, error } = await supabase.from(TABLE).select('*').eq('StopId', stopId).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'No proof of delivery for this stop' });
  return res.json({ pod: await withSignedUrls(data) });
}

//GET /api/pod/:id — admin, or the POD's stop's parent job's own driver/client. */
export async function getPod(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid pod id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'Proof of delivery not found' });

  const { data: stop, error: stopErr } = await supabase
    .from('DeliveryStop')
    .select('DeliveryJob(DriverId, ClientId)')
    .eq('StopId', data.StopId)
    .maybeSingle();
  if (stopErr) return writeError(res, stopErr, 'Database error');
  const job = (stop as any)?.DeliveryJob;
  if (!stop || !job || !(await userCanAccessJob(req, job)))
    return res.status(404).json({ error: 'Proof of delivery not found' });

  return res.json({ pod: await withSignedUrls(data) });
}

//GET /api/jobs/:jobId/pods — admin, or the job's own driver/client. All PODs for a job's stops. */
export async function listPodsForJob(req: Request, res: Response) {
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
    .select('*, DeliveryStop!inner(JobId, SequenceNumber)')
    .eq('DeliveryStop.JobId', jobId)
    .order('CapturedAt', { ascending: true });
  if (error) return writeError(res, error, 'Database error');
  return res.json({ pods: data });
}

//DELETE /api/pod/:id — admin only. Hard delete + remove stored files. */
export async function deletePod(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid pod id' });

  const { data: existing, error: getErr } = await supabase
    .from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (getErr) return writeError(res, getErr, 'Database error');
  if (!existing) return res.status(404).json({ error: 'Proof of delivery not found' });

  const { error } = await supabase.from(TABLE).delete().eq(PK, id);
  if (error) return writeError(res, error, 'Could not delete proof of delivery');

  const paths = [existing.PhotoUrl, existing.SignatureUrl].filter(Boolean) as string[];
  if (paths.length) await supabase.storage.from(BUCKET).remove(paths);

  return res.json({ deleted: true, podId: id });
}
