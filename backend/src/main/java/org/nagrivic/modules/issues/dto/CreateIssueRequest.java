package org.nagrivic.modules.issues.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for reporting a new civic issue.
 * Notice: 'reportedBy' is a temporary development-only identifier that will be replaced
 * by authenticated user identity once authentication is implemented.
 */
public record CreateIssueRequest(
    @NotBlank(message = "Title is required and must not be blank")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @NotNull(message = "Category ID is required")
    UUID categoryId,

    @NotNull(message = "Location ID is required")
    UUID locationId,

    @NotNull(message = "Reporter ID is required (temporary development-only identifier)")
    UUID reportedBy
) {}
