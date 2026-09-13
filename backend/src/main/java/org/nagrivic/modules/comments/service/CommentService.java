package org.nagrivic.modules.comments.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.comments.dto.CommentResponse;
import org.nagrivic.modules.comments.dto.CreateCommentRequest;
import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.comments.repository.CommentRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final CurrentUserService currentUserService;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;
    private final org.nagrivic.modules.moderation.service.ModerationService moderationService;
    private final org.nagrivic.modules.moderation.ratelimit.RateLimiter rateLimiter;
    private final org.nagrivic.modules.moderation.service.ContentAbuseValidator contentAbuseValidator;
    private final org.nagrivic.modules.notifications.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${nagrivic.moderation.rate-limit.comment-per-minute:5}")
    private int commentPerMinute;

    public CommentService(
            CommentRepository commentRepository,
            IssueRepository issueRepository,
            CurrentUserService currentUserService,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService,
            org.nagrivic.modules.moderation.service.ModerationService moderationService,
            org.nagrivic.modules.moderation.ratelimit.RateLimiter rateLimiter,
            org.nagrivic.modules.moderation.service.ContentAbuseValidator contentAbuseValidator,
            @org.springframework.context.annotation.Lazy org.nagrivic.modules.notifications.service.NotificationService notificationService
    ) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.currentUserService = currentUserService;
        this.issueActivityService = issueActivityService;
        this.moderationService = moderationService;
        this.rateLimiter = rateLimiter;
        this.contentAbuseValidator = contentAbuseValidator;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new comment on an issue.
     * Authenticated citizen identity is derived strictly from JWT.
     */
    @Transactional
    public CommentResponse createComment(UUID issueId, CreateCommentRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required to post a comment");
        }
        if (!currentUser.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }
        moderationService.checkUserNotRestricted(currentUser);

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        if (issue.getModerationStatus() == org.nagrivic.modules.moderation.model.ModerationStatus.HIDDEN) {
            throw new ResourceNotFoundException("Issue", issueId);
        }

        if (request == null || request.content() == null || request.content().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be blank");
        }

        // Rate limiting
        rateLimiter.checkLimit("comment:" + currentUser.getId(), commentPerMinute, java.time.Duration.ofMinutes(1));

        // Content abuse validation (repeated characters, rapid duplicate posting)
        contentAbuseValidator.validateComment(currentUser.getId(), issueId, request.content());

        CommentEntity comment = new CommentEntity(issue, currentUser, request.content());
        comment = commentRepository.save(comment);

        issueActivityService.recordActivity(
                issue,
                org.nagrivic.modules.activity.model.IssueActivityType.COMMENT_ADDED,
                currentUser,
                java.util.Map.of("commentId", comment.getId().toString())
        );

        notificationService.handleCommentAdded(issue, currentUser);

        return CommentResponse.fromEntity(comment);
    }

    /**
     * Retrieves paginated comments for an issue.
     * Publicly accessible without requiring authentication.
     * Hidden comments are omitted from public response.
     */
    public Page<CommentResponse> getComments(UUID issueId, Pageable pageable) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        if (issue.getModerationStatus() == org.nagrivic.modules.moderation.model.ModerationStatus.HIDDEN) {
            throw new ResourceNotFoundException("Issue", issueId);
        }

        Page<CommentEntity> page = commentRepository.findByIssue_IdAndModerationStatusNot(
                issueId,
                org.nagrivic.modules.moderation.model.ModerationStatus.HIDDEN,
                pageable
        );
        return page.map(CommentResponse::fromEntity);
    }

    /**
     * Soft-deletes a comment.
     * Only the authenticated author can delete their own comment.
     */
    @Transactional
    public void deleteComment(UUID issueId, UUID commentId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required to delete a comment");
        }
        if (!currentUser.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }

        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (comment.getIssue() == null || !comment.getIssue().getId().equals(issueId)) {
            throw new ResourceNotFoundException("Comment", commentId);
        }

        if (comment.getUser() == null || !comment.getUser().getId().equals(currentUser.getId())) {
            throw AuthException.forbidden("You are not authorized to delete this comment");
        }

        if (!comment.isDeleted()) {
            comment.softDelete();
            commentRepository.save(comment);

            issueActivityService.recordActivity(
                    comment.getIssue(),
                    org.nagrivic.modules.activity.model.IssueActivityType.COMMENT_DELETED,
                    currentUser,
                    java.util.Map.of("commentId", comment.getId().toString())
            );
        }
    }

    /**
     * Returns the count of active (non-deleted) comments for an issue.
     */
    public long getActiveCommentCount(UUID issueId) {
        return commentRepository.countByIssue_IdAndDeletedAtIsNull(issueId);
    }
}
