package org.nagrivic.modules.notifications.dto;

import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.nagrivic.modules.notifications.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe public user-facing notification response object.
 * Exposes only non-sensitive notification properties.
 */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String body,
        UUID issueId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse fromEntity(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getIssue() != null ? entity.getIssue().getId() : null,
                entity.isRead(),
                entity.getCreatedAt()
        );
    }
}
