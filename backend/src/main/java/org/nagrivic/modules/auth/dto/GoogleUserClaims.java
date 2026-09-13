package org.nagrivic.modules.auth.dto;

public record GoogleUserClaims(
    String subject,
    String email,
    boolean emailVerified,
    String name,
    String pictureUrl
) {}
