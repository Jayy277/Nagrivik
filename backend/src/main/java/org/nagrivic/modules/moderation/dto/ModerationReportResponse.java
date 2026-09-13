package org.nagrivic.modules.moderation.dto;

import org.nagrivic.modules.moderation.entity.ModerationReportEntity;
import org.nagrivic.modules.moderation.model.ModerationReason;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.model.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ModerationReportResponse(
        UUID id,
        ModerationTargetType targetType,
        UUID targetId,
        ModerationReason reason,
        String description,
        ReportStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
    public static ModerationReportResponse fromEntity(ModerationReportEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ModerationReportResponse(
                entity.getId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getReason(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }
}
