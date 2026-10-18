// Loaded with `node --import` before any test module, so the env checks that
// run at import time (supabaseClient, auth middleware) see these dummy values.
// No test reaches the database — they cover the validation and auth logic that
// runs before any Supabase call.
process.env.SUPABASE_URL = 'http://localhost:54321';
process.env.SUPABASE_SERVICE_ROLE_KEY = 'test-service-role-key';
process.env.JWT_SECRET = 'test-jwt-secret';
// verifyIdToken refuses to load with no verifier; a dummy Google client ID
// is enough (it's only used when a real token is checked).
process.env.GOOGLE_CLIENT_ID = 'test-client-id.apps.googleusercontent.com';
process.env.FIREBASE_SERVICE_ACCOUNT = '';
