package org.nagrivic.modules.accountability;

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
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.model.ModerationStatus;
import org.nagrivic.modules.priority.entity.IssuePriorityEntity;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.priority.repository.IssuePriorityRepository;
import org.nagrivic.modules.statushistory.entity.StatusHistoryEntity;
import org.nagrivic.modules.statushistory.repository.StatusHistoryRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PublicAccountabilityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private CivicBodyRepository civicBodyRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssuePriorityRepository priorityRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    private static final AtomicInteger SEQ = new AtomicInteger(500);

    private UserEntity citizenUser;
    private CivicBodyEntity civicBody;
    private CityEntity city;
    private WardEntity ward1;
    private WardEntity ward2;
    private DepartmentEntity roadDept;
    private DepartmentEntity waterDept;
    private CategoryEntity roadCategory;
    private CategoryEntity waterCategory;
    private LocationEntity sampleLocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        int s = SEQ.incrementAndGet();

        citizenUser = userRepository.save(new UserEntity(
                "+9198765" + String.format("%05d", s),
                "Citizen " + s
        ));

        civicBody = civicBodyRepository.findAll().stream()
                .filter(b -> "Ahmedabad Municipal Corporation".equalsIgnoreCase(b.getName()))
                .findFirst()
                .orElseGet(() -> civicBodyRepository.save(new CivicBodyEntity(
                        "Ahmedabad Municipal Corporation",
                        CivicBodyType.MUNICIPAL_CORPORATION,
                        "Gujarat",
                        "Ahmedabad"
                )));

        city = cityRepository.findByNameAndState("Ahmedabad", "Gujarat")
                .orElseGet(() -> cityRepository.save(new CityEntity(
                        "Ahmedabad",
                        "Gujarat",
                        civicBody
                )));

        ward1 = wardRepository.findByCityIdAndWardNumber(city.getId(), "W01-" + s)
                .orElseGet(() -> wardRepository.save(new WardEntity(
                        city,
                        civicBody,
                        "W01-" + s,
                        "Navrangpura " + s
                )));

        ward2 = wardRepository.findByCityIdAndWardNumber(city.getId(), "W02-" + s)
                .orElseGet(() -> wardRepository.save(new WardEntity(
                        city,
                        civicBody,
                        "W02-" + s,
                        "Naranpura " + s
                )));

        roadDept = departmentRepository.findByCivicBodyIdAndCode(civicBody.getId(), "ENG_ROAD_" + s)
                .orElseGet(() -> departmentRepository.save(new DepartmentEntity(
                        civicBody,
                        "Engineering - Roads " + s,
                        "ENG_ROAD_" + s,
                        "Roads"
                )));

        waterDept = departmentRepository.findByCivicBodyIdAndCode(civicBody.getId(), "WTR_OPS_" + s)
                .orElseGet(() -> departmentRepository.save(new DepartmentEntity(
                        civicBody,
                        "Water Works " + s,
                        "WTR_OPS_" + s,
                        "Water"
                )));

        roadCategory = categoryRepository.findBySlug("roads-test-" + s)
                .orElseGet(() -> categoryRepository.save(new CategoryEntity(
                        "Roads " + s,
                        "roads-test-" + s,
                        "Roads " + s,
                        10
                )));

        waterCategory = categoryRepository.findBySlug("water-test-" + s)
                .orElseGet(() -> categoryRepository.save(new CategoryEntity(
                        "Water " + s,
                        "water-test-" + s,
                        "Water " + s,
                        20
                )));

        sampleLocation = locationService.createLocation(23.0338, 72.5467, new BigDecimal("5.00"));
    }

    private IssueEntity createIssue(
            String title,
            IssueStatus status,
            PriorityLevel priorityLevel,
            CategoryEntity category,
            WardEntity ward,
            DepartmentEntity department,
            ResponsibilityStatus respStatus,
            ModerationStatus modStatus,
            IssueEntity duplicateOf,
            Instant createdAt
    ) {
        IssueEntity issue = new IssueEntity(citizenUser, category, sampleLocation, title, "Description for " + title);
        issue.setStatus(status);
        issue.setCity(city);
        issue.setCivicBody(civicBody);
        issue.setWard(ward);
        issue.setDepartment(department);
        issue.setResponsibilityStatus(respStatus);
        issue.setModerationStatus(modStatus);
        issue.setDuplicateOf(duplicateOf);
        issue.setCreatedAt(createdAt != null ? createdAt : Instant.now());
        issue.setUpdatedAt(createdAt != null ? createdAt : Instant.now());
        issue = issueRepository.save(issue);

        if (priorityLevel != null) {
            int score = priorityLevel == PriorityLevel.CRITICAL ? 90 : priorityLevel == PriorityLevel.HIGH ? 70 : priorityLevel == PriorityLevel.MEDIUM ? 45 : 20;
            IssuePriorityEntity priority = new IssuePriorityEntity(
                    issue,
                    priorityLevel,
                    score,
                    20,
                    20,
                    20,
                    10,
                    0,
                    "v1"
            );
            priorityRepository.save(priority);
            issue.setPriority(priority);
        }

        // Add initial status history
        statusHistoryRepository.save(new StatusHistoryEntity(
                issue,
                null,
                status,
                citizenUser,
                "Initial creation"
        ));

        return issue;
    }

    @Test
    @DisplayName("Test 1: Public unauthenticated access to /api/public/accountability succeeds")
    void testPublicAccess_UnauthenticatedSucceeds() throws Exception {
        mockMvc.perform(get("/api/public/accountability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName").value("Ahmedabad"))
                .andExpect(jsonPath("$.range").value("30d"))
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.statusBreakdown").exists())
                .andExpect(jsonPath("$.priorityBreakdown").exists())
                .andExpect(jsonPath("$.categoryBreakdown").isArray())
                .andExpect(jsonPath("$.wardBreakdown").isArray())
                .andExpect(jsonPath("$.agingBreakdown").exists())
                .andExpect(jsonPath("$.verificationSummary").exists())
                .andExpect(jsonPath("$.responsibilitySummary").exists())
                .andExpect(jsonPath("$.trend").isArray())
                .andExpect(jsonPath("$.lastUpdated").exists());
    }

    @Test
    @DisplayName("Test 2: Summary counts, status breakdown, and priority breakdown calculate accurately")
    void testSummaryAndStatusBreakdown() throws Exception {
        Instant now = Instant.now();

        createIssue("Pothole 1", IssueStatus.REPORTED, PriorityLevel.HIGH, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Pipe Burst", IssueStatus.IN_PROGRESS, PriorityLevel.CRITICAL, waterCategory, ward1, waterDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Pothole 2", IssueStatus.RESOLVED, PriorityLevel.MEDIUM, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Pothole 3", IssueStatus.CITIZEN_VERIFIED, PriorityLevel.LOW, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Water Leak", IssueStatus.NOT_FIXED, PriorityLevel.HIGH, waterCategory, ward1, waterDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);

        mockMvc.perform(get("/api/public/accountability")
                        .param("cityId", city.getId().toString())
                        .param("range", "all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalPublicIssues", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.summary.reportedCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.inProgressCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.resolvedCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.citizenVerifiedCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.notFixedCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.highPriorityCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.summary.criticalPriorityCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.statusBreakdown.reported", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.statusBreakdown.inProgress", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.statusBreakdown.resolved", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.statusBreakdown.citizenVerified", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.statusBreakdown.notFixed", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Test 3: Hidden and duplicate issues are excluded from public accountability counts")
    void testHiddenAndDuplicateIssuesExcluded() throws Exception {
        Instant now = Instant.now();

        // Primary canonical issue
        IssueEntity primary = createIssue("Canonical Primary Pothole", IssueStatus.IN_PROGRESS, PriorityLevel.HIGH, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);

        // Duplicate of primary issue -> MUST BE EXCLUDED from counts
        createIssue("Duplicate Pothole Report", IssueStatus.IN_PROGRESS, PriorityLevel.HIGH, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, primary, now);

        // Moderated hidden issue -> MUST BE EXCLUDED from counts
        createIssue("Hidden Inappropriate Report", IssueStatus.IN_PROGRESS, PriorityLevel.CRITICAL, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.HIDDEN, null, now);

        // Query accountability scoped to ward1 and roadCategory
        mockMvc.perform(get("/api/public/accountability")
                        .param("cityId", city.getId().toString())
                        .param("wardId", ward1.getId().toString())
                        .param("categoryId", roadCategory.getId().toString())
                        .param("range", "all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalPublicIssues").value(1))
                .andExpect(jsonPath("$.summary.inProgressCount").value(1))
                .andExpect(jsonPath("$.summary.highPriorityCount").value(1))
                .andExpect(jsonPath("$.summary.criticalPriorityCount").value(0));
    }

    @Test
    @DisplayName("Test 4: Analytical age distribution accurately categorizes open issues")
    void testAgingBreakdown() throws Exception {
        Instant now = Instant.now();

        createIssue("Fresh Issue", IssueStatus.REPORTED, PriorityLevel.MEDIUM, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now.minus(4, ChronoUnit.HOURS));
        createIssue("Week Old Issue", IssueStatus.IN_PROGRESS, PriorityLevel.MEDIUM, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now.minus(4, ChronoUnit.DAYS));
        createIssue("Fortnight Old Issue", IssueStatus.NOT_FIXED, PriorityLevel.HIGH, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now.minus(15, ChronoUnit.DAYS));
        createIssue("Month Old Issue", IssueStatus.ACKNOWLEDGED, PriorityLevel.HIGH, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now.minus(45, ChronoUnit.DAYS));
        createIssue("Long Open Issue", IssueStatus.IN_PROGRESS, PriorityLevel.CRITICAL, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now.minus(100, ChronoUnit.DAYS));
        createIssue("Resolved Long Ago", IssueStatus.RESOLVED, PriorityLevel.LOW, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now.minus(100, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/public/accountability")
                        .param("cityId", city.getId().toString())
                        .param("wardId", ward1.getId().toString())
                        .param("range", "all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agingBreakdown.zeroToOneDay", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.agingBreakdown.twoToSevenDays", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.agingBreakdown.eightToThirtyDays", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.agingBreakdown.thirtyOneToNinetyDays", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.agingBreakdown.overNinetyDays", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Test 5: Citizen verification metrics and verification rate calculation")
    void testCitizenVerificationMetrics() throws Exception {
        Instant now = Instant.now();

        createIssue("Resolved Pending 1", IssueStatus.RESOLVED, PriorityLevel.MEDIUM, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Resolved Pending 2", IssueStatus.RESOLVED, PriorityLevel.MEDIUM, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Verified 1", IssueStatus.CITIZEN_VERIFIED, PriorityLevel.MEDIUM, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Verified 2", IssueStatus.CITIZEN_VERIFIED, PriorityLevel.MEDIUM, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Verified 3", IssueStatus.CITIZEN_VERIFIED, PriorityLevel.MEDIUM, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Not Fixed 1", IssueStatus.NOT_FIXED, PriorityLevel.MEDIUM, roadCategory, ward2, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);

        mockMvc.perform(get("/api/public/accountability")
                        .param("cityId", city.getId().toString())
                        .param("wardId", ward2.getId().toString())
                        .param("range", "all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationSummary.resolvedByAuthority").value(6))
                .andExpect(jsonPath("$.verificationSummary.citizenVerified").value(3))
                .andExpect(jsonPath("$.verificationSummary.citizenReportedNotFixed").value(1))
                .andExpect(jsonPath("$.verificationSummary.verificationPending").value(2))
                .andExpect(jsonPath("$.verificationSummary.verificationRate").value(50.0));
    }

    @Test
    @DisplayName("Test 6: Category and Ward breakdown accurately reflect civic entities")
    void testCategoryAndWardBreakdown() throws Exception {
        Instant now = Instant.now();

        createIssue("Road Defect Ward 1", IssueStatus.IN_PROGRESS, PriorityLevel.HIGH, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Water Defect Ward 2", IssueStatus.REPORTED, PriorityLevel.MEDIUM, waterCategory, ward2, waterDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);

        mockMvc.perform(get("/api/public/accountability")
                        .param("cityId", city.getId().toString())
                        .param("range", "all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryBreakdown[?(@.slug == '" + roadCategory.getSlug() + "')].total", hasItem(1)))
                .andExpect(jsonPath("$.categoryBreakdown[?(@.slug == '" + waterCategory.getSlug() + "')].total", hasItem(1)))
                .andExpect(jsonPath("$.wardBreakdown[?(@.wardNumber == '" + ward1.getWardNumber() + "')].total", hasItem(1)))
                .andExpect(jsonPath("$.wardBreakdown[?(@.wardNumber == '" + ward2.getWardNumber() + "')].total", hasItem(1)));
    }

    @Test
    @DisplayName("Test 7: Responsibility coverage shows resolved vs unresolved without guessing")
    void testResponsibilityCoverage() throws Exception {
        Instant now = Instant.now();

        createIssue("Resolved Responsibility Issue", IssueStatus.REPORTED, PriorityLevel.LOW, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, now);
        createIssue("Unresolved Responsibility Issue", IssueStatus.REPORTED, PriorityLevel.LOW, roadCategory, null, null, ResponsibilityStatus.UNRESOLVED, ModerationStatus.VISIBLE, null, now);

        mockMvc.perform(get("/api/public/accountability")
                        .param("cityId", city.getId().toString())
                        .param("categoryId", roadCategory.getId().toString())
                        .param("range", "all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsibilitySummary.resolvedCount").value(1))
                .andExpect(jsonPath("$.responsibilitySummary.unresolvedCount").value(1))
                .andExpect(jsonPath("$.responsibilitySummary.coveragePercentage").value(50.0))
                .andExpect(jsonPath("$.responsibilitySummary.departmentBreakdown").isArray());
    }

    @Test
    @DisplayName("Test 8: Strict privacy verification - Zero citizen or officer PII in public response")
    void testPrivacy_ZeroPiiExposure() throws Exception {
        createIssue("Public Test Issue", IssueStatus.IN_PROGRESS, PriorityLevel.HIGH, roadCategory, ward1, roadDept, ResponsibilityStatus.RESOLVED, ModerationStatus.VISIBLE, null, Instant.now());

        String responseContent = mockMvc.perform(get("/api/public/accountability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(responseContent.contains(citizenUser.getPhoneNumber()), "Phone number must never be exposed");
        assertFalse(responseContent.contains(citizenUser.getId().toString()), "User ID must never be exposed");
        assertFalse(responseContent.contains("token"), "No authentication tokens in response");
    }
}
