package org.nagrivic.modules.moderation.model;

/**
 * Controlled taxonomy of reasons for content moderation reports.
 * Political disagreement or criticism of public officials is explicitly NOT a valid moderation reason.
 */
public enum ModerationReason {
    SPAM,
    ABUSIVE_OR_HARASSING,
    HATEFUL_CONTENT,
    SEXUAL_OR_EXPLICIT,
    PERSONAL_INFORMATION,
    MISLEADING_OR_MANIPULATIVE,
    DUPLICATE_CONTENT,
    IRRELEVANT,
    OTHER
}
