import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, isIsoDate } from '../lib/db.js';
import { haversineKm, type Point } from '../lib/geo.js';

/**
 * Depot performance dashboard.
 */

// South Africa has no daylight saving, so a fixed offset is exact.
const SAST_OFFSET_MS = 2 * 60 * 60 * 1000;
const DAY_MS = 24 * 60 * 60 * 1000;
const DEFAULT_RANGE_DAYS = 30;
const MAX_RANGE_DAYS = 366;
const PAGE_SIZE = 1000; // Supabase's default max rows per request

/**
 * Typical consumption (litres per 100 km) by VehicleType, used to estimate fuel
 * because the app doesn't log fill-ups. 
 */
export const FUEL_L_PER_100KM: Record<string, number> = {
  motorbike: 3.5,
  car: 7,
  bakkie: 9.5,
  van: 11,
  truck: 25,
};
const DEFAULT_FUEL_L_PER_100KM = 12;

export function fuelRateFor(vehicleType: string | null | undefined): number {
  return FUEL_L_PER_100KM[(vehicleType ?? '').trim().toLowerCase()] ?? DEFAULT_FUEL_L_PER_100KM;
}

export interface ReportStop {
  StopId: number;
  SequenceNumber: number;
  Status: string;
  Latitude: number;
  Longitude: number;
  DeliveryWindowEnd: string | null;
  CompletedAt: string | null;
  FailureReason: string | null;
}

export interface ReportJob {
  JobId: number;
  DriverId: number | null;
  VehicleId: number | null;
  DepotId: number;
  ScheduledDate: string;
  Status: string;
  EstimatedDistanceKm: number | null;
  Depot: { Latitude: number; Longitude: number } | null;
  DeliveryStop: ReportStop[];
}

/**
 * The deadline a completed stop is measured against: its delivery window end
 * if it has one, otherwise the end of the job's scheduled day (SAST).
 */
export function stopDeadline(stop: Pick<ReportStop, 'DeliveryWindowEnd'>, scheduledDate: string): number {
  if (stop.DeliveryWindowEnd) return Date.parse(stop.DeliveryWindowEnd);
  const local = Date.parse(scheduledDate) + SAST_OFFSET_MS;
  const endOfLocalDay = Math.floor(local / DAY_MS) * DAY_MS + DAY_MS - 1;
  return endOfLocalDay - SAST_OFFSET_MS;
}

export function isOnTime(stop: ReportStop, scheduledDate: string): boolean {
  return stop.CompletedAt !== null && Date.parse(stop.CompletedAt) <= stopDeadline(stop, scheduledDate);
}

/**
 * Distance for a job: the planner's EstimatedDistanceKm when set, otherwise the
 * straight-line path depot -> stops in sequence order.
 */
export function jobDistanceKm(job: ReportJob): number {
  if (job.EstimatedDistanceKm !== null && job.EstimatedDistanceKm !== undefined) return Number(job.EstimatedDistanceKm);
  if (!job.Depot) return 0;
  const stops = [...job.DeliveryStop]
    .filter((s) => s.Status !== 'Cancelled')
    .sort((a, b) => a.SequenceNumber - b.SequenceNumber);
  let km = 0;
  let prev: Point = { lat: job.Depot.Latitude, lng: job.Depot.Longitude };
  for (const s of stops) {
    const next = { lat: s.Latitude, lng: s.Longitude };
    km += haversineKm(prev, next);
    prev = next;
  }
  return km;
}

const round = (n: number, dp = 1) => Math.round(n * 10 ** dp) / 10 ** dp;
const pct = (part: number, whole: number) => (whole === 0 ? null : round((part / whole) * 100));

interface Tally {
  jobsTotal: number;
  jobsCompleted: number;
  jobsCancelled: number;
  stopsCompleted: number;
  stopsFailed: number;
  stopsOnTime: number;
  distanceKm: number;
  fuelLitres: number;
}

const emptyTally = (): Tally => ({
  jobsTotal: 0, jobsCompleted: 0, jobsCancelled: 0,
  stopsCompleted: 0, stopsFailed: 0, stopsOnTime: 0,
  distanceKm: 0, fuelLitres: 0,
});

function summarise(t: Tally, fuelPrice: number | null) {
  const attempted = t.stopsCompleted + t.stopsFailed;
  return {
    jobsTotal: t.jobsTotal,
    jobsCompleted: t.jobsCompleted,
    jobsCancelled: t.jobsCancelled,
    stopsCompleted: t.stopsCompleted,
    stopsFailed: t.stopsFailed,
    onTimeRatePct: pct(t.stopsOnTime, t.stopsCompleted),
    successRatePct: pct(t.stopsCompleted, attempted),
    distanceKm: round(t.distanceKm),
    estimatedFuelLitres: round(t.fuelLitres),
    estimatedFuelCost: fuelPrice === null ? null : round(t.fuelLitres * fuelPrice, 2),
  };
}

/**
 * Turn raw jobs into the dashboard payload.
 * Distance/fuel only count jobs that were actually driven (EnRoute/Completed).
 */
export function computeDepotPerformance(
  jobs: ReportJob[],
  opts: {
    vehicleTypes: Map<number, string>;
    driverNames: Map<number, string>;
    driverRatings: Map<number, number | null>;
    fuelPrice: number | null;
  }
) {
  const overall = emptyTally();
  const perDriver = new Map<number, Tally>();
  const failureReasons = new Map<string, number>();
  const daily = new Map<string, { completed: number; failed: number; onTime: number }>();

  for (const job of jobs) {
    const driverTally = job.DriverId !== null
      ? perDriver.get(job.DriverId) ?? perDriver.set(job.DriverId, emptyTally()).get(job.DriverId)!
      : null;
    const tallies = driverTally ? [overall, driverTally] : [overall];

    const driven = job.Status === 'EnRoute' || job.Status === 'Completed';
    const km = driven ? jobDistanceKm(job) : 0;
    const litres = (km * fuelRateFor(job.VehicleId !== null ? opts.vehicleTypes.get(job.VehicleId) : null)) / 100;

    for (const t of tallies) {
      t.jobsTotal++;
      if (job.Status === 'Completed') t.jobsCompleted++;
      if (job.Status === 'Cancelled') t.jobsCancelled++;
      t.distanceKm += km;
      t.fuelLitres += litres;
    }

    for (const stop of job.DeliveryStop) {
      if (stop.Status === 'Completed') {
        const onTime = isOnTime(stop, job.ScheduledDate);
        for (const t of tallies) {
          t.stopsCompleted++;
          if (onTime) t.stopsOnTime++;
        }
        const day = new Date(Date.parse(stop.CompletedAt ?? job.ScheduledDate) + SAST_OFFSET_MS).toISOString().slice(0, 10);
        const d = daily.get(day) ?? daily.set(day, { completed: 0, failed: 0, onTime: 0 }).get(day)!;
        d.completed++;
        if (onTime) d.onTime++;
      } else if (stop.Status === 'Failed') {
        for (const t of tallies) t.stopsFailed++;
        const reason = stop.FailureReason?.trim() || 'Not specified';
        failureReasons.set(reason, (failureReasons.get(reason) ?? 0) + 1);
        const day = new Date(Date.parse(job.ScheduledDate) + SAST_OFFSET_MS).toISOString().slice(0, 10);
        const d = daily.get(day) ?? daily.set(day, { completed: 0, failed: 0, onTime: 0 }).get(day)!;
        d.failed++;
      }
    }
  }

  const drivers = [...perDriver.entries()]
    .map(([driverId, t]) => ({
      driverId,
      name: opts.driverNames.get(driverId) ?? `Driver #${driverId}`,
      rating: opts.driverRatings.get(driverId) ?? null,
      ...summarise(t, opts.fuelPrice),
    }))
    // Best first: on-time rate, then volume. Drivers with no completed stops last.
    .sort((a, b) => (b.onTimeRatePct ?? -1) - (a.onTimeRatePct ?? -1) || b.stopsCompleted - a.stopsCompleted);

  return {
    summary: summarise(overall, opts.fuelPrice),
    drivers,
    failureReasons: [...failureReasons.entries()]
      .map(([reason, count]) => ({ reason, count }))
      .sort((a, b) => b.count - a.count),
    daily: [...daily.entries()]
      .map(([date, d]) => ({ date, ...d, onTimeRatePct: pct(d.onTime, d.completed) }))
      .sort((a, b) => a.date.localeCompare(b.date)),
  };
}

export function parseRange(q: Record<string, unknown>, now = new Date()):
  { ok: true; from: Date; to: Date } | { ok: false; error: string } {
  if (q.from !== undefined && !isIsoDate(q.from)) return { ok: false, error: 'from must be a valid date' };
  if (q.to !== undefined && !isIsoDate(q.to)) return { ok: false, error: 'to must be a valid date' };
  const to = q.to !== undefined ? new Date(q.to as string) : now;
  const from = q.from !== undefined ? new Date(q.from as string) : new Date(to.getTime() - DEFAULT_RANGE_DAYS * DAY_MS);
  if (from > to) return { ok: false, error: 'from must be before to' };
  if (to.getTime() - from.getTime() > MAX_RANGE_DAYS * DAY_MS)
    return { ok: false, error: `Date range cannot exceed ${MAX_RANGE_DAYS} days` };
  return { ok: true, from, to };
}

/**
 * GET /api/reports/depot-performance — admin only.
 */
export async function getDepotPerformance(req: Request, res: Response) {
  const q = req.query;

  const range = parseRange(q);
  if (!range.ok) return res.status(400).json({ error: range.error });

  const depotId = q.depotId !== undefined ? parseId(q.depotId as string) : undefined;
  if (depotId === null) return res.status(400).json({ error: 'Invalid depotId' });

  let fuelPrice: number | null = null;
  if (q.fuelPrice !== undefined) {
    fuelPrice = Number(q.fuelPrice);
    if (!Number.isFinite(fuelPrice) || fuelPrice <= 0 || fuelPrice > 1000)
      return res.status(400).json({ error: 'fuelPrice must be a positive number (Rand per litre)' });
  }

  // Page through jobs so large ranges aren't silently cut off at 1000 rows.
  const jobs: ReportJob[] = [];
  for (let offset = 0; ; offset += PAGE_SIZE) {
    let query = supabase
      .from('DeliveryJob')
      .select(
        'JobId, DriverId, VehicleId, DepotId, ScheduledDate, Status, EstimatedDistanceKm, ' +
        'Depot(Latitude, Longitude), ' +
        'DeliveryStop(StopId, SequenceNumber, Status, Latitude, Longitude, DeliveryWindowEnd, CompletedAt, FailureReason)'
      )
      .gte('ScheduledDate', range.from.toISOString())
      .lte('ScheduledDate', range.to.toISOString())
      .order('JobId', { ascending: true })
      .range(offset, offset + PAGE_SIZE - 1);
    if (depotId !== undefined) query = query.eq('DepotId', depotId);
    const { data, error } = await query;
    if (error) return writeError(res, error, 'Database error');
    jobs.push(...((data ?? []) as unknown as ReportJob[]));
    if (!data || data.length < PAGE_SIZE) break;
  }

  // Look-ups for names, ratings and vehicle types (separate queries keep this
  // independent of which foreign-key embeds the schema exposes).
  const driverIds = [...new Set(jobs.map((j) => j.DriverId).filter((id): id is number => id !== null))];
  const vehicleIds = [...new Set(jobs.map((j) => j.VehicleId).filter((id): id is number => id !== null))];

  const driverNames = new Map<number, string>();
  const driverRatings = new Map<number, number | null>();
  if (driverIds.length) {
    const { data: drivers, error } = await supabase
      .from('Driver').select('DriverId, UserId, EmployeeNumber, Rating').in('DriverId', driverIds);
    if (error) return writeError(res, error, 'Database error');
    const userIds = (drivers ?? []).map((d) => d.UserId);
    const { data: users, error: userErr } = userIds.length
      ? await supabase.from('User').select('UserId, FirstName, LastName').in('UserId', userIds)
      : { data: [], error: null };
    if (userErr) return writeError(res, userErr, 'Database error');
    const userName = new Map((users ?? []).map((u) => [u.UserId, `${u.FirstName ?? ''} ${u.LastName ?? ''}`.trim()]));
    for (const d of drivers ?? []) {
      driverNames.set(d.DriverId, userName.get(d.UserId) || d.EmployeeNumber || `Driver #${d.DriverId}`);
      driverRatings.set(d.DriverId, d.Rating ?? null);
    }
  }

  const vehicleTypes = new Map<number, string>();
  if (vehicleIds.length) {
    const { data: vehicles, error } = await supabase
      .from('Vehicle').select('VehicleId, VehicleType').in('VehicleId', vehicleIds);
    if (error) return writeError(res, error, 'Database error');
    for (const v of vehicles ?? []) vehicleTypes.set(v.VehicleId, v.VehicleType);
  }

  const report = computeDepotPerformance(jobs, { vehicleTypes, driverNames, driverRatings, fuelPrice });
  console.log(`Depot performance: ${jobs.length} jobs, depot ${depotId ?? 'all'}, ${range.from.toISOString()} -> ${range.to.toISOString()}`);

  return res.json({
    depotId: depotId ?? null,
    from: range.from.toISOString(),
    to: range.to.toISOString(),
    fuelPricePerLitre: fuelPrice,
    ...report,
  });
}
