package org.nagrivic.modules.civicgeography.dto.admin;

public record UpdateDepartmentRequest(
        String name,
        String code,
        String description,
        String source,
        String sourceUrl,
        Boolean isActive,
        Long version
) {}
