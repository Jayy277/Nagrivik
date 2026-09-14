package org.nagrivic.modules.civicgeography.dto.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateWardDepartmentMappingRequest(
        @NotNull(message = "Ward ID is required")
        UUID wardId,

        @NotNull(message = "Department ID is required")
        UUID departmentId,

        String source,
        String sourceUrl
) {}
