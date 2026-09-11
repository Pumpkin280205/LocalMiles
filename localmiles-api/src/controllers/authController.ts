import type { Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { supabase } from '../supabaseClient.js';
import { verifyIdToken } from '../lib/verifyIdToken.js';

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error('JWT_SECRET is not set in the environment');
}

// Role a brand-new self-registered user is given.
const DEFAULT_ROLE = 'driver';

export async function loginHandler(req: Request, res: Response) {
  try {
    const { idToken } = req.body ?? {};
    if (!idToken) {
      return res.status(400).json({ error: 'Missing idToken in request body' });
    }

    // 1. Verify the ID token (Firebase and/or Google)
    //    and normalise it to a common identity shape.
    const { email, ssoProviderId, firstName, lastName } = await verifyIdToken(idToken);

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
