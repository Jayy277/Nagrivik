package org.nagrivic.modules.duplicates.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.duplicates.ai.DuplicateAiRequest;
import org.nagrivic.modules.duplicates.dto.AiDuplicateSuggestionResponse;
import org.nagrivic.modules.duplicates.dto.DismissDuplicateSuggestionRequest;
import org.nagrivic.modules.duplicates.entity.AiDuplicateSuggestionEntity;
import org.nagrivic.modules.duplicates.model.DuplicateConfidence;
import org.nagrivic.modules.duplicates.model.DuplicateSuggestionStatus;
import org.nagrivic.modules.duplicates.repository.AiDuplicateSuggestionRepository;
import org.nagrivic.modules.duplicates.repository.DuplicateCandidateProjection;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class AdminDuplicateService {

    private final AiDuplicateSuggestionRepository suggestionRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final AiDuplicateDetectionService aiDetectionService;
    private final IssueRepository issueRepository;
    private final CurrentUserService currentUserService;

    public AdminDuplicateService(
            AiDuplicateSuggestionRepository suggestionRepository,
            DuplicateDetectionService duplicateDetectionService,
            AiDuplicateDetectionService aiDetectionService,
            IssueRepository issueRepository,
            CurrentUserService currentUserService
    ) {
        this.suggestionRepository = suggestionRepository;
        this.duplicateDetectionService = duplicateDetectionService;
        this.aiDetectionService = aiDetectionService;
        this.issueRepository = issueRepository;
        this.currentUserService = currentUserService;
    }

    private void checkReviewerPermissions() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String role = currentUser.getRole();
        if (!"MODERATOR".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role) && !"OFFICER".equalsIgnoreCase(role)) {
            throw AuthException.forbidden("Only moderators, administrators, or officers can review duplicate suggestions");
        }
    }

    @Transactional(readOnly = true)
    public Page<AiDuplicateSuggestionResponse> getSuggestions(
            DuplicateSuggestionStatus status,
            DuplicateConfidence confidence,
            Integer minScore,
            Pageable pageable
    ) {
        checkReviewerPermissions();
        Page<AiDuplicateSuggestionEntity> page = suggestionRepository.findSuggestions(status, confidence, minScore, pageable);
        return page.map(entity -> AiDuplicateSuggestionResponse.fromEntity(
                entity,
                aiDetectionService.parseSignalsJson(entity.getSignals())
        ));
    }

    public IssueResponse linkDuplicateSuggestion(UUID suggestionId) {
        checkReviewerPermissions();
        UserEntity reviewer = currentUserService.getCurrentUser();

        AiDuplicateSuggestionEntity suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("AiDuplicateSuggestion", suggestionId));

        if (suggestion.getStatus() == DuplicateSuggestionStatus.LINKED) {
            throw new IllegalArgumentException("Duplicate suggestion is already linked");
        }

        // Execute canonical duplicate link
        IssueResponse linkedResponse = duplicateDetectionService.linkDuplicate(
                suggestion.getIssue().getId(),
                suggestion.getCandidateIssue().getId()
        );

        suggestion.setStatus(DuplicateSuggestionStatus.LINKED);
        suggestion.setReviewedBy(reviewer);
        suggestion.setReviewedAt(Instant.now());
        suggestionRepository.save(suggestion);

        return linkedResponse;
    }

    public AiDuplicateSuggestionResponse dismissSuggestion(UUID suggestionId, DismissDuplicateSuggestionRequest request) {
        checkReviewerPermissions();
        UserEntity reviewer = currentUserService.getCurrentUser();

        AiDuplicateSuggestionEntity suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("AiDuplicateSuggestion", suggestionId));

        suggestion.setStatus(DuplicateSuggestionStatus.DISMISSED);
        suggestion.setDismissReason(request != null && request.reason() != null ? request.reason().trim() : null);
        suggestion.setReviewedBy(reviewer);
        suggestion.setReviewedAt(Instant.now());

        AiDuplicateSuggestionEntity saved = suggestionRepository.save(suggestion);
        return AiDuplicateSuggestionResponse.fromEntity(saved, aiDetectionService.parseSignalsJson(saved.getSignals()));
    }

    public List<AiDuplicateSuggestionResponse> scanIssueForDuplicates(UUID issueId) {
        checkReviewerPermissions();

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        List<DuplicateCandidateProjection> projections = duplicateDetectionService.findPotentialDuplicates(issueId, null);
        if (projections.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> candidateIds = projections.stream().map(DuplicateCandidateProjection::getIssueId).toList();
        Map<UUID, IssueEntity> entityMap = issueRepository.findAllById(candidateIds).stream()
                .collect(java.util.stream.Collectors.toMap(IssueEntity::getId, java.util.function.Function.identity()));

        List<DuplicateAiRequest.CandidateInfo> candidateInfos = new ArrayList<>();
        for (var proj : projections) {
            IssueEntity ent = entityMap.get(proj.getIssueId());
            candidateInfos.add(new DuplicateAiRequest.CandidateInfo(
                    proj.getIssueId(),
                    proj.getTitle(),
                    ent != null ? ent.getDescription() : null,
                    proj.getCategoryId(),
                    proj.getCategoryName(),
                    proj.getDistanceMeters()
            ));
        }

        List<AiDuplicateSuggestionEntity> savedList = aiDetectionService.recordSuggestionsForIssue(issue, candidateInfos);
        return savedList.stream()
                .map(e -> AiDuplicateSuggestionResponse.fromEntity(e, aiDetectionService.parseSignalsJson(e.getSignals())))
                .toList();
    }
}
