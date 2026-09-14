package org.nagrivic.modules.push.provider.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.nagrivic.modules.push.provider.PushDeliveryResult;
import org.nagrivic.modules.push.provider.PushMessage;
import org.nagrivic.modules.push.provider.PushNotificationProvider;
import org.nagrivic.modules.push.provider.PushProviderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) implementation of PushNotificationProvider.
 */
public class FcmPushNotificationProvider implements PushNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationProvider.class);

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushNotificationProvider(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public PushDeliveryResult send(PushMessage message) {
        if (firebaseMessaging == null) {
            return PushDeliveryResult.failed("UNAVAILABLE", "FirebaseMessaging instance not configured");
        }

        if (message.deviceToken() == null || message.deviceToken().isBlank()) {
            return PushDeliveryResult.invalidToken("EMPTY_TOKEN", "Device token is blank");
        }

        String tokenPrefix = message.deviceToken().length() > 6
                ? message.deviceToken().substring(0, 6) + "..."
                : "unknown";

        try {
            // Build lock-screen notification
            Notification notification = Notification.builder()
                    .setTitle(message.title())
                    .setBody(message.body())
                    .build();

            // Prepare safe data payload (all values must be non-null strings)
            Map<String, String> safeData = new HashMap<>();
            if (message.data() != null) {
                for (Map.Entry<String, String> entry : message.data().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        safeData.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            Message fcmMessage = Message.builder()
                    .setToken(message.deviceToken())
                    .setNotification(notification)
                    .putAllData(safeData)
                    .build();

            log.debug("Sending FCM push notification to token prefix: {}", tokenPrefix);
            String messageId = firebaseMessaging.send(fcmMessage);
            log.info("FCM push delivery succeeded. MessageId: {}, Token prefix: {}", messageId, tokenPrefix);

            return PushDeliveryResult.success(messageId);

        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            String codeStr = errorCode != null ? errorCode.name() : "FCM_ERROR";
            String errorMsg = e.getMessage();

            log.warn("FCM push delivery failed for token prefix {}: [{}] {}", tokenPrefix, codeStr, errorMsg);

            if (isInvalidTokenError(errorCode)) {
                return PushDeliveryResult.invalidToken(codeStr, errorMsg);
            } else {
                return PushDeliveryResult.failed(codeStr, errorMsg);
            }
        } catch (Exception e) {
            log.error("Unexpected error during FCM push delivery to token prefix {}: {}", tokenPrefix, e.getMessage(), e);
            return PushDeliveryResult.failed("INTERNAL_ERROR", e.getMessage());
        }
    }

    private boolean isInvalidTokenError(MessagingErrorCode errorCode) {
        if (errorCode == null) {
            return false;
        }
        return errorCode == MessagingErrorCode.UNREGISTERED
                || errorCode == MessagingErrorCode.INVALID_ARGUMENT
                || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH;
    }

    @Override
    public PushProviderStatus getStatus() {
        return firebaseMessaging != null ? PushProviderStatus.CONFIGURED : PushProviderStatus.UNAVAILABLE;
    }
}
