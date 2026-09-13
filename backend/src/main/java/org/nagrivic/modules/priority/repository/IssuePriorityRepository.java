package org.nagrivic.modules.priority.repository;

import org.nagrivic.modules.priority.entity.IssuePriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssuePriorityRepository extends JpaRepository<IssuePriorityEntity, UUID> {
    Optional<IssuePriorityEntity> findByIssue_Id(UUID issueId);
    List<IssuePriorityEntity> findByIssue_IdIn(Collection<UUID> issueIds);

    @Query("SELECT p FROM IssuePriorityEntity p JOIN FETCH p.issue WHERE p.issue.id IN :issueIds")
    List<IssuePriorityEntity> findByIssueIdsWithIssue(@Param("issueIds") Collection<UUID> issueIds);
}
