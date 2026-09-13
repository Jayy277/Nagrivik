package org.nagrivic.modules.comments.repository;

import org.nagrivic.modules.comments.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "issue"})
    Page<CommentEntity> findByIssue_Id(UUID issueId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "issue"})
    Page<CommentEntity> findByIssue_IdAndModerationStatusNot(
            UUID issueId,
            org.nagrivic.modules.moderation.model.ModerationStatus moderationStatus,
            Pageable pageable
    );

    long countByIssue_IdAndDeletedAtIsNull(UUID issueId);

    /**
     * Batch count active (non-deleted) comments for multiple issues to avoid N+1 queries.
     * Returns tuples of [UUID issueId, Long count].
     */
    @Query("SELECT c.issue.id, COUNT(c.id) FROM CommentEntity c WHERE c.issue.id IN :issueIds AND c.deletedAt IS NULL GROUP BY c.issue.id")
    List<Object[]> countActiveCommentsByIssueIds(@Param("issueIds") Collection<UUID> issueIds);
}
