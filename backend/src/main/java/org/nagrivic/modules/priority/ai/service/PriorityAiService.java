package org.nagrivic.modules.priority.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.activity.service.IssueActivityService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.media.ai.entity.ImageAiAnalysisEntity;
import org.nagrivic.modules.media.ai.model.ImageAnalysisStatus;
import org.nagrivic.modules.media.ai.model.VisualSafetyConcern;
import org.nagrivic.modules.media.ai.repository.ImageAiAnalysisRepository;
import org.nagrivic.modules.priority.ai.dto.AiPriorityRecommendationResponse;
import org.nagrivic.modules.priority.ai.dto.PriorityAiRequest;
import org.nagrivic.modules.priority.ai.dto.PriorityAiResult;
import org.nagrivic.modules.priority.ai.entity.IssueAiPriorityEntity;
import org.nagrivic.modules.priority.ai.model.PriorityAiStatus;
import org.nagrivic.modules.priority.ai.model.PriorityInfluenceMode;
import org.nagrivic.modules.priority.ai.provider.DisabledPriorityAiProvider;
import org.nagrivic.modules.priority.ai.provider.LocalHeuristicPriorityAiProvider;
import org.nagrivic.modules.priority.ai.provider.PriorityAiProvider;
import org.nagrivic.modules.priority.ai.repository.IssueAiPriorityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class PriorityAiService {

    private static final Logger log = LoggerFactory.getLogger(PriorityAiService.class);

    private final IssueAiPriorityRepository priorityRepository;
    private final IssueRepository issueRepository;
    private final ImageAiAnalysisRepository imageAiAnalysisRepository;
    private final IssueActivityService issueActivityService;
    private final ObjectMapper objectMapper;

    @Value("${nagrivic.ai.priority.enabled:false}")
    private boolean enabled;

    @Value("${nagrivic.ai.priority.provider:LOCAL_HEURISTIC}")
    private String providerType;

    @Value("${nagrivic.ai.priority.model:heuristic-priority-v1}")
    private String modelName;

    @Value("${nagrivic.ai.priority.model-version:1.0.0}")
    private String modelVersion;

    @Value("${nagrivic.ai.priority.calculation-version:priority-assistance-v1}")
    private String calculationVersion;

    @Value("${nagrivic.ai.priority.timeout-ms:4000}")
    private long timeoutMs;

    @Value("${nagrivic.ai.priority.confidence-threshold:70}")
    private int confidenceThreshold;

    @Value("${nagrivic.ai.priority.influence-mode:ADVISORY}")
    private String influenceModeStr;

    private PriorityInfluenceMode influenceMode = PriorityInfluenceMode.ADVISORY;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public PriorityAiService(
            IssueAiPriorityRepository priorityRepository,
            IssueRepository issueRepository,
            ImageAiAnalysisRepository imageAiAnalysisRepository,
            IssueActivityService issueActivityService,
            ObjectMapper objectMapper
    ) {
        this.priorityRepository = priorityRepository;
        this.issueRepository = issueRepository;
        this.imageAiAnalysisRepository = imageAiAnalysisRepository;
        this.issueActivityService = issueActivityService;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public PriorityInfluenceMode getInfluenceMode() {
        if (influenceModeStr != null && influenceModeStr.equalsIgnoreCase("BLENDED")) {
            return PriorityInfluenceMode.BLENDED;
        }
        return influenceMode;
    }

    public void setInfluenceMode(PriorityInfluenceMode influenceMode) {
        this.influenceMode = influenceMode;
    }

    public int getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(int confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public PriorityAiProvider getProvider() {
        if (!enabled) {
            return new DisabledPriorityAiProvider(calculationVersion);
        }
        return new LocalHeuristicPriorityAiProvider(modelName, modelVersion, calculationVersion);
    }

    /**
     * Trigger an AI priority assessment for an issue.
     */
    @Transactional
    public AiPriorityRecommendationResponse assessPriority(UUID issueId, UUID actorId, String actorRole) {
        if (issueId == null) {
            throw new IllegalArgumentException("Issue ID cannot be null");
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        validateAccess(issue, actorId, actorRole);

        // Resolve canonical issue if duplicate
        IssueEntity canonical = issue.isDuplicate() && issue.getDuplicateOf() != null
                ? issue.getDuplicateOf()
                : issue;

        Optional<IssueAiPriorityEntity> existing = priorityRepository.findByIssue_Id(canonical.getId());

        if (!enabled) {
            IssueAiPriorityEntity analysis = existing.orElseGet(() -> new IssueAiPriorityEntity(
                    canonical, "DISABLED", "none", "none", calculationVersion, PriorityAiStatus.UNAVAILABLE
            ));
            analysis.setStatus(PriorityAiStatus.UNAVAILABLE);
            analysis.setFailureReason("AI priority assistance is disabled in server configuration");
            analysis.setCompletedAt(Instant.now());
            return toResponse(priorityRepository.save(analysis));
        }

        // 1. Collect safe civic context and Task 47 structured signals (if available)
        List<String> visualProblems = new ArrayList<>();
        List<String> visualSeveritySignals = new ArrayList<>();
        VisualSafetyConcern safetyConcern = VisualSafetyConcern.NONE;

        List<ImageAiAnalysisEntity> imgAnalyses = imageAiAnalysisRepository.findByIssue_Id(canonical.getId());
        for (ImageAiAnalysisEntity imgAnalysis : imgAnalyses) {
            if (imgAnalysis.getStatus() == ImageAnalysisStatus.COMPLETED) {
                visualProblems.addAll(parseJsonList(imgAnalysis.getVisualProblemTypes()));
                visualSeveritySignals.addAll(parseJsonList(imgAnalysis.getVisualSeveritySignals()));
                if (imgAnalysis.getSafetyConcern() != null) {
                    if (imgAnalysis.getSafetyConcern() == VisualSafetyConcern.HIGH) {
                        safetyConcern = VisualSafetyConcern.HIGH;
                    } else if (safetyConcern != VisualSafetyConcern.HIGH && imgAnalysis.getSafetyConcern() == VisualSafetyConcern.POSSIBLE) {
                        safetyConcern = VisualSafetyConcern.POSSIBLE;
                    }
                }
            }
        }

        String categorySlug = null;
        String categoryName = null;
        if (canonical.getCategory() != null) {
            try {
                categorySlug = canonical.getCategory().getSlug();
                categoryName = canonical.getCategory().getName();
            } catch (Exception ignored) {}
        }

        // 2. Build sanitized request (Zero PII!)
        PriorityAiRequest request = new PriorityAiRequest(
                canonical.getId(),
                canonical.getTitle(),
                canonical.getDescription(),
                categorySlug,
                categoryName,
                canonical.getSeverity(),
                canonical.getPublicImpact(),
                canonical.getSafetyImpact(),
                visualProblems,
                visualSeveritySignals,
                safetyConcern
        );

        // 3. Asynchronously execute analysis with timeout outside DB transaction
        PriorityAiProvider provider = getProvider();
        PriorityAiResult result;

        Future<PriorityAiResult> future = executor.submit(() -> provider.analyze(request));
        try {
            result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            log.warn("AI priority assessment timed out after {}ms for issue {}", timeoutMs, canonical.getId());
            future.cancel(true);
            result = PriorityAiResult.failed(provider.getProviderName(), provider.getModelName(), modelVersion, calculationVersion, "Analysis timed out");
        } catch (Exception e) {
            log.error("AI priority assessment failed safely for issue {}: {}", canonical.getId(), e.getMessage());
            result = PriorityAiResult.failed(provider.getProviderName(), provider.getModelName(), modelVersion, calculationVersion, e.getMessage());
        }

        final PriorityAiResult finalResult = result;

        // 4. Persist structured assessment
        IssueAiPriorityEntity entity = existing.orElseGet(() -> new IssueAiPriorityEntity(
                canonical, finalResult.provider(), finalResult.model(), finalResult.modelVersion(), finalResult.calculationVersion(), finalResult.status()
        ));

        entity.setProvider(finalResult.provider());
        entity.setModel(finalResult.model());
        entity.setModelVersion(finalResult.modelVersion());
        entity.setCalculationVersion(finalResult.calculationVersion());
        entity.setStatus(finalResult.status());
        entity.setSuggestedSeverity(finalResult.suggestedSeverity());
        entity.setSuggestedImpact(finalResult.suggestedImpact());
        entity.setSuggestedSafety(finalResult.suggestedSafety());
        entity.setConfidence(finalResult.confidence());
        entity.setSeverityConfidence(finalResult.severityConfidence());
        entity.setImpactConfidence(finalResult.impactConfidence());
        entity.setSafetyConfidence(finalResult.safetyConfidence());
        entity.setSignals(toJson(finalResult.signals()));
        entity.setFailureReason(finalResult.failureReason());
        entity.setCompletedAt(Instant.now());

        IssueAiPriorityEntity saved = priorityRepository.save(entity);

        // Record activity log
        if (saved.getStatus() == PriorityAiStatus.COMPLETED) {
            issueActivityService.recordActivity(
                    canonical,
                    IssueActivityType.PRIORITY_AI_ASSESSED,
                    null,
                    Map.of(
                            "provider", saved.getProvider(),
                            "confidence", saved.getConfidence() != null ? saved.getConfidence() : 0,
                            "suggestedSeverity", saved.getSuggestedSeverity() != null ? saved.getSuggestedSeverity() : 0,
                            "suggestedImpact", saved.getSuggestedImpact() != null ? saved.getSuggestedImpact() : 0,
                            "suggestedSafety", saved.getSuggestedSafety() != null ? saved.getSuggestedSafety() : 0
                    )
            );
        }

        return toResponse(saved);
    }

    /**
     * Retrieve an existing AI priority recommendation for an issue.
     */
    @Transactional(readOnly = true)
    public AiPriorityRecommendationResponse getRecommendation(UUID issueId, UUID actorId, String actorRole) {
        if (issueId == null) {
            throw new IllegalArgumentException("Issue ID cannot be null");
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        validateAccess(issue, actorId, actorRole);

        IssueEntity canonical = issue.isDuplicate() && issue.getDuplicateOf() != null
                ? issue.getDuplicateOf()
                : issue;

        IssueAiPriorityEntity entity = priorityRepository.findByIssue_Id(canonical.getId())
                .orElseThrow(() -> new ResourceNotFoundException("IssueAiPriority", canonical.getId()));

        return toResponse(entity);
    }

    /**
     * Retrieve existing recommendation entity internally for priority calculation.
     */
    @Transactional(readOnly = true)
    public Optional<IssueAiPriorityEntity> getExistingRecommendation(UUID issueId) {
        return priorityRepository.findByIssue_Id(issueId);
    }

    /**
     * Compute safe blended score if enabled and eligible.
     * Guaranteed invariants:
     * - Total AI contribution capped at max ±10 points.
     * - Severity adjustment clamped to max ±5.
     * - Impact adjustment clamped to max ±4.
     * - Safety adjustment clamped to max ±4.
     * - Total score clamped between 0 and 100.
     * - AI alone can NEVER escalate a non-critical baseline (<75) into CRITICAL (>=75).
     */
    public int calculateBlendedScore(
            int deterministicScore,
            int baseSeverity,
            int baseImpact,
            int baseSafety,
            IssueAiPriorityEntity recommendation
    ) {
        if (getInfluenceMode() != PriorityInfluenceMode.BLENDED || !enabled || recommendation == null) {
            return deterministicScore;
        }

        if (recommendation.getStatus() != PriorityAiStatus.COMPLETED) {
            return deterministicScore;
        }

        if (recommendation.getConfidence() == null || recommendation.getConfidence() < confidenceThreshold) {
            return deterministicScore;
        }

        if (recommendation.getSuggestedSeverity() == null || recommendation.getSuggestedImpact() == null || recommendation.getSuggestedSafety() == null) {
            return deterministicScore;
        }

        // Bounded adjustments
        int adjSeverity = Math.min(5, Math.max(-5, recommendation.getSuggestedSeverity() - baseSeverity));
        int adjImpact = Math.min(4, Math.max(-4, recommendation.getSuggestedImpact() - baseImpact));
        int adjSafety = Math.min(4, Math.max(-4, recommendation.getSuggestedSafety() - baseSafety));

        int totalAdjustment = Math.min(10, Math.max(-10, adjSeverity + adjImpact + adjSafety));
        int blendedScore = Math.min(100, Math.max(0, deterministicScore + totalAdjustment));

        // ANTI-CRITICAL SAFEGUARD: AI alone cannot force CRITICAL
        if (deterministicScore < 75 && blendedScore >= 75) {
            blendedScore = 74; // Cap at HIGH maximum
        }

        recommendation.setAppliedToCalculation(true);
        return blendedScore;
    }

    private void validateAccess(IssueEntity issue, UUID actorId, String actorRole) {
        if (actorRole == null) {
            throw new AccessDeniedException("Authentication required to access priority AI analysis");
        }
        if (isPrivilegedRole(actorRole)) {
            return;
        }
        if (issue.getReporter() != null && issue.getReporter().getId().equals(actorId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access priority analysis for this issue");
    }

    private boolean isPrivilegedRole(String role) {
        return "OFFICER".equalsIgnoreCase(role)
                || "AUTHORITY".equalsIgnoreCase(role)
                || "MODERATOR".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "ROLE_OFFICER".equalsIgnoreCase(role)
                || "ROLE_AUTHORITY".equalsIgnoreCase(role)
                || "ROLE_MODERATOR".equalsIgnoreCase(role)
                || "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON string list: {}", e.getMessage());
            return List.of();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    public AiPriorityRecommendationResponse toResponse(IssueAiPriorityEntity entity) {
        return new AiPriorityRecommendationResponse(
                entity.getId(),
                entity.getIssue().getId(),
                entity.getProvider(),
                entity.getModel(),
                entity.getModelVersion(),
                entity.getCalculationVersion(),
                entity.getStatus(),
                entity.getSuggestedSeverity(),
                entity.getSuggestedImpact(),
                entity.getSuggestedSafety(),
                entity.getConfidence(),
                entity.getSeverityConfidence(),
                entity.getImpactConfidence(),
                entity.getSafetyConfidence(),
                parseJsonList(entity.getSignals()),
                entity.getFailureReason(),
                entity.isAppliedToCalculation(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
