import {
  AiDuplicateSuggestion,
  DuplicateSuggestionsFilter,
} from '../../types/duplicate';
import { PagedResponse } from '../../types/api';

export async function getDuplicateSuggestions(
  params: DuplicateSuggestionsFilter = {}
): Promise<PagedResponse<AiDuplicateSuggestion>> {
  const query = new URLSearchParams();
  if (params.status) query.set('status', params.status);
  if (params.confidence) query.set('confidence', params.confidence);
  if (params.minScore !== undefined) query.set('minScore', params.minScore.toString());
  if (params.page !== undefined) query.set('page', params.page.toString());
  if (params.size !== undefined) query.set('size', params.size.toString());

  const res = await fetch(`/api/admin/duplicates/suggestions?${query.toString()}`, { cache: 'no-store' });
  if (!res.ok) {
    throw new Error('Failed to load duplicate suggestions');
  }
  return res.json();
}

export async function linkDuplicateSuggestion(suggestionId: string): Promise<void> {
  const res = await fetch(`/api/admin/duplicates/suggestions/${suggestionId}/link`, {
    method: 'POST',
  });
  if (res.status === 409) {
    throw new Error('CONFLICT: This suggestion or issue has already been linked or modified.');
  }
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || 'Failed to link duplicate suggestion');
  }
}

export async function dismissDuplicateSuggestion(
  suggestionId: string,
  reason?: string
): Promise<AiDuplicateSuggestion> {
  const res = await fetch(`/api/admin/duplicates/suggestions/${suggestionId}/dismiss`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason: reason || null }),
  });
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || 'Failed to dismiss duplicate suggestion');
  }
  return res.json();
}

export async function scanIssueForDuplicates(issueId: string): Promise<AiDuplicateSuggestion[]> {
  const res = await fetch(`/api/admin/duplicates/scan/${issueId}`, {
    method: 'POST',
  });
  if (!res.ok) {
    throw new Error('Failed to scan issue for duplicates');
  }
  return res.json();
}
