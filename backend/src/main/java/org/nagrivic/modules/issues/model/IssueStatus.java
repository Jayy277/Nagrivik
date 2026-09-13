package org.nagrivic.modules.issues.model;

import java.util.Set;

/**
 * Lifecycle status of a civic issue.
 */
public enum IssueStatus {
    REPORTED,
    VERIFIED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    CITIZEN_VERIFIED,
    NOT_FIXED;

    /**
     * Determines whether a transition from this status to the target status is allowed.
     *
     * @param target the desired next status
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(IssueStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case REPORTED -> target == VERIFIED;
            case VERIFIED -> target == ACKNOWLEDGED;
            case ACKNOWLEDGED -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == RESOLVED;
            case RESOLVED -> target == CITIZEN_VERIFIED || target == NOT_FIXED;
            case NOT_FIXED -> target == IN_PROGRESS;
            case CITIZEN_VERIFIED -> false; // Terminal state through normal workflow
        };
    }
}
