import { AiPriorityRecommendation } from '../../types/priority';
import { apiRequest } from './client';

/**
 * Retrieve AI priority recommendation for an issue.
 * AI analysis is ADVISORY ONLY and provides explainable component suggestions without bypassing deterministic calculations.
 * Returns null if no recommendation has been generated yet (404).
 */
export async function getAiPriorityRecommendation(
  issueId: string
): Promise<AiPriorityRecommendation | null> {
  try {
    return await apiRequest<AiPriorityRecommendation>(
      `/issues/${issueId}/priority/ai-recommendation`,
      {
        method: 'GET',
      }
    );
  } catch (error: any) {
    if (error?.status === 404 || error?.statusCode === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * Trigger an AI priority assessment or reassessment for an issue.
 * Privileged operation restricted to authorized authority personnel.
 */
export async function triggerAiPriorityAssessment(
  issueId: string
): Promise<AiPriorityRecommendation> {
  return apiRequest<AiPriorityRecommendation>(
    `/issues/${issueId}/priority/ai-assess`,
    {
      method: 'POST',
    }
  );
}
