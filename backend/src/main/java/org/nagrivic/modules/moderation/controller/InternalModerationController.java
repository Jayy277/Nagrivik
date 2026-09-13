package org.nagrivic.modules.moderation.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.moderation.dto.ModerationActionResponse;
import org.nagrivic.modules.moderation.dto.ModerationReportResponse;
import org.nagrivic.modules.moderation.dto.ResolveReportRequest;
import org.nagrivic.modules.moderation.dto.RestrictUserRequest;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.model.ReportStatus;
import org.nagrivic.modules.moderation.service.ModerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Privileged internal moderation operations accessible only to moderators (officers/admins).
 */
@RestController
@RequestMapping("/api/moderation")
public class InternalModerationController {

    private final ModerationService moderationService;

    public InternalModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ModerationReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            Pageable pageable
    ) {
        Page<ModerationReportResponse> page = moderationService.getReports(status, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/reports/{reportId}/review")
    public ResponseEntity<ModerationReportResponse> reviewReport(@PathVariable UUID reportId) {
        ModerationReportResponse response = moderationService.reviewReport(reportId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<ModerationReportResponse> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request
    ) {
        ModerationReportResponse response = moderationService.resolveReport(reportId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reports/{reportId}/dismiss")
    public ResponseEntity<ModerationReportResponse> dismissReport(
            @PathVariable UUID reportId,
            @RequestParam(required = false) String reason
    ) {
        ModerationReportResponse response = moderationService.dismissReport(reportId, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/content/{targetType}/{targetId}/hide")
    public ResponseEntity<ModerationActionResponse> hideContent(
            @PathVariable ModerationTargetType targetType,
            @PathVariable UUID targetId,
            @RequestParam String reason,
            @RequestParam(required = false) String notes
    ) {
        ModerationActionResponse response = moderationService.hideContent(targetType, targetId, reason, notes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/content/{targetType}/{targetId}/restore")
    public ResponseEntity<ModerationActionResponse> restoreContent(
            @PathVariable ModerationTargetType targetType,
            @PathVariable UUID targetId,
            @RequestParam String reason,
            @RequestParam(required = false) String notes
    ) {
        ModerationActionResponse response = moderationService.restoreContent(targetType, targetId, reason, notes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/restrict")
    public ResponseEntity<ModerationActionResponse> restrictUser(
            @PathVariable UUID userId,
            @Valid @RequestBody RestrictUserRequest request
    ) {
        ModerationActionResponse response = moderationService.restrictUser(userId, request.durationMinutes(), request.reason());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/unrestrict")
    public ResponseEntity<ModerationActionResponse> unrestrictUser(
            @PathVariable UUID userId,
            @RequestParam(required = false, defaultValue = "Manual lift of restriction") String reason
    ) {
        ModerationActionResponse response = moderationService.unrestrictUser(userId, reason);
        return ResponseEntity.ok(response);
    }
}
