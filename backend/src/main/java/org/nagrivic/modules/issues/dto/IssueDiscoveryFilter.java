package org.nagrivic.modules.issues.dto;

import org.nagrivic.modules.issues.model.IssueDiscoverySort;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.priority.model.PriorityLevel;

import java.util.UUID;

/**
 * Encapsulates discovery and search query filters for issues.
 */
public record IssueDiscoveryFilter(
        String q,
        UUID categoryId,
        IssueStatus status,
        PriorityLevel priority,
        UUID cityId,
        UUID wardId,
        UUID civicBodyId,
        UUID departmentId,
        UUID reportedBy,
        Double latitude,
        Double longitude,
        Double radiusMeters,
        IssueDiscoverySort sort,
        boolean includeDuplicates
) {
    public static Builder builder() {
        return new Builder();
    }

    public boolean hasGeographicSearch() {
        return latitude != null && longitude != null;
    }

    public static class Builder {
        private String q;
        private UUID categoryId;
        private IssueStatus status;
        private PriorityLevel priority;
        private UUID cityId;
        private UUID wardId;
        private UUID civicBodyId;
        private UUID departmentId;
        private UUID reportedBy;
        private Double latitude;
        private Double longitude;
        private Double radiusMeters;
        private IssueDiscoverySort sort = IssueDiscoverySort.NEWEST;
        private boolean includeDuplicates = false;

        public Builder q(String q) {
            this.q = q;
            return this;
        }

        public Builder categoryId(UUID categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder status(IssueStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(PriorityLevel priority) {
            this.priority = priority;
            return this;
        }

        public Builder cityId(UUID cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder wardId(UUID wardId) {
            this.wardId = wardId;
            return this;
        }

        public Builder civicBodyId(UUID civicBodyId) {
            this.civicBodyId = civicBodyId;
            return this;
        }

        public Builder departmentId(UUID departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public Builder reportedBy(UUID reportedBy) {
            this.reportedBy = reportedBy;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder radiusMeters(Double radiusMeters) {
            this.radiusMeters = radiusMeters;
            return this;
        }

        public Builder sort(IssueDiscoverySort sort) {
            this.sort = sort != null ? sort : IssueDiscoverySort.NEWEST;
            return this;
        }

        public Builder includeDuplicates(boolean includeDuplicates) {
            this.includeDuplicates = includeDuplicates;
            return this;
        }

        public IssueDiscoveryFilter build() {
            return new IssueDiscoveryFilter(
                    q, categoryId, status, priority, cityId, wardId, civicBodyId, departmentId,
                    reportedBy, latitude, longitude, radiusMeters, sort, includeDuplicates
            );
        }
    }
}
