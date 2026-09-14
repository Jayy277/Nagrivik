package org.nagrivic.modules.departments.repository;

import org.nagrivic.modules.departments.entity.CategoryDepartmentMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryDepartmentMappingRepository extends JpaRepository<CategoryDepartmentMappingEntity, UUID> {
    List<CategoryDepartmentMappingEntity> findByCategoryId(UUID categoryId);
    List<CategoryDepartmentMappingEntity> findByCategoryIdAndIsActiveTrue(UUID categoryId);
    Optional<CategoryDepartmentMappingEntity> findByCategoryIdAndDepartmentId(UUID categoryId, UUID departmentId);
    List<CategoryDepartmentMappingEntity> findByDepartmentId(UUID departmentId);
    long countByDepartmentId(UUID departmentId);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM CategoryDepartmentMappingEntity m " +
           "WHERE (:categoryId IS NULL OR m.category.id = :categoryId) " +
           "AND (:departmentId IS NULL OR m.department.id = :departmentId) " +
           "AND (:isActive IS NULL OR m.isActive = :isActive) " +
           "ORDER BY m.createdAt DESC")
    org.springframework.data.domain.Page<CategoryDepartmentMappingEntity> searchMappings(
            @org.springframework.data.repository.query.Param("categoryId") UUID categoryId,
            @org.springframework.data.repository.query.Param("departmentId") UUID departmentId,
            @org.springframework.data.repository.query.Param("isActive") Boolean isActive,
            org.springframework.data.domain.Pageable pageable
    );

    List<CategoryDepartmentMappingEntity> findAllByOrderByCreatedAtDesc();
}
