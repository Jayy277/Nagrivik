package org.nagrivic.modules.authorities.service;

import org.nagrivic.modules.authorities.dto.ResolutionEvidenceResponse;
import org.nagrivic.modules.authorities.model.ResolutionEvidenceType;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ResolutionEvidenceService {

    /**
     * Attaches resolution evidence to an issue in IN_PROGRESS or RESOLVED status by an authorized authority.
     */
    ResolutionEvidenceResponse addResolutionEvidence(
            UUID issueId,
            MultipartFile file,
            ResolutionEvidenceType evidenceType,
            String note,
            Instant capturedAt,
            UserEntity currentUser
    );

    /**
     * Retrieves all attached resolution evidence for an issue in chronological order.
     */
    List<ResolutionEvidenceResponse> getResolutionEvidence(UUID issueId);
}
