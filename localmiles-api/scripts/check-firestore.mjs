// Dev helper: confirm the API can reach Firestore with FIREBASE_SERVICE_ACCOUNT.
//   node scripts/check-firestore.mjs
// Writes a test doc to _healthcheck/api, reads it back, then deletes it.
// Exits 0 on success, 1 on failure (with a hint for the common errors).
import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { readFileSync } from 'node:fs';
import dotenv from 'dotenv';
dotenv.config({ quiet: true });

const raw = process.env.FIREBASE_SERVICE_ACCOUNT?.trim();
if (!raw) {
  console.error('FAIL: FIREBASE_SERVICE_ACCOUNT is not set in .env');
  process.exit(1);
}

const creds = JSON.parse(raw.startsWith('{') ? raw : readFileSync(raw, 'utf8'));
console.log(`Project: ${creds.project_id}`);
console.log(`Service account: ${creds.client_email}`);

const db = getFirestore(initializeApp({ credential: cert(creds) }));
const ref = db.collection('_healthcheck').doc('api');

try {
  await ref.set({ ok: true, at: FieldValue.serverTimestamp() });
  console.log('Write: OK');
  const snap = await ref.get();
  console.log(`Read:  OK (server time ${snap.data().at.toDate().toISOString()})`);
  await ref.delete();
  console.log('Delete: OK');
  console.log('\nFirestore is connected.');
  process.exit(0);
} catch (e) {
  console.error(`\nFAIL: ${e.message}`);
  if (e.code === 5 || /NOT_FOUND/.test(e.message))
    console.error('Hint: Firestore database not created yet - Firebase console > Build > Firestore Database > Create database.');
  else if (e.code === 7 || /PERMISSION_DENIED/.test(e.message))
    console.error('Hint: the service account lacks access, or the Firestore API is disabled for this project.');
  process.exit(1);
}
