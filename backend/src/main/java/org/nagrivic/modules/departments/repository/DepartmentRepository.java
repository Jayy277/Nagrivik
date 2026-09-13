package org.nagrivic.modules.departments.repository;

import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {
    List<DepartmentEntity> findByCivicBodyId(UUID civicBodyId);
    Optional<DepartmentEntity> findByCivicBodyIdAndName(UUID civicBodyId, String name);
    Optional<DepartmentEntity> findByCivicBodyIdAndCode(UUID civicBodyId, String code);
}
