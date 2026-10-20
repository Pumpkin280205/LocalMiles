import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  computeDepotPerformance, stopDeadline, isOnTime, jobDistanceKm, fuelRateFor, parseRange,
  getDepotPerformance, type ReportJob, type ReportStop,
} from '../src/controllers/reportController.js';
import { fakeReq, fakeRes } from './helpers.js';

const DEPOT = { Latitude: -29.8587, Longitude: 31.0218 };

const stop = (over: Partial<ReportStop> = {}): ReportStop => ({
  StopId: 1, SequenceNumber: 1, Status: 'Pending', Latitude: -29.85, Longitude: 31.03,
  DeliveryWindowEnd: null, CompletedAt: null, FailureReason: null, ...over,
});

const job = (over: Partial<ReportJob> = {}): ReportJob => ({
  JobId: 1, DriverId: 7, VehicleId: 3, DepotId: 1, ScheduledDate: '2026-10-19T06:00:00Z',
  Status: 'Completed', EstimatedDistanceKm: 40, Depot: DEPOT, DeliveryStop: [], ...over,
});

const opts = {
  vehicleTypes: new Map([[3, 'Van']]),
  driverNames: new Map([[7, 'Sipho Dlamini'], [8, 'Lerato Nkosi']]),
  driverRatings: new Map<number, number | null>([[7, 4.6]]),
  fuelPrice: null as number | null,
};

test('deadline is the delivery window end when there is one', () => {
  assert.equal(stopDeadline({ DeliveryWindowEnd: '2026-10-19T12:00:00Z' }, '2026-10-19T06:00:00Z'), Date.parse('2026-10-19T12:00:00Z'));
});

test('without a window, the deadline is the end of the scheduled day in SAST', () => {
  // 23:59:59.999 SAST on 19 Oct = 21:59:59.999 UTC
  assert.equal(stopDeadline({ DeliveryWindowEnd: null }, '2026-10-19T06:00:00Z'), Date.parse('2026-10-19T21:59:59.999Z'));
});

test('on-time compares completion against the deadline', () => {
  const window = '2026-10-19T12:00:00Z';
  assert.equal(isOnTime(stop({ Status: 'Completed', CompletedAt: '2026-10-19T11:30:00Z', DeliveryWindowEnd: window }), '2026-10-19T06:00:00Z'), true);
  assert.equal(isOnTime(stop({ Status: 'Completed', CompletedAt: '2026-10-19T12:30:00Z', DeliveryWindowEnd: window }), '2026-10-19T06:00:00Z'), false);
  assert.equal(isOnTime(stop({ CompletedAt: null }), '2026-10-19T06:00:00Z'), false);
});

test('fuel rate matches vehicle type case-insensitively with a default', () => {
  assert.equal(fuelRateFor('VAN'), 11);
  assert.equal(fuelRateFor(' truck '), 25);
  assert.equal(fuelRateFor('hovercraft'), 12);
  assert.equal(fuelRateFor(null), 12);
});

test('job distance uses the estimate, or falls back to the depot-to-stops path', () => {
  assert.equal(jobDistanceKm(job({ EstimatedDistanceKm: 40 })), 40);
  const fallback = jobDistanceKm(job({
    EstimatedDistanceKm: null,
    DeliveryStop: [
      stop({ SequenceNumber: 2, Latitude: -26.2041, Longitude: 28.0473 }), // Johannesburg
      stop({ SequenceNumber: 1, Latitude: -29.8587, Longitude: 31.0218 }), // at the depot
    ],
  }));
  assert.ok(fallback > 480 && fallback < 520, `got ${fallback}`);
  assert.equal(jobDistanceKm(job({ EstimatedDistanceKm: null, Depot: null })), 0);
});

test('summary, per-driver ranking, failure reasons and daily trend', () => {
  const jobs = [
    job({
      JobId: 1, DriverId: 7, EstimatedDistanceKm: 100,
      DeliveryStop: [
        stop({ StopId: 1, Status: 'Completed', CompletedAt: '2026-10-19T09:00:00Z' }),
        stop({ StopId: 2, Status: 'Completed', CompletedAt: '2026-10-19T10:00:00Z' }),
        stop({ StopId: 3, Status: 'Failed', FailureReason: 'Client absent' }),
      ],
    }),
    job({
      JobId: 2, DriverId: 8, VehicleId: null, Status: 'EnRoute', EstimatedDistanceKm: 50,
      DeliveryStop: [
        // Completed after its window closed -> late
        stop({ StopId: 4, Status: 'Completed', DeliveryWindowEnd: '2026-10-19T08:00:00Z', CompletedAt: '2026-10-19T09:00:00Z' }),
        stop({ StopId: 5, Status: 'Failed', FailureReason: 'Client absent' }),
        stop({ StopId: 6, Status: 'Failed', FailureReason: '  ' }),
      ],
    }),
    // Cancelled before it was driven: counts as a job, adds no distance
    job({ JobId: 3, DriverId: 7, Status: 'Cancelled', EstimatedDistanceKm: 999 }),
  ];

  const r = computeDepotPerformance(jobs, { ...opts, fuelPrice: 22.5 });

  assert.equal(r.summary.jobsTotal, 3);
  assert.equal(r.summary.jobsCompleted, 1);
  assert.equal(r.summary.jobsCancelled, 1);
  assert.equal(r.summary.stopsCompleted, 3);
  assert.equal(r.summary.stopsFailed, 3);
  assert.equal(r.summary.onTimeRatePct, 66.7);   // 2 of 3 completed stops on time
  assert.equal(r.summary.successRatePct, 50);    // 3 of 6 attempts succeeded
  assert.equal(r.summary.distanceKm, 150);
  // 100 km van @ 11 L/100km + 50 km unknown vehicle @ 12 L/100km = 11 + 6 = 17 L
  assert.equal(r.summary.estimatedFuelLitres, 17);
  assert.equal(r.summary.estimatedFuelCost, 382.5);

  assert.deepEqual(r.drivers.map((d) => d.name), ['Sipho Dlamini', 'Lerato Nkosi']);
  assert.equal(r.drivers[0]!.onTimeRatePct, 100);
  assert.equal(r.drivers[0]!.rating, 4.6);
  assert.equal(r.drivers[1]!.onTimeRatePct, 0);
  assert.equal(r.drivers[1]!.rating, null);

  assert.deepEqual(r.failureReasons, [
    { reason: 'Client absent', count: 2 },
    { reason: 'Not specified', count: 1 },
  ]);

  assert.deepEqual(r.daily, [{ date: '2026-10-19', completed: 3, failed: 3, onTime: 2, onTimeRatePct: 66.7 }]);
});

test('an empty period gives zeros and null rates rather than NaN', () => {
  const r = computeDepotPerformance([], opts);
  assert.equal(r.summary.jobsTotal, 0);
  assert.equal(r.summary.onTimeRatePct, null);
  assert.equal(r.summary.successRatePct, null);
  assert.equal(r.summary.estimatedFuelCost, null);
  assert.deepEqual(r.drivers, []);
});

test('date range defaults to the last 30 days and is validated', () => {
  const now = new Date('2026-10-21T00:00:00Z');
  const def = parseRange({}, now);
  assert.ok(def.ok);
  assert.equal(def.from.toISOString(), '2026-09-21T00:00:00.000Z');
  assert.equal(parseRange({ from: 'nope' }, now).ok, false);
  assert.equal(parseRange({ from: '2026-10-20', to: '2026-10-01' }, now).ok, false);
  assert.equal(parseRange({ from: '2024-01-01', to: '2026-01-01' }, now).ok, false);
});

test('depot-performance endpoint rejects bad query params before querying', async () => {
  const admin = { id: '1', role: 'admin' };
  for (const query of [{ depotId: 'x' }, { fuelPrice: '-5' }, { from: 'bad' }]) {
    const res = fakeRes();
    await getDepotPerformance(fakeReq({ query, user: admin }), res);
    assert.equal(res.statusCode, 400, JSON.stringify(query));
  }
});
