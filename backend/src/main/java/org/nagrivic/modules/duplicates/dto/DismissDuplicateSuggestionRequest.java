package org.nagrivic.modules.duplicates.dto;

import jakarta.validation.constraints.Size;

public record DismissDuplicateSuggestionRequest(
    @Size(max = 255, message = "Dismissal reason cannot exceed 255 characters")
    String reason
) {}
