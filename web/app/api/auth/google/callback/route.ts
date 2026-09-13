import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const code = searchParams.get('code');
  const returnedState = searchParams.get('state');
  const errorParam = searchParams.get('error');

  const origin = request.nextUrl.origin;
  const isProd = process.env.NODE_ENV === 'production';

  // Read and clear state cookie
  const stateCookie = request.cookies.get('nagrivic_oauth_state')?.value;
  let targetUrl = '/';

  if (!stateCookie) {
    const errorUrl = new URL('/login', origin);
    errorUrl.searchParams.set('error', 'session_expired');
    return NextResponse.redirect(errorUrl.toString(), 302);
  }

  try {
    const parsed = JSON.parse(stateCookie);
    if (parsed.state !== returnedState) {
      const errorUrl = new URL('/login', origin);
      errorUrl.searchParams.set('error', 'invalid_state');
      return NextResponse.redirect(errorUrl.toString(), 302);
    }
    if (parsed.returnUrl) {
      targetUrl = parsed.returnUrl;
    }
  } catch {
    // Malformed state cookie
  }

  if (errorParam || !code) {
    const errorUrl = new URL('/login', origin);
    errorUrl.searchParams.set('error', errorParam || 'missing_code');
    return NextResponse.redirect(errorUrl.toString(), 302);
  }

  try {
    const clientId =
      process.env.GOOGLE_WEB_CLIENT_ID ||
      process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ||
      'nagrivic-dev-web-client-id';
    const clientSecret = process.env.GOOGLE_WEB_CLIENT_SECRET;
    const redirectUri = `${origin}/api/auth/google/callback`;

    // 1. Server-side token exchange with Google
    let idToken: string | null = null;

    if (clientSecret) {
      const tokenRes = await fetch('https://oauth2.googleapis.com/token', {
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

      if (!tokenRes.ok) {
        const errBody = await tokenRes.text();
        console.error('Google token exchange error:', errBody);
        const errorUrl = new URL('/login', origin);
        errorUrl.searchParams.set('error', 'token_exchange_failed');
        return NextResponse.redirect(errorUrl.toString(), 302);
      }

      const tokenData = await tokenRes.json();
      idToken = tokenData.id_token;
    } else {
      // In development environments without client secret, treat code as idToken if passed directly
      idToken = code;
    }

    if (!idToken) {
      const errorUrl = new URL('/login', origin);
      errorUrl.searchParams.set('error', 'missing_id_token');
      return NextResponse.redirect(errorUrl.toString(), 302);
    }

    // 2. Call Nagrivic backend POST /api/auth/google
    const backendRes = await fetch(`${BACKEND_URL}/api/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken }),
    });

    if (!backendRes.ok) {
      const errJson = await backendRes.json().catch(() => ({}));
      console.error('Nagrivic backend auth failed:', errJson);
      const errorUrl = new URL('/login', origin);
      errorUrl.searchParams.set('error', 'auth_failed');
      return NextResponse.redirect(errorUrl.toString(), 302);
    }

    const authData = await backendRes.json();

    // 3. Set secure HTTP-only cookies and redirect to target URL
    const destination = new URL(targetUrl, origin);
    const response = NextResponse.redirect(destination.toString(), 302);

    // Access token (15 mins)
    response.cookies.set('nagrivic_access_token', authData.accessToken, {
      httpOnly: true,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: authData.expiresIn || 900,
    });

    // Refresh token (30 days)
    response.cookies.set('nagrivic_refresh_token', authData.refreshToken, {
      httpOnly: true,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: 30 * 24 * 60 * 60,
    });

    // User profile summary for UI
    response.cookies.set('nagrivic_user', JSON.stringify(authData.user), {
      httpOnly: false,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: 30 * 24 * 60 * 60,
    });

    // Clear state cookie
    response.cookies.delete('nagrivic_oauth_state');

    return response;
  } catch (err: any) {
    console.error('OAuth callback processing error:', err);
    const errorUrl = new URL('/login', origin);
    errorUrl.searchParams.set('error', 'unexpected_error');
    return NextResponse.redirect(errorUrl.toString(), 302);
  }
}
