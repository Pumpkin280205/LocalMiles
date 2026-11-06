import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildPushMessage, isDeadTokenError, isValidFcmToken, pushNotifications, ANDROID_CHANNEL_ID } from '../src/lib/push.js';
import { registerToken, unregisterToken } from '../src/controllers/deviceController.js';
import { fakeReq, fakeRes } from './helpers.js';

const TOKEN = 'dQw4w9WgXcQ:APA91bH-example_token_value_1234567890';
const user = { id: '5', role: 'driver' };

test('push message carries title/body, string-only data and the Android channel', () => {
  const msg = buildPushMessage(
    { NotificationId: 12, UserId: 5, Title: 'New job assigned', Message: "You've been assigned job #7.", NotificationType: 'JobAssigned', RelatedEntityId: '7' },
    [TOKEN]
  );
  assert.deepEqual(msg.tokens, [TOKEN]);
  assert.deepEqual(msg.notification, { title: 'New job assigned', body: "You've been assigned job #7." });
  assert.deepEqual(msg.data, { type: 'JobAssigned', relatedEntityId: '7', notificationId: '12' });
  assert.equal(msg.android?.priority, 'high');
  assert.equal(msg.android?.notification?.channelId, ANDROID_CHANNEL_ID);
});

test('push message data uses empty string for a missing related entity', () => {
  const msg = buildPushMessage(
    { UserId: 5, Title: 't', Message: 'm', NotificationType: 'SyncIssue', RelatedEntityId: null },
    [TOKEN]
  );
  assert.equal(msg.data?.relatedEntityId, '');
  assert.equal(msg.data?.notificationId, undefined);
});

test('only permanent FCM errors mark a token as dead', () => {
  assert.equal(isDeadTokenError('messaging/registration-token-not-registered'), true);
  assert.equal(isDeadTokenError('messaging/invalid-registration-token'), true);
  assert.equal(isDeadTokenError('messaging/internal-error'), false);
  assert.equal(isDeadTokenError(undefined), false);
});

test('FCM token validation', () => {
  assert.equal(isValidFcmToken(TOKEN), true);
  assert.equal(isValidFcmToken('short'), false);
  assert.equal(isValidFcmToken('has a space in it 1234567890'), false);
  assert.equal(isValidFcmToken('has/slash/1234567890abcdefgh'), false);
  assert.equal(isValidFcmToken(123), false);
});

test('register token rejects bad input before touching Firebase', async () => {
  const noToken = fakeRes();
  await registerToken(fakeReq({ body: {}, user }), noToken);
  assert.equal(noToken.statusCode, 400);

  const badPlatform = fakeRes();
  await registerToken(fakeReq({ body: { Token: TOKEN, Platform: 'ios' }, user }), badPlatform);
  assert.equal(badPlatform.statusCode, 400);
});

test('device token endpoints return 503 when Firebase is not configured', async () => {
  const reg = fakeRes();
  await registerToken(fakeReq({ body: { Token: TOKEN }, user }), reg);
  assert.equal(reg.statusCode, 503);

  const unreg = fakeRes();
  await unregisterToken(fakeReq({ body: { Token: TOKEN }, user }), unreg);
  assert.equal(unreg.statusCode, 503);
});

test('pushNotifications is a silent no-op without Firebase', async () => {
  await pushNotifications([{ UserId: 5, Title: 't', Message: 'm', NotificationType: 'X', RelatedEntityId: null }]);
});
