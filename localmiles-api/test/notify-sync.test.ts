import { test } from 'node:test';
import assert from 'node:assert/strict';
import { jobNotificationEvents, type JobSnapshot } from '../src/controllers/jobController.js';
import { parsePodInput } from '../src/controllers/podController.js';
import { decodePodImage } from '../src/controllers/syncController.js';

const job = (over: Partial<JobSnapshot> = {}): JobSnapshot => ({
  JobId: 1, ClientId: 2, DriverId: null, ReferenceNumber: 'LM-000001', Status: 'Pending', ...over,
});

// --- Job notifications ---

test('assigning a driver and moving to Assigned sends "assigned"', () => {
  assert.deepEqual(jobNotificationEvents(job(), job({ Status: 'Assigned', DriverId: 9 })), ['assigned']);
});

test('setting the driver first, then the status, notifies only on the status change', () => {
  const withDriver = job({ DriverId: 9 });
  assert.deepEqual(jobNotificationEvents(job(), withDriver), []);
  assert.deepEqual(jobNotificationEvents(withDriver, job({ DriverId: 9, Status: 'Assigned' })), ['assigned']);
});

test('reassigning an Assigned job to a new driver notifies again', () => {
  const before = job({ Status: 'Assigned', DriverId: 9 });
  assert.deepEqual(jobNotificationEvents(before, job({ Status: 'Assigned', DriverId: 10 })), ['assigned']);
});

test('saving an Assigned job without changing driver or status sends nothing', () => {
  const same = job({ Status: 'Assigned', DriverId: 9 });
  assert.deepEqual(jobNotificationEvents(same, { ...same }), []);
});

test('Assigned with no driver sends nothing', () => {
  assert.deepEqual(jobNotificationEvents(job(), job({ Status: 'Assigned' })), []);
});

test('completing a job sends "completed" once', () => {
  const enRoute = job({ Status: 'EnRoute', DriverId: 9 });
  const done = job({ Status: 'Completed', DriverId: 9 });
  assert.deepEqual(jobNotificationEvents(enRoute, done), ['completed']);
  assert.deepEqual(jobNotificationEvents(done, done), []);
});

test('a job created already Assigned to a driver notifies', () => {
  assert.deepEqual(jobNotificationEvents(null, job({ Status: 'Assigned', DriverId: 9 })), ['assigned']);
  assert.deepEqual(jobNotificationEvents(null, job()), []);
});

// --- POD input (shared by /pod and offline SubmitPod) ---

test('parsePodInput accepts a valid POD and normalises CapturedAt', () => {
  const r = parsePodInput({ RecipientName: ' Thandi ', Latitude: '-29.85', Longitude: 31.02, CapturedAt: '2026-10-19T10:00:00+02:00' });
  assert.ok(r.ok);
  assert.equal(r.value.RecipientName, 'Thandi');
  assert.equal(r.value.Latitude, -29.85);
  assert.equal(r.value.CapturedAt, '2026-10-19T08:00:00.000Z');
  assert.equal(r.value.Notes, null);
});

test('parsePodInput rejects missing recipient, blank or bad coordinates, and bad dates', () => {
  assert.equal(parsePodInput({ Latitude: -29, Longitude: 31 }).ok, false);
  assert.equal(parsePodInput({ RecipientName: 'A', Latitude: '', Longitude: 31 }).ok, false);
  assert.equal(parsePodInput({ RecipientName: 'A', Latitude: -29, Longitude: 200 }).ok, false);
  assert.equal(parsePodInput({ RecipientName: 'A', Latitude: -29, Longitude: 31, CapturedAt: 'soon' }).ok, false);
});

// --- Offline POD images ---

const PNG_1PX = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==';

test('decodePodImage decodes base64 with an explicit MimeType', () => {
  const f = decodePodImage({ Base64: PNG_1PX, MimeType: 'image/png' }, 'Photo');
  assert.ok(f && typeof f === 'object');
  assert.equal(f.mimetype, 'image/png');
  assert.equal(f.buffer.subarray(1, 4).toString(), 'PNG');
});

test('decodePodImage accepts a data URL and takes the type from it', () => {
  const f = decodePodImage({ Base64: `data:image/png;base64,${PNG_1PX}` }, 'Photo');
  assert.ok(f && typeof f === 'object');
  assert.equal(f.mimetype, 'image/png');
});

test('decodePodImage treats a missing image as optional', () => {
  assert.equal(decodePodImage(undefined, 'Photo'), undefined);
  assert.equal(decodePodImage(null, 'Signature'), undefined);
});

test('decodePodImage rejects non-images and invalid base64', () => {
  assert.equal(typeof decodePodImage({ Base64: PNG_1PX, MimeType: 'application/pdf' }, 'Photo'), 'string');
  assert.equal(typeof decodePodImage({ Base64: 'not base64!!', MimeType: 'image/png' }, 'Photo'), 'string');
  assert.equal(typeof decodePodImage({ MimeType: 'image/png' }, 'Photo'), 'string');
  assert.equal(typeof decodePodImage('just a string', 'Photo'), 'string');
});

test('decodePodImage rejects images over 10 MB', () => {
  const big = Buffer.alloc(10 * 1024 * 1024 + 1).toString('base64');
  assert.match(decodePodImage({ Base64: big, MimeType: 'image/jpeg' }, 'Photo') as string, /10 MB/);
});
