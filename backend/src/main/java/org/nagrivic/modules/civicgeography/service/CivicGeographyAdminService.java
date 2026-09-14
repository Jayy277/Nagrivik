package org.nagrivic.modules.civicgeography.service;

import org.nagrivic.modules.civicgeography.dto.admin.*;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CivicGeographyAdminService {

    GeographyOverviewResponse getOverview();

    // Civic Bodies
    Page<CivicBodyResponse> getCivicBodies(Pageable pageable);
    CivicBodyResponse getCivicBody(UUID id);
    CivicBodyResponse createCivicBody(CreateCivicBodyRequest request, UserEntity actor);
    CivicBodyResponse updateCivicBody(UUID id, UpdateCivicBodyRequest request, UserEntity actor);
    CivicBodyResponse toggleCivicBodyActive(UUID id, boolean active, String reason, Long version, UserEntity actor);
    void deleteCivicBody(UUID id, Long version, UserEntity actor);

    // Cities
    Page<CityResponse> getCities(UUID civicBodyId, Pageable pageable);
    CityResponse getCity(UUID id);
    CityResponse createCity(CreateCityRequest request, UserEntity actor);
    CityResponse updateCity(UUID id, UpdateCityRequest request, UserEntity actor);
    CityResponse toggleCityActive(UUID id, boolean active, String reason, Long version, UserEntity actor);
    void deleteCity(UUID id, Long version, UserEntity actor);

    // Wards
    Page<WardResponse> getWards(UUID cityId, Boolean isActive, String search, Pageable pageable);
    WardResponse getWard(UUID id);
    WardResponse createWard(CreateWardRequest request, UserEntity actor);
    WardResponse updateWard(UUID id, UpdateWardRequest request, UserEntity actor);
    WardResponse toggleWardActive(UUID id, boolean active, String reason, Long version, UserEntity actor);
    void deleteWard(UUID id, Long version, UserEntity actor);
    WardBoundaryValidationResponse validateWardBoundaries();

    // Departments
    Page<DepartmentResponse> getDepartments(UUID civicBodyId, Boolean isActive, String search, Pageable pageable);
    DepartmentResponse getDepartment(UUID id);
    DepartmentResponse createDepartment(CreateDepartmentRequest request, UserEntity actor);
    DepartmentResponse updateDepartment(UUID id, UpdateDepartmentRequest request, UserEntity actor);
    DepartmentResponse toggleDepartmentActive(UUID id, boolean active, String reason, Long version, UserEntity actor);
    void deleteDepartment(UUID id, Long version, UserEntity actor);

    // Category mappings
    Page<CategoryDepartmentMappingResponse> getCategoryMappings(UUID categoryId, UUID departmentId, Boolean isActive, Pageable pageable);
    CategoryDepartmentMappingResponse getCategoryMapping(UUID id);
    CategoryDepartmentMappingResponse createCategoryMapping(CreateCategoryDepartmentMappingRequest request, UserEntity actor);
    CategoryDepartmentMappingResponse updateCategoryMapping(UUID id, UpdateCategoryDepartmentMappingRequest request, UserEntity actor);
    CategoryDepartmentMappingResponse toggleCategoryMappingActive(UUID id, boolean active, String reason, Long version, UserEntity actor);
    void deleteCategoryMapping(UUID id, Long version, UserEntity actor);

    // Ward mappings
    Page<WardDepartmentMappingResponse> getWardMappings(UUID wardId, UUID departmentId, Boolean isActive, Pageable pageable);
    WardDepartmentMappingResponse getWardMapping(UUID id);
    WardDepartmentMappingResponse createWardMapping(CreateWardDepartmentMappingRequest request, UserEntity actor);
    WardDepartmentMappingResponse updateWardMapping(UUID id, UpdateWardDepartmentMappingRequest request, UserEntity actor);
    WardDepartmentMappingResponse toggleWardMappingActive(UUID id, boolean active, String reason, Long version, UserEntity actor);
    void deleteWardMapping(UUID id, Long version, UserEntity actor);

    // Bounded re-resolution
    ReResolveResponse reResolveResponsibility(ReResolveRequest request, UserEntity actor);
}
