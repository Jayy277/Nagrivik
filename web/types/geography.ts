export type CivicBodyType = 'MUNICIPAL_CORPORATION' | 'MUNICIPALITY' | 'GRAM_PANCHAYAT';

export interface CivicBodyResponse {
  id: string;
  name: string;
  type: CivicBodyType;
  state: string;
  city: string;
  officialWebsite?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  lastVerifiedAt?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
  referencingCitiesCount: number;
  referencingWardsCount: number;
  referencingDepartmentsCount: number;
  referencingIssuesCount: number;
}

export interface CreateCivicBodyRequest {
  name: string;
  type: CivicBodyType;
  state: string;
  city: string;
  officialWebsite?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
}

export interface UpdateCivicBodyRequest {
  name?: string;
  type?: CivicBodyType;
  state?: string;
  city?: string;
  officialWebsite?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  isActive?: boolean;
  version?: number;
}

export interface CityResponse {
  id: string;
  name: string;
  state: string;
  countryCode: string;
  civicBodyId?: string | null;
  civicBodyName?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  lastVerifiedAt?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
  referencingWardsCount: number;
  referencingIssuesCount: number;
}

export interface CreateCityRequest {
  name: string;
  state: string;
  countryCode?: string;
  civicBodyId?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
}

export interface UpdateCityRequest {
  name?: string;
  state?: string;
  countryCode?: string;
  civicBodyId?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  isActive?: boolean;
  version?: number;
}

export interface WardResponse {
  id: string;
  wardNumber?: string | null;
  wardName: string;
  wardCode?: string | null;
  cityId?: string | null;
  cityName?: string | null;
  civicBodyId?: string | null;
  civicBodyName?: string | null;
  hasBoundary: boolean;
  boundaryGeometryWkt?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  lastVerifiedAt?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
  referencingIssuesCount: number;
  departmentMappingsCount: number;
}

export interface CreateWardRequest {
  cityId: string;
  civicBodyId?: string | null;
  wardNumber?: string | null;
  wardName: string;
  wardCode?: string | null;
  boundaryGeometryWkt?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
}

export interface UpdateWardRequest {
  cityId?: string;
  civicBodyId?: string | null;
  wardNumber?: string | null;
  wardName?: string;
  wardCode?: string | null;
  boundaryGeometryWkt?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  isActive?: boolean;
  version?: number;
}

export interface DepartmentResponse {
  id: string;
  civicBodyId?: string | null;
  civicBodyName?: string | null;
  name: string;
  code?: string | null;
  description?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  lastVerifiedAt?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
  referencingIssuesCount: number;
  categoryMappingsCount: number;
  wardMappingsCount: number;
}

export interface CreateDepartmentRequest {
  civicBodyId: string;
  name: string;
  code?: string | null;
  description?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
}

export interface UpdateDepartmentRequest {
  name?: string;
  code?: string | null;
  description?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  isActive?: boolean;
  version?: number;
}

export interface CategoryDepartmentMappingResponse {
  id: string;
  categoryId: string;
  categoryName: string;
  categorySlug: string;
  departmentId: string;
  departmentName: string;
  departmentCode?: string | null;
  civicBodyId?: string | null;
  civicBodyName?: string | null;
  isActive: boolean;
  source?: string | null;
  sourceUrl?: string | null;
  lastVerifiedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateCategoryDepartmentMappingRequest {
  categoryId: string;
  departmentId: string;
  source?: string | null;
  sourceUrl?: string | null;
}

export interface UpdateCategoryDepartmentMappingRequest {
  isActive?: boolean;
  source?: string | null;
  sourceUrl?: string | null;
  version?: number;
}

export interface WardDepartmentMappingResponse {
  id: string;
  wardId: string;
  wardNumber?: string | null;
  wardName: string;
  wardCode?: string | null;
  cityId?: string | null;
  cityName?: string | null;
  departmentId: string;
  departmentName: string;
  departmentCode?: string | null;
  civicBodyId?: string | null;
  civicBodyName?: string | null;
  isActive: boolean;
  source?: string | null;
  sourceUrl?: string | null;
  lastVerifiedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateWardDepartmentMappingRequest {
  wardId: string;
  departmentId: string;
  source?: string | null;
  sourceUrl?: string | null;
}

export interface UpdateWardDepartmentMappingRequest {
  isActive?: boolean;
  source?: string | null;
  sourceUrl?: string | null;
  version?: number;
}

export interface WardBoundaryIssue {
  wardId: string;
  wardNumber?: string | null;
  wardName: string;
  issueType: string;
  message: string;
}

export interface WardBoundaryValidationResponse {
  totalWardsChecked: number;
  validBoundaries: number;
  invalidBoundaries: number;
  missingBoundaries: number;
  duplicateGeometries: number;
  issues: WardBoundaryIssue[];
  warnings?: string[];
}

export type BoundaryValidationSummary = WardBoundaryValidationResponse;
export type CivicGeographyOverview = GeographyOverviewResponse;

export interface ReResolveRequest {
  issueId?: string | null;
  onlyUnresolved?: boolean;
  limit?: number;
}

export interface ReResolveResponse {
  totalProcessed: number;
  resolvedCount: number;
  unresolvedCount: number;
  failureCount: number;
  messages: string[];
}

export interface GeographyOverviewResponse {
  totalCivicBodies: number;
  activeCivicBodies: number;
  totalCities: number;
  activeCities: number;
  totalWards: number;
  activeWards: number;
  wardsWithBoundaries: number;
  wardsWithoutBoundaries: number;
  totalDepartments: number;
  activeDepartments: number;
  totalCategoryMappings: number;
  activeCategoryMappings: number;
  totalWardMappings: number;
  activeWardMappings: number;
  totalIssues: number;
  resolvedIssues: number;
  unresolvedIssues: number;
  totalAudits: number;
}

export interface CivicGeographyAuditResponse {
  id: string;
  actorId?: string | null;
  actorName?: string | null;
  actorEmail?: string | null;
  entityType: string;
  entityId: string;
  action: string;
  previousState?: string | null;
  newState?: string | null;
  reason?: string | null;
  source?: string | null;
  sourceUrl?: string | null;
  createdAt: string;
}

export interface PaginatedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
