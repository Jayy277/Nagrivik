package org.nagrivic.modules.civicgeography.dto.admin;

public record UpdateCategoryDepartmentMappingRequest(
        Boolean isActive,
        String source,
        String sourceUrl,
        Long version
) {}
