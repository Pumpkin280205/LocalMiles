import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, driverIdForUser } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';

const EARTH_RADIUS_KM = 6371;
// Rough urban delivery-van average, used only to turn distance into an ETA estimate.
const AVERAGE_SPEED_KMH = 35;

interface Point {
  lat: number;
  lng: number;
}

function toRad(deg: number): number {
  return (deg * Math.PI) / 180;
}

//Great-circle ("as the crow flies") distance between two lat/lng points, in km.
function haversineKm(a: Point, b: Point): number {
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);

  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;

  return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
}

function nearestNeighbourOrder<T extends Point>(
  start: Point,
  points: T[]
): { order: T[]; totalKm: number } {
  const remaining = [...points];
  const order: T[] = [];
  let current = start;
  let totalKm = 0;

  while (remaining.length > 0) {
    let nearestIdx = 0;
    let nearestDist = haversineKm(current, remaining[0]!);

    for (let i = 1; i < remaining.length; i++) {
      const d = haversineKm(current, remaining[i]!);
      if (d < nearestDist) {
        nearestDist = d;
        nearestIdx = i;
      }
    }

    const next = remaining.splice(nearestIdx, 1)[0]!;
    order.push(next);
    totalKm += nearestDist;
    current = next;
  }

  return { order, totalKm };
}

/**
 * GET /api/jobs/:id/route — the job's assigned driver, or an admin.
 * Computes an ordered stop sequence fresh on every call (nothing is persisted)
 * using nearest-neighbour from the job's depot. Only Pending/Arrived stops are
 * routed — Completed/Failed/Cancelled stops have nothing left to visit.
 */
export async function getJobRoute(req: Request, res: Response) {
  const jobId = parseId(req.params.id);
  if (jobId === null) return res.status(400).json({ error: 'Invalid job id' });

  const { data: job, error: jobErr } = await supabase
    .from('DeliveryJob')
    .select('JobId, DriverId, DepotId, Depot(Latitude, Longitude)')
    .eq('JobId', jobId)
    .maybeSingle();
  if (jobErr) return writeError(res, jobErr, 'Database error');
  if (!job) return res.status(404).json({ error: 'Job not found' });

  if (req.user!.role !== ROLE.ADMIN) {
    const driverId = await driverIdForUser(Number(req.user!.id));
    if (driverId === null || (job as any).DriverId !== driverId)
      return res.status(403).json({ error: 'Not your job' });
  }

  const depot = (job as any).Depot;
  if (!depot) return res.status(409).json({ error: 'Job has no depot location to route from' });

  const { data: stops, error: stopsErr } = await supabase
    .from('DeliveryStop')
    .select('StopId, Latitude, Longitude')
    .eq('JobId', jobId)
    .in('Status', ['Pending', 'Arrived']);
  if (stopsErr) return writeError(res, stopsErr, 'Database error');

  if (!stops || stops.length === 0) {
    return res.json({ jobId, route: [], totalDistanceKm: 0, estimatedDurationMinutes: 0 });
  }

  const start: Point = { lat: depot.Latitude, lng: depot.Longitude };
  const points = stops.map((s) => ({ lat: s.Latitude, lng: s.Longitude, stop: s }));

  const { order, totalKm } = nearestNeighbourOrder(start, points);

  const route = order.map((p, i) => ({
    stopId: p.stop.StopId,
    sequence: i + 1,
    latitude: p.lat,
    longitude: p.lng,
  }));

  return res.json({
    jobId,
    route,
    totalDistanceKm: Math.round(totalKm * 10) / 10,
    estimatedDurationMinutes: Math.round((totalKm / AVERAGE_SPEED_KMH) * 60),
  });
}
