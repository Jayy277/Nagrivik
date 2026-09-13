package org.nagrivic.modules.issues.repository;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<IssueEntity, UUID>, JpaSpecificationExecutor<IssueEntity>, IssueDiscoveryRepository {
    List<IssueEntity> findByReporter_Id(UUID reporterId);
    List<IssueEntity> findByCategory_Id(UUID categoryId);
    List<IssueEntity> findByStatus(IssueStatus status);
    List<IssueEntity> findByDuplicateOf_Id(UUID primaryIssueId);

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
