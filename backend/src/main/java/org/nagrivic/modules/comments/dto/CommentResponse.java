package org.nagrivic.modules.comments.dto;

import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID issueId,
    String content,
    AuthorSummary author,
    boolean deleted,
    Instant createdAt,
    Instant updatedAt
) {
    public record AuthorSummary(
        UUID id,
        String displayName
    ) {}

    public static CommentResponse fromEntity(CommentEntity entity) {
        if (entity == null) {
            return null;
        }

        UUID issueId = entity.getIssue() != null ? entity.getIssue().getId() : null;
        boolean isDeleted = entity.isDeleted();

        String displayContent = isDeleted ? "[Comment deleted]" : entity.getContent();

        AuthorSummary authorSummary = null;
        if (entity.getUser() != null) {
            UserEntity user = entity.getUser();
            String name = "Citizen";
            if (!isDeleted && user.getFullName() != null && !user.getFullName().isBlank()) {
                name = user.getFullName().trim();
            }
            authorSummary = new AuthorSummary(user.getId(), name);
        }

        return new CommentResponse(
            entity.getId(),
            issueId,
            displayContent,
            authorSummary,
            isDeleted,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
