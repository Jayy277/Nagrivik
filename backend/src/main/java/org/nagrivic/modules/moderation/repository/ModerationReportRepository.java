package org.nagrivic.modules.moderation.repository;

import org.nagrivic.modules.moderation.entity.ModerationReportEntity;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModerationReportRepository extends JpaRepository<ModerationReportEntity, UUID> {

    boolean existsByReporter_IdAndTargetTypeAndTargetIdAndStatusIn(
            UUID reporterId,
            ModerationTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses
    );

    List<ModerationReportEntity> findByReporter_IdAndTargetTypeAndTargetIdAndStatusIn(
            UUID reporterId,
            ModerationTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses
    );

    @EntityGraph(attributePaths = {"reporter", "resolvedBy"})
    Optional<ModerationReportEntity> findDetailById(UUID id);

    @EntityGraph(attributePaths = {"reporter", "resolvedBy"})
    Page<ModerationReportEntity> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "resolvedBy"})
    Page<ModerationReportEntity> findByTargetTypeAndTargetId(
            ModerationTargetType targetType,
            UUID targetId,
            Pageable pageable
    );
}
