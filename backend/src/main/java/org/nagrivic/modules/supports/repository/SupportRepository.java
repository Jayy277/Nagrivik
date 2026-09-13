package org.nagrivic.modules.supports.repository;

import org.nagrivic.modules.supports.entity.SupportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportRepository extends JpaRepository<SupportEntity, UUID> {

    boolean existsByIssue_IdAndUser_Id(UUID issueId, UUID userId);

    Optional<SupportEntity> findByIssue_IdAndUser_Id(UUID issueId, UUID userId);

    long countByIssue_Id(UUID issueId);

    void deleteByIssue_IdAndUser_Id(UUID issueId, UUID userId);

    /**
     * Batch count supports for multiple issues to avoid N+1 queries.
     * Returns tuples of [UUID issueId, Long count].
     */
    @Query("SELECT s.issue.id, COUNT(s.id) FROM SupportEntity s WHERE s.issue.id IN :issueIds GROUP BY s.issue.id")
    List<Object[]> countSupportsByIssueIds(@Param("issueIds") Collection<UUID> issueIds);

    /**
     * Batch check whether the specified user supports any of the given issues.
     * Returns the list of issue IDs supported by the user.
     */
    @Query("SELECT s.issue.id FROM SupportEntity s WHERE s.user.id = :userId AND s.issue.id IN :issueIds")
    List<UUID> findSupportedIssueIdsByUser(@Param("userId") UUID userId, @Param("issueIds") Collection<UUID> issueIds);

    /**
     * Finds all supporter user IDs for a given issue ID.
     */
    @Query("SELECT s.user.id FROM SupportEntity s WHERE s.issue.id = :issueId")
    List<UUID> findSupporterUserIdsByIssueId(@Param("issueId") UUID issueId);
}
