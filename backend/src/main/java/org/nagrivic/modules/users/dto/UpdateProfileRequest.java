package org.nagrivic.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Citizen profile update payload.
 * Allows updating only user-controlled safe profile fields.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Full name cannot be blank")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName
) {
}
