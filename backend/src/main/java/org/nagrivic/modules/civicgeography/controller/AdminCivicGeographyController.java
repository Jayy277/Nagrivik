package org.nagrivic.modules.civicgeography.controller;

import jakarta.validation.Valid;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.civicgeography.dto.admin.*;
import org.nagrivic.modules.civicgeography.service.CivicGeographyAdminService;
import org.nagrivic.modules.civicgeography.service.CivicGeographyAuditService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/geography")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCivicGeographyController {

    private final CivicGeographyAdminService adminService;
    private final CivicGeographyAuditService auditService;
    private final CurrentUserService currentUserService;

    public AdminCivicGeographyController(
            CivicGeographyAdminService adminService,
            CivicGeographyAuditService auditService,
            CurrentUserService currentUserService
    ) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    // =========================================================================
    // OVERVIEW
    // =========================================================================

    @GetMapping("/overview")
    public ResponseEntity<GeographyOverviewResponse> getOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    // =========================================================================
    // CIVIC BODIES
    // =========================================================================

    @GetMapping("/civic-bodies")
    public ResponseEntity<Page<CivicBodyResponse>> getCivicBodies(Pageable pageable) {
        return ResponseEntity.ok(adminService.getCivicBodies(pageable));
    }

    @GetMapping("/civic-bodies/{id}")
    public ResponseEntity<CivicBodyResponse> getCivicBody(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getCivicBody(id));
    }

    @PostMapping("/civic-bodies")
    public ResponseEntity<CivicBodyResponse> createCivicBody(@Valid @RequestBody CreateCivicBodyRequest request) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCivicBody(request, actor));
    }

    @PutMapping("/civic-bodies/{id}")
    public ResponseEntity<CivicBodyResponse> updateCivicBody(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCivicBodyRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.updateCivicBody(id, request, actor));
    }

    @PatchMapping("/civic-bodies/{id}/active")
    public ResponseEntity<CivicBodyResponse> toggleCivicBodyActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.toggleCivicBodyActive(id, active, reason, version, actor));
    }

    @DeleteMapping("/civic-bodies/{id}")
    public ResponseEntity<Void> deleteCivicBody(
            @PathVariable UUID id,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        adminService.deleteCivicBody(id, version, actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // CITIES
    // =========================================================================

    @GetMapping("/cities")
    public ResponseEntity<Page<CityResponse>> getCities(
            @RequestParam(required = false) UUID civicBodyId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getCities(civicBodyId, pageable));
    }

    @GetMapping("/cities/{id}")
    public ResponseEntity<CityResponse> getCity(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getCity(id));
    }

    @PostMapping("/cities")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CreateCityRequest request) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCity(request, actor));
    }

    @PutMapping("/cities/{id}")
    public ResponseEntity<CityResponse> updateCity(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCityRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.updateCity(id, request, actor));
    }

    @PatchMapping("/cities/{id}/active")
    public ResponseEntity<CityResponse> toggleCityActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.toggleCityActive(id, active, reason, version, actor));
    }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<Void> deleteCity(
            @PathVariable UUID id,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        adminService.deleteCity(id, version, actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // WARDS
    // =========================================================================

    @GetMapping("/wards")
    public ResponseEntity<Page<WardResponse>> getWards(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getWards(cityId, isActive, search, pageable));
    }

    @GetMapping("/wards/{id}")
    public ResponseEntity<WardResponse> getWard(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getWard(id));
    }

    @PostMapping("/wards")
    public ResponseEntity<WardResponse> createWard(@Valid @RequestBody CreateWardRequest request) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createWard(request, actor));
    }

    @PutMapping("/wards/{id}")
    public ResponseEntity<WardResponse> updateWard(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWardRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.updateWard(id, request, actor));
    }

    @PatchMapping("/wards/{id}/active")
    public ResponseEntity<WardResponse> toggleWardActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.toggleWardActive(id, active, reason, version, actor));
    }

    @DeleteMapping("/wards/{id}")
    public ResponseEntity<Void> deleteWard(
            @PathVariable UUID id,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        adminService.deleteWard(id, version, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/wards/validate")
    public ResponseEntity<WardBoundaryValidationResponse> validateWardBoundaries() {
        return ResponseEntity.ok(adminService.validateWardBoundaries());
    }

    // =========================================================================
    // DEPARTMENTS
    // =========================================================================

    @GetMapping("/departments")
    public ResponseEntity<Page<DepartmentResponse>> getDepartments(
            @RequestParam(required = false) UUID civicBodyId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getDepartments(civicBodyId, isActive, search, pageable));
    }

    @GetMapping("/departments/{id}")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getDepartment(id));
    }

    @PostMapping("/departments")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDepartment(request, actor));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.updateDepartment(id, request, actor));
    }

    @PatchMapping("/departments/{id}/active")
    public ResponseEntity<DepartmentResponse> toggleDepartmentActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.toggleDepartmentActive(id, active, reason, version, actor));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(
            @PathVariable UUID id,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        adminService.deleteDepartment(id, version, actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // CATEGORY MAPPINGS
    // =========================================================================

    @GetMapping("/category-mappings")
    public ResponseEntity<Page<CategoryDepartmentMappingResponse>> getCategoryMappings(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getCategoryMappings(categoryId, departmentId, isActive, pageable));
    }

    @GetMapping("/category-mappings/{id}")
    public ResponseEntity<CategoryDepartmentMappingResponse> getCategoryMapping(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getCategoryMapping(id));
    }

    @PostMapping("/category-mappings")
    public ResponseEntity<CategoryDepartmentMappingResponse> createCategoryMapping(
            @Valid @RequestBody CreateCategoryDepartmentMappingRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCategoryMapping(request, actor));
    }

    @PutMapping("/category-mappings/{id}")
    public ResponseEntity<CategoryDepartmentMappingResponse> updateCategoryMapping(
            @PathVariable UUID id,
            @RequestBody UpdateCategoryDepartmentMappingRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.updateCategoryMapping(id, request, actor));
    }

    @PatchMapping("/category-mappings/{id}/active")
    public ResponseEntity<CategoryDepartmentMappingResponse> toggleCategoryMappingActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.toggleCategoryMappingActive(id, active, reason, version, actor));
    }

    @DeleteMapping("/category-mappings/{id}")
    public ResponseEntity<Void> deleteCategoryMapping(
            @PathVariable UUID id,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        adminService.deleteCategoryMapping(id, version, actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // WARD MAPPINGS
    // =========================================================================

    @GetMapping("/ward-mappings")
    public ResponseEntity<Page<WardDepartmentMappingResponse>> getWardMappings(
            @RequestParam(required = false) UUID wardId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getWardMappings(wardId, departmentId, isActive, pageable));
    }

    @GetMapping("/ward-mappings/{id}")
    public ResponseEntity<WardDepartmentMappingResponse> getWardMapping(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getWardMapping(id));
    }

    @PostMapping("/ward-mappings")
    public ResponseEntity<WardDepartmentMappingResponse> createWardMapping(
            @Valid @RequestBody CreateWardDepartmentMappingRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createWardMapping(request, actor));
    }

    @PutMapping("/ward-mappings/{id}")
    public ResponseEntity<WardDepartmentMappingResponse> updateWardMapping(
            @PathVariable UUID id,
            @RequestBody UpdateWardDepartmentMappingRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.updateWardMapping(id, request, actor));
    }

    @PatchMapping("/ward-mappings/{id}/active")
    public ResponseEntity<WardDepartmentMappingResponse> toggleWardMappingActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(adminService.toggleWardMappingActive(id, active, reason, version, actor));
    }

    @DeleteMapping("/ward-mappings/{id}")
    public ResponseEntity<Void> deleteWardMapping(
            @PathVariable UUID id,
            @RequestParam(required = false) Long version
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        adminService.deleteWardMapping(id, version, actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // RE-RESOLUTION & AUDITS
    // =========================================================================

    @PostMapping("/re-resolve")
    public ResponseEntity<ReResolveResponse> reResolveResponsibility(
            @RequestBody(required = false) ReResolveRequest request
    ) {
        UserEntity actor = currentUserService.getCurrentUser();
        ReResolveRequest req = request != null ? request : new ReResolveRequest(null, true, 50);
        return ResponseEntity.ok(adminService.reResolveResponsibility(req, actor));
    }

    @GetMapping("/audits")
    public ResponseEntity<Page<CivicGeographyAuditResponse>> getAudits(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.getAudits(entityType, action, pageable));
    }
}
