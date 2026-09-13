package org.nagrivic.modules.priority.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.priority.model.PriorityLevel;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issue_priorities")
public class IssuePriorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private IssueEntity issue;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 30)
    private PriorityLevel priorityLevel;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "severity_score", nullable = false)
    private int severityScore;

    @Column(name = "impact_score", nullable = false)
    private int impactScore;

    @Column(name = "safety_score", nullable = false)
    private int safetyScore;

    @Column(name = "age_score", nullable = false)
    private int ageScore;

    @Column(name = "support_score", nullable = false)
    private int supportScore;

    @Column(name = "calculation_version", nullable = false, length = 50)
    private String calculationVersion = "v1";

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IssuePriorityEntity() {
    }

    public IssuePriorityEntity(
            IssueEntity issue,
            PriorityLevel priorityLevel,
            int score,
            int severityScore,
            int impactScore,
            int safetyScore,
            int ageScore,
            int supportScore,
            String calculationVersion
    ) {
        this.issue = issue;
        this.priorityLevel = priorityLevel;
        this.score = score;
        this.severityScore = severityScore;
        this.impactScore = impactScore;
        this.safetyScore = safetyScore;
        this.ageScore = ageScore;
        this.supportScore = supportScore;
        this.calculationVersion = calculationVersion != null ? calculationVersion : "v1";
        Instant now = Instant.now();
        this.calculatedAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.calculatedAt == null) {
            this.calculatedAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.calculationVersion == null) {
            this.calculationVersion = "v1";
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

    public IssueEntity getIssue() {
        return issue;
    }

    public void setIssue(IssueEntity issue) {
        this.issue = issue;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(int severityScore) {
        this.severityScore = severityScore;
    }

    public int getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(int impactScore) {
        this.impactScore = impactScore;
    }

    public int getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(int safetyScore) {
        this.safetyScore = safetyScore;
    }

    public int getAgeScore() {
        return ageScore;
    }

    public void setAgeScore(int ageScore) {
        this.ageScore = ageScore;
    }

    public int getSupportScore() {
        return supportScore;
    }

    public void setSupportScore(int supportScore) {
        this.supportScore = supportScore;
    }

    public String getCalculationVersion() {
        return calculationVersion;
    }

    public void setCalculationVersion(String calculationVersion) {
        this.calculationVersion = calculationVersion;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
