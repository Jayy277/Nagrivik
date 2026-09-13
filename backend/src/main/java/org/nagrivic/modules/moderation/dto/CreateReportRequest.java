package org.nagrivic.modules.moderation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nagrivic.modules.moderation.model.ModerationReason;
import org.nagrivic.modules.moderation.model.ModerationTargetType;

import java.util.UUID;

public record CreateReportRequest(
        @NotNull(message = "Target type is required")
        ModerationTargetType targetType,

        @NotNull(message = "Target ID is required")
        UUID targetId,

        @NotNull(message = "Moderation reason is required")
        ModerationReason reason,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {
}
