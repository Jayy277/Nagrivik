package org.nagrivic.modules.moderation.service;

import org.nagrivic.common.error.ConflictException;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.comments.repository.CommentRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.moderation.dto.*;
import org.nagrivic.modules.moderation.entity.ModerationActionEntity;
import org.nagrivic.modules.moderation.entity.ModerationReportEntity;
import org.nagrivic.modules.moderation.model.*;
import org.nagrivic.modules.moderation.ratelimit.RateLimiter;
import org.nagrivic.modules.moderation.repository.ModerationActionRepository;
import org.nagrivic.modules.moderation.repository.ModerationReportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class ModerationServiceImpl implements ModerationService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private final ModerationReportRepository reportRepository;
    private final ModerationActionRepository actionRepository;
    private final IssueRepository issueRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final RateLimiter rateLimiter;

    @Value("${nagrivic.moderation.rate-limit.report-per-hour:10}")
    private int reportPerHour;

    public ModerationServiceImpl(
            ModerationReportRepository reportRepository,
            ModerationActionRepository actionRepository,
            IssueRepository issueRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            RateLimiter rateLimiter
    ) {
        this.reportRepository = reportRepository;
        this.actionRepository = actionRepository;
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    @Transactional
    public ModerationReportResponse submitReport(CreateReportRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required to submit a report");
        }
        checkUserNotRestricted(currentUser);

        // Enforce rate limiting on report submissions
        rateLimiter.checkLimit("report:" + currentUser.getId(), reportPerHour, Duration.ofHours(1));

        String sanitizedDescription = sanitizeDescription(request.description());

        // Validate target existence, ownership, and moderation status
        validateReportTarget(currentUser, request.targetType(), request.targetId());

        // Prevent duplicate open reports from the same citizen
        boolean alreadyReported = reportRepository.existsByReporter_IdAndTargetTypeAndTargetIdAndStatusIn(
                currentUser.getId(),
                request.targetType(),
                request.targetId(),
                List.of(ReportStatus.OPEN, ReportStatus.IN_REVIEW)
        );
        if (alreadyReported) {
            throw new ConflictException("A report for this content has already been submitted by you and is under review");
        }

        ModerationReportEntity report = new ModerationReportEntity(
                currentUser,
                request.targetType(),
                request.targetId(),
                request.reason(),
                sanitizedDescription
        );
        report = reportRepository.save(report);

        return ModerationReportResponse.fromEntity(report);
    }

    @Override
    public Page<ModerationReportResponse> getReports(ReportStatus status, Pageable pageable) {
        verifyModeratorRole();
        Page<ModerationReportEntity> page = (status != null)
                ? reportRepository.findByStatus(status, pageable)
                : reportRepository.findAll(pageable);
        return page.map(ModerationReportResponse::fromEntity);
    }

    @Override
    @Transactional
    public ModerationReportResponse reviewReport(UUID reportId) {
        verifyModeratorRole();
        ModerationReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ModerationReport", reportId));

        if (report.getStatus() == ReportStatus.OPEN) {
            report.setStatus(ReportStatus.IN_REVIEW);
            report = reportRepository.save(report);
        }
        return ModerationReportResponse.fromEntity(report);
    }

    @Override
    @Transactional
    public ModerationReportResponse resolveReport(UUID reportId, ResolveReportRequest request) {
        UserEntity moderator = verifyModeratorRole();
        ModerationReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ModerationReport", reportId));

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(Instant.now());
        report.setResolvedBy(moderator);
        report = reportRepository.save(report);

        // Execute corresponding content action if requested
        if (request.action() == ModerationActionType.HIDE_CONTENT || request.action() == ModerationActionType.REMOVE_COMMENT) {
            applyContentVisibility(report.getTargetType(), report.getTargetId(), ModerationStatus.HIDDEN);
        } else if (request.action() == ModerationActionType.RESTORE_CONTENT) {
            applyContentVisibility(report.getTargetType(), report.getTargetId(), ModerationStatus.VISIBLE);
        }

        // Record immutable audit action
        ModerationActionEntity action = new ModerationActionEntity(
                report,
                report.getTargetType(),
                report.getTargetId(),
                moderator,
                request.action(),
                request.reason(),
                request.notes()
        );
        actionRepository.save(action);

        return ModerationReportResponse.fromEntity(report);
    }

    @Override
    @Transactional
    public ModerationReportResponse dismissReport(UUID reportId, String reason) {
        UserEntity moderator = verifyModeratorRole();
        ModerationReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ModerationReport", reportId));

        report.setStatus(ReportStatus.DISMISSED);
        report.setResolvedAt(Instant.now());
        report.setResolvedBy(moderator);
        report = reportRepository.save(report);

        // Record audit
        ModerationActionEntity action = new ModerationActionEntity(
                report,
                report.getTargetType(),
                report.getTargetId(),
                moderator,
                ModerationActionType.NO_ACTION,
                reason != null ? reason : "Report dismissed after review",
                null
        );
        actionRepository.save(action);

        return ModerationReportResponse.fromEntity(report);
    }

    @Override
    @Transactional
    public ModerationActionResponse hideContent(ModerationTargetType targetType, UUID targetId, String reason, String notes) {
        UserEntity moderator = verifyModeratorRole();
        applyContentVisibility(targetType, targetId, ModerationStatus.HIDDEN);

        ModerationActionEntity action = new ModerationActionEntity(
                null,
                targetType,
                targetId,
                moderator,
                ModerationActionType.HIDE_CONTENT,
                reason,
                notes
        );
        action = actionRepository.save(action);
        return ModerationActionResponse.fromEntity(action);
    }

    @Override
    @Transactional
    public ModerationActionResponse restoreContent(ModerationTargetType targetType, UUID targetId, String reason, String notes) {
        UserEntity moderator = verifyModeratorRole();
        applyContentVisibility(targetType, targetId, ModerationStatus.VISIBLE);

        ModerationActionEntity action = new ModerationActionEntity(
                null,
                targetType,
                targetId,
                moderator,
                ModerationActionType.RESTORE_CONTENT,
                reason,
                notes
        );
        action = actionRepository.save(action);
        return ModerationActionResponse.fromEntity(action);
    }

    @Override
    @Transactional
    public ModerationActionResponse restrictUser(UUID userId, Long durationMinutes, String reason) {
        UserEntity moderator = verifyModeratorRole();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setModerationStatus(UserModerationStatus.RESTRICTED);
        user.setRestrictedUntil(durationMinutes != null ? Instant.now().plus(Duration.ofMinutes(durationMinutes)) : null);
        user.setRestrictionReason(reason);
        userRepository.save(user);

        ModerationActionEntity action = new ModerationActionEntity(
                null,
                ModerationTargetType.USER,
                userId,
                moderator,
                ModerationActionType.RESTRICT_USER,
                reason,
                durationMinutes != null ? "Restricted for " + durationMinutes + " minutes" : "Indefinite restriction"
        );
        action = actionRepository.save(action);
        return ModerationActionResponse.fromEntity(action);
    }

    @Override
    @Transactional
    public ModerationActionResponse unrestrictUser(UUID userId, String reason) {
        UserEntity moderator = verifyModeratorRole();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setModerationStatus(UserModerationStatus.ACTIVE);
        user.setRestrictedUntil(null);
        user.setRestrictionReason(null);
        userRepository.save(user);

        ModerationActionEntity action = new ModerationActionEntity(
                null,
                ModerationTargetType.USER,
                userId,
                moderator,
                ModerationActionType.UNRESTRICT_USER,
                reason != null ? reason : "User restriction lifted",
                null
        );
        action = actionRepository.save(action);
        return ModerationActionResponse.fromEntity(action);
    }

    @Override
    public void checkUserNotRestricted(UserEntity user) {
        if (user != null && user.isRestricted()) {
            throw AuthException.forbidden("Your account is currently restricted from performing this action due to moderation.");
        }
    }

    private UserEntity verifyModeratorRole() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required");
        }
        String role = currentUser.getRole();
        if (!"OFFICER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw AuthException.forbidden("Only municipal officers or administrators can perform moderation actions");
        }
        return currentUser;
    }

    private void validateReportTarget(UserEntity currentUser, ModerationTargetType targetType, UUID targetId) {
        switch (targetType) {
            case ISSUE -> {
                IssueEntity issue = issueRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Issue", targetId));
                if (issue.getReporter().getId().equals(currentUser.getId())) {
                    throw new IllegalArgumentException("You cannot report your own issue");
                }
                if (issue.getModerationStatus() == ModerationStatus.HIDDEN) {
                    throw new IllegalArgumentException("Content has already been hidden by moderation");
                }
            }
            case COMMENT -> {
                CommentEntity comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Comment", targetId));
                if (comment.getUser().getId().equals(currentUser.getId())) {
                    throw new IllegalArgumentException("You cannot report your own comment");
                }
                if (comment.isDeleted() || comment.getModerationStatus() == ModerationStatus.HIDDEN) {
                    throw new IllegalArgumentException("Content has already been removed or hidden");
                }
            }
            case USER -> {
                if (targetId.equals(currentUser.getId())) {
                    throw new IllegalArgumentException("You cannot report yourself");
                }
                if (!userRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("User", targetId);
                }
            }
        }
    }

    private void applyContentVisibility(ModerationTargetType targetType, UUID targetId, ModerationStatus status) {
        switch (targetType) {
            case ISSUE -> {
                IssueEntity issue = issueRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Issue", targetId));
                issue.setModerationStatus(status);
                issueRepository.save(issue);
            }
            case COMMENT -> {
                CommentEntity comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Comment", targetId));
                comment.setModerationStatus(status);
                commentRepository.save(comment);
            }
            case USER -> {
                // User visibility handled through restrictUser / unrestrictUser
            }
        }
    }

    private String sanitizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String stripped = HTML_TAG_PATTERN.matcher(description.trim()).replaceAll("");
        if (stripped.length() > 1000) {
            return stripped.substring(0, 1000);
        }
        return stripped;
    }
}
