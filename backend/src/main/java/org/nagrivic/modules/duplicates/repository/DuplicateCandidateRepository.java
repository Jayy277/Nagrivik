package org.nagrivic.modules.duplicates.repository;

import java.util.List;
import java.util.UUID;

public interface DuplicateCandidateRepository {
    List<DuplicateCandidateProjection> findPotentialDuplicates(
            UUID categoryId,
            double targetLon,
            double targetLat,
            double radiusMeters,
            UUID excludeIssueId,
            int limit
    );
}
