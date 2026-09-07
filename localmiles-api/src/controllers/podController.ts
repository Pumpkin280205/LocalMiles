import type { Request, Response } from 'express';
import multer from 'multer';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, posInt, isIsoDate, driverIdForUser } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';

const TABLE = 'ProofOfDelivery';
const PK = 'PodId';
//Storage bucket. The brief specifies this exact (lower-case) name. */
const BUCKET = 'pod-uploads';
const SIGNED_URL_TTL = 3600; // seconds

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

/**
 * POST /api/stops/:stopId/pod  (multipart/form-data)
 * Fields: RecipientName*, Latitude*, Longitude*, Notes?, CapturedAt? (ISO),
 *         DriverId? (required only when an admin submits on a driver's behalf).
 * Files:  photo?, signature?  (images, <=10MB each)
 * The assigned driver, or an admin, may submit. One POD per stop (unique StopId).
 */
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
    .select('StopId, DeliveryJob(DriverId)')
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
    // Roll back the just-uploaded files so a retry isn't blocked by orphans.
    const paths = [PhotoUrl, SignatureUrl].filter(Boolean) as string[];
    if (paths.length) await supabase.storage.from(BUCKET).remove(paths);
    return writeError(res, error, 'Could not save proof of delivery');
  }

  // Best-effort: a POD means the stop is delivered. Remove this block if you'd
  // rather the client call PATCH /api/stops/:id/status itself.
  await supabase
    .from('DeliveryStop')
    .update({ Status: 'Completed', CompletedAt: new Date().toISOString() })
    .eq('StopId', stopId);

  return res.status(201).json({ pod: await withSignedUrls(data) });
}

//GET /api/stops/:stopId/pod — any authenticated user. */
export async function getPodForStop(req: Request, res: Response) {
  const stopId = parseId(req.params.stopId);
  if (stopId === null) return res.status(400).json({ error: 'Invalid stop id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq('StopId', stopId).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'No proof of delivery for this stop' });
  return res.json({ pod: await withSignedUrls(data) });
}

//GET /api/pod/:id — any authenticated user. */
export async function getPod(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid pod id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data) return res.status(404).json({ error: 'Proof of delivery not found' });
  return res.json({ pod: await withSignedUrls(data) });
}

//GET /api/jobs/:jobId/pods — any authenticated user. All PODs for a job's stops. */
export async function listPodsForJob(req: Request, res: Response) {
  const jobId = parseId(req.params.jobId);
  if (jobId === null) return res.status(400).json({ error: 'Invalid job id' });

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
