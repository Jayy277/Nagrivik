import { apiRequest, RequestOptions } from './client';
import {
  AuthorityDashboardMetrics,
  AuthorityIssueFilter,
  AuthorityIssueItemResponse,
  AuthorityIssueDetailResponse,
  ChangeAuthorityStatusRequest,
  ResolutionEvidenceResponse,
} from '../../types/authority';
import { IssueResponse } from '../../types/issue';
import { ApiClientError } from '../../types/api';

export interface AuthorityPaginatedIssues {
  content: AuthorityIssueItemResponse[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export class ConcurrencyConflictError extends Error {
  constructor(message: string = 'The issue was modified by another authority user. Please refresh to see the latest version.') {
    super(message);
    this.name = 'ConcurrencyConflictError';
  }
}

export async function getAuthorityDashboard(options: RequestOptions = {}): Promise<AuthorityDashboardMetrics> {
  return apiRequest<AuthorityDashboardMetrics>('/authority/dashboard', options);
}

export async function getAuthorityIssues(
  filter: AuthorityIssueFilter = {},
  options: RequestOptions = {}
): Promise<AuthorityPaginatedIssues> {
  const query = new URLSearchParams();
  if (filter.status) query.set('status', filter.status);
  if (filter.priority) query.set('priority', filter.priority);
  if (filter.wardId) query.set('wardId', filter.wardId);
  if (filter.departmentId) query.set('departmentId', filter.departmentId);
  if (filter.actionableOnly) query.set('actionableOnly', 'true');
  if (filter.page !== undefined) query.set('page', filter.page.toString());
  if (filter.size !== undefined) query.set('size', filter.size.toString());

  const queryString = query.toString();
  const endpoint = queryString ? `/authority/issues?${queryString}` : '/authority/issues';
  return apiRequest<AuthorityPaginatedIssues>(endpoint, options);
}

export async function getAuthorityIssueDetail(
  issueId: string,
  options: RequestOptions = {}
): Promise<AuthorityIssueDetailResponse> {
  return apiRequest<AuthorityIssueDetailResponse>(`/authority/issues/${issueId}`, options);
}

export async function changeAuthorityIssueStatus(
  issueId: string,
  request: ChangeAuthorityStatusRequest,
  options: RequestOptions = {}
): Promise<IssueResponse> {
  try {
    return await apiRequest<IssueResponse>(`/authority/issues/${issueId}/status`, {
      ...options,
      method: 'POST',
      body: JSON.stringify(request),
    });
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 409) {
      throw new ConcurrencyConflictError(error.message);
    }
    throw error;
  }
}

export async function uploadResolutionEvidence(
  issueId: string,
  formData: FormData,
  options: RequestOptions = {}
): Promise<ResolutionEvidenceResponse> {
  return apiRequest<ResolutionEvidenceResponse>(`/authority/issues/${issueId}/resolution-evidence`, {
    ...options,
    method: 'POST',
    body: formData,
  });
}
