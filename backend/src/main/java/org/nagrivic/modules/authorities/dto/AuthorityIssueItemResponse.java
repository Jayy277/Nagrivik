package org.nagrivic.modules.authorities.dto;

import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.priority.model.PriorityLevel;

import java.time.Instant;
import java.util.UUID;

public record AuthorityIssueItemResponse(
        UUID id,
        String title,
        String descriptionSnippet,
        String categoryName,
        String categorySlug,
        IssueStatus status,
        PriorityLevel priorityLevel,
        Integer priorityScore,
        String civicBodyName,
        String cityName,
        String wardName,
        String wardNumber,
        String departmentName,
        String departmentCode,
        int supportCount,
        int commentCount,
        boolean isActionable,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
