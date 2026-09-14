package org.nagrivic.modules.accountability.dto;

import java.util.UUID;

public record CategoryBreakdownDto(
        UUID categoryId,
        String name,
        String slug,
        long total,
        long openActionable,
        long inProgress,
        long resolved,
        long citizenVerified,
        long notFixed
) {}
