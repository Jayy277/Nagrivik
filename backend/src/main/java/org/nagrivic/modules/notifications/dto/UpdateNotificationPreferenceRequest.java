package org.nagrivic.modules.notifications.dto;

public record UpdateNotificationPreferenceRequest(
        Boolean inAppEnabled,
        Boolean pushEnabled,
        Boolean emailEnabled,
        Boolean smsEnabled
) {
}
