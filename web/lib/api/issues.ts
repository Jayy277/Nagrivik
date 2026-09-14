import { apiRequest, getApiBaseUrl, RequestOptions } from './client';
import {
  IssueResponse,
  CategorySummary,
  IssueDiscoveryFilterParams,
  ResolutionEvidenceResponse,
} from '../../types/issue';
import { PagedResponse } from '../../types/api';

export async function getCategories(): Promise<CategorySummary[]> {
  try {
    return await apiRequest<CategorySummary[]>('/categories');
  } catch {
    return [
      { id: '1', name: 'Roads / Potholes', slug: 'roads-potholes' },
      { id: '2', name: 'Garbage', slug: 'garbage' },
      { id: '3', name: 'Streetlights', slug: 'streetlights' },
      { id: '4', name: 'Water', slug: 'water' },
      { id: '5', name: 'Drainage', slug: 'drainage' },
    ];
  }
}

export async function getIssues(
  params: IssueDiscoveryFilterParams = {},
  signal?: AbortSignal
): Promise<PagedResponse<IssueResponse>> {
  const queryParts: string[] = [];
  if (params.q && params.q.trim()) queryParts.push(`q=${encodeURIComponent(params.q.trim())}`);
  if (params.categoryId) queryParts.push(`categoryId=${encodeURIComponent(params.categoryId)}`);
  if (params.status) queryParts.push(`status=${encodeURIComponent(params.status)}`);
  if (params.priority) queryParts.push(`priority=${encodeURIComponent(params.priority)}`);
  if (params.cityId) queryParts.push(`cityId=${encodeURIComponent(params.cityId)}`);
  if (params.wardId) queryParts.push(`wardId=${encodeURIComponent(params.wardId)}`);
  if (params.civicBodyId) queryParts.push(`civicBodyId=${encodeURIComponent(params.civicBodyId)}`);
  if (params.departmentId) queryParts.push(`departmentId=${encodeURIComponent(params.departmentId)}`);
  if (params.latitude !== undefined && params.latitude !== null) queryParts.push(`latitude=${params.latitude}`);
  if (params.longitude !== undefined && params.longitude !== null) queryParts.push(`longitude=${params.longitude}`);
  if (params.radiusMeters !== undefined && params.radiusMeters !== null) queryParts.push(`radiusMeters=${params.radiusMeters}`);
  if (params.sort) queryParts.push(`sort=${encodeURIComponent(params.sort)}`);
  if (params.page !== undefined && params.page !== null) queryParts.push(`page=${params.page}`);
  if (params.size !== undefined && params.size !== null) queryParts.push(`size=${params.size}`);

  const queryString = queryParts.length > 0 ? `?${queryParts.join('&')}` : '';
  return apiRequest<PagedResponse<IssueResponse>>(`/issues${queryString}`, { signal });
}

export async function getIssueById(
  issueId: string,
  signal?: AbortSignal
): Promise<IssueResponse> {
  return apiRequest<IssueResponse>(`/issues/${issueId}`, { signal });
}

export function getIssueImageUrl(issueId: string, mediaId?: string): string | null {
  if (!mediaId) return null;
  const baseUrl = getApiBaseUrl();
  return `${baseUrl}/api/issues/${issueId}/media/${mediaId}`;
}

export async function getResolutionEvidence(
  issueId: string,
  options: RequestOptions = {}
): Promise<ResolutionEvidenceResponse[]> {
  return apiRequest<ResolutionEvidenceResponse[]>(`/issues/${issueId}/resolution-evidence`, options);
}
