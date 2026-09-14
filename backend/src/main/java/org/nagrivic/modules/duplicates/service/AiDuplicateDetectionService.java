package org.nagrivic.modules.duplicates.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nagrivic.modules.duplicates.ai.*;
import org.nagrivic.modules.duplicates.dto.AiStatusDto;
import org.nagrivic.modules.duplicates.entity.AiDuplicateSuggestionEntity;
import org.nagrivic.modules.duplicates.model.DuplicateSuggestionStatus;
import org.nagrivic.modules.duplicates.repository.AiDuplicateSuggestionRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

@Service
public class AiDuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AiDuplicateDetectionService.class);

    private final AiDuplicateSuggestionRepository suggestionRepository;
    private final IssueRepository issueRepository;
    private final ObjectMapper objectMapper;

    @Value("${nagrivic.ai.duplicates.enabled:false}")
    private boolean enabled;

    @Value("${nagrivic.ai.duplicates.provider:LOCAL_SEMANTIC}")
    private String providerType;

    @Value("${nagrivic.ai.duplicates.model:heuristic-text-v1}")
    private String modelName;

    @Value("${nagrivic.ai.duplicates.score-version:duplicate-v1}")
    private String scoreVersion;

    @Value("${nagrivic.ai.duplicates.timeout-ms:3000}")
    private long timeoutMs;

    @Value("${nagrivic.ai.duplicates.threshold:70}")
    private int suggestionThreshold;

    @Value("${nagrivic.duplicates.detection-radius-meters:100.0}")
    private double detectionRadiusMeters;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public AiDuplicateDetectionService(
            AiDuplicateSuggestionRepository suggestionRepository,
            IssueRepository issueRepository,
            ObjectMapper objectMapper
    ) {
        this.suggestionRepository = suggestionRepository;
        this.issueRepository = issueRepository;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DuplicateAiProvider getProvider() {
        if (!enabled) {
            return new DisabledDuplicateAiProvider();
        }
        return new LocalSemanticDuplicateAiProvider(modelName, scoreVersion, detectionRadiusMeters);
    }

    public AiStatusDto getAiStatus() {
        if (!enabled) {
            return AiStatusDto.disabled();
        }
        return AiStatusDto.available(modelName);
    }

    /**
     * Executes AI candidate analysis outside DB transactions with timeout protection.
     */
    public DuplicateAiAnalysisResult analyzeSafely(DuplicateAiRequest request) {
        if (!enabled) {
            return new DuplicateAiAnalysisResult("DISABLED", "none", scoreVersion, Collections.emptyList());
        }

        DuplicateAiProvider provider = getProvider();

        Future<DuplicateAiAnalysisResult> future = executorService.submit(() -> provider.analyze(request));

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            log.warn("AI duplicate detection timed out after {}ms", timeoutMs);
            future.cancel(true);
            return new DuplicateAiAnalysisResult(provider.getProviderName(), provider.getModelName(), scoreVersion, Collections.emptyList());
        } catch (Exception e) {
            log.error("AI duplicate detection failed safely: {}", e.getMessage());
            return new DuplicateAiAnalysisResult(provider.getProviderName(), provider.getModelName(), scoreVersion, Collections.emptyList());
        }
    }

    /**
     * Persist AI duplicate suggestions for an existing issue when score >= threshold.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AiDuplicateSuggestionEntity> recordSuggestionsForIssue(IssueEntity sourceIssue, List<DuplicateAiRequest.CandidateInfo> candidates) {
        if (!enabled || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        DuplicateAiRequest.NewIssueInfo target = new DuplicateAiRequest.NewIssueInfo(
                sourceIssue.getTitle(),
                sourceIssue.getDescription(),
                sourceIssue.getCategory() != null ? sourceIssue.getCategory().getId() : null,
                sourceIssue.getCategory() != null ? sourceIssue.getCategory().getName() : null
        );

        DuplicateAiRequest request = new DuplicateAiRequest(target, candidates);
        DuplicateAiAnalysisResult result = analyzeSafely(request);

        List<AiDuplicateSuggestionEntity> savedSuggestions = new ArrayList<>();

        for (var scoreItem : result.candidateScores()) {
            if (scoreItem.score() >= suggestionThreshold) {
                // Check if suggestion already exists
                Optional<AiDuplicateSuggestionEntity> existing = suggestionRepository
                        .findByIssue_IdAndCandidateIssue_Id(sourceIssue.getId(), scoreItem.candidateIssueId());

                if (existing.isPresent()) {
                    // Update score and confidence if still suggested
                    var entity = existing.get();
                    if (entity.getStatus() == DuplicateSuggestionStatus.SUGGESTED) {
                        entity.setScore(scoreItem.score());
                        entity.setConfidence(scoreItem.confidence());
                        entity.setSignals(toJson(scoreItem.signals()));
                        savedSuggestions.add(suggestionRepository.save(entity));
                    }
                } else {
                    Optional<IssueEntity> candidateEntity = issueRepository.findById(scoreItem.candidateIssueId());
                    if (candidateEntity.isPresent() && !candidateEntity.get().getId().equals(sourceIssue.getId())) {
                        AiDuplicateSuggestionEntity newEntity = new AiDuplicateSuggestionEntity(
                                sourceIssue,
                                candidateEntity.get(),
                                scoreItem.score(),
                                scoreItem.confidence(),
                                toJson(scoreItem.signals()),
                                result.provider(),
                                result.model(),
                                result.calculationVersion()
                        );
                        savedSuggestions.add(suggestionRepository.save(newEntity));
                    }
                }
            }
        }

        return savedSuggestions;
    }

    public List<String> parseSignalsJson(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : Collections.emptyList());
        } catch (Exception e) {
            return "[]";
        }
    }
}
