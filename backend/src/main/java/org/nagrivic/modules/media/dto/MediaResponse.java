package org.nagrivic.modules.media.dto;

import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;

import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
    UUID id,
    UUID issueId,
    MediaType mediaType,
    String contentType,
    Long fileSizeBytes,
    Integer displayOrder,
    Instant createdAt
) {
    public static MediaResponse fromEntity(MediaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MediaResponse(
            entity.getId(),
            entity.getIssue() != null ? entity.getIssue().getId() : null,
            entity.getMediaType(),
            entity.getContentType(),
            entity.getFileSizeBytes(),
            entity.getDisplayOrder(),
            entity.getCreatedAt()
        );
    }
}
