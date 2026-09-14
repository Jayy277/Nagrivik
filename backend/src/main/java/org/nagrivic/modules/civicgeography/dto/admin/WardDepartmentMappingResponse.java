package org.nagrivic.modules.civicgeography.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record WardDepartmentMappingResponse(
        UUID id,
        UUID wardId,
        String wardNumber,
        String wardName,
        String wardCode,
        UUID cityId,
        String cityName,
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
