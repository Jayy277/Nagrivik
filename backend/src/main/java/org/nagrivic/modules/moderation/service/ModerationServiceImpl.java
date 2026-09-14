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
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    private final MediaRepository mediaRepository;
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
            MediaRepository mediaRepository,
            CurrentUserService currentUserService,
            RateLimiter rateLimiter
    ) {
        this.reportRepository = reportRepository;
        this.actionRepository = actionRepository;
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
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
    public Page<ModerationReportResponse> getReports(
            ReportStatus status,
            ModerationTargetType targetType,
            ModerationReason reason,
            Pageable pageable
    ) {
        verifyModeratorRole();

        int boundedSize = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest boundedPageable = PageRequest.of(pageable.getPageNumber(), boundedSize, sort);

        Specification<ModerationReportEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            if (reason != null) {
                predicates.add(cb.equal(root.get("reason"), reason));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ModerationReportEntity> page = reportRepository.findAll(spec, boundedPageable);
        return page.map(ModerationReportResponse::fromEntity);
    }

    @Override
    public ModerationSummaryResponse getSummary() {
        verifyModeratorRole();
        long openCount = reportRepository.countByStatus(ReportStatus.OPEN);
        long inReviewCount = reportRepository.countByStatus(ReportStatus.IN_REVIEW);
        long resolvedCount = reportRepository.countByStatus(ReportStatus.RESOLVED);
        long dismissedCount = reportRepository.countByStatus(ReportStatus.DISMISSED);
        long totalReports = reportRepository.count();
        return new ModerationSummaryResponse(openCount, inReviewCount, resolvedCount, dismissedCount, totalReports);
    }

    @Override
    public ModerationReportDetailResponse getReportDetail(UUID reportId) {
        verifyModeratorRole();
        ModerationReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ModerationReport", reportId));

        ModerationReportDetailResponse.SafeUserSummary reporterSummary = report.getReporter() != null
                ? new ModerationReportDetailResponse.SafeUserSummary(
                        report.getReporter().getId(),
                        report.getReporter().getFullName(),
                        report.getReporter().getRole()
                ) : null;

        ModerationReportDetailResponse.SafeUserSummary resolverSummary = report.getResolvedBy() != null
                ? new ModerationReportDetailResponse.SafeUserSummary(
                        report.getResolvedBy().getId(),
                        report.getResolvedBy().getFullName(),
                        report.getResolvedBy().getRole()
                ) : null;

        ModerationReportDetailResponse.IssueTargetDetail issueTarget = null;
        ModerationReportDetailResponse.CommentTargetDetail commentTarget = null;

        if (report.getTargetType() == ModerationTargetType.ISSUE) {
            Optional<IssueEntity> issueOpt = issueRepository.findById(report.getTargetId());
            if (issueOpt.isPresent()) {
                IssueEntity issue = issueOpt.get();
                ModerationReportDetailResponse.SafeUserSummary issueReporter = issue.getReporter() != null
                        ? new ModerationReportDetailResponse.SafeUserSummary(
                                issue.getReporter().getId(),
                                issue.getReporter().getFullName(),
                                issue.getReporter().getRole()
                        ) : null;

                ModerationReportDetailResponse.CivicResponsibilitySummary responsibility = null;
                if (issue.getCivicBody() != null || issue.getWard() != null || issue.getDepartment() != null) {
                    responsibility = new ModerationReportDetailResponse.CivicResponsibilitySummary(
                            issue.getCivicBody() != null ? issue.getCivicBody().getName() : null,
                            issue.getWard() != null ? issue.getWard().getWardName() : null,
                            issue.getDepartment() != null ? issue.getDepartment().getName() : null
                    );
                }

                List<String> mediaUrls = mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(issue.getId())
                        .stream()
                        .map(m -> "/api/issues/" + issue.getId() + "/media/" + m.getId())
                        .toList();

                String priorityLevel = (issue.getPriority() != null && issue.getPriority().getPriorityLevel() != null)
                        ? issue.getPriority().getPriorityLevel().name()
                        : null;

                issueTarget = new ModerationReportDetailResponse.IssueTargetDetail(
                        issue.getId(),
                        issue.getTitle(),
                        issue.getDescription(),
                        issue.getCategory() != null ? issue.getCategory().getName() : null,
                        issue.getStatus() != null ? issue.getStatus().name() : null,
                        priorityLevel,
                        issue.getModerationStatus(),
                        issue.getCreatedAt(),
                        issueReporter,
                        responsibility,
                        mediaUrls
                );
            }
        } else if (report.getTargetType() == ModerationTargetType.COMMENT) {
            Optional<CommentEntity> commentOpt = commentRepository.findById(report.getTargetId());
            if (commentOpt.isPresent()) {
                CommentEntity comment = commentOpt.get();
                ModerationReportDetailResponse.SafeUserSummary authorSummary = comment.getUser() != null
                        ? new ModerationReportDetailResponse.SafeUserSummary(
                                comment.getUser().getId(),
                                comment.getUser().getFullName(),
                                comment.getUser().getRole()
                        ) : null;

                commentTarget = new ModerationReportDetailResponse.CommentTargetDetail(
                        comment.getId(),
                        comment.getIssue() != null ? comment.getIssue().getId() : null,
                        comment.getIssue() != null ? comment.getIssue().getTitle() : null,
                        comment.getContent(),
                        comment.getModerationStatus(),
                        comment.isDeleted(),
                        comment.getCreatedAt(),
                        authorSummary
                );
            }
        }

        List<ModerationActionResponse> actionHistory = actionRepository.findByReport_IdOrderByCreatedAtDesc(reportId)
                .stream()
                .map(ModerationActionResponse::fromEntity)
                .toList();

        return new ModerationReportDetailResponse(
                report.getId(),
                report.getStatus(),
                report.getReason(),
                report.getDescription(),
                report.getTargetType(),
                report.getTargetId(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getResolvedAt(),
                reporterSummary,
                resolverSummary,
                issueTarget,
                commentTarget,
                actionHistory
        );
    }

    @Override
    @Transactional
    public ModerationReportResponse reviewReport(UUID reportId) {
        verifyModeratorRole();
        ModerationReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ModerationReport", reportId));

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw new ConflictException("Report has already been " + report.getStatus().name().toLowerCase() + " by another moderator");
        }

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

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw new ConflictException("Report has already been " + report.getStatus().name().toLowerCase() + " by another moderator");
        }

        if (request.action() == ModerationActionType.REMOVE_COMMENT && report.getTargetType() != ModerationTargetType.COMMENT) {
            throw new IllegalArgumentException("REMOVE_COMMENT action is only valid for comment targets");
        }

        if (request.action() == ModerationActionType.RESTRICT_USER && !"ADMIN".equalsIgnoreCase(moderator.getRole())) {
            throw AuthException.forbidden("Only administrators can execute user restriction actions");
        }

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

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw new ConflictException("Report has already been " + report.getStatus().name().toLowerCase() + " by another moderator");
        }

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
        UserEntity admin = verifyAdminRole();
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
                admin,
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
        UserEntity admin = verifyAdminRole();
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
                admin,
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
        if (!"MODERATOR".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role) && !"OFFICER".equalsIgnoreCase(role)) {
            throw AuthException.forbidden("Only moderators or administrators can perform moderation actions");
        }
        return currentUser;
    }

    private UserEntity verifyAdminRole() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required");
        }
        String role = currentUser.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"OFFICER".equalsIgnoreCase(role)) {
            throw AuthException.forbidden("Only administrators can perform user restrictions");
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
                if (status == ModerationStatus.HIDDEN && !comment.isDeleted()) {
                    comment.softDelete();
                }
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
