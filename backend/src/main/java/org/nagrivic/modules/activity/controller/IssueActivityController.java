package org.nagrivic.modules.activity.controller;

import org.nagrivic.common.dto.PagedResponse;
import org.nagrivic.modules.activity.dto.ActivityResponse;
import org.nagrivic.modules.activity.service.IssueActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/activity")
public class IssueActivityController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final IssueActivityService issueActivityService;

    public IssueActivityController(IssueActivityService issueActivityService) {
        this.issueActivityService = issueActivityService;
    }

    /**
     * Retrieves the chronological, append-only activity timeline for an issue.
     * Publicly viewable without authentication.
     * Guaranteed deterministic ordering: newest first (createdAt DESC, id DESC).
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ActivityResponse>> getActivityTimeline(
            @PathVariable UUID issueId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.min(Math.max(size, 1), MAX_SIZE);

        Sort deterministicSort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(clampedPage, clampedSize, deterministicSort);

        Page<ActivityResponse> activities = issueActivityService.getIssueActivities(issueId, pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(activities));
    }
}
