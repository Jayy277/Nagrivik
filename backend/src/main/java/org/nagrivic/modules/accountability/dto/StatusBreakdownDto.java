package org.nagrivic.modules.accountability.dto;

public record StatusBreakdownDto(
        long reported,
        long verified,
        long acknowledged,
        long inProgress,
        long resolved,
        long citizenVerified,
        long notFixed
) {}
