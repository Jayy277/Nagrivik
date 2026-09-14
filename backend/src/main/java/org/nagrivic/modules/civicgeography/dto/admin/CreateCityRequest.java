package org.nagrivic.modules.civicgeography.dto.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCityRequest(
        @NotBlank(message = "City name is required")
        String name,

        @NotBlank(message = "State is required")
        String state,

        String countryCode,
        UUID civicBodyId,
        String source,
        String sourceUrl
) {}
