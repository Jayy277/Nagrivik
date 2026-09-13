package org.nagrivic.modules.notifications.model;

/**
 * Controlled enumeration of user-facing notification types.
 * Notifications represent personal awareness signals for relevant issue events.
 */
public enum NotificationType {
    ISSUE_STATUS_CHANGED,
    ISSUE_RESOLVED,
    ISSUE_NOT_FIXED,
    ISSUE_VERIFIED,
    ISSUE_ACKNOWLEDGED,
    ISSUE_IN_PROGRESS,
    ISSUE_CITIZEN_VERIFIED,
    ISSUE_RESPONSIBILITY_RESOLVED,
    ISSUE_PRIORITY_CHANGED,
    ISSUE_COMMENT_ACTIVITY,
    ISSUE_DUPLICATE_DETECTED
}
