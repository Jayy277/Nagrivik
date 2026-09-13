package org.nagrivic.modules.priority.dto;

import org.nagrivic.modules.priority.entity.IssuePriorityEntity;
import org.nagrivic.modules.priority.model.PriorityLevel;

import java.time.Instant;

/**
 * Safe public representation of an issue's calculated civic priority.
 */
public record PriorityResponse(
        PriorityLevel level,
        Integer score,
        Instant calculatedAt
) {
    public static PriorityResponse fromEntity(IssuePriorityEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PriorityResponse(
                entity.getPriorityLevel(),
                entity.getScore(),
                entity.getCalculatedAt()
        );
    }
}
