export type IssueStatus =
  | 'REPORTED'
  | 'VERIFIED'
  | 'ACKNOWLEDGED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CITIZEN_VERIFIED'
  | 'NOT_FIXED';

export type PriorityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type IssueDiscoverySort =
  | 'NEWEST'
  | 'OLDEST'
  | 'PRIORITY'
  | 'MOST_SUPPORTED'
  | 'NEAREST';

export interface CategorySummary {
  id: string;
  name: string;
  slug?: string;
  description?: string;
  displayOrder?: number;
}

export interface LocationSummary {
  id?: string;
  latitude?: number;
  longitude?: number;
  accuracyMeters?: number;
}

export interface CivicAreaSummary {
  city?: string;
  ward?: { name: string; number?: string };
  civicBody?: { name: string };
  department?: { name: string };
}

export interface CivicResponsibilityDto {
  status?: string;
  civicBody?: { id: string; name: string };
  city?: { id: string; name: string };
  ward?: { id: string; name: string };
  department?: { id: string; name: string };
}

export interface PriorityResponse {
  level: PriorityLevel;
  score?: number;
  calculatedAt?: string;
}

export interface MediaSummary {
  id: string;
  mediaType: string;
  contentType: string;
  displayOrder: number;
}

export interface IssueResponse {
  id: string;
  reportedBy?: string | null;
  category: CategorySummary;
  location?: LocationSummary;
  title: string;
  description: string;
  status: IssueStatus;
  isDuplicate: boolean;
  primaryIssueId?: string | null;
  media?: MediaSummary[];
  supportCount: number;
  commentCount: number;
  supportedByCurrentUser: boolean;
  civicArea?: CivicAreaSummary;
  civicResponsibility?: CivicResponsibilityDto;
  priority?: PriorityResponse;
  distanceMeters?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface IssueDiscoveryFilterParams {
  q?: string;
  categoryId?: string;
  status?: IssueStatus;
  priority?: PriorityLevel;
  cityId?: string;
  wardId?: string;
  civicBodyId?: string;
  departmentId?: string;
  latitude?: number;
  longitude?: number;
  radiusMeters?: number;
  sort?: IssueDiscoverySort;
  page?: number;
  size?: number;
}

export interface AuthorSummary {
  id?: string;
  displayName: string;
}

export interface CommentResponse {
  id: string;
  issueId: string;
  content: string;
  author?: AuthorSummary;
  deleted: boolean;
  createdAt: string;
  updatedAt?: string;
}

export type IssueActivityType =
  | 'ISSUE_REPORTED'
  | 'STATUS_CHANGED'
  | 'COMMENT_ADDED'
  | 'COMMENT_DELETED'
  | 'SUPPORT_ADDED'
  | 'SUPPORT_REMOVED'
  | 'DUPLICATE_LINKED'
  | 'RESPONSIBILITY_RESOLVED'
  | 'RESPONSIBILITY_RE_RESOLVED'
  | 'PRIORITY_RECALCULATED'
  | 'MEDIA_ADDED';

export interface ActivityResponse {
  id: string;
  eventType: IssueActivityType;
  actor?: AuthorSummary;
  data?: Record<string, unknown>;
  createdAt: string;
}

export interface StatusHistoryItemDto {
  id: string;
  fromStatus?: IssueStatus;
  toStatus: IssueStatus;
  changedBy?: { displayName: string };
  reason?: string;
  createdAt: string;
}

export interface StatusHistoryResponse {
  issueId: string;
  history: StatusHistoryItemDto[];
}
