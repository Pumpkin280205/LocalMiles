import { supabase } from '../supabaseClient.js';

interface NotificationRow {
  UserId: number;
  Title: string;
  Message: string;
  NotificationType: string;
  RelatedEntityId: string | null;
}

//Best-effort bulk insert: logs and swallows errors so a notification failure never blocks the caller's actual mutation. */
async function insertNotifications(rows: NotificationRow[]): Promise<void> {
  if (rows.length === 0) return;
  const { error } = await supabase.from('Notification').insert(rows);
  if (error) console.error('Failed to create notification(s):', error);
}

//Active admin UserIds, for dispatcher-visibility notifications. */
async function adminUserIds(): Promise<number[]> {
  const { data, error } = await supabase.from('User').select('UserId').eq('Role', 'admin').eq('IsActive', true);
  if (error) { console.error('Failed to look up admins for notification:', error); return []; }
  return (data ?? []).map((u) => u.UserId);
}

async function userIdForDriverId(driverId: number): Promise<number | null> {
  const { data } = await supabase.from('Driver').select('UserId').eq('DriverId', driverId).maybeSingle();
  return data ? data.UserId : null;
}

async function userIdForClientId(clientId: number): Promise<number | null> {
  const { data } = await supabase.from('Client').select('UserId').eq('ClientId', clientId).maybeSingle();
  return data ? data.UserId : null;
}

export interface JobForNotify {
  JobId: number;
  ClientId: number;
  DriverId: number | null;
  ReferenceNumber: string | null;
}

//Job moved to Assigned: notify the driver, the client, and dispatchers (admins). */
export async function notifyJobAssigned(job: JobForNotify): Promise<void> {
  const ref = job.ReferenceNumber ?? `#${job.JobId}`;
  const rows: NotificationRow[] = [];

  if (job.DriverId !== null) {
    const driverUserId = await userIdForDriverId(job.DriverId);
    if (driverUserId !== null) {
      rows.push({
        UserId: driverUserId,
        Title: 'New job assigned',
        Message: `You've been assigned job ${ref}.`,
        NotificationType: 'JobAssigned',
        RelatedEntityId: String(job.JobId),
      });
    }
  }

  const clientUserId = await userIdForClientId(job.ClientId);
  if (clientUserId !== null) {
    rows.push({
      UserId: clientUserId,
      Title: 'Job assigned',
      Message: `Your job ${ref} has been assigned to a driver.`,
      NotificationType: 'JobAssigned',
      RelatedEntityId: String(job.JobId),
    });
  }

  for (const adminId of await adminUserIds()) {
    rows.push({
      UserId: adminId,
      Title: 'Job assigned',
      Message: `Job ${ref} was assigned to a driver.`,
      NotificationType: 'JobAssigned',
      RelatedEntityId: String(job.JobId),
    });
  }

  await insertNotifications(rows);
}

//Job moved to Completed: notify the client and dispatchers. */
export async function notifyJobCompleted(job: JobForNotify): Promise<void> {
  const ref = job.ReferenceNumber ?? `#${job.JobId}`;
  const rows: NotificationRow[] = [];

  const clientUserId = await userIdForClientId(job.ClientId);
  if (clientUserId !== null) {
    rows.push({
      UserId: clientUserId,
      Title: 'Delivery completed',
      Message: `Your job ${ref} has been completed.`,
      NotificationType: 'JobCompleted',
      RelatedEntityId: String(job.JobId),
    });
  }

  for (const adminId of await adminUserIds()) {
    rows.push({
      UserId: adminId,
      Title: 'Job completed',
      Message: `Job ${ref} has been completed.`,
      NotificationType: 'JobCompleted',
      RelatedEntityId: String(job.JobId),
    });
  }

  await insertNotifications(rows);
}

//Stop marked Failed: notify the client and dispatchers, with the reason. */
export async function notifyDeliveryFailed(job: JobForNotify, stopId: number, reason: string): Promise<void> {
  const ref = job.ReferenceNumber ?? `#${job.JobId}`;
  const rows: NotificationRow[] = [];

  const clientUserId = await userIdForClientId(job.ClientId);
  if (clientUserId !== null) {
    rows.push({
      UserId: clientUserId,
      Title: 'Delivery attempt failed',
      Message: `A delivery attempt on job ${ref} failed: ${reason}`,
      NotificationType: 'DeliveryFailed',
      RelatedEntityId: String(stopId),
    });
  }

  for (const adminId of await adminUserIds()) {
    rows.push({
      UserId: adminId,
      Title: 'Delivery attempt failed',
      Message: `Job ${ref}: a delivery attempt failed (${reason}).`,
      NotificationType: 'DeliveryFailed',
      RelatedEntityId: String(stopId),
    });
  }

  await insertNotifications(rows);
}

//An offline sync action ended Failed: notify the user who queued it, and dispatchers. */
export async function notifySyncIssue(userId: number, syncActionId: string, errorMessage: string | null): Promise<void> {
  const suffix = errorMessage ? `: ${errorMessage}` : '.';
  const rows: NotificationRow[] = [
    {
      UserId: userId,
      Title: 'Sync issue',
      Message: `One of your offline actions failed to sync${suffix}`,
      NotificationType: 'SyncIssue',
      RelatedEntityId: syncActionId,
    },
  ];

  for (const adminId of await adminUserIds()) {
    rows.push({
      UserId: adminId,
      Title: 'Sync issue',
      Message: `A driver's offline action failed to sync${suffix}`,
      NotificationType: 'SyncIssue',
      RelatedEntityId: syncActionId,
    });
  }

  await insertNotifications(rows);
}
