package org.nagrivic.modules.locations.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "locations")
public class LocationEntity {

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "location_point", nullable = false)
    private Point locationPoint;

    @Column(name = "accuracy_meters", precision = 6, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LocationEntity() {
    }

    public LocationEntity(Point locationPoint, BigDecimal accuracyMeters) {
        validatePoint(locationPoint);
        validateAccuracy(accuracyMeters);
        this.locationPoint = locationPoint;
        this.accuracyMeters = accuracyMeters;
    }

    public LocationEntity(double latitude, double longitude, BigDecimal accuracyMeters) {
        this(createPoint(latitude, longitude), accuracyMeters);
    }

    public static Point createPoint(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees. Received: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees. Received: " + longitude);
        }
        // PostGIS convention: coordinate is (X=longitude, Y=latitude)
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);
        return point;
    }

    private static void validatePoint(Point point) {
        if (point == null) {
            throw new IllegalArgumentException("Location point cannot be null");
        }
        double longitude = point.getX();
        double latitude = point.getY();
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees. Received: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees. Received: " + longitude);
        }
    }

    private static void validateAccuracy(BigDecimal accuracyMeters) {
        if (accuracyMeters != null && accuracyMeters.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Accuracy meters cannot be negative. Received: " + accuracyMeters);
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Point getLocationPoint() {
        return locationPoint;
    }

    public void setLocationPoint(Point locationPoint) {
        validatePoint(locationPoint);
        this.locationPoint = locationPoint;
    }

    public Double getLatitude() {
        return locationPoint != null ? locationPoint.getY() : null;
    }

    public Double getLongitude() {
        return locationPoint != null ? locationPoint.getX() : null;
    }

    public BigDecimal getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(BigDecimal accuracyMeters) {
        validateAccuracy(accuracyMeters);
        this.accuracyMeters = accuracyMeters;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
