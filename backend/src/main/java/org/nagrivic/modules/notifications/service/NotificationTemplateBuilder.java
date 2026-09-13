package org.nagrivic.modules.notifications.service;

import org.nagrivic.modules.notifications.model.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Centralized notification title and body generator for server-controlled templates.
 * Facilitates future localization (English, Gujarati, Hindi).
 */
@Component
public class NotificationTemplateBuilder {

    public record NotificationMessage(String title, String body) {}

    /**
     * Builds localized/template message based on notification type and context data.
     *
     * @param type       the notification type
     * @param isReporter true if the target recipient is the issue reporter; false if supporter
     * @param params     contextual template parameters (e.g. status, priority, department)
     * @return constructed NotificationMessage with server-controlled title and body
     */
    public NotificationMessage buildMessage(NotificationType type, boolean isReporter, Map<String, Object> params) {
        if (type == null) {
            return new NotificationMessage("Notification", "You have a new update.");
        }

        String targetContext = isReporter ? "Your reported issue" : "An issue you supported";

        return switch (type) {
            case ISSUE_STATUS_CHANGED -> {
                String statusStr = formatStatus(params != null ? (String) params.get("to") : null);
                yield new NotificationMessage(
                        "Issue status updated",
                        targetContext + " status has changed to " + statusStr + "."
                );
            }
            case ISSUE_RESOLVED -> new NotificationMessage(
                    "Issue resolved",
                    targetContext + " has been marked as resolved."
            );
            case ISSUE_NOT_FIXED -> new NotificationMessage(
                    "Issue marked as not fixed",
                    targetContext + " has been marked as not fixed and reopened."
            );
            case ISSUE_VERIFIED -> new NotificationMessage(
                    "Issue verified",
                    targetContext + " has been verified by local authorities."
            );
            case ISSUE_ACKNOWLEDGED -> new NotificationMessage(
                    "Issue acknowledged",
                    targetContext + " has been acknowledged by local authorities."
            );
            case ISSUE_IN_PROGRESS -> new NotificationMessage(
                    "Issue in progress",
                    "Work has started on " + (isReporter ? "your reported issue" : "an issue you supported") + "."
            );
            case ISSUE_CITIZEN_VERIFIED -> new NotificationMessage(
                    "Resolution confirmed",
                    "Resolution of " + (isReporter ? "your reported issue" : "an issue you supported") + " has been confirmed."
            );
            case ISSUE_RESPONSIBILITY_RESOLVED -> {
                String dept = params != null && params.containsKey("departmentName") ? (String) params.get("departmentName") : null;
                String civicBody = params != null && params.containsKey("civicBodyName") ? (String) params.get("civicBodyName") : null;

                String body;
                if (dept != null && civicBody != null) {
                    body = "Civic responsibility for your reported issue has been assigned to " + dept + " (" + civicBody + ").";
                } else if (dept != null) {
                    body = "Civic responsibility for your reported issue has been assigned to " + dept + ".";
                } else {
                    body = "Civic responsibility for your reported issue has been resolved.";
                }
                yield new NotificationMessage("Civic responsibility assigned", body);
            }
            case ISSUE_PRIORITY_CHANGED -> {
                String priorityLevel = params != null && params.containsKey("priorityLevel") ? (String) params.get("priorityLevel") : "UPDATED";
                yield new NotificationMessage(
                        "Issue priority updated",
                        "The priority level for your reported issue is now " + priorityLevel + "."
                );
            }
            case ISSUE_COMMENT_ACTIVITY -> new NotificationMessage(
                    "New comment on your issue",
                    "A new comment was added to your reported issue."
            );
            case ISSUE_DUPLICATE_DETECTED -> new NotificationMessage(
                    "Duplicate issue linked",
                    "Your reported issue was linked as a duplicate of an existing primary report."
            );
        };
    }

    private String formatStatus(String statusRaw) {
        if (statusRaw == null || statusRaw.isBlank()) {
            return "Updated";
        }
        String formatted = statusRaw.replace("_", " ").toLowerCase();
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }
}
