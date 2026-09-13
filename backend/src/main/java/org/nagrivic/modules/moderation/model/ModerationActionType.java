package org.nagrivic.modules.moderation.model;

/**
 * Controlled moderation actions recorded in the immutable audit trail.
 */
public enum ModerationActionType {
    NO_ACTION,
    HIDE_CONTENT,
    RESTORE_CONTENT,
    REMOVE_COMMENT,
    RESTRICT_USER,
    UNRESTRICT_USER
}
