import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createDriver } from '../src/controllers/driverController.js';
import { createJob, updateJob, updateJobStatus } from '../src/controllers/jobController.js';
import { fakeReq, fakeRes } from './helpers.js';

// Every case here is rejected before the controller touches Supabase.

const admin = { id: '1', role: 'admin' };

test('createDriver requires a UserId', async () => {
  const res = fakeRes();
  await createDriver(fakeReq({ body: { DepotId: 1 }, user: admin }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /UserId/);
});

test('createDriver rejects blank licence details', async () => {
  const res = fakeRes();
  await createDriver(fakeReq({
    body: { UserId: 5, DepotId: 1, EmployeeNumber: 'EMP-0001', PhoneNumber: '0821234567', LicenceNumber: '   ' },
    user: admin,
  }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /LicenceNumber/);
});

test('createDriver rejects a non-boolean IsAvailable', async () => {
  const res = fakeRes();
  await createDriver(fakeReq({
    body: { UserId: 5, DepotId: 1, EmployeeNumber: 'E1', PhoneNumber: '0821234567', LicenceNumber: 'L1', IsAvailable: 'yes' },
    user: admin,
  }), res);
  assert.equal(res.statusCode, 400);
});

test('createJob requires a client, depot and scheduled date', async () => {
  const noClient = fakeRes();
  await createJob(fakeReq({ body: { DepotId: 1, ScheduledDate: '2026-10-19T09:00:00Z' }, user: admin }), noClient);
  assert.equal(noClient.statusCode, 400);
  assert.match(noClient.body.error, /ClientId/);

  const noDepot = fakeRes();
  await createJob(fakeReq({ body: { ClientId: 1, ScheduledDate: '2026-10-19T09:00:00Z' }, user: admin }), noDepot);
  assert.match(noDepot.body.error, /DepotId/);

  const badDate = fakeRes();
  await createJob(fakeReq({ body: { ClientId: 1, DepotId: 1, ScheduledDate: 'tomorrow-ish' }, user: admin }), badDate);
  assert.match(badDate.body.error, /ScheduledDate/);
});

test('createJob rejects an unknown status or priority', async () => {
  const base = { ClientId: 1, DepotId: 1, ScheduledDate: '2026-10-19T09:00:00Z' };

  const status = fakeRes();
  await createJob(fakeReq({ body: { ...base, Status: 'Lost' }, user: admin }), status);
  assert.equal(status.statusCode, 400);

  const priority = fakeRes();
  await createJob(fakeReq({ body: { ...base, Priority: 'Whenever' }, user: admin }), priority);
  assert.equal(priority.statusCode, 400);
});

test('createJob refuses server-owned fields', async () => {
  const res = fakeRes();
  await createJob(fakeReq({
    body: { ClientId: 1, DepotId: 1, ScheduledDate: '2026-10-19T09:00:00Z', CreatedByUserId: 99 },
    user: admin,
  }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /CreatedByUserId/);
});

test('updateJob validates the id and needs at least one field', async () => {
  const badId = fakeRes();
  await updateJob(fakeReq({ params: { id: 'abc' }, body: { Priority: 'High' }, user: admin }), badId);
  assert.equal(badId.statusCode, 400);

  const empty = fakeRes();
  await updateJob(fakeReq({ params: { id: '1' }, body: {}, user: admin }), empty);
  assert.equal(empty.statusCode, 400);
});

test('updateJobStatus rejects a status that does not exist', async () => {
  const res = fakeRes();
  await updateJobStatus(fakeReq({ params: { id: '1' }, body: { Status: 'Teleported' }, user: admin }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /Status must be one of/);
});
