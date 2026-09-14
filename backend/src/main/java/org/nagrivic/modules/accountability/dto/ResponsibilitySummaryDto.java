package org.nagrivic.modules.accountability.dto;

import java.util.List;

public record ResponsibilitySummaryDto(
        long resolvedCount,
        long unresolvedCount,
        double coveragePercentage,
        List<DepartmentBreakdownDto> departmentBreakdown
) {}
