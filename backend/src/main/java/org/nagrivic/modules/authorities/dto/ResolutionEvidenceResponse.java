package org.nagrivic.modules.authorities.dto;

import org.nagrivic.modules.authorities.entity.ResolutionEvidenceEntity;
import org.nagrivic.modules.authorities.model.ResolutionEvidenceType;

import java.time.Instant;
import java.util.UUID;

public record ResolutionEvidenceResponse(
        UUID id,
        UUID issueId,
        ResolutionEvidenceType evidenceType,
        String mediaUrl,
        String originalFilename,
        Long fileSizeBytes,
        String note,
        Instant capturedAt,
        Instant createdAt,
        String submittedByRole,
        String submittedByName
) {
    public static ResolutionEvidenceResponse fromEntity(ResolutionEvidenceEntity entity) {
        String mediaUrl = entity.getStorageKey() != null
                ? "/api/media/" + entity.getStorageKey()
                : null;

        String role = entity.getSubmittedBy() != null ? entity.getSubmittedBy().getRole() : "OFFICER";
        String name = entity.getSubmittedBy() != null ? entity.getSubmittedBy().getFullName() : "Municipal Authority";

        return new ResolutionEvidenceResponse(
                entity.getId(),
                entity.getIssue().getId(),
                entity.getEvidenceType(),
                mediaUrl,
                entity.getOriginalFilename(),
                entity.getFileSizeBytes(),
                entity.getNote(),
                entity.getCapturedAt(),
                entity.getCreatedAt(),
                role,
                name
        );
    }
}
