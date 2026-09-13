package org.nagrivic.modules.statushistory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "status_history")
public class StatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private IssueEntity issue;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private IssueStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private IssueStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private UserEntity changedByUser;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StatusHistoryEntity() {
    }

    public StatusHistoryEntity(
            IssueEntity issue,
            IssueStatus fromStatus,
            IssueStatus toStatus,
            UserEntity changedByUser,
            String reason
    ) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue cannot be null for status history");
        }
        if (toStatus == null) {
            throw new IllegalArgumentException("Target status (toStatus) cannot be null");
        }
        this.issue = issue;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUser = changedByUser;
        this.reason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : null;
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

    public IssueEntity getIssue() {
        return issue;
    }

    public void setIssue(IssueEntity issue) {
        this.issue = issue;
    }

    public IssueStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(IssueStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public IssueStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(IssueStatus toStatus) {
        this.toStatus = toStatus;
    }

    public UserEntity getChangedByUser() {
        return changedByUser;
    }

    public void setChangedByUser(UserEntity changedByUser) {
        this.changedByUser = changedByUser;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
