package org.nagrivic.modules.notifications.dto;

import org.nagrivic.modules.notifications.entity.NotificationPreferenceEntity;

public record NotificationPreferenceResponse(
        boolean inAppEnabled,
        boolean pushEnabled,
        boolean emailEnabled,
        boolean smsEnabled
) {
    public static NotificationPreferenceResponse fromEntity(NotificationPreferenceEntity entity) {
        return new NotificationPreferenceResponse(
                entity.isInAppEnabled(),
                entity.isPushEnabled(),
                entity.isEmailEnabled(),
                entity.isSmsEnabled()
        );
    }

    public static NotificationPreferenceResponse defaultPreferences() {
        return new NotificationPreferenceResponse(true, false, false, false);
    }
}
