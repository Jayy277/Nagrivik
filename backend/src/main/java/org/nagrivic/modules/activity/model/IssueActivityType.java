package org.nagrivic.modules.activity.model;

/**
 * Controlled domain enumeration of meaningful issue activity lifecycle events.
 */
public enum IssueActivityType {
    ISSUE_REPORTED,
    STATUS_CHANGED,
    COMMENT_ADDED,
    COMMENT_DELETED,
    SUPPORT_ADDED,
    SUPPORT_REMOVED,
    DUPLICATE_LINKED,
    RESPONSIBILITY_RESOLVED,
    RESPONSIBILITY_RE_RESOLVED,
    PRIORITY_RECALCULATED,
    MEDIA_ADDED
}
