package org.nagrivic.modules.authorities.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.authorities.model.ResolutionEvidenceType;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resolution_evidence")
public class ResolutionEvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private IssueEntity issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private UserEntity submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 60)
    private ResolutionEvidenceType evidenceType;

    @Column(name = "storage_key", length = 512)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public ResolutionEvidenceEntity() {
    }

    public ResolutionEvidenceEntity(
            IssueEntity issue,
            UserEntity submittedBy,
            ResolutionEvidenceType evidenceType,
            String storageKey,
            String originalFilename,
            String contentType,
            Long fileSizeBytes,
            String note,
            Instant capturedAt
    ) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue cannot be null");
        }
        if (submittedBy == null) {
            throw new IllegalArgumentException("SubmittedBy user cannot be null");
        }
        if (evidenceType == null) {
            throw new IllegalArgumentException("EvidenceType cannot be null");
        }
        this.issue = issue;
        this.submittedBy = submittedBy;
        this.evidenceType = evidenceType;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.note = note != null ? note.trim() : null;
        this.capturedAt = capturedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
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
        this.issue = issue;
    }

    public UserEntity getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UserEntity submittedBy) {
        this.submittedBy = submittedBy;
    }

    public ResolutionEvidenceType getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(ResolutionEvidenceType evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note != null ? note.trim() : null;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
