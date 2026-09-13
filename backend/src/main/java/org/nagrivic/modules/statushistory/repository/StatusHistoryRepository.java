package org.nagrivic.modules.statushistory.repository;

import org.nagrivic.modules.statushistory.entity.StatusHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistoryEntity, UUID> {
    List<StatusHistoryEntity> findByIssue_IdOrderByCreatedAtAsc(UUID issueId);
    Page<StatusHistoryEntity> findByIssue_IdOrderByCreatedAtAsc(UUID issueId, Pageable pageable);
}
