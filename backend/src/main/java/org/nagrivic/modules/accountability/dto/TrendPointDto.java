package org.nagrivic.modules.accountability.dto;

public record TrendPointDto(
        String date,
        long reportedCount,
        long resolvedCount,
        long citizenVerifiedCount
) {}
