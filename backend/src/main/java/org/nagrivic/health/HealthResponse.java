package org.nagrivic.health;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
    String status,
    String service,
    String database
) {
    public HealthResponse(String status, String service) {
        this(status, service, null);
    }
}
