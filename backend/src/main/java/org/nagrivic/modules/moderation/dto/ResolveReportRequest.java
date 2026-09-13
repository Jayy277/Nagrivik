package org.nagrivic.modules.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nagrivic.modules.moderation.model.ModerationActionType;

public record ResolveReportRequest(
        @NotNull(message = "Moderation action is required")
        ModerationActionType action,

        @NotBlank(message = "Resolution reason is required")
        @Size(max = 255, message = "Reason must not exceed 255 characters")
        String reason,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}
