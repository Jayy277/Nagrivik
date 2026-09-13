package org.nagrivic.modules.moderation.dto;

import org.nagrivic.modules.moderation.entity.ModerationActionEntity;
import org.nagrivic.modules.moderation.model.ModerationActionType;
import org.nagrivic.modules.moderation.model.ModerationTargetType;

import java.time.Instant;
import java.util.UUID;

public record ModerationActionResponse(
        UUID id,
        UUID reportId,
        ModerationTargetType targetType,
        UUID targetId,
        ModerationActionType action,
        String reason,
        Instant createdAt
) {
    public static ModerationActionResponse fromEntity(ModerationActionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ModerationActionResponse(
                entity.getId(),
                entity.getReport() != null ? entity.getReport().getId() : null,
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getAction(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
