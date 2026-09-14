package org.nagrivic.modules.accountability.dto;

public record AgingBreakdownDto(
        long zeroToOneDay,
        long twoToSevenDays,
        long eightToThirtyDays,
        long thirtyOneToNinetyDays,
        long overNinetyDays
) {}
