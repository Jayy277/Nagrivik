import {
  CivicGeographyOverview,
  CivicBodyResponse,
  CreateCivicBodyRequest,
  UpdateCivicBodyRequest,
  CityResponse,
  CreateCityRequest,
  UpdateCityRequest,
  WardResponse,
  CreateWardRequest,
  UpdateWardRequest,
  DepartmentResponse,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
  CategoryDepartmentMappingResponse,
  CreateCategoryDepartmentMappingRequest,
  UpdateCategoryDepartmentMappingRequest,
  WardDepartmentMappingResponse,
  CreateWardDepartmentMappingRequest,
  UpdateWardDepartmentMappingRequest,
  BoundaryValidationSummary,
  ReResolveRequest,
  ReResolveResponse,
  CivicGeographyAuditResponse,
} from '../../types/geography';
import { PagedResponse } from '../../types/api';

export class ConcurrencyConflictError extends Error {
  constructor(message = 'The entity was modified by another administrator. Please refresh and try again.') {
    super(message);
    this.name = 'ConcurrencyConflictError';
  }
}

async function handleResponse<T>(res: Response, notFoundMsg = 'Resource not found'): Promise<T> {
  if (res.status === 409) {
    const errorData = await res.json().catch(() => ({}));
    throw new ConcurrencyConflictError(errorData.message || 'CONFLICT: Stale version detected. The record has been modified by another administrator.');
  }
  if (res.status === 404) {
    throw new Error(notFoundMsg);
  }
  if (res.status === 403) {
    throw new Error('FORBIDDEN: You do not have permission to manage civic geography.');
  }
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || `Request failed with status ${res.status}`);
  }
  if (res.status === 204) {
    return null as unknown as T;
  }
  return res.json();
}

// Overview
export async function getGeographyOverview(): Promise<CivicGeographyOverview> {
  const res = await fetch('/api/admin/geography/overview', { cache: 'no-store' });
  return handleResponse<CivicGeographyOverview>(res, 'Overview unavailable');
}

// Civic Bodies
export async function getCivicBodies(activeOnly = false): Promise<CivicBodyResponse[]> {
  const res = await fetch(`/api/admin/geography/civic-bodies?activeOnly=${activeOnly}`, { cache: 'no-store' });
  return handleResponse<CivicBodyResponse[]>(res);
}

export async function getCivicBody(id: string): Promise<CivicBodyResponse> {
  const res = await fetch(`/api/admin/geography/civic-bodies/${id}`, { cache: 'no-store' });
  return handleResponse<CivicBodyResponse>(res, 'Civic body not found');
}

export async function createCivicBody(req: CreateCivicBodyRequest): Promise<CivicBodyResponse> {
  const res = await fetch('/api/admin/geography/civic-bodies', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<CivicBodyResponse>(res);
}

export async function updateCivicBody(id: string, req: UpdateCivicBodyRequest): Promise<CivicBodyResponse> {
  const res = await fetch(`/api/admin/geography/civic-bodies/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<CivicBodyResponse>(res);
}

export async function toggleCivicBodyActive(id: string, active: boolean, version: number): Promise<CivicBodyResponse> {
  const res = await fetch(`/api/admin/geography/civic-bodies/${id}/active?active=${active}&version=${version}`, {
    method: 'PATCH',
  });
  return handleResponse<CivicBodyResponse>(res);
}

// Cities
export async function getCities(activeOnly = false): Promise<CityResponse[]> {
  const res = await fetch(`/api/admin/geography/cities?activeOnly=${activeOnly}`, { cache: 'no-store' });
  return handleResponse<CityResponse[]>(res);
}

export async function getCity(id: string): Promise<CityResponse> {
  const res = await fetch(`/api/admin/geography/cities/${id}`, { cache: 'no-store' });
  return handleResponse<CityResponse>(res, 'City not found');
}

export async function createCity(req: CreateCityRequest): Promise<CityResponse> {
  const res = await fetch('/api/admin/geography/cities', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<CityResponse>(res);
}

export async function updateCity(id: string, req: UpdateCityRequest): Promise<CityResponse> {
  const res = await fetch(`/api/admin/geography/cities/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<CityResponse>(res);
}

export async function toggleCityActive(id: string, active: boolean, version: number): Promise<CityResponse> {
  const res = await fetch(`/api/admin/geography/cities/${id}/active?active=${active}&version=${version}`, {
    method: 'PATCH',
  });
  return handleResponse<CityResponse>(res);
}

export async function deleteCity(id: string, version?: number): Promise<void> {
  const query = version !== undefined ? `?version=${version}` : '';
  const res = await fetch(`/api/admin/geography/cities/${id}${query}`, {
    method: 'DELETE',
  });
  return handleResponse<void>(res);
}

// Wards
export async function getWards(params: {
  query?: string;
  cityId?: string;
  active?: boolean;
  page?: number;
  size?: number;
} = {}): Promise<PagedResponse<WardResponse>> {
  const searchParams = new URLSearchParams();
  if (params.query) searchParams.set('query', params.query);
  if (params.cityId) searchParams.set('cityId', params.cityId);
  if (params.active !== undefined) searchParams.set('active', params.active.toString());
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const res = await fetch(`/api/admin/geography/wards?${searchParams.toString()}`, { cache: 'no-store' });
  return handleResponse<PagedResponse<WardResponse>>(res);
}

export async function getWard(id: string): Promise<WardResponse> {
  const res = await fetch(`/api/admin/geography/wards/${id}`, { cache: 'no-store' });
  return handleResponse<WardResponse>(res, 'Ward not found');
}

export async function createWard(req: CreateWardRequest): Promise<WardResponse> {
  const res = await fetch('/api/admin/geography/wards', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<WardResponse>(res);
}

export async function updateWard(id: string, req: UpdateWardRequest): Promise<WardResponse> {
  const res = await fetch(`/api/admin/geography/wards/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<WardResponse>(res);
}

export async function toggleWardActive(id: string, active: boolean, version: number): Promise<WardResponse> {
  const res = await fetch(`/api/admin/geography/wards/${id}/active?active=${active}&version=${version}`, {
    method: 'PATCH',
  });
  return handleResponse<WardResponse>(res);
}

// Departments
export async function getDepartments(params: {
  civicBodyId?: string;
  active?: boolean;
  page?: number;
  size?: number;
} = {}): Promise<PagedResponse<DepartmentResponse>> {
  const searchParams = new URLSearchParams();
  if (params.civicBodyId) searchParams.set('civicBodyId', params.civicBodyId);
  if (params.active !== undefined) searchParams.set('active', params.active.toString());
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const res = await fetch(`/api/admin/geography/departments?${searchParams.toString()}`, { cache: 'no-store' });
  return handleResponse<PagedResponse<DepartmentResponse>>(res);
}

export async function getDepartment(id: string): Promise<DepartmentResponse> {
  const res = await fetch(`/api/admin/geography/departments/${id}`, { cache: 'no-store' });
  return handleResponse<DepartmentResponse>(res, 'Department not found');
}

export async function createDepartment(req: CreateDepartmentRequest): Promise<DepartmentResponse> {
  const res = await fetch('/api/admin/geography/departments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<DepartmentResponse>(res);
}

export async function updateDepartment(id: string, req: UpdateDepartmentRequest): Promise<DepartmentResponse> {
  const res = await fetch(`/api/admin/geography/departments/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<DepartmentResponse>(res);
}

export async function toggleDepartmentActive(id: string, active: boolean, version: number): Promise<DepartmentResponse> {
  const res = await fetch(`/api/admin/geography/departments/${id}/active?active=${active}&version=${version}`, {
    method: 'PATCH',
  });
  return handleResponse<DepartmentResponse>(res);
}

// Category mappings
export async function getCategoryMappings(params: {
  civicBodyId?: string;
  category?: string;
  active?: boolean;
  page?: number;
  size?: number;
} = {}): Promise<PagedResponse<CategoryDepartmentMappingResponse>> {
  const searchParams = new URLSearchParams();
  if (params.civicBodyId) searchParams.set('civicBodyId', params.civicBodyId);
  if (params.category) searchParams.set('category', params.category);
  if (params.active !== undefined) searchParams.set('active', params.active.toString());
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const res = await fetch(`/api/admin/geography/category-department-mappings?${searchParams.toString()}`, { cache: 'no-store' });
  return handleResponse<PagedResponse<CategoryDepartmentMappingResponse>>(res);
}

export async function createCategoryMapping(req: CreateCategoryDepartmentMappingRequest): Promise<CategoryDepartmentMappingResponse> {
  const res = await fetch('/api/admin/geography/category-department-mappings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<CategoryDepartmentMappingResponse>(res);
}

export async function updateCategoryMapping(id: string, req: UpdateCategoryDepartmentMappingRequest): Promise<CategoryDepartmentMappingResponse> {
  const res = await fetch(`/api/admin/geography/category-department-mappings/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<CategoryDepartmentMappingResponse>(res);
}

export async function toggleCategoryMappingActive(id: string, active: boolean, version: number): Promise<CategoryDepartmentMappingResponse> {
  const res = await fetch(`/api/admin/geography/category-department-mappings/${id}/active?active=${active}&version=${version}`, {
    method: 'PATCH',
  });
  return handleResponse<CategoryDepartmentMappingResponse>(res);
}

// Ward mappings
export async function getWardMappings(params: {
  wardId?: string;
  departmentId?: string;
  active?: boolean;
  page?: number;
  size?: number;
} = {}): Promise<PagedResponse<WardDepartmentMappingResponse>> {
  const searchParams = new URLSearchParams();
  if (params.wardId) searchParams.set('wardId', params.wardId);
  if (params.departmentId) searchParams.set('departmentId', params.departmentId);
  if (params.active !== undefined) searchParams.set('active', params.active.toString());
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const res = await fetch(`/api/admin/geography/ward-department-mappings?${searchParams.toString()}`, { cache: 'no-store' });
  return handleResponse<PagedResponse<WardDepartmentMappingResponse>>(res);
}

export async function createWardMapping(req: CreateWardDepartmentMappingRequest): Promise<WardDepartmentMappingResponse> {
  const res = await fetch('/api/admin/geography/ward-department-mappings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<WardDepartmentMappingResponse>(res);
}

export async function updateWardMapping(id: string, req: UpdateWardDepartmentMappingRequest): Promise<WardDepartmentMappingResponse> {
  const res = await fetch(`/api/admin/geography/ward-department-mappings/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<WardDepartmentMappingResponse>(res);
}

export async function toggleWardMappingActive(id: string, active: boolean, version: number): Promise<WardDepartmentMappingResponse> {
  const res = await fetch(`/api/admin/geography/ward-department-mappings/${id}/active?active=${active}&version=${version}`, {
    method: 'PATCH',
  });
  return handleResponse<WardDepartmentMappingResponse>(res);
}

// Validation & Re-resolution
export async function validateWardBoundaries(): Promise<BoundaryValidationSummary> {
  const res = await fetch('/api/admin/geography/wards/validate', {
    method: 'POST',
  });
  return handleResponse<BoundaryValidationSummary>(res);
}

export async function triggerReResolution(req: ReResolveRequest): Promise<ReResolveResponse> {
  const res = await fetch('/api/admin/geography/re-resolve', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  return handleResponse<ReResolveResponse>(res);
}

// Audits
export async function getAudits(params: {
  entityType?: string;
  page?: number;
  size?: number;
} = {}): Promise<PagedResponse<CivicGeographyAuditResponse>> {
  const searchParams = new URLSearchParams();
  if (params.entityType) searchParams.set('entityType', params.entityType);
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const res = await fetch(`/api/admin/geography/audits?${searchParams.toString()}`, { cache: 'no-store' });
  return handleResponse<PagedResponse<CivicGeographyAuditResponse>>(res);
}
