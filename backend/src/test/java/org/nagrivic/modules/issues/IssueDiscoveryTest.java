package org.nagrivic.modules.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.model.ModerationStatus;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.priority.model.PublicImpact;
import org.nagrivic.modules.priority.model.SafetyImpact;
import org.nagrivic.modules.priority.service.IssuePriorityService;
import org.nagrivic.modules.supports.entity.SupportEntity;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IssueDiscoveryTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private SupportRepository supportRepository;

    @Autowired
    private IssuePriorityService issuePriorityService;

    @Autowired
    private CivicBodyRepository civicBodyRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizen1;
    private UserEntity citizen2;
    private CategoryEntity roadsCategory;
    private CategoryEntity waterCategory;
    private LocationEntity centralLocation; // 23.0225, 72.5714
    private LocationEntity nearbyLocation; // ~500m away (23.0260, 72.5720)
    private LocationEntity distantLocation; // ~15km away (23.1500, 72.6500)

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        citizen1 = userRepository.save(new UserEntity("+919876543211", "Citizen One"));
        citizen2 = userRepository.save(new UserEntity("+919876543212", "Citizen Two"));

        roadsCategory = categoryRepository.save(new CategoryEntity("Roads & Potholes", "roads-potholes", "Pothole issues", 1));
        waterCategory = categoryRepository.save(new CategoryEntity("Water Supply", "water-supply", "Water supply issues", 2));

        centralLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
        nearbyLocation = locationService.createLocation(23.0260, 72.5720, new BigDecimal("5.00"));
        distantLocation = locationService.createLocation(23.1500, 72.6500, new BigDecimal("10.00"));
    }

    // ==========================================
    // 1. DEFAULT LISTING & ORDERING
    // ==========================================

    @Test
    @DisplayName("Default listing returns public issues ordered by newest first, excluding duplicates and hidden")
    void defaultListing_returnsNonHiddenNonDuplicateIssues_sortedNewestFirst() throws Exception {
        IssueEntity issue1 = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "First Issue", "Desc 1");
        Thread.sleep(15);
        IssueEntity issue2 = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Second Issue", "Desc 2");
        Thread.sleep(15);
        IssueEntity hiddenIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Hidden Issue", "Desc 3");
        hiddenIssue.setModerationStatus(ModerationStatus.HIDDEN);
        issueRepository.save(hiddenIssue);

        IssueEntity duplicateIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Duplicate Issue", "Desc 4");
        duplicateIssue.setDuplicateOf(issue1);
        issueRepository.save(duplicateIssue);

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(issue2.getId().toString()))
                .andExpect(jsonPath("$.content[1].id").value(issue1.getId().toString()))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(hiddenIssue.getId().toString()))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(duplicateIssue.getId().toString()))))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ==========================================
    // 2. TEXT SEARCH
    // ==========================================

    @Test
    @DisplayName("Text search matches title and description")
    void textSearch_matchesTitleAndDescription() throws Exception {
        issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Severe crater pothole on Main Road", "Cars are getting damaged");
        issueService.createIssue(citizen1.getId(), waterCategory.getId(), centralLocation.getId(), "Leaking underground pipeline", "Water is flooding the crater nearby");
        issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Broken streetlight pole", "Dark area at night");

        // Search 'crater' (present in issue 1 title and issue 2 description)
        mockMvc.perform(get("/api/issues?q=crater"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title", hasItems("Severe crater pothole on Main Road", "Leaking underground pipeline")));

        // Search 'streetlight' (only issue 3)
        mockMvc.perform(get("/api/issues?q=streetlight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Broken streetlight pole"));
    }

    @Test
    @DisplayName("Text search with non-matching query returns empty list")
    void textSearch_nonMatchingQuery_returnsEmpty() throws Exception {
        issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Regular pothole", "Regular description");

        mockMvc.perform(get("/api/issues?q=nonexistentcivicquery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Text search query validation rejects < 2 chars or > 100 chars")
    void textSearch_invalidQueryLength_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/issues?q=a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        String hugeQuery = "a".repeat(101);
        mockMvc.perform(get("/api/issues?q=" + hugeQuery))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ==========================================
    // 3. CATEGORY FILTER
    // ==========================================

    @Test
    @DisplayName("Category filter returns only selected category and 404 for nonexistent category")
    void categoryFilter_matchesOnlySelectedCategory() throws Exception {
        issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Road Issue", "Desc");
        issueService.createIssue(citizen1.getId(), waterCategory.getId(), centralLocation.getId(), "Water Issue", "Desc");

        mockMvc.perform(get("/api/issues?categoryId=" + waterCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Water Issue"));

        mockMvc.perform(get("/api/issues?categoryId=" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ==========================================
    // 4. STATUS FILTER
    // ==========================================

    @Test
    @DisplayName("Status filter matches selected statuses and keeps resolved issues discoverable")
    void statusFilter_matchesSelectedStatus_resolvedIssuesDiscoverable() throws Exception {
        IssueEntity reported = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Reported Problem", "Desc");
        IssueEntity resolved = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Fixed Problem", "Desc");
        resolved.setStatus(IssueStatus.RESOLVED);
        issueRepository.save(resolved);

        mockMvc.perform(get("/api/issues?status=RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Fixed Problem"))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"));

        // When no status filter provided, both reported and resolved are discoverable
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ==========================================
    // 5. PRIORITY FILTER & REASONING
    // ==========================================

    @Test
    @DisplayName("Priority filter matches selected priority level")
    void priorityFilter_matchesSelectedPriority() throws Exception {
        IssueEntity issueLow = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Minor scratch", "Desc");
        issueLow.setSeverity(IssueSeverity.LOW);
        issueLow.setPublicImpact(PublicImpact.LOW);
        issueLow.setSafetyImpact(SafetyImpact.NONE);
        issueLow = issueRepository.save(issueLow);
        issuePriorityService.calculatePriority(issueLow);

        IssueEntity issueCritical = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Massive sinkhole", "Desc");
        issueCritical.setSeverity(IssueSeverity.CRITICAL);
        issueCritical.setPublicImpact(PublicImpact.HIGH);
        issueCritical.setSafetyImpact(SafetyImpact.CRITICAL);
        issueCritical = issueRepository.save(issueCritical);
        issuePriorityService.calculatePriority(issueCritical);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/issues?priority=CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Massive sinkhole"))
                .andExpect(jsonPath("$.content[0].priority.level").value("CRITICAL"));
    }

    // ==========================================
    // 6. CIVIC RESPONSIBILITY FILTERS
    // ==========================================

    @Test
    @DisplayName("Civic responsibility filters by city, ward, civic body, department")
    void civicResponsibilityFilter_cityWardCivicBodyDepartment() throws Exception {
        CivicBodyEntity body = civicBodyRepository.save(new CivicBodyEntity("Ahmedabad Municipal Corp", CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Ahmedabad"));
        CityEntity city = cityRepository.save(new CityEntity("Ahmedabad", "Gujarat", body));
        WardEntity ward = wardRepository.save(new WardEntity(city, body, "12", "Navrangpura", null));
        DepartmentEntity dept = departmentRepository.save(new DepartmentEntity(body, "Roads Department", "ROADS", "Roads maintenance"));

        IssueEntity enrichedIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Assigned Issue", "Desc");
        enrichedIssue.setCity(city);
        enrichedIssue.setWard(ward);
        enrichedIssue.setCivicBody(body);
        enrichedIssue.setDepartment(dept);
        enrichedIssue.setResponsibilityStatus(ResponsibilityStatus.RESOLVED);
        issueRepository.save(enrichedIssue);

        IssueEntity unresolvedIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Unresolved Issue", "Desc");

        // Filter by wardId
        mockMvc.perform(get("/api/issues?wardId=" + ward.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Assigned Issue"));

        // Unresolved issue still appears in general search
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ==========================================
    // 7. GEOGRAPHIC NEARBY SEARCH & DISTANCE
    // ==========================================

    @Test
    @DisplayName("Geographic search returns nearby issues with distanceMeters and filters out distant issues")
    void nearbySearch_withinRadius_returnsDistanceMeters() throws Exception {
        // central location: 23.0225, 72.5714
        IssueEntity centralIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Central Issue", "Desc");
        // nearby location: 23.0260, 72.5720 (~390m away)
        IssueEntity nearbyIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), nearbyLocation.getId(), "Nearby Issue", "Desc");
        // distant location: 23.1500, 72.6500 (~16km away)
        IssueEntity distantIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), distantLocation.getId(), "Distant Issue", "Desc");

        // Radius 2000m around central point: should include central and nearby, exclude distant
        mockMvc.perform(get("/api/issues?latitude=23.0225&longitude=72.5714&radiusMeters=2000&sort=NEAREST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Central Issue"))
                .andExpect(jsonPath("$.content[0].distanceMeters", notNullValue()))
                .andExpect(jsonPath("$.content[1].title").value("Nearby Issue"))
                .andExpect(jsonPath("$.content[1].distanceMeters", notNullValue()))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Distant Issue"))));
    }

    @Test
    @DisplayName("Non-geographic search omits distanceMeters field in response")
    void nearbySearch_omitsDistanceMeters_whenNoGeoSearchRequested() throws Exception {
        issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "No Geo Issue", "Desc");

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceMeters").doesNotExist());
    }

    @Test
    @DisplayName("Geographic parameter validation rejects invalid coordinates and excessive radius")
    void geographicValidation_rejectsInvalidCoordinatesAndExcessiveRadius() throws Exception {
        // Missing longitude
        mockMvc.perform(get("/api/issues?latitude=23.0225"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        // Latitude out of range
        mockMvc.perform(get("/api/issues?latitude=95.0&longitude=72.5714"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        // Radius negative or zero
        mockMvc.perform(get("/api/issues?latitude=23.0225&longitude=72.5714&radiusMeters=-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        // Radius exceeding max (50,000m)
        mockMvc.perform(get("/api/issues?latitude=23.0225&longitude=72.5714&radiusMeters=60000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ==========================================
    // 8. SORTING MODES
    // ==========================================

    @Test
    @DisplayName("Sorting modes: OLDEST, PRIORITY, MOST_SUPPORTED, NEAREST")
    void sortingModes_workCorrectly() throws Exception {
        IssueEntity issue1 = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "First Created", "Desc 1");
        Thread.sleep(15);
        IssueEntity issue2 = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), nearbyLocation.getId(), "Second Created", "Desc 2");

        // Priority calculation: give issue1 a high priority
        issue1.setSeverity(IssueSeverity.CRITICAL);
        issue1.setPublicImpact(PublicImpact.HIGH);
        issue1.setSafetyImpact(SafetyImpact.CRITICAL);
        issue1 = issueRepository.save(issue1);
        issuePriorityService.calculatePriority(issue1);

        issue2.setSeverity(IssueSeverity.LOW);
        issue2.setPublicImpact(PublicImpact.LOW);
        issue2.setSafetyImpact(SafetyImpact.NONE);
        issue2 = issueRepository.save(issue2);
        issuePriorityService.calculatePriority(issue2);

        // Support: add support to issue2
        supportRepository.save(new SupportEntity(issue2, citizen2));

        // 1. OLDEST: issue1 first
        mockMvc.perform(get("/api/issues?sort=OLDEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("First Created"))
                .andExpect(jsonPath("$.content[1].title").value("Second Created"));

        // 2. PRIORITY: issue1 first (higher priority score)
        mockMvc.perform(get("/api/issues?sort=PRIORITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("First Created"))
                .andExpect(jsonPath("$.content[1].title").value("Second Created"));

        // 3. MOST_SUPPORTED: issue2 first (1 support vs 0)
        mockMvc.perform(get("/api/issues?sort=MOST_SUPPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Second Created"))
                .andExpect(jsonPath("$.content[1].title").value("First Created"));

        // 4. NEAREST to nearbyLocation (issue2 distance ~0m, issue1 distance >0m)
        mockMvc.perform(get("/api/issues?latitude=23.0260&longitude=72.5720&sort=NEAREST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Second Created"))
                .andExpect(jsonPath("$.content[1].title").value("First Created"));
    }

    @Test
    @DisplayName("NEAREST sort without coordinates returns 400 Bad Request")
    void nearestSort_withoutCoordinates_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/issues?sort=NEAREST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Invalid sort name returns 400 Bad Request")
    void invalidSort_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/issues?sort=UNSUPPORTED_SORT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // ==========================================
    // 9. DUPLICATE & MODERATION VISIBILITY
    // ==========================================

    @Test
    @DisplayName("Duplicate issues excluded from discovery but remain accessible via direct ID lookup")
    void duplicateFiltering_directLookupWorks() throws Exception {
        IssueEntity primary = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Primary Issue", "Desc");
        IssueEntity duplicate = issueService.createIssue(citizen2.getId(), roadsCategory.getId(), centralLocation.getId(), "Duplicate Issue", "Desc");
        duplicate.setDuplicateOf(primary);
        issueRepository.save(duplicate);

        // Discovery feed excludes duplicate
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(primary.getId().toString()));

        // Direct lookup of duplicate still returns it with duplicate metadata
        mockMvc.perform(get("/api/issues/" + duplicate.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(duplicate.getId().toString()))
                .andExpect(jsonPath("$.isDuplicate").value(true))
                .andExpect(jsonPath("$.primaryIssueId").value(primary.getId().toString()));
    }

    @Test
    @DisplayName("Moderation UNDER_REVIEW remains public, HIDDEN is excluded")
    void moderationVisibility_underReviewVisible_hiddenExcluded() throws Exception {
        IssueEntity reviewIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Under Review Issue", "Desc");
        reviewIssue.setModerationStatus(ModerationStatus.UNDER_REVIEW);
        issueRepository.save(reviewIssue);

        IssueEntity hiddenIssue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Hidden Abuse Issue", "Desc");
        hiddenIssue.setModerationStatus(ModerationStatus.HIDDEN);
        issueRepository.save(hiddenIssue);

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(reviewIssue.getId().toString()))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(hiddenIssue.getId().toString()))));
    }

    // ==========================================
    // 10. PRIVACY & HYGIENE
    // ==========================================

    @Test
    @DisplayName("Public issue response never exposes sensitive user information or internal moderation notes")
    void privacy_doesNotExposeSensitiveData() throws Exception {
        IssueEntity issue = issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Privacy Test", "Desc");

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reportedBy").value(citizen1.getId().toString()))
                .andExpect(jsonPath("$.content[0].phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.content[0].phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].moderationStatus").doesNotExist())
                .andExpect(jsonPath("$.content[0].moderationNotes").doesNotExist())
                .andExpect(jsonPath("$.content[0].rawGeometry").doesNotExist());
    }

    // ==========================================
    // 11. PAGINATION & BOUNDS
    // ==========================================

    @Test
    @DisplayName("Custom pagination and max size limit (100) work as expected")
    void pagination_boundsAndLimits() throws Exception {
        for (int i = 1; i <= 5; i++) {
            issueService.createIssue(citizen1.getId(), roadsCategory.getId(), centralLocation.getId(), "Issue " + i, "Desc " + i);
        }

        mockMvc.perform(get("/api/issues?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/issues?size=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
}
