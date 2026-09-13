package org.nagrivic.modules.moderation.model;

/**
 * Moderation state for a user account.
 * Restricts active contributions (issues, comments, supports, reports) while preserving read-only access.
 */
public enum UserModerationStatus {
    /**
     * Normal active standing.
     */
    ACTIVE,

    /**
     * Temporarily or conditionally restricted from contributing.
     */
    RESTRICTED
}
