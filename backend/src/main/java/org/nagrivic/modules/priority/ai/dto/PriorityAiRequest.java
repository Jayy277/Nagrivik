package org.nagrivic.modules.priority.ai.dto;

import org.nagrivic.modules.media.ai.model.VisualSafetyConcern;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PublicImpact;
import org.nagrivic.modules.priority.model.SafetyImpact;

import java.util.List;
import java.util.UUID;

/**
 * Sanitized server-side request sent to priority AI providers.
 * Contains ZERO citizen PII (no phone, email, JWT, reporter UUID, or private address).
 */
public record PriorityAiRequest(
        UUID issueId,
        String title,
        String description,
        String categorySlug,
        String categoryName,
        IssueSeverity currentSeverity,
        PublicImpact currentImpact,
        SafetyImpact currentSafety,
        List<String> visualProblemTypes,
        List<String> visualSeveritySignals,
        VisualSafetyConcern visualSafetyConcern
) {
    public PriorityAiRequest {
        visualProblemTypes = visualProblemTypes != null ? List.copyOf(visualProblemTypes) : List.of();
        visualSeveritySignals = visualSeveritySignals != null ? List.copyOf(visualSeveritySignals) : List.of();
    }
}
