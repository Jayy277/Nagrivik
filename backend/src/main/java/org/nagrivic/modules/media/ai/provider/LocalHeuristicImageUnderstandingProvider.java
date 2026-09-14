package org.nagrivic.modules.media.ai.provider;

import org.nagrivic.modules.media.ai.model.*;

import java.util.*;

public class LocalHeuristicImageUnderstandingProvider implements ImageUnderstandingProvider {

    private final String modelName;
    private final String modelVersion;
    private final String calculationVersion;

    public LocalHeuristicImageUnderstandingProvider(String modelName, String modelVersion, String calculationVersion) {
        this.modelName = modelName != null ? modelName : "heuristic-vision-v1";
        this.modelVersion = modelVersion != null ? modelVersion : "1.0.0";
        this.calculationVersion = calculationVersion != null ? calculationVersion : "image-understanding-v1";
    }

    @Override
    public String getProviderName() {
        return "LOCAL_HEURISTIC";
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ImageUnderstandingResult analyze(ImageUnderstandingRequest request) {
        if (request == null) {
            return ImageUnderstandingResult.failed(getProviderName(), getModelName(), modelVersion, "Null request");
        }

        // 1. Image Quality Assessment
        ImageQuality quality = ImageQuality.GOOD;
        List<QualityIssue> qualityIssues = new ArrayList<>();

        if (request.imageBytes() == null || request.imageBytes().length < 50) {
            quality = ImageQuality.POOR;
            qualityIssues.add(QualityIssue.INSUFFICIENT_CONTEXT);
        } else if (request.imageBytes().length < 1024) {
            quality = ImageQuality.ACCEPTABLE;
        }

        // 2. Text/Context Inspection for Semantic Heuristic
        String text = ((request.issueTitle() != null ? request.issueTitle() : "") + " "
                + (request.issueDescription() != null ? request.issueDescription() : "")).toLowerCase();

        // 3. Category & Problem Type Classification
        CivicVisualCategory likelyCategory;
        int confidence;
        List<VisualProblemType> problems = new ArrayList<>();
        List<VisualSeveritySignal> severitySignals = new ArrayList<>();
        VisualSafetyConcern safetyConcern = VisualSafetyConcern.NONE;
        CivicRelevance relevance = CivicRelevance.LIKELY_RELEVANT;

        if (text.contains("cat") || text.contains("selfie") || text.contains("screenshot") || text.contains("movie")) {
            likelyCategory = null;
            confidence = 20;
            relevance = CivicRelevance.LIKELY_IRRELEVANT;
            quality = ImageQuality.POOR;
            qualityIssues.add(QualityIssue.INSUFFICIENT_CONTEXT);
        } else if (text.contains("pothole") || text.contains("crater") || text.contains("road") || text.contains("asphalt") || text.contains("crack")) {
            likelyCategory = CivicVisualCategory.ROADS_POTHOLES;
            confidence = 88;
            problems.add(VisualProblemType.POTHOLE);
            problems.add(VisualProblemType.DAMAGED_SURFACE);
            if (text.contains("crater") || text.contains("deep") || text.contains("danger") || text.contains("huge")) {
                severitySignals.add(VisualSeveritySignal.DEEP_POTHOLE);
                severitySignals.add(VisualSeveritySignal.POTENTIAL_SAFETY_HAZARD);
                safetyConcern = VisualSafetyConcern.HIGH;
            } else {
                safetyConcern = VisualSafetyConcern.POSSIBLE;
            }
        } else if (text.contains("garbage") || text.contains("waste") || text.contains("dump") || text.contains("trash") || text.contains("bin")) {
            likelyCategory = CivicVisualCategory.GARBAGE;
            confidence = 85;
            problems.add(VisualProblemType.GARBAGE_PILE);
            if (text.contains("bin")) {
                problems.add(VisualProblemType.OVERFLOWING_BIN);
            }
            if (text.contains("huge") || text.contains("large") || text.contains("pile")) {
                severitySignals.add(VisualSeveritySignal.LARGE_WASTE_ACCUMULATION);
            }
            safetyConcern = VisualSafetyConcern.POSSIBLE;
        } else if (text.contains("light") || text.contains("lamp") || text.contains("pole") || text.contains("dark") || text.contains("bulb")) {
            likelyCategory = CivicVisualCategory.STREETLIGHTS;
            confidence = 82;
            problems.add(VisualProblemType.DAMAGED_LIGHT_POLE);
            problems.add(VisualProblemType.BROKEN_LAMP);
            safetyConcern = VisualSafetyConcern.POSSIBLE;
        } else if (text.contains("drain") || text.contains("drainage") || text.contains("sewer") || text.contains("manhole") || text.contains("overflow")) {
            likelyCategory = CivicVisualCategory.DRAINAGE;
            confidence = 86;
            problems.add(VisualProblemType.BLOCKED_DRAIN);
            problems.add(VisualProblemType.OVERFLOWING_DRAIN);
            if (text.contains("flood") || text.contains("waterlog")) {
                severitySignals.add(VisualSeveritySignal.FLOODING);
                severitySignals.add(VisualSeveritySignal.BLOCKED_DRAINAGE);
                safetyConcern = VisualSafetyConcern.HIGH;
            } else {
                safetyConcern = VisualSafetyConcern.POSSIBLE;
            }
        } else if (text.contains("water") || text.contains("leak") || text.contains("pipeline") || text.contains("burst")) {
            likelyCategory = CivicVisualCategory.WATER;
            confidence = 84;
            problems.add(VisualProblemType.WATER_LEAKAGE);
            problems.add(VisualProblemType.PIPELINE_ISSUE);
            safetyConcern = VisualSafetyConcern.POSSIBLE;
        } else {
            // Fallback to category slug if provided
            CivicVisualCategory catFromSlug = CivicVisualCategory.fromSlug(request.issueCategorySlug());
            likelyCategory = catFromSlug != null ? catFromSlug : CivicVisualCategory.ROADS_POTHOLES;
            confidence = 65;
            problems.add(VisualProblemType.UNSPECIFIED_CIVIC_DEFECT);
        }

        String categoryName = likelyCategory != null ? likelyCategory.getDisplayName() : "Uncertain";
        String summary = "Visual analysis indicates " + categoryName + " with " + quality.name().toLowerCase() + " image quality.";

        return new ImageUnderstandingResult(
                ImageAnalysisStatus.COMPLETED,
                likelyCategory,
                confidence,
                problems,
                quality,
                qualityIssues,
                relevance,
                severitySignals,
                safetyConcern,
                false, // sensitiveVisualContentDetected
                summary,
                getProviderName(),
                getModelName(),
                modelVersion,
                calculationVersion,
                null
        );
    }
}
