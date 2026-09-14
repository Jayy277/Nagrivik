package org.nagrivic.modules.push.entity;

import jakarta.persistence.*;
import org.nagrivic.modules.notifications.entity.NotificationEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "notification_push_deliveries",
    indexes = {
        @Index(name = "idx_push_deliveries_notification", columnList = "notification_id"),
        @Index(name = "idx_push_deliveries_device", columnList = "device_id")
    }
)
public class PushDeliveryEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private NotificationEntity notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private PushDeviceEntity device;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PushDeliveryEntity() {
    }

    public PushDeliveryEntity(NotificationEntity notification, PushDeviceEntity device, String status, String providerMessageId, String errorCode) {
        this.id = UUID.randomUUID();
        this.notification = notification;
        this.device = device;
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.errorCode = errorCode;
        this.sentAt = "SUCCESS".equalsIgnoreCase(status) ? Instant.now() : null;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public NotificationEntity getNotification() {
        return notification;
    }

    public PushDeviceEntity getDevice() {
        return device;
    }

    public String getStatus() {
        return status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
