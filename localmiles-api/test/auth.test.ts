import { test } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';
import { requireAuth, requireRole, ROLE } from '../src/middleware/auth.js';
import { loginHandler } from '../src/controllers/authController.js';
import { fakeReq, fakeRes } from './helpers.js';

const sign = (payload: object, secret = process.env.JWT_SECRET!) =>
  jwt.sign(payload, secret, { expiresIn: '1h' });

test('requireAuth rejects a request with no bearer token', () => {
  const res = fakeRes();
  let called = false;
  requireAuth(fakeReq({}), res, () => { called = true; });
  assert.equal(res.statusCode, 401);
  assert.equal(called, false);
});

test('requireAuth rejects a token signed with the wrong secret', () => {
  const res = fakeRes();
  const token = sign({ id: 1, role: 'admin' }, 'someone-elses-secret');
  requireAuth(fakeReq({ headers: { authorization: `Bearer ${token}` } }), res, () => {});
  assert.equal(res.statusCode, 401);
});

test('requireAuth rejects a token without id/role', () => {
  const res = fakeRes();
  requireAuth(fakeReq({ headers: { authorization: `Bearer ${sign({ foo: 'bar' })}` } }), res, () => {});
  assert.equal(res.statusCode, 401);
});

test('requireAuth accepts a valid token and attaches the user', () => {
  const req = fakeReq({ headers: { authorization: `Bearer ${sign({ id: 7, role: 'driver' })}` } });
  let called = false;
  requireAuth(req, fakeRes(), () => { called = true; });
  assert.equal(called, true);
  assert.deepEqual(req.user, { id: '7', role: 'driver' });
});

test('requireRole lets admins through and blocks other roles', () => {
  const adminOnly = requireRole(ROLE.ADMIN);

  let adminPassed = false;
  adminOnly(fakeReq({ user: { id: '1', role: 'admin' } }), fakeRes(), () => { adminPassed = true; });
  assert.equal(adminPassed, true);

  const res = fakeRes();
  let driverPassed = false;
  adminOnly(fakeReq({ user: { id: '2', role: 'driver' } }), res, () => { driverPassed = true; });
  assert.equal(driverPassed, false);
  assert.equal(res.statusCode, 403);
});

test('POST /auth/login without an idToken is a 400', async () => {
  const res = fakeRes();
  await loginHandler(fakeReq({ body: {} }), res);
  assert.equal(res.statusCode, 400);
  assert.match(res.body.error, /idToken/);
});
