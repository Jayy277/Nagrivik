package org.nagrivic.modules.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestrictUserRequest(
        Long durationMinutes,

        @NotBlank(message = "Restriction reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
