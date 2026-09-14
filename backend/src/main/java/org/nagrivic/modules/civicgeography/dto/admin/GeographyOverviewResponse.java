package org.nagrivic.modules.civicgeography.dto.admin;

public record GeographyOverviewResponse(
        long totalCivicBodies,
        long activeCivicBodies,
        long totalCities,
        long activeCities,
        long totalWards,
        long activeWards,
        long wardsWithBoundaries,
        long wardsWithoutBoundaries,
        long totalDepartments,
        long activeDepartments,
        long totalCategoryMappings,
        long activeCategoryMappings,
        long totalWardMappings,
        long activeWardMappings,
        long totalIssues,
        long resolvedIssues,
        long unresolvedIssues,
        long totalAudits
) {}
