import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isValidJobTransition, JOB_STATUSES } from '../src/controllers/jobController.js';
import { isValidStopTransition } from '../src/controllers/stopController.js';

test('job lifecycle allows Pending -> Assigned -> EnRoute -> Completed', () => {
  assert.equal(isValidJobTransition('Pending', 'Assigned'), true);
  assert.equal(isValidJobTransition('Assigned', 'EnRoute'), true);
  assert.equal(isValidJobTransition('EnRoute', 'Completed'), true);
});

test('job lifecycle rejects skipping steps', () => {
  assert.equal(isValidJobTransition('Pending', 'EnRoute'), false);
  assert.equal(isValidJobTransition('Pending', 'Completed'), false);
  assert.equal(isValidJobTransition('Assigned', 'Completed'), false);
});

test('Completed and Cancelled jobs are terminal', () => {
  for (const to of JOB_STATUSES) {
    if (to !== 'Completed') assert.equal(isValidJobTransition('Completed', to), false);
    if (to !== 'Cancelled') assert.equal(isValidJobTransition('Cancelled', to), false);
  }
});

test('a same-status update is always a no-op', () => {
  for (const s of JOB_STATUSES) assert.equal(isValidJobTransition(s, s), true);
});

test('stops must be Arrived before Completed, and Failed can be retried', () => {
  assert.equal(isValidStopTransition('Pending', 'Completed'), false);
  assert.equal(isValidStopTransition('Pending', 'Arrived'), true);
  assert.equal(isValidStopTransition('Arrived', 'Completed'), true);
  assert.equal(isValidStopTransition('Failed', 'Arrived'), true);
  assert.equal(isValidStopTransition('Completed', 'Pending'), false);
});
