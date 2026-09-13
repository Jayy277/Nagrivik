package org.nagrivic.modules.statushistory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nagrivic.modules.issues.model.IssueStatus;

public record ChangeStatusRequest(
    @NotNull(message = "Target status is required")
    IssueStatus status,

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    String reason
) {}
