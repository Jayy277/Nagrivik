package org.nagrivic.modules.push.service;

import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.nagrivic.modules.notifications.entity.NotificationPreferenceEntity;
import org.nagrivic.modules.notifications.event.NotificationCreatedEvent;
import org.nagrivic.modules.notifications.repository.NotificationPreferenceRepository;
import org.nagrivic.modules.notifications.repository.NotificationRepository;
import org.nagrivic.modules.push.entity.PushDeliveryEntity;
import org.nagrivic.modules.push.entity.PushDeviceEntity;
import org.nagrivic.modules.push.provider.PushDeliveryResult;
import org.nagrivic.modules.push.provider.PushMessage;
import org.nagrivic.modules.push.provider.PushNotificationProvider;
import org.nagrivic.modules.push.repository.PushDeliveryRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles post-transaction push notification delivery.
 * Executes strictly AFTER_COMMIT to ensure database transactions are never
 * coupled to external network I/O or Firebase service availability.
 */
@Component
public class PushNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationDispatcher.class);

    private final PushNotificationProvider pushNotificationProvider;
    private final PushDeviceService pushDeviceService;
    private final PushDeliveryRepository pushDeliveryRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public PushNotificationDispatcher(
            PushNotificationProvider pushNotificationProvider,
            PushDeviceService pushDeviceService,
            PushDeliveryRepository pushDeliveryRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository
    ) {
        this.pushNotificationProvider = pushNotificationProvider;
        this.pushDeviceService = pushDeviceService;
        this.pushDeliveryRepository = pushDeliveryRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        if (event == null || event.notificationId() == null || event.userId() == null) {
            return;
        }

        UUID userId = event.userId();

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            log.debug("Push delivery skipped: user {} is null or inactive", userId);
            return;
        }

        // 1. Preference check
        Optional<NotificationPreferenceEntity> prefOpt = preferenceRepository.findByUser_Id(userId);
        if (prefOpt.isEmpty() || !prefOpt.get().isPushEnabled()) {
            log.debug("Push delivery skipped for user {}: push notifications not enabled in preferences", userId);
            return;
        }

        // 2. Query active devices
        List<PushDeviceEntity> devices = pushDeviceService.getActiveDevicesForUser(userId);
        if (devices.isEmpty()) {
            log.debug("Push delivery skipped for user {}: zero active devices registered", userId);
            return;
        }

        // 3. Construct privacy-safe payload
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", event.notificationId().toString());
        data.put("type", event.type() != null ? event.type().name() : "GENERAL");

        if (event.issueId() != null) {
            String issueIdStr = event.issueId().toString();
            data.put("issueId", issueIdStr);
            data.put("deepLink", "nagrivicapp://issue/" + issueIdStr);
        } else {
            data.put("deepLink", "nagrivicapp://notifications");
        }

        String title = event.title();
        String body = event.body();

        NotificationEntity notifRef = notificationRepository.getReferenceById(event.notificationId());

        // 4. Dispatch to each active device
        for (PushDeviceEntity device : devices) {
            try {
                PushMessage message = new PushMessage(
                        device.getDeviceToken(),
                        title,
                        body,
                        data,
                        device.getPlatform()
                );

                PushDeliveryResult result = pushNotificationProvider.send(message);

                // If token invalid, deactivate registration
                if (result.isInvalidToken()) {
                    log.info("Deactivating invalid device token for user {}", userId);
                    pushDeviceService.markTokenInvalid(device.getDeviceToken());
                }

                // Audit push delivery
                PushDeliveryEntity delivery = new PushDeliveryEntity(
                        notifRef,
                        device,
                        result.status().name(),
                        result.providerMessageId(),
                        result.errorCode()
                );
                pushDeliveryRepository.save(delivery);

            } catch (Exception e) {
                log.error("Failed to process push delivery for device {}: {}", device.getId(), e.getMessage(), e);
            }
        }
    }
}
