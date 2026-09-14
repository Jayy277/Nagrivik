package org.nagrivic.modules.civicgeography.service;

import org.nagrivic.modules.civicgeography.dto.admin.CivicGeographyAuditResponse;
import org.nagrivic.modules.civicgeography.entity.CivicGeographyAuditEntity;
import org.nagrivic.modules.civicgeography.repository.CivicGeographyAuditRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CivicGeographyAuditServiceImpl implements CivicGeographyAuditService {

    private final CivicGeographyAuditRepository auditRepository;

    public CivicGeographyAuditServiceImpl(CivicGeographyAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public CivicGeographyAuditEntity recordAudit(
            UserEntity actor,
            String entityType,
            UUID entityId,
            String action,
            String previousState,
            String newState,
            String reason,
            String source,
            String sourceUrl
    ) {
        CivicGeographyAuditEntity audit = new CivicGeographyAuditEntity(
                actor,
                entityType,
                entityId,
                action,
                previousState,
                newState,
                reason,
                source,
                sourceUrl
        );
        return auditRepository.save(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CivicGeographyAuditResponse> getAudits(String entityType, String action, Pageable pageable) {
        return auditRepository.findFiltered(entityType, action, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CivicGeographyAuditResponse> getEntityAudits(String entityType, UUID entityId, Pageable pageable) {
        return auditRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAudits() {
        return auditRepository.count();
    }

    private CivicGeographyAuditResponse toResponse(CivicGeographyAuditEntity entity) {
        String actorName = entity.getActor() != null ? entity.getActor().getFullName() : "System";
        String actorEmail = entity.getActor() != null ? entity.getActor().getEmail() : null;
        UUID actorId = entity.getActor() != null ? entity.getActor().getId() : null;

        return new CivicGeographyAuditResponse(
                entity.getId(),
                actorId,
                actorName,
                actorEmail,
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getPreviousState(),
                entity.getNewState(),
                entity.getReason(),
                entity.getSource(),
                entity.getSourceUrl(),
                entity.getCreatedAt()
        );
    }
}
