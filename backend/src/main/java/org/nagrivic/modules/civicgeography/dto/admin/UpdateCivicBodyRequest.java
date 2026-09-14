package org.nagrivic.modules.civicgeography.dto.admin;

import org.nagrivic.modules.civicgeography.model.CivicBodyType;

public record UpdateCivicBodyRequest(
        String name,
        CivicBodyType type,
        String state,
        String city,
        String officialWebsite,
        String source,
        String sourceUrl,
        Boolean isActive,
        Long version
) {}
