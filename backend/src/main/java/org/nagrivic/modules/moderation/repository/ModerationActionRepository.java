package org.nagrivic.modules.moderation.repository;

import org.nagrivic.modules.moderation.entity.ModerationActionEntity;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationActionEntity, UUID> {

    @EntityGraph(attributePaths = {"moderator", "report"})
    Page<ModerationActionEntity> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ModerationTargetType targetType,
            UUID targetId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"moderator", "report"})
    Page<ModerationActionEntity> findByModerator_IdOrderByCreatedAtDesc(
            UUID moderatorId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"moderator", "report"})
    java.util.List<ModerationActionEntity> findByReport_IdOrderByCreatedAtDesc(UUID reportId);
}
