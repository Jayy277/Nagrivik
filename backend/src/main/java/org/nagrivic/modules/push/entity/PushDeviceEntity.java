package org.nagrivic.modules.push.entity;

import jakarta.persistence.*;
import org.nagrivic.modules.push.model.PlatformType;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "push_devices",
    indexes = {
        @Index(name = "idx_push_devices_user_active", columnList = "user_id, is_active"),
        @Index(name = "idx_push_devices_token", columnList = "device_token")
    }
)
public class PushDeviceEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "device_token", nullable = false, unique = true, length = 512)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private PlatformType platform;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PushDeviceEntity() {
    }

    public PushDeviceEntity(UserEntity user, String deviceToken, PlatformType platform, String appVersion) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.deviceToken = deviceToken;
        this.platform = platform;
        this.appVersion = appVersion;
        this.isActive = true;
        this.lastSeenAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
        this.updatedAt = Instant.now();
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public PlatformType getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformType platform) {
        this.platform = platform;
        this.updatedAt = Instant.now();
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = Instant.now();
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void touch() {
        this.lastSeenAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
