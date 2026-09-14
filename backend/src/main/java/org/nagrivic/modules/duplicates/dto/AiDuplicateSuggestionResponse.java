package org.nagrivic.modules.duplicates.dto;

import org.nagrivic.modules.duplicates.entity.AiDuplicateSuggestionEntity;
import org.nagrivic.modules.duplicates.model.DuplicateConfidence;
import org.nagrivic.modules.duplicates.model.DuplicateSuggestionStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record AiDuplicateSuggestionResponse(
    UUID id,
    IssueSummary sourceIssue,
    IssueSummary candidateIssue,
    int score,
    DuplicateConfidence confidence,
    List<String> signals,
    String provider,
    String model,
    String calculationVersion,
    DuplicateSuggestionStatus status,
    String dismissReason,
    UUID reviewedBy,
    Instant reviewedAt,
    Instant createdAt
) {
    public record IssueSummary(
        UUID id,
        String title,
        String description,
        String categoryName,
        String categorySlug,
        String status,
        Double latitude,
        Double longitude,
        Instant createdAt
    ) {}

    public static AiDuplicateSuggestionResponse fromEntity(AiDuplicateSuggestionEntity entity, List<String> signalsList) {
        var src = entity.getIssue();
        var cand = entity.getCandidateIssue();

        IssueSummary srcSummary = new IssueSummary(
                src.getId(),
                src.getTitle(),
                src.getDescription(),
                src.getCategory() != null ? src.getCategory().getName() : null,
                src.getCategory() != null ? src.getCategory().getSlug() : null,
                src.getStatus() != null ? src.getStatus().name() : null,
                src.getLocation() != null ? src.getLocation().getLatitude() : null,
                src.getLocation() != null ? src.getLocation().getLongitude() : null,
                src.getCreatedAt()
        );

        IssueSummary candSummary = new IssueSummary(
                cand.getId(),
                cand.getTitle(),
                cand.getDescription(),
                cand.getCategory() != null ? cand.getCategory().getName() : null,
                cand.getCategory() != null ? cand.getCategory().getSlug() : null,
                cand.getStatus() != null ? cand.getStatus().name() : null,
                cand.getLocation() != null ? cand.getLocation().getLatitude() : null,
                cand.getLocation() != null ? cand.getLocation().getLongitude() : null,
                cand.getCreatedAt()
        );

        return new AiDuplicateSuggestionResponse(
                entity.getId(),
                srcSummary,
                candSummary,
                entity.getScore(),
                entity.getConfidence(),
                signalsList != null ? signalsList : Collections.emptyList(),
                entity.getProvider(),
                entity.getModel(),
                entity.getCalculationVersion(),
                entity.getStatus(),
                entity.getDismissReason(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null,
                entity.getReviewedAt(),
                entity.getCreatedAt()
        );
    }
}
