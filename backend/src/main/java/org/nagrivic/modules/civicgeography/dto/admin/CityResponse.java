package org.nagrivic.modules.civicgeography.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record CityResponse(
        UUID id,
        String name,
        String state,
        String countryCode,
        UUID civicBodyId,
        String civicBodyName,
        String source,
        String sourceUrl,
        Instant lastVerifiedAt,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        long referencingWardsCount,
        long referencingIssuesCount
) {}
