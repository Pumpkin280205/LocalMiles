import type { Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { supabase } from '../supabaseClient.js';
import { verifyIdToken } from '../lib/verifyIdToken.js';
import { ROLE } from '../middleware/auth.js';

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error('JWT_SECRET is not set in the environment');
}

// Role a brand-new self-registered user is given. Least-privileged on purpose —
// promotion to driver/admin happens via PATCH /api/users/:id/role (admin only),
// never automatically at login.
const DEFAULT_ROLE = 'client';

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

      // New self-registered users default to the client role — give them a
      // linked Client row immediately so a dispatcher can assign jobs to
      // them right away, without a separate manual POST /api/clients step.
      if (user.Role === ROLE.CLIENT) {
        const { error: clientInsertError } = await supabase.from('Client').insert({
          UserId: user.UserId,
          CompanyName: `${user.FirstName ?? ''} ${user.LastName ?? ''}`.trim() || user.Email,
          ContactPerson: `${user.FirstName ?? ''} ${user.LastName ?? ''}`.trim() || user.Email,
          Email: user.Email,
          Address: 'Not provided',
        });
        if (clientInsertError) {
          console.error('Supabase insert failed (Client row):', clientInsertError);
        }
      }
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

const SWITCHABLE_ROLES: string[] = Object.values(ROLE);

// Placeholder Driver/Client fields used only when self-switching into a role
// that has no row yet. A real admin-created Driver/Client (via POST
// /api/drivers or /api/clients) always overrides these once it exists.
const DEV_PHONE = '000-000-0000';
const DEV_LICENCE = 'DEV-LICENCE';

/**
 * PATCH /api/auth/switch-role — DEV/TEST ONLY. Lets an already-authenticated
 * user change their own Role with no admin approval, so one account can be
 * flipped between client/driver/admin while testing the three role-specific
 * flows. Disabled outside development (404s in production) because it
 * bypasses the RBAC model documented for the real app: promotion is normally
 * admin-only via PATCH /api/users/:id/role (see authController's
 * DEFAULT_ROLE comment and userController.updateUserRole).
 */
export async function switchRoleHandler(req: Request, res: Response) {
  if (process.env.NODE_ENV === 'production') {
    return res.status(404).json({ error: 'Not found' });
  }

  try {
    const role = String(req.body?.role ?? '').toLowerCase();
    if (!SWITCHABLE_ROLES.includes(role)) {
      return res.status(400).json({ error: `role must be one of: ${SWITCHABLE_ROLES.join(', ')}` });
    }

    const userId = Number(req.user!.id);

    const { data: user, error: userError } = await supabase
      .from('User')
      .update({ Role: role })
      .eq('UserId', userId)
      .select()
      .maybeSingle();

    if (userError) {
      console.error('Supabase update failed:', userError);
      return res.status(500).json({ error: 'Database error' });
    }
    if (!user) return res.status(404).json({ error: 'User not found' });

    // Ensure the role-specific row exists so the new role's screens have
    // something to read (a driver with no Driver row, or a client with no
    // Client row, would otherwise 404 on every subsequent call).
    if (role === ROLE.DRIVER) {
      const { data: existingDriver } = await supabase
        .from('Driver')
        .select('DriverId')
        .eq('UserId', userId)
        .maybeSingle();

      if (!existingDriver) {
        const { data: depot } = await supabase
          .from('Depot')
          .select('DepotId')
          .eq('IsActive', true)
          .limit(1)
          .maybeSingle();

        if (!depot) {
          return res.status(409).json({
            error: 'No active Depot exists yet — create one (POST /api/depots) before switching into the driver role.',
          });
        }

        const { error: driverInsertError } = await supabase.from('Driver').insert({
          UserId: userId,
          DepotId: depot.DepotId,
          EmployeeNumber: `DEV-${userId}`,
          PhoneNumber: DEV_PHONE,
          LicenceNumber: DEV_LICENCE,
          IsAvailable: true,
          IsActive: true,
        });
        if (driverInsertError) {
          console.error('Supabase insert failed:', driverInsertError);
          return res.status(500).json({ error: 'Could not create Driver row' });
        }
      }
    } else if (role === ROLE.CLIENT) {
      const { data: existingClient } = await supabase
        .from('Client')
        .select('ClientId')
        .eq('UserId', userId)
        .maybeSingle();

      if (!existingClient) {
        const { error: clientInsertError } = await supabase.from('Client').insert({
          UserId: userId,
          CompanyName: `${user.FirstName ?? 'Dev'} ${user.LastName ?? 'Client'}`.trim(),
          ContactPerson: `${user.FirstName ?? ''} ${user.LastName ?? ''}`.trim() || 'Dev Client',
          Email: user.Email,
          Address: '1 Dev Street',
        });
        if (clientInsertError) {
          console.error('Supabase insert failed:', clientInsertError);
          return res.status(500).json({ error: 'Could not create Client row' });
        }
      }
    }

    // The role lives inside the JWT payload, so a new token has to be minted
    // for the change to actually take effect on the next authenticated call.
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
    console.error('Role switch failed:', err);
    return res.status(500).json({ error: 'Role switch failed' });
  }
}
