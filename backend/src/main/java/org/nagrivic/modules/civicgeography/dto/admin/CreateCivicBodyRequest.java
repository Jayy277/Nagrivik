package org.nagrivic.modules.civicgeography.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;

public record CreateCivicBodyRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Civic body type is required")
        CivicBodyType type,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "City is required")
        String city,

        String officialWebsite,
        String source,
        String sourceUrl
) {}
