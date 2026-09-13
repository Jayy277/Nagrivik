package org.nagrivic.modules.civicgeography.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cities_name_state", columnNames = {"name", "state"})
})
public class CityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode = "IN";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "civic_body_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private CivicBodyEntity civicBody;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CityEntity() {
    }

    public CityEntity(String name, String state, CivicBodyEntity civicBody) {
        this.name = name;
        this.state = state;
        this.countryCode = "IN";
        this.civicBody = civicBody;
    }

    public CityEntity(String name, String state, String countryCode, CivicBodyEntity civicBody) {
        this.name = name;
        this.state = state;
        this.countryCode = (countryCode != null && !countryCode.trim().isEmpty()) ? countryCode.trim().toUpperCase() : "IN";
        this.civicBody = civicBody;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public CivicBodyEntity getCivicBody() {
        return civicBody;
    }

    public void setCivicBody(CivicBodyEntity civicBody) {
        this.civicBody = civicBody;
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
