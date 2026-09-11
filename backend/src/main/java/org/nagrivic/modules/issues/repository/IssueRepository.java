package org.nagrivic.modules.issues.repository;

import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<IssueEntity, UUID> {
    List<IssueEntity> findByReporter_Id(UUID reporterId);
    List<IssueEntity> findByCategory_Id(UUID categoryId);
    List<IssueEntity> findByStatus(IssueStatus status);
}
