package org.nagrivic.modules.moderation.dto;

public record ModerationSummaryResponse(
        long openCount,
        long inReviewCount,
        long resolvedCount,
        long dismissedCount,
        long totalReports
) {
}
