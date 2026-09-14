package org.nagrivic.modules.accountability.dto;

import java.util.UUID;

public record AccountabilityFilter(
        UUID cityId,
        UUID wardId,
        UUID categoryId,
        String range
) {
    public AccountabilityFilter {
        if (range == null || range.isBlank()) {
            range = "30d";
        }
    }
}
