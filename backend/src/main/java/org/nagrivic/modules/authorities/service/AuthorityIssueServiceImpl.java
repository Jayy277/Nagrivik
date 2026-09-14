package org.nagrivic.modules.authorities.service;

import jakarta.persistence.criteria.Predicate;
import org.nagrivic.common.error.ConflictException;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.activity.dto.ActivityResponse;
import org.nagrivic.modules.activity.service.IssueActivityService;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.authorities.dto.*;
import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.comments.repository.CommentRepository;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.moderation.model.ModerationStatus;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.dto.StatusHistoryItemDto;
import org.nagrivic.modules.statushistory.service.StatusHistoryService;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthorityIssueServiceImpl implements AuthorityIssueService {

    private final AuthorityScopeService authorityScopeService;
    private final IssueRepository issueRepository;
    private final StatusHistoryService statusHistoryService;
    private final CommentRepository commentRepository;
    private final MediaRepository mediaRepository;
    private final IssueActivityService issueActivityService;
    private final SupportRepository supportRepository;
    private final org.nagrivic.modules.authorities.repository.ResolutionEvidenceRepository resolutionEvidenceRepository;

    public AuthorityIssueServiceImpl(
            AuthorityScopeService authorityScopeService,
            IssueRepository issueRepository,
            StatusHistoryService statusHistoryService,
            CommentRepository commentRepository,
            MediaRepository mediaRepository,
            IssueActivityService issueActivityService,
            SupportRepository supportRepository,
            org.nagrivic.modules.authorities.repository.ResolutionEvidenceRepository resolutionEvidenceRepository
    ) {
        this.authorityScopeService = authorityScopeService;
        this.issueRepository = issueRepository;
        this.statusHistoryService = statusHistoryService;
        this.commentRepository = commentRepository;
        this.mediaRepository = mediaRepository;
        this.issueActivityService = issueActivityService;
        this.supportRepository = supportRepository;
        this.resolutionEvidenceRepository = resolutionEvidenceRepository;
    }

    @Override
    public AuthorityDashboardMetrics getDashboardMetrics(UserEntity user) {
        Specification<IssueEntity> scopeSpec = authorityScopeService.createScopeSpecification(user);

        long total = issueRepository.count(scopeSpec);
        long reported = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("status"), IssueStatus.REPORTED)));
        long verified = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("status"), IssueStatus.VERIFIED)));
        long acknowledged = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("status"), IssueStatus.ACKNOWLEDGED)));
        long inProgress = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("status"), IssueStatus.IN_PROGRESS)));
        long resolved = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("status"), IssueStatus.RESOLVED)));
        long citizenVerified = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("status"), IssueStatus.CITIZEN_VERIFIED)));
        long notFixed = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("status"), IssueStatus.NOT_FIXED)));

        long highPriority = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("priority").get("priorityLevel"), PriorityLevel.HIGH)));
        long criticalPriority = issueRepository.count(scopeSpec.and((r, q, cb) -> cb.equal(r.get("priority").get("priorityLevel"), PriorityLevel.CRITICAL)));

        long actionable = reported + verified + acknowledged + inProgress + notFixed;

        List<AuthorityScopeDto> activeAssignments = authorityScopeService.getActiveScopes(user);

        return new AuthorityDashboardMetrics(
                total,
                reported,
                verified,
                acknowledged,
                inProgress,
                resolved,
                citizenVerified,
                notFixed,
                highPriority,
                criticalPriority,
                actionable,
                activeAssignments
        );
    }

    @Override
    public Page<AuthorityIssueItemResponse> findScopedIssues(
            UserEntity user,
            AuthorityIssueFilter filter,
            Pageable pageable
    ) {
        Specification<IssueEntity> spec = authorityScopeService.createScopeSpecification(user);

        if (filter != null) {
            if (filter.status() != null) {
                spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), filter.status()));
            }
            if (filter.priority() != null) {
                spec = spec.and((r, q, cb) -> cb.equal(r.get("priority").get("priorityLevel"), filter.priority()));
            }
            if (filter.categoryId() != null) {
                spec = spec.and((r, q, cb) -> cb.equal(r.get("category").get("id"), filter.categoryId()));
            }
            if (filter.wardId() != null) {
                spec = spec.and((r, q, cb) -> cb.equal(r.get("ward").get("id"), filter.wardId()));
            }
            if (filter.departmentId() != null) {
                spec = spec.and((r, q, cb) -> cb.equal(r.get("department").get("id"), filter.departmentId()));
            }
            if (filter.fromDate() != null) {
                spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("createdAt"), filter.toDate()));
            }
            if (filter.search() != null && !filter.search().trim().isEmpty()) {
                String term = "%" + filter.search().trim().toLowerCase() + "%";
                spec = spec.and((r, q, cb) -> cb.or(
                        cb.like(cb.lower(r.get("title")), term),
                        cb.like(cb.lower(r.get("description")), term)
                ));
            }
        }

        return issueRepository.findAll(spec, pageable).map(this::toIssueItemResponse);
    }

    @Override
    public AuthorityIssueDetailResponse getScopedIssueDetail(UUID issueId, UserEntity user) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        authorityScopeService.validateAuthorityScope(user, issue);

        // Media
        List<MediaEntity> mediaEntities = mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(issueId);
        List<AuthorityIssueDetailResponse.MediaDto> mediaList = mediaEntities.stream()
                .map(m -> new AuthorityIssueDetailResponse.MediaDto(
                        m.getId(),
                        "/api/media/" + m.getStorageKey(),
                        m.getMediaType() != null ? m.getMediaType().name() : "IMAGE"
                ))
                .toList();

        // Support & comment counts
        int supportCount = (int) supportRepository.countByIssue_Id(issueId);
        int commentCount = (int) commentRepository.countByIssue_IdAndDeletedAtIsNull(issueId);

        // Safe comments
        Page<CommentEntity> commentsPage = commentRepository.findByIssue_IdAndModerationStatusNot(
                issueId,
                ModerationStatus.HIDDEN,
                PageRequest.of(0, 50)
        );
        List<AuthorityIssueDetailResponse.AuthorityCommentDto> comments = commentsPage.getContent().stream()
                .map(c -> {
                    String role = c.getUser() != null ? c.getUser().getRole() : "CITIZEN";
                    String initials = getInitials(c.getUser() != null ? c.getUser().getFullName() : null);
                    return new AuthorityIssueDetailResponse.AuthorityCommentDto(
                            c.getId(),
                            c.getContent(),
                            role,
                            initials,
                            c.getCreatedAt()
                    );
                })
                .toList();

        // Activity timeline
        Page<ActivityResponse> activityPage = issueActivityService.getIssueActivities(issueId, PageRequest.of(0, 50));
        List<ActivityResponse> activities = activityPage.getContent();

        // Status history
        List<StatusHistoryItemDto> statusHistory = statusHistoryService.getStatusHistory(issueId).history();

        // Allowed next authority transitions
        List<IssueStatus> allowedTransitions = new ArrayList<>();
        for (IssueStatus target : IssueStatus.values()) {
            if (issue.getStatus().canTransitionTo(target)) {
                // Citizen-only transitions: CITIZEN_VERIFIED, NOT_FIXED
                if (target != IssueStatus.CITIZEN_VERIFIED && target != IssueStatus.NOT_FIXED) {
                    allowedTransitions.add(target);
                }
            }
        }

        // Resolution evidence
        List<org.nagrivic.modules.authorities.dto.ResolutionEvidenceResponse> resolutionEvidence = resolutionEvidenceRepository
                .findByIssue_IdOrderByCreatedAtAsc(issueId)
                .stream()
                .map(org.nagrivic.modules.authorities.dto.ResolutionEvidenceResponse::fromEntity)
                .toList();

        // Location summary
        String locSummary = "Location unmapped";
        Double lat = null;
        Double lon = null;
        if (issue.getLocation() != null) {
            lat = issue.getLocation().getLatitude();
            lon = issue.getLocation().getLongitude();
            locSummary = String.format("Lat: %.4f, Lon: %.4f", lat, lon);
        }

        // Civic responsibility
        AuthorityIssueDetailResponse.CivicResponsibilityDto civicResp = new AuthorityIssueDetailResponse.CivicResponsibilityDto(
                issue.getResponsibilityStatus(),
                issue.getCivicBody() != null ? new AuthorityIssueDetailResponse.NamedEntityDto(issue.getCivicBody().getId(), issue.getCivicBody().getName(), null) : null,
                issue.getCity() != null ? new AuthorityIssueDetailResponse.NamedEntityDto(issue.getCity().getId(), issue.getCity().getName(), null) : null,
                issue.getWard() != null ? new AuthorityIssueDetailResponse.NamedEntityDto(issue.getWard().getId(), issue.getWard().getWardName(), issue.getWard().getWardNumber()) : null,
                issue.getDepartment() != null ? new AuthorityIssueDetailResponse.NamedEntityDto(issue.getDepartment().getId(), issue.getDepartment().getName(), issue.getDepartment().getCode()) : null,
                issue.getResponsibilityResolvedAt(),
                issue.getResponsibilitySource()
        );

        PriorityLevel priorityLevel = issue.getPriority() != null ? issue.getPriority().getPriorityLevel() : PriorityLevel.MEDIUM;
        Integer priorityScore = issue.getPriority() != null ? issue.getPriority().getScore() : 50;

        return new AuthorityIssueDetailResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                new AuthorityIssueDetailResponse.CategoryDto(
                        issue.getCategory().getId(),
                        issue.getCategory().getName(),
                        issue.getCategory().getSlug()
                ),
                issue.getStatus(),
                priorityLevel,
                priorityScore,
                civicResp,
                locSummary,
                lat,
                lon,
                mediaList,
                supportCount,
                commentCount,
                comments,
                activities,
                statusHistory,
                allowedTransitions,
                resolutionEvidence,
                issue.getVersion(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public IssueResponse changeIssueStatus(UUID issueId, ChangeStatusRequest request, UserEntity user) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        authorityScopeService.validateAuthorityScope(user, issue);

        IssueStatus targetStatus = request.status();
        if (targetStatus == IssueStatus.CITIZEN_VERIFIED || targetStatus == IssueStatus.NOT_FIXED) {
            throw AuthException.forbidden("Authorities cannot mark issues " + targetStatus + ". Citizen verification is reserved for the reporter.");
        }

        // Check optimistic locking version
        if (request.version() != null && !request.version().equals(issue.getVersion())) {
            throw new ConflictException("Issue has been modified by another authority user. Please refresh and try again.");
        }

        // Consequential transition reason requirement
        if (targetStatus == IssueStatus.RESOLVED) {
            if (request.reason() == null || request.reason().trim().isEmpty()) {
                throw new IllegalArgumentException("A reason is mandatory when marking an issue as RESOLVED");
            }
        }

        return statusHistoryService.changeStatus(issueId, request);
    }

    private AuthorityIssueItemResponse toIssueItemResponse(IssueEntity issue) {
        String descSnippet = issue.getDescription() != null && issue.getDescription().length() > 120
                ? issue.getDescription().substring(0, 120) + "..."
                : issue.getDescription();

        PriorityLevel priorityLevel = issue.getPriority() != null ? issue.getPriority().getPriorityLevel() : PriorityLevel.MEDIUM;
        Integer priorityScore = issue.getPriority() != null ? issue.getPriority().getScore() : 50;

        int supportCount = (int) supportRepository.countByIssue_Id(issue.getId());
        int commentCount = (int) commentRepository.countByIssue_IdAndDeletedAtIsNull(issue.getId());

        boolean actionable = issue.getStatus() == IssueStatus.REPORTED ||
                issue.getStatus() == IssueStatus.VERIFIED ||
                issue.getStatus() == IssueStatus.ACKNOWLEDGED ||
                issue.getStatus() == IssueStatus.IN_PROGRESS ||
                issue.getStatus() == IssueStatus.NOT_FIXED;

        return new AuthorityIssueItemResponse(
                issue.getId(),
                issue.getTitle(),
                descSnippet,
                issue.getCategory().getName(),
                issue.getCategory().getSlug(),
                issue.getStatus(),
                priorityLevel,
                priorityScore,
                issue.getCivicBody() != null ? issue.getCivicBody().getName() : null,
                issue.getCity() != null ? issue.getCity().getName() : null,
                issue.getWard() != null ? issue.getWard().getWardName() : null,
                issue.getWard() != null ? issue.getWard().getWardNumber() : null,
                issue.getDepartment() != null ? issue.getDepartment().getName() : null,
                issue.getDepartment() != null ? issue.getDepartment().getCode() : null,
                supportCount,
                commentCount,
                actionable,
                issue.getVersion(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "C.";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase() + ".";
        }
        return parts[0].substring(0, 1).toUpperCase() + "." + parts[parts.length - 1].substring(0, 1).toUpperCase() + ".";
    }
}
