package org.nagrivic.modules.duplicates.dto;

import org.nagrivic.modules.duplicates.model.DuplicateConfidence;
import org.nagrivic.modules.duplicates.model.DuplicateMatchType;
import org.nagrivic.modules.issues.dto.IssueResponse.CategorySummary;
import org.nagrivic.modules.issues.model.IssueStatus;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record DuplicateCandidateDto(
    UUID issueId,
    String title,
    CategorySummary category,
    double distanceMeters,
    IssueStatus status,
    long supportCount,
    boolean deterministicMatch,
    Integer aiScore,
    DuplicateConfidence confidence,
    DuplicateMatchType matchType,
    List<String> signals
) {
    public DuplicateCandidateDto(
            UUID issueId,
            String title,
            CategorySummary category,
            double distanceMeters,
            IssueStatus status,
            long supportCount
    ) {
        this(
                issueId,
                title,
                category,
                distanceMeters,
                status,
                supportCount,
                true,
                null,
                null,
                DuplicateMatchType.DETERMINISTIC_MATCH,
                Collections.emptyList()
        );
    }
}
