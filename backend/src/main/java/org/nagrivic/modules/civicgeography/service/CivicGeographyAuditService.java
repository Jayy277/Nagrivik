package org.nagrivic.modules.civicgeography.service;

import org.nagrivic.modules.civicgeography.dto.admin.CivicGeographyAuditResponse;
import org.nagrivic.modules.civicgeography.entity.CivicGeographyAuditEntity;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CivicGeographyAuditService {

    CivicGeographyAuditEntity recordAudit(
            UserEntity actor,
            String entityType,
            UUID entityId,
            String action,
            String previousState,
            String newState,
            String reason,
            String source,
            String sourceUrl
    );

    Page<CivicGeographyAuditResponse> getAudits(String entityType, String action, Pageable pageable);

    Page<CivicGeographyAuditResponse> getEntityAudits(String entityType, UUID entityId, Pageable pageable);

    long countAudits();
}
