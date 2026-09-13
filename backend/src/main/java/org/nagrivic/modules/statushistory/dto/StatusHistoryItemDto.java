package org.nagrivic.modules.statushistory.dto;

import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.statushistory.entity.StatusHistoryEntity;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryItemDto(
    UUID id,
    IssueStatus fromStatus,
    IssueStatus toStatus,
    String reason,
    ActorSummary changedBy,
    Instant createdAt
) {
    public record ActorSummary(
        String displayName
    ) {}

    public static StatusHistoryItemDto fromEntity(StatusHistoryEntity entity) {
        ActorSummary actorSummary;
        if (entity.getChangedByUser() != null) {
            UserEntity u = entity.getChangedByUser();
            String displayName = resolveDisplayName(u);
            actorSummary = new ActorSummary(displayName);
        } else {
            actorSummary = new ActorSummary("System");
        }

        return new StatusHistoryItemDto(
            entity.getId(),
            entity.getFromStatus(),
            entity.getToStatus(),
            entity.getReason(),
            actorSummary,
            entity.getCreatedAt()
        );
    }

    private static String resolveDisplayName(UserEntity u) {
        String role = u.getRole();
        if ("OFFICER".equalsIgnoreCase(role)) {
            return "Municipal Officer";
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            return "Administrator";
        } else {
            return "Citizen";
        }
    }
}
