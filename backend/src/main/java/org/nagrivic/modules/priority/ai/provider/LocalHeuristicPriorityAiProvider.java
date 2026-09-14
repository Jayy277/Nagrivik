package org.nagrivic.modules.priority.ai.provider;

import org.nagrivic.modules.media.ai.model.VisualSafetyConcern;
import org.nagrivic.modules.priority.ai.dto.PriorityAiRequest;
import org.nagrivic.modules.priority.ai.dto.PriorityAiResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in, high-speed heuristic provider for priority assessment.
 * Combines semantic text signals with Task 47 structured image understanding signals.
 * Operates offline with zero external dependencies and guaranteed bounded outputs.
 */
public class LocalHeuristicPriorityAiProvider implements PriorityAiProvider {

    private final String modelName;
    private final String modelVersion;
    private final String calculationVersion;

    public LocalHeuristicPriorityAiProvider(String modelName, String modelVersion, String calculationVersion) {
        this.modelName = modelName != null ? modelName : "heuristic-priority-v1";
        this.modelVersion = modelVersion != null ? modelVersion : "1.0.0";
        this.calculationVersion = calculationVersion != null ? calculationVersion : "priority-assistance-v1";
    }

    @Override
    public PriorityAiResult analyze(PriorityAiRequest request) {
        if (request == null) {
            return PriorityAiResult.failed(getProviderName(), getModelName(), modelVersion, calculationVersion, "Request is null");
        }

        List<String> signals = new ArrayList<>();

        // Start from existing baseline components if available
        int severity = request.currentSeverity() != null ? request.currentSeverity().getScore() : 15;
        int impact = request.currentImpact() != null ? request.currentImpact().getScore() : 10;
        int safety = request.currentSafety() != null ? request.currentSafety().getScore() : 5;

        int severityConf = 75;
        int impactConf = 75;
        int safetyConf = 75;
        int overallConfidence = 80;

        // 1. Incorporate Task 47 Structured Image Understanding Signals (if present)
        if (request.visualSeveritySignals() != null && !request.visualSeveritySignals().isEmpty()) {
            boolean hasHighVisualSeverity = request.visualSeveritySignals().stream().anyMatch(sig ->
                    sig.contains("DEEP") || sig.contains("OBSTRUCTION") || sig.contains("FLOODING") || sig.contains("HAZARD")
            );
            if (hasHighVisualSeverity) {
                severity = Math.max(severity, 25);
                severityConf = 90;
                signals.add("High visual severity hazard detected via Task 47 image analysis");
            } else {
                severity = Math.max(severity, 18);
                signals.add("Visual defect confirmed by image analysis");
            }
            overallConfidence += 5;
        }

        if (request.visualSafetyConcern() != null) {
            if (request.visualSafetyConcern() == VisualSafetyConcern.HIGH) {
                safety = Math.max(safety, 22);
                safetyConf = 92;
                signals.add("High visual safety concern flagged by image understanding");
                overallConfidence += 5;
            } else if (request.visualSafetyConcern() == VisualSafetyConcern.POSSIBLE) {
                safety = Math.max(safety, 15);
                safetyConf = 80;
                signals.add("Possible visual safety hazard observed in issue photo");
            }
        }

        // 2. Semantic text & civic context analysis
        String text = ((request.title() != null ? request.title() : "") + " "
                + (request.description() != null ? request.description() : "")).toLowerCase();

        if (text.length() < 15 && signals.isEmpty()) {
            overallConfidence = 50;
            signals.add("Limited report context; default baseline signals retained");
        } else {
            // Severity text patterns
            if (text.contains("crater") || text.contains("cave-in") || text.contains("deep") || text.contains("severe")
                    || text.contains("burst") || text.contains("overflow") || text.contains("massive")) {
                severity = Math.max(severity, 25);
                signals.add("Severe physical degradation keywords detected in description");
            }

            // Public impact text patterns
            if (text.contains("highway") || text.contains("main road") || text.contains("junction")
                    || text.contains("crossroad") || text.contains("market") || text.contains("school")
                    || text.contains("hospital") || text.contains("circle") || text.contains("bus stand")) {
                impact = Math.max(impact, 20);
                impactConf = 85;
                signals.add("High public transit or pedestrian density zone context");
            }

            // Safety impact text patterns
            if (text.contains("accident") || text.contains("injury") || text.contains("danger")
                    || text.contains("electric") || text.contains("wire") || text.contains("spark")
                    || text.contains("open manhole") || text.contains("flood") || text.contains("hazard")) {
                safety = Math.max(safety, 20);
                safetyConf = 88;
                signals.add("Immediate public safety hazard keywords identified");
            }
        }

        if (signals.isEmpty()) {
            signals.add("Standard civic defect characteristics; baseline confirmed");
        }

        overallConfidence = Math.min(95, overallConfidence);

        return PriorityAiResult.completed(
                severity,
                impact,
                safety,
                overallConfidence,
                severityConf,
                impactConf,
                safetyConf,
                signals,
                getProviderName(),
                getModelName(),
                modelVersion,
                calculationVersion
        );
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
}
