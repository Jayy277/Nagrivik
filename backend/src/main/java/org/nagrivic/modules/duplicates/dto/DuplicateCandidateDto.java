package org.nagrivic.modules.duplicates.dto;

import org.nagrivic.modules.issues.dto.IssueResponse.CategorySummary;
import org.nagrivic.modules.issues.model.IssueStatus;

import java.util.UUID;

public record DuplicateCandidateDto(
    UUID issueId,
    String title,
    CategorySummary category,
    double distanceMeters,
    IssueStatus status,
    long supportCount
) {}
