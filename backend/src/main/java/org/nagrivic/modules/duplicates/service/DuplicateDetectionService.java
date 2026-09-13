package org.nagrivic.modules.duplicates.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.duplicates.dto.CheckDuplicatesRequest;
import org.nagrivic.modules.duplicates.dto.DuplicateCandidateDto;
import org.nagrivic.modules.duplicates.dto.DuplicateCheckResponse;
import org.nagrivic.modules.duplicates.repository.DuplicateCandidateProjection;
import org.nagrivic.modules.duplicates.repository.DuplicateCandidateRepository;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.dto.IssueResponse.CategorySummary;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class DuplicateDetectionService {

    private final DuplicateCandidateRepository candidateRepository;
    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final SupportRepository supportRepository;
    private final CurrentUserService currentUserService;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;
    private final org.nagrivic.modules.notifications.service.NotificationService notificationService;

    @Value("${nagrivic.duplicates.detection-radius-meters:100.0}")
    private double detectionRadiusMeters;

    @Value("${nagrivic.duplicates.max-citizen-candidates:5}")
    private int maxCitizenCandidates;

    public DuplicateDetectionService(
            DuplicateCandidateRepository candidateRepository,
            IssueRepository issueRepository,
            CategoryRepository categoryRepository,
            LocationRepository locationRepository,
            SupportRepository supportRepository,
            CurrentUserService currentUserService,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService,
            @org.springframework.context.annotation.Lazy org.nagrivic.modules.notifications.service.NotificationService notificationService
    ) {
        this.candidateRepository = candidateRepository;
        this.issueRepository = issueRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.supportRepository = supportRepository;
        this.currentUserService = currentUserService;
        this.issueActivityService = issueActivityService;
        this.notificationService = notificationService;
    }

    /**
     * Citizen pre-creation duplicate check.
     * Evaluates category and location coordinates, searches PostGIS for nearby candidates,
     * and returns up to 5 potential duplicate issues without persisting an issue.
     */
    public DuplicateCheckResponse checkDuplicates(CheckDuplicatesRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!currentUser.isActive()) {
            throw AuthException.forbidden("User account is inactive");
        }

        if (request.title() == null || request.title().trim().isEmpty()) {
            throw new IllegalArgumentException("Issue title cannot be blank");
        }

        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.categoryId()));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Cannot check duplicates under an inactive category: " + category.getName());
        }

        double targetLon;
        double targetLat;
        if (request.latitude() != null && request.longitude() != null) {
            if (!Double.isFinite(request.latitude()) || !Double.isFinite(request.longitude())) {
                throw new IllegalArgumentException("Coordinates must be valid finite numbers");
            }
            if (request.latitude() < -90.0 || request.latitude() > 90.0) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
            }
            if (request.longitude() < -180.0 || request.longitude() > 180.0) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
            }
            targetLat = request.latitude();
            targetLon = request.longitude();
        } else if (request.locationId() != null) {
            LocationEntity location = locationRepository.findById(request.locationId())
                    .orElseThrow(() -> new IllegalArgumentException("Location not found: " + request.locationId()));
            targetLon = location.getLongitude();
            targetLat = location.getLatitude();
        } else {
            throw new IllegalArgumentException("Location is required: provide either coordinates (latitude, longitude) or locationId");
        }

        double radius = (request.radiusMeters() != null && request.radiusMeters() > 0)
                ? request.radiusMeters()
                : detectionRadiusMeters;

        List<DuplicateCandidateProjection> candidateProjections = candidateRepository.findPotentialDuplicates(
                category.getId(),
                targetLon,
                targetLat,
                radius,
                null,
                maxCitizenCandidates
        );

        if (candidateProjections.isEmpty()) {
            return DuplicateCheckResponse.empty();
        }

        List<UUID> candidateIssueIds = candidateProjections.stream()
                .map(DuplicateCandidateProjection::getIssueId)
                .toList();

        Map<UUID, Long> supportCounts = new HashMap<>();
        List<Object[]> supportCountRows = supportRepository.countSupportsByIssueIds(candidateIssueIds);
        for (Object[] row : supportCountRows) {
            UUID issueId = (UUID) row[0];
            Long count = (Long) row[1];
            supportCounts.put(issueId, count);
        }

        List<DuplicateCandidateDto> candidates = candidateProjections.stream()
                .map(proj -> new DuplicateCandidateDto(
                        proj.getIssueId(),
                        proj.getTitle(),
                        new CategorySummary(proj.getCategoryId(), proj.getCategoryName(), proj.getCategorySlug()),
                        proj.getDistanceMeters(),
                        IssueStatus.valueOf(proj.getStatus()),
                        supportCounts.getOrDefault(proj.getIssueId(), 0L)
                ))
                .toList();

        return DuplicateCheckResponse.of(candidates);
    }

    /**
     * Internal duplicate candidate search for an existing issue.
     */
    public List<DuplicateCandidateProjection> findPotentialDuplicates(UUID issueId, Double radiusOverride) {
        IssueEntity targetIssue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        double radius = radiusOverride != null ? radiusOverride : detectionRadiusMeters;
        LocationEntity loc = targetIssue.getLocation();

        return candidateRepository.findPotentialDuplicates(
                targetIssue.getCategory().getId(),
                loc.getLongitude(),
                loc.getLatitude(),
                radius,
                targetIssue.getId(),
                20
        );
    }

    /**
     * Privileged duplicate linking operation for moderation/administrative workflows.
     * Normalizes target to root primary and prevents circular relationships.
     */
    @Transactional
    public IssueResponse linkDuplicate(UUID sourceIssueId, UUID primaryCandidateId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String role = currentUser.getRole();
        if (!"OFFICER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw AuthException.forbidden("Only municipal officers or administrators can link duplicate issues");
        }

        if (sourceIssueId.equals(primaryCandidateId)) {
            throw new IllegalArgumentException("An issue cannot be marked as a duplicate of itself");
        }

        IssueEntity sourceIssue = issueRepository.findById(sourceIssueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", sourceIssueId));

        IssueEntity targetIssue = issueRepository.findById(primaryCandidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", primaryCandidateId));

        // Traverse target's duplicateOf chain to find canonical root primary
        IssueEntity rootPrimary = targetIssue;
        Set<UUID> visited = new HashSet<>();
        visited.add(rootPrimary.getId());
        while (rootPrimary.getDuplicateOf() != null) {
            rootPrimary = rootPrimary.getDuplicateOf();
            if (!visited.add(rootPrimary.getId())) {
                throw new IllegalStateException("Circular duplicate relationship detected in target issue hierarchy");
            }
        }

        // Prevent circular relationship
        if (rootPrimary.getId().equals(sourceIssue.getId())) {
            throw new IllegalArgumentException("Cannot link duplicate: would create a circular duplicate relationship");
        }

        // Reparent any existing duplicates pointing to sourceIssue to point directly to rootPrimary
        List<IssueEntity> children = issueRepository.findByDuplicateOf_Id(sourceIssue.getId());
        for (IssueEntity child : children) {
            child.setDuplicateOf(rootPrimary);
            issueRepository.save(child);
        }

        sourceIssue.setDuplicateOf(rootPrimary);
        IssueEntity saved = issueRepository.save(sourceIssue);

        issueActivityService.recordActivity(
                saved,
                org.nagrivic.modules.activity.model.IssueActivityType.DUPLICATE_LINKED,
                currentUserService.getCurrentUser(),
                java.util.Map.of(
                        "duplicateIssueId", saved.getId().toString(),
                        "primaryIssueId", rootPrimary.getId().toString()
                )
        );

        notificationService.handleDuplicateLinked(saved, rootPrimary);

        return IssueResponse.fromEntity(saved);
    }
}
