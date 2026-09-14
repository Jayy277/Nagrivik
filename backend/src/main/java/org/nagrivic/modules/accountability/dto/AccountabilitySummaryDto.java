package org.nagrivic.modules.accountability.dto;

public record AccountabilitySummaryDto(
        long totalPublicIssues,
        long actionableCount,
        long reportedCount,
        long verifiedCount,
        long acknowledgedCount,
        long inProgressCount,
        long resolvedCount,
        long citizenVerifiedCount,
        long notFixedCount,
        long highPriorityCount,
        long criticalPriorityCount,
        long unresolvedResponsibilityCount
) {}
