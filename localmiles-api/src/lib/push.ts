import type { MulticastMessage } from 'firebase-admin/messaging';
import { getFirebaseApp, getFirestoreDb, isFirebaseConfigured } from './firebase.js';

/**
 * Real-time push via Firebase Cloud Messaging (FCM).
 *
 * Device tokens live in Firestore, one doc per token (the token is the doc id,
 * so re-registering is idempotent and a phone that switches accounts simply
 * moves to the new UserId):
 *   deviceTokens/{token}  { UserId, Platform, CreatedAt, LastSeenAt }
 *
 * Every Notification row the API creates is also pushed to each of that user's
 * devices. Pushing is best-effort: failures are logged, never thrown, so a push
 * problem can't block the mutation that triggered it.
 */
const COLLECTION = 'deviceTokens';
// Must match the channel the Android app creates (NotificationChannel id).
export const ANDROID_CHANNEL_ID = 'localmiles_updates';
const MAX_TOKEN_LENGTH = 4096;

export interface PushNotification {
  NotificationId?: number;
  UserId: number;
  Title: string;
  Message: string;
  NotificationType: string;
  RelatedEntityId: string | null;
}

//FCM error codes meaning the token is permanently dead and should be forgotten. */
const DEAD_TOKEN_CODES = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
]);

export function isDeadTokenError(code: string | undefined): boolean {
  return !!code && DEAD_TOKEN_CODES.has(code);
}

//A plausible FCM registration token: non-empty, no whitespace or '/', sane length. */
export function isValidFcmToken(v: unknown): v is string {
  return typeof v === 'string' && v.length >= 20 && v.length <= MAX_TOKEN_LENGTH && !/[\s/]/.test(v);
}

/**
 * Build the FCM message for one notification. `notification` makes Android show
 * it in the tray when the app is in the background; `data` lets the app route a
 * tap to the right screen (all data values must be strings).
 */
export function buildPushMessage(n: PushNotification, tokens: string[]): MulticastMessage {
  const data: Record<string, string> = {
    type: n.NotificationType,
    relatedEntityId: n.RelatedEntityId ?? '',
  };
  if (n.NotificationId !== undefined) data.notificationId = String(n.NotificationId);
  return {
    tokens,
    notification: { title: n.Title, body: n.Message },
    data,
    android: {
      priority: 'high',
      notification: { channelId: ANDROID_CHANNEL_ID },
    },
  };
}

//Save (or re-assign) a device token for a user. */
export async function registerDeviceToken(userId: number, token: string, platform: string): Promise<void> {
  const db = await getFirestoreDb();
  const { FieldValue } = await import('firebase-admin/firestore');
  const ref = db.collection(COLLECTION).doc(token);
  const existing = await ref.get();
  await ref.set(
    {
      UserId: userId,
      Platform: platform,
      LastSeenAt: FieldValue.serverTimestamp(),
      ...(existing.exists ? {} : { CreatedAt: FieldValue.serverTimestamp() }),
    },
    { merge: true }
  );
}

//Remove a token (logout). Only the owning user can remove it. Returns false if it wasn't theirs. */
export async function unregisterDeviceToken(userId: number, token: string): Promise<boolean> {
  const db = await getFirestoreDb();
  const ref = db.collection(COLLECTION).doc(token);
  const snap = await ref.get();
  if (!snap.exists || snap.data()?.UserId !== userId) return false;
  await ref.delete();
  return true;
}

async function tokensForUser(userId: number): Promise<string[]> {
  const db = await getFirestoreDb();
  const snap = await db.collection(COLLECTION).where('UserId', '==', userId).get();
  return snap.docs.map((d) => d.id);
}

/** Push each notification to its user's devices. Best-effort; never throws. */
export async function pushNotifications(rows: PushNotification[]): Promise<void> {
  if (rows.length === 0 || !isFirebaseConfigured()) return;
  try {
    const { getMessaging } = await import('firebase-admin/messaging');
    const messaging = getMessaging(await getFirebaseApp());
    const db = await getFirestoreDb();
    const tokenCache = new Map<number, string[]>();

    for (const n of rows) {
      let tokens = tokenCache.get(n.UserId);
      if (!tokens) {
        tokens = await tokensForUser(n.UserId);
        tokenCache.set(n.UserId, tokens);
      }
      if (tokens.length === 0) continue;

      const result = await messaging.sendEachForMulticast(buildPushMessage(n, tokens));
      console.log(`FCM ${n.NotificationType} -> user ${n.UserId}: ${result.successCount} sent, ${result.failureCount} failed`);

      // Forget tokens FCM says are dead (app uninstalled, data cleared, etc.).
      const dead = result.responses
        .map((r, i) => (!r.success && isDeadTokenError(r.error?.code) ? tokens![i]! : null))
        .filter((t): t is string => t !== null);
      if (dead.length) {
        await Promise.all(dead.map((t) => db.collection(COLLECTION).doc(t).delete()));
        tokenCache.set(n.UserId, tokens.filter((t) => !dead.includes(t)));
        console.log(`Removed ${dead.length} dead FCM token(s) for user ${n.UserId}`);
      }
    }
  } catch (e) {
    console.error('FCM push failed:', e);
  }
}
