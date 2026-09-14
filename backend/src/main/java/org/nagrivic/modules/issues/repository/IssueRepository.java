package org.nagrivic.modules.issues.repository;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

@Repository
public interface IssueRepository extends JpaRepository<IssueEntity, UUID>, JpaSpecificationExecutor<IssueEntity>, IssueDiscoveryRepository {
    List<IssueEntity> findByReporter_Id(UUID reporterId);
    List<IssueEntity> findByCategory_Id(UUID categoryId);
    List<IssueEntity> findByStatus(IssueStatus status);
    List<IssueEntity> findByDuplicateOf_Id(UUID primaryIssueId);
    List<IssueEntity> findByResponsibilityStatus(ResponsibilityStatus responsibilityStatus);
    org.springframework.data.domain.Page<IssueEntity> findByResponsibilityStatus(ResponsibilityStatus responsibilityStatus, org.springframework.data.domain.Pageable pageable);

    long countByCivicBody_Id(UUID civicBodyId);
    long countByCity_Id(UUID cityId);
    long countByWard_Id(UUID wardId);
    long countByDepartment_Id(UUID departmentId);
    long countByResponsibilityStatus(ResponsibilityStatus responsibilityStatus);

    @Query("SELECT i FROM IssueEntity i " +
           "LEFT JOIN FETCH i.reporter " +
           "LEFT JOIN FETCH i.category " +
           "LEFT JOIN FETCH i.location " +
           "LEFT JOIN FETCH i.civicBody " +
           "LEFT JOIN FETCH i.city " +
           "LEFT JOIN FETCH i.ward " +
           "LEFT JOIN FETCH i.department " +
           "LEFT JOIN FETCH i.priority " +
           "WHERE i.id IN :ids")
    List<IssueEntity> findWithDetailsByIdIn(@Param("ids") Collection<UUID> ids);
}
