package org.nagrivic.modules.authorities.repository;

import org.nagrivic.modules.authorities.entity.ResolutionEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResolutionEvidenceRepository extends JpaRepository<ResolutionEvidenceEntity, UUID> {

    @Query("SELECT r FROM ResolutionEvidenceEntity r JOIN FETCH r.submittedBy WHERE r.issue.id = :issueId ORDER BY r.createdAt ASC")
    List<ResolutionEvidenceEntity> findByIssue_IdOrderByCreatedAtAsc(@Param("issueId") UUID issueId);

    long countByIssue_Id(UUID issueId);

    @Query("SELECT r FROM ResolutionEvidenceEntity r JOIN FETCH r.submittedBy WHERE r.issue.id = :issueId ORDER BY r.createdAt DESC")
    List<ResolutionEvidenceEntity> findByIssue_IdOrderByCreatedAtDesc(@Param("issueId") UUID issueId);

    Optional<ResolutionEvidenceEntity> findTopByIssue_IdOrderByCreatedAtDesc(UUID issueId);

    boolean existsByStorageKey(String storageKey);

    Optional<ResolutionEvidenceEntity> findByStorageKey(String storageKey);
}
