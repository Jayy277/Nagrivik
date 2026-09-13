package org.nagrivic.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
    @NotBlank(message = "Google ID token must not be blank")
    String idToken
) {}
