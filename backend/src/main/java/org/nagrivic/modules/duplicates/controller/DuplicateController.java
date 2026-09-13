package org.nagrivic.modules.duplicates.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.duplicates.dto.LinkDuplicateRequest;
import org.nagrivic.modules.duplicates.service.DuplicateDetectionService;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
public class DuplicateController {

    private final DuplicateDetectionService duplicateDetectionService;

    public DuplicateController(DuplicateDetectionService duplicateDetectionService) {
        this.duplicateDetectionService = duplicateDetectionService;
    }

    /**
     * Privileged endpoint to mark an issue as duplicate of a primary issue.
     * Accessible only by municipal officers or administrators.
     */
    @PostMapping("/{issueId}/duplicate")
    public ResponseEntity<IssueResponse> linkDuplicate(
            @PathVariable UUID issueId,
            @Valid @RequestBody LinkDuplicateRequest request
    ) {
        IssueResponse response = duplicateDetectionService.linkDuplicate(issueId, request.primaryIssueId());
        return ResponseEntity.ok(response);
    }
}
