package org.nagrivic.modules.duplicates.repository;

import java.util.UUID;

public record DuplicateCandidateRecord(
    UUID issueId,
    String title,
    UUID categoryId,
    String categoryName,
    String categorySlug,
    String status,
    Double distanceMeters
) implements DuplicateCandidateProjection {
    @Override
    public UUID getIssueId() {
        return issueId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public UUID getCategoryId() {
        return categoryId;
    }

    @Override
    public String getCategoryName() {
        return categoryName;
    }

    @Override
    public String getCategorySlug() {
        return categorySlug;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Double getDistanceMeters() {
        return distanceMeters;
    }
}
