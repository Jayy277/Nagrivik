package org.nagrivic.modules.civicgeography.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit record tracking administrative changes to civic geography,
 * departments, and responsibility mappings.
 */
@Entity
@Table(name = "civic_geography_audits", indexes = {
        @Index(name = "idx_civic_geo_audits_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_civic_geo_audits_actor", columnList = "actor_id"),
        @Index(name = "idx_civic_geo_audits_created", columnList = "created_at")
})
public class CivicGeographyAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity actor;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "previous_state", columnDefinition = "TEXT")
    private String previousState;

    @Column(name = "new_state", columnDefinition = "TEXT")
    private String newState;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CivicGeographyAuditEntity() {
    }

    public CivicGeographyAuditEntity(
            UserEntity actor,
            String entityType,
            UUID entityId,
            String action,
            String previousState,
            String newState,
            String reason,
            String source,
            String sourceUrl
    ) {
        this.actor = actor;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.reason = reason;
        this.source = source;
        this.sourceUrl = sourceUrl;
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

    public void setId(UUID id) {
        this.id = id;
    }

    public UserEntity getActor() {
        return actor;
    }

    public void setActor(UserEntity actor) {
        this.actor = actor;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
