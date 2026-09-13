package org.nagrivic.modules.issues.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for reporting a new civic issue.
 * The reporter identity is derived server-side exclusively from the authenticated JWT.
 * Location can be supplied either via an existing locationId or via inline location coordinates.
 */
public record CreateIssueRequest(
    @NotBlank(message = "Title is required and must not be blank")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @NotNull(message = "Category ID is required")
    UUID categoryId,

    UUID locationId,

    org.nagrivic.modules.priority.model.IssueSeverity severity,

    LocationPayload location
) {
    public CreateIssueRequest(String title, String description, UUID categoryId, UUID locationId) {
        this(title, description, categoryId, locationId, null, null);
    }

    public CreateIssueRequest(String title, String description, UUID categoryId, UUID locationId, org.nagrivic.modules.priority.model.IssueSeverity severity) {
        this(title, description, categoryId, locationId, severity, null);
    }

    public CreateIssueRequest(String title, String description, UUID categoryId, LocationPayload location) {
        this(title, description, categoryId, null, null, location);
    }
}
