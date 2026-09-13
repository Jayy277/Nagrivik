package org.nagrivic.modules.activity.repository;

import org.nagrivic.modules.activity.entity.IssueActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IssueActivityRepository extends JpaRepository<IssueActivityEntity, UUID> {

    @EntityGraph(attributePaths = {"actor"})
    Page<IssueActivityEntity> findByIssue_Id(UUID issueId, Pageable pageable);
}
