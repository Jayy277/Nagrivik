package org.nagrivic.modules.statushistory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyResolutionRequest(
    @NotNull(message = "Field 'fixed' is required (true or false)")
    Boolean fixed,

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    String reason
) {}
