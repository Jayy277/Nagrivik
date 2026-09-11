package org.nagrivic.modules.issues.service;

import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public IssueService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            LocationRepository locationRepository
    ) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
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
