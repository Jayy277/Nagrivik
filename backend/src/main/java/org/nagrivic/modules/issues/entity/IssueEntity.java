package org.nagrivic.modules.issues.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.priority.entity.IssuePriorityEntity;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PublicImpact;
import org.nagrivic.modules.priority.model.SafetyImpact;
import org.nagrivic.modules.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issues")
public class IssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private UserEntity reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private LocationEntity location;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IssueStatus status = IssueStatus.REPORTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 32)
    private org.nagrivic.modules.moderation.model.ModerationStatus moderationStatus = org.nagrivic.modules.moderation.model.ModerationStatus.VISIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_of_issue_id")
    private IssueEntity duplicateOf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "civic_body_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private CivicBodyEntity civicBody;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private CityEntity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private WardEntity ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private DepartmentEntity department;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsibility_status", nullable = false, length = 30)
    private ResponsibilityStatus responsibilityStatus = ResponsibilityStatus.UNRESOLVED;

    @Column(name = "responsibility_resolved_at")
    private Instant responsibilityResolvedAt;

    @Column(name = "responsibility_source", length = 255)
    private String responsibilitySource;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private IssueSeverity severity = IssueSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "public_impact", nullable = false, length = 30)
    private PublicImpact publicImpact = PublicImpact.LOW;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_impact", nullable = false, length = 30)
    private SafetyImpact safetyImpact = SafetyImpact.LOW;

    @OneToOne(mappedBy = "issue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IssuePriorityEntity priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public IssueEntity() {
    }

    public IssueEntity(UserEntity reporter, CategoryEntity category, LocationEntity location, String title, String description) {
        this.reporter = reporter;
        this.category = category;
        this.location = location;
        this.title = title;
        this.description = description;
        this.status = IssueStatus.REPORTED;
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
        if (this.status == null) {
            this.status = IssueStatus.REPORTED;
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

    public UserEntity getReporter() {
        return reporter;
    }

    public void setReporter(UserEntity reporter) {
        this.reporter = reporter;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public void setCategory(CategoryEntity category) {
        this.category = category;
    }

    public LocationEntity getLocation() {
        return location;
    }

    public void setLocation(LocationEntity location) {
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
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

    public IssueEntity getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(IssueEntity duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public boolean isDuplicate() {
        return duplicateOf != null;
    }

    public UUID getPrimaryIssueId() {
        return duplicateOf != null ? duplicateOf.getId() : null;
    }

    public Long getVersion() {
        return version;
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

    public ResponsibilityStatus getResponsibilityStatus() {
        return responsibilityStatus;
    }

    public void setResponsibilityStatus(ResponsibilityStatus responsibilityStatus) {
        this.responsibilityStatus = responsibilityStatus;
    }

    public Instant getResponsibilityResolvedAt() {
        return responsibilityResolvedAt;
    }

    public void setResponsibilityResolvedAt(Instant responsibilityResolvedAt) {
        this.responsibilityResolvedAt = responsibilityResolvedAt;
    }

    public String getResponsibilitySource() {
        return responsibilitySource;
    }

    public void setResponsibilitySource(String responsibilitySource) {
        this.responsibilitySource = responsibilitySource;
    }

    public void setResolvedResponsibility(
            CivicBodyEntity civicBody,
            CityEntity city,
            WardEntity ward,
            DepartmentEntity department,
            String source
    ) {
        this.civicBody = civicBody;
        this.city = city;
        this.ward = ward;
        this.department = department;
        this.responsibilityStatus = ResponsibilityStatus.RESOLVED;
        this.responsibilityResolvedAt = Instant.now();
        this.responsibilitySource = source;
    }

    public void setUnresolvedResponsibility(
            CivicBodyEntity civicBody,
            CityEntity city,
            WardEntity ward,
            DepartmentEntity department
    ) {
        this.civicBody = civicBody;
        this.city = city;
        this.ward = ward;
        this.department = department;
        this.responsibilityStatus = ResponsibilityStatus.UNRESOLVED;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IssueSeverity severity) {
        this.severity = severity != null ? severity : IssueSeverity.MEDIUM;
    }

    public PublicImpact getPublicImpact() {
        return publicImpact;
    }

    public void setPublicImpact(PublicImpact publicImpact) {
        this.publicImpact = publicImpact != null ? publicImpact : PublicImpact.LOW;
    }

    public SafetyImpact getSafetyImpact() {
        return safetyImpact;
    }

    public void setSafetyImpact(SafetyImpact safetyImpact) {
        this.safetyImpact = safetyImpact != null ? safetyImpact : SafetyImpact.LOW;
    }

    public IssuePriorityEntity getPriority() {
        return priority;
    }

    public void setPriority(IssuePriorityEntity priority) {
        this.priority = priority;
    }

    public org.nagrivic.modules.moderation.model.ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(org.nagrivic.modules.moderation.model.ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus != null ? moderationStatus : org.nagrivic.modules.moderation.model.ModerationStatus.VISIBLE;
    }
}
