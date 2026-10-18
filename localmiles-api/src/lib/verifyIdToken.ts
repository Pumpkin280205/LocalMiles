import { OAuth2Client } from 'google-auth-library';
import { getFirebaseApp } from './firebase.js';

/**
 * Identity extracted from a verified SSO ID token, normalised so the login
 * handler doesn't care which provider issued the token.
 */
export interface SsoIdentity {
  email: string;
  ssoProviderId: string;
  firstName: string;
  lastName: string;
}

const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID;
const FIREBASE_SERVICE_ACCOUNT = process.env.FIREBASE_SERVICE_ACCOUNT;

if (!GOOGLE_CLIENT_ID && !FIREBASE_SERVICE_ACCOUNT) {
  throw new Error(
    'No SSO verifier configured: set FIREBASE_SERVICE_ACCOUNT and/or GOOGLE_CLIENT_ID',
  );
}

// --- Google: raw Google Sign-In ID tokens ---
const googleClient = GOOGLE_CLIENT_ID ? new OAuth2Client(GOOGLE_CLIENT_ID) : null;

async function verifyWithGoogle(idToken: string): Promise<SsoIdentity> {
  if (!googleClient) throw new Error('Google verifier not configured');
  const ticket = await googleClient.verifyIdToken({ idToken, audience: GOOGLE_CLIENT_ID! });
  const p = ticket.getPayload();
  if (!p?.email || !p.sub) throw new Error('Google token missing email or sub');
  return {
    email: p.email,
    ssoProviderId: p.sub,
    firstName: p.given_name ?? p.name ?? '',
    lastName: p.family_name ?? '',
  };
}

// --- Firebase: Firebase Auth ID tokens ---
let firebaseAuthPromise: Promise<import('firebase-admin/auth').Auth> | null = null;

function getFirebaseAuth() {
  if (!FIREBASE_SERVICE_ACCOUNT) throw new Error('Firebase verifier not configured');
  if (!firebaseAuthPromise) {
    firebaseAuthPromise = (async () => {
      const { getAuth } = await import('firebase-admin/auth');
      return getAuth(await getFirebaseApp());
    })();
  }
  return firebaseAuthPromise;
}

async function verifyWithFirebase(idToken: string): Promise<SsoIdentity> {
  const auth = await getFirebaseAuth();
  const d = await auth.verifyIdToken(idToken);
  if (!d.email) throw new Error('Firebase token missing email');
  const name = (d.name as string | undefined)?.trim() ?? '';
  const space = name.indexOf(' ');
  return {
    email: d.email,
    ssoProviderId: d.uid,
    firstName: space === -1 ? name : name.slice(0, space),
    lastName: space === -1 ? '' : name.slice(space + 1),
  };
}

/**
 * Verify a client-supplied ID token against whichever providers are configured.
 * Firebase is tried first (if configured), then Google. Returns a normalised
 * identity, or throws if no configured verifier accepts the token.
 */
export async function verifyIdToken(idToken: string): Promise<SsoIdentity> {
  const errors: string[] = [];

  if (FIREBASE_SERVICE_ACCOUNT) {
    try {
      return await verifyWithFirebase(idToken);
    } catch (e) {
      errors.push(`firebase: ${(e as Error).message}`);
    }
  }
  if (GOOGLE_CLIENT_ID) {
    try {
      return await verifyWithGoogle(idToken);
    } catch (e) {
      errors.push(`google: ${(e as Error).message}`);
    }
  }
  throw new Error(`ID token rejected by all verifiers [${errors.join('; ')}]`);
}
