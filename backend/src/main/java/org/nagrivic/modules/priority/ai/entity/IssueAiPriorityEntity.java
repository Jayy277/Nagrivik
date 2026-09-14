package org.nagrivic.modules.priority.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.priority.ai.model.PriorityAiStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issue_ai_priorities")
public class IssueAiPriorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private IssueEntity issue;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "calculation_version", nullable = false, length = 50)
    private String calculationVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PriorityAiStatus status;

    @Column(name = "suggested_severity")
    private Integer suggestedSeverity;

    @Column(name = "suggested_impact")
    private Integer suggestedImpact;

    @Column(name = "suggested_safety")
    private Integer suggestedSafety;

    private Integer confidence;

    @Column(name = "severity_confidence")
    private Integer severityConfidence;

    @Column(name = "impact_confidence")
    private Integer impactConfidence;

    @Column(name = "safety_confidence")
    private Integer safetyConfidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String signals = "[]";

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "applied_to_calculation", nullable = false)
    private boolean appliedToCalculation = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public IssueAiPriorityEntity() {}

    public IssueAiPriorityEntity(
            IssueEntity issue,
            String provider,
            String model,
            String modelVersion,
            String calculationVersion,
            PriorityAiStatus status
    ) {
        this.issue = issue;
        this.provider = provider;
        this.model = model;
        this.modelVersion = modelVersion;
        this.calculationVersion = calculationVersion;
        this.status = status;
        this.createdAt = Instant.now();
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

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getCalculationVersion() {
        return calculationVersion;
    }

    public void setCalculationVersion(String calculationVersion) {
        this.calculationVersion = calculationVersion;
    }

    public PriorityAiStatus getStatus() {
        return status;
    }

    public void setStatus(PriorityAiStatus status) {
        this.status = status;
    }

    public Integer getSuggestedSeverity() {
        return suggestedSeverity;
    }

    public void setSuggestedSeverity(Integer suggestedSeverity) {
        this.suggestedSeverity = suggestedSeverity;
    }

    public Integer getSuggestedImpact() {
        return suggestedImpact;
    }

    public void setSuggestedImpact(Integer suggestedImpact) {
        this.suggestedImpact = suggestedImpact;
    }

    public Integer getSuggestedSafety() {
        return suggestedSafety;
    }

    public void setSuggestedSafety(Integer suggestedSafety) {
        this.suggestedSafety = suggestedSafety;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Integer getSeverityConfidence() {
        return severityConfidence;
    }

    public void setSeverityConfidence(Integer severityConfidence) {
        this.severityConfidence = severityConfidence;
    }

    public Integer getImpactConfidence() {
        return impactConfidence;
    }

    public void setImpactConfidence(Integer impactConfidence) {
        this.impactConfidence = impactConfidence;
    }

    public Integer getSafetyConfidence() {
        return safetyConfidence;
    }

    public void setSafetyConfidence(Integer safetyConfidence) {
        this.safetyConfidence = safetyConfidence;
    }

    public String getSignals() {
        return signals;
    }

    public void setSignals(String signals) {
        this.signals = signals;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean isAppliedToCalculation() {
        return appliedToCalculation;
    }

    public void setAppliedToCalculation(boolean appliedToCalculation) {
        this.appliedToCalculation = appliedToCalculation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
