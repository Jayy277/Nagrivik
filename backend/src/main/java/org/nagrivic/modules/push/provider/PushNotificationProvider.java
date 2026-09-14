package org.nagrivic.modules.push.provider;

/**
 * Provider-independent abstraction for sending push notifications.
 * Domain/application layers interact solely through this interface.
 */
public interface PushNotificationProvider {

    /**
     * Attempts delivery of a single push notification message.
     * Must not throw uncaught exceptions; error outcomes must be wrapped in PushDeliveryResult.
     */
    PushDeliveryResult send(PushMessage message);

    /**
     * Returns the operational readiness status of this provider.
     */
    PushProviderStatus getStatus();
}
