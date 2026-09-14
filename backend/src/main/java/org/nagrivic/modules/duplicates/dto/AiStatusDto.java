package org.nagrivic.modules.duplicates.dto;

public record AiStatusDto(
    boolean enabled,
    String status,
    String model
) {
    public static AiStatusDto disabled() {
        return new AiStatusDto(false, "DISABLED", null);
    }

    public static AiStatusDto available(String model) {
        return new AiStatusDto(true, "AVAILABLE", model);
    }

    public static AiStatusDto timeout() {
        return new AiStatusDto(true, "TIMEOUT", null);
    }

    public static AiStatusDto unavailable(String reason) {
        return new AiStatusDto(true, reason != null ? reason : "UNAVAILABLE", null);
    }
}
