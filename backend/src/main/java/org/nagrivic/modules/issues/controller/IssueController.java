package org.nagrivic.modules.issues.controller;

import jakarta.validation.Valid;
import org.nagrivic.common.dto.PagedResponse;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.service.IssueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(@Valid @RequestBody CreateIssueRequest request) {
        IssueResponse response = issueService.createIssueFromRequest(request);
        URI location = URI.create("/api/issues/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssueById(@PathVariable UUID id) {
        IssueResponse response = issueService.getIssueById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<IssueResponse>> listIssues(
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID reportedBy,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<IssueResponse> pageResult = issueService.findIssues(status, categoryId, reportedBy, pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(pageResult));
    }
}
