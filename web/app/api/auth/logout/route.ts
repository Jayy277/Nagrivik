import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

export async function POST(request: NextRequest) {
  const refreshToken = request.cookies.get('nagrivic_refresh_token')?.value;

  if (refreshToken) {
    try {
      await fetch(`${BACKEND_URL}/api/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
    } catch {
      // Proceed with cookie clearance regardless of network/backend response
    }
  }

  const response = NextResponse.json({ message: 'Logged out successfully' });
  response.cookies.delete('nagrivic_access_token');
  response.cookies.delete('nagrivic_refresh_token');
  response.cookies.delete('nagrivic_user');
  response.cookies.delete('nagrivic_oauth_state');

  return response;
}
