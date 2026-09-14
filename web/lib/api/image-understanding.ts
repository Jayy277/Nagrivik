import { ImageAiAnalysis } from '../../types/image-understanding';
import { apiRequest } from './client';

/**
 * Request AI image understanding analysis for a specific media item on an issue.
 * AI analysis is ASSISTIVE ONLY and will never mutate the issue category, status, or priority.
 */
export async function triggerImageAnalysis(
  issueId: string,
  mediaId: string
): Promise<ImageAiAnalysis> {
  return apiRequest<ImageAiAnalysis>(
    `/issues/${issueId}/media/${mediaId}/analyze`,
    {
      method: 'POST',
    }
  );
}

/**
 * Fetch existing AI image understanding analysis for a media item.
 * Returns null if no analysis has been performed yet (404).
 */
export async function getImageAnalysis(
  issueId: string,
  mediaId: string
): Promise<ImageAiAnalysis | null> {
  try {
    return await apiRequest<ImageAiAnalysis>(
      `/issues/${issueId}/media/${mediaId}/analysis`,
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
