package org.nagrivic.modules.issues.dto;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;

import java.time.Instant;
import java.util.UUID;

public record IssueResponse(
    UUID id,
    UUID reportedBy,
    CategorySummary category,
    String title,
    String description,
    IssueStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public record CategorySummary(
        UUID id,
        String name,
        String slug
    ) {}

    public static IssueResponse fromEntity(IssueEntity entity) {
        CategorySummary categorySummary = null;
        if (entity.getCategory() != null) {
            categorySummary = new CategorySummary(
                entity.getCategory().getId(),
                entity.getCategory().getName(),
                entity.getCategory().getSlug()
            );
        }

        return new IssueResponse(
            entity.getId(),
            entity.getReporter() != null ? entity.getReporter().getId() : null,
            categorySummary,
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
