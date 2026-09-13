import { apiRequest } from './client';
import { ActivityResponse, StatusHistoryResponse } from '../../types/issue';
import { PagedResponse } from '../../types/api';

export async function getIssueActivity(
  issueId: string,
  page = 0,
  size = 30,
  signal?: AbortSignal
): Promise<PagedResponse<ActivityResponse>> {
  return apiRequest<PagedResponse<ActivityResponse>>(
    `/issues/${issueId}/activity?page=${page}&size=${size}`,
    { signal }
  );
}

export async function getIssueStatusHistory(
  issueId: string,
  signal?: AbortSignal
): Promise<StatusHistoryResponse> {
  return apiRequest<StatusHistoryResponse>(`/issues/${issueId}/status-history`, { signal });
}
