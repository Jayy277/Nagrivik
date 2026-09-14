package org.nagrivic.modules.moderation.dto;

import org.nagrivic.modules.moderation.model.ModerationReason;
import org.nagrivic.modules.moderation.model.ModerationStatus;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.model.ReportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ModerationReportDetailResponse(
        UUID id,
        ReportStatus status,
        ModerationReason reason,
        String description,
        ModerationTargetType targetType,
        UUID targetId,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        SafeUserSummary reporter,
        SafeUserSummary resolvedBy,
        IssueTargetDetail issueTarget,
        CommentTargetDetail commentTarget,
        List<ModerationActionResponse> actionHistory
) {

    public record SafeUserSummary(
            UUID id,
            String fullName,
            String role
    ) {
    }

    public record IssueTargetDetail(
            UUID id,
            String title,
            String description,
            String categoryName,
            String status,
            String priority,
            ModerationStatus moderationStatus,
            Instant createdAt,
            SafeUserSummary reporter,
            CivicResponsibilitySummary responsibility,
            List<String> mediaUrls
    ) {
    }

    public record CommentTargetDetail(
            UUID id,
            UUID issueId,
            String issueTitle,
            String content,
            ModerationStatus moderationStatus,
            boolean isDeleted,
            Instant createdAt,
            SafeUserSummary author
    ) {
    }

    public record CivicResponsibilitySummary(
            String civicBodyName,
            String wardName,
            String departmentName
    ) {
    }
}
