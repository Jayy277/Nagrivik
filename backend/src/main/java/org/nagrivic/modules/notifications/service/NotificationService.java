package org.nagrivic.modules.notifications.service;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.notifications.dto.NotificationPreferenceResponse;
import org.nagrivic.modules.notifications.dto.NotificationResponse;
import org.nagrivic.modules.notifications.dto.UnreadCountResponse;
import org.nagrivic.modules.notifications.dto.UpdateNotificationPreferenceRequest;
import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.nagrivic.modules.notifications.model.NotificationType;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    /**
     * Internal method to persist a notification for a user with preference and idempotency protection.
     */
    NotificationEntity createNotification(
            UserEntity user,
            NotificationType type,
            String title,
            String body,
            IssueEntity issue,
            String eventKey,
            Map<String, Object> metadata
    );

    /**
     * Domain event integration: handles issue status change notification fan-out.
     */
    void handleStatusChange(IssueEntity issue, IssueStatus fromStatus, IssueStatus toStatus, UserEntity actor);

    /**
     * Domain event integration: handles civic responsibility resolution for issue reporter.
     */
    void handleResponsibilityResolved(IssueEntity issue);

    /**
     * Domain event integration: handles issue priority level changes for issue reporter.
     */
    void handlePriorityLevelChanged(IssueEntity issue, PriorityLevel newLevel);

    /**
     * Domain event integration: handles new comment added to an issue.
     */
    void handleCommentAdded(IssueEntity issue, UserEntity commenter);

    /**
     * Domain event integration: handles explicit duplicate linking of an issue.
     */
    void handleDuplicateLinked(IssueEntity sourceIssue, IssueEntity primaryIssue);

    /**
     * Retrieves paginated notifications for the currently authenticated user (newest first).
     */
    Page<NotificationResponse> getNotificationsForCurrentUser(Pageable pageable);

    /**
     * Retrieves unread notification count for the currently authenticated user.
     */
    UnreadCountResponse getUnreadCountForCurrentUser();

    /**
     * Marks a specific notification owned by the currently authenticated user as read.
     */
    NotificationResponse markAsReadForCurrentUser(UUID notificationId);

    /**
     * Marks all unread notifications owned by the currently authenticated user as read.
     */
    void markAllAsReadForCurrentUser();

    /**
     * Retrieves notification preferences for the currently authenticated user.
     */
    NotificationPreferenceResponse getPreferencesForCurrentUser();

    /**
     * Updates notification preferences for the currently authenticated user.
     */
    NotificationPreferenceResponse updatePreferencesForCurrentUser(UpdateNotificationPreferenceRequest request);
}
