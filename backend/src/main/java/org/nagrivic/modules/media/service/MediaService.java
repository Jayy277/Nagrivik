package org.nagrivic.modules.media.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.media.dto.MediaResponse;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.media.storage.MediaStorageService;
import org.nagrivic.modules.media.validator.ImageFileValidator;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MediaService {

    private final MediaRepository mediaRepository;
    private final IssueRepository issueRepository;
    private final CurrentUserService currentUserService;
    private final MediaStorageService mediaStorageService;
    private final ImageFileValidator imageFileValidator;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;

    public MediaService(
            MediaRepository mediaRepository,
            IssueRepository issueRepository,
            CurrentUserService currentUserService,
            MediaStorageService mediaStorageService,
            ImageFileValidator imageFileValidator,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService
    ) {
        this.mediaRepository = mediaRepository;
        this.issueRepository = issueRepository;
        this.currentUserService = currentUserService;
        this.mediaStorageService = mediaStorageService;
        this.imageFileValidator = imageFileValidator;
        this.issueActivityService = issueActivityService;
    }

    /**
     * Uploads and attaches an image to an issue.
     * Enforces issue ownership, user activity, strict file validation, server-generated storage key,
     * server-controlled display order, and storage-database consistency.
     */
    @Transactional
    public MediaResponse uploadIssueMedia(UUID issueId, MultipartFile file) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required to attach media");
        }
        if (!currentUser.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        if (issue.getReporter() == null || !issue.getReporter().getId().equals(currentUser.getId())) {
            throw AuthException.forbidden("You are not authorized to attach media to this issue");
        }

        String extension = imageFileValidator.validateAndGetExtension(file);

        // Server-generated storage key preventing path traversal
        String storageKey = "issues/" + issueId + "/" + UUID.randomUUID() + "." + extension;

        // Server-controlled display order: 0 for first image, then 1, 2, ...
        int displayOrder = (int) mediaRepository.countByIssue_Id(issueId);

        // 1. Store the binary file in storage first
        try (InputStream inputStream = file.getInputStream()) {
            mediaStorageService.store(storageKey, inputStream, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file stream", e);
        }

        // 2. Persist metadata in database with compensation cleanup on failure
        MediaEntity media;
        try {
            media = new MediaEntity(
                    issue,
                    storageKey,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    MediaType.IMAGE,
                    displayOrder
            );
            media = mediaRepository.save(media);
            issueActivityService.recordActivity(
                    issue,
                    org.nagrivic.modules.activity.model.IssueActivityType.MEDIA_ADDED,
                    currentUser,
                    java.util.Map.of("mediaId", media.getId().toString())
            );
        } catch (Exception ex) {
            // Compensate: delete binary from storage if database persistence fails
            mediaStorageService.delete(storageKey);
            throw ex;
        }

        return MediaResponse.fromEntity(media);
    }

    @Transactional
    public MediaEntity createMedia(
            UUID issueId,
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            MediaType mediaType,
            Integer displayOrder
    ) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));

        if (storageKey != null && mediaRepository.existsByStorageKey(storageKey.trim())) {
            throw new IllegalArgumentException("Storage key already exists: " + storageKey.trim());
        }

        MediaEntity media = new MediaEntity(
                issue,
                storageKey,
                originalFilename,
                contentType,
                fileSizeBytes,
                mediaType,
                displayOrder
        );
        MediaEntity saved = mediaRepository.save(media);
        issueActivityService.recordActivity(
                issue,
                org.nagrivic.modules.activity.model.IssueActivityType.MEDIA_ADDED,
                issue.getReporter(),
                java.util.Map.of("mediaId", saved.getId().toString())
        );
        return saved;
    }

    public Optional<MediaEntity> findById(UUID id) {
        return mediaRepository.findById(id);
    }

    public List<MediaEntity> findByIssueId(UUID issueId) {
        return mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(issueId);
    }

    public long countByIssueId(UUID issueId) {
        return mediaRepository.countByIssue_Id(issueId);
    }

    public record MediaContent(InputStream inputStream, String contentType, long contentLength) {}

    public MediaContent loadMediaContent(UUID issueId, UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        if (media.getIssue() == null || !media.getIssue().getId().equals(issueId)) {
            throw new ResourceNotFoundException("Media", mediaId);
        }

        InputStream is = mediaStorageService.load(media.getStorageKey());
        return new MediaContent(is, media.getContentType(), media.getFileSizeBytes());
    }
}
