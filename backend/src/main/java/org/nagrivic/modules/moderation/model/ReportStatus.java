package org.nagrivic.modules.moderation.model;

/**
 * State lifecycle of a moderation report.
 */
public enum ReportStatus {
    /**
     * Report submitted by a citizen and waiting in the moderation queue.
     */
    OPEN,

    /**
     * Assigned or currently under review by a moderator.
     */
    IN_REVIEW,

    /**
     * Moderator investigated and took a moderation action.
     */
    RESOLVED,

    /**
     * Moderator evaluated the report and determined no violation occurred.
     */
    DISMISSED
}
