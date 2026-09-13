package org.nagrivic.modules.issues.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.nagrivic.modules.civicgeography.dto.CivicAreaResponse;
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
    boolean isDuplicate,
    UUID primaryIssueId,
    List<MediaSummary> media,
    long supportCount,
    long commentCount,
    boolean supportedByCurrentUser,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    CivicAreaResponse civicArea,
    CivicResponsibilityDto civicResponsibility,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    org.nagrivic.modules.priority.dto.PriorityResponse priority,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Double distanceMeters,
    Instant createdAt,
    Instant updatedAt
) {
    public IssueResponse(
        UUID id,
        UUID reportedBy,
        CategorySummary category,
        LocationResponse location,
        String title,
        String description,
        IssueStatus status,
        boolean isDuplicate,
        UUID primaryIssueId,
        List<MediaSummary> media,
        long supportCount,
        long commentCount,
        boolean supportedByCurrentUser,
        CivicAreaResponse civicArea,
        CivicResponsibilityDto civicResponsibility,
        org.nagrivic.modules.priority.dto.PriorityResponse priority,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, reportedBy, category, location, title, description, status, isDuplicate, primaryIssueId,
             media, supportCount, commentCount, supportedByCurrentUser, civicArea, civicResponsibility,
             priority, null, createdAt, updatedAt);
    }

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
        return fromEntity(entity, Collections.emptyList(), 0L, 0L, false, null, null);
    }

    public static IssueResponse fromEntity(IssueEntity entity, List<MediaEntity> mediaList) {
        return fromEntity(entity, mediaList, 0L, 0L, false, null, null);
    }

    public static IssueResponse fromEntity(IssueEntity entity, long supportCount, boolean supportedByCurrentUser) {
        return fromEntity(entity, Collections.emptyList(), supportCount, 0L, supportedByCurrentUser, null, null);
    }

    public static IssueResponse fromEntity(IssueEntity entity, long supportCount, long commentCount, boolean supportedByCurrentUser) {
        return fromEntity(entity, Collections.emptyList(), supportCount, commentCount, supportedByCurrentUser, null, null);
    }

    public static IssueResponse fromEntity(
            IssueEntity entity,
            List<MediaEntity> mediaList,
            long supportCount,
            boolean supportedByCurrentUser
    ) {
        return fromEntity(entity, mediaList, supportCount, 0L, supportedByCurrentUser, null, null);
    }

    public static IssueResponse fromEntity(
            IssueEntity entity,
            List<MediaEntity> mediaList,
            long supportCount,
            long commentCount,
            boolean supportedByCurrentUser
    ) {
        return fromEntity(entity, mediaList, supportCount, commentCount, supportedByCurrentUser, null, null);
    }

    public static IssueResponse fromEntity(
            IssueEntity entity,
            List<MediaEntity> mediaList,
            long supportCount,
            long commentCount,
            boolean supportedByCurrentUser,
            CivicAreaResponse civicArea
    ) {
        return fromEntity(entity, mediaList, supportCount, commentCount, supportedByCurrentUser, civicArea, null);
    }

    public static IssueResponse fromEntity(
            IssueEntity entity,
            List<MediaEntity> mediaList,
            long supportCount,
            long commentCount,
            boolean supportedByCurrentUser,
            CivicAreaResponse civicArea,
            Double distanceMeters
    ) {
        return fromEntity(entity, mediaList, supportCount, commentCount, supportedByCurrentUser, civicArea, null, distanceMeters);
    }

    public static IssueResponse fromEntity(
            IssueEntity entity,
            List<MediaEntity> mediaList,
            long supportCount,
            long commentCount,
            boolean supportedByCurrentUser,
            CivicAreaResponse civicArea,
            org.nagrivic.modules.priority.dto.PriorityResponse priorityOverride,
            Double distanceMeters
    ) {
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

        CivicResponsibilityDto civicResponsibility = CivicResponsibilityDto.fromEntity(entity);
        org.nagrivic.modules.priority.dto.PriorityResponse priority = priorityOverride != null
                ? priorityOverride
                : org.nagrivic.modules.priority.dto.PriorityResponse.fromEntity(entity.getPriority());

        return new IssueResponse(
            entity.getId(),
            entity.getReporter() != null ? entity.getReporter().getId() : null,
            categorySummary,
            locationResponse,
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            entity.isDuplicate(),
            entity.getPrimaryIssueId(),
            mediaSummaries,
            supportCount,
            commentCount,
            supportedByCurrentUser,
            civicArea,
            civicResponsibility,
            priority,
            distanceMeters,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
