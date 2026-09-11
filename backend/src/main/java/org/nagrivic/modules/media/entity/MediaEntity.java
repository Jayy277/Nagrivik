package org.nagrivic.modules.media.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.media.model.MediaType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media")
public class MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private IssueEntity issue;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 32)
    private MediaType mediaType = MediaType.IMAGE;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MediaEntity() {
    }

    public MediaEntity(
            IssueEntity issue,
            String storageKey,
            String originalFilename,
            String contentType,
            Long fileSizeBytes,
            MediaType mediaType,
            Integer displayOrder
    ) {
        validateIssue(issue);
        validateStorageKey(storageKey);
        validateContentType(contentType);
        validateFileSizeBytes(fileSizeBytes);
        validateDisplayOrder(displayOrder);

        this.issue = issue;
        this.storageKey = storageKey.trim();
        this.originalFilename = originalFilename != null ? originalFilename.trim() : null;
        this.contentType = contentType.trim().toLowerCase();
        this.fileSizeBytes = fileSizeBytes;
        this.mediaType = mediaType != null ? mediaType : MediaType.IMAGE;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    private static void validateIssue(IssueEntity issue) {
        if (issue == null) {
            throw new IllegalArgumentException("Associated issue cannot be null");
        }
    }

    private static void validateStorageKey(String storageKey) {
        if (storageKey == null || storageKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage key cannot be blank");
        }
    }

    private static void validateContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be blank");
        }
    }

    private static void validateFileSizeBytes(Long fileSizeBytes) {
        if (fileSizeBytes == null || fileSizeBytes < 0) {
            throw new IllegalArgumentException("File size must be non-negative. Received: " + fileSizeBytes);
        }
    }

    private static void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder != null && displayOrder < 0) {
            throw new IllegalArgumentException("Display order cannot be negative. Received: " + displayOrder);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
        if (this.mediaType == null) {
            this.mediaType = MediaType.IMAGE;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public IssueEntity getIssue() {
        return issue;
    }

    public void setIssue(IssueEntity issue) {
        validateIssue(issue);
        this.issue = issue;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        validateStorageKey(storageKey);
        this.storageKey = storageKey.trim();
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename != null ? originalFilename.trim() : null;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        validateContentType(contentType);
        this.contentType = contentType.trim().toLowerCase();
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        validateFileSizeBytes(fileSizeBytes);
        this.fileSizeBytes = fileSizeBytes;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType != null ? mediaType : MediaType.IMAGE;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        validateDisplayOrder(displayOrder);
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
