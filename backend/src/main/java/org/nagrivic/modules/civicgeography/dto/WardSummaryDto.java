package org.nagrivic.modules.civicgeography.dto;

import java.util.UUID;

public record WardSummaryDto(
        UUID id,
        String name,
        String number,
        String code
) {
}
