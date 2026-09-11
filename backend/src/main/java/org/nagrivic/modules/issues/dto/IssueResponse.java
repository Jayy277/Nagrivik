package org.nagrivic.modules.issues.dto;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.locations.dto.LocationResponse;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record IssueResponse(
    UUID id,
    UUID reportedBy,
    CategorySummary category,
    LocationResponse location,
    String title,
    String description,
    IssueStatus status,
    List<MediaSummary> media,
    Instant createdAt,
    Instant updatedAt
) {
    public record CategorySummary(
        UUID id,
        String name,
        String slug
    ) {}

    public record MediaSummary(
        UUID id,
        MediaType mediaType,
        String contentType,
        Integer displayOrder
    ) {
        public static MediaSummary fromEntity(MediaEntity entity) {
            if (entity == null) {
                return null;
            }
            return new MediaSummary(
                entity.getId(),
                entity.getMediaType(),
                entity.getContentType(),
                entity.getDisplayOrder()
            );
        }
    }

    public static IssueResponse fromEntity(IssueEntity entity) {
        return fromEntity(entity, Collections.emptyList());
    }

    public static IssueResponse fromEntity(IssueEntity entity, List<MediaEntity> mediaList) {
        CategorySummary categorySummary = null;
        if (entity.getCategory() != null) {
            categorySummary = new CategorySummary(
                entity.getCategory().getId(),
                entity.getCategory().getName(),
                entity.getCategory().getSlug()
            );
        }

        LocationResponse locationResponse = null;
        if (entity.getLocation() != null) {
            locationResponse = LocationResponse.fromEntity(entity.getLocation());
        }

        List<MediaSummary> mediaSummaries = Collections.emptyList();
        if (mediaList != null && !mediaList.isEmpty()) {
            mediaSummaries = mediaList.stream()
                .map(MediaSummary::fromEntity)
                .toList();
        }

        return new IssueResponse(
            entity.getId(),
            entity.getReporter() != null ? entity.getReporter().getId() : null,
            categorySummary,
            locationResponse,
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            mediaSummaries,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
