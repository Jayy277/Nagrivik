package org.nagrivic.modules.issues.dto;

import jakarta.validation.constraints.NotNull;

public record LocationPayload(
    @NotNull(message = "Latitude is required")
    Double latitude,

    @NotNull(message = "Longitude is required")
    Double longitude,

    Double accuracyMeters
) {}
