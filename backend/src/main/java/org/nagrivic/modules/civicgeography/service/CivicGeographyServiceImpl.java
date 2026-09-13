package org.nagrivic.modules.civicgeography.service;

import org.nagrivic.modules.civicgeography.dto.CivicAreaResponse;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.repository.WardSpatialRepository;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.service.DepartmentResolverService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CivicGeographyServiceImpl implements CivicGeographyService {

    private static final Logger log = LoggerFactory.getLogger(CivicGeographyServiceImpl.class);

    private final WardSpatialRepository wardSpatialRepository;
    private final DepartmentResolverService departmentResolverService;

    public CivicGeographyServiceImpl(
            WardSpatialRepository wardSpatialRepository,
            DepartmentResolverService departmentResolverService
    ) {
        this.wardSpatialRepository = wardSpatialRepository;
        this.departmentResolverService = departmentResolverService;
    }

    @Override
    public Optional<WardEntity> findWardContainingPoint(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        try {
            return wardSpatialRepository.findWardContainingPoint(latitude, longitude);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error evaluating point-in-polygon for lat={}, lon={}: {}", latitude, longitude, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<CivicAreaResponse> resolvePoint(double latitude, double longitude, UUID categoryId) {
        validateCoordinates(latitude, longitude);
        Optional<WardEntity> wardOpt = findWardContainingPoint(latitude, longitude);
        if (wardOpt.isEmpty()) {
            return Optional.empty();
        }

        WardEntity ward = wardOpt.get();
        CityEntity city = ward.getCity();
        CivicBodyEntity civicBody = ward.getCivicBody();
        if (civicBody == null && city != null) {
            civicBody = city.getCivicBody();
        }

        String cityName = city != null ? city.getName() : null;

        CivicAreaResponse.WardAreaSummary wardSummary = new CivicAreaResponse.WardAreaSummary(
                ward.getWardName(),
                ward.getWardNumber()
        );

        CivicAreaResponse.CivicBodyAreaSummary civicBodySummary = civicBody != null
                ? new CivicAreaResponse.CivicBodyAreaSummary(civicBody.getName())
                : null;

        CivicAreaResponse.DepartmentAreaSummary deptSummary = null;
        if (categoryId != null && civicBody != null) {
            Optional<DepartmentEntity> deptOpt = departmentResolverService.resolveDepartment(categoryId, civicBody, ward);
            if (deptOpt.isPresent()) {
                deptSummary = new CivicAreaResponse.DepartmentAreaSummary(deptOpt.get().getName());
            }
        }

        return Optional.of(new CivicAreaResponse(cityName, wardSummary, civicBodySummary, deptSummary));
    }

    @Override
    public Optional<CivicAreaResponse> resolveLocation(LocationEntity location, UUID categoryId) {
        if (location == null) {
            return Optional.empty();
        }
        return resolvePoint(location.getLatitude(), location.getLongitude(), categoryId);
    }

    @Override
    public Optional<CivicAreaResponse> enrichIssue(IssueEntity issue) {
        if (issue == null || issue.getLocation() == null) {
            return Optional.empty();
        }

        UUID categoryId = issue.getCategory() != null ? issue.getCategory().getId() : null;
        return resolveLocation(issue.getLocation(), categoryId);
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees. Received: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees. Received: " + longitude);
        }
    }
}
