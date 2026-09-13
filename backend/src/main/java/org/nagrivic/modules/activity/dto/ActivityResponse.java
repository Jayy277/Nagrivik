package org.nagrivic.modules.activity.dto;

import org.nagrivic.modules.activity.entity.IssueActivityEntity;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Public response DTO for an entry in the Issue Activity Timeline.
 * Enforces strict citizen privacy and anonymity guards.
 */
public record ActivityResponse(
        UUID id,
        IssueActivityType eventType,
        ActorSummaryDto actor,
        Map<String, Object> data,
        Instant createdAt
) {
    public static ActivityResponse fromEntity(IssueActivityEntity entity) {
        if (entity == null) {
            return null;
        }

        ActorSummaryDto actorSummary = null;
        if (entity.getActor() != null) {
            // Principle: Supporter identities must not be exposed on the public timeline
            if (entity.getEventType() == IssueActivityType.SUPPORT_ADDED ||
                    entity.getEventType() == IssueActivityType.SUPPORT_REMOVED) {
                actorSummary = new ActorSummaryDto(null, "Citizen");
            } else {
                UserEntity u = entity.getActor();
                String displayName = (u.getFullName() != null && !u.getFullName().trim().isEmpty())
                        ? u.getFullName().trim()
                        : "Citizen";
                actorSummary = new ActorSummaryDto(u.getId(), displayName);
            }
        }

        return new ActivityResponse(
                entity.getId(),
                entity.getEventType(),
                actorSummary,
                entity.getEventData(),
                entity.getCreatedAt()
        );
    }
}
