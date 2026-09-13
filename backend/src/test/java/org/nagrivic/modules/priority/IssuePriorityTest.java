package org.nagrivic.modules.priority;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.priority.entity.IssuePriorityEntity;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.priority.model.PublicImpact;
import org.nagrivic.modules.priority.model.SafetyImpact;
import org.nagrivic.modules.priority.repository.IssuePriorityRepository;
import org.nagrivic.modules.priority.service.IssuePriorityService;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.dto.VerifyResolutionRequest;
import org.nagrivic.modules.statushistory.service.StatusHistoryService;
import org.nagrivic.modules.supports.entity.SupportEntity;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.supports.service.SupportService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IssuePriorityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssuePriorityService issuePriorityService;

    @Autowired
    private IssuePriorityRepository issuePriorityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private SupportService supportService;

    @Autowired
    private SupportRepository supportRepository;

    @Autowired
    private StatusHistoryService statusHistoryService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizen;
    private String citizenToken;
    private UserEntity officer;
    private String officerToken;
    private CategoryEntity potholeCategory;
    private LocationEntity testLocation;

    private UserEntity createUniqueUser(String name, String role) {
        long uniqueNum = Math.abs(java.util.concurrent.ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
        UserEntity u = new UserEntity("+91" + uniqueNum, name);
        if (role != null) {
            u.setRole(role);
        }
        return userRepository.save(u);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        citizen = createUniqueUser("Citizen Aarav", "CITIZEN");
        citizenToken = jwtService.generateAccessToken(citizen.getId(), citizen.getRole());

        officer = createUniqueUser("Officer Sharma", "OFFICER");
        officerToken = jwtService.generateAccessToken(officer.getId(), officer.getRole());

        potholeCategory = categoryRepository.save(new CategoryEntity(
                "Roads / Potholes",
                "roads-potholes-" + System.nanoTime(),
                "Pothole issues",
                1
        ));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("A. Basic calculation: verify exact component scores, total, and priority level")
    void testBasicPriorityCalculation() {
        // Create issue
        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                testLocation.getId(),
                "Hazardous crater on road",
                "Deep crater",
                IssueSeverity.HIGH
        );
        issue.setPublicImpact(PublicImpact.MEDIUM);
        issue.setSafetyImpact(SafetyImpact.MEDIUM);
        issue = issueRepository.save(issue);

        // Recalculate
        IssuePriorityEntity priority = issuePriorityService.calculatePriority(issue);

        // Severity: HIGH (25)
        // Public Impact: MEDIUM (15)
        // Safety Impact: MEDIUM (15)
        // Age: 0 days (0)
        // Support: 0 supports (0)
        // Total: 25 + 15 + 15 + 0 + 0 = 55 -> HIGH (50-74)
        assertThat(priority.getSeverityScore()).isEqualTo(25);
        assertThat(priority.getImpactScore()).isEqualTo(15);
        assertThat(priority.getSafetyScore()).isEqualTo(15);
        assertThat(priority.getAgeScore()).isEqualTo(0);
        assertThat(priority.getSupportScore()).isEqualTo(0);
        assertThat(priority.getScore()).isEqualTo(55);
        assertThat(priority.getPriorityLevel()).isEqualTo(PriorityLevel.HIGH);
        assertThat(priority.getCalculationVersion()).isEqualTo("v1");

        // Verify IssueResponse DTO
        IssueResponse response = issueService.getIssueById(issue.getId());
        assertThat(response.priority()).isNotNull();
        assertThat(response.priority().level()).isEqualTo(PriorityLevel.HIGH);
        assertThat(response.priority().score()).isEqualTo(55);
    }

    @Test
    @DisplayName("B. Boundary tests: verify exact band transitions (24/25, 49/50, 74/75)")
    void testBoundaryScoreTransitions() {
        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                testLocation.getId(),
                "Boundary test issue",
                "Testing boundaries",
                IssueSeverity.LOW // 5
        );

        // Score 24 -> LOW (0-24)
        // sev=5, imp=5, saf=5, age=5, sup=4 -> 24
        issue.setPublicImpact(PublicImpact.LOW);   // 5
        issue.setSafetyImpact(SafetyImpact.LOW);   // 5
        issue.setCreatedAt(Instant.now().minus(15, ChronoUnit.DAYS)); // age=5 (8-30d)
        issue = issueRepository.save(issue);

        // Mock 4 supports for support score = 4
        for (int i = 0; i < 4; i++) {
            UserEntity supporter = createUniqueUser("Supporter " + i, "CITIZEN");
            supportRepository.save(new SupportEntity(issue, supporter));
        }

        IssuePriorityEntity p24 = issuePriorityService.calculatePriority(issue);
        assertThat(p24.getScore()).isEqualTo(24);
        assertThat(p24.getPriorityLevel()).isEqualTo(PriorityLevel.LOW);

        // Score 25 -> MEDIUM (25-49)
        // Increase severity from LOW (5) to MEDIUM (15) -> 15 + 5 + 5 + 5 + 4 = 34 or adjust to 25
        // Let's set: sev=15 (MED), imp=5 (LOW), saf=5 (LOW), age=0 (0d), sup=0 (0) -> 25
        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setPublicImpact(PublicImpact.LOW);
        issue.setSafetyImpact(SafetyImpact.LOW);
        issue.setCreatedAt(Instant.now());
        supportRepository.deleteAll();
        issue = issueRepository.save(issue);

        IssuePriorityEntity p25 = issuePriorityService.calculatePriority(issue);
        assertThat(p25.getScore()).isEqualTo(25);
        assertThat(p25.getPriorityLevel()).isEqualTo(PriorityLevel.MEDIUM);

        // Score 49 -> MEDIUM (25-49)
        // sev=25 (HIGH), imp=15 (MED), saf=5 (LOW), age=0, sup=4 (4 supports) -> 49
        issue.setSeverity(IssueSeverity.HIGH);     // 25
        issue.setPublicImpact(PublicImpact.MEDIUM); // 15
        issue.setSafetyImpact(SafetyImpact.LOW);   // 5
        issue.setCreatedAt(Instant.now());         // 0
        for (int i = 0; i < 4; i++) {
            UserEntity supporter = createUniqueUser("Supporter " + i, "CITIZEN");
            supportRepository.save(new SupportEntity(issue, supporter));
        }
        issue = issueRepository.save(issue);

        IssuePriorityEntity p49 = issuePriorityService.calculatePriority(issue);
        assertThat(p49.getScore()).isEqualTo(49);
        assertThat(p49.getPriorityLevel()).isEqualTo(PriorityLevel.MEDIUM);

        // Score 50 -> HIGH (50-74)
        // Add 3 more supports (total 7 supports -> support score = 6) -> 25 + 15 + 5 + 0 + 6 = 51, or age=2 -> 25+15+5+2+3 (sup=4) = 49+2 = 51
        // Let's test exact 50: sev=25 (HIGH), imp=15 (MED), saf=5 (LOW), age=5 (15d), sup=0 -> 25+15+5+5+0 = 50
        supportRepository.deleteAll();
        issue.setCreatedAt(Instant.now().minus(15, ChronoUnit.DAYS)); // age=5
        issue = issueRepository.save(issue);

        IssuePriorityEntity p50 = issuePriorityService.calculatePriority(issue);
        assertThat(p50.getScore()).isEqualTo(50);
        assertThat(p50.getPriorityLevel()).isEqualTo(PriorityLevel.HIGH);

        // Score 74 -> HIGH (50-74)
        // sev=30 (CRITICAL), imp=25 (HIGH), saf=15 (MED), age=0, sup=4 (4 supports) -> 30+25+15+0+4 = 74
        issue.setSeverity(IssueSeverity.CRITICAL);
        issue.setPublicImpact(PublicImpact.HIGH);
        issue.setSafetyImpact(SafetyImpact.MEDIUM);
        issue.setCreatedAt(Instant.now());
        for (int i = 0; i < 4; i++) {
            UserEntity supporter = createUniqueUser("Supporter " + i, "CITIZEN");
            supportRepository.save(new SupportEntity(issue, supporter));
        }
        issue = issueRepository.save(issue);

        IssuePriorityEntity p74 = issuePriorityService.calculatePriority(issue);
        assertThat(p74.getScore()).isEqualTo(74);
        assertThat(p74.getPriorityLevel()).isEqualTo(PriorityLevel.HIGH);

        // Score 75 -> CRITICAL (75-100)
        // Add 3 more supports (total 7 supports -> support score = 6) -> 30+25+15+0+6 = 76 -> CRITICAL
        // Or sev=30, imp=25, saf=20 (HIGH), age=0, sup=0 -> 75
        supportRepository.deleteAll();
        issue.setSafetyImpact(SafetyImpact.HIGH); // 20
        issue = issueRepository.save(issue);

        IssuePriorityEntity p75 = issuePriorityService.calculatePriority(issue);
        assertThat(p75.getScore()).isEqualTo(75);
        assertThat(p75.getPriorityLevel()).isEqualTo(PriorityLevel.CRITICAL);
    }

    @Test
    @DisplayName("C. Age progression: older otherwise-equivalent issues receive higher age scores (capped at 10)")
    void testAgeProgression() {
        IssueEntity freshIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Fresh", "Desc"
        );
        freshIssue.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS)); // 1 day -> 0 pts
        freshIssue = issueRepository.save(freshIssue);

        IssueEntity weekOldIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Week Old", "Desc"
        );
        weekOldIssue.setCreatedAt(Instant.now().minus(5, ChronoUnit.DAYS)); // 5 days -> 2 pts
        weekOldIssue = issueRepository.save(weekOldIssue);

        IssueEntity monthOldIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Month Old", "Desc"
        );
        monthOldIssue.setCreatedAt(Instant.now().minus(20, ChronoUnit.DAYS)); // 20 days -> 5 pts
        monthOldIssue = issueRepository.save(monthOldIssue);

        IssueEntity ancientIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Ancient", "Desc"
        );
        ancientIssue.setCreatedAt(Instant.now().minus(120, ChronoUnit.DAYS)); // 120 days -> 10 pts (cap)
        ancientIssue = issueRepository.save(ancientIssue);

        IssuePriorityEntity pFresh = issuePriorityService.calculatePriority(freshIssue);
        IssuePriorityEntity pWeek = issuePriorityService.calculatePriority(weekOldIssue);
        IssuePriorityEntity pMonth = issuePriorityService.calculatePriority(monthOldIssue);
        IssuePriorityEntity pAncient = issuePriorityService.calculatePriority(ancientIssue);

        assertThat(pFresh.getAgeScore()).isEqualTo(0);
        assertThat(pWeek.getAgeScore()).isEqualTo(2);
        assertThat(pMonth.getAgeScore()).isEqualTo(5);
        assertThat(pAncient.getAgeScore()).isEqualTo(10);
    }

    @Test
    @DisplayName("D. Support cap: support contribution strictly caps at 10 even with many supporters")
    void testSupportCap() {
        IssueEntity issue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Popular Pothole", "Desc"
        );

        // Add 30 supporters
        for (int i = 0; i < 30; i++) {
            UserEntity supporter = createUniqueUser("Supporter " + i, "CITIZEN");
            supportRepository.save(new SupportEntity(issue, supporter));
        }

        IssuePriorityEntity priority = issuePriorityService.calculatePriority(issue);
        assertThat(priority.getSupportScore()).isEqualTo(10); // Capped at 10
    }

    @Test
    @DisplayName("E. Safety importance: safety contributes independently and physical hazards cannot be suppressed")
    void testSafetyImportance() {
        // High hazard, 0 supports
        IssueEntity hazardousIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Exposed Live Wire", "Desc", IssueSeverity.CRITICAL
        );
        hazardousIssue.setSafetyImpact(SafetyImpact.CRITICAL); // 25
        hazardousIssue.setPublicImpact(PublicImpact.HIGH);      // 25
        hazardousIssue = issueRepository.save(hazardousIssue);

        IssuePriorityEntity pDanger = issuePriorityService.calculatePriority(hazardousIssue);
        // sev=30, saf=25, imp=25, age=0, sup=0 -> total=80 -> CRITICAL
        assertThat(pDanger.getSafetyScore()).isEqualTo(25);
        assertThat(pDanger.getScore()).isGreaterThanOrEqualTo(75);
        assertThat(pDanger.getPriorityLevel()).isEqualTo(PriorityLevel.CRITICAL);

        // Low hazard, high support (capped at 10)
        IssueEntity aestheticIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Paint peeling", "Desc", IssueSeverity.LOW
        );
        aestheticIssue.setSafetyImpact(SafetyImpact.NONE);      // 0
        aestheticIssue.setPublicImpact(PublicImpact.LOW);       // 5
        aestheticIssue = issueRepository.save(aestheticIssue);

        for (int i = 0; i < 25; i++) {
            UserEntity supporter = createUniqueUser("Supporter " + i, "CITIZEN");
            supportRepository.save(new SupportEntity(aestheticIssue, supporter));
        }

        IssuePriorityEntity pAesthetic = issuePriorityService.calculatePriority(aestheticIssue);
        // sev=5, imp=5, saf=0, age=0, sup=10 -> total=20 -> LOW
        assertThat(pAesthetic.getScore()).isEqualTo(20);
        assertThat(pAesthetic.getPriorityLevel()).isEqualTo(PriorityLevel.LOW);
    }

    @Test
    @DisplayName("F. Canonical duplicate handling: priority is associated with canonical issue")
    void testCanonicalDuplicateHandling() {
        IssueEntity canonicalIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Canonical Primary", "Desc"
        );

        IssueEntity duplicateIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Duplicate Report", "Desc"
        );
        duplicateIssue.setDuplicateOf(canonicalIssue);
        duplicateIssue = issueRepository.save(duplicateIssue);

        // Calculating on duplicateIssue calculates for canonicalIssue
        IssuePriorityEntity priority = issuePriorityService.calculatePriority(duplicateIssue);
        assertThat(priority.getIssue().getId()).isEqualTo(canonicalIssue.getId());
    }

    @Test
    @DisplayName("G. Recalculation: running recalculation multiple times produces deterministic result without duplicates")
    void testRecalculationIdempotency() {
        IssueEntity issue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Idempotent Issue", "Desc"
        );

        long initialCount = issuePriorityRepository.count();
        IssuePriorityEntity p1 = issuePriorityService.recalculatePriority(issue.getId());
        IssuePriorityEntity p2 = issuePriorityService.recalculatePriority(issue.getId());
        IssuePriorityEntity p3 = issuePriorityService.recalculatePriority(issue.getId());

        assertThat(p1.getId()).isEqualTo(p2.getId());
        assertThat(p2.getId()).isEqualTo(p3.getId());
        assertThat(p1.getScore()).isEqualTo(p3.getScore());
        assertThat(issuePriorityRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("H. Client spoofing defense: client-supplied priority fields in POST /api/issues are ignored")
    void testClientSpoofingDefense() throws Exception {
        String token = jwtService.generateAccessToken(citizen.getId(), citizen.getRole());

        // Client attempts to spoof priorityLevel, score, and component scores
        Map<String, Object> payload = Map.of(
                "title", "Pothole with spoofed priority",
                "description", "Attempting to force CRITICAL priority",
                "categoryId", potholeCategory.getId(),
                "locationId", testLocation.getId(),
                "priorityLevel", "CRITICAL",
                "score", 99,
                "severityScore", 30,
                "supportScore", 10
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").exists())
                // Score is server-calculated, not 99!
                .andExpect(jsonPath("$.priority.score").value(org.hamcrest.Matchers.not(99)));
    }

    @Test
    @DisplayName("I. Resolved issue behavior and NOT_FIXED reactivation")
    void testResolvedAndNotFixedReactivation() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Issue for workflow", "Desc"
        );

        // Transition: REPORTED -> VERIFIED -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED via officerToken
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.VERIFIED, "Verified by officer"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.ACKNOWLEDGED, "Acknowledged"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.IN_PROGRESS, "Under repair"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.RESOLVED, "Repaired"))))
                .andExpect(status().isOk());

        // Historical priority remains preserved
        IssueEntity resolved = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(resolved.getPriority()).isNotNull();

        // Citizen verifies: fixed = false (NOT_FIXED -> returns to IN_PROGRESS)
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/verify-resolution")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyResolutionRequest(false, "Pothole still present"))))
                .andExpect(status().isOk());

        IssueEntity reactivated = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(reactivated.getStatus()).isEqualTo(IssueStatus.NOT_FIXED);
        assertThat(reactivated.getPriority()).isNotNull();
    }

    @Test
    @DisplayName("J. Filter issues by priority level in findIssues")
    void testFilterIssuesByPriority() {
        IssueEntity lowIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Low Issue", "Desc", IssueSeverity.LOW
        );
        lowIssue.setPublicImpact(PublicImpact.LOW);
        lowIssue.setSafetyImpact(SafetyImpact.NONE);
        lowIssue = issueRepository.save(lowIssue);
        issuePriorityService.calculatePriority(lowIssue); // 5 + 5 + 0 + 0 + 0 = 10 -> LOW

        IssueEntity criticalIssue = issueService.createIssue(
                citizen, potholeCategory.getId(), testLocation.getId(), "Critical Issue", "Desc", IssueSeverity.CRITICAL
        );
        criticalIssue.setPublicImpact(PublicImpact.HIGH);
        criticalIssue.setSafetyImpact(SafetyImpact.CRITICAL);
        criticalIssue = issueRepository.save(criticalIssue);
        issuePriorityService.calculatePriority(criticalIssue); // 30 + 25 + 25 + 0 + 0 = 80 -> CRITICAL

        UUID lowId = lowIssue.getId();
        UUID criticalId = criticalIssue.getId();

        Page<IssueResponse> lowPage = issueService.findIssues(null, null, null, PriorityLevel.LOW, org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(lowPage.getContent()).anyMatch(i -> i.id().equals(lowId));
        assertThat(lowPage.getContent()).noneMatch(i -> i.id().equals(criticalId));

        Page<IssueResponse> criticalPage = issueService.findIssues(null, null, null, PriorityLevel.CRITICAL, org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(criticalPage.getContent()).anyMatch(i -> i.id().equals(criticalId));
        assertThat(criticalPage.getContent()).noneMatch(i -> i.id().equals(lowId));
    }
}
