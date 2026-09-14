package org.nagrivic.modules.duplicates.controller;

import jakarta.validation.Valid;
import org.nagrivic.common.dto.PagedResponse;
import org.nagrivic.modules.duplicates.dto.AiDuplicateSuggestionResponse;
import org.nagrivic.modules.duplicates.dto.DismissDuplicateSuggestionRequest;
import org.nagrivic.modules.duplicates.model.DuplicateConfidence;
import org.nagrivic.modules.duplicates.model.DuplicateSuggestionStatus;
import org.nagrivic.modules.duplicates.service.AdminDuplicateService;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/duplicates")
public class AdminDuplicateController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminDuplicateService adminDuplicateService;

    public AdminDuplicateController(AdminDuplicateService adminDuplicateService) {
        this.adminDuplicateService = adminDuplicateService;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<PagedResponse<AiDuplicateSuggestionResponse>> getSuggestions(
            @RequestParam(required = false) DuplicateSuggestionStatus status,
            @RequestParam(required = false) DuplicateConfidence confidence,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(clampedPage, clampedSize);

        Page<AiDuplicateSuggestionResponse> pageResult = adminDuplicateService.getSuggestions(
                status,
                confidence,
                minScore,
                pageable
        );

        PagedResponse<AiDuplicateSuggestionResponse> response = PagedResponse.fromPage(pageResult);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/suggestions/{id}/link")
    public ResponseEntity<IssueResponse> linkSuggestion(@PathVariable UUID id) {
        IssueResponse response = adminDuplicateService.linkDuplicateSuggestion(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/suggestions/{id}/dismiss")
    public ResponseEntity<AiDuplicateSuggestionResponse> dismissSuggestion(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) DismissDuplicateSuggestionRequest request
    ) {
        AiDuplicateSuggestionResponse response = adminDuplicateService.dismissSuggestion(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/scan/{issueId}")
    public ResponseEntity<List<AiDuplicateSuggestionResponse>> scanIssue(@PathVariable UUID issueId) {
        List<AiDuplicateSuggestionResponse> suggestions = adminDuplicateService.scanIssueForDuplicates(issueId);
        return ResponseEntity.ok(suggestions);
    }
}
