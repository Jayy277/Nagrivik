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
    Optional<CategoryDepartmentMappingEntity> findByCategoryIdAndDepartmentId(UUID categoryId, UUID departmentId);
}
