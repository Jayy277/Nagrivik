package org.nagrivic.modules.issues.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.nagrivic.modules.issues.dto.IssueDiscoveryFilter;
import org.nagrivic.modules.issues.dto.IssueDiscoveryItem;
import org.nagrivic.modules.issues.model.IssueDiscoverySort;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Repository
public class IssueDiscoveryRepositoryImpl implements IssueDiscoveryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final LocationRepository locationRepository;

    public IssueDiscoveryRepositoryImpl(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    private boolean isPostgreSQL() {
        try {
            Session session = entityManager.unwrap(Session.class);
            return session.doReturningWork(connection -> {
                String dbProduct = connection.getMetaData().getDatabaseProductName();
                return dbProduct != null && dbProduct.toLowerCase().contains("postgres");
            });
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Page<IssueDiscoveryItem> discoverIssues(IssueDiscoveryFilter filter, Pageable pageable) {
        if (isPostgreSQL()) {
            return discoverIssuesPostgres(filter, pageable);
        } else {
            return discoverIssuesFallback(filter, pageable);
        }
    }

    // ==========================================
    // POSTGRESQL NATIVE DISCOVERY (PRODUCTION)
    // ==========================================

    @SuppressWarnings("unchecked")
    private Page<IssueDiscoveryItem> discoverIssuesPostgres(IssueDiscoveryFilter filter, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder whereClause = new StringBuilder(" WHERE i.moderation_status <> 'HIDDEN'");

        if (!filter.includeDuplicates()) {
            whereClause.append(" AND i.duplicate_of_issue_id IS NULL");
        }

        if (filter.categoryId() != null) {
            whereClause.append(" AND i.category_id = :categoryId");
            params.put("categoryId", filter.categoryId());
        }

        if (filter.status() != null) {
            whereClause.append(" AND i.status = :status");
            params.put("status", filter.status().name());
        }

        if (filter.priority() != null) {
            whereClause.append(" AND p.priority_level = :priorityLevel");
            params.put("priorityLevel", filter.priority().name());
        }

        if (filter.cityId() != null) {
            whereClause.append(" AND i.city_id = :cityId");
            params.put("cityId", filter.cityId());
        }

        if (filter.wardId() != null) {
            whereClause.append(" AND i.ward_id = :wardId");
            params.put("wardId", filter.wardId());
        }

        if (filter.civicBodyId() != null) {
            whereClause.append(" AND i.civic_body_id = :civicBodyId");
            params.put("civicBodyId", filter.civicBodyId());
        }

        if (filter.departmentId() != null) {
            whereClause.append(" AND i.department_id = :departmentId");
            params.put("departmentId", filter.departmentId());
        }

        if (filter.reportedBy() != null) {
            whereClause.append(" AND i.reported_by = :reportedBy");
            params.put("reportedBy", filter.reportedBy());
        }

        if (filter.q() != null && !filter.q().trim().isEmpty()) {
            whereClause.append(" AND i.search_vector @@ plainto_tsquery('english', :queryText)");
            params.put("queryText", filter.q().trim());
        }

        boolean hasGeo = filter.hasGeographicSearch();
        if (hasGeo) {
            double radiusMeters = filter.radiusMeters() != null ? filter.radiusMeters() : 50000.0;
            double radiusDegrees = (radiusMeters / 111320.0) * 1.5;
            whereClause.append(" AND l.location_point && ST_Expand(ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326), :radiusDegrees)")
                    .append(" AND ST_DWithin(l.location_point::geography, ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326)::geography, :radiusMeters)");
            params.put("targetLon", filter.longitude());
            params.put("targetLat", filter.latitude());
            params.put("radiusDegrees", radiusDegrees);
            params.put("radiusMeters", radiusMeters);
        }

        // 1. Total Count Query
        String countSql = "SELECT COUNT(i.id) FROM issues i " +
                "JOIN locations l ON i.location_id = l.id " +
                "LEFT JOIN issue_priorities p ON p.issue_id = i.id" +
                whereClause;

        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);
        Number totalCountNum = (Number) countQuery.getSingleResult();
        long totalElements = totalCountNum != null ? totalCountNum.longValue() : 0L;

        if (totalElements == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 2. Data Query
        StringBuilder selectClause = new StringBuilder("SELECT CAST(i.id AS VARCHAR) AS issue_id");
        if (hasGeo) {
            selectClause.append(", ROUND(CAST(ST_Distance(l.location_point::geography, ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326)::geography) AS numeric), 2) AS distance_meters");
        } else {
            selectClause.append(", NULL AS distance_meters");
        }

        selectClause.append(" FROM issues i ")
                .append("JOIN locations l ON i.location_id = l.id ")
                .append("LEFT JOIN issue_priorities p ON p.issue_id = i.id")
                .append(whereClause);

        // Order by
        IssueDiscoverySort sortMode = filter.sort() != null ? filter.sort() : IssueDiscoverySort.NEWEST;
        switch (sortMode) {
            case OLDEST -> selectClause.append(" ORDER BY i.created_at ASC");
            case PRIORITY -> selectClause.append(" ORDER BY COALESCE(p.score, 0) DESC, i.created_at ASC");
            case MOST_SUPPORTED -> selectClause.append(" ORDER BY (SELECT COUNT(*) FROM supports s WHERE s.issue_id = i.id) DESC, i.created_at DESC");
            case NEAREST -> {
                if (hasGeo) {
                    selectClause.append(" ORDER BY distance_meters ASC, i.created_at DESC");
                } else {
                    selectClause.append(" ORDER BY i.created_at DESC");
                }
            }
            default -> selectClause.append(" ORDER BY i.created_at DESC");
        }

        Query dataQuery = entityManager.createNativeQuery(selectClause.toString());
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<Object[]> rows = dataQuery.getResultList();
        List<IssueDiscoveryItem> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            UUID id = UUID.fromString(row[0].toString());
            Double distance = null;
            if (row[1] != null) {
                distance = ((Number) row[1]).doubleValue();
            }
            items.add(new IssueDiscoveryItem(id, distance));
        }

        return new PageImpl<>(items, pageable, totalElements);
    }

    // ==========================================
    // H2 / UNIT TEST FALLBACK DISCOVERY
    // ==========================================

    @SuppressWarnings("unchecked")
    private Page<IssueDiscoveryItem> discoverIssuesFallback(IssueDiscoveryFilter filter, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder sql = new StringBuilder(
                "SELECT i.id, l.id, i.created_at, COALESCE(p.score, 0) as p_score, " +
                "(SELECT COUNT(*) FROM supports s WHERE s.issue_id = i.id) as s_count " +
                "FROM issues i " +
                "JOIN locations l ON i.location_id = l.id " +
                "LEFT JOIN issue_priorities p ON p.issue_id = i.id " +
                "WHERE i.moderation_status <> 'HIDDEN'"
        );

        if (!filter.includeDuplicates()) {
            sql.append(" AND i.duplicate_of_issue_id IS NULL");
        }

        if (filter.categoryId() != null) {
            sql.append(" AND i.category_id = :categoryId");
            params.put("categoryId", filter.categoryId());
        }

        if (filter.status() != null) {
            sql.append(" AND i.status = :status");
            params.put("status", filter.status().name());
        }

        if (filter.priority() != null) {
            sql.append(" AND p.priority_level = :priorityLevel");
            params.put("priorityLevel", filter.priority().name());
        }

        if (filter.cityId() != null) {
            sql.append(" AND i.city_id = :cityId");
            params.put("cityId", filter.cityId());
        }

        if (filter.wardId() != null) {
            sql.append(" AND i.ward_id = :wardId");
            params.put("wardId", filter.wardId());
        }

        if (filter.civicBodyId() != null) {
            sql.append(" AND i.civic_body_id = :civicBodyId");
            params.put("civicBodyId", filter.civicBodyId());
        }

        if (filter.departmentId() != null) {
            sql.append(" AND i.department_id = :departmentId");
            params.put("departmentId", filter.departmentId());
        }

        if (filter.reportedBy() != null) {
            sql.append(" AND i.reported_by = :reportedBy");
            params.put("reportedBy", filter.reportedBy());
        }

        if (filter.q() != null && !filter.q().trim().isEmpty()) {
            sql.append(" AND (LOWER(i.title) LIKE :queryLike OR LOWER(i.description) LIKE :queryLike)");
            params.put("queryLike", "%" + filter.q().trim().toLowerCase() + "%");
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        List<Object[]> rows = query.getResultList();
        boolean hasGeo = filter.hasGeographicSearch();
        double radiusMeters = filter.radiusMeters() != null ? filter.radiusMeters() : 50000.0;

        List<FallbackRow> matchedRows = new ArrayList<>();
        for (Object[] row : rows) {
            UUID issueId = toUUID(row[0]);
            UUID locationId = toUUID(row[1]);
            Instant createdAt = null;
            if (row[2] instanceof Instant inst) {
                createdAt = inst;
            } else if (row[2] instanceof java.time.OffsetDateTime odt) {
                createdAt = odt.toInstant();
            } else if (row[2] instanceof java.sql.Timestamp ts) {
                createdAt = ts.toInstant();
            } else if (row[2] instanceof Date d) {
                createdAt = d.toInstant();
            }
            int priorityScore = ((Number) row[3]).intValue();
            long supportCount = ((Number) row[4]).longValue();

            Double distance = null;
            if (hasGeo) {
                Optional<LocationEntity> locOpt = locationRepository.findById(locationId);
                if (locOpt.isPresent()) {
                    LocationEntity loc = locOpt.get();
                    distance = calculateHaversineDistance(filter.latitude(), filter.longitude(), loc.getLatitude(), loc.getLongitude());
                    if (distance > radiusMeters) {
                        continue; // beyond radius
                    }
                    distance = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP).doubleValue();
                } else {
                    continue;
                }
            }

            matchedRows.add(new FallbackRow(issueId, distance, createdAt, priorityScore, supportCount));
        }

        // Sort rows
        IssueDiscoverySort sortMode = filter.sort() != null ? filter.sort() : IssueDiscoverySort.NEWEST;
        switch (sortMode) {
            case OLDEST -> matchedRows.sort(Comparator.comparing(FallbackRow::createdAt, Comparator.nullsLast(Comparator.naturalOrder())));
            case PRIORITY -> matchedRows.sort((a, b) -> {
                int scoreComp = Integer.compare(b.priorityScore(), a.priorityScore());
                if (scoreComp != 0) return scoreComp;
                if (a.createdAt() == null || b.createdAt() == null) return 0;
                return a.createdAt().compareTo(b.createdAt()); // Older first tie-breaker
            });
            case MOST_SUPPORTED -> matchedRows.sort((a, b) -> {
                int suppComp = Long.compare(b.supportCount(), a.supportCount());
                if (suppComp != 0) return suppComp;
                if (a.createdAt() == null || b.createdAt() == null) return 0;
                return b.createdAt().compareTo(a.createdAt());
            });
            case NEAREST -> {
                if (hasGeo) {
                    matchedRows.sort(Comparator.comparing(FallbackRow::distance, Comparator.nullsLast(Comparator.naturalOrder())));
                } else {
                    matchedRows.sort(Comparator.comparing(FallbackRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
                }
            }
            default -> matchedRows.sort(Comparator.comparing(FallbackRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        long totalElements = matchedRows.size();
        int fromIndex = (int) pageable.getOffset();
        if (fromIndex >= totalElements) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
        }
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), (int) totalElements);
        List<FallbackRow> pagedRows = matchedRows.subList(fromIndex, toIndex);

        List<IssueDiscoveryItem> items = pagedRows.stream()
                .map(r -> new IssueDiscoveryItem(r.issueId(), r.distance()))
                .toList();

        return new PageImpl<>(items, pageable, totalElements);
    }

    private record FallbackRow(
            UUID issueId,
            Double distance,
            Instant createdAt,
            int priorityScore,
            long supportCount
    ) {}

    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Earth mean radius in meters
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return R * c;
    }

    private static UUID toUUID(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof UUID u) {
            return u;
        }
        if (obj instanceof byte[] bytes) {
            if (bytes.length == 16) {
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
                long high = bb.getLong();
                long low = bb.getLong();
                return new UUID(high, low);
            }
        }
        return UUID.fromString(obj.toString());
    }
}
