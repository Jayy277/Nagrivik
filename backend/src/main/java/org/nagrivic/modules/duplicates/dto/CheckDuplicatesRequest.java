package org.nagrivic.modules.duplicates.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CheckDuplicatesRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    String description,

    @NotNull(message = "Category ID is required")
    UUID categoryId,

    UUID locationId,

    Double latitude,

    Double longitude,

    Double radiusMeters
) {
    public CheckDuplicatesRequest(String title, String description, UUID categoryId, UUID locationId) {
        this(title, description, categoryId, locationId, null, null, null);
    }
}
