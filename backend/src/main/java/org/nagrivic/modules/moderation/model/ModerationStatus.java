package org.nagrivic.modules.moderation.model;

/**
 * Moderation state for user-generated content (issues and comments).
 * Distinguishes content safety from factual verification.
 */
public enum ModerationStatus {
    /**
     * Normal visibility in public feeds, listings, and detail views.
     */
    VISIBLE,

    /**
     * Under moderator review; remains publicly accessible unless explicitly hidden.
     */
    UNDER_REVIEW,

    /**
     * Soft-hidden from public feeds and APIs. Preserved internally for audit history.
     */
    HIDDEN
}
