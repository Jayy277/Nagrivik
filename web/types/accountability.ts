import { IssueStatus, PriorityLevel } from './issue';

export interface AccountabilityFilterParams {
  cityId?: string;
  wardId?: string;
  categoryId?: string;
  range?: '7d' | '30d' | '90d' | 'all' | string;
}

export interface AccountabilitySummary {
  totalPublicIssues: number;
  actionableCount: number;
  reportedCount: number;
  verifiedCount: number;
  acknowledgedCount: number;
  inProgressCount: number;
  resolvedCount: number;
  citizenVerifiedCount: number;
  notFixedCount: number;
  highPriorityCount: number;
  criticalPriorityCount: number;
  unresolvedResponsibilityCount: number;
}

export interface StatusBreakdown {
  reported: number;
  verified: number;
  acknowledged: number;
  inProgress: number;
  resolved: number;
  citizenVerified: number;
  notFixed: number;
}

export interface PriorityBreakdown {
  low: number;
  medium: number;
  high: number;
  critical: number;
}

export interface CategoryBreakdownItem {
  categoryId: string;
  name: string;
  slug: string;
  total: number;
  openActionable: number;
  inProgress: number;
  resolved: number;
  citizenVerified: number;
  notFixed: number;
}

export interface WardBreakdownItem {
  wardId: string;
  name: string;
  wardNumber?: string;
  wardCode?: string;
  total: number;
  openActionable: number;
  inProgress: number;
  resolved: number;
  citizenVerified: number;
  notFixed: number;
  highOrCriticalCount: number;
}

export interface AgingBreakdown {
  zeroToOneDay: number;
  twoToSevenDays: number;
  eightToThirtyDays: number;
  thirtyOneToNinetyDays: number;
  overNinetyDays: number;
}

export interface VerificationSummary {
  resolvedByAuthority: number;
  citizenVerified: number;
  citizenReportedNotFixed: number;
  verificationPending: number;
  verificationRate: number;
}

export interface DepartmentBreakdownItem {
  departmentId: string;
  name: string;
  code: string;
  totalAssigned: number;
  openActionable: number;
  resolved: number;
  citizenVerified: number;
}

export interface ResponsibilitySummary {
  resolvedCount: number;
  unresolvedCount: number;
  coveragePercentage: number;
  departmentBreakdown: DepartmentBreakdownItem[];
}

export interface TrendPoint {
  date: string; // YYYY-MM-DD
  reportedCount: number;
  resolvedCount: number;
  citizenVerifiedCount: number;
}

export interface PublicAccountabilityResponse {
  cityId?: string | null;
  cityName?: string | null;
  wardId?: string | null;
  wardName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  range: string;
  summary: AccountabilitySummary;
  statusBreakdown: StatusBreakdown;
  priorityBreakdown: PriorityBreakdown;
  categoryBreakdown: CategoryBreakdownItem[];
  wardBreakdown: WardBreakdownItem[];
  agingBreakdown: AgingBreakdown;
  verificationSummary: VerificationSummary;
  responsibilitySummary: ResponsibilitySummary;
  trend: TrendPoint[];
  lastUpdated: string;
}
