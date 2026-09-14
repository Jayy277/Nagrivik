export type PriorityAiStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'UNAVAILABLE';

export type PriorityInfluenceMode = 'ADVISORY' | 'BLENDED';

export interface AiPriorityRecommendation {
  id: string;
  issueId: string;
  provider: string;
  model: string;
  modelVersion: string;
  calculationVersion: string;
  status: PriorityAiStatus;
  suggestedSeverity?: number | null;
  suggestedImpact?: number | null;
  suggestedSafety?: number | null;
  confidence?: number | null;
  severityConfidence?: number | null;
  impactConfidence?: number | null;
  safetyConfidence?: number | null;
  signals: string[];
  failureReason?: string | null;
  appliedToCalculation: boolean;
  createdAt: string;
  completedAt?: string | null;
}
