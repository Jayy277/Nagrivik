package org.nagrivic.modules.issues;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IssueDomainTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IssueService issueService;

    private UserEntity testUser;
    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    @BeforeEach
    void setUp() {
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        testUser = new UserEntity("+919876543210", "Aarav Patel");
        testUser = userRepository.save(testUser);

        testCategory = new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1);
        testCategory = categoryRepository.save(testCategory);

        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @Test
    void shouldCreateAndPersistIssueWithLocationAndCategoryAndDefaultStatusReported() {
        IssueEntity issue = issueService.createIssue(
                testUser.getId(),
                testCategory.getId(),
                testLocation.getId(),
                "Large pothole near SG Highway",
                "Deep pothole in the left lane causing traffic slowdown."
        );

        assertNotNull(issue.getId());
        assertEquals("Large pothole near SG Highway", issue.getTitle());
        assertEquals("Deep pothole in the left lane causing traffic slowdown.", issue.getDescription());
        assertEquals(IssueStatus.REPORTED, issue.getStatus());
        assertEquals(testUser.getId(), issue.getReporter().getId());
        assertEquals(testCategory.getId(), issue.getCategory().getId());
        assertEquals("roads-potholes", issue.getCategory().getSlug());
        assertEquals(testLocation.getId(), issue.getLocation().getId());
        assertEquals(23.0225, issue.getLocation().getLatitude(), 0.0001);
        assertEquals(72.5714, issue.getLocation().getLongitude(), 0.0001);
        assertNotNull(issue.getCreatedAt());
        assertNotNull(issue.getUpdatedAt());

        // Verify IssueResponse DTO representation
        IssueResponse response = IssueResponse.fromEntity(issue);
        assertNotNull(response);
        assertEquals(issue.getId(), response.id());
        assertNotNull(response.location());
        assertEquals(23.0225, response.location().latitude(), 0.0001);
        assertEquals(72.5714, response.location().longitude(), 0.0001);
        assertEquals(new BigDecimal("5.00"), response.location().accuracyMeters());
    }

    @Test
    void shouldFindIssuesByCategory() {
        CategoryEntity waterCat = new CategoryEntity("Water", "water", "Water leaks", 4);
        waterCat = categoryRepository.save(waterCat);

        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Road Issue", "Desc");
        issueService.createIssue(testUser.getId(), waterCat.getId(), testLocation.getId(), "Water Pipe Leak", "Desc");

        List<IssueEntity> roadIssues = issueService.findByCategory(testCategory.getId());
        assertEquals(1, roadIssues.size());
        assertEquals("Road Issue", roadIssues.get(0).getTitle());

        List<IssueEntity> waterIssues = issueService.findByCategory(waterCat.getId());
        assertEquals(1, waterIssues.size());
        assertEquals("Water Pipe Leak", waterIssues.get(0).getTitle());
    }

    @Test
    void shouldFindIssuesByReporterId() {
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue 1", "Desc 1");
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue 2", "Desc 2");

        List<IssueEntity> userIssues = issueService.findByReportedBy(testUser.getId());
        assertEquals(2, userIssues.size());
    }

    @Test
    void shouldFindIssuesByStatus() {
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Pothole report", "Details");

        List<IssueEntity> reportedIssues = issueService.findByStatus(IssueStatus.REPORTED);
        assertFalse(reportedIssues.isEmpty());
        assertEquals("Pothole report", reportedIssues.get(0).getTitle());
    }

    @Test
    void shouldRejectIssueWithBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> {
            issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "   ", "Description");
        });
    }

    @Test
    void shouldRejectIssueWithNonExistentUser() {
        UUID nonExistentUserId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> {
            issueService.createIssue(nonExistentUserId, testCategory.getId(), testLocation.getId(), "Valid Title", "Description");
        });
    }

    @Test
    void shouldRejectIssueWithNonExistentCategory() {
        UUID nonExistentCategoryId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> {
            issueService.createIssue(testUser.getId(), nonExistentCategoryId, testLocation.getId(), "Valid Title", "Description");
        });
    }

    @Test
    void shouldRejectIssueWithNonExistentLocation() {
        UUID nonExistentLocationId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> {
            issueService.createIssue(testUser.getId(), testCategory.getId(), nonExistentLocationId, "Valid Title", "Description");
        });
    }

    @Test
    void shouldRejectIssueWithInactiveCategory() {
        CategoryEntity inactiveCat = new CategoryEntity("Retired Cat", "retired", "Inactive", 99);
        inactiveCat.setActive(false);
        inactiveCat = categoryRepository.save(inactiveCat);

        final UUID inactiveCatId = inactiveCat.getId();
        assertThrows(IllegalArgumentException.class, () -> {
            issueService.createIssue(testUser.getId(), inactiveCatId, testLocation.getId(), "Valid Title", "Description");
        });
    }

    @Test
    void shouldPreventUserDeletionWhenIssuesExistDueToForeignKeyIntegrity() {
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Pothole on Ring Road", "Needs urgent repair");

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.delete(testUser);
            userRepository.flush();
        });
    }

    @Test
    void shouldPreventCategoryDeletionWhenIssuesExistDueToForeignKeyIntegrity() {
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Pothole on Ring Road", "Needs urgent repair");

        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryRepository.delete(testCategory);
            categoryRepository.flush();
        });
    }

    @Test
    void shouldPreventLocationDeletionWhenIssuesExistDueToForeignKeyIntegrity() {
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Pothole on Ring Road", "Needs urgent repair");

        assertThrows(DataIntegrityViolationException.class, () -> {
            locationRepository.delete(testLocation);
            locationRepository.flush();
        });
    }
}
