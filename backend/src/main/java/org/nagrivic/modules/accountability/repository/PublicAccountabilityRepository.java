package org.nagrivic.modules.accountability.repository;

import org.nagrivic.modules.accountability.dto.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PublicAccountabilityRepository {
    AccountabilitySummaryDto getSummary(UUID cityId, UUID wardId, UUID categoryId, Instant since);
    StatusBreakdownDto getStatusBreakdown(AccountabilitySummaryDto summary);
    PriorityBreakdownDto getPriorityBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since);
    List<CategoryBreakdownDto> getCategoryBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since);
    List<WardBreakdownDto> getWardBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since);
    AgingBreakdownDto getAgingBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since);
    VerificationSummaryDto getVerificationSummary(AccountabilitySummaryDto summary);
    ResponsibilitySummaryDto getResponsibilitySummary(UUID cityId, UUID wardId, UUID categoryId, Instant since);
    List<TrendPointDto> getTrend(UUID cityId, UUID wardId, UUID categoryId, Instant since, String range);
}
