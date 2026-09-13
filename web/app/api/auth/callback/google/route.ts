import { NextRequest, NextResponse } from 'next/server';
import { getApiBaseUrl } from '@/lib/api/client';

export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const error = searchParams.get('error');
  const code = searchParams.get('code');
  const rawState = searchParams.get('state');

  let returnTo = '/issues';
  if (rawState) {
    try {
      const parsed = JSON.parse(Buffer.from(rawState, 'base64url').toString('utf8'));
      if (parsed.returnTo && typeof parsed.returnTo === 'string' && parsed.returnTo.startsWith('/')) {
        returnTo = parsed.returnTo;
      }
    } catch {
      // Ignore invalid state decode
    }
  }

  const host = request.headers.get('host') || 'localhost:3000';
  const proto = request.headers.get('x-forwarded-proto') || 'http';
  const siteUrl = process.env.NEXT_PUBLIC_SITE_URL || `${proto}://${host}`;

  if (error || !code) {
    return NextResponse.redirect(`${siteUrl}/login?error=cancelled`);
  }

  const clientId =
    process.env.GOOGLE_WEB_CLIENT_ID ||
    process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ||
    'nagrivic-dev-web-client-id';
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET || '';
  const redirectUri = `${siteUrl}/api/auth/callback/google`;

  let idToken: string | null = null;

  try {
    // 1. Exchange authorization code with Google OAuth token endpoint
    const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        code,
        client_id: clientId,
        client_secret: clientSecret,
        redirect_uri: redirectUri,
        grant_type: 'authorization_code',
      }),
    });

    if (!tokenResponse.ok) {
      console.error('Google token exchange failed:', await tokenResponse.text());
      return NextResponse.redirect(`${siteUrl}/login?error=google_failed`);
    }

    const tokenData = await tokenResponse.json();
    idToken = tokenData.id_token;
  } catch (err) {
    console.error('Network error during Google token exchange:', err);
    return NextResponse.redirect(`${siteUrl}/login?error=google_network_failed`);
  }

  if (!idToken) {
    return NextResponse.redirect(`${siteUrl}/login?error=missing_id_token`);
  }

  // 2. Independently verify Google credential with Nagrivic backend
  const backendBaseUrl = getApiBaseUrl();
  try {
    const backendRes = await fetch(`${backendBaseUrl}/api/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken }),
    });

    if (!backendRes.ok) {
      const errText = await backendRes.text();
      console.error('Backend Google login verification failed:', backendRes.status, errText);
      return NextResponse.redirect(`${siteUrl}/login?error=backend_failed`);
    }

    const authData = await backendRes.json();

    // 3. Set secure HTTP-only cookies
    const response = NextResponse.redirect(`${siteUrl}${returnTo}`);
    const isProduction = process.env.NODE_ENV === 'production';

    response.cookies.set('nagrivic_access_token', authData.accessToken, {
      httpOnly: true,
      secure: isProduction,
      sameSite: 'lax',
      path: '/',
      maxAge: authData.expiresIn || 900,
    });

    response.cookies.set('nagrivic_refresh_token', authData.refreshToken, {
      httpOnly: true,
      secure: isProduction,
      sameSite: 'lax',
      path: '/',
      maxAge: 30 * 24 * 60 * 60, // 30 days
    });

    // Safe profile cookie for client UI
    response.cookies.set('nagrivic_user', JSON.stringify(authData.user), {
      httpOnly: false,
      secure: isProduction,
      sameSite: 'lax',
      path: '/',
      maxAge: 30 * 24 * 60 * 60,
    });

    return response;
  } catch (err) {
    console.error('Failed to communicate with Nagrivic backend:', err);
    return NextResponse.redirect(`${siteUrl}/login?error=backend_network_failed`);
  }
}
