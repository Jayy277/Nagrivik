package org.nagrivic.modules.accountability.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.nagrivic.modules.accountability.dto.*;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Repository
public class PublicAccountabilityRepositoryImpl implements PublicAccountabilityRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final CategoryRepository categoryRepository;
    private final WardRepository wardRepository;

    public PublicAccountabilityRepositoryImpl(
            CategoryRepository categoryRepository,
            WardRepository wardRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.wardRepository = wardRepository;
    }

    private void appendFilters(
            StringBuilder jpql,
            Map<String, Object> params,
            String alias,
            UUID cityId,
            UUID wardId,
            UUID categoryId,
            Instant since
    ) {
        jpql.append(" AND ").append(alias).append(".moderationStatus <> org.nagrivic.modules.moderation.model.ModerationStatus.HIDDEN");
        jpql.append(" AND ").append(alias).append(".duplicateOf IS NULL");

        if (cityId != null) {
            jpql.append(" AND ").append(alias).append(".city.id = :cityId");
            params.put("cityId", cityId);
        }
        if (wardId != null) {
            jpql.append(" AND ").append(alias).append(".ward.id = :wardId");
            params.put("wardId", wardId);
        }
        if (categoryId != null) {
            jpql.append(" AND ").append(alias).append(".category.id = :categoryId");
            params.put("categoryId", categoryId);
        }
        if (since != null) {
            jpql.append(" AND ").append(alias).append(".createdAt >= :since");
            params.put("since", since);
        }
    }

    private long toLong(Object val) {
        if (val instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    @Override
    public AccountabilitySummaryDto getSummary(UUID cityId, UUID wardId, UUID categoryId, Instant since) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "SELECT " +
                        "COUNT(i), " +
                        "SUM(CASE WHEN i.status IN (org.nagrivic.modules.issues.model.IssueStatus.REPORTED, org.nagrivic.modules.issues.model.IssueStatus.VERIFIED, org.nagrivic.modules.issues.model.IssueStatus.ACKNOWLEDGED, org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS, org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED) THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.REPORTED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.VERIFIED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.ACKNOWLEDGED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.RESOLVED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.CITIZEN_VERIFIED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN p.priorityLevel = org.nagrivic.modules.priority.model.PriorityLevel.HIGH THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN p.priorityLevel = org.nagrivic.modules.priority.model.PriorityLevel.CRITICAL THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.responsibilityStatus = org.nagrivic.modules.issues.model.ResponsibilityStatus.UNRESOLVED THEN 1L ELSE 0L END) " +
                        "FROM IssueEntity i LEFT JOIN i.priority p WHERE 1=1"
        );

        appendFilters(jpql, params, "i", cityId, wardId, categoryId, since);

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        Object single = query.getSingleResult();
        Object[] row;
        if (single instanceof Object[] arr) {
            row = arr;
        } else {
            row = new Object[]{single};
        }

        long total = toLong(row[0]);
        long actionable = toLong(row[1]);
        long reported = toLong(row[2]);
        long verified = toLong(row[3]);
        long acknowledged = toLong(row[4]);
        long inProgress = toLong(row[5]);
        long resolved = toLong(row[6]);
        long citizenVerified = toLong(row[7]);
        long notFixed = toLong(row[8]);
        long high = toLong(row[9]);
        long critical = toLong(row[10]);
        long unresolved = toLong(row[11]);

        return new AccountabilitySummaryDto(
                total,
                actionable,
                reported,
                verified,
                acknowledged,
                inProgress,
                resolved,
                citizenVerified,
                notFixed,
                high,
                critical,
                unresolved
        );
    }

    @Override
    public StatusBreakdownDto getStatusBreakdown(AccountabilitySummaryDto summary) {
        return new StatusBreakdownDto(
                summary.reportedCount(),
                summary.verifiedCount(),
                summary.acknowledgedCount(),
                summary.inProgressCount(),
                summary.resolvedCount(),
                summary.citizenVerifiedCount(),
                summary.notFixedCount()
        );
    }

    @Override
    public PriorityBreakdownDto getPriorityBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "SELECT " +
                        "SUM(CASE WHEN p.priorityLevel = org.nagrivic.modules.priority.model.PriorityLevel.LOW THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN p.priorityLevel = org.nagrivic.modules.priority.model.PriorityLevel.MEDIUM THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN p.priorityLevel = org.nagrivic.modules.priority.model.PriorityLevel.HIGH THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN p.priorityLevel = org.nagrivic.modules.priority.model.PriorityLevel.CRITICAL THEN 1L ELSE 0L END) " +
                        "FROM IssueEntity i LEFT JOIN i.priority p WHERE 1=1"
        );

        appendFilters(jpql, params, "i", cityId, wardId, categoryId, since);

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        Object single = query.getSingleResult();
        Object[] row = single instanceof Object[] arr ? arr : new Object[]{single};

        return new PriorityBreakdownDto(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3])
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CategoryBreakdownDto> getCategoryBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since) {
        List<CategoryEntity> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "SELECT i.category.id, " +
                        "COUNT(i), " +
                        "SUM(CASE WHEN i.status IN (org.nagrivic.modules.issues.model.IssueStatus.REPORTED, org.nagrivic.modules.issues.model.IssueStatus.VERIFIED, org.nagrivic.modules.issues.model.IssueStatus.ACKNOWLEDGED, org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS, org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED) THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.RESOLVED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.CITIZEN_VERIFIED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED THEN 1L ELSE 0L END) " +
                        "FROM IssueEntity i WHERE 1=1"
        );

        appendFilters(jpql, params, "i", cityId, wardId, categoryId, since);
        jpql.append(" GROUP BY i.category.id");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        Map<UUID, Object[]> map = new HashMap<>();
        for (Object[] r : rows) {
            map.put((UUID) r[0], r);
        }

        List<CategoryBreakdownDto> result = new ArrayList<>();
        for (CategoryEntity c : categories) {
            if (categoryId != null && !c.getId().equals(categoryId)) {
                continue;
            }
            Object[] r = map.get(c.getId());
            long total = r != null ? toLong(r[1]) : 0L;
            long openActionable = r != null ? toLong(r[2]) : 0L;
            long inProgress = r != null ? toLong(r[3]) : 0L;
            long resolved = r != null ? toLong(r[4]) : 0L;
            long citizenVerified = r != null ? toLong(r[5]) : 0L;
            long notFixed = r != null ? toLong(r[6]) : 0L;

            result.add(new CategoryBreakdownDto(
                    c.getId(),
                    c.getName(),
                    c.getSlug(),
                    total,
                    openActionable,
                    inProgress,
                    resolved,
                    citizenVerified,
                    notFixed
            ));
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<WardBreakdownDto> getWardBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since) {
        List<WardEntity> wards;
        if (cityId != null) {
            wards = wardRepository.findByCityId(cityId);
        } else {
            wards = wardRepository.findAllByOrderByWardNumberAsc();
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "SELECT i.ward.id, " +
                        "COUNT(i), " +
                        "SUM(CASE WHEN i.status IN (org.nagrivic.modules.issues.model.IssueStatus.REPORTED, org.nagrivic.modules.issues.model.IssueStatus.VERIFIED, org.nagrivic.modules.issues.model.IssueStatus.ACKNOWLEDGED, org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS, org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED) THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.RESOLVED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.CITIZEN_VERIFIED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN p.priorityLevel IN (org.nagrivic.modules.priority.model.PriorityLevel.HIGH, org.nagrivic.modules.priority.model.PriorityLevel.CRITICAL) THEN 1L ELSE 0L END) " +
                        "FROM IssueEntity i LEFT JOIN i.priority p WHERE i.ward IS NOT NULL"
        );

        appendFilters(jpql, params, "i", cityId, wardId, categoryId, since);
        jpql.append(" GROUP BY i.ward.id");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        Map<UUID, Object[]> map = new HashMap<>();
        for (Object[] r : rows) {
            map.put((UUID) r[0], r);
        }

        List<WardBreakdownDto> result = new ArrayList<>();
        for (WardEntity w : wards) {
            if (wardId != null && !w.getId().equals(wardId)) {
                continue;
            }
            Object[] r = map.get(w.getId());
            long total = r != null ? toLong(r[1]) : 0L;
            long openActionable = r != null ? toLong(r[2]) : 0L;
            long inProgress = r != null ? toLong(r[3]) : 0L;
            long resolved = r != null ? toLong(r[4]) : 0L;
            long citizenVerified = r != null ? toLong(r[5]) : 0L;
            long notFixed = r != null ? toLong(r[6]) : 0L;
            long highOrCritical = r != null ? toLong(r[7]) : 0L;

            result.add(new WardBreakdownDto(
                    w.getId(),
                    w.getWardName(),
                    w.getWardNumber(),
                    w.getWardCode(),
                    total,
                    openActionable,
                    inProgress,
                    resolved,
                    citizenVerified,
                    notFixed,
                    highOrCritical
            ));
        }

        return result;
    }

    @Override
    public AgingBreakdownDto getAgingBreakdown(UUID cityId, UUID wardId, UUID categoryId, Instant since) {
        Instant now = Instant.now();
        Instant d1 = now.minus(1, ChronoUnit.DAYS);
        Instant d7 = now.minus(7, ChronoUnit.DAYS);
        Instant d30 = now.minus(30, ChronoUnit.DAYS);
        Instant d90 = now.minus(90, ChronoUnit.DAYS);

        Map<String, Object> params = new HashMap<>();
        params.put("d1", d1);
        params.put("d7", d7);
        params.put("d30", d30);
        params.put("d90", d90);

        StringBuilder jpql = new StringBuilder(
                "SELECT " +
                        "SUM(CASE WHEN i.createdAt >= :d1 THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.createdAt >= :d7 AND i.createdAt < :d1 THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.createdAt >= :d30 AND i.createdAt < :d7 THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.createdAt >= :d90 AND i.createdAt < :d30 THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.createdAt < :d90 THEN 1L ELSE 0L END) " +
                        "FROM IssueEntity i WHERE 1=1"
        );

        appendFilters(jpql, params, "i", cityId, wardId, categoryId, since);
        jpql.append(" AND i.status IN (org.nagrivic.modules.issues.model.IssueStatus.REPORTED, org.nagrivic.modules.issues.model.IssueStatus.VERIFIED, org.nagrivic.modules.issues.model.IssueStatus.ACKNOWLEDGED, org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS, org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED)");

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        Object single = query.getSingleResult();
        Object[] row = single instanceof Object[] arr ? arr : new Object[]{single};

        return new AgingBreakdownDto(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3]),
                toLong(row[4])
        );
    }

    @Override
    public VerificationSummaryDto getVerificationSummary(AccountabilitySummaryDto summary) {
        long resolvedByAuthority = summary.resolvedCount() + summary.citizenVerifiedCount() + summary.notFixedCount();
        long citizenVerified = summary.citizenVerifiedCount();
        long citizenReportedNotFixed = summary.notFixedCount();
        long verificationPending = summary.resolvedCount();
        double verificationRate = resolvedByAuthority > 0
                ? Math.round(((double) citizenVerified / resolvedByAuthority) * 1000.0) / 10.0
                : 0.0;

        return new VerificationSummaryDto(
                resolvedByAuthority,
                citizenVerified,
                citizenReportedNotFixed,
                verificationPending,
                verificationRate
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponsibilitySummaryDto getResponsibilitySummary(UUID cityId, UUID wardId, UUID categoryId, Instant since) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "SELECT " +
                        "SUM(CASE WHEN i.responsibilityStatus = org.nagrivic.modules.issues.model.ResponsibilityStatus.RESOLVED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.responsibilityStatus = org.nagrivic.modules.issues.model.ResponsibilityStatus.UNRESOLVED THEN 1L ELSE 0L END) " +
                        "FROM IssueEntity i WHERE 1=1"
        );

        appendFilters(jpql, params, "i", cityId, wardId, categoryId, since);

        Query query = entityManager.createQuery(jpql.toString());
        params.forEach(query::setParameter);

        Object single = query.getSingleResult();
        Object[] row = single instanceof Object[] arr ? arr : new Object[]{single};

        long resolved = toLong(row[0]);
        long unresolved = toLong(row[1]);
        long total = resolved + unresolved;
        double coverage = total > 0 ? Math.round(((double) resolved / total) * 1000.0) / 10.0 : 0.0;

        // Department breakdown
        Map<String, Object> deptParams = new HashMap<>();
        StringBuilder deptJpql = new StringBuilder(
                "SELECT i.department.id, i.department.name, i.department.code, " +
                        "COUNT(i), " +
                        "SUM(CASE WHEN i.status IN (org.nagrivic.modules.issues.model.IssueStatus.REPORTED, org.nagrivic.modules.issues.model.IssueStatus.VERIFIED, org.nagrivic.modules.issues.model.IssueStatus.ACKNOWLEDGED, org.nagrivic.modules.issues.model.IssueStatus.IN_PROGRESS, org.nagrivic.modules.issues.model.IssueStatus.NOT_FIXED) THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.RESOLVED THEN 1L ELSE 0L END), " +
                        "SUM(CASE WHEN i.status = org.nagrivic.modules.issues.model.IssueStatus.CITIZEN_VERIFIED THEN 1L ELSE 0L END) " +
                        "FROM IssueEntity i WHERE i.department IS NOT NULL"
        );

        appendFilters(deptJpql, deptParams, "i", cityId, wardId, categoryId, since);
        deptJpql.append(" GROUP BY i.department.id, i.department.name, i.department.code ORDER BY i.department.name ASC");

        Query deptQuery = entityManager.createQuery(deptJpql.toString());
        deptParams.forEach(deptQuery::setParameter);

        List<Object[]> deptRows = deptQuery.getResultList();
        List<DepartmentBreakdownDto> departments = new ArrayList<>();
        for (Object[] r : deptRows) {
            departments.add(new DepartmentBreakdownDto(
                    (UUID) r[0],
                    (String) r[1],
                    (String) r[2],
                    toLong(r[3]),
                    toLong(r[4]),
                    toLong(r[5]),
                    toLong(r[6])
            ));
        }

        return new ResponsibilitySummaryDto(resolved, unresolved, coverage, departments);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TrendPointDto> getTrend(UUID cityId, UUID wardId, UUID categoryId, Instant since, String range) {
        Instant effectiveSince = since;
        if (effectiveSince == null) {
            effectiveSince = Instant.now().minus(30, ChronoUnit.DAYS);
        }

        LocalDate startDate = effectiveSince.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);

        // Initialize daily buckets
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, long[]> bucketMap = new LinkedHashMap<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            bucketMap.put(curr.format(dtf), new long[3]); // [reported, resolved, citizenVerified]
            curr = curr.plusDays(1);
        }

        // 1. Reported counts
        Map<String, Object> repParams = new HashMap<>();
        StringBuilder repJpql = new StringBuilder("SELECT i.createdAt FROM IssueEntity i WHERE 1=1");
        appendFilters(repJpql, repParams, "i", cityId, wardId, categoryId, effectiveSince);

        Query repQuery = entityManager.createQuery(repJpql.toString());
        repParams.forEach(repQuery::setParameter);

        List<Instant> createdDates = repQuery.getResultList();
        for (Instant inst : createdDates) {
            if (inst != null) {
                String dStr = inst.atZone(ZoneOffset.UTC).toLocalDate().format(dtf);
                long[] counts = bucketMap.get(dStr);
                if (counts != null) {
                    counts[0]++;
                }
            }
        }

        // 2. Resolved & Citizen Verified counts from status_history
        Map<String, Object> histParams = new HashMap<>();
        StringBuilder histJpql = new StringBuilder(
                "SELECT sh.createdAt, sh.toStatus FROM StatusHistoryEntity sh JOIN sh.issue i WHERE 1=1"
        );
        appendFilters(histJpql, histParams, "i", cityId, wardId, categoryId, null);
        histJpql.append(" AND sh.createdAt >= :histSince");
        histParams.put("histSince", effectiveSince);
        histJpql.append(" AND sh.toStatus IN (org.nagrivic.modules.issues.model.IssueStatus.RESOLVED, org.nagrivic.modules.issues.model.IssueStatus.CITIZEN_VERIFIED)");

        Query histQuery = entityManager.createQuery(histJpql.toString());
        histParams.forEach(histQuery::setParameter);

        List<Object[]> histRows = histQuery.getResultList();
        for (Object[] r : histRows) {
            Instant inst = (Instant) r[0];
            org.nagrivic.modules.issues.model.IssueStatus st = (org.nagrivic.modules.issues.model.IssueStatus) r[1];
            if (inst != null) {
                String dStr = inst.atZone(ZoneOffset.UTC).toLocalDate().format(dtf);
                long[] counts = bucketMap.get(dStr);
                if (counts != null) {
                    if (st == org.nagrivic.modules.issues.model.IssueStatus.RESOLVED) {
                        counts[1]++;
                    } else if (st == org.nagrivic.modules.issues.model.IssueStatus.CITIZEN_VERIFIED) {
                        counts[2]++;
                    }
                }
            }
        }

        List<TrendPointDto> points = new ArrayList<>();
        bucketMap.forEach((dateStr, counts) -> {
            points.add(new TrendPointDto(dateStr, counts[0], counts[1], counts[2]));
        });

        return points;
    }
}
