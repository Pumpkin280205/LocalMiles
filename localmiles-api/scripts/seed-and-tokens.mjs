// Dev helper: ensure baseline rows exist (admin/driver/client users, a Client,
// an active Depot) and print signed JWTs + useful IDs as shell-eval-able lines.
//   eval "$(node scripts/seed-and-tokens.mjs)"
//
// What the tokens are for
// -----------------------
// Every route except POST /api/auth/login sits behind requireAuth
// (src/middleware/auth.ts), which expects an "Authorization: Bearer <jwt>"
// header. Normally that JWT is minted by the login handler after it verifies a
// Google ID token. For manual testing (Postman, curl) we skip Google entirely:
// this script signs the SAME kind of session JWT directly.
//
// How it works
// ------------
//   1. Upsert three known users (admin / driver / client) plus a Client row and
//      an active Depot, so the DB has something to act on.
//   2. sign(u) below builds a JWT with the exact payload the real login handler
//      uses -- { id: UserId, role: Role } -- and signs it with JWT_SECRET from
//      .env. requireAuth only checks that signature, so it can't tell this token
//      apart from one issued via Google.
//   3. Tokens expire in 4h (see `sign`). Re-run this script to get fresh ones.
//
// Usage: paste the value after ADMIN_TOKEN= / DRIVER_TOKEN= into Postman as
//        Authorization: Bearer <token>, then call e.g. GET /api/depots.
//        Use ADMIN_TOKEN for admin-only routes (create/update/delete);
//        DRIVER_TOKEN behaves as a regular authenticated driver.
import { createClient } from '@supabase/supabase-js';
import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';
dotenv.config();

const sb = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);
const SECRET = process.env.JWT_SECRET;

async function ensureUser(p) {
  let { data: row } = await sb.from('User').select('*').eq('Email', p.Email).maybeSingle();
  if (!row) {
    const ins = await sb.from('User').insert(p).select().single();
    if (ins.error) { console.error(`insert ${p.Email}:`, ins.error.message); process.exit(1); }
    row = ins.data;
  }
  return row;
}

const admin = await ensureUser({ Email: 'admin@localmiles.test', FirstName: 'Test', LastName: 'Admin', Role: 'admin', SsoProviderId: 'seed-admin' });
const driver = await ensureUser({ Email: 'driver@localmiles.test', FirstName: 'Test', LastName: 'Driver', Role: 'driver', SsoProviderId: 'seed-driver' });
const clientUser = await ensureUser({ Email: 'client@localmiles.test', FirstName: 'Test', LastName: 'Client', Role: 'client', SsoProviderId: 'seed-client' });

let { data: client } = await sb.from('Client').select('*').eq('UserId', clientUser.UserId).maybeSingle();
if (!client) {
  const ins = await sb.from('Client').insert({
    UserId: clientUser.UserId, CompanyName: 'Acme Logistics', ContactPerson: 'Test Client',
    Email: 'client@localmiles.test', Address: '1 Test Rd',
  }).select().single();
  if (ins.error) { console.error('insert Client:', ins.error.message); process.exit(1); }
  client = ins.data;
}

let { data: depot } = await sb.from('Depot').select('*').eq('IsActive', true).limit(1).maybeSingle();
if (!depot) {
  const ins = await sb.from('Depot').insert({
    Name: 'Seed Depot', Address: '1 Seed St', Latitude: -33.9, Longitude: 18.4,
  }).select().single();
  if (ins.error) { console.error('insert Depot:', ins.error.message); process.exit(1); }
  depot = ins.data;
}

const sign = (u) => jwt.sign({ id: u.UserId, role: u.Role }, SECRET, { expiresIn: '4h' });

console.log(`ADMIN_USER_ID=${admin.UserId}`);
console.log(`DRIVER_USER_ID=${driver.UserId}`);
console.log(`CLIENT_ID=${client.ClientId}`);
console.log(`DEPOT_ID=${depot.DepotId}`);
console.log(`ADMIN_TOKEN=${sign(admin)}`);
console.log(`DRIVER_TOKEN=${sign(driver)}`);
console.log(`CLIENT_TOKEN=${sign(clientUser)}`);
