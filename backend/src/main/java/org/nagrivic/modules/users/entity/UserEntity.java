package org.nagrivic.modules.users.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "profile_picture_url", length = 1024)
    private String profilePictureUrl;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "role", nullable = false, length = 32)
    private String role = "CITIZEN";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 32)
    private org.nagrivic.modules.moderation.model.UserModerationStatus moderationStatus = org.nagrivic.modules.moderation.model.UserModerationStatus.ACTIVE;

    @Column(name = "restricted_until")
    private Instant restrictedUntil;

    @Column(name = "restriction_reason", length = 500)
    private String restrictionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserEntity() {
    }

    public UserEntity(String phoneNumber, String fullName) {
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.role = "CITIZEN";
    }

    public UserEntity(String fullName, String email, String profilePictureUrl) {
        this.fullName = fullName;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.role = "CITIZEN";
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public org.nagrivic.modules.moderation.model.UserModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(org.nagrivic.modules.moderation.model.UserModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public Instant getRestrictedUntil() {
        return restrictedUntil;
    }

    public void setRestrictedUntil(Instant restrictedUntil) {
        this.restrictedUntil = restrictedUntil;
    }

    public String getRestrictionReason() {
        return restrictionReason;
    }

    public void setRestrictionReason(String restrictionReason) {
        this.restrictionReason = restrictionReason;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    /**
     * Evaluates whether the user is actively restricted by moderation.
     * If restrictedUntil is specified and in the past, the temporary restriction is considered expired.
     */
    public boolean isRestricted() {
        if (moderationStatus != org.nagrivic.modules.moderation.model.UserModerationStatus.RESTRICTED) {
            return false;
        }
        if (restrictedUntil == null) {
            return true;
        }
        return restrictedUntil.isAfter(Instant.now());
    }
}
