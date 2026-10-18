import { test } from 'node:test';
import assert from 'node:assert/strict';
import { haversineKm } from '../src/lib/geo.js';
import { parseId, posInt, isIsoDate, writeError } from '../src/lib/db.js';
import { fakeRes } from './helpers.js';

test('haversine distance between Durban and Johannesburg is about 500 km', () => {
  const km = haversineKm({ lat: -29.8587, lng: 31.0218 }, { lat: -26.2041, lng: 28.0473 });
  assert.ok(km > 480 && km < 520, `got ${km}`);
});

test('haversine distance from a point to itself is zero', () => {
  assert.equal(haversineKm({ lat: -33.92, lng: 18.42 }, { lat: -33.92, lng: 18.42 }), 0);
});

test('the 200 m POD radius is measurable at street scale', () => {
  // ~150 m apart on the same street.
  const metres = haversineKm({ lat: -29.8587, lng: 31.0218 }, { lat: -29.8574, lng: 31.0218 }) * 1000;
  assert.ok(metres > 100 && metres < 200, `got ${metres}`);
});

test('parseId only accepts positive integer strings', () => {
  assert.equal(parseId('42'), 42);
  assert.equal(parseId('0'), null);
  assert.equal(parseId('-1'), null);
  assert.equal(parseId('3.5'), null);
  assert.equal(parseId('abc'), null);
  assert.equal(parseId(undefined), null);
});

test('posInt and isIsoDate', () => {
  assert.equal(posInt(5), true);
  assert.equal(posInt(0), false);
  assert.equal(posInt('5'), false);
  assert.equal(isIsoDate('2026-10-19T09:30:00+02:00'), true);
  assert.equal(isIsoDate('not a date'), false);
  assert.equal(isIsoDate(123), false);
});

test('writeError maps Postgres error codes to HTTP statuses', () => {
  const unique = fakeRes();
  writeError(unique, { code: '23505', details: 'Key (EmployeeNumber)=(E1) already exists.' }, 'x');
  assert.equal(unique.statusCode, 409);
  assert.match(unique.body.error, /EmployeeNumber/);

  const fk = fakeRes();
  writeError(fk, { code: '23503', details: 'bad depot' }, 'x');
  assert.equal(fk.statusCode, 400);

  const notNull = fakeRes();
  writeError(notNull, { code: '23502', message: 'null value' }, 'x');
  assert.equal(notNull.statusCode, 400);

  const unknown = fakeRes();
  const originalError = console.error;
  console.error = () => {};
  writeError(unknown, { code: 'XX000' }, 'Database error');
  console.error = originalError;
  assert.equal(unknown.statusCode, 500);
  assert.deepEqual(unknown.body, { error: 'Database error' });
});
