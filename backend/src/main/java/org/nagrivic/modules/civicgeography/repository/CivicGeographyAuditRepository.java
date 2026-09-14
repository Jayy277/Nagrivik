package org.nagrivic.modules.civicgeography.repository;

import org.nagrivic.modules.civicgeography.entity.CivicGeographyAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CivicGeographyAuditRepository extends JpaRepository<CivicGeographyAuditEntity, UUID> {

    Page<CivicGeographyAuditEntity> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    Page<CivicGeographyAuditEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId, Pageable pageable);

    Page<CivicGeographyAuditEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM CivicGeographyAuditEntity a " +
           "WHERE (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "ORDER BY a.createdAt DESC")
    Page<CivicGeographyAuditEntity> findFiltered(
            @Param("entityType") String entityType,
            @Param("action") String action,
            Pageable pageable
    );
}
