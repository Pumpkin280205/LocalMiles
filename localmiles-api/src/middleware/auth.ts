import type { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';
dotenv.config();

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error('JWT_SECRET is not set in the environment');
}

export const ROLE = {
  ADMIN: 'admin',
  DRIVER: 'driver',
  CLIENT: 'client',
} as const;

/** Shape of what we put on req.user — mirrors the payload signed in authController. */
export interface AuthUser {
  id: string;
  role: string;
}

declare global {
  namespace Express {
    interface Request {
      user?: AuthUser;
    }
  }
}

/**
 * Reads `Authorization: Bearer <token>`, verifies the JWT, and attaches the
 * decoded id/role to `req.user`. Rejects with 401 if anything is off.
 */
export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;

  if (!header?.startsWith('Bearer ')) {
    return res
      .status(401)
      .json({ error: 'Missing or malformed Authorization header' });
  }

  const token = header.slice('Bearer '.length).trim();

  try {
    const decoded = jwt.verify(token, JWT_SECRET!);

    if (typeof decoded === 'string' || !decoded.id || !decoded.role) {
      return res.status(401).json({ error: 'Invalid token payload' });
    }

    req.user = { id: String(decoded.id), role: String(decoded.role) };
    return next();
  } catch (err) {
    // Bad signature, expired token, malformed token, etc.
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}

export function requireRole(...allowed: string[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.user || !allowed.includes(req.user.role)) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    return next();
  };
}
