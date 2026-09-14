package org.nagrivic.modules.authorities.dto;

import org.nagrivic.modules.activity.dto.ActivityResponse;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.statushistory.dto.StatusHistoryItemDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthorityIssueDetailResponse(
        UUID id,
        String title,
        String description,
        CategoryDto category,
        IssueStatus status,
        PriorityLevel priorityLevel,
        Integer priorityScore,
        CivicResponsibilityDto civicResponsibility,
        String locationSummary,
        Double latitude,
        Double longitude,
        List<MediaDto> media,
        int supportCount,
        int commentCount,
        List<AuthorityCommentDto> comments,
        List<ActivityResponse> activity,
        List<StatusHistoryItemDto> statusHistory,
        List<IssueStatus> allowedTransitions,
        List<ResolutionEvidenceResponse> resolutionEvidence,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public record CategoryDto(UUID id, String name, String slug) {}

    public record NamedEntityDto(UUID id, String name, String code) {}

    public record CivicResponsibilityDto(
            ResponsibilityStatus status,
            NamedEntityDto civicBody,
            NamedEntityDto city,
            NamedEntityDto ward,
            NamedEntityDto department,
            Instant resolvedAt,
            String source
    ) {}

    public record MediaDto(UUID id, String mediaUrl, String mediaType) {}

    public record AuthorityCommentDto(
            UUID id,
            String commentText,
            String authorRole,
            String authorInitials,
            Instant createdAt
    ) {}
}
