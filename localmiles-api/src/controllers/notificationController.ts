import type { Request, Response } from 'express';
import { supabase } from '../supabaseClient.js';
import { parseId, writeError, posInt } from '../lib/db.js';
import { ROLE } from '../middleware/auth.js';

const TABLE = 'Notification';
const PK = 'NotificationId';

const callerId = (req: Request) => Number(req.user!.id);
const isAdmin = (req: Request) => req.user!.role === ROLE.ADMIN;

function nonEmptyString(v: unknown, max = 1000): v is string {
  return typeof v === 'string' && v.trim().length > 0 && v.trim().length <= max;
}

/**
 * GET /api/notifications — the caller's own notifications.
 * Filters: isRead=true|false, type=<string>, limit (<=200, def 50), offset.
 */
export async function listNotifications(req: Request, res: Response) {
  const q = req.query;
  let query = supabase
    .from(TABLE)
    .select('*')
    .eq('UserId', callerId(req))
    .order('CreatedAt', { ascending: false });

  if (q.isRead === 'true') query = query.eq('IsRead', true);
  else if (q.isRead === 'false') query = query.eq('IsRead', false);

  if (q.type !== undefined) {
    if (!nonEmptyString(q.type)) return res.status(400).json({ error: 'Invalid type filter' });
    query = query.eq('NotificationType', (q.type as string).trim());
  }

  const limit = Math.min(Math.max(Number(q.limit) || 50, 1), 200);
  const offset = Math.max(Number(q.offset) || 0, 0);
  query = query.range(offset, offset + limit - 1);

  const { data, error } = await query;
  if (error) return writeError(res, error, 'Database error');
  return res.json({ notifications: data, limit, offset });
}

//GET /api/notifications/unread-count — { count } of the caller's unread notifications. */
export async function unreadCount(req: Request, res: Response) {
  const { count, error } = await supabase
    .from(TABLE)
    .select('*', { count: 'exact', head: true })
    .eq('UserId', callerId(req))
    .eq('IsRead', false);
  if (error) return writeError(res, error, 'Database error');
  return res.json({ count: count ?? 0 });
}

//GET /api/notifications/:id — owner or admin. */
export async function getNotification(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid notification id' });

  const { data, error } = await supabase.from(TABLE).select('*').eq(PK, id).maybeSingle();
  if (error) return writeError(res, error, 'Database error');
  if (!data || (data.UserId !== callerId(req) && !isAdmin(req)))
    return res.status(404).json({ error: 'Notification not found' });
  return res.json({ notification: data });
}

/**
 * POST /api/notifications — admin only.
 * Body: exactly one of UserId (number) or UserIds (number[]); plus
 *       Title*, Message*, NotificationType*, RelatedEntityId? (string|number|null).
 */
export async function createNotification(req: Request, res: Response) {
  const b = req.body ?? {};

  const single = b.UserId !== undefined;
  const many = b.UserIds !== undefined;
  if (single === many)
    return res.status(400).json({ error: 'Provide exactly one of UserId or UserIds' });

  let userIds: number[];
  if (single) {
    if (!posInt(b.UserId)) return res.status(400).json({ error: 'UserId must be a positive integer' });
    userIds = [b.UserId];
  } else {
    if (!Array.isArray(b.UserIds) || b.UserIds.length === 0 || !b.UserIds.every(posInt))
      return res.status(400).json({ error: 'UserIds must be a non-empty array of positive integers' });
    userIds = [...new Set(b.UserIds as number[])];
  }

  if (!nonEmptyString(b.Title, 200)) return res.status(400).json({ error: 'Title is required (<=200 chars)' });
  if (!nonEmptyString(b.Message, 2000)) return res.status(400).json({ error: 'Message is required (<=2000 chars)' });
  if (!nonEmptyString(b.NotificationType, 64))
    return res.status(400).json({ error: 'NotificationType is required (<=64 chars)' });

  let relatedEntityId: string | null = null;
  if (b.RelatedEntityId !== undefined && b.RelatedEntityId !== null) {
    if (typeof b.RelatedEntityId !== 'string' && typeof b.RelatedEntityId !== 'number')
      return res.status(400).json({ error: 'RelatedEntityId must be a string, number, or null' });
    relatedEntityId = String(b.RelatedEntityId);
  }

  const rows = userIds.map((uid) => ({
    UserId: uid,
    Title: b.Title.trim(),
    Message: b.Message.trim(),
    NotificationType: b.NotificationType.trim(),
    RelatedEntityId: relatedEntityId,
  }));

  const { data, error } = await supabase.from(TABLE).insert(rows).select();
  if (error) return writeError(res, error, 'Could not create notification');

  if (single) return res.status(201).json({ notification: data?.[0] });
  return res.status(201).json({ notifications: data, count: data?.length ?? 0 });
}

//PATCH /api/notifications/:id — owner only. Body: { IsRead: boolean }. */
export async function updateNotification(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid notification id' });

  const { IsRead } = req.body ?? {};
  if (typeof IsRead !== 'boolean')
    return res.status(400).json({ error: 'IsRead (boolean) is required' });

  // Scope the update to the caller's own row so nobody can flip someone else's.
  const { data, error } = await supabase
    .from(TABLE)
    .update({ IsRead })
    .eq(PK, id)
    .eq('UserId', callerId(req))
    .select()
    .maybeSingle();
  if (error) return writeError(res, error, 'Could not update notification');
  if (!data) return res.status(404).json({ error: 'Notification not found' });
  return res.json({ notification: data });
}

//POST /api/notifications/read-all — mark all the caller's unread notifications as read. */
export async function markAllRead(req: Request, res: Response) {
  const { data, error } = await supabase
    .from(TABLE)
    .update({ IsRead: true })
    .eq('UserId', callerId(req))
    .eq('IsRead', false)
    .select('NotificationId');
  if (error) return writeError(res, error, 'Could not update notifications');
  return res.json({ updated: data?.length ?? 0 });
}

//DELETE /api/notifications/:id — owner or admin. */
export async function deleteNotification(req: Request, res: Response) {
  const id = parseId(req.params.id);
  if (id === null) return res.status(400).json({ error: 'Invalid notification id' });

  let del = supabase.from(TABLE).delete().eq(PK, id);
  if (!isAdmin(req)) del = del.eq('UserId', callerId(req));

  const { data, error } = await del.select('NotificationId').maybeSingle();
  if (error) return writeError(res, error, 'Could not delete notification');
  if (!data) return res.status(404).json({ error: 'Notification not found' });
  return res.json({ deleted: true, notificationId: id });
}
