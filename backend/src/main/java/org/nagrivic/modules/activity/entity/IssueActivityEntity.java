package org.nagrivic.modules.activity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "issue_activities")
public class IssueActivityEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private IssueEntity issue;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private IssueActivityType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private UserEntity actor;

    @Column(name = "event_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> eventData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IssueActivityEntity() {
    }

    public IssueActivityEntity(
            IssueEntity issue,
            IssueActivityType eventType,
            UserEntity actor,
            Map<String, Object> eventData
    ) {
        this.id = UUID.randomUUID();
        this.issue = issue;
        this.eventType = eventType;
        this.actor = actor;
        this.eventData = eventData;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public IssueEntity getIssue() {
        return issue;
    }

    public IssueActivityType getEventType() {
        return eventType;
    }

    public UserEntity getActor() {
        return actor;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
