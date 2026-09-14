package org.nagrivic.modules.civicgeography.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateWardRequest(
        @NotNull(message = "City ID is required")
        UUID cityId,

        UUID civicBodyId,
        String wardNumber,

        @NotBlank(message = "Ward name is required")
        String wardName,

        String wardCode,
        String boundaryGeometryWkt,
        String source,
        String sourceUrl
) {}
