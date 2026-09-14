package org.nagrivic.modules.civicgeography.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.nagrivic.common.error.ConflictException;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.dto.admin.*;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.departments.entity.CategoryDepartmentMappingEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.entity.WardDepartmentMappingEntity;
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.departments.repository.WardDepartmentMappingRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueResponsibilityService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class CivicGeographyAdminServiceImpl implements CivicGeographyAdminService {

    private static final Logger log = LoggerFactory.getLogger(CivicGeographyAdminServiceImpl.class);
    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private final CivicBodyRepository civicBodyRepository;
    private final CityRepository cityRepository;
    private final WardRepository wardRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryDepartmentMappingRepository categoryDepartmentMappingRepository;
    private final WardDepartmentMappingRepository wardDepartmentMappingRepository;
    private final IssueRepository issueRepository;
    private final CivicGeographyAuditService auditService;
    private final IssueResponsibilityService issueResponsibilityService;

    public CivicGeographyAdminServiceImpl(
            CivicBodyRepository civicBodyRepository,
            CityRepository cityRepository,
            WardRepository wardRepository,
            DepartmentRepository departmentRepository,
            CategoryRepository categoryRepository,
            CategoryDepartmentMappingRepository categoryDepartmentMappingRepository,
            WardDepartmentMappingRepository wardDepartmentMappingRepository,
            IssueRepository issueRepository,
            CivicGeographyAuditService auditService,
            IssueResponsibilityService issueResponsibilityService
    ) {
        this.civicBodyRepository = civicBodyRepository;
        this.cityRepository = cityRepository;
        this.wardRepository = wardRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.categoryDepartmentMappingRepository = categoryDepartmentMappingRepository;
        this.wardDepartmentMappingRepository = wardDepartmentMappingRepository;
        this.issueRepository = issueRepository;
        this.auditService = auditService;
        this.issueResponsibilityService = issueResponsibilityService;
    }

    // =========================================================================
    // OVERVIEW
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public GeographyOverviewResponse getOverview() {
        List<CivicBodyEntity> civicBodies = civicBodyRepository.findAll();
        long totalCivicBodies = civicBodies.size();
        long activeCivicBodies = civicBodies.stream().filter(CivicBodyEntity::isActive).count();

        List<CityEntity> cities = cityRepository.findAll();
        long totalCities = cities.size();
        long activeCities = cities.stream().filter(CityEntity::isActive).count();

        List<WardEntity> wards = wardRepository.findAll();
        long totalWards = wards.size();
        long activeWards = wards.stream().filter(WardEntity::isActive).count();
        long wardsWithBoundaries = wards.stream().filter(w -> w.getBoundaryGeometry() != null).count();
        long wardsWithoutBoundaries = totalWards - wardsWithBoundaries;

        List<DepartmentEntity> departments = departmentRepository.findAll();
        long totalDepartments = departments.size();
        long activeDepartments = departments.stream().filter(DepartmentEntity::isActive).count();

        List<CategoryDepartmentMappingEntity> catMappings = categoryDepartmentMappingRepository.findAll();
        long totalCategoryMappings = catMappings.size();
        long activeCategoryMappings = catMappings.stream().filter(CategoryDepartmentMappingEntity::isActive).count();

        List<WardDepartmentMappingEntity> wardMappings = wardDepartmentMappingRepository.findAll();
        long totalWardMappings = wardMappings.size();
        long activeWardMappings = wardMappings.stream().filter(WardDepartmentMappingEntity::isActive).count();

        long totalIssues = issueRepository.count();
        long unresolvedIssues = issueRepository.countByResponsibilityStatus(ResponsibilityStatus.UNRESOLVED);
        long resolvedIssues = issueRepository.countByResponsibilityStatus(ResponsibilityStatus.RESOLVED);
        long totalAudits = auditService.countAudits();

        return new GeographyOverviewResponse(
                totalCivicBodies,
                activeCivicBodies,
                totalCities,
                activeCities,
                totalWards,
                activeWards,
                wardsWithBoundaries,
                wardsWithoutBoundaries,
                totalDepartments,
                activeDepartments,
                totalCategoryMappings,
                activeCategoryMappings,
                totalWardMappings,
                activeWardMappings,
                totalIssues,
                resolvedIssues,
                unresolvedIssues,
                totalAudits
        );
    }

    // =========================================================================
    // CIVIC BODIES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CivicBodyResponse> getCivicBodies(Pageable pageable) {
        return civicBodyRepository.findAllByOrderByNameAsc(pageable)
                .map(this::toCivicBodyResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CivicBodyResponse getCivicBody(UUID id) {
        CivicBodyEntity entity = civicBodyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CivicBody", id));
        return toCivicBodyResponse(entity);
    }

    @Override
    public CivicBodyResponse createCivicBody(CreateCivicBodyRequest request, UserEntity actor) {
        if (civicBodyRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("A civic body with name '" + request.name().trim() + "' already exists");
        }

        CivicBodyEntity entity = new CivicBodyEntity(
                request.name().trim(),
                request.type(),
                request.state().trim(),
                request.city().trim()
        );
        entity.setOfficialWebsite(clean(request.officialWebsite()));
        entity.setSource(clean(request.source()));
        entity.setSourceUrl(clean(request.sourceUrl()));
        if (entity.getSource() != null) {
            entity.setLastVerifiedAt(Instant.now());
        }

        CivicBodyEntity saved = civicBodyRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CIVIC_BODY",
                saved.getId(),
                "CIVIC_BODY_CREATED",
                null,
                "Name: " + saved.getName() + ", Type: " + saved.getType(),
                "Initial creation",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCivicBodyResponse(saved);
    }

    @Override
    public CivicBodyResponse updateCivicBody(UUID id, UpdateCivicBodyRequest request, UserEntity actor) {
        CivicBodyEntity entity = civicBodyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CivicBody", id));

        checkVersion(request.version(), entity.getVersion(), "CivicBody");

        String prevState = "Name: " + entity.getName() + ", Active: " + entity.isActive();

        if (request.name() != null && !request.name().trim().isEmpty()) {
            String newName = request.name().trim();
            if (!newName.equalsIgnoreCase(entity.getName()) && civicBodyRepository.existsByNameIgnoreCase(newName)) {
                throw new ConflictException("A civic body with name '" + newName + "' already exists");
            }
            entity.setName(newName);
        }
        if (request.type() != null) {
            entity.setType(request.type());
        }
        if (request.state() != null && !request.state().trim().isEmpty()) {
            entity.setState(request.state().trim());
        }
        if (request.city() != null && !request.city().trim().isEmpty()) {
            entity.setCity(request.city().trim());
        }
        if (request.officialWebsite() != null) {
            entity.setOfficialWebsite(clean(request.officialWebsite()));
        }
        if (request.source() != null) {
            entity.setSource(clean(request.source()));
            entity.setLastVerifiedAt(Instant.now());
        }
        if (request.sourceUrl() != null) {
            entity.setSourceUrl(clean(request.sourceUrl()));
        }
        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }

        CivicBodyEntity saved = civicBodyRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CIVIC_BODY",
                saved.getId(),
                "CIVIC_BODY_UPDATED",
                prevState,
                "Name: " + saved.getName() + ", Active: " + saved.isActive(),
                "Metadata update",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCivicBodyResponse(saved);
    }

    @Override
    public CivicBodyResponse toggleCivicBodyActive(UUID id, boolean active, String reason, Long version, UserEntity actor) {
        CivicBodyEntity entity = civicBodyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CivicBody", id));

        checkVersion(version, entity.getVersion(), "CivicBody");

        String prev = "Active: " + entity.isActive();
        entity.setActive(active);
        CivicBodyEntity saved = civicBodyRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CIVIC_BODY",
                saved.getId(),
                active ? "CIVIC_BODY_ACTIVATED" : "CIVIC_BODY_DEACTIVATED",
                prev,
                "Active: " + active,
                reason != null ? reason : (active ? "Re-activated" : "Deactivated"),
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCivicBodyResponse(saved);
    }

    @Override
    public void deleteCivicBody(UUID id, Long version, UserEntity actor) {
        CivicBodyEntity entity = civicBodyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CivicBody", id));

        checkVersion(version, entity.getVersion(), "CivicBody");

        long issueCount = issueRepository.countByCivicBody_Id(id);
        if (issueCount > 0) {
            throw new ConflictException("Cannot delete civic body: it is referenced by " + issueCount + " historical issues. Please deactivate it instead.");
        }
        long cityCount = cityRepository.countByCivicBodyId(id);
        if (cityCount > 0) {
            throw new ConflictException("Cannot delete civic body: it is referenced by " + cityCount + " cities. Deactivate instead.");
        }
        long wardCount = wardRepository.countByCivicBodyId(id);
        if (wardCount > 0) {
            throw new ConflictException("Cannot delete civic body: it is referenced by " + wardCount + " wards. Deactivate instead.");
        }
        long deptCount = departmentRepository.countByCivicBodyId(id);
        if (deptCount > 0) {
            throw new ConflictException("Cannot delete civic body: it is referenced by " + deptCount + " departments. Deactivate instead.");
        }

        auditService.recordAudit(
                actor,
                "CIVIC_BODY",
                id,
                "CIVIC_BODY_DELETED",
                "Name: " + entity.getName(),
                null,
                "Administrative deletion",
                entity.getSource(),
                entity.getSourceUrl()
        );

        civicBodyRepository.delete(entity);
    }

    // =========================================================================
    // CITIES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CityResponse> getCities(UUID civicBodyId, Pageable pageable) {
        if (civicBodyId != null) {
            List<CityEntity> list = cityRepository.findByCivicBodyId(civicBodyId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            List<CityResponse> sub = (start > list.size()) ? List.of() : list.subList(start, end).stream().map(this::toCityResponse).toList();
            return new org.springframework.data.domain.PageImpl<>(sub, pageable, list.size());
        }
        return cityRepository.findAllByOrderByNameAsc(pageable)
                .map(this::toCityResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CityResponse getCity(UUID id) {
        CityEntity entity = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));
        return toCityResponse(entity);
    }

    @Override
    public CityResponse createCity(CreateCityRequest request, UserEntity actor) {
        if (cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(request.name().trim(), request.state().trim())) {
            throw new ConflictException("City '" + request.name().trim() + "' in state '" + request.state().trim() + "' already exists");
        }

        CivicBodyEntity civicBody = null;
        if (request.civicBodyId() != null) {
            civicBody = civicBodyRepository.findById(request.civicBodyId())
                    .orElseThrow(() -> new ResourceNotFoundException("CivicBody", request.civicBodyId()));
        }

        CityEntity entity = new CityEntity(request.name().trim(), request.state().trim(), request.countryCode(), civicBody);
        entity.setSource(clean(request.source()));
        entity.setSourceUrl(clean(request.sourceUrl()));
        if (entity.getSource() != null) {
            entity.setLastVerifiedAt(Instant.now());
        }

        CityEntity saved = cityRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CITY",
                saved.getId(),
                "CITY_CREATED",
                null,
                "Name: " + saved.getName() + ", State: " + saved.getState(),
                "Initial creation",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCityResponse(saved);
    }

    @Override
    public CityResponse updateCity(UUID id, UpdateCityRequest request, UserEntity actor) {
        CityEntity entity = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));

        checkVersion(request.version(), entity.getVersion(), "City");

        String prev = "Name: " + entity.getName() + ", Active: " + entity.isActive();

        if (request.name() != null && !request.name().trim().isEmpty()) {
            String newName = request.name().trim();
            String state = request.state() != null ? request.state().trim() : entity.getState();
            if (!newName.equalsIgnoreCase(entity.getName()) && cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(newName, state)) {
                throw new ConflictException("City '" + newName + "' in state '" + state + "' already exists");
            }
            entity.setName(newName);
        }
        if (request.state() != null && !request.state().trim().isEmpty()) {
            entity.setState(request.state().trim());
        }
        if (request.countryCode() != null) {
            entity.setCountryCode(request.countryCode().trim().toUpperCase());
        }
        if (request.civicBodyId() != null) {
            CivicBodyEntity cb = civicBodyRepository.findById(request.civicBodyId())
                    .orElseThrow(() -> new ResourceNotFoundException("CivicBody", request.civicBodyId()));
            entity.setCivicBody(cb);
        }
        if (request.source() != null) {
            entity.setSource(clean(request.source()));
            entity.setLastVerifiedAt(Instant.now());
        }
        if (request.sourceUrl() != null) {
            entity.setSourceUrl(clean(request.sourceUrl()));
        }
        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }

        CityEntity saved = cityRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CITY",
                saved.getId(),
                "CITY_UPDATED",
                prev,
                "Name: " + saved.getName() + ", Active: " + saved.isActive(),
                "Metadata update",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCityResponse(saved);
    }

    @Override
    public CityResponse toggleCityActive(UUID id, boolean active, String reason, Long version, UserEntity actor) {
        CityEntity entity = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));

        checkVersion(version, entity.getVersion(), "City");

        String prev = "Active: " + entity.isActive();
        entity.setActive(active);
        CityEntity saved = cityRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CITY",
                saved.getId(),
                active ? "CITY_ACTIVATED" : "CITY_DEACTIVATED",
                prev,
                "Active: " + active,
                reason != null ? reason : (active ? "Re-activated" : "Deactivated"),
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCityResponse(saved);
    }

    @Override
    public void deleteCity(UUID id, Long version, UserEntity actor) {
        CityEntity entity = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", id));

        checkVersion(version, entity.getVersion(), "City");

        long issueCount = issueRepository.countByCity_Id(id);
        if (issueCount > 0) {
            throw new ConflictException("Cannot delete city: it is referenced by " + issueCount + " historical issues. Please deactivate it instead.");
        }
        long wardCount = wardRepository.countByCityId(id);
        if (wardCount > 0) {
            throw new ConflictException("Cannot delete city: it is referenced by " + wardCount + " wards. Please deactivate instead.");
        }

        auditService.recordAudit(
                actor,
                "CITY",
                id,
                "CITY_DELETED",
                "Name: " + entity.getName(),
                null,
                "Administrative deletion",
                entity.getSource(),
                entity.getSourceUrl()
        );

        cityRepository.delete(entity);
    }

    // =========================================================================
    // WARDS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<WardResponse> getWards(UUID cityId, Boolean isActive, String search, Pageable pageable) {
        String cleanSearch = clean(search);
        return wardRepository.searchWards(cityId, isActive, cleanSearch, pageable)
                .map(this::toWardResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WardResponse getWard(UUID id) {
        WardEntity entity = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward", id));
        return toWardResponse(entity);
    }

    @Override
    public WardResponse createWard(CreateWardRequest request, UserEntity actor) {
        CityEntity city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", request.cityId()));

        if (request.wardNumber() != null && !request.wardNumber().trim().isEmpty()) {
            String num = request.wardNumber().trim();
            if (wardRepository.existsByCityIdAndWardNumber(city.getId(), num)) {
                throw new ConflictException("Ward number '" + num + "' already exists in city '" + city.getName() + "'");
            }
        }
        if (request.wardCode() != null && !request.wardCode().trim().isEmpty()) {
            String code = request.wardCode().trim();
            if (wardRepository.existsByCityIdAndWardCode(city.getId(), code)) {
                throw new ConflictException("Ward code '" + code + "' already exists in city '" + city.getName() + "'");
            }
        }

        CivicBodyEntity civicBody = null;
        if (request.civicBodyId() != null) {
            civicBody = civicBodyRepository.findById(request.civicBodyId())
                    .orElseThrow(() -> new ResourceNotFoundException("CivicBody", request.civicBodyId()));
        } else if (city.getCivicBody() != null) {
            civicBody = city.getCivicBody();
        }

        MultiPolygon boundary = parseAndValidateBoundary(request.boundaryGeometryWkt());

        WardEntity entity = new WardEntity(city, civicBody, clean(request.wardNumber()), request.wardName().trim(), boundary);
        entity.setWardCode(clean(request.wardCode()));
        entity.setSource(clean(request.source()));
        entity.setSourceUrl(clean(request.sourceUrl()));
        if (entity.getSource() != null) {
            entity.setLastVerifiedAt(Instant.now());
        }

        WardEntity saved = wardRepository.save(entity);

        auditService.recordAudit(
                actor,
                "WARD",
                saved.getId(),
                "WARD_CREATED",
                null,
                "Ward: " + saved.getWardName() + " (" + saved.getWardNumber() + "), City: " + city.getName(),
                "Initial creation",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toWardResponse(saved);
    }

    @Override
    public WardResponse updateWard(UUID id, UpdateWardRequest request, UserEntity actor) {
        WardEntity entity = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward", id));

        checkVersion(request.version(), entity.getVersion(), "Ward");

        String prev = "Name: " + entity.getWardName() + ", Number: " + entity.getWardNumber() + ", HasBoundary: " + (entity.getBoundaryGeometry() != null) + ", Active: " + entity.isActive();

        if (request.cityId() != null) {
            CityEntity city = cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new ResourceNotFoundException("City", request.cityId()));
            entity.setCity(city);
        }
        if (request.civicBodyId() != null) {
            CivicBodyEntity cb = civicBodyRepository.findById(request.civicBodyId())
                    .orElseThrow(() -> new ResourceNotFoundException("CivicBody", request.civicBodyId()));
            entity.setCivicBody(cb);
        }
        if (request.wardName() != null && !request.wardName().trim().isEmpty()) {
            entity.setWardName(request.wardName().trim());
        }
        if (request.wardNumber() != null) {
            String num = clean(request.wardNumber());
            if (num != null && !num.equals(entity.getWardNumber()) && wardRepository.existsByCityIdAndWardNumber(entity.getCity().getId(), num)) {
                throw new ConflictException("Ward number '" + num + "' already exists in city '" + entity.getCity().getName() + "'");
            }
            entity.setWardNumber(num);
        }
        if (request.wardCode() != null) {
            String code = clean(request.wardCode());
            if (code != null && !code.equals(entity.getWardCode()) && wardRepository.existsByCityIdAndWardCode(entity.getCity().getId(), code)) {
                throw new ConflictException("Ward code '" + code + "' already exists in city '" + entity.getCity().getName() + "'");
            }
            entity.setWardCode(code);
        }
        boolean boundaryUpdated = false;
        if (request.boundaryGeometryWkt() != null) {
            MultiPolygon boundary = parseAndValidateBoundary(request.boundaryGeometryWkt());
            entity.setBoundaryGeometry(boundary);
            boundaryUpdated = true;
        }
        if (request.source() != null) {
            entity.setSource(clean(request.source()));
            entity.setLastVerifiedAt(Instant.now());
        }
        if (request.sourceUrl() != null) {
            entity.setSourceUrl(clean(request.sourceUrl()));
        }
        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }

        WardEntity saved = wardRepository.save(entity);

        auditService.recordAudit(
                actor,
                "WARD",
                saved.getId(),
                boundaryUpdated ? "WARD_BOUNDARY_UPDATED" : "WARD_UPDATED",
                prev,
                "Name: " + saved.getWardName() + ", Number: " + saved.getWardNumber() + ", HasBoundary: " + (saved.getBoundaryGeometry() != null) + ", Active: " + saved.isActive(),
                boundaryUpdated ? "Authoritative boundary geometry updated" : "Ward metadata updated",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toWardResponse(saved);
    }

    @Override
    public WardResponse toggleWardActive(UUID id, boolean active, String reason, Long version, UserEntity actor) {
        WardEntity entity = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward", id));

        checkVersion(version, entity.getVersion(), "Ward");

        String prev = "Active: " + entity.isActive();
        entity.setActive(active);
        WardEntity saved = wardRepository.save(entity);

        auditService.recordAudit(
                actor,
                "WARD",
                saved.getId(),
                active ? "WARD_ACTIVATED" : "WARD_DEACTIVATED",
                prev,
                "Active: " + active,
                reason != null ? reason : (active ? "Re-activated" : "Deactivated"),
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toWardResponse(saved);
    }

    @Override
    public void deleteWard(UUID id, Long version, UserEntity actor) {
        WardEntity entity = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward", id));

        checkVersion(version, entity.getVersion(), "Ward");

        long issueCount = issueRepository.countByWard_Id(id);
        if (issueCount > 0) {
            throw new ConflictException("Cannot delete ward: it is referenced by " + issueCount + " historical issues. Please deactivate it instead.");
        }
        long mappingCount = wardDepartmentMappingRepository.countByWardId(id);
        if (mappingCount > 0) {
            throw new ConflictException("Cannot delete ward: it has " + mappingCount + " department mappings. Please deactivate instead.");
        }

        auditService.recordAudit(
                actor,
                "WARD",
                id,
                "WARD_DELETED",
                "Ward: " + entity.getWardName() + " (" + entity.getWardNumber() + ")",
                null,
                "Administrative deletion",
                entity.getSource(),
                entity.getSourceUrl()
        );

        wardRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public WardBoundaryValidationResponse validateWardBoundaries() {
        List<WardEntity> wards = wardRepository.findAll();
        int totalWardsChecked = wards.size();
        int validBoundaries = 0;
        int invalidBoundaries = 0;
        int missingBoundaries = 0;
        int duplicateGeometries = 0;
        List<WardBoundaryValidationResponse.WardBoundaryIssue> issues = new ArrayList<>();
        Map<String, UUID> seenGeometries = new HashMap<>();

        for (WardEntity ward : wards) {
            MultiPolygon geom = ward.getBoundaryGeometry();
            if (geom == null) {
                missingBoundaries++;
                issues.add(new WardBoundaryValidationResponse.WardBoundaryIssue(
                        ward.getId(),
                        ward.getWardNumber(),
                        ward.getWardName(),
                        "MISSING_BOUNDARY",
                        "Ward has no boundary geometry assigned (authoritative geometry awaiting official AMC import)"
                ));
                continue;
            }

            // 1. SRID check
            if (geom.getSRID() != SRID_WGS84 && geom.getSRID() != 0) {
                invalidBoundaries++;
                issues.add(new WardBoundaryValidationResponse.WardBoundaryIssue(
                        ward.getId(),
                        ward.getWardNumber(),
                        ward.getWardName(),
                        "INVALID_SRID",
                        "Geometry SRID is " + geom.getSRID() + " (expected 4326)"
                ));
                continue;
            }

            // 2. Validity check
            if (!geom.isValid()) {
                invalidBoundaries++;
                issues.add(new WardBoundaryValidationResponse.WardBoundaryIssue(
                        ward.getId(),
                        ward.getWardNumber(),
                        ward.getWardName(),
                        "TOPOLOGY_INVALID",
                        "ST_IsValid is false: topology corruption or self-intersection"
                ));
                continue;
            }

            // 3. Coordinate range check (lon -180..180, lat -90..90)
            boolean outOfBounds = false;
            for (Coordinate c : geom.getCoordinates()) {
                if (c.x < -180.0 || c.x > 180.0 || c.y < -90.0 || c.y > 90.0) {
                    outOfBounds = true;
                    break;
                }
            }
            if (outOfBounds) {
                invalidBoundaries++;
                issues.add(new WardBoundaryValidationResponse.WardBoundaryIssue(
                        ward.getId(),
                        ward.getWardNumber(),
                        ward.getWardName(),
                        "COORDINATE_OUT_OF_RANGE",
                        "One or more coordinates fall outside valid WGS-84 ranges [-180,180] / [-90,90]"
                ));
                continue;
            }

            // 4. Duplicate geometry check
            String wkt = geom.toText();
            if (seenGeometries.containsKey(wkt)) {
                duplicateGeometries++;
                issues.add(new WardBoundaryValidationResponse.WardBoundaryIssue(
                        ward.getId(),
                        ward.getWardNumber(),
                        ward.getWardName(),
                        "DUPLICATE_GEOMETRY",
                        "Identical boundary geometry to ward ID: " + seenGeometries.get(wkt)
                ));
            } else {
                seenGeometries.put(wkt, ward.getId());
            }

            validBoundaries++;
        }

        return new WardBoundaryValidationResponse(
                totalWardsChecked,
                validBoundaries,
                invalidBoundaries,
                missingBoundaries,
                duplicateGeometries,
                issues
        );
    }

    // =========================================================================
    // DEPARTMENTS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getDepartments(UUID civicBodyId, Boolean isActive, String search, Pageable pageable) {
        String cleanSearch = clean(search);
        return departmentRepository.searchDepartments(civicBodyId, isActive, cleanSearch, pageable)
                .map(this::toDepartmentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(UUID id) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        return toDepartmentResponse(entity);
    }

    @Override
    public DepartmentResponse createDepartment(CreateDepartmentRequest request, UserEntity actor) {
        CivicBodyEntity civicBody = civicBodyRepository.findById(request.civicBodyId())
                .orElseThrow(() -> new ResourceNotFoundException("CivicBody", request.civicBodyId()));

        String name = request.name().trim();
        if (departmentRepository.existsByCivicBodyIdAndNameIgnoreCase(civicBody.getId(), name)) {
            throw new ConflictException("Department '" + name + "' already exists in civic body '" + civicBody.getName() + "'");
        }
        if (request.code() != null && !request.code().trim().isEmpty()) {
            String code = request.code().trim();
            if (departmentRepository.existsByCivicBodyIdAndCodeIgnoreCase(civicBody.getId(), code)) {
                throw new ConflictException("Department code '" + code + "' already exists in civic body '" + civicBody.getName() + "'");
            }
        }

        DepartmentEntity entity = new DepartmentEntity(civicBody, name, clean(request.code()), clean(request.description()));
        entity.setSource(clean(request.source()));
        entity.setSourceUrl(clean(request.sourceUrl()));
        if (entity.getSource() != null) {
            entity.setLastVerifiedAt(Instant.now());
        }

        DepartmentEntity saved = departmentRepository.save(entity);

        auditService.recordAudit(
                actor,
                "DEPARTMENT",
                saved.getId(),
                "DEPARTMENT_CREATED",
                null,
                "Name: " + saved.getName() + " (" + saved.getCode() + "), CivicBody: " + civicBody.getName(),
                "Initial creation",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toDepartmentResponse(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(UUID id, UpdateDepartmentRequest request, UserEntity actor) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));

        checkVersion(request.version(), entity.getVersion(), "Department");

        String prev = "Name: " + entity.getName() + ", Code: " + entity.getCode() + ", Active: " + entity.isActive();

        if (request.name() != null && !request.name().trim().isEmpty()) {
            String newName = request.name().trim();
            if (!newName.equalsIgnoreCase(entity.getName()) && departmentRepository.existsByCivicBodyIdAndNameIgnoreCase(entity.getCivicBody().getId(), newName)) {
                throw new ConflictException("Department '" + newName + "' already exists in civic body '" + entity.getCivicBody().getName() + "'");
            }
            entity.setName(newName);
        }
        if (request.code() != null) {
            String newCode = clean(request.code());
            if (newCode != null && !newCode.equalsIgnoreCase(entity.getCode()) && departmentRepository.existsByCivicBodyIdAndCodeIgnoreCase(entity.getCivicBody().getId(), newCode)) {
                throw new ConflictException("Department code '" + newCode + "' already exists in civic body '" + entity.getCivicBody().getName() + "'");
            }
            entity.setCode(newCode);
        }
        if (request.description() != null) {
            entity.setDescription(clean(request.description()));
        }
        if (request.source() != null) {
            entity.setSource(clean(request.source()));
            entity.setLastVerifiedAt(Instant.now());
        }
        if (request.sourceUrl() != null) {
            entity.setSourceUrl(clean(request.sourceUrl()));
        }
        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }

        DepartmentEntity saved = departmentRepository.save(entity);

        auditService.recordAudit(
                actor,
                "DEPARTMENT",
                saved.getId(),
                "DEPARTMENT_UPDATED",
                prev,
                "Name: " + saved.getName() + ", Code: " + saved.getCode() + ", Active: " + saved.isActive(),
                "Metadata update",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toDepartmentResponse(saved);
    }

    @Override
    public DepartmentResponse toggleDepartmentActive(UUID id, boolean active, String reason, Long version, UserEntity actor) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));

        checkVersion(version, entity.getVersion(), "Department");

        String prev = "Active: " + entity.isActive();
        entity.setActive(active);
        DepartmentEntity saved = departmentRepository.save(entity);

        auditService.recordAudit(
                actor,
                "DEPARTMENT",
                saved.getId(),
                active ? "DEPARTMENT_ACTIVATED" : "DEPARTMENT_DEACTIVATED",
                prev,
                "Active: " + active,
                reason != null ? reason : (active ? "Re-activated" : "Deactivated"),
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toDepartmentResponse(saved);
    }

    @Override
    public void deleteDepartment(UUID id, Long version, UserEntity actor) {
        DepartmentEntity entity = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));

        checkVersion(version, entity.getVersion(), "Department");

        long issueCount = issueRepository.countByDepartment_Id(id);
        if (issueCount > 0) {
            throw new ConflictException("Cannot delete department: it is referenced by " + issueCount + " historical issues. Please deactivate it instead.");
        }
        long catCount = categoryDepartmentMappingRepository.countByDepartmentId(id);
        if (catCount > 0) {
            throw new ConflictException("Cannot delete department: it has " + catCount + " category mappings. Please deactivate instead.");
        }
        long wardCount = wardDepartmentMappingRepository.countByDepartmentId(id);
        if (wardCount > 0) {
            throw new ConflictException("Cannot delete department: it has " + wardCount + " ward mappings. Please deactivate instead.");
        }

        auditService.recordAudit(
                actor,
                "DEPARTMENT",
                id,
                "DEPARTMENT_DELETED",
                "Name: " + entity.getName(),
                null,
                "Administrative deletion",
                entity.getSource(),
                entity.getSourceUrl()
        );

        departmentRepository.delete(entity);
    }

    // =========================================================================
    // CATEGORY -> DEPARTMENT MAPPINGS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDepartmentMappingResponse> getCategoryMappings(UUID categoryId, UUID departmentId, Boolean isActive, Pageable pageable) {
        return categoryDepartmentMappingRepository.searchMappings(categoryId, departmentId, isActive, pageable)
                .map(this::toCategoryMappingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDepartmentMappingResponse getCategoryMapping(UUID id) {
        CategoryDepartmentMappingEntity entity = categoryDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryDepartmentMapping", id));
        return toCategoryMappingResponse(entity);
    }

    @Override
    public CategoryDepartmentMappingResponse createCategoryMapping(CreateCategoryDepartmentMappingRequest request, UserEntity actor) {
        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        DepartmentEntity department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.departmentId()));

        Optional<CategoryDepartmentMappingEntity> existing = categoryDepartmentMappingRepository
                .findByCategoryIdAndDepartmentId(category.getId(), department.getId());
        if (existing.isPresent()) {
            throw new ConflictException("A mapping between category '" + category.getName() + "' and department '" + department.getName() + "' already exists");
        }

        CategoryDepartmentMappingEntity entity = new CategoryDepartmentMappingEntity(category, department, true);
        entity.setSource(clean(request.source()));
        entity.setSourceUrl(clean(request.sourceUrl()));
        if (entity.getSource() != null) {
            entity.setLastVerifiedAt(Instant.now());
        }

        CategoryDepartmentMappingEntity saved = categoryDepartmentMappingRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CATEGORY_DEPARTMENT_MAPPING",
                saved.getId(),
                "CATEGORY_DEPARTMENT_MAPPING_CREATED",
                null,
                "Category: " + category.getName() + " -> Dept: " + department.getName(),
                "New authoritative category mapping created",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCategoryMappingResponse(saved);
    }

    @Override
    public CategoryDepartmentMappingResponse updateCategoryMapping(UUID id, UpdateCategoryDepartmentMappingRequest request, UserEntity actor) {
        CategoryDepartmentMappingEntity entity = categoryDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryDepartmentMapping", id));

        checkVersion(request.version(), entity.getVersion(), "CategoryDepartmentMapping");

        String prev = "Active: " + entity.isActive();

        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }
        if (request.source() != null) {
            entity.setSource(clean(request.source()));
            entity.setLastVerifiedAt(Instant.now());
        }
        if (request.sourceUrl() != null) {
            entity.setSourceUrl(clean(request.sourceUrl()));
        }

        CategoryDepartmentMappingEntity saved = categoryDepartmentMappingRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CATEGORY_DEPARTMENT_MAPPING",
                saved.getId(),
                "CATEGORY_DEPARTMENT_MAPPING_UPDATED",
                prev,
                "Active: " + saved.isActive(),
                "Metadata / status update",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCategoryMappingResponse(saved);
    }

    @Override
    public CategoryDepartmentMappingResponse toggleCategoryMappingActive(UUID id, boolean active, String reason, Long version, UserEntity actor) {
        CategoryDepartmentMappingEntity entity = categoryDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryDepartmentMapping", id));

        checkVersion(version, entity.getVersion(), "CategoryDepartmentMapping");

        String prev = "Active: " + entity.isActive();
        entity.setActive(active);
        CategoryDepartmentMappingEntity saved = categoryDepartmentMappingRepository.save(entity);

        auditService.recordAudit(
                actor,
                "CATEGORY_DEPARTMENT_MAPPING",
                saved.getId(),
                active ? "CATEGORY_DEPARTMENT_MAPPING_ACTIVATED" : "CATEGORY_DEPARTMENT_MAPPING_DEACTIVATED",
                prev,
                "Active: " + active,
                reason != null ? reason : (active ? "Re-activated" : "Deactivated"),
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toCategoryMappingResponse(saved);
    }

    @Override
    public void deleteCategoryMapping(UUID id, Long version, UserEntity actor) {
        CategoryDepartmentMappingEntity entity = categoryDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryDepartmentMapping", id));

        checkVersion(version, entity.getVersion(), "CategoryDepartmentMapping");

        auditService.recordAudit(
                actor,
                "CATEGORY_DEPARTMENT_MAPPING",
                id,
                "CATEGORY_DEPARTMENT_MAPPING_DELETED",
                "Category: " + entity.getCategory().getName() + " -> Dept: " + entity.getDepartment().getName(),
                null,
                "Mapping deleted",
                entity.getSource(),
                entity.getSourceUrl()
        );

        categoryDepartmentMappingRepository.delete(entity);
    }

    // =========================================================================
    // WARD -> DEPARTMENT MAPPINGS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<WardDepartmentMappingResponse> getWardMappings(UUID wardId, UUID departmentId, Boolean isActive, Pageable pageable) {
        return wardDepartmentMappingRepository.searchMappings(wardId, departmentId, isActive, pageable)
                .map(this::toWardMappingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WardDepartmentMappingResponse getWardMapping(UUID id) {
        WardDepartmentMappingEntity entity = wardDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WardDepartmentMapping", id));
        return toWardMappingResponse(entity);
    }

    @Override
    public WardDepartmentMappingResponse createWardMapping(CreateWardDepartmentMappingRequest request, UserEntity actor) {
        WardEntity ward = wardRepository.findById(request.wardId())
                .orElseThrow(() -> new ResourceNotFoundException("Ward", request.wardId()));

        DepartmentEntity department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.departmentId()));

        Optional<WardDepartmentMappingEntity> existing = wardDepartmentMappingRepository
                .findByWardIdAndDepartmentId(ward.getId(), department.getId());
        if (existing.isPresent()) {
            throw new ConflictException("A mapping between ward '" + ward.getWardName() + "' and department '" + department.getName() + "' already exists");
        }

        WardDepartmentMappingEntity entity = new WardDepartmentMappingEntity(ward, department, true);
        entity.setSource(clean(request.source()));
        entity.setSourceUrl(clean(request.sourceUrl()));
        if (entity.getSource() != null) {
            entity.setLastVerifiedAt(Instant.now());
        }

        WardDepartmentMappingEntity saved = wardDepartmentMappingRepository.save(entity);

        auditService.recordAudit(
                actor,
                "WARD_DEPARTMENT_MAPPING",
                saved.getId(),
                "WARD_DEPARTMENT_MAPPING_CREATED",
                null,
                "Ward: " + ward.getWardName() + " (" + ward.getWardNumber() + ") -> Dept: " + department.getName(),
                "New ward-specific department mapping created",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toWardMappingResponse(saved);
    }

    @Override
    public WardDepartmentMappingResponse updateWardMapping(UUID id, UpdateWardDepartmentMappingRequest request, UserEntity actor) {
        WardDepartmentMappingEntity entity = wardDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WardDepartmentMapping", id));

        checkVersion(request.version(), entity.getVersion(), "WardDepartmentMapping");

        String prev = "Active: " + entity.isActive();

        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }
        if (request.source() != null) {
            entity.setSource(clean(request.source()));
            entity.setLastVerifiedAt(Instant.now());
        }
        if (request.sourceUrl() != null) {
            entity.setSourceUrl(clean(request.sourceUrl()));
        }

        WardDepartmentMappingEntity saved = wardDepartmentMappingRepository.save(entity);

        auditService.recordAudit(
                actor,
                "WARD_DEPARTMENT_MAPPING",
                saved.getId(),
                "WARD_DEPARTMENT_MAPPING_UPDATED",
                prev,
                "Active: " + saved.isActive(),
                "Metadata / status update",
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toWardMappingResponse(saved);
    }

    @Override
    public WardDepartmentMappingResponse toggleWardMappingActive(UUID id, boolean active, String reason, Long version, UserEntity actor) {
        WardDepartmentMappingEntity entity = wardDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WardDepartmentMapping", id));

        checkVersion(version, entity.getVersion(), "WardDepartmentMapping");

        String prev = "Active: " + entity.isActive();
        entity.setActive(active);
        WardDepartmentMappingEntity saved = wardDepartmentMappingRepository.save(entity);

        auditService.recordAudit(
                actor,
                "WARD_DEPARTMENT_MAPPING",
                saved.getId(),
                active ? "WARD_DEPARTMENT_MAPPING_ACTIVATED" : "WARD_DEPARTMENT_MAPPING_DEACTIVATED",
                prev,
                "Active: " + active,
                reason != null ? reason : (active ? "Re-activated" : "Deactivated"),
                saved.getSource(),
                saved.getSourceUrl()
        );

        return toWardMappingResponse(saved);
    }

    @Override
    public void deleteWardMapping(UUID id, Long version, UserEntity actor) {
        WardDepartmentMappingEntity entity = wardDepartmentMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WardDepartmentMapping", id));

        checkVersion(version, entity.getVersion(), "WardDepartmentMapping");

        auditService.recordAudit(
                actor,
                "WARD_DEPARTMENT_MAPPING",
                id,
                "WARD_DEPARTMENT_MAPPING_DELETED",
                "Ward: " + entity.getWard().getWardName() + " -> Dept: " + entity.getDepartment().getName(),
                null,
                "Mapping deleted",
                entity.getSource(),
                entity.getSourceUrl()
        );

        wardDepartmentMappingRepository.delete(entity);
    }

    // =========================================================================
    // BOUNDED RE-RESOLUTION
    // =========================================================================

    @Override
    public ReResolveResponse reResolveResponsibility(ReResolveRequest request, UserEntity actor) {
        int limit = (request.limit() != null && request.limit() > 0) ? Math.min(request.limit(), 100) : 50;
        boolean onlyUnresolved = request.onlyUnresolved() == null || request.onlyUnresolved();

        List<String> messages = new ArrayList<>();
        int totalProcessed = 0;
        int resolvedCount = 0;
        int unresolvedCount = 0;
        int failureCount = 0;

        if (request.issueId() != null) {
            // Single issue re-resolution
            UUID issueId = request.issueId();
            Optional<IssueEntity> issueOpt = issueRepository.findById(issueId);
            if (issueOpt.isEmpty()) {
                throw new ResourceNotFoundException("Issue", issueId);
            }
            IssueEntity issue = issueOpt.get();
            try {
                totalProcessed = 1;
                issueResponsibilityService.resolveIssueResponsibility(issueId);
                // Refresh to check new status
                IssueEntity refreshed = issueRepository.findById(issueId).orElse(issue);
                if (refreshed.getResponsibilityStatus() == ResponsibilityStatus.RESOLVED) {
                    resolvedCount = 1;
                    messages.add("Issue " + issueId + " successfully resolved to " +
                            (refreshed.getCivicBody() != null ? refreshed.getCivicBody().getName() : "N/A") + ", " +
                            (refreshed.getDepartment() != null ? refreshed.getDepartment().getName() : "N/A"));
                } else {
                    unresolvedCount = 1;
                    messages.add("Issue " + issueId + " remains UNRESOLVED (missing boundary or mapping)");
                }
            } catch (Exception e) {
                failureCount = 1;
                messages.add("Failed to re-resolve issue " + issueId + ": " + e.getMessage());
            }

            auditService.recordAudit(
                    actor,
                    "ISSUE",
                    issueId,
                    "RESPONSIBILITY_RE_RESOLVED",
                    "Single issue re-resolution",
                    "Resolved: " + resolvedCount + ", Unresolved: " + unresolvedCount,
                    "Single issue administrative re-resolution",
                    null,
                    null
            );

            return new ReResolveResponse(totalProcessed, resolvedCount, unresolvedCount, failureCount, messages);
        }

        // Bounded batch re-resolution
        Page<IssueEntity> candidateIssues;
        if (onlyUnresolved) {
            candidateIssues = issueRepository.findByResponsibilityStatus(ResponsibilityStatus.UNRESOLVED, PageRequest.of(0, limit));
        } else {
            candidateIssues = issueRepository.findAll(PageRequest.of(0, limit));
        }

        for (IssueEntity issue : candidateIssues.getContent()) {
            totalProcessed++;
            try {
                issueResponsibilityService.resolveResponsibility(issue);
                IssueEntity saved = issueRepository.save(issue);
                if (saved.getResponsibilityStatus() == ResponsibilityStatus.RESOLVED) {
                    resolvedCount++;
                } else {
                    unresolvedCount++;
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to re-resolve issue {}: {}", issue.getId(), e.getMessage());
            }
        }

        messages.add("Processed " + totalProcessed + " issues: " + resolvedCount + " resolved, " + unresolvedCount + " unresolved, " + failureCount + " failures");

        auditService.recordAudit(
                actor,
                "BATCH_RE_RESOLUTION",
                UUID.randomUUID(),
                "RESPONSIBILITY_RE_RESOLVED",
                "Batch scan: onlyUnresolved=" + onlyUnresolved + ", limit=" + limit,
                "Processed: " + totalProcessed + ", Resolved: " + resolvedCount + ", Unresolved: " + unresolvedCount,
                "Administrative batch re-resolution",
                null,
                null
        );

        return new ReResolveResponse(totalProcessed, resolvedCount, unresolvedCount, failureCount, messages);
    }

    // =========================================================================
    // HELPER METHODS & DTO MAPPING
    // =========================================================================

    private void checkVersion(Long providedVersion, Long currentVersion, String entityType) {
        if (providedVersion != null && !providedVersion.equals(currentVersion)) {
            throw new ConflictException("Stale version for " + entityType + ". Expected: " + currentVersion + ", provided: " + providedVersion);
        }
    }

    private MultiPolygon parseAndValidateBoundary(String wkt) {
        if (wkt == null || wkt.trim().isEmpty()) {
            return null;
        }

        try {
            WKTReader reader = new WKTReader(GEOMETRY_FACTORY);
            Geometry geom = reader.read(wkt.trim());
            geom.setSRID(SRID_WGS84);

            if (!geom.isValid()) {
                throw new IllegalArgumentException("Boundary geometry is not valid (ST_IsValid = false)");
            }

            if (geom instanceof MultiPolygon mp) {
                return mp;
            } else if (geom instanceof Polygon p) {
                return GEOMETRY_FACTORY.createMultiPolygon(new Polygon[]{p});
            } else {
                throw new IllegalArgumentException("Expected MultiPolygon geometry but found: " + geom.getGeometryType());
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid WKT geometry: " + e.getMessage());
        }
    }

    private CivicBodyResponse toCivicBodyResponse(CivicBodyEntity entity) {
        long cities = cityRepository.countByCivicBodyId(entity.getId());
        long wards = wardRepository.countByCivicBodyId(entity.getId());
        long depts = departmentRepository.countByCivicBodyId(entity.getId());
        long issues = issueRepository.countByCivicBody_Id(entity.getId());

        return new CivicBodyResponse(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getState(),
                entity.getCity(),
                entity.getOfficialWebsite(),
                entity.getSource(),
                entity.getSourceUrl(),
                entity.getLastVerifiedAt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                cities,
                wards,
                depts,
                issues
        );
    }

    private CityResponse toCityResponse(CityEntity entity) {
        long wards = wardRepository.countByCityId(entity.getId());
        long issues = issueRepository.countByCity_Id(entity.getId());

        return new CityResponse(
                entity.getId(),
                entity.getName(),
                entity.getState(),
                entity.getCountryCode(),
                entity.getCivicBody() != null ? entity.getCivicBody().getId() : null,
                entity.getCivicBody() != null ? entity.getCivicBody().getName() : null,
                entity.getSource(),
                entity.getSourceUrl(),
                entity.getLastVerifiedAt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                wards,
                issues
        );
    }

    private WardResponse toWardResponse(WardEntity entity) {
        long issues = issueRepository.countByWard_Id(entity.getId());
        long deptMappings = wardDepartmentMappingRepository.countByWardId(entity.getId());

        String wkt = null;
        if (entity.getBoundaryGeometry() != null) {
            WKTWriter writer = new WKTWriter();
            wkt = writer.write(entity.getBoundaryGeometry());
        }

        return new WardResponse(
                entity.getId(),
                entity.getWardNumber(),
                entity.getWardName(),
                entity.getWardCode(),
                entity.getCity() != null ? entity.getCity().getId() : null,
                entity.getCity() != null ? entity.getCity().getName() : null,
                entity.getCivicBody() != null ? entity.getCivicBody().getId() : null,
                entity.getCivicBody() != null ? entity.getCivicBody().getName() : null,
                entity.getBoundaryGeometry() != null,
                wkt,
                entity.getSource(),
                entity.getSourceUrl(),
                entity.getLastVerifiedAt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                issues,
                deptMappings
        );
    }

    private DepartmentResponse toDepartmentResponse(DepartmentEntity entity) {
        long issues = issueRepository.countByDepartment_Id(entity.getId());
        long catMappings = categoryDepartmentMappingRepository.countByDepartmentId(entity.getId());
        long wardMappings = wardDepartmentMappingRepository.countByDepartmentId(entity.getId());

        return new DepartmentResponse(
                entity.getId(),
                entity.getCivicBody() != null ? entity.getCivicBody().getId() : null,
                entity.getCivicBody() != null ? entity.getCivicBody().getName() : null,
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getSource(),
                entity.getSourceUrl(),
                entity.getLastVerifiedAt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                issues,
                catMappings,
                wardMappings
        );
    }

    private CategoryDepartmentMappingResponse toCategoryMappingResponse(CategoryDepartmentMappingEntity entity) {
        return new CategoryDepartmentMappingResponse(
                entity.getId(),
                entity.getCategory().getId(),
                entity.getCategory().getName(),
                entity.getCategory().getSlug(),
                entity.getDepartment().getId(),
                entity.getDepartment().getName(),
                entity.getDepartment().getCode(),
                entity.getDepartment().getCivicBody() != null ? entity.getDepartment().getCivicBody().getId() : null,
                entity.getDepartment().getCivicBody() != null ? entity.getDepartment().getCivicBody().getName() : null,
                entity.isActive(),
                entity.getSource(),
                entity.getSourceUrl(),
                entity.getLastVerifiedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private WardDepartmentMappingResponse toWardMappingResponse(WardDepartmentMappingEntity entity) {
        return new WardDepartmentMappingResponse(
                entity.getId(),
                entity.getWard().getId(),
                entity.getWard().getWardNumber(),
                entity.getWard().getWardName(),
                entity.getWard().getWardCode(),
                entity.getWard().getCity() != null ? entity.getWard().getCity().getId() : null,
                entity.getWard().getCity() != null ? entity.getWard().getCity().getName() : null,
                entity.getDepartment().getId(),
                entity.getDepartment().getName(),
                entity.getDepartment().getCode(),
                entity.getDepartment().getCivicBody() != null ? entity.getDepartment().getCivicBody().getId() : null,
                entity.getDepartment().getCivicBody() != null ? entity.getDepartment().getCivicBody().getName() : null,
                entity.isActive(),
                entity.getSource(),
                entity.getSourceUrl(),
                entity.getLastVerifiedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private String clean(String str) {
        if (str == null) return null;
        String t = str.trim();
        return t.isEmpty() ? null : t;
    }
}
