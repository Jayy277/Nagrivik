package org.nagrivic.modules.statushistory.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.dto.StatusHistoryItemDto;
import org.nagrivic.modules.statushistory.dto.StatusHistoryResponse;
import org.nagrivic.modules.statushistory.dto.VerifyResolutionRequest;
import org.nagrivic.modules.statushistory.entity.StatusHistoryEntity;
import org.nagrivic.modules.statushistory.repository.StatusHistoryRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StatusHistoryService {

    private final StatusHistoryRepository statusHistoryRepository;
    private final IssueRepository issueRepository;
    private final IssueService issueService;
    private final CurrentUserService currentUserService;
    private final org.nagrivic.modules.priority.service.IssuePriorityService issuePriorityService;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;
    private final org.nagrivic.modules.notifications.service.NotificationService notificationService;
    private final org.nagrivic.modules.authorities.repository.AuthorityAssignmentRepository authorityAssignmentRepository;
    private final org.nagrivic.modules.authorities.service.AuthorityScopeService authorityScopeService;

    public StatusHistoryService(
            StatusHistoryRepository statusHistoryRepository,
            IssueRepository issueRepository,
            @Lazy IssueService issueService,
            CurrentUserService currentUserService,
            @Lazy org.nagrivic.modules.priority.service.IssuePriorityService issuePriorityService,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService,
            @Lazy org.nagrivic.modules.notifications.service.NotificationService notificationService,
            @Lazy org.nagrivic.modules.authorities.repository.AuthorityAssignmentRepository authorityAssignmentRepository,
            @Lazy org.nagrivic.modules.authorities.service.AuthorityScopeService authorityScopeService
    ) {
        this.statusHistoryRepository = statusHistoryRepository;
        this.issueRepository = issueRepository;
        this.issueService = issueService;
        this.currentUserService = currentUserService;
        this.issuePriorityService = issuePriorityService;
        this.issueActivityService = issueActivityService;
        this.notificationService = notificationService;
        this.authorityAssignmentRepository = authorityAssignmentRepository;
        this.authorityScopeService = authorityScopeService;
    }

    /**
     * Atomically records the initial status history entry when a new issue is created.
     */
    @Transactional
    public void recordInitialStatus(IssueEntity issue, UserEntity reporter) {
        StatusHistoryEntity history = new StatusHistoryEntity(
                issue,
                null,
                IssueStatus.REPORTED,
                reporter,
                null
        );
        statusHistoryRepository.save(history);
    }

    /**
     * Privileged operational status transition for municipal officers and administrators.
     */
    @Transactional
    public IssueResponse changeStatus(UUID issueId, ChangeStatusRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String role = currentUser.getRole();
        if (!"OFFICER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw AuthException.forbidden("Only municipal officers or administrators can update operational issue status");
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        // Optimistic locking check
        if (request.version() != null && !request.version().equals(issue.getVersion())) {
            throw new org.nagrivic.common.error.ConflictException("Issue has been modified by another authority user. Please refresh and try again.");
        }

        // Validate authority scope if active assignments exist for officer or when scoped
        List<org.nagrivic.modules.authorities.entity.AuthorityAssignmentEntity> assignments =
                authorityAssignmentRepository.findByUser_IdAndActiveTrue(currentUser.getId());
        if (!assignments.isEmpty() || ("OFFICER".equalsIgnoreCase(role) && authorityAssignmentRepository.count() > 0)) {
            authorityScopeService.validateAuthorityScope(currentUser, issue);
        }

        IssueStatus currentStatus = issue.getStatus();
        IssueStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            throw new org.nagrivic.common.error.ConflictException("Issue is already in status " + targetStatus);
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition: cannot transition issue from " + currentStatus + " to " + targetStatus
            );
        }

        if (targetStatus == IssueStatus.RESOLVED) {
            if (request.reason() == null || request.reason().trim().isEmpty()) {
                throw new IllegalArgumentException("A reason is mandatory when marking an issue as RESOLVED");
            }
        }

        if (targetStatus == IssueStatus.NOT_FIXED) {
            if (request.reason() == null || request.reason().trim().isEmpty()) {
                throw new IllegalArgumentException("A reason is mandatory when marking an issue as NOT_FIXED");
            }
        }

        issue.setStatus(targetStatus);
        issueRepository.save(issue);

        StatusHistoryEntity history = new StatusHistoryEntity(
                issue,
                currentStatus,
                targetStatus,
                currentUser,
                request.reason()
        );
        statusHistoryRepository.save(history);

        java.util.Map<String, Object> activityData = new java.util.HashMap<>();
        if (currentStatus != null) {
            activityData.put("from", currentStatus.name());
        }
        activityData.put("to", targetStatus.name());
        if (request.reason() != null && !request.reason().trim().isEmpty()) {
            activityData.put("reason", request.reason().trim());
        }
        issueActivityService.recordActivity(
                issue,
                org.nagrivic.modules.activity.model.IssueActivityType.STATUS_CHANGED,
                currentUser,
                activityData
        );

        issuePriorityService.recalculatePriority(issueId);
        notificationService.handleStatusChange(issue, currentStatus, targetStatus, currentUser);

        return issueService.getIssueById(issueId);
    }

    /**
     * Citizen resolution verification operation.
     * Restricted to the original authenticated issue reporter when issue is in RESOLVED status.
     */
    @Transactional
    public IssueResponse verifyResolution(UUID issueId, VerifyResolutionRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        if (!issue.getReporter().getId().equals(currentUser.getId())) {
            throw AuthException.forbidden("Only the original issue reporter can verify issue resolution");
        }

        if (issue.getStatus() != IssueStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    "Resolution can only be verified when the issue is in RESOLVED status. Current status: " + issue.getStatus()
            );
        }

        IssueStatus currentStatus = issue.getStatus();
        IssueStatus targetStatus;
        if (Boolean.TRUE.equals(request.fixed())) {
            targetStatus = IssueStatus.CITIZEN_VERIFIED;
        } else {
            targetStatus = IssueStatus.NOT_FIXED;
            if (request.reason() == null || request.reason().trim().isEmpty()) {
                throw new IllegalArgumentException("A reason is required when reporting that an issue was NOT_FIXED");
            }
        }

        issue.setStatus(targetStatus);
        issueRepository.save(issue);

        StatusHistoryEntity history = new StatusHistoryEntity(
                issue,
                currentStatus,
                targetStatus,
                currentUser,
                request.reason()
        );
        statusHistoryRepository.save(history);

        java.util.Map<String, Object> verifyActivityData = new java.util.HashMap<>();
        verifyActivityData.put("from", currentStatus.name());
        verifyActivityData.put("to", targetStatus.name());
        if (request.reason() != null && !request.reason().trim().isEmpty()) {
            verifyActivityData.put("reason", request.reason().trim());
        }
        issueActivityService.recordActivity(
                issue,
                org.nagrivic.modules.activity.model.IssueActivityType.STATUS_CHANGED,
                currentUser,
                verifyActivityData
        );

        issuePriorityService.recalculatePriority(issueId);
        notificationService.handleStatusChange(issue, currentStatus, targetStatus, currentUser);

        return issueService.getIssueById(issueId);
    }

    /**
     * Publicly queries the chronological status history for an issue.
     */
    public StatusHistoryResponse getStatusHistory(UUID issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue", issueId);
        }

        List<StatusHistoryEntity> historyList = statusHistoryRepository.findByIssue_IdOrderByCreatedAtAsc(issueId);
        List<StatusHistoryItemDto> items = historyList.stream()
                .map(StatusHistoryItemDto::fromEntity)
                .toList();

        return new StatusHistoryResponse(issueId, items);
    }
}
