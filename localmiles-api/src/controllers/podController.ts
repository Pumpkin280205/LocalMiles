import type { Request, Response } from 'express';
import multer from 'multer';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, posInt, isIsoDate, driverIdForUser, userCanAccessJob } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';
import { haversineKm } from '../lib/geo.js';

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

export async function submitPod(req: Request, res: Response) {
  const stopId = parseId(req.params.stopId);
  if (stopId === null) return res.status(400).json({ error: 'Invalid stop id' });

  const body = req.body ?? {};

  const RecipientName = typeof body.RecipientName === 'string' ? body.RecipientName.trim() : '';
  if (!RecipientName) return res.status(400).json({ error: 'RecipientName is required' });

  const Latitude = Number(body.Latitude);
  const Longitude = Number(body.Longitude);
  if (!Number.isFinite(Latitude) || Latitude < -90 || Latitude > 90)
    return res.status(400).json({ error: 'Latitude is required and must be between -90 and 90' });
  if (!Number.isFinite(Longitude) || Longitude < -180 || Longitude > 180)
    return res.status(400).json({ error: 'Longitude is required and must be between -180 and 180' });

  let CapturedAt: string | undefined;
  if (body.CapturedAt !== undefined && body.CapturedAt !== '') {
    if (!isIsoDate(body.CapturedAt))
      return res.status(400).json({ error: 'CapturedAt must be a valid date/time string' });
    CapturedAt = new Date(body.CapturedAt).toISOString();
  }

  const Notes = typeof body.Notes === 'string' && body.Notes.trim() ? body.Notes.trim() : null;

  // Look up the stop and the driver assigned to its parent job.
  const { data: stop, error: stopErr } = await supabase
    .from('DeliveryStop')
    .select('StopId, Latitude, Longitude, DeliveryJob(DriverId)')
    .eq('StopId', stopId)
    .maybeSingle();
  if (stopErr) return writeError(res, stopErr, 'Database error');
  if (!stop) return res.status(404).json({ error: 'Stop not found' });

  // Resolve which DriverId this POD is attributed to.
  let driverId: number | null;
  if (req.user!.role === ROLE.ADMIN) {
    driverId = posInt(Number(body.DriverId)) ? Number(body.DriverId) : null;
    if (driverId === null)
      return res.status(400).json({ error: 'DriverId is required when an admin submits a POD' });
  } else {
    driverId = await driverIdForUser(Number(req.user!.id));
    const jobDriverId = (stop as any).DeliveryJob?.DriverId ?? null;
    if (driverId === null || jobDriverId !== driverId)
      return res.status(403).json({ error: 'Not your delivery' });

    // GPS-proximity check: only enforced for the driver's own submission, not
    // an admin's back-office override (an admin isn't physically at the stop).
    const distanceMeters =
      haversineKm(
        { lat: (stop as any).Latitude, lng: (stop as any).Longitude },
        { lat: Latitude, lng: Longitude }
      ) * 1000;
    if (distanceMeters > MAX_POD_DISTANCE_METERS) {
      return res.status(400).json({
        error: `Submitted location is ${Math.round(distanceMeters)}m from the stop (max ${MAX_POD_DISTANCE_METERS}m)`,
      });
    }
  }

  const files = req.files as Record<string, Express.Multer.File[]> | undefined;
  const photo = files?.photo?.[0];
  const signature = files?.signature?.[0];
  for (const f of [photo, signature]) {
    if (f && !f.mimetype.startsWith('image/'))
      return res.status(400).json({ error: 'photo and signature must be image files' });
  }

  // Upload files to storage first; keep the returned paths for the DB row.
  const uploadOne = async (kind: string, f?: Express.Multer.File): Promise<string | null> => {
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
    PhotoUrl = await uploadOne('photo', photo);
    SignatureUrl = await uploadOne('signature', signature);
  } catch (e) {
    console.error('POD file upload failed:', e);
    return res.status(502).json({ error: 'File upload failed' });
  }

  const row: Record<string, unknown> = {
    StopId: stopId,
    DriverId: driverId,
    RecipientName,
    Latitude,
    Longitude,
    PhotoUrl,
    SignatureUrl,
    Notes,
  };
  if (CapturedAt) row.CapturedAt = CapturedAt;

  const { data, error } = await supabase.from(TABLE).insert(row).select().single();
  if (error) {
    const paths = [PhotoUrl, SignatureUrl].filter(Boolean) as string[];
    if (paths.length) await supabase.storage.from(BUCKET).remove(paths);
    return writeError(res, error, 'Could not save proof of delivery');
  }

  // Best-effort: a POD means the stop is delivered. 
  await supabase
    .from('DeliveryStop')
    .update({ Status: 'Completed', CompletedAt: new Date().toISOString() })
    .eq('StopId', stopId);

  return res.status(201).json({ pod: await withSignedUrls(data) });
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
