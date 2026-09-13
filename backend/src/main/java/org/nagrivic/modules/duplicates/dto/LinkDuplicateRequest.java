package org.nagrivic.modules.duplicates.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkDuplicateRequest(
    @NotNull(message = "Primary issue ID is required")
    UUID primaryIssueId
) {}
