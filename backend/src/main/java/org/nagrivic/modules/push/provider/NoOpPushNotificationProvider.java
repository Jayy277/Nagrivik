package org.nagrivic.modules.push.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op implementation used when push notifications / FCM are disabled or unconfigured.
 */
public class NoOpPushNotificationProvider implements PushNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(NoOpPushNotificationProvider.class);

    @Override
    public PushDeliveryResult send(PushMessage message) {
        String tokenPrefix = message.deviceToken() != null && message.deviceToken().length() > 6
                ? message.deviceToken().substring(0, 6) + "..."
                : "unknown";
        log.debug("Push notification delivery skipped (FCM disabled). Token prefix: {}", tokenPrefix);
        return PushDeliveryResult.skipped("FCM push delivery is disabled");
    }

    @Override
    public PushProviderStatus getStatus() {
        return PushProviderStatus.DISABLED;
    }
}
