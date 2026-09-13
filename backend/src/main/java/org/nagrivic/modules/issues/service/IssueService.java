package org.nagrivic.modules.issues.service;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private static final Logger log = LoggerFactory.getLogger(IssueService.class);

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final MediaRepository mediaRepository;
    private final org.nagrivic.modules.supports.repository.SupportRepository supportRepository;
    private final org.nagrivic.modules.comments.repository.CommentRepository commentRepository;
    private final org.nagrivic.modules.auth.service.CurrentUserService currentUserService;
    private final org.nagrivic.modules.duplicates.service.DuplicateDetectionService duplicateDetectionService;
    private final org.nagrivic.modules.statushistory.service.StatusHistoryService statusHistoryService;
    private final org.nagrivic.modules.civicgeography.service.CivicGeographyService civicGeographyService;
    private final IssueResponsibilityService issueResponsibilityService;
    private final org.nagrivic.modules.priority.service.IssuePriorityService issuePriorityService;
    private final org.nagrivic.modules.priority.repository.IssuePriorityRepository issuePriorityRepository;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;
    private final org.nagrivic.modules.moderation.service.ModerationService moderationService;
    private final org.nagrivic.modules.moderation.ratelimit.RateLimiter rateLimiter;
    private final org.nagrivic.modules.moderation.service.ContentAbuseValidator contentAbuseValidator;
    private final org.nagrivic.modules.notifications.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${nagrivic.moderation.rate-limit.issue-per-hour:10}")
    private int issuePerHour;

    public IssueService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            LocationRepository locationRepository,
            MediaRepository mediaRepository,
            org.nagrivic.modules.supports.repository.SupportRepository supportRepository,
            org.nagrivic.modules.comments.repository.CommentRepository commentRepository,
            org.nagrivic.modules.auth.service.CurrentUserService currentUserService,
            org.nagrivic.modules.duplicates.service.DuplicateDetectionService duplicateDetectionService,
            org.nagrivic.modules.statushistory.service.StatusHistoryService statusHistoryService,
            org.nagrivic.modules.civicgeography.service.CivicGeographyService civicGeographyService,
            IssueResponsibilityService issueResponsibilityService,
            org.nagrivic.modules.priority.service.IssuePriorityService issuePriorityService,
            org.nagrivic.modules.priority.repository.IssuePriorityRepository issuePriorityRepository,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService,
            org.nagrivic.modules.moderation.service.ModerationService moderationService,
            org.nagrivic.modules.moderation.ratelimit.RateLimiter rateLimiter,
            org.nagrivic.modules.moderation.service.ContentAbuseValidator contentAbuseValidator,
            @org.springframework.context.annotation.Lazy org.nagrivic.modules.notifications.service.NotificationService notificationService
    ) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.mediaRepository = mediaRepository;
        this.supportRepository = supportRepository;
        this.commentRepository = commentRepository;
        this.currentUserService = currentUserService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.statusHistoryService = statusHistoryService;
        this.civicGeographyService = civicGeographyService;
        this.issueResponsibilityService = issueResponsibilityService;
        this.issuePriorityService = issuePriorityService;
        this.issuePriorityRepository = issuePriorityRepository;
        this.issueActivityService = issueActivityService;
        this.moderationService = moderationService;
        this.rateLimiter = rateLimiter;
        this.contentAbuseValidator = contentAbuseValidator;
        this.notificationService = notificationService;
    }

    @Transactional
    public IssueEntity createIssue(UserEntity reporter, UUID categoryId, UUID locationId, String title, String description) {
        return createIssue(reporter, categoryId, locationId, title, description, null);
    }

    @Transactional
    public IssueEntity createIssue(
            UserEntity reporter,
            UUID categoryId,
            UUID locationId,
            String title,
            String description,
            org.nagrivic.modules.priority.model.IssueSeverity severity
    ) {
        if (reporter == null) {
            throw new IllegalArgumentException("Reporter is required");
        }
        if (!reporter.isActive()) {
            throw org.nagrivic.modules.auth.exception.AuthException.forbidden("User account is inactive");
        }
        moderationService.checkUserNotRestricted(reporter);
        rateLimiter.checkLimit("issue:" + reporter.getId(), issuePerHour, java.time.Duration.ofHours(1));

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Issue title cannot be blank");
        }
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Cannot report an issue under an inactive category: " + category.getName());
        }

        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));

        contentAbuseValidator.validateIssueSubmission(reporter.getId(), title, categoryId, locationId);

        IssueEntity issue = new IssueEntity(reporter, category, location, title.trim(), description);
        if (severity != null) {
            issue.setSeverity(severity);
        }

        // Server-controlled civic responsibility enrichment
        try {
            issueResponsibilityService.resolveResponsibility(issue);
        } catch (Exception e) {
            // Principle: "Unresolved does not mean invalid"
            // Failures or missing mappings must never fail issue creation
            log.warn("Civic responsibility resolution failed gracefully for new issue: {}", e.getMessage());
            issue.setResponsibilityStatus(ResponsibilityStatus.UNRESOLVED);
        }

        IssueEntity saved = issueRepository.save(issue);
        statusHistoryService.recordInitialStatus(saved, reporter);
        issueActivityService.recordActivity(
                saved,
                org.nagrivic.modules.activity.model.IssueActivityType.ISSUE_REPORTED,
                reporter,
                java.util.Map.of("title", saved.getTitle())
        );
        if (saved.getResponsibilityStatus() == ResponsibilityStatus.RESOLVED) {
            java.util.Map<String, Object> respData = new java.util.HashMap<>();
            if (saved.getCivicBody() != null) respData.put("civicBodyId", saved.getCivicBody().getId().toString());
            if (saved.getCity() != null) respData.put("cityId", saved.getCity().getId().toString());
            if (saved.getWard() != null) respData.put("wardId", saved.getWard().getId().toString());
            if (saved.getDepartment() != null) respData.put("departmentId", saved.getDepartment().getId().toString());
            issueActivityService.recordActivity(
                    saved,
                    org.nagrivic.modules.activity.model.IssueActivityType.RESPONSIBILITY_RESOLVED,
                    null,
                    respData
            );
            notificationService.handleResponsibilityResolved(saved);
        }
        issuePriorityService.calculatePriority(saved);
        return saved;
    }

    @Transactional
    public IssueEntity createIssue(UUID reportedBy, UUID categoryId, UUID locationId, String title, String description) {
        UserEntity reporter = userRepository.findById(reportedBy)
                .orElseThrow(() -> new IllegalArgumentException("Reporter user not found: " + reportedBy));
        return createIssue(reporter, categoryId, locationId, title, description);
    }

    @Transactional
    public IssueResponse createIssueFromRequest(CreateIssueRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        UUID resolvedLocationId = request.locationId();
        if (request.location() != null) {
            Double lat = request.location().latitude();
            Double lon = request.location().longitude();
            if (lat == null || lon == null || !Double.isFinite(lat) || !Double.isFinite(lon)) {
                throw new IllegalArgumentException("Valid finite latitude and longitude coordinates are required");
            }
            if (lat < -90.0 || lat > 90.0) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
            }
            if (lon < -180.0 || lon > 180.0) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
            }
            java.math.BigDecimal acc = null;
            if (request.location().accuracyMeters() != null) {
                if (!Double.isFinite(request.location().accuracyMeters()) || request.location().accuracyMeters() < 0) {
                    throw new IllegalArgumentException("Accuracy meters must be a non-negative finite number");
                }
                acc = java.math.BigDecimal.valueOf(request.location().accuracyMeters());
            }
            LocationEntity newLocation = new LocationEntity(lat, lon, acc);
            LocationEntity savedLocation = locationRepository.save(newLocation);
            resolvedLocationId = savedLocation.getId();
        } else if (resolvedLocationId == null) {
            throw new IllegalArgumentException("Location is required: provide either location coordinates or locationId");
        }

        IssueEntity issue = createIssue(
                currentUser,
                request.categoryId(),
                resolvedLocationId,
                request.title(),
                request.description(),
                request.severity()
        );
        org.nagrivic.modules.civicgeography.dto.CivicAreaResponse civicArea = civicGeographyService.enrichIssue(issue).orElse(null);
        return IssueResponse.fromEntity(issue, Collections.emptyList(), 0L, 0L, false, civicArea);
    }

    public IssueResponse getIssueById(UUID id) {
        IssueEntity issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", id));
        if (issue.getModerationStatus() == org.nagrivic.modules.moderation.model.ModerationStatus.HIDDEN) {
            throw new ResourceNotFoundException("Issue", id);
        }
        List<MediaEntity> mediaList = mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(id);
        long supportCount = supportRepository.countByIssue_Id(id);
        long commentCount = commentRepository.countByIssue_IdAndDeletedAtIsNull(id);
        Optional<UUID> currentUserIdOpt = currentUserService.getCurrentUserIdOpt();
        boolean supportedByCurrentUser = currentUserIdOpt.isPresent()
                && supportRepository.existsByIssue_IdAndUser_Id(id, currentUserIdOpt.get());
        org.nagrivic.modules.civicgeography.dto.CivicAreaResponse civicArea = civicGeographyService.enrichIssue(issue).orElse(null);
        return IssueResponse.fromEntity(issue, mediaList, supportCount, commentCount, supportedByCurrentUser, civicArea);
    }

    public Page<IssueResponse> findIssues(IssueStatus status, UUID categoryId, UUID reportedBy, Pageable pageable) {
        return findIssues(status, categoryId, reportedBy, null, pageable);
    }

    public Page<IssueResponse> findIssues(
            IssueStatus status,
            UUID categoryId,
            UUID reportedBy,
            org.nagrivic.modules.priority.model.PriorityLevel priority,
            Pageable pageable
    ) {
        org.nagrivic.modules.issues.dto.IssueDiscoveryFilter filter = org.nagrivic.modules.issues.dto.IssueDiscoveryFilter.builder()
                .status(status)
                .categoryId(categoryId)
                .reportedBy(reportedBy)
                .priority(priority)
                .includeDuplicates(false)
                .build();
        return findIssues(filter, pageable);
    }

    public Page<IssueResponse> findMyIssues(
            UUID categoryId,
            IssueStatus status,
            org.nagrivic.modules.priority.model.PriorityLevel priority,
            org.nagrivic.modules.issues.model.IssueDiscoverySort sort,
            Pageable pageable
    ) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        org.nagrivic.modules.issues.dto.IssueDiscoveryFilter filter = org.nagrivic.modules.issues.dto.IssueDiscoveryFilter.builder()
                .categoryId(categoryId)
                .status(status)
                .priority(priority)
                .reportedBy(currentUser.getId())
                .sort(sort)
                .includeDuplicates(true)
                .build();
        return findIssues(filter, pageable);
    }

    public Page<IssueResponse> findIssues(
            org.nagrivic.modules.issues.dto.IssueDiscoveryFilter filter,
            Pageable pageable
    ) {
        if (filter.categoryId() != null) {
            categoryRepository.findById(filter.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", filter.categoryId()));
        }

        if (filter.latitude() != null || filter.longitude() != null) {
            if (filter.latitude() == null || filter.longitude() == null) {
                throw new IllegalArgumentException("Both latitude and longitude must be provided for geographic search");
            }
            if (filter.latitude() < -90.0 || filter.latitude() > 90.0) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
            }
            if (filter.longitude() < -180.0 || filter.longitude() > 180.0) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
            }
        }

        if (filter.radiusMeters() != null) {
            if (filter.radiusMeters() <= 0.0 || filter.radiusMeters() > 50000.0) {
                throw new IllegalArgumentException("radiusMeters must be greater than 0 and at most 50000 meters (50km)");
            }
        }

        if (filter.sort() == org.nagrivic.modules.issues.model.IssueDiscoverySort.NEAREST && !filter.hasGeographicSearch()) {
            throw new IllegalArgumentException("sort=NEAREST requires latitude and longitude");
        }

        if (filter.q() != null && !filter.q().trim().isEmpty()) {
            String qTrimmed = filter.q().trim();
            if (qTrimmed.length() < 2 || qTrimmed.length() > 100) {
                throw new IllegalArgumentException("Search query must be between 2 and 100 characters");
            }
        }

        Page<org.nagrivic.modules.issues.dto.IssueDiscoveryItem> discoveryPage = issueRepository.discoverIssues(filter, pageable);
        if (discoveryPage.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, discoveryPage.getTotalElements());
        }

        List<UUID> issueIds = discoveryPage.getContent().stream()
                .map(org.nagrivic.modules.issues.dto.IssueDiscoveryItem::issueId)
                .toList();

        List<IssueEntity> entities = issueRepository.findWithDetailsByIdIn(issueIds);
        java.util.Map<UUID, IssueEntity> entityMap = entities.stream()
                .collect(java.util.stream.Collectors.toMap(IssueEntity::getId, java.util.function.Function.identity()));

        // 1. Batch support counts
        java.util.Map<UUID, Long> supportCounts = new java.util.HashMap<>();
        List<Object[]> countResults = supportRepository.countSupportsByIssueIds(issueIds);
        for (Object[] row : countResults) {
            UUID issueId = (UUID) row[0];
            Long count = (Long) row[1];
            supportCounts.put(issueId, count);
        }

        // 2. Batch active comment counts
        java.util.Map<UUID, Long> commentCounts = new java.util.HashMap<>();
        List<Object[]> commentResults = commentRepository.countActiveCommentsByIssueIds(issueIds);
        for (Object[] row : commentResults) {
            UUID issueId = (UUID) row[0];
            Long count = (Long) row[1];
            commentCounts.put(issueId, count);
        }

        // 3. Batch user support check if authenticated
        java.util.Set<UUID> userSupportedIssueIds = new java.util.HashSet<>();
        Optional<UUID> currentUserIdOpt = currentUserService.getCurrentUserIdOpt();
        if (currentUserIdOpt.isPresent()) {
            userSupportedIssueIds.addAll(supportRepository.findSupportedIssueIdsByUser(currentUserIdOpt.get(), issueIds));
        }

        // 4. Batch priorities
        List<org.nagrivic.modules.priority.entity.IssuePriorityEntity> priorityList = issuePriorityRepository.findByIssue_IdIn(issueIds);
        java.util.Map<UUID, org.nagrivic.modules.priority.entity.IssuePriorityEntity> priorityMap = new java.util.HashMap<>();
        for (org.nagrivic.modules.priority.entity.IssuePriorityEntity p : priorityList) {
            priorityMap.put(p.getIssue().getId(), p);
        }

        List<IssueResponse> content = new ArrayList<>();
        for (org.nagrivic.modules.issues.dto.IssueDiscoveryItem item : discoveryPage.getContent()) {
            IssueEntity issue = entityMap.get(item.issueId());
            if (issue == null) continue;
            if (priorityMap.containsKey(issue.getId())) {
                issue.setPriority(priorityMap.get(issue.getId()));
            }
            long sCount = supportCounts.getOrDefault(issue.getId(), 0L);
            long cCount = commentCounts.getOrDefault(issue.getId(), 0L);
            boolean supported = userSupportedIssueIds.contains(issue.getId());
            org.nagrivic.modules.civicgeography.dto.CivicAreaResponse civicArea = civicGeographyService.enrichIssue(issue).orElse(null);
            org.nagrivic.modules.priority.dto.PriorityResponse priority = org.nagrivic.modules.priority.dto.PriorityResponse.fromEntity(priorityMap.get(issue.getId()));
            content.add(IssueResponse.fromEntity(
                    issue,
                    Collections.emptyList(),
                    sCount,
                    cCount,
                    supported,
                    civicArea,
                    priority,
                    item.distanceMeters()
            ));
        }

        return new org.springframework.data.domain.PageImpl<>(content, pageable, discoveryPage.getTotalElements());
    }

    public Optional<IssueEntity> findById(UUID id) {
        return issueRepository.findById(id);
    }

    public List<IssueEntity> findByReportedBy(UUID reportedBy) {
        return issueRepository.findByReporter_Id(reportedBy);
    }

    public List<IssueEntity> findByCategory(UUID categoryId) {
        return issueRepository.findByCategory_Id(categoryId);
    }

    public List<IssueEntity> findByStatus(IssueStatus status) {
        return issueRepository.findByStatus(status);
    }

    public org.nagrivic.modules.duplicates.dto.DuplicateCheckResponse checkDuplicates(org.nagrivic.modules.duplicates.dto.CheckDuplicatesRequest request) {
        return duplicateDetectionService.checkDuplicates(request);
    }
}
