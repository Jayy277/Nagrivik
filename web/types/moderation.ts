export type ReportStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'DISMISSED';

export type ModerationTargetType = 'ISSUE' | 'COMMENT' | 'USER';

export type ModerationReason =
  | 'SPAM'
  | 'ABUSIVE_OR_HARASSING'
  | 'HATEFUL_CONTENT'
  | 'SEXUAL_OR_EXPLICIT'
  | 'PERSONAL_INFORMATION'
  | 'MISLEADING_OR_MANIPULATIVE'
  | 'DUPLICATE_CONTENT'
  | 'IRRELEVANT'
  | 'OTHER';

export type ModerationActionType =
  | 'NO_ACTION'
  | 'HIDE_CONTENT'
  | 'RESTORE_CONTENT'
  | 'REMOVE_COMMENT'
  | 'RESTRICT_USER'
  | 'UNRESTRICT_USER';

export type ModerationStatus = 'VISIBLE' | 'HIDDEN';

export interface SafeUserSummary {
  id: string;
  fullName: string | null;
  role: string;
}

export interface ModerationReport {
  id: string;
  reporterId: string;
  targetType: ModerationTargetType;
  targetId: string;
  reason: ModerationReason;
  description: string | null;
  status: ReportStatus;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  resolvedByUserId: string | null;
}

export interface ModerationActionRecord {
  id: string;
  reportId: string | null;
  targetType: ModerationTargetType;
  targetId: string;
  moderatorUserId: string;
  action: ModerationActionType;
  reason: string;
  notes: string | null;
  createdAt: string;
}

export interface IssueTargetDetail {
  id: string;
  title: string;
  description: string;
  categoryName: string | null;
  status: string | null;
  priority: string | null;
  moderationStatus: ModerationStatus;
  createdAt: string;
  reporter: SafeUserSummary | null;
  responsibility?: {
    civicBodyName: string | null;
    wardName: string | null;
    departmentName: string | null;
  } | null;
  mediaUrls: string[];
}

export interface CommentTargetDetail {
  id: string;
  issueId: string | null;
  issueTitle: string | null;
  content: string;
  moderationStatus: ModerationStatus;
  isDeleted: boolean;
  createdAt: string;
  author: SafeUserSummary | null;
}

export interface ModerationReportDetail {
  id: string;
  status: ReportStatus;
  reason: ModerationReason;
  description: string | null;
  targetType: ModerationTargetType;
  targetId: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  reporter: SafeUserSummary | null;
  resolvedBy: SafeUserSummary | null;
  issueTarget?: IssueTargetDetail | null;
  commentTarget?: CommentTargetDetail | null;
  actionHistory: ModerationActionRecord[];
}

export interface ModerationSummary {
  openCount: number;
  inReviewCount: number;
  resolvedCount: number;
  dismissedCount: number;
  totalReports: number;
}

export interface ModerationFilterParams {
  status?: ReportStatus | 'ALL';
  targetType?: ModerationTargetType | 'ALL';
  reason?: ModerationReason | 'ALL';
  sort?: string;
  page?: number;
  size?: number;
}

export const MODERATION_REASON_LABELS: Record<ModerationReason, string> = {
  SPAM: 'Spam or Commercial Solicitation',
  ABUSIVE_OR_HARASSING: 'Abusive or Harassing Language',
  HATEFUL_CONTENT: 'Hate Speech or Discrimination',
  SEXUAL_OR_EXPLICIT: 'Sexually Explicit or Inappropriate',
  PERSONAL_INFORMATION: 'Private Personal Data (PII)',
  MISLEADING_OR_MANIPULATIVE: 'Misleading or Manipulative Information',
  DUPLICATE_CONTENT: 'Duplicate Content',
  IRRELEVANT: 'Irrelevant to Civic Issues',
  OTHER: 'Other Policy Violation',
};
