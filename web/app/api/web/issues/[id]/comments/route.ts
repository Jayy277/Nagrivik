import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

export async function POST(
  request: NextRequest,
  context: { params: Promise<{ id: string }> }
) {
  const { id } = await context.params;
  const accessToken = request.cookies.get('nagrivic_access_token')?.value;

  if (!accessToken) {
    return NextResponse.json(
      { message: 'Authentication required to post comments.' },
      { status: 401 }
    );
  }

  try {
    const body = await request.json();
    const content = body?.content;

    if (!content || typeof content !== 'string' || !content.trim()) {
      return NextResponse.json(
        { message: 'Comment content must not be blank' },
        { status: 400 }
      );
    }

    const res = await fetch(`${BACKEND_URL}/api/issues/${id}/comments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ content: content.trim() }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { message: err.message || 'Unable to post comment.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data, { status: 201 });
  } catch {
    return NextResponse.json(
      { message: 'Unable to connect to Nagrivic services.' },
      { status: 500 }
    );
  }
}
