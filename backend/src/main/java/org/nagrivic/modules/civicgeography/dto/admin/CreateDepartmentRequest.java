package org.nagrivic.modules.civicgeography.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDepartmentRequest(
        @NotNull(message = "Civic body ID is required")
        UUID civicBodyId,

        @NotBlank(message = "Department name is required")
        String name,

        String code,
        String description,
        String source,
        String sourceUrl
) {}
