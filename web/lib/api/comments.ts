import { apiRequest } from './client';
import { CommentResponse } from '../../types/issue';
import { PagedResponse } from '../../types/api';

export async function getIssueComments(
  issueId: string,
  page = 0,
  size = 20,
  signal?: AbortSignal
): Promise<PagedResponse<CommentResponse>> {
  return apiRequest<PagedResponse<CommentResponse>>(
    `/issues/${issueId}/comments?page=${page}&size=${size}`,
    { signal }
  );
}
