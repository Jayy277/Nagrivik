package org.nagrivic.modules.issues.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.service.CivicGeographyService;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.service.DepartmentResolverService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class IssueResponsibilityServiceImpl implements IssueResponsibilityService {

    private static final Logger log = LoggerFactory.getLogger(IssueResponsibilityServiceImpl.class);
    private static final String RESOLUTION_SOURCE = "GEOGRAPHIC_POINT_IN_POLYGON";

    private final CivicGeographyService civicGeographyService;
    private final DepartmentResolverService departmentResolverService;
    private final IssueRepository issueRepository;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;
    private final org.nagrivic.modules.notifications.service.NotificationService notificationService;

    public IssueResponsibilityServiceImpl(
            CivicGeographyService civicGeographyService,
            DepartmentResolverService departmentResolverService,
            IssueRepository issueRepository,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService,
            @org.springframework.context.annotation.Lazy org.nagrivic.modules.notifications.service.NotificationService notificationService
    ) {
        this.civicGeographyService = civicGeographyService;
        this.departmentResolverService = departmentResolverService;
        this.issueRepository = issueRepository;
        this.issueActivityService = issueActivityService;
        this.notificationService = notificationService;
    }

    @Override
    public void resolveResponsibility(IssueEntity issue) {
        if (issue == null) {
            return;
        }

        LocationEntity location = issue.getLocation();
        if (location == null) {
            log.info("Issue {} has no location; marking civic responsibility as UNRESOLVED", issue.getId());
            issue.setResponsibilityStatus(ResponsibilityStatus.UNRESOLVED);
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // 1. Resolve containing Ward via PostGIS point-in-polygon
        Optional<WardEntity> wardOpt = civicGeographyService.findWardContainingPoint(lat, lon);
        if (wardOpt.isEmpty()) {
            log.info("No authoritative ward boundary contains point (lat={}, lon={}) for issue {}; marking UNRESOLVED",
                    lat, lon, issue.getId());
            issue.setUnresolvedResponsibility(null, null, null, null);
            return;
        }

        WardEntity ward = wardOpt.get();
        CityEntity city = ward.getCity();
        CivicBodyEntity civicBody = ward.getCivicBody();
        if (civicBody == null && city != null) {
            civicBody = city.getCivicBody();
        }

        if (civicBody == null) {
            log.info("Ward {} has no associated civic body; marking responsibility as UNRESOLVED for issue {}",
                    ward.getId(), issue.getId());
            issue.setUnresolvedResponsibility(null, city, ward, null);
            return;
        }

        UUID categoryId = issue.getCategory() != null ? issue.getCategory().getId() : null;
        if (categoryId == null) {
            log.info("Issue {} has no category; marking responsibility as UNRESOLVED", issue.getId());
            issue.setUnresolvedResponsibility(civicBody, city, ward, null);
            return;
        }

        // 2. Resolve Department via precedence rules
        Optional<DepartmentEntity> deptOpt = departmentResolverService.resolveDepartment(categoryId, civicBody, ward);
        if (deptOpt.isEmpty()) {
            log.info("No unambiguous department mapping found for categoryId={}, civicBodyId={}, wardId={}; marking UNRESOLVED for issue {}",
                    categoryId, civicBody.getId(), ward.getId(), issue.getId());
            issue.setUnresolvedResponsibility(civicBody, city, ward, null);
            return;
        }

        // 3. Fully resolved: assign authoritative civic responsibility context
        DepartmentEntity department = deptOpt.get();
        issue.setResolvedResponsibility(civicBody, city, ward, department, RESOLUTION_SOURCE);
        log.info("Successfully resolved civic responsibility for issue {}: civicBody={}, city={}, ward={}, department={}",
                issue.getId(), civicBody.getName(), city != null ? city.getName() : "N/A", ward.getWardName(), department.getName());
    }

    @Override
    public void resolveIssueResponsibility(UUID issueId) {
        if (issueId == null) {
            return;
        }
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        ResponsibilityStatus prevStatus = issue.getResponsibilityStatus();
        resolveResponsibility(issue);
        IssueEntity saved = issueRepository.save(issue);

        if (saved.getResponsibilityStatus() == ResponsibilityStatus.RESOLVED) {
            recordResolutionActivityAndNotify(saved, prevStatus);
        }
    }

    @Override
    public int reResolveUnresolvedIssues() {
        java.util.List<IssueEntity> unresolved = issueRepository.findByResponsibilityStatus(ResponsibilityStatus.UNRESOLVED);
        int resolvedCount = 0;
        for (IssueEntity issue : unresolved) {
            resolveResponsibility(issue);
            IssueEntity saved = issueRepository.save(issue);
            if (saved.getResponsibilityStatus() == ResponsibilityStatus.RESOLVED) {
                resolvedCount++;
                recordResolutionActivityAndNotify(saved, ResponsibilityStatus.UNRESOLVED);
            }
        }
        log.info("Re-resolved {} previously unresolved issues into RESOLVED status (scanned {} issues)", resolvedCount, unresolved.size());
        return resolvedCount;
    }

    @Override
    public int reResolveAllIssues() {
        java.util.List<IssueEntity> allIssues = issueRepository.findAll();
        int updatedCount = 0;
        for (IssueEntity issue : allIssues) {
            ResponsibilityStatus prevStatus = issue.getResponsibilityStatus();
            resolveResponsibility(issue);
            IssueEntity saved = issueRepository.save(issue);
            if (saved.getResponsibilityStatus() == ResponsibilityStatus.RESOLVED) {
                updatedCount++;
                recordResolutionActivityAndNotify(saved, prevStatus);
            }
        }
        log.info("Re-evaluated civic responsibility for all {} issues ({} currently RESOLVED)", allIssues.size(), updatedCount);
        return updatedCount;
    }

    private void recordResolutionActivityAndNotify(IssueEntity saved, ResponsibilityStatus prevStatus) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        if (saved.getCivicBody() != null) data.put("civicBodyId", saved.getCivicBody().getId().toString());
        if (saved.getCity() != null) data.put("cityId", saved.getCity().getId().toString());
        if (saved.getWard() != null) data.put("wardId", saved.getWard().getId().toString());
        if (saved.getDepartment() != null) data.put("departmentId", saved.getDepartment().getId().toString());

        org.nagrivic.modules.activity.model.IssueActivityType eventType =
                (prevStatus == ResponsibilityStatus.RESOLVED)
                        ? org.nagrivic.modules.activity.model.IssueActivityType.RESPONSIBILITY_RE_RESOLVED
                        : org.nagrivic.modules.activity.model.IssueActivityType.RESPONSIBILITY_RESOLVED;

        issueActivityService.recordActivity(saved, eventType, null, data);

        if (prevStatus != ResponsibilityStatus.RESOLVED) {
            notificationService.handleResponsibilityResolved(saved);
        }
    }
}
