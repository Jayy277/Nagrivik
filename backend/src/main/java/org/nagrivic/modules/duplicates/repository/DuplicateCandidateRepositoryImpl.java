package org.nagrivic.modules.duplicates.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DuplicateCandidateRepositoryImpl implements DuplicateCandidateRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final LocationRepository locationRepository;

    public DuplicateCandidateRepositoryImpl(LocationRepository locationRepository) {
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
    @SuppressWarnings("unchecked")
    public List<DuplicateCandidateProjection> findPotentialDuplicates(
            UUID categoryId,
            double targetLon,
            double targetLat,
            double radiusMeters,
            UUID excludeIssueId,
            int limit
    ) {
        if (isPostgreSQL()) {
            return findPotentialDuplicatesPostgres(categoryId, targetLon, targetLat, radiusMeters, excludeIssueId, limit);
        } else {
            return findPotentialDuplicatesFallback(categoryId, targetLon, targetLat, radiusMeters, excludeIssueId, limit);
        }
    }

    @SuppressWarnings("unchecked")
    private List<DuplicateCandidateProjection> findPotentialDuplicatesPostgres(
            UUID categoryId,
            double targetLon,
            double targetLat,
            double radiusMeters,
            UUID excludeIssueId,
            int limit
    ) {
        // PostGIS bounding box degree expansion for GiST spatial index optimization (1 deg latitude ~ 111,320m)
        double radiusDegrees = (radiusMeters / 111320.0) * 1.5;

        String sql = """
            SELECT CAST(i.id AS VARCHAR) AS issue_id,
                   i.title AS title,
                   CAST(c.id AS VARCHAR) AS category_id,
                   c.name AS category_name,
                   c.slug AS category_slug,
                   i.status AS status,
                   ROUND(CAST(ST_Distance(l.location_point::geography, ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326)::geography) AS numeric), 2) AS distance_meters
            FROM issues i
            JOIN locations l ON i.location_id = l.id
            JOIN categories c ON i.category_id = c.id
            WHERE i.category_id = :categoryId
              AND i.duplicate_of_issue_id IS NULL
              AND (:excludeIssueId IS NULL OR i.id <> :excludeIssueId)
              AND l.location_point && ST_Expand(ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326), :radiusDegrees)
              AND ST_DWithin(l.location_point::geography, ST_SetSRID(ST_MakePoint(:targetLon, :targetLat), 4326)::geography, :radiusMeters)
            ORDER BY distance_meters ASC
            LIMIT :limit
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categoryId", categoryId);
        query.setParameter("targetLon", targetLon);
        query.setParameter("targetLat", targetLat);
        query.setParameter("radiusDegrees", radiusDegrees);
        query.setParameter("radiusMeters", radiusMeters);
        query.setParameter("excludeIssueId", excludeIssueId);
        query.setParameter("limit", limit);

        List<Object[]> rows = query.getResultList();
        List<DuplicateCandidateProjection> candidates = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            UUID issueId = UUID.fromString(row[0].toString());
            String title = (String) row[1];
            UUID catId = UUID.fromString(row[2].toString());
            String catName = (String) row[3];
            String catSlug = (String) row[4];
            String status = (String) row[5];
            Double distanceMeters = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;

            candidates.add(new DuplicateCandidateRecord(
                    issueId, title, catId, catName, catSlug, status, distanceMeters
            ));
        }

        return candidates;
    }

    @SuppressWarnings("unchecked")
    private List<DuplicateCandidateProjection> findPotentialDuplicatesFallback(
            UUID categoryId,
            double targetLon,
            double targetLat,
            double radiusMeters,
            UUID excludeIssueId,
            int limit
    ) {
        String sql = """
            SELECT i.id,
                   i.title,
                   c.id,
                   c.name,
                   c.slug,
                   i.status,
                   l.id
            FROM issues i
            JOIN locations l ON i.location_id = l.id
            JOIN categories c ON i.category_id = c.id
            WHERE i.category_id = :categoryId
              AND i.duplicate_of_issue_id IS NULL
              AND (:excludeIssueId IS NULL OR i.id <> :excludeIssueId)
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categoryId", categoryId);
        query.setParameter("excludeIssueId", excludeIssueId);

        List<Object[]> rows = query.getResultList();
        List<DuplicateCandidateRecord> candidatesWithDistance = new ArrayList<>();

        for (Object[] row : rows) {
            UUID issueId = toUUID(row[0]);
            String title = (String) row[1];
            UUID catId = toUUID(row[2]);
            String catName = (String) row[3];
            String catSlug = (String) row[4];
            String status = row[5] != null ? row[5].toString() : "REPORTED";
            UUID locationId = toUUID(row[6]);

            Optional<LocationEntity> locOpt = locationRepository.findById(locationId);
            if (locOpt.isPresent()) {
                LocationEntity loc = locOpt.get();
                double distance = calculateHaversineDistance(targetLat, targetLon, loc.getLatitude(), loc.getLongitude());
                if (distance <= radiusMeters) {
                    double roundedDistance = Math.round(distance * 100.0) / 100.0;
                    candidatesWithDistance.add(new DuplicateCandidateRecord(
                            issueId, title, catId, catName, catSlug, status, roundedDistance
                    ));
                }
            }
        }

        candidatesWithDistance.sort(Comparator.comparingDouble(DuplicateCandidateRecord::distanceMeters));
        if (candidatesWithDistance.size() > limit) {
            candidatesWithDistance = candidatesWithDistance.subList(0, limit);
        }

        return new ArrayList<>(candidatesWithDistance);
    }

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
