package org.nagrivic.modules.auth.dto;

import java.util.UUID;

public record UserAuthSummary(
    UUID id,
    String phoneNumber,
    String email,
    String role,
    String fullName,
    String profilePictureUrl
) {
    public UserAuthSummary(UUID id, String phoneNumber, String role, String fullName) {
        this(id, phoneNumber, null, role, fullName, null);
    }
}
