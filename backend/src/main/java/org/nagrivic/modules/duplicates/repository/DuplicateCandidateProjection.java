package org.nagrivic.modules.duplicates.repository;

import java.util.UUID;

public interface DuplicateCandidateProjection {
    UUID getIssueId();
    String getTitle();
    UUID getCategoryId();
    String getCategoryName();
    String getCategorySlug();
    String getStatus();
    Double getDistanceMeters();
}
