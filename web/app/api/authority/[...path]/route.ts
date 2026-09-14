import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

const BACKEND_URL = process.env.API_BASE_URL || 'http://localhost:8080';

async function handleProxy(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const { path } = await params;
  const targetPath = (path || []).join('/');
  const accessToken = request.cookies.get('nagrivic_access_token')?.value;

  if (!accessToken) {
    return NextResponse.json({ message: 'Authentication required' }, { status: 401 });
  }

  const { searchParams } = new URL(request.url);
  const queryString = searchParams.toString() ? `?${searchParams.toString()}` : '';
  const url = `${BACKEND_URL}/api/authority/${targetPath}${queryString}`;

  const headers: Record<string, string> = {
    Authorization: `Bearer ${accessToken}`,
  };

  const contentType = request.headers.get('content-type');
  if (contentType) {
    headers['Content-Type'] = contentType;
  }

  let body: string | undefined;
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    try {
      body = await request.text();
    } catch {
      body = undefined;
    }
  }

  try {
    const res = await fetch(url, {
      method: request.method,
      headers,
      body: body || undefined,
    });

    if (res.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const data = await res.json().catch(() => null);
    return NextResponse.json(data ?? { status: res.status }, { status: res.status });
  } catch (error) {
    return NextResponse.json({ message: 'Unable to connect to Nagrivic authority services', error: String(error) }, { status: 500 });
  }
}

export async function GET(request: NextRequest, props: { params: Promise<{ path: string[] }> }) {
  return handleProxy(request, props);
}

export async function POST(request: NextRequest, props: { params: Promise<{ path: string[] }> }) {
  return handleProxy(request, props);
}

export async function PUT(request: NextRequest, props: { params: Promise<{ path: string[] }> }) {
  return handleProxy(request, props);
}

export async function PATCH(request: NextRequest, props: { params: Promise<{ path: string[] }> }) {
  return handleProxy(request, props);
}

export async function DELETE(request: NextRequest, props: { params: Promise<{ path: string[] }> }) {
  return handleProxy(request, props);
}
