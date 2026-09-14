package org.nagrivic.modules.civicgeography.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID civicBodyId,
        String civicBodyName,
        String name,
        String code,
        String description,
        String source,
        String sourceUrl,
        Instant lastVerifiedAt,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        long referencingIssuesCount,
        long categoryMappingsCount,
        long wardMappingsCount
) {}
