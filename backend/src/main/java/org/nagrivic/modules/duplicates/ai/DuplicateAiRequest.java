package org.nagrivic.modules.duplicates.ai;

import java.util.List;
import java.util.UUID;

public record DuplicateAiRequest(
    NewIssueInfo target,
    List<CandidateInfo> candidates
) {
    public record NewIssueInfo(
        String title,
        String description,
        UUID categoryId,
        String categoryName
    ) {}

    public record CandidateInfo(
        UUID issueId,
        String title,
        String description,
        UUID categoryId,
        String categoryName,
        double distanceMeters
    ) {}
}
