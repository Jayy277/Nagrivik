package org.nagrivic.modules.priority.ai.dto;

import org.nagrivic.modules.priority.ai.model.PriorityAiStatus;

import java.util.List;

/**
 * Structured, bounded output produced by a PriorityAiProvider.
 */
public record PriorityAiResult(
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
        String provider,
        String model,
        String modelVersion,
        String calculationVersion
) {
    public PriorityAiResult {
        signals = signals != null ? List.copyOf(signals) : List.of();
    }

    public static PriorityAiResult completed(
            int suggestedSeverity,
            int suggestedImpact,
            int suggestedSafety,
            int confidence,
            int severityConfidence,
            int impactConfidence,
            int safetyConfidence,
            List<String> signals,
            String provider,
            String model,
            String modelVersion,
            String calculationVersion
    ) {
        // Enforce strict server-side clamping
        int clampedSeverity = Math.min(30, Math.max(0, suggestedSeverity));
        int clampedImpact = Math.min(25, Math.max(0, suggestedImpact));
        int clampedSafety = Math.min(25, Math.max(0, suggestedSafety));
        int clampedConfidence = Math.min(100, Math.max(0, confidence));

        return new PriorityAiResult(
                PriorityAiStatus.COMPLETED,
                clampedSeverity,
                clampedImpact,
                clampedSafety,
                clampedConfidence,
                Math.min(100, Math.max(0, severityConfidence)),
                Math.min(100, Math.max(0, impactConfidence)),
                Math.min(100, Math.max(0, safetyConfidence)),
                signals,
                null,
                provider,
                model,
                modelVersion,
                calculationVersion
        );
    }

    public static PriorityAiResult unavailable(String calculationVersion) {
        return new PriorityAiResult(
                PriorityAiStatus.UNAVAILABLE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "AI priority assistance is disabled or unavailable",
                "DISABLED",
                "none",
                "none",
                calculationVersion != null ? calculationVersion : "priority-assistance-v1"
        );
    }

    public static PriorityAiResult failed(String provider, String model, String modelVersion, String calculationVersion, String reason) {
        return new PriorityAiResult(
                PriorityAiStatus.FAILED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                reason,
                provider,
                model,
                modelVersion,
                calculationVersion != null ? calculationVersion : "priority-assistance-v1"
        );
    }
}
