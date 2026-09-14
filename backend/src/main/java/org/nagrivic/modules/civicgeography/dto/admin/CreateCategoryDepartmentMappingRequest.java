package org.nagrivic.modules.civicgeography.dto.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCategoryDepartmentMappingRequest(
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotNull(message = "Department ID is required")
        UUID departmentId,

        String source,
        String sourceUrl
) {}
