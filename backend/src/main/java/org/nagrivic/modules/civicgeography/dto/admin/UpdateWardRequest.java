package org.nagrivic.modules.civicgeography.dto.admin;

import java.util.UUID;

public record UpdateWardRequest(
        UUID cityId,
        UUID civicBodyId,
        String wardNumber,
        String wardName,
        String wardCode,
        String boundaryGeometryWkt,
        String source,
        String sourceUrl,
        Boolean isActive,
        Long version
) {}
