package org.nagrivic.modules.authorities.entity;

import jakarta.persistence.*;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authority_assignments")
public class AuthorityAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "civic_body_id")
    private CivicBodyEntity civicBody;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private CityEntity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id")
    private WardEntity ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AuthorityAssignmentEntity() {
    }

    public AuthorityAssignmentEntity(
            UserEntity user,
            CivicBodyEntity civicBody,
            CityEntity city,
            WardEntity ward,
            DepartmentEntity department,
            String designation
    ) {
        this.user = user;
        this.civicBody = civicBody;
        this.city = city;
        this.ward = ward;
        this.department = department;
        this.designation = designation;
        this.active = true;
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public CivicBodyEntity getCivicBody() {
        return civicBody;
    }

    public void setCivicBody(CivicBodyEntity civicBody) {
        this.civicBody = civicBody;
    }

    public CityEntity getCity() {
        return city;
    }

    public void setCity(CityEntity city) {
        this.city = city;
    }

    public WardEntity getWard() {
        return ward;
    }

    public void setWard(WardEntity ward) {
        this.ward = ward;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
