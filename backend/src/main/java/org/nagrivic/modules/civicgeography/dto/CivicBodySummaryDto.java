package org.nagrivic.modules.civicgeography.dto;

import java.util.UUID;

public record CivicBodySummaryDto(
        UUID id,
        String name,
        String type
) {
}
