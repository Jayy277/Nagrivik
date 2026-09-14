package org.nagrivic.modules.media.ai.entity;

import jakarta.persistence.*;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.media.ai.model.*;
import org.nagrivic.modules.media.entity.MediaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "image_ai_analysis")
public class ImageAiAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false, unique = true)
    private MediaEntity media;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
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
    private ImageAnalysisStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "likely_category", length = 50)
    private CivicVisualCategory likelyCategory;

    @Column(name = "category_confidence")
    private Integer categoryConfidence;

    @Column(name = "visual_problem_types", nullable = false, columnDefinition = "TEXT")
    private String visualProblemTypes;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_quality", length = 30)
    private ImageQuality imageQuality;

    @Column(name = "quality_issues", nullable = false, columnDefinition = "TEXT")
    private String qualityIssues;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CivicRelevance relevance;

    @Column(name = "visual_severity_signals", nullable = false, columnDefinition = "TEXT")
    private String visualSeveritySignals;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_concern", length = 30)
    private VisualSafetyConcern safetyConcern;

    @Column(name = "sensitive_visual_content_detected", nullable = false)
    private boolean sensitiveVisualContentDetected = false;

    @Column(length = 500)
    private String summary;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ImageAiAnalysisEntity() {}

    public ImageAiAnalysisEntity(
            MediaEntity media,
            IssueEntity issue,
            String provider,
            String model,
            String modelVersion,
            String calculationVersion,
            ImageAnalysisStatus status
    ) {
        this.media = media;
        this.issue = issue;
        this.provider = provider;
        this.model = model;
        this.modelVersion = modelVersion;
        this.calculationVersion = calculationVersion;
        this.status = status;
        this.visualProblemTypes = "[]";
        this.qualityIssues = "[]";
        this.visualSeveritySignals = "[]";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public MediaEntity getMedia() {
        return media;
    }

    public void setMedia(MediaEntity media) {
        this.media = media;
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

    public ImageAnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(ImageAnalysisStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public CivicVisualCategory getLikelyCategory() {
        return likelyCategory;
    }

    public void setLikelyCategory(CivicVisualCategory likelyCategory) {
        this.likelyCategory = likelyCategory;
    }

    public Integer getCategoryConfidence() {
        return categoryConfidence;
    }

    public void setCategoryConfidence(Integer categoryConfidence) {
        this.categoryConfidence = categoryConfidence;
    }

    public String getVisualProblemTypes() {
        return visualProblemTypes;
    }

    public void setVisualProblemTypes(String visualProblemTypes) {
        this.visualProblemTypes = visualProblemTypes;
    }

    public ImageQuality getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(ImageQuality imageQuality) {
        this.imageQuality = imageQuality;
    }

    public String getQualityIssues() {
        return qualityIssues;
    }

    public void setQualityIssues(String qualityIssues) {
        this.qualityIssues = qualityIssues;
    }

    public CivicRelevance getRelevance() {
        return relevance;
    }

    public void setRelevance(CivicRelevance relevance) {
        this.relevance = relevance;
    }

    public String getVisualSeveritySignals() {
        return visualSeveritySignals;
    }

    public void setVisualSeveritySignals(String visualSeveritySignals) {
        this.visualSeveritySignals = visualSeveritySignals;
    }

    public VisualSafetyConcern getSafetyConcern() {
        return safetyConcern;
    }

    public void setSafetyConcern(VisualSafetyConcern safetyConcern) {
        this.safetyConcern = safetyConcern;
    }

    public boolean isSensitiveVisualContentDetected() {
        return sensitiveVisualContentDetected;
    }

    public void setSensitiveVisualContentDetected(boolean sensitiveVisualContentDetected) {
        this.sensitiveVisualContentDetected = sensitiveVisualContentDetected;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
