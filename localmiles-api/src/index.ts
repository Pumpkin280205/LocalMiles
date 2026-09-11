import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { loginHandler } from './controllers/authController.js';
import { requireAuth, requireRole, ROLE } from './middleware/auth.js';
import {
  listDepots,
  getDepot,
  createDepot,
  updateDepot,
  deactivateDepot,
} from './controllers/depotController.js';
import {
  listDrivers,
  getMyDriver,
  getDriver,
  createDriver,
  updateDriver,
  deactivateDriver,
} from './controllers/driverController.js';
import {
  listVehicles,
  getVehicle,
  createVehicle,
  updateVehicle,
  deactivateVehicle,
} from './controllers/vehicleController.js';
import {
  listJobs,
  getJob,
  createJob,
  updateJob,
  updateJobStatus,
  cancelJob,
} from './controllers/jobController.js';
import {
  listStopsForJob,
  getStop,
  createStop,
  updateStop,
  updateStopStatus,
  cancelStop,
} from './controllers/stopController.js';
import { getJobRoute } from './controllers/routeController.js';
import {
  podUpload,
  submitPod,
  getPodForStop,
  getPod,
  listPodsForJob,
  deletePod,
} from './controllers/podController.js';
import {
  listNotifications,
  unreadCount,
  getNotification,
  createNotification,
  updateNotification,
  markAllRead,
  deleteNotification,
} from './controllers/notificationController.js';
import {
  submitSync,
  listSync,
  getSync,
  retrySync,
} from './controllers/syncController.js';
dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

// --- Public routes (no token required) ---
app.post('/api/auth/login', loginHandler);

// --- Everything below requires a valid JWT ---
app.use(requireAuth);

// Depots: any authenticated user can read; only admins can mutate.
app.get('/api/depots', listDepots);
app.get('/api/depots/:id', getDepot);
app.post('/api/depots', requireRole(ROLE.ADMIN), createDepot);
app.patch('/api/depots/:id', requireRole(ROLE.ADMIN), updateDepot);
app.delete('/api/depots/:id', requireRole(ROLE.ADMIN), deactivateDepot);

// Drivers: any authenticated user can read; only admins can mutate.
// '/me' must be registered before '/:id' so it isn't captured as an id.
app.get('/api/drivers', listDrivers);
app.get('/api/drivers/me', getMyDriver);
app.get('/api/drivers/:id', getDriver);
app.post('/api/drivers', requireRole(ROLE.ADMIN), createDriver);
app.patch('/api/drivers/:id', requireRole(ROLE.ADMIN), updateDriver);
app.delete('/api/drivers/:id', requireRole(ROLE.ADMIN), deactivateDriver);

// Vehicles: any authenticated user can read; only admins can mutate.
app.get('/api/vehicles', listVehicles);
app.get('/api/vehicles/:id', getVehicle);
app.post('/api/vehicles', requireRole(ROLE.ADMIN), createVehicle);
app.patch('/api/vehicles/:id', requireRole(ROLE.ADMIN), updateVehicle);
app.delete('/api/vehicles/:id', requireRole(ROLE.ADMIN), deactivateVehicle);

// Jobs: read = any authenticated user; create/edit/cancel = admin;
// status updates = the assigned driver or an admin.
app.get('/api/jobs', listJobs);
app.get('/api/jobs/:id', getJob);
app.post('/api/jobs', requireRole(ROLE.ADMIN), createJob);
app.patch('/api/jobs/:id', requireRole(ROLE.ADMIN), updateJob);
app.patch('/api/jobs/:id/status', updateJobStatus);
app.delete('/api/jobs/:id', requireRole(ROLE.ADMIN), cancelJob);

// Route optimisation: read-only, computed fresh each call (nothing persisted).
// Assigned driver or admin only.
app.get('/api/jobs/:id/route', getJobRoute);

// Delivery stops: nested under a job for list/create; flat for item ops.
// Read = any auth; plan/edit/cancel = admin; status updates = assigned driver or admin.
app.get('/api/jobs/:jobId/stops', listStopsForJob);
app.post('/api/jobs/:jobId/stops', requireRole(ROLE.ADMIN), createStop);
app.get('/api/stops/:id', getStop);
app.patch('/api/stops/:id', requireRole(ROLE.ADMIN), updateStop);
app.patch('/api/stops/:id/status', updateStopStatus);
app.delete('/api/stops/:id', requireRole(ROLE.ADMIN), cancelStop);

// Proof of delivery: submit = assigned driver or admin (multipart upload);
// read = any auth; delete = admin.
app.post('/api/stops/:stopId/pod', podUpload, submitPod);
app.get('/api/stops/:stopId/pod', getPodForStop);
app.get('/api/jobs/:jobId/pods', listPodsForJob);
app.get('/api/pod/:id', getPod);
app.delete('/api/pod/:id', requireRole(ROLE.ADMIN), deletePod);

// Notifications: a user reads/dismisses their own; only admins create them.
// Fixed sub-paths before '/:id' so they aren't captured as an id.
app.get('/api/notifications', listNotifications);
app.get('/api/notifications/unread-count', unreadCount);
app.post('/api/notifications', requireRole(ROLE.ADMIN), createNotification);
app.post('/api/notifications/read-all', markAllRead);
app.get('/api/notifications/:id', getNotification);
app.patch('/api/notifications/:id', updateNotification);
app.delete('/api/notifications/:id', deleteNotification);

// Offline sync: clients replay queued actions keyed by a client-generated UUID.
// Duplicates (same UUID) are 
// and never re-applied. Actions are the
// caller's own; ownership rules inside dispatch mirror the live endpoints.
app.post('/api/sync', submitSync);
app.get('/api/sync', listSync);
app.get('/api/sync/:id', getSync);
app.post('/api/sync/:id/retry', retrySync);

// Error handler — turns Multer upload errors into 400s instead of 500s.
app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  if (err?.name === 'MulterError') {
    return res.status(400).json({ error: `Upload error: ${err.message}` });
  }
  console.error('Unhandled error:', err);
  return res.status(500).json({ error: 'Internal server error' });
});

// Dev aid: dump every registered route to the terminal on boot.
function printRoutes(a: typeof app) {
  const order = ['GET', 'POST', 'PATCH', 'PUT', 'DELETE'];
  const rows: { method: string; path: string }[] = [];
  for (const layer of (a as any).router.stack) {
    if (!layer.route) continue;
    for (const method of Object.keys(layer.route.methods)) {
      rows.push({ method: method.toUpperCase(), path: layer.route.path });
    }
  }
  rows.sort(
    (x, y) => x.path.localeCompare(y.path) || order.indexOf(x.method) - order.indexOf(y.method)
  );
  console.log(`\nRegistered routes (${rows.length}):`);
  for (const r of rows) console.log(`  ${r.method.padEnd(6)} ${r.path}`);
  console.log();
}

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`LocalMiles API running on http://localhost:${port}`);
  printRoutes(app);
});
