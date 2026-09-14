import { IssueStatus, PriorityLevel, ActivityResponse, StatusHistoryItemDto } from './issue';

export interface AuthorityScopeDto {
  id: string;
  civicBodyId: string;
  civicBodyName: string;
  cityId: string;
  cityName: string;
  wardId?: string | null;
  wardName?: string | null;
  wardNumber?: string | null;
  departmentId?: string | null;
  departmentName?: string | null;
  departmentCode?: string | null;
  designation?: string | null;
}

export interface AuthorityDashboardMetrics {
  totalScopedIssues: number;
  actionableIssues: number;
  verifiedIssues: number;
  acknowledgedIssues: number;
  inProgressIssues: number;
  resolvedIssues: number;
  assignedScopes: AuthorityScopeDto[];
}

export interface AuthorityIssueItemResponse {
  id: string;
  title: string;
  descriptionSnippet: string;
  categoryName: string;
  categorySlug: string;
  status: IssueStatus;
  priorityLevel: PriorityLevel;
  priorityScore: number;
  civicBodyName?: string | null;
  cityName?: string | null;
  wardName?: string | null;
  departmentName?: string | null;
  supportCount: number;
  commentCount: number;
  actionable: boolean;
  createdAt: string;
  updatedAt?: string | null;
}

export interface AuthorityNamedEntity {
  id: string;
  name: string;
  code?: string | null;
}

export interface AuthorityCivicResponsibility {
  status: 'RESOLVED' | 'UNRESOLVED';
  civicBody?: AuthorityNamedEntity | null;
  city?: AuthorityNamedEntity | null;
  ward?: AuthorityNamedEntity | null;
  department?: AuthorityNamedEntity | null;
  resolvedAt?: string | null;
  source?: string | null;
}

export interface AuthorityMediaDto {
  id: string;
  mediaUrl: string;
  mediaType: string;
}

export interface AuthorityCommentDto {
  id: string;
  commentText: string;
  authorRole: string;
  authorInitials: string;
  createdAt: string;
}

export interface AuthorityIssueDetailResponse {
  id: string;
  title: string;
  description: string;
  category: {
    id: string;
    name: string;
    slug: string;
  };
  status: IssueStatus;
  priorityLevel: PriorityLevel;
  priorityScore: number;
  civicResponsibility: AuthorityCivicResponsibility;
  locationSummary: string;
  latitude?: number | null;
  longitude?: number | null;
  media: AuthorityMediaDto[];
  supportCount: number;
  commentCount: number;
  comments: AuthorityCommentDto[];
  activity: ActivityResponse[];
  statusHistory: StatusHistoryItemDto[];
  allowedTransitions: IssueStatus[];
  resolutionEvidence?: ResolutionEvidenceResponse[];
  version: number;
  createdAt: string;
  updatedAt?: string | null;
}

export type ResolutionEvidenceType = 'COMPLETION_PHOTO' | 'COMPLETION_NOTE' | 'BEFORE_AFTER_PHOTO';

export interface ResolutionEvidenceResponse {
  id: string;
  issueId: string;
  evidenceType: ResolutionEvidenceType;
  mediaUrl?: string | null;
  originalFilename?: string | null;
  fileSizeBytes?: number | null;
  note?: string | null;
  capturedAt?: string | null;
  createdAt: string;
  submittedByRole: string;
  submittedByName: string;
}

export interface AuthorityIssueFilter {
  status?: IssueStatus;
  priority?: PriorityLevel;
  wardId?: string;
  departmentId?: string;
  actionableOnly?: boolean;
  page?: number;
  size?: number;
}

export interface ChangeAuthorityStatusRequest {
  status: IssueStatus;
  reason?: string;
  version?: number;
}
