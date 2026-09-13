package org.nagrivic.modules.civicgeography.dto;

import java.util.UUID;

public record CitySummaryDto(
        UUID id,
        String name,
        String state
) {
}
