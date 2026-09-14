package org.nagrivic.modules.media.ai.dto;

import org.nagrivic.modules.media.ai.entity.ImageAiAnalysisEntity;
import org.nagrivic.modules.media.ai.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImageAiAnalysisResponse(
    UUID id,
    UUID mediaId,
    UUID issueId,
    ImageAnalysisStatus status,
    CivicVisualCategory likelyCategory,
    String likelyCategorySlug,
    String likelyCategoryDisplayName,
    Integer categoryConfidence,
    List<VisualProblemType> visualProblemTypes,
    ImageQuality imageQuality,
    List<QualityIssue> qualityIssues,
    CivicRelevance relevance,
    List<VisualSeveritySignal> visualSeveritySignals,
    VisualSafetyConcern safetyConcern,
    boolean sensitiveVisualContentDetected,
    String summary,
    String provider,
    String model,
    String calculationVersion,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt
) {
    public static ImageAiAnalysisResponse fromEntity(
            ImageAiAnalysisEntity entity,
            List<VisualProblemType> problemTypes,
            List<QualityIssue> qualityIssues,
            List<VisualSeveritySignal> severitySignals
    ) {
        CivicVisualCategory cat = entity.getLikelyCategory();
        return new ImageAiAnalysisResponse(
                entity.getId(),
                entity.getMedia().getId(),
                entity.getIssue().getId(),
                entity.getStatus(),
                cat,
                cat != null ? cat.getSlug() : null,
                cat != null ? cat.getDisplayName() : null,
                entity.getCategoryConfidence(),
                problemTypes != null ? problemTypes : List.of(),
                entity.getImageQuality(),
                qualityIssues != null ? qualityIssues : List.of(),
                entity.getRelevance(),
                severitySignals != null ? severitySignals : List.of(),
                entity.getSafetyConcern(),
                entity.isSensitiveVisualContentDetected(),
                entity.getSummary(),
                entity.getProvider(),
                entity.getModel(),
                entity.getCalculationVersion(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
