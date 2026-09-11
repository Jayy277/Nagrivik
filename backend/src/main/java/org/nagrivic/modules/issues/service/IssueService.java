package org.nagrivic.modules.issues.service;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final MediaRepository mediaRepository;

    public IssueService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            LocationRepository locationRepository,
            MediaRepository mediaRepository
    ) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional
    public IssueEntity createIssue(UUID reportedBy, UUID categoryId, UUID locationId, String title, String description) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Issue title cannot be blank");
        }
        UserEntity reporter = userRepository.findById(reportedBy)
                .orElseThrow(() -> new IllegalArgumentException("Reporter user not found: " + reportedBy));

        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Cannot report an issue under an inactive category: " + category.getName());
        }

        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));

        IssueEntity issue = new IssueEntity(reporter, category, location, title.trim(), description);
        return issueRepository.save(issue);
    }

    @Transactional
    public IssueResponse createIssueFromRequest(CreateIssueRequest request) {
        IssueEntity issue = createIssue(
                request.reportedBy(),
                request.categoryId(),
                request.locationId(),
                request.title(),
                request.description()
        );
        return IssueResponse.fromEntity(issue);
    }

    public IssueResponse getIssueById(UUID id) {
        IssueEntity issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", id));
        List<MediaEntity> mediaList = mediaRepository.findByIssue_IdOrderByDisplayOrderAsc(id);
        return IssueResponse.fromEntity(issue, mediaList);
    }

    public Page<IssueResponse> findIssues(IssueStatus status, UUID categoryId, UUID reportedBy, Pageable pageable) {
        Specification<IssueEntity> spec = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("reporter", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
                root.fetch("location", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (reportedBy != null) {
                predicates.add(cb.equal(root.get("reporter").get("id"), reportedBy));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<IssueEntity> page = issueRepository.findAll(spec, pageable);
        return page.map(IssueResponse::fromEntity);
    }

    public Optional<IssueEntity> findById(UUID id) {
        return issueRepository.findById(id);
    }

    public List<IssueEntity> findByReportedBy(UUID reportedBy) {
        return issueRepository.findByReporter_Id(reportedBy);
    }

    public List<IssueEntity> findByCategory(UUID categoryId) {
        return issueRepository.findByCategory_Id(categoryId);
    }

    public List<IssueEntity> findByStatus(IssueStatus status) {
        return issueRepository.findByStatus(status);
    }
}
