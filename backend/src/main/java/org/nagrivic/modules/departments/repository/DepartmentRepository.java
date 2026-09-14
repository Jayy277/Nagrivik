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
    boolean existsByCivicBodyIdAndNameIgnoreCase(UUID civicBodyId, String name);
    boolean existsByCivicBodyIdAndCodeIgnoreCase(UUID civicBodyId, String code);

    long countByCivicBodyId(UUID civicBodyId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DepartmentEntity d " +
           "WHERE (:civicBodyId IS NULL OR d.civicBody.id = :civicBodyId) " +
           "AND (:isActive IS NULL OR d.isActive = :isActive) " +
           "AND (:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY d.name ASC")
    org.springframework.data.domain.Page<DepartmentEntity> searchDepartments(
            @org.springframework.data.repository.query.Param("civicBodyId") UUID civicBodyId,
            @org.springframework.data.repository.query.Param("isActive") Boolean isActive,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    List<DepartmentEntity> findAllByOrderByNameAsc();
}
