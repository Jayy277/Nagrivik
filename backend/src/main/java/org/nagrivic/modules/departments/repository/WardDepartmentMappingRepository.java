package org.nagrivic.modules.departments.repository;

import org.nagrivic.modules.departments.entity.WardDepartmentMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WardDepartmentMappingRepository extends JpaRepository<WardDepartmentMappingEntity, UUID> {
    List<WardDepartmentMappingEntity> findByWardIdAndIsActiveTrue(UUID wardId);
    List<WardDepartmentMappingEntity> findByWardId(UUID wardId);
    Optional<WardDepartmentMappingEntity> findByWardIdAndDepartmentId(UUID wardId, UUID departmentId);
    List<WardDepartmentMappingEntity> findByDepartmentId(UUID departmentId);
    long countByWardId(UUID wardId);
    long countByDepartmentId(UUID departmentId);

    @org.springframework.data.jpa.repository.Query("SELECT wdm FROM WardDepartmentMappingEntity wdm " +
           "WHERE (:wardId IS NULL OR wdm.ward.id = :wardId) " +
           "AND (:departmentId IS NULL OR wdm.department.id = :departmentId) " +
           "AND (:isActive IS NULL OR wdm.isActive = :isActive) " +
           "ORDER BY wdm.createdAt DESC")
    org.springframework.data.domain.Page<WardDepartmentMappingEntity> searchMappings(
            @org.springframework.data.repository.query.Param("wardId") UUID wardId,
            @org.springframework.data.repository.query.Param("departmentId") UUID departmentId,
            @org.springframework.data.repository.query.Param("isActive") Boolean isActive,
            org.springframework.data.domain.Pageable pageable
    );

    List<WardDepartmentMappingEntity> findAllByOrderByCreatedAtDesc();
}
