package org.nagrivic.modules.accountability.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicAccountabilityResponse(
        UUID cityId,
        String cityName,
        UUID wardId,
        String wardName,
        UUID categoryId,
        String categoryName,
        String range,
        AccountabilitySummaryDto summary,
        StatusBreakdownDto statusBreakdown,
        PriorityBreakdownDto priorityBreakdown,
        List<CategoryBreakdownDto> categoryBreakdown,
        List<WardBreakdownDto> wardBreakdown,
        AgingBreakdownDto agingBreakdown,
        VerificationSummaryDto verificationSummary,
        ResponsibilitySummaryDto responsibilitySummary,
        List<TrendPointDto> trend,
        Instant lastUpdated
) {}
