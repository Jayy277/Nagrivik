package org.nagrivic.modules.civicgeography.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record CategoryDepartmentMappingResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String categorySlug,
        UUID departmentId,
        String departmentName,
        String departmentCode,
        UUID civicBodyId,
        String civicBodyName,
        boolean isActive,
        String source,
        String sourceUrl,
        Instant lastVerifiedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {}
