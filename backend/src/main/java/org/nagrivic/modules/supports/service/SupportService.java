package org.nagrivic.modules.supports.service;

import org.nagrivic.common.error.ConflictException;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.supports.entity.SupportEntity;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SupportService {

    private final SupportRepository supportRepository;
    private final IssueRepository issueRepository;
    private final CurrentUserService currentUserService;
    private final org.nagrivic.modules.priority.service.IssuePriorityService issuePriorityService;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;
    private final org.nagrivic.modules.moderation.service.ModerationService moderationService;
    private final org.nagrivic.modules.moderation.ratelimit.RateLimiter rateLimiter;

    @org.springframework.beans.factory.annotation.Value("${nagrivic.moderation.rate-limit.support-per-minute:20}")
    private int supportPerMinute;

    public SupportService(
            SupportRepository supportRepository,
            IssueRepository issueRepository,
            CurrentUserService currentUserService,
            org.nagrivic.modules.priority.service.IssuePriorityService issuePriorityService,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService,
            org.nagrivic.modules.moderation.service.ModerationService moderationService,
            org.nagrivic.modules.moderation.ratelimit.RateLimiter rateLimiter
    ) {
        this.supportRepository = supportRepository;
        this.issueRepository = issueRepository;
        this.currentUserService = currentUserService;
        this.issuePriorityService = issuePriorityService;
        this.issueActivityService = issueActivityService;
        this.moderationService = moderationService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Adds citizen support to an existing civic issue.
     * The supporter identity is derived exclusively from the authenticated JWT.
     * Duplicate support is rejected with 409 Conflict.
     */
    @Transactional
    public void addSupport(UUID issueId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required to support an issue");
        }
        if (!currentUser.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }
        moderationService.checkUserNotRestricted(currentUser);

        // Rate limit support actions
        rateLimiter.checkLimit("support:" + currentUser.getId(), supportPerMinute, java.time.Duration.ofMinutes(1));

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));
        if (issue.getModerationStatus() == org.nagrivic.modules.moderation.model.ModerationStatus.HIDDEN) {
            throw new ResourceNotFoundException("Issue", issueId);
        }

        if (supportRepository.existsByIssue_IdAndUser_Id(issueId, currentUser.getId())) {
            throw new ConflictException("You have already supported this issue");
        }

        try {
            SupportEntity support = new SupportEntity(issue, currentUser);
            supportRepository.saveAndFlush(support);
            issuePriorityService.recalculatePriority(issueId);

            long count = supportRepository.countByIssue_Id(issueId);
            issueActivityService.recordActivity(
                    issue,
                    org.nagrivic.modules.activity.model.IssueActivityType.SUPPORT_ADDED,
                    currentUser,
                    java.util.Map.of("supportCount", count)
            );
        } catch (DataIntegrityViolationException ex) {
            // Concurrent race condition handling
            throw new ConflictException("You have already supported this issue");
        }
    }

    /**
     * Removes the authenticated citizen's support from the specified issue.
     * Only the user who supported can remove their own support.
     * If no support exists, throws 404 ResourceNotFoundException.
     */
    @Transactional
    public void removeSupport(UUID issueId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            throw AuthException.unauthorized("Authentication required to remove support");
        }
        if (!currentUser.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        SupportEntity support = supportRepository.findByIssue_IdAndUser_Id(issue.getId(), currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You have not supported this issue"));

        supportRepository.delete(support);
        supportRepository.flush();
        issuePriorityService.recalculatePriority(issueId);

        long count = supportRepository.countByIssue_Id(issueId);
        issueActivityService.recordActivity(
                issue,
                org.nagrivic.modules.activity.model.IssueActivityType.SUPPORT_REMOVED,
                currentUser,
                java.util.Map.of("supportCount", count)
        );
    }

    /**
     * Retrieves total support count for an issue.
     */
    public long getSupportCount(UUID issueId) {
        return supportRepository.countByIssue_Id(issueId);
    }

    /**
     * Checks if a user has supported a given issue.
     */
    public boolean hasUserSupported(UUID issueId, UUID userId) {
        if (userId == null) {
            return false;
        }
        return supportRepository.existsByIssue_IdAndUser_Id(issueId, userId);
    }
}
