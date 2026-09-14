package org.nagrivic.modules.media.ai.provider;

import org.nagrivic.modules.media.ai.model.*;

import java.util.Collections;
import java.util.List;

public record ImageUnderstandingResult(
    ImageAnalysisStatus status,
    CivicVisualCategory likelyCategory,
    int categoryConfidence,
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
    String modelVersion,
    String calculationVersion,
    String errorMessage
) {
    public static ImageUnderstandingResult disabled(String calculationVersion) {
        return new ImageUnderstandingResult(
                ImageAnalysisStatus.UNAVAILABLE,
                null,
                0,
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                CivicRelevance.UNCERTAIN,
                Collections.emptyList(),
                VisualSafetyConcern.NONE,
                false,
                null,
                "DISABLED",
                "none",
                "none",
                calculationVersion != null ? calculationVersion : "image-understanding-v1",
                "AI image understanding is disabled"
        );
    }

    public static ImageUnderstandingResult failed(String provider, String model, String version, String error) {
        return new ImageUnderstandingResult(
                ImageAnalysisStatus.FAILED,
                null,
                0,
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                CivicRelevance.UNCERTAIN,
                Collections.emptyList(),
                VisualSafetyConcern.NONE,
                false,
                null,
                provider != null ? provider : "UNKNOWN",
                model != null ? model : "none",
                version != null ? version : "v1",
                "image-understanding-v1",
                error
        );
    }
}
