package org.nagrivic.modules.priority.ai.repository;

import org.nagrivic.modules.priority.ai.entity.IssueAiPriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueAiPriorityRepository extends JpaRepository<IssueAiPriorityEntity, UUID> {

    Optional<IssueAiPriorityEntity> findByIssue_Id(UUID issueId);

    default Optional<IssueAiPriorityEntity> findByIssueId(UUID issueId) {
        return findByIssue_Id(issueId);
    }

    boolean existsByIssue_Id(UUID issueId);

    void deleteByIssue_Id(UUID issueId);
}
