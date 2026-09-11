package org.nagrivic.modules.issues.dto;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;

import java.time.Instant;
import java.util.UUID;

public record IssueResponse(
    UUID id,
    UUID reportedBy,
    String title,
    String description,
    IssueStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static IssueResponse fromEntity(IssueEntity entity) {
        return new IssueResponse(
            entity.getId(),
            entity.getReporter() != null ? entity.getReporter().getId() : null,
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
