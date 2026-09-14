export type DuplicateConfidence = 'LOW' | 'POSSIBLE' | 'LIKELY' | 'HIGH';

export type DuplicateMatchType = 'DETERMINISTIC_MATCH' | 'AI_SUGGESTION' | 'BOTH';

export type DuplicateSuggestionStatus = 'SUGGESTED' | 'LINKED' | 'DISMISSED';

export interface AiStatus {
  enabled: boolean;
  status: string;
  model?: string | null;
}

export interface DuplicateCandidate {
  issueId: string;
  title: string;
  category: {
    id: string;
    name: string;
    slug?: string;
  };
  distanceMeters: number;
  status: string;
  supportCount: number;
  deterministicMatch: boolean;
  aiScore?: number | null;
  confidence?: DuplicateConfidence | null;
  matchType: DuplicateMatchType;
  signals: string[];
}

export interface DuplicateCheckResponse {
  hasPotentialDuplicates: boolean;
  candidates: DuplicateCandidate[];
  ai: AiStatus;
}

export interface IssueSummary {
  id: string;
  title: string;
  description?: string | null;
  categoryName?: string | null;
  categorySlug?: string | null;
  status: string;
  latitude?: number | null;
  longitude?: number | null;
  createdAt: string;
}

export interface AiDuplicateSuggestion {
  id: string;
  sourceIssue: IssueSummary;
  candidateIssue: IssueSummary;
  score: number;
  confidence: DuplicateConfidence;
  signals: string[];
  provider: string;
  model: string;
  calculationVersion: string;
  status: DuplicateSuggestionStatus;
  dismissReason?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
}

export interface DuplicateSuggestionsFilter {
  status?: DuplicateSuggestionStatus;
  confidence?: DuplicateConfidence;
  minScore?: number;
  page?: number;
  size?: number;
}
