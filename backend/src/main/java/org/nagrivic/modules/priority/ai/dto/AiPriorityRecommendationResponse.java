package org.nagrivic.modules.priority.ai.dto;

import org.nagrivic.modules.priority.ai.model.PriorityAiStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public/Privileged DTO representing an AI priority recommendation.
 */
public record AiPriorityRecommendationResponse(
        UUID id,
        UUID issueId,
        String provider,
        String model,
        String modelVersion,
        String calculationVersion,
        PriorityAiStatus status,
        Integer suggestedSeverity,
        Integer suggestedImpact,
        Integer suggestedSafety,
        Integer confidence,
        Integer severityConfidence,
        Integer impactConfidence,
        Integer safetyConfidence,
        List<String> signals,
        String failureReason,
        boolean appliedToCalculation,
        Instant createdAt,
        Instant completedAt
) {}
