import {
  ModerationSummary,
  ModerationReport,
  ModerationReportDetail,
  ModerationFilterParams,
  ModerationActionType,
  ModerationTargetType,
} from '../../types/moderation';
import { PagedResponse } from '../../types/api';

export async function getModerationSummary(): Promise<ModerationSummary> {
  const res = await fetch('/api/admin/moderation/summary', { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('Failed to load moderation summary');
  }
  return res.json();
}

export async function getModerationReports(
  params: ModerationFilterParams = {}
): Promise<PagedResponse<ModerationReport>> {
  const query = new URLSearchParams();
  if (params.status && params.status !== 'ALL') query.set('status', params.status);
  if (params.targetType && params.targetType !== 'ALL') query.set('targetType', params.targetType);
  if (params.reason && params.reason !== 'ALL') query.set('reason', params.reason);
  if (params.page !== undefined) query.set('page', params.page.toString());
  if (params.size !== undefined) query.set('size', params.size.toString());
  if (params.sort) query.set('sort', params.sort);

  const res = await fetch(`/api/admin/moderation/reports?${query.toString()}`, { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('Failed to load moderation reports');
  }
  return res.json();
}

export async function getModerationReportDetail(
  reportId: string
): Promise<ModerationReportDetail> {
  const res = await fetch(`/api/admin/moderation/reports/${reportId}`, { cache: 'no-store' });
  if (res.status === 404) {
    throw new Error('Report not found');
  }
  if (!res.ok) {
    throw new Error('Failed to load report detail');
  }
  return res.json();
}

export async function reviewReport(reportId: string): Promise<void> {
  const res = await fetch(`/api/admin/moderation/reports/${reportId}/review`, {
    method: 'POST',
  });
  if (res.status === 409) {
    throw new Error('CONFLICT: This report has already been resolved or dismissed.');
  }
  if (!res.ok) {
    throw new Error('Failed to review report');
  }
}

export async function resolveReport(
  reportId: string,
  action: ModerationActionType,
  reason: string,
  notes?: string
): Promise<void> {
  const res = await fetch(`/api/admin/moderation/reports/${reportId}/resolve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action, reason, notes }),
  });
  if (res.status === 409) {
    throw new Error('CONFLICT: This report was already modified or resolved by another moderator.');
  }
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || 'Failed to resolve report');
  }
}

export async function dismissReport(
  reportId: string,
  reason?: string
): Promise<void> {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
  const res = await fetch(`/api/admin/moderation/reports/${reportId}/dismiss${query}`, {
    method: 'POST',
  });
  if (res.status === 409) {
    throw new Error('CONFLICT: This report was already modified or dismissed by another moderator.');
  }
  if (!res.ok) {
    throw new Error('Failed to dismiss report');
  }
}

export async function hideContent(
  targetType: ModerationTargetType,
  targetId: string,
  reason: string,
  notes?: string
): Promise<void> {
  const res = await fetch(`/api/admin/moderation/content/${targetType}/${targetId}/hide`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason, notes }),
  });
  if (!res.ok) {
    throw new Error('Failed to hide content');
  }
}

export async function restoreContent(
  targetType: ModerationTargetType,
  targetId: string,
  reason: string,
  notes?: string
): Promise<void> {
  const res = await fetch(`/api/admin/moderation/content/${targetType}/${targetId}/restore`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason, notes }),
  });
  if (!res.ok) {
    throw new Error('Failed to restore content');
  }
}

export async function restrictUser(
  userId: string,
  durationMinutes: number | null,
  reason: string
): Promise<void> {
  const res = await fetch(`/api/admin/moderation/users/${userId}/restrict`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ durationMinutes, reason }),
  });
  if (res.status === 403) {
    throw new Error('FORBIDDEN: Only administrators can restrict users.');
  }
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || 'Failed to restrict user');
  }
}
