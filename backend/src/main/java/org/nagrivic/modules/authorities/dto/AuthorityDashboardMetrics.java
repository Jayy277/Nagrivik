package org.nagrivic.modules.authorities.dto;

import java.util.List;

public record AuthorityDashboardMetrics(
        long totalScopedIssues,
        long reportedCount,
        long verifiedCount,
        long acknowledgedCount,
        long inProgressCount,
        long resolvedCount,
        long citizenVerifiedCount,
        long notFixedCount,
        long highPriorityCount,
        long criticalPriorityCount,
        long actionableCount,
        List<AuthorityScopeDto> activeAssignments
) {
    public List<AuthorityScopeDto> assignedScopes() {
        return activeAssignments;
    }

    public List<AuthorityScopeDto> getAssignedScopes() {
        return activeAssignments;
    }
}

