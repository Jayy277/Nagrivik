package org.nagrivic.modules.auth.dto;

public record SendOtpResponse(
    String message,
    long expiresInSeconds
) {
    public SendOtpResponse(String message) {
        this(message, 300);
    }
}

