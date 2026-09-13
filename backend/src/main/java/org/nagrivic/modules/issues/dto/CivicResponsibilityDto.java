package org.nagrivic.modules.issues.dto;

import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;

import java.util.UUID;

/**
 * Exposes safe, server-controlled civic responsibility information.
 * Does not expose internal database details, raw geometry, or sensitive contact info.
 */
public record CivicResponsibilityDto(
        ResponsibilityStatus status,
        CivicBodySummary civicBody,
        CitySummary city,
        WardSummary ward,
        DepartmentSummary department
) {
    public record CivicBodySummary(UUID id, String name) {
        public static CivicBodySummary fromEntity(CivicBodyEntity entity) {
            return entity != null ? new CivicBodySummary(entity.getId(), entity.getName()) : null;
        }
    }

    public record CitySummary(UUID id, String name) {
        public static CitySummary fromEntity(CityEntity entity) {
            return entity != null ? new CitySummary(entity.getId(), entity.getName()) : null;
        }
    }

    public record WardSummary(UUID id, String name) {
        public static WardSummary fromEntity(WardEntity entity) {
            return entity != null ? new WardSummary(entity.getId(), entity.getWardName()) : null;
        }
    }

    public record DepartmentSummary(UUID id, String name) {
        public static DepartmentSummary fromEntity(DepartmentEntity entity) {
            return entity != null ? new DepartmentSummary(entity.getId(), entity.getName()) : null;
        }
    }

    public static CivicResponsibilityDto unresolved() {
        return new CivicResponsibilityDto(ResponsibilityStatus.UNRESOLVED, null, null, null, null);
    }

    public static CivicResponsibilityDto fromEntity(IssueEntity issue) {
        if (issue == null) {
            return unresolved();
        }

        ResponsibilityStatus status = issue.getResponsibilityStatus() != null
                ? issue.getResponsibilityStatus()
                : ResponsibilityStatus.UNRESOLVED;

        CivicBodySummary civicBody = CivicBodySummary.fromEntity(issue.getCivicBody());
        CitySummary city = CitySummary.fromEntity(issue.getCity());
        WardSummary ward = WardSummary.fromEntity(issue.getWard());
        DepartmentSummary department = DepartmentSummary.fromEntity(issue.getDepartment());

        return new CivicResponsibilityDto(status, civicBody, city, ward, department);
    }
}
