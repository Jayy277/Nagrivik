package org.nagrivic.modules.notifications.event;

import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.nagrivic.modules.notifications.model.NotificationType;

import java.util.UUID;

/**
 * Domain event fired when a notification has been safely persisted to the database.
 * Uses detached immutable scalar values to ensure post-transaction event listeners
 * never encounter LazyInitializationException.
 */
public record NotificationCreatedEvent(
    UUID notificationId,
    UUID userId,
    NotificationType type,
    String title,
    String body,
    UUID issueId
) {
    public static NotificationCreatedEvent fromEntity(NotificationEntity entity) {
        return new NotificationCreatedEvent(
                entity.getId(),
                entity.getUser().getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getIssue() != null ? entity.getIssue().getId() : null
        );
    }
}
