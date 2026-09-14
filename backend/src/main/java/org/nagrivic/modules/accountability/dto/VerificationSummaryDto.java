package org.nagrivic.modules.accountability.dto;

public record VerificationSummaryDto(
        long resolvedByAuthority,
        long citizenVerified,
        long citizenReportedNotFixed,
        long verificationPending,
        double verificationRate
) {}
