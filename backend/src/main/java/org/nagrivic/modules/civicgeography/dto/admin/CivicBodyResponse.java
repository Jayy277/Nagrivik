package org.nagrivic.modules.civicgeography.dto.admin;

import org.nagrivic.modules.civicgeography.model.CivicBodyType;

import java.time.Instant;
import java.util.UUID;

public record CivicBodyResponse(
        UUID id,
        String name,
        CivicBodyType type,
        String state,
        String city,
        String officialWebsite,
        String source,
        String sourceUrl,
        Instant lastVerifiedAt,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        long referencingCitiesCount,
        long referencingWardsCount,
        long referencingDepartmentsCount,
        long referencingIssuesCount
) {}
