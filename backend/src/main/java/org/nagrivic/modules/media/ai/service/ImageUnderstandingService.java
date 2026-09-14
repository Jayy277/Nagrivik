package org.nagrivic.modules.media.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.media.ai.dto.ImageAiAnalysisResponse;
import org.nagrivic.modules.media.ai.entity.ImageAiAnalysisEntity;
import org.nagrivic.modules.media.ai.model.*;
import org.nagrivic.modules.media.ai.provider.*;
import org.nagrivic.modules.media.ai.repository.ImageAiAnalysisRepository;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.media.storage.MediaStorageService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ImageUnderstandingService {

    private static final Logger log = LoggerFactory.getLogger(ImageUnderstandingService.class);

    private final ImageAiAnalysisRepository analysisRepository;
    private final MediaRepository mediaRepository;
    private final IssueRepository issueRepository;
    private final MediaStorageService mediaStorageService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Value("${nagrivic.ai.image.enabled:false}")
    private boolean enabled;

    @Value("${nagrivic.ai.image.provider:LOCAL_HEURISTIC}")
    private String providerType;

    @Value("${nagrivic.ai.image.model:heuristic-vision-v1}")
    private String modelName;

    @Value("${nagrivic.ai.image.model-version:1.0.0}")
    private String modelVersion;

    @Value("${nagrivic.ai.image.calculation-version:image-understanding-v1}")
    private String calculationVersion;

    @Value("${nagrivic.ai.image.timeout-ms:5000}")
    private long timeoutMs;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ImageUnderstandingService(
            ImageAiAnalysisRepository analysisRepository,
            MediaRepository mediaRepository,
            IssueRepository issueRepository,
            MediaStorageService mediaStorageService,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.analysisRepository = analysisRepository;
        this.mediaRepository = mediaRepository;
        this.issueRepository = issueRepository;
        this.mediaStorageService = mediaStorageService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    public ImageUnderstandingProvider getProvider() {
        if (!enabled) {
            return new DisabledImageUnderstandingProvider(calculationVersion);
        }
        return new LocalHeuristicImageUnderstandingProvider(modelName, modelVersion, calculationVersion);
    }

    private void checkAuthorization(IssueEntity issue) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required to analyze media");
        }

        String role = currentUser.getRole();
        boolean isPrivileged = "OFFICER".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "MODERATOR".equalsIgnoreCase(role);

        boolean isReporter = issue.getReporter() != null
                && issue.getReporter().getId().equals(currentUser.getId());

        if (!isPrivileged && !isReporter) {
            throw AuthException.forbidden("You are not authorized to analyze media for this issue");
        }
    }

    @Transactional
    public ImageAiAnalysisResponse analyzeMedia(UUID issueId, UUID mediaId) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        checkAuthorization(issue);

        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        if (media.getIssue() == null || !media.getIssue().getId().equals(issueId)) {
            throw new ResourceNotFoundException("Media", mediaId);
        }

        // Return cached result if already completed
        Optional<ImageAiAnalysisEntity> existing = analysisRepository.findByMedia_Id(mediaId);
        if (existing.isPresent() && existing.get().getStatus() == ImageAnalysisStatus.COMPLETED) {
            return toResponse(existing.get());
        }

        if (!enabled) {
            ImageAiAnalysisEntity analysis = existing.orElseGet(() -> new ImageAiAnalysisEntity(
                    media, issue, "DISABLED", "none", "none", calculationVersion, ImageAnalysisStatus.UNAVAILABLE
            ));
            analysis.setStatus(ImageAnalysisStatus.UNAVAILABLE);
            analysis.setErrorMessage("AI image understanding is disabled");
            ImageAiAnalysisEntity saved = analysisRepository.save(analysis);
            return toResponse(saved);
        }

        // Load media binary
        byte[] bytes;
        try (InputStream is = mediaStorageService.load(media.getStorageKey())) {
            bytes = is.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to load media binary for analysis: {}", e.getMessage());
            ImageAiAnalysisEntity analysis = existing.orElseGet(() -> new ImageAiAnalysisEntity(
                    media, issue, providerType, modelName, modelVersion, calculationVersion, ImageAnalysisStatus.FAILED
            ));
            analysis.setStatus(ImageAnalysisStatus.FAILED);
            analysis.setErrorMessage("Failed to read media binary: " + e.getMessage());
            return toResponse(analysisRepository.save(analysis));
        }

        ImageUnderstandingRequest request = new ImageUnderstandingRequest(
                mediaId,
                bytes,
                media.getContentType(),
                media.getFileSizeBytes(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getCategory() != null ? issue.getCategory().getSlug() : null,
                issue.getCategory() != null ? issue.getCategory().getName() : null
        );

        ImageUnderstandingProvider provider = getProvider();
        ImageUnderstandingResult result;

        Future<ImageUnderstandingResult> future = executor.submit(() -> provider.analyze(request));
        try {
            result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            log.warn("AI image analysis timed out after {}ms", timeoutMs);
            future.cancel(true);
            result = ImageUnderstandingResult.failed(provider.getProviderName(), provider.getModelName(), modelVersion, "Analysis timed out");
        } catch (Exception e) {
            log.error("AI image analysis failed safely: {}", e.getMessage());
            result = ImageUnderstandingResult.failed(provider.getProviderName(), provider.getModelName(), modelVersion, e.getMessage());
        }

        final ImageUnderstandingResult finalResult = result;
        // Persist structured analysis result
        ImageAiAnalysisEntity entity = existing.orElseGet(() -> new ImageAiAnalysisEntity(
                media, issue, finalResult.provider(), finalResult.model(), finalResult.modelVersion(), finalResult.calculationVersion(), finalResult.status()
        ));

        entity.setProvider(result.provider());
        entity.setModel(result.model());
        entity.setModelVersion(result.modelVersion());
        entity.setCalculationVersion(result.calculationVersion());
        entity.setStatus(result.status());
        entity.setLikelyCategory(result.likelyCategory());
        entity.setCategoryConfidence(result.categoryConfidence());
        entity.setVisualProblemTypes(toJson(result.visualProblemTypes()));
        entity.setImageQuality(result.imageQuality());
        entity.setQualityIssues(toJson(result.qualityIssues()));
        entity.setRelevance(result.relevance());
        entity.setVisualSeveritySignals(toJson(result.visualSeveritySignals()));
        entity.setSafetyConcern(result.safetyConcern());
        entity.setSensitiveVisualContentDetected(result.sensitiveVisualContentDetected());
        entity.setSummary(result.summary());
        entity.setErrorMessage(result.errorMessage());

        ImageAiAnalysisEntity saved = analysisRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ImageAiAnalysisResponse getAnalysis(UUID issueId, UUID mediaId) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        checkAuthorization(issue);

        ImageAiAnalysisEntity entity = analysisRepository.findByMedia_Id(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("ImageAiAnalysis", mediaId));

        return toResponse(entity);
    }

    private ImageAiAnalysisResponse toResponse(ImageAiAnalysisEntity entity) {
        List<VisualProblemType> problems = fromJson(entity.getVisualProblemTypes(), new TypeReference<>() {});
        List<QualityIssue> qualityIssues = fromJson(entity.getQualityIssues(), new TypeReference<>() {});
        List<VisualSeveritySignal> severitySignals = fromJson(entity.getVisualSeveritySignals(), new TypeReference<>() {});

        return ImageAiAnalysisResponse.fromEntity(entity, problems, qualityIssues, severitySignals);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj != null ? obj : Collections.emptyList());
        } catch (Exception e) {
            return "[]";
        }
    }

    private <T> List<T> fromJson(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
