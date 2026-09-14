package org.nagrivic.modules.civicgeography.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record WardResponse(
        UUID id,
        String wardNumber,
        String wardName,
        String wardCode,
        UUID cityId,
        String cityName,
        UUID civicBodyId,
        String civicBodyName,
        boolean hasBoundary,
        String boundaryGeometryWkt,
        String source,
        String sourceUrl,
        Instant lastVerifiedAt,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        long referencingIssuesCount,
        long departmentMappingsCount
) {}
