package org.nagrivic.modules.civicgeography.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WardSpatialRepositoryImpl implements WardSpatialRepository {

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    @PersistenceContext
    private EntityManager entityManager;

    private final WardRepository wardRepository;

    public WardSpatialRepositoryImpl(WardRepository wardRepository) {
        this.wardRepository = wardRepository;
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
    public Optional<WardEntity> findWardContainingPoint(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Invalid coordinates: lat=" + latitude + ", lon=" + longitude);
        }

        if (isPostgreSQL()) {
            return findWardContainingPointPostgres(latitude, longitude);
        } else {
            return findWardContainingPointFallback(latitude, longitude);
        }
    }

    private Optional<WardEntity> findWardContainingPointPostgres(double latitude, double longitude) {
        String sql = "SELECT w.id FROM wards w " +
                "WHERE w.is_active = true " +
                "  AND w.boundary_geometry IS NOT NULL " +
                "  AND ST_Contains(w.boundary_geometry, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) " +
                "LIMIT 1";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("lon", longitude);
        query.setParameter("lat", latitude);

        List<?> results = query.getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }

        UUID wardId = toUUID(results.get(0));
        return wardId != null ? wardRepository.findById(wardId) : Optional.empty();
    }

    private Optional<WardEntity> findWardContainingPointFallback(double latitude, double longitude) {
        // Fallback for H2 / unit test environment
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);

        List<WardEntity> allWards = wardRepository.findAll();
        for (WardEntity ward : allWards) {
            if (ward.isActive() && ward.getBoundaryGeometry() != null) {
                if (ward.getBoundaryGeometry().contains(point)) {
                    return Optional.of(ward);
                }
            }
        }
        return Optional.empty();
    }

    private static UUID toUUID(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof UUID u) {
            return u;
        }
        if (obj instanceof byte[] bytes && bytes.length == 16) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
            return new UUID(bb.getLong(), bb.getLong());
        }
        try {
            return UUID.fromString(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
