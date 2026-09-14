package org.nagrivic.modules.civicgeography.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record CivicGeographyAuditResponse(
        UUID id,
        UUID actorId,
        String actorName,
        String actorEmail,
        String entityType,
        UUID entityId,
        String action,
        String previousState,
        String newState,
        String reason,
        String source,
        String sourceUrl,
        Instant createdAt
) {}
