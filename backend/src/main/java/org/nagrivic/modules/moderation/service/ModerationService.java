package org.nagrivic.modules.moderation.service;

import org.nagrivic.modules.moderation.dto.*;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.model.ReportStatus;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ModerationService {

    /**
     * Submits a new moderation report as the authenticated citizen.
     */
    ModerationReportResponse submitReport(CreateReportRequest request);

    /**
     * Lists moderation reports in the moderation queue (moderators/admins only).
     */
    Page<ModerationReportResponse> getReports(ReportStatus status, Pageable pageable);

    /**
     * Marks a report as IN_REVIEW by a moderator.
     */
    ModerationReportResponse reviewReport(UUID reportId);

    /**
     * Resolves a report with a definitive moderation action and audit log.
     */
    ModerationReportResponse resolveReport(UUID reportId, ResolveReportRequest request);

    /**
     * Dismisses a report as non-violating.
     */
    ModerationReportResponse dismissReport(UUID reportId, String reason);

    /**
     * Soft-hides content from public APIs without deleting it.
     */
    ModerationActionResponse hideContent(ModerationTargetType targetType, UUID targetId, String reason, String notes);

    /**
     * Restores hidden content back to visible standing.
     */
    ModerationActionResponse restoreContent(ModerationTargetType targetType, UUID targetId, String reason, String notes);

    /**
     * Temporarily or indefinitely restricts a user from submitting contributions.
     */
    ModerationActionResponse restrictUser(UUID userId, Long durationMinutes, String reason);

    /**
     * Restores a restricted user back to active standing.
     */
    ModerationActionResponse unrestrictUser(UUID userId, String reason);

    /**
     * Throws an AuthException.forbidden if the user is currently restricted.
     */
    void checkUserNotRestricted(UserEntity user);
}
