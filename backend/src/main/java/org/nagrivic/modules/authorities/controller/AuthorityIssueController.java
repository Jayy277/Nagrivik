package org.nagrivic.modules.authorities.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.authorities.dto.*;
import org.nagrivic.modules.authorities.service.AuthorityIssueService;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/authority")
@PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
public class AuthorityIssueController {

    private final AuthorityIssueService authorityIssueService;
    private final org.nagrivic.modules.authorities.service.ResolutionEvidenceService resolutionEvidenceService;
    private final CurrentUserService currentUserService;

    public AuthorityIssueController(
            AuthorityIssueService authorityIssueService,
            org.nagrivic.modules.authorities.service.ResolutionEvidenceService resolutionEvidenceService,
            CurrentUserService currentUserService
    ) {
        this.authorityIssueService = authorityIssueService;
        this.resolutionEvidenceService = resolutionEvidenceService;
        this.currentUserService = currentUserService;
    }

    /**
     * Retrieves aggregated authority dashboard metrics scoped to the authenticated officer.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AuthorityDashboardMetrics> getDashboard() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        AuthorityDashboardMetrics metrics = authorityIssueService.getDashboardMetrics(currentUser);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Lists civic issues within the officer's server-enforced authority scope.
     */
    @GetMapping("/issues")
    public ResponseEntity<Page<AuthorityIssueItemResponse>> getScopedIssues(
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID wardId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            Pageable pageable
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        AuthorityIssueFilter filter = new AuthorityIssueFilter(
                status,
                priority,
                categoryId,
                wardId,
                departmentId,
                search,
                fromDate,
                toDate,
                null
        );
        Page<AuthorityIssueItemResponse> page = authorityIssueService.findScopedIssues(currentUser, filter, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Retrieves detailed, privacy-safe information for a single scoped issue.
     */
    @GetMapping("/issues/{issueId}")
    public ResponseEntity<AuthorityIssueDetailResponse> getScopedIssueDetail(
            @PathVariable UUID issueId
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        AuthorityIssueDetailResponse response = authorityIssueService.getScopedIssueDetail(issueId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Executes an operational status transition on an issue within the officer's authority scope.
     */
    @PostMapping("/issues/{issueId}/status")
    public ResponseEntity<IssueResponse> changeStatus(
            @PathVariable UUID issueId,
            @Valid @RequestBody ChangeStatusRequest request
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        IssueResponse response = authorityIssueService.changeIssueStatus(issueId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Attaches resolution evidence (completion photo, note, or before/after photo) to a scoped issue.
     */
    @PostMapping(value = "/issues/{issueId}/resolution-evidence", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResolutionEvidenceResponse> addResolutionEvidence(
            @PathVariable UUID issueId,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam("evidenceType") org.nagrivic.modules.authorities.model.ResolutionEvidenceType evidenceType,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "capturedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant capturedAt
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        ResolutionEvidenceResponse response = resolutionEvidenceService.addResolutionEvidence(
                issueId,
                file,
                evidenceType,
                note,
                capturedAt,
                currentUser
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }
}
