package org.nagrivic.modules.statushistory.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.dto.StatusHistoryResponse;
import org.nagrivic.modules.statushistory.dto.VerifyResolutionRequest;
import org.nagrivic.modules.statushistory.service.StatusHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}")
public class StatusHistoryController {

    private final StatusHistoryService statusHistoryService;

    public StatusHistoryController(StatusHistoryService statusHistoryService) {
        this.statusHistoryService = statusHistoryService;
    }

    /**
     * Privileged endpoint to transition issue status.
     * Accessible by municipal officers and administrators.
     */
    @PostMapping("/status")
    public ResponseEntity<IssueResponse> changeStatus(
            @PathVariable UUID issueId,
            @Valid @RequestBody ChangeStatusRequest request
    ) {
        IssueResponse response = statusHistoryService.changeStatus(issueId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Citizen verification endpoint to confirm whether a RESOLVED issue was actually fixed.
     * Restricted to the original issue reporter.
     */
    @PostMapping("/verify-resolution")
    public ResponseEntity<IssueResponse> verifyResolution(
            @PathVariable UUID issueId,
            @Valid @RequestBody VerifyResolutionRequest request
    ) {
        IssueResponse response = statusHistoryService.verifyResolution(issueId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Public endpoint to view transparent, chronological status history for an issue.
     */
    @GetMapping("/status-history")
    public ResponseEntity<StatusHistoryResponse> getStatusHistory(
            @PathVariable UUID issueId
    ) {
        StatusHistoryResponse response = statusHistoryService.getStatusHistory(issueId);
        return ResponseEntity.ok(response);
    }
}
