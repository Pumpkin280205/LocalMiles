import type { Request, Response } from 'express';
import { OAuth2Client } from 'google-auth-library';
import jwt from 'jsonwebtoken';
import { supabase } from '../supabaseClient.js';

const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID;
if (!GOOGLE_CLIENT_ID) {
  throw new Error('GOOGLE_CLIENT_ID is not set in the environment');
}
const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error('JWT_SECRET is not set in the environment');
}

const googleClient = new OAuth2Client(GOOGLE_CLIENT_ID);

// Role a brand-new self-registered Google user is given.
const DEFAULT_ROLE = 'driver';

export async function loginHandler(req: Request, res: Response) {
  try {
    const { idToken } = req.body ?? {};
    if (!idToken) {
      return res.status(400).json({ error: 'Missing idToken in request body' });
    }

    // 1. Verify the Google ID token is genuine and issued for our client.
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: GOOGLE_CLIENT_ID!, // guaranteed non-null by the check above
    });

    // 2. Pull the identity out of the verified payload.
    const payload = ticket.getPayload();
    if (!payload?.email || !payload.sub) {
      return res.status(401).json({ error: 'Google token is missing email or sub' });
    }
    const email = payload.email;
    const ssoProviderId = payload.sub;
    const firstName = payload.given_name ?? payload.name ?? '';
    const lastName = payload.family_name ?? '';

    // 3. Look for an existing User row with this email.
    const { data: existingUser, error: selectError } = await supabase
      .from('User')
      .select()
      .eq('Email', email)
      .maybeSingle();

    if (selectError) {
      console.error('Supabase select failed:', selectError);
      return res.status(500).json({ error: 'Database error' });
    }

    let user = existingUser;

    if (user) {
      // Best-effort: record the login time. Don't fail the request if it errors.
      await supabase
        .from('User')
        .update({ LastLoginAt: new Date().toISOString() })
        .eq('UserId', user.UserId);
    } else {
      // 4. Create the row if this is a first-time login.
      const { data: createdUser, error: insertError } = await supabase
        .from('User')
        .insert({
          SsoProviderId: ssoProviderId,
          Email: email,
          FirstName: firstName,
          LastName: lastName,
          Role: DEFAULT_ROLE,
        })
        .select()
        .single();

      if (insertError) {
        console.error('Supabase insert failed:', insertError);
        return res.status(500).json({ error: 'Could not create user' });
      }
      user = createdUser;
    }

    // 5. Sign our own session JWT for the client to send on later requests.
    const token = jwt.sign(
      { id: user.UserId, role: user.Role },
      JWT_SECRET!,
      { expiresIn: '7d' }
    );

    return res.json({
      token,
      user: {
        id: user.UserId,
        email: user.Email,
        firstName: user.FirstName,
        lastName: user.LastName,
        role: user.Role,
      },
    });
  } catch (err) {
    // verifyIdToken throws on a forged / expired / wrong-audience token.
    console.error('Login failed:', err);
    return res.status(401).json({ error: 'Authentication failed' });
  }
}
