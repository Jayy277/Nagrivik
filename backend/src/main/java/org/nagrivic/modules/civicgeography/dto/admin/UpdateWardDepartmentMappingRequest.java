package org.nagrivic.modules.civicgeography.dto.admin;

public record UpdateWardDepartmentMappingRequest(
        Boolean isActive,
        String source,
        String sourceUrl,
        Long version
) {}
