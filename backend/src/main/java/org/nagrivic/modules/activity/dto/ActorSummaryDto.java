package org.nagrivic.modules.activity.dto;

import java.util.UUID;

/**
 * Safe public summary of an actor who performed an issue activity.
 * Excludes phone numbers, emails, passwords, and authentication tokens.
 */
public record ActorSummaryDto(
        UUID id,
        String displayName
) {
}
