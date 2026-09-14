package org.nagrivic.modules.civicgeography.dto.admin;

import java.util.UUID;

public record UpdateCityRequest(
        String name,
        String state,
        String countryCode,
        UUID civicBodyId,
        String source,
        String sourceUrl,
        Boolean isActive,
        Long version
) {}
