import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const accessToken = request.cookies.get('nagrivic_access_token')?.value;
  if (!accessToken) {
    return NextResponse.json({ message: 'Authentication required' }, { status: 401 });
  }

  const { id } = await params;

  try {
    const res = await fetch(`${BACKEND_URL}/api/admin/duplicates/suggestions/${id}/link`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json({ message: err.message || 'Error linking duplicate suggestion' }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json({ message: 'Unable to connect to Nagrivic services' }, { status: 500 });
  }
}
