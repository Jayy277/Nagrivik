package org.nagrivic.modules.moderation.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.moderation.model.ModerationActionType;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit entity recording every action taken by a moderator.
 * Kept strictly separate from the public Issue Activity timeline.
 */
@Entity
@Table(name = "moderation_actions")
public class ModerationActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ModerationReportEntity report;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private ModerationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moderator_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity moderator;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64)
    private ModerationActionType action;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ModerationActionEntity() {
    }

    public ModerationActionEntity(
            ModerationReportEntity report,
            ModerationTargetType targetType,
            UUID targetId,
            UserEntity moderator,
            ModerationActionType action,
            String reason,
            String notes
    ) {
        this.report = report;
        this.targetType = targetType;
        this.targetId = targetId;
        this.moderator = moderator;
        this.action = action;
        this.reason = reason;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public ModerationReportEntity getReport() {
        return report;
    }

    public ModerationTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public UserEntity getModerator() {
        return moderator;
    }

    public ModerationActionType getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
