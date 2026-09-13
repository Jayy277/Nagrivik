import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

export async function GET(request: NextRequest) {
  let accessToken = request.cookies.get('nagrivic_access_token')?.value;
  const refreshToken = request.cookies.get('nagrivic_refresh_token')?.value;
  const isProd = process.env.NODE_ENV === 'production';

  if (!accessToken && !refreshToken) {
    return NextResponse.json({ user: null });
  }

  let responseCookiesToSet: Array<{ name: string; value: string; options: any }> = [];

  // If no accessToken but refreshToken exists, attempt token refresh
  if (!accessToken && refreshToken) {
    try {
      const refreshRes = await fetch(`${BACKEND_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (refreshRes.ok) {
        const refreshData = await refreshRes.json();
        accessToken = refreshData.accessToken;

        responseCookiesToSet.push({
          name: 'nagrivic_access_token',
          value: refreshData.accessToken,
          options: {
            httpOnly: true,
            secure: isProd,
            sameSite: 'lax',
            path: '/',
            maxAge: refreshData.expiresIn || 900,
          },
        });

        responseCookiesToSet.push({
          name: 'nagrivic_refresh_token',
          value: refreshData.refreshToken,
          options: {
            httpOnly: true,
            secure: isProd,
            sameSite: 'lax',
            path: '/',
            maxAge: 30 * 24 * 60 * 60,
          },
        });

        responseCookiesToSet.push({
          name: 'nagrivic_user',
          value: JSON.stringify(refreshData.user),
          options: {
            httpOnly: false,
            secure: isProd,
            sameSite: 'lax',
            path: '/',
            maxAge: 30 * 24 * 60 * 60,
          },
        });
      }
    } catch {
      // Refresh failed
    }
  }

  if (!accessToken) {
    const res = NextResponse.json({ user: null });
    res.cookies.delete('nagrivic_access_token');
    res.cookies.delete('nagrivic_refresh_token');
    res.cookies.delete('nagrivic_user');
    return res;
  }

  try {
    const meRes = await fetch(`${BACKEND_URL}/api/auth/me`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (meRes.ok) {
      const user = await meRes.json();
      const res = NextResponse.json({ user });
      for (const cookie of responseCookiesToSet) {
        res.cookies.set(cookie.name, cookie.value, cookie.options);
      }
      return res;
    }

    // Access token expired, attempt refresh
    if (meRes.status === 401 && refreshToken) {
      const refreshRes = await fetch(`${BACKEND_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (refreshRes.ok) {
        const refreshData = await refreshRes.json();
        const res = NextResponse.json({ user: refreshData.user });

        res.cookies.set('nagrivic_access_token', refreshData.accessToken, {
          httpOnly: true,
          secure: isProd,
          sameSite: 'lax',
          path: '/',
          maxAge: refreshData.expiresIn || 900,
        });

        res.cookies.set('nagrivic_refresh_token', refreshData.refreshToken, {
          httpOnly: true,
          secure: isProd,
          sameSite: 'lax',
          path: '/',
          maxAge: 30 * 24 * 60 * 60,
        });

        res.cookies.set('nagrivic_user', JSON.stringify(refreshData.user), {
          httpOnly: false,
          secure: isProd,
          sameSite: 'lax',
          path: '/',
          maxAge: 30 * 24 * 60 * 60,
        });

        return res;
      }
    }

    // Unauthorized and could not refresh
    const res = NextResponse.json({ user: null });
    res.cookies.delete('nagrivic_access_token');
    res.cookies.delete('nagrivic_refresh_token');
    res.cookies.delete('nagrivic_user');
    return res;
  } catch (err) {
    return NextResponse.json({ user: null });
  }
}
