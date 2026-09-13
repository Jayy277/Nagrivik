package org.nagrivic.modules.moderation.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.moderation.dto.CreateReportRequest;
import org.nagrivic.modules.moderation.dto.ModerationReportResponse;
import org.nagrivic.modules.moderation.service.ModerationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation/reports")
public class ModerationReportController {

    private final ModerationService moderationService;

    public ModerationReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * Submits a new moderation report against an issue, comment, or user.
     * Authenticated citizen identity is derived strictly from JWT.
     */
    @PostMapping
    public ResponseEntity<ModerationReportResponse> submitReport(@Valid @RequestBody CreateReportRequest request) {
        ModerationReportResponse response = moderationService.submitReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
