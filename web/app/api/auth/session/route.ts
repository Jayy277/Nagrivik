import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const idToken = body?.idToken;

    if (!idToken || typeof idToken !== 'string' || !idToken.trim()) {
      return NextResponse.json(
        { message: 'Google ID token must not be blank' },
        { status: 400 }
      );
    }

    // Call Nagrivic backend
    const backendRes = await fetch(`${BACKEND_URL}/api/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken: idToken.trim() }),
    });

    if (!backendRes.ok) {
      const err = await backendRes.json().catch(() => ({}));
      return NextResponse.json(
        { message: err.message || 'Unable to verify Google credential' },
        { status: backendRes.status }
      );
    }

    const authData = await backendRes.json();
    const isProd = process.env.NODE_ENV === 'production';

    const response = NextResponse.json({
      user: authData.user,
      expiresIn: authData.expiresIn,
    });

    // Set secure HTTP-only cookies
    response.cookies.set('nagrivic_access_token', authData.accessToken, {
      httpOnly: true,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: authData.expiresIn || 900,
    });

    response.cookies.set('nagrivic_refresh_token', authData.refreshToken, {
      httpOnly: true,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: 30 * 24 * 60 * 60,
    });

    response.cookies.set('nagrivic_user', JSON.stringify(authData.user), {
      httpOnly: false,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: 30 * 24 * 60 * 60,
    });

    return response;
  } catch (err: any) {
    return NextResponse.json(
      { message: err.message || 'Internal authentication error' },
      { status: 500 }
    );
  }
}
