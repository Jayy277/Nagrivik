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
    Optional<WardDepartmentMappingEntity> findByWardIdAndDepartmentId(UUID wardId, UUID departmentId);
}
