package org.nagrivic.modules.duplicates.entity;

import jakarta.persistence.*;
import org.nagrivic.modules.duplicates.model.DuplicateConfidence;
import org.nagrivic.modules.duplicates.model.DuplicateSuggestionStatus;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_duplicate_suggestions")
public class AiDuplicateSuggestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private IssueEntity issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_issue_id", nullable = false)
    private IssueEntity candidateIssue;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DuplicateConfidence confidence;

    @Column(name = "signals", nullable = false, columnDefinition = "TEXT")
    private String signals;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "calculation_version", nullable = false, length = 50)
    private String calculationVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DuplicateSuggestionStatus status = DuplicateSuggestionStatus.SUGGESTED;

    @Column(name = "dismiss_reason", length = 255)
    private String dismissReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AiDuplicateSuggestionEntity() {}

    public AiDuplicateSuggestionEntity(
            IssueEntity issue,
            IssueEntity candidateIssue,
            int score,
            DuplicateConfidence confidence,
            String signals,
            String provider,
            String model,
            String calculationVersion
    ) {
        this.issue = issue;
        this.candidateIssue = candidateIssue;
        this.score = score;
        this.confidence = confidence;
        this.signals = signals != null ? signals : "[]";
        this.provider = provider;
        this.model = model;
        this.calculationVersion = calculationVersion;
        this.status = DuplicateSuggestionStatus.SUGGESTED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public IssueEntity getIssue() {
        return issue;
    }

    public void setIssue(IssueEntity issue) {
        this.issue = issue;
    }

    public IssueEntity getCandidateIssue() {
        return candidateIssue;
    }

    public void setCandidateIssue(IssueEntity candidateIssue) {
        this.candidateIssue = candidateIssue;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public DuplicateConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(DuplicateConfidence confidence) {
        this.confidence = confidence;
    }

    public String getSignals() {
        return signals;
    }

    public void setSignals(String signals) {
        this.signals = signals;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCalculationVersion() {
        return calculationVersion;
    }

    public void setCalculationVersion(String calculationVersion) {
        this.calculationVersion = calculationVersion;
    }

    public DuplicateSuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(DuplicateSuggestionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getDismissReason() {
        return dismissReason;
    }

    public void setDismissReason(String dismissReason) {
        this.dismissReason = dismissReason;
        this.updatedAt = Instant.now();
    }

    public UserEntity getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UserEntity reviewedBy) {
        this.reviewedBy = reviewedBy;
        this.updatedAt = Instant.now();
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
