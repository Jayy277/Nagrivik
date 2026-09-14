package org.nagrivic.modules.accountability.service;

import org.nagrivic.modules.accountability.dto.*;
import org.nagrivic.modules.accountability.repository.PublicAccountabilityRepository;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PublicAccountabilityServiceImpl implements PublicAccountabilityService {

    private final PublicAccountabilityRepository accountabilityRepository;
    private final CityRepository cityRepository;
    private final WardRepository wardRepository;
    private final CategoryRepository categoryRepository;

    public PublicAccountabilityServiceImpl(
            PublicAccountabilityRepository accountabilityRepository,
            CityRepository cityRepository,
            WardRepository wardRepository,
            CategoryRepository categoryRepository
    ) {
        this.accountabilityRepository = accountabilityRepository;
        this.cityRepository = cityRepository;
        this.wardRepository = wardRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public PublicAccountabilityResponse getPublicAccountability(AccountabilityFilter filter) {
        // Resolve default city (Ahmedabad) if not explicitly specified
        UUID effectiveCityId = filter.cityId();
        String cityName = null;

        if (effectiveCityId != null) {
            cityName = cityRepository.findById(effectiveCityId).map(CityEntity::getName).orElse(null);
        } else {
            Optional<CityEntity> ahmedabad = cityRepository.findByNameAndState("Ahmedabad", "Gujarat");
            if (ahmedabad.isPresent()) {
                effectiveCityId = ahmedabad.get().getId();
                cityName = ahmedabad.get().getName();
            } else {
                List<CityEntity> allCities = cityRepository.findAllByOrderByNameAsc();
                if (!allCities.isEmpty()) {
                    effectiveCityId = allCities.get(0).getId();
                    cityName = allCities.get(0).getName();
                }
            }
        }

        // Ward name
        String wardName = null;
        if (filter.wardId() != null) {
            wardName = wardRepository.findById(filter.wardId()).map(w -> w.getWardName()).orElse(null);
        }

        // Category name
        String categoryName = null;
        if (filter.categoryId() != null) {
            categoryName = categoryRepository.findById(filter.categoryId()).map(c -> c.getName()).orElse(null);
        }

        // Date range
        String range = filter.range() != null ? filter.range().toLowerCase() : "30d";
        Instant since = null;
        Instant now = Instant.now();
        if ("7d".equals(range)) {
            since = now.minus(7, ChronoUnit.DAYS);
        } else if ("30d".equals(range)) {
            since = now.minus(30, ChronoUnit.DAYS);
        } else if ("90d".equals(range)) {
            since = now.minus(90, ChronoUnit.DAYS);
        } else {
            // "all"
            range = "all";
            since = null;
        }

        // Compute metrics
        AccountabilitySummaryDto summary = accountabilityRepository.getSummary(
                effectiveCityId,
                filter.wardId(),
                filter.categoryId(),
                since
        );

        StatusBreakdownDto statusBreakdown = accountabilityRepository.getStatusBreakdown(summary);

        PriorityBreakdownDto priorityBreakdown = accountabilityRepository.getPriorityBreakdown(
                effectiveCityId,
                filter.wardId(),
                filter.categoryId(),
                since
        );

        List<CategoryBreakdownDto> categoryBreakdown = accountabilityRepository.getCategoryBreakdown(
                effectiveCityId,
                filter.wardId(),
                filter.categoryId(),
                since
        );

        List<WardBreakdownDto> wardBreakdown = accountabilityRepository.getWardBreakdown(
                effectiveCityId,
                filter.wardId(),
                filter.categoryId(),
                since
        );

        AgingBreakdownDto agingBreakdown = accountabilityRepository.getAgingBreakdown(
                effectiveCityId,
                filter.wardId(),
                filter.categoryId(),
                since
        );

        VerificationSummaryDto verificationSummary = accountabilityRepository.getVerificationSummary(summary);

        ResponsibilitySummaryDto responsibilitySummary = accountabilityRepository.getResponsibilitySummary(
                effectiveCityId,
                filter.wardId(),
                filter.categoryId(),
                since
        );

        List<TrendPointDto> trend = accountabilityRepository.getTrend(
                effectiveCityId,
                filter.wardId(),
                filter.categoryId(),
                since,
                range
        );

        return new PublicAccountabilityResponse(
                effectiveCityId,
                cityName,
                filter.wardId(),
                wardName,
                filter.categoryId(),
                categoryName,
                range,
                summary,
                statusBreakdown,
                priorityBreakdown,
                categoryBreakdown,
                wardBreakdown,
                agingBreakdown,
                verificationSummary,
                responsibilitySummary,
                trend,
                now
        );
    }
}
