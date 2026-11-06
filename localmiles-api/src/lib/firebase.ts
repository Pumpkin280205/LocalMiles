import type { App } from 'firebase-admin/app';
import type { Firestore } from 'firebase-admin/firestore';

/**
 * Shared firebase-admin app, initialised once from FIREBASE_SERVICE_ACCOUNT
 * (the service-account JSON on one line, or a path to the JSON file).
 * Used by SSO token verification, Firestore and FCM so they all share one
 * set of credentials. Loaded lazily so the API still boots without Firebase.
 */
let appPromise: Promise<App> | null = null;

export function isFirebaseConfigured(): boolean {
  return !!process.env.FIREBASE_SERVICE_ACCOUNT?.trim();
}

export function getFirebaseApp(): Promise<App> {
  if (!isFirebaseConfigured()) throw new Error('Firebase not configured: set FIREBASE_SERVICE_ACCOUNT');
  if (!appPromise) {
    appPromise = (async () => {
      const { initializeApp, cert, getApps } = await import('firebase-admin/app');
      const raw = process.env.FIREBASE_SERVICE_ACCOUNT!.trim();
      const json = raw.startsWith('{')
        ? raw
        : (await import('node:fs')).readFileSync(raw, 'utf8');
      const creds = JSON.parse(json);
      return getApps().length ? getApps()[0]! : initializeApp({ credential: cert(creds) });
    })();
  }
  return appPromise;
}

let firestorePromise: Promise<Firestore> | null = null;

//Firestore (NoSQL) handle on the shared app. */
export function getFirestoreDb(): Promise<Firestore> {
  if (!firestorePromise) {
    firestorePromise = (async () => {
      const { getFirestore } = await import('firebase-admin/firestore');
      return getFirestore(await getFirebaseApp());
    })();
  }
  return firestorePromise;
}
