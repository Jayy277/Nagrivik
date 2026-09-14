export type ImageAnalysisStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'UNAVAILABLE';

export type CivicVisualCategory =
  | 'ROADS_POTHOLES'
  | 'GARBAGE'
  | 'STREETLIGHTS'
  | 'WATER'
  | 'DRAINAGE';

export type ImageQuality = 'GOOD' | 'ACCEPTABLE' | 'POOR';

export type QualityIssue =
  | 'TOO_DARK'
  | 'TOO_BLURRY'
  | 'TOO_DISTANT'
  | 'HEAVILY_OBSTRUCTED'
  | 'INSUFFICIENT_CONTEXT';

export type CivicRelevance = 'LIKELY_RELEVANT' | 'UNCERTAIN' | 'LIKELY_IRRELEVANT';

export type VisualSafetyConcern = 'NONE' | 'POSSIBLE' | 'HIGH';

export type VisualProblemType =
  | 'POTHOLE'
  | 'CRACKED_ROAD'
  | 'ROAD_CAVE_IN'
  | 'UNPAVED_ROAD'
  | 'GARBAGE_PILE'
  | 'OVERFLOWING_BIN'
  | 'SCATTERED_LITTER'
  | 'DEAD_ANIMAL'
  | 'DAMAGED_LIGHT_POLE'
  | 'DARK_STREETLIGHT'
  | 'HANGING_WIRES'
  | 'BROKEN_FIXTURE'
  | 'WATER_LEAKAGE'
  | 'BURST_PIPE'
  | 'CONTAMINATED_WATER'
  | 'LOW_PRESSURE_OUTLET'
  | 'BLOCKED_DRAIN'
  | 'OVERFLOWING_MANHOLE'
  | 'OPEN_SEWER'
  | 'STAGNANT_WATER';

export type VisualSeveritySignal =
  | 'LARGE_ROAD_OBSTRUCTION'
  | 'DEEP_POTHOLE'
  | 'EXPOSED_INFRASTRUCTURE'
  | 'FLOODING'
  | 'BLOCKED_DRAINAGE'
  | 'LARGE_WASTE_ACCUMULATION'
  | 'POTENTIAL_SAFETY_HAZARD';

export interface ImageAiAnalysis {
  id: string;
  mediaId: string;
  issueId: string;
  provider: string;
  model?: string | null;
  modelVersion?: string | null;
  calculationVersion: string;
  status: ImageAnalysisStatus;
  likelyCategory?: CivicVisualCategory | null;
  categoryConfidence?: number | null;
  visualProblemTypes: VisualProblemType[];
  imageQuality?: ImageQuality | null;
  qualityIssues: QualityIssue[];
  relevance?: CivicRelevance | null;
  visualSeveritySignals: VisualSeveritySignal[];
  safetyConcern?: VisualSafetyConcern | null;
  sensitiveVisualContentDetected: boolean;
  summary?: string | null;
  createdAt: string;
  updatedAt: string;
}
