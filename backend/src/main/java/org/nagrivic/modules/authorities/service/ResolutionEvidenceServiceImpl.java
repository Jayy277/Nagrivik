package org.nagrivic.modules.authorities.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.activity.service.IssueActivityService;
import org.nagrivic.modules.authorities.dto.ResolutionEvidenceResponse;
import org.nagrivic.modules.authorities.entity.ResolutionEvidenceEntity;
import org.nagrivic.modules.authorities.model.ResolutionEvidenceType;
import org.nagrivic.modules.authorities.repository.ResolutionEvidenceRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.media.storage.MediaStorageService;
import org.nagrivic.modules.media.validator.ImageFileValidator;
import org.nagrivic.modules.users.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ResolutionEvidenceServiceImpl implements ResolutionEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionEvidenceServiceImpl.class);

    private final IssueRepository issueRepository;
    private final ResolutionEvidenceRepository resolutionEvidenceRepository;
    private final AuthorityScopeService authorityScopeService;
    private final ImageFileValidator imageFileValidator;
    private final MediaStorageService mediaStorageService;
    private final IssueActivityService issueActivityService;

    public ResolutionEvidenceServiceImpl(
            IssueRepository issueRepository,
            ResolutionEvidenceRepository resolutionEvidenceRepository,
            AuthorityScopeService authorityScopeService,
            ImageFileValidator imageFileValidator,
            MediaStorageService mediaStorageService,
            IssueActivityService issueActivityService
    ) {
        this.issueRepository = issueRepository;
        this.resolutionEvidenceRepository = resolutionEvidenceRepository;
        this.authorityScopeService = authorityScopeService;
        this.imageFileValidator = imageFileValidator;
        this.mediaStorageService = mediaStorageService;
        this.issueActivityService = issueActivityService;
    }

    @Override
    @Transactional
    public ResolutionEvidenceResponse addResolutionEvidence(
            UUID issueId,
            MultipartFile file,
            ResolutionEvidenceType evidenceType,
            String note,
            Instant capturedAt,
            UserEntity currentUser
    ) {
        if (evidenceType == null) {
            throw new IllegalArgumentException("Evidence type must be specified");
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        // 1. Authority Scope Verification (Enforces Ward/Department/City scope & prevents IDOR)
        authorityScopeService.validateAuthorityScope(currentUser, issue);

        // 2. Issue Lifecycle State Policy (Only IN_PROGRESS or RESOLVED)
        if (issue.getStatus() != IssueStatus.IN_PROGRESS && issue.getStatus() != IssueStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    "Resolution evidence can only be submitted when issue is IN_PROGRESS or RESOLVED. Current status: " + issue.getStatus()
            );
        }

        // 3. Note Validation
        String validatedNote = null;
        if (note != null) {
            String trimmed = note.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Resolution note cannot be blank or whitespace-only");
            }
            if (trimmed.length() > 1000) {
                throw new IllegalArgumentException("Resolution note cannot exceed 1000 characters");
            }
            validatedNote = trimmed;
        }

        // 4. Evidence Type Specific Rules
        boolean hasFile = file != null && !file.isEmpty();
        if (evidenceType == ResolutionEvidenceType.COMPLETION_NOTE) {
            if (validatedNote == null) {
                throw new IllegalArgumentException("A resolution note is mandatory for COMPLETION_NOTE evidence");
            }
        } else if (evidenceType == ResolutionEvidenceType.COMPLETION_PHOTO || evidenceType == ResolutionEvidenceType.BEFORE_AFTER_PHOTO) {
            if (!hasFile) {
                throw new IllegalArgumentException("An image file is required for " + evidenceType + " evidence");
            }
        }

        // 5. CapturedAt Timestamp Validation
        if (capturedAt != null) {
            Instant maxFuture = Instant.now().plus(Duration.ofMinutes(15));
            if (capturedAt.isAfter(maxFuture)) {
                throw new IllegalArgumentException("Captured timestamp cannot be in the future");
            }
            Instant minPast = Instant.parse("2020-01-01T00:00:00Z");
            if (capturedAt.isBefore(minPast)) {
                throw new IllegalArgumentException("Captured timestamp is unreasonably far in the past");
            }
        }

        // 6. Media Validation and Storage (Reuse existing media abstraction)
        String storageKey = null;
        String originalFilename = null;
        String contentType = null;
        Long fileSizeBytes = null;

        if (hasFile) {
            String extension = imageFileValidator.validateAndGetExtension(file);
            storageKey = "resolution-evidence/" + issueId + "/" + UUID.randomUUID() + "." + extension;
            originalFilename = sanitizeFilename(file.getOriginalFilename());
            contentType = file.getContentType();
            fileSizeBytes = file.getSize();

            try {
                mediaStorageService.store(storageKey, file.getInputStream(), file.getSize(), contentType);
            } catch (IOException e) {
                log.error("Failed to read uploaded resolution evidence file stream for issue {}: {}", issueId, e.getMessage());
                throw new RuntimeException("Failed to process resolution evidence image upload", e);
            }
        }

        // 7. Persist Resolution Evidence Record with compensation cleanup on failure
        ResolutionEvidenceEntity entity;
        try {
            entity = new ResolutionEvidenceEntity(
                    issue,
                    currentUser,
                    evidenceType,
                    storageKey,
                    originalFilename,
                    contentType,
                    fileSizeBytes,
                    validatedNote,
                    capturedAt
            );
            entity = resolutionEvidenceRepository.save(entity);

            // 8. Record Activity Event (Append-only audit, zero PII, zero secrets)
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("evidenceId", entity.getId().toString());
            eventData.put("evidenceType", entity.getEvidenceType().name());
            eventData.put("hasImage", entity.getStorageKey() != null);
            if (entity.getNote() != null) {
                String snippet = entity.getNote().length() > 80 ? entity.getNote().substring(0, 80) + "..." : entity.getNote();
                eventData.put("noteSnippet", snippet);
            }
            issueActivityService.recordActivity(issue, IssueActivityType.RESOLUTION_EVIDENCE_ADDED, currentUser, eventData);
        } catch (Exception ex) {
            if (storageKey != null) {
                mediaStorageService.delete(storageKey);
            }
            throw ex;
        }

        return ResolutionEvidenceResponse.fromEntity(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResolutionEvidenceResponse> getResolutionEvidence(UUID issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", issueId);
        }

        List<ResolutionEvidenceEntity> list = resolutionEvidenceRepository.findByIssue_IdOrderByCreatedAtAsc(issueId);
        return list.stream()
                .map(ResolutionEvidenceResponse::fromEntity)
                .toList();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String base = Paths.get(filename).getFileName().toString();
        return base.length() > 255 ? base.substring(0, 255) : base;
    }
}
