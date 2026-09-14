package org.nagrivic.modules.authorities.dto;

import org.nagrivic.modules.authorities.entity.AuthorityAssignmentEntity;

import java.util.UUID;

public record AuthorityScopeDto(
        UUID id,
        UUID civicBodyId,
        String civicBodyName,
        UUID cityId,
        String cityName,
        UUID wardId,
        String wardName,
        String wardNumber,
        UUID departmentId,
        String departmentName,
        String departmentCode,
        String designation,
        boolean active
) {
    public static AuthorityScopeDto fromEntity(AuthorityAssignmentEntity a) {
        return new AuthorityScopeDto(
                a.getId(),
                a.getCivicBody() != null ? a.getCivicBody().getId() : null,
                a.getCivicBody() != null ? a.getCivicBody().getName() : null,
                a.getCity() != null ? a.getCity().getId() : null,
                a.getCity() != null ? a.getCity().getName() : null,
                a.getWard() != null ? a.getWard().getId() : null,
                a.getWard() != null ? a.getWard().getWardName() : null,
                a.getWard() != null ? a.getWard().getWardNumber() : null,
                a.getDepartment() != null ? a.getDepartment().getId() : null,
                a.getDepartment() != null ? a.getDepartment().getName() : null,
                a.getDepartment() != null ? a.getDepartment().getCode() : null,
                a.getDesignation(),
                a.isActive()
        );
    }
}
