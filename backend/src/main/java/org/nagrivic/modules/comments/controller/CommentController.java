package org.nagrivic.modules.comments.controller;

import jakarta.validation.Valid;
import org.nagrivic.common.dto.PagedResponse;
import org.nagrivic.modules.comments.dto.CommentResponse;
import org.nagrivic.modules.comments.dto.CreateCommentRequest;
import org.nagrivic.modules.comments.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/comments")
public class CommentController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Creates a new comment on a civic issue.
     * Requires authentication.
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID issueId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse response = commentService.createComment(issueId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves paginated comments for an issue in chronological conversation order.
     * Publicly accessible.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CommentResponse>> getComments(
            @PathVariable UUID issueId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<CommentResponse> pageResult = commentService.getComments(issueId, pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(pageResult));
    }

    /**
     * Soft-deletes a comment.
     * Only the authenticated author can delete their comment.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId
    ) {
        commentService.deleteComment(issueId, commentId);
        return ResponseEntity.noContent().build();
    }
}
