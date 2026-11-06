import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseLocationBody, postMyLocation, getDriverLocationHistory } from '../src/controllers/locationController.js';
import { fakeReq, fakeRes } from './helpers.js';

const NOW = new Date('2026-10-19T10:00:00Z');

test('a minimal location ping is valid and defaults RecordedAt to now', () => {
  const r = parseLocationBody({ Latitude: -29.8587, Longitude: 31.0218 }, NOW);
  assert.ok(r.ok);
  assert.equal(r.value.Latitude, -29.8587);
  assert.equal(r.value.RecordedAt.getTime(), NOW.getTime());
  assert.equal(r.value.AccuracyMeters, null);
  assert.equal(r.value.JobId, null);
});

test('location ping accepts numeric strings and optional fields', () => {
  const r = parseLocationBody(
    { Latitude: '-26.2', Longitude: '28.04', AccuracyMeters: 12, SpeedMps: '8.5', Heading: 270, JobId: '7', RecordedAt: '2026-10-19T09:55:00Z' },
    NOW
  );
  assert.ok(r.ok);
  assert.equal(r.value.SpeedMps, 8.5);
  assert.equal(r.value.Heading, 270);
  assert.equal(r.value.JobId, 7);
});

test('location ping rejects missing or out-of-range coordinates', () => {
  assert.equal(parseLocationBody({ Longitude: 31 }, NOW).ok, false);
  assert.equal(parseLocationBody({ Latitude: 91, Longitude: 31 }, NOW).ok, false);
  assert.equal(parseLocationBody({ Latitude: -29, Longitude: -181 }, NOW).ok, false);
  assert.equal(parseLocationBody({ Latitude: 'abc', Longitude: 31 }, NOW).ok, false);
});

test('location ping rejects bad optional fields', () => {
  const base = { Latitude: -29, Longitude: 31 };
  assert.equal(parseLocationBody({ ...base, AccuracyMeters: -1 }, NOW).ok, false);
  assert.equal(parseLocationBody({ ...base, Heading: 360 }, NOW).ok, false);
  assert.equal(parseLocationBody({ ...base, JobId: 0 }, NOW).ok, false);
  assert.equal(parseLocationBody({ ...base, RecordedAt: 'yesterday-ish' }, NOW).ok, false);
});

test('location ping rejects timestamps in the future or older than 24 hours', () => {
  const base = { Latitude: -29, Longitude: 31 };
  assert.equal(parseLocationBody({ ...base, RecordedAt: '2026-10-19T10:30:00Z' }, NOW).ok, false);
  assert.equal(parseLocationBody({ ...base, RecordedAt: '2026-10-18T09:00:00Z' }, NOW).ok, false);
  // A few minutes of phone clock drift is tolerated.
  assert.equal(parseLocationBody({ ...base, RecordedAt: '2026-10-19T10:02:00Z' }, NOW).ok, true);
});

test('only drivers can post a location', async () => {
  const res = fakeRes();
  await postMyLocation(fakeReq({ body: { Latitude: -29, Longitude: 31 }, user: { id: '1', role: 'client' } }), res);
  assert.equal(res.statusCode, 403);
});

test('an invalid ping is rejected before any database call', async () => {
  const res = fakeRes();
  await postMyLocation(fakeReq({ body: { Latitude: 200, Longitude: 31 }, user: { id: '1', role: 'driver' } }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /Latitude/);
});

test('location history validates its params', async () => {
  const badId = fakeRes();
  await getDriverLocationHistory(fakeReq({ params: { id: 'x' }, user: { id: '1', role: 'admin' } }), badId);
  assert.equal(badId.statusCode, 400);

  const badSince = fakeRes();
  await getDriverLocationHistory(fakeReq({ params: { id: '3' }, query: { since: 'nope' }, user: { id: '1', role: 'admin' } }), badSince);
  assert.equal(badSince.statusCode, 400);
});

test('location history returns 503 when Firestore is not configured', async () => {
  const res = fakeRes();
  await getDriverLocationHistory(fakeReq({ params: { id: '3' }, user: { id: '1', role: 'admin' } }), res);
  assert.equal(res.statusCode, 503);
});
