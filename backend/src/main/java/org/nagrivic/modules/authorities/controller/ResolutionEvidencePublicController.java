package org.nagrivic.modules.authorities.controller;

import org.nagrivic.modules.authorities.dto.ResolutionEvidenceResponse;
import org.nagrivic.modules.authorities.service.ResolutionEvidenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/resolution-evidence")
public class ResolutionEvidencePublicController {

    private final ResolutionEvidenceService resolutionEvidenceService;

    public ResolutionEvidencePublicController(ResolutionEvidenceService resolutionEvidenceService) {
        this.resolutionEvidenceService = resolutionEvidenceService;
    }

    /**
     * Public endpoint to view transparent, privacy-safe resolution evidence for an issue.
     */
    @GetMapping
    public ResponseEntity<List<ResolutionEvidenceResponse>> getResolutionEvidence(
            @PathVariable UUID issueId
    ) {
        List<ResolutionEvidenceResponse> response = resolutionEvidenceService.getResolutionEvidence(issueId);
        return ResponseEntity.ok(response);
    }
}
