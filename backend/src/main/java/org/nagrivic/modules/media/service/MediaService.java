package org.nagrivic.modules.media.service;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MediaService {

    private final MediaRepository mediaRepository;
    private final IssueRepository issueRepository;

    public MediaService(MediaRepository mediaRepository, IssueRepository issueRepository) {
        this.mediaRepository = mediaRepository;
        this.issueRepository = issueRepository;
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
        return mediaRepository.save(media);
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
}
