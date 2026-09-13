package org.nagrivic.modules.civicgeography.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.locationtech.jts.geom.MultiPolygon;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wards")
public class WardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private CityEntity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "civic_body_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private CivicBodyEntity civicBody;

    @Column(name = "ward_number", length = 50)
    private String wardNumber;

    @Column(name = "ward_name", nullable = false, length = 255)
    private String wardName;

    @Column(name = "ward_code", length = 50)
    private String wardCode;

    @Column(name = "boundary_geometry", columnDefinition = "geometry(MultiPolygon, 4326)")
    private MultiPolygon boundaryGeometry;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WardEntity() {
    }

    public WardEntity(CityEntity city, CivicBodyEntity civicBody, String wardNumber, String wardName) {
        this.city = city;
        this.civicBody = civicBody;
        this.wardNumber = wardNumber;
        this.wardName = wardName;
    }

    public WardEntity(CityEntity city, CivicBodyEntity civicBody, String wardNumber, String wardName, MultiPolygon boundaryGeometry) {
        this.city = city;
        this.civicBody = civicBody;
        this.wardNumber = wardNumber;
        this.wardName = wardName;
        this.boundaryGeometry = boundaryGeometry;
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

    public CityEntity getCity() {
        return city;
    }

    public void setCity(CityEntity city) {
        this.city = city;
    }

    public CivicBodyEntity getCivicBody() {
        return civicBody;
    }

    public void setCivicBody(CivicBodyEntity civicBody) {
        this.civicBody = civicBody;
    }

    public String getWardNumber() {
        return wardNumber;
    }

    public void setWardNumber(String wardNumber) {
        this.wardNumber = wardNumber;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public String getWardCode() {
        return wardCode;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public MultiPolygon getBoundaryGeometry() {
        return boundaryGeometry;
    }

    public void setBoundaryGeometry(MultiPolygon boundaryGeometry) {
        this.boundaryGeometry = boundaryGeometry;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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
