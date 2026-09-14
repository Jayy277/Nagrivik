package org.nagrivic.modules.duplicates.repository;

import org.nagrivic.modules.duplicates.entity.AiDuplicateSuggestionEntity;
import org.nagrivic.modules.duplicates.model.DuplicateConfidence;
import org.nagrivic.modules.duplicates.model.DuplicateSuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiDuplicateSuggestionRepository extends JpaRepository<AiDuplicateSuggestionEntity, UUID> {

    Optional<AiDuplicateSuggestionEntity> findByIssue_IdAndCandidateIssue_Id(UUID issueId, UUID candidateIssueId);

    @Query("""
        SELECT s FROM AiDuplicateSuggestionEntity s
        JOIN FETCH s.issue i
        JOIN FETCH s.candidateIssue ci
        LEFT JOIN FETCH i.category
        LEFT JOIN FETCH ci.category
        LEFT JOIN FETCH i.location
        LEFT JOIN FETCH ci.location
        WHERE (:status IS NULL OR s.status = :status)
          AND (:confidence IS NULL OR s.confidence = :confidence)
          AND (:minScore IS NULL OR s.score >= :minScore)
        ORDER BY s.score DESC, s.createdAt DESC
    """)
    Page<AiDuplicateSuggestionEntity> findSuggestions(
            @Param("status") DuplicateSuggestionStatus status,
            @Param("confidence") DuplicateConfidence confidence,
            @Param("minScore") Integer minScore,
            Pageable pageable
    );

    long countByStatus(DuplicateSuggestionStatus status);
}
