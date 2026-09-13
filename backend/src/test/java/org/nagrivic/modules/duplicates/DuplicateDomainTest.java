package org.nagrivic.modules.duplicates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.duplicates.service.DuplicateDetectionService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DuplicateDomainTest {

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

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    private UserEntity citizenUser;
    private UserEntity officerUser;
    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    @BeforeEach
    void setUp() {
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        citizenUser = userRepository.save(new UserEntity("+919876543210", "Citizen One"));
        officerUser = new UserEntity("+919876543211", "Officer One");
        officerUser.setRole("OFFICER");
        officerUser = userRepository.save(officerUser);

        testCategory = categoryRepository.save(new CategoryEntity("Potholes", "potholes", "Pothole issues", 1));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    private void authenticateAs(UserEntity user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldCreatePrimaryIssueWithNullDuplicateOf() {
        authenticateAs(citizenUser);
        IssueEntity issue = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Main Road Pothole", "Deep pothole");

        assertNotNull(issue.getId());
        assertNull(issue.getDuplicateOf());
        assertFalse(issue.isDuplicate());
        assertNull(issue.getPrimaryIssueId());

        IssueResponse response = issueService.getIssueById(issue.getId());
        assertFalse(response.isDuplicate());
        assertNull(response.primaryIssueId());
    }

    @Test
    void shouldLinkDuplicateIssueToPrimaryAndPreserveHistory() {
        IssueEntity primary = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Primary Pothole", "Original report");
        IssueEntity duplicate = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Duplicate Pothole", "Another report of same spot");

        authenticateAs(officerUser);
        IssueResponse linked = duplicateDetectionService.linkDuplicate(duplicate.getId(), primary.getId());

        assertTrue(linked.isDuplicate());
        assertEquals(primary.getId(), linked.primaryIssueId());

        // Verify entity persisted in database
        IssueEntity reloadedDuplicate = issueRepository.findById(duplicate.getId()).orElseThrow();
        assertTrue(reloadedDuplicate.isDuplicate());
        assertEquals(primary.getId(), reloadedDuplicate.getDuplicateOf().getId());

        // Verify history preserved
        assertEquals("Duplicate Pothole", reloadedDuplicate.getTitle());
        assertEquals("Another report of same spot", reloadedDuplicate.getDescription());
        assertEquals(citizenUser.getId(), reloadedDuplicate.getReporter().getId());
        assertEquals(testLocation.getId(), reloadedDuplicate.getLocation().getId());
        assertEquals(testCategory.getId(), reloadedDuplicate.getCategory().getId());
        assertEquals(IssueStatus.REPORTED, reloadedDuplicate.getStatus());
    }

    @Test
    void shouldRejectLinkingIssueToItself() {
        IssueEntity issue = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Self Pothole", "Self report");

        authenticateAs(officerUser);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                duplicateDetectionService.linkDuplicate(issue.getId(), issue.getId())
        );
        assertTrue(ex.getMessage().contains("cannot be marked as a duplicate of itself"));
    }

    @Test
    void shouldNormalizeDuplicateOfDuplicateToUltimatePrimary() {
        // A = primary, B = duplicate of A, C = duplicate of B -> C should normalize to A
        IssueEntity issueA = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Issue A", "Primary");
        IssueEntity issueB = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Issue B", "First duplicate");
        IssueEntity issueC = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Issue C", "Second duplicate");

        authenticateAs(officerUser);
        duplicateDetectionService.linkDuplicate(issueB.getId(), issueA.getId());

        // Link C to B
        IssueResponse linkedC = duplicateDetectionService.linkDuplicate(issueC.getId(), issueB.getId());

        // Must normalize C directly to A
        assertTrue(linkedC.isDuplicate());
        assertEquals(issueA.getId(), linkedC.primaryIssueId());

        IssueEntity reloadedC = issueRepository.findById(issueC.getId()).orElseThrow();
        assertEquals(issueA.getId(), reloadedC.getDuplicateOf().getId());
    }

    @Test
    void shouldReparentExistingDuplicatesWhenPrimaryBecomesDuplicate() {
        // A is primary, B is duplicate of A.
        // Later, A is found to be duplicate of Root X.
        // B should be reparented so B points directly to X, avoiding X <- A <- B chain.
        IssueEntity rootX = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Root X", "Ultimate primary");
        IssueEntity issueA = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Issue A", "Temporary primary");
        IssueEntity issueB = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Issue B", "Duplicate of A");

        authenticateAs(officerUser);
        duplicateDetectionService.linkDuplicate(issueB.getId(), issueA.getId());

        // Now link A to Root X
        duplicateDetectionService.linkDuplicate(issueA.getId(), rootX.getId());

        // Both A and B must now point directly to Root X
        IssueEntity reloadedA = issueRepository.findById(issueA.getId()).orElseThrow();
        IssueEntity reloadedB = issueRepository.findById(issueB.getId()).orElseThrow();

        assertEquals(rootX.getId(), reloadedA.getDuplicateOf().getId());
        assertEquals(rootX.getId(), reloadedB.getDuplicateOf().getId());
    }

    @Test
    void shouldPreventCircularDuplicateRelationship() {
        // A linked to B, then trying to link B to A must fail
        IssueEntity issueA = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Issue A", "First");
        IssueEntity issueB = issueService.createIssue(citizenUser, testCategory.getId(), testLocation.getId(), "Issue B", "Second");

        authenticateAs(officerUser);
        duplicateDetectionService.linkDuplicate(issueB.getId(), issueA.getId());

        // Attempting to link A to B (which points to A) must be rejected
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                duplicateDetectionService.linkDuplicate(issueA.getId(), issueB.getId())
        );
        assertTrue(ex.getMessage().contains("circular duplicate relationship"));
    }
}
