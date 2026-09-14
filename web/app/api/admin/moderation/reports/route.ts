import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

export async function GET(request: NextRequest) {
  const accessToken = request.cookies.get('nagrivic_access_token')?.value;
  if (!accessToken) {
    return NextResponse.json({ message: 'Authentication required' }, { status: 401 });
  }

  const { searchParams } = new URL(request.url);

  try {
    const res = await fetch(`${BACKEND_URL}/api/moderation/reports?${searchParams.toString()}`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json({ message: err.message || 'Error fetching reports' }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json({ message: 'Unable to connect to Nagrivic services' }, { status: 500 });
  }
}
