import type { Request, Response } from 'express';
import { isFirebaseConfigured } from '../lib/firebase.js';
import { isValidFcmToken, registerDeviceToken, unregisterDeviceToken } from '../lib/push.js';

const PLATFORMS = ['android'] as const;

const callerId = (req: Request) => Number(req.user!.id);

/**
 * POST /api/devices/token — any authenticated user.
 * Body: { Token*: string, Platform?: 'android' }
 * Call after login and whenever FirebaseMessagingService.onNewToken fires.
 */
export async function registerToken(req: Request, res: Response) {
  const { Token, Platform = 'android' } = req.body ?? {};
  if (!isValidFcmToken(Token)) return res.status(400).json({ error: 'Token must be a valid FCM registration token' });
  if (!PLATFORMS.includes(Platform)) return res.status(400).json({ error: `Platform must be one of: ${PLATFORMS.join(', ')}` });
  if (!isFirebaseConfigured()) return res.status(503).json({ error: 'Push notifications are not configured on this server' });

  try {
    await registerDeviceToken(callerId(req), Token, Platform);
    console.log(`FCM token registered for user ${callerId(req)}`);
    return res.status(201).json({ registered: true });
  } catch (e) {
    console.error('Failed to register FCM token:', e);
    return res.status(502).json({ error: 'Could not register device' });
  }
}

/**
 * DELETE /api/devices/token — the token's owner.
 * Body: { Token*: string }. Call on logout so the device stops receiving pushes.
 */
export async function unregisterToken(req: Request, res: Response) {
  const { Token } = req.body ?? {};
  if (!isValidFcmToken(Token)) return res.status(400).json({ error: 'Token must be a valid FCM registration token' });
  if (!isFirebaseConfigured()) return res.status(503).json({ error: 'Push notifications are not configured on this server' });

  try {
    const removed = await unregisterDeviceToken(callerId(req), Token);
    if (!removed) return res.status(404).json({ error: 'Device token not found' });
    return res.json({ unregistered: true });
  } catch (e) {
    console.error('Failed to unregister FCM token:', e);
    return res.status(502).json({ error: 'Could not unregister device' });
  }
}
