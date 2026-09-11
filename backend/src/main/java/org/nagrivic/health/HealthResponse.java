package org.nagrivic.health;

public record HealthResponse(
    String status,
    String service
) {}
