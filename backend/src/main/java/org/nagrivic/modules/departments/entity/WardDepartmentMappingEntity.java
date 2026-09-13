package org.nagrivic.modules.departments.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.civicgeography.entity.WardEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ward_department_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ward_dept", columnNames = {"ward_id", "department_id"})
})
public class WardDepartmentMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ward_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WardEntity ward;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DepartmentEntity department;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WardDepartmentMappingEntity() {
    }

    public WardDepartmentMappingEntity(WardEntity ward, DepartmentEntity department) {
        this.ward = ward;
        this.department = department;
    }

    public WardDepartmentMappingEntity(WardEntity ward, DepartmentEntity department, boolean isActive) {
        this.ward = ward;
        this.department = department;
        this.isActive = isActive;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
