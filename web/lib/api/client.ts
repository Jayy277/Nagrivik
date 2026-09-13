import { ApiClientError } from '../../types/api';

const DEFAULT_API_BASE_URL = 'http://localhost:8080';

export function getApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    // Server-side
    return (
      process.env.API_BASE_URL ||
      process.env.NEXT_PUBLIC_API_BASE_URL ||
      DEFAULT_API_BASE_URL
    ).replace(/\/+$/, '');
  }
  // Client-side
  return (
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    DEFAULT_API_BASE_URL
  ).replace(/\/+$/, '');
}

export interface RequestOptions extends RequestInit {
  timeoutMs?: number;
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const baseUrl = getApiBaseUrl();
  let path = endpoint;
  if (!path.startsWith('http')) {
    if (!path.startsWith('/')) path = `/${path}`;
    if (!path.startsWith('/api/')) path = `/api${path}`;
  }
  const url = path.startsWith('http') ? path : `${baseUrl}${path}`;

  const timeoutMs = options.timeoutMs || 15000;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  if (options.signal) {
    if (options.signal.aborted) {
      controller.abort();
    } else {
      options.signal.addEventListener('abort', () => controller.abort(), { once: true });
    }
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...(options.headers as Record<string, string>),
  };

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (response.status === 204) {
      return undefined as unknown as T;
    }

    const contentType = response.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');
    const data = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      let userMessage = 'Unable to complete request. Please try again.';
      if (response.status === 400) {
        userMessage = (typeof data === 'object' && data?.message) || 'Please check your search or filter criteria.';
      } else if (response.status === 401) {
        userMessage = 'Authentication required.';
      } else if (response.status === 403) {
        userMessage = 'You do not have permission to perform this action.';
      } else if (response.status === 404) {
        userMessage = (typeof data === 'object' && data?.message) || 'The requested civic issue was not found.';
      } else if (response.status === 429) {
        userMessage = 'Too many requests. Please wait a moment and try again.';
      } else if (response.status >= 500) {
        userMessage = 'Nagrivic service is temporarily unavailable. Please try again shortly.';
      }

      const code = typeof data === 'object' ? data?.code : undefined;
      const fieldErrors = typeof data === 'object' ? data?.errors : undefined;

      throw new ApiClientError(response.status, userMessage, code, fieldErrors);
    }

    return data as T;
  } catch (error: unknown) {
    clearTimeout(timeoutId);
    if (error instanceof ApiClientError) {
      throw error;
    }
    if (error instanceof Error && error.name === 'AbortError') {
      if (options.signal?.aborted) {
        throw new ApiClientError(0, 'Request cancelled.', 'ABORTED');
      }
      throw new ApiClientError(408, 'Connection timed out. Please check your internet connection.');
    }
    throw new ApiClientError(0, 'Unable to connect to Nagrivic. Check your internet connection.');
  }
}
