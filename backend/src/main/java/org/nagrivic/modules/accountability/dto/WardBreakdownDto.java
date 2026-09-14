package org.nagrivic.modules.accountability.dto;

import java.util.UUID;

public record WardBreakdownDto(
        UUID wardId,
        String name,
        String wardNumber,
        String wardCode,
        long total,
        long openActionable,
        long inProgress,
        long resolved,
        long citizenVerified,
        long notFixed,
        long highOrCriticalCount
) {}
