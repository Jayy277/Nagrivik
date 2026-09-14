package org.nagrivic.modules.priority.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.activity.repository.IssueActivityRepository;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.ai.entity.ImageAiAnalysisEntity;
import org.nagrivic.modules.media.ai.model.CivicVisualCategory;
import org.nagrivic.modules.media.ai.model.ImageAnalysisStatus;
import org.nagrivic.modules.media.ai.model.VisualSafetyConcern;
import org.nagrivic.modules.media.ai.repository.ImageAiAnalysisRepository;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.priority.ai.dto.PriorityAiRequest;
import org.nagrivic.modules.priority.ai.dto.PriorityAiResult;
import org.nagrivic.modules.priority.ai.entity.IssueAiPriorityEntity;
import org.nagrivic.modules.priority.ai.model.PriorityAiStatus;
import org.nagrivic.modules.priority.ai.model.PriorityInfluenceMode;
import org.nagrivic.modules.priority.ai.provider.DisabledPriorityAiProvider;
import org.nagrivic.modules.priority.ai.provider.LocalHeuristicPriorityAiProvider;
import org.nagrivic.modules.priority.ai.repository.IssueAiPriorityRepository;
import org.nagrivic.modules.priority.ai.service.PriorityAiService;
import org.nagrivic.modules.priority.entity.IssuePriorityEntity;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.priority.model.PublicImpact;
import org.nagrivic.modules.priority.model.SafetyImpact;
import org.nagrivic.modules.priority.repository.IssuePriorityRepository;
import org.nagrivic.modules.priority.service.IssuePriorityService;
import org.nagrivic.modules.supports.entity.SupportEntity;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PriorityAiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssuePriorityService issuePriorityService;

    @Autowired
    private IssuePriorityRepository issuePriorityRepository;

    @Autowired
    private IssueAiPriorityRepository aiPriorityRepository;

    @Autowired
    private PriorityAiService priorityAiService;

    @Autowired
    private ImageAiAnalysisRepository imageAiAnalysisRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private SupportRepository supportRepository;

    @Autowired
    private IssueActivityRepository issueActivityRepository;

    private UserEntity citizenOwner;
    private UserEntity otherCitizen;
    private UserEntity officerUser;
    private UserEntity moderatorUser;

    private String citizenToken;
    private String otherCitizenToken;
    private String officerToken;
    private String moderatorToken;

    private CategoryEntity roadCategory;
    private LocationEntity location;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanupDatabase();

        citizenOwner = userRepository.save(new UserEntity("+919876543201", "Citizen Reporter"));
        citizenToken = jwtService.generateAccessToken(citizenOwner.getId(), "CITIZEN");

        otherCitizen = userRepository.save(new UserEntity("+919876543202", "Other Citizen"));
        otherCitizenToken = jwtService.generateAccessToken(otherCitizen.getId(), "CITIZEN");

        officerUser = new UserEntity("+919876543203", "Authority Officer");
        officerUser.setRole("OFFICER");
        officerUser = userRepository.save(officerUser);
        officerToken = jwtService.generateAccessToken(officerUser.getId(), "OFFICER");

        moderatorUser = new UserEntity("+919876543204", "Moderator User");
        moderatorUser.setRole("MODERATOR");
        moderatorUser = userRepository.save(moderatorUser);
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), "MODERATOR");

        roadCategory = categoryRepository.save(new CategoryEntity("Roads & Potholes", "roads-potholes", "Pothole issues", 1));
        location = locationService.createLocation(23.0225, 72.5714, BigDecimal.valueOf(5.0));

        // Default test state: AI enabled in advisory mode
        priorityAiService.setEnabled(true);
        priorityAiService.setInfluenceMode(PriorityInfluenceMode.ADVISORY);
        priorityAiService.setConfidenceThreshold(70);
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
        priorityAiService.setEnabled(false);
        priorityAiService.setInfluenceMode(PriorityInfluenceMode.ADVISORY);
    }

    private void cleanupDatabase() {
        aiPriorityRepository.deleteAll();
        imageAiAnalysisRepository.deleteAll();
        mediaRepository.deleteAll();
        supportRepository.deleteAll();
        issueActivityRepository.deleteAll();
        issuePriorityRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testLocalHeuristicProvider_ProducesBoundedOutputs() {
        LocalHeuristicPriorityAiProvider provider = new LocalHeuristicPriorityAiProvider("heuristic-priority-v1", "1.0.0", "v1");

        PriorityAiRequest request = new PriorityAiRequest(
                UUID.randomUUID(),
                "Massive dangerous pothole near market junction",
                "Deep crater on highway causing severe vehicle accidents and injury hazard",
                "roads-potholes",
                "Roads & Potholes",
                IssueSeverity.MEDIUM,
                PublicImpact.LOW,
                SafetyImpact.LOW,
                List.of("POTHOLE"),
                List.of("DEEP_POTHOLE", "LARGE_ROAD_OBSTRUCTION"),
                VisualSafetyConcern.HIGH
        );

        PriorityAiResult result = provider.analyze(request);

        assertEquals(PriorityAiStatus.COMPLETED, result.status());
        assertNotNull(result.suggestedSeverity());
        assertNotNull(result.suggestedImpact());
        assertNotNull(result.suggestedSafety());
        assertNotNull(result.confidence());

        // Bounds enforcement
        assertTrue(result.suggestedSeverity() >= 0 && result.suggestedSeverity() <= 30);
        assertTrue(result.suggestedImpact() >= 0 && result.suggestedImpact() <= 25);
        assertTrue(result.suggestedSafety() >= 0 && result.suggestedSafety() <= 25);
        assertTrue(result.confidence() >= 0 && result.confidence() <= 100);

        // Signal assertions
        assertFalse(result.signals().isEmpty());
        assertTrue(result.signals().stream().anyMatch(s -> s.contains("Task 47") || s.contains("hazard") || s.contains("image")));
    }

    @Test
    void testDisabledProvider_ReturnsUnavailableWithoutError() {
        DisabledPriorityAiProvider provider = new DisabledPriorityAiProvider("v1");
        PriorityAiResult result = provider.analyze(null);

        assertEquals(PriorityAiStatus.UNAVAILABLE, result.status());
        assertEquals("DISABLED", result.provider());
        assertNull(result.suggestedSeverity());
    }

    @Test
    void testAssessPriority_WhenAiDisabled_ReturnsUnavailableWithoutMutatingPriority() throws Exception {
        priorityAiService.setEnabled(false);

        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Road pothole",
                "Standard road problem"
        );

        mockMvc.perform(post("/api/issues/{issueId}/priority/ai-assess", issue.getId())
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.provider").value("DISABLED"))
                .andExpect(jsonPath("$.appliedToCalculation").value(false));

        // Deterministic priority remains valid
        IssuePriorityEntity priority = issuePriorityRepository.findByIssue_Id(issue.getId()).orElseThrow();
        assertEquals("v1", priority.getCalculationVersion());
        assertEquals(PriorityLevel.MEDIUM, priority.getPriorityLevel()); // medium(15) + low(5) + low(5) = 25 -> MEDIUM
    }

    @Test
    void testAssessPriority_ConsumesTask47StructuredVisualSignals() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Broken asphalt road",
                "Damaged road surface near crossroad"
        );

        // Attach media
        MediaEntity media = new MediaEntity(
                issue,
                "uploads/issues/" + issue.getId() + "/photo.jpg",
                "photo.jpg",
                "image/jpeg",
                2048L,
                MediaType.IMAGE,
                0
        );
        media = mediaRepository.save(media);

        // Mock Task 47 completed image analysis in DB
        ImageAiAnalysisEntity imgAnalysis = new ImageAiAnalysisEntity(
                media,
                issue,
                "LOCAL_HEURISTIC",
                "heuristic-vision-v1",
                "1.0.0",
                "image-understanding-v1",
                ImageAnalysisStatus.COMPLETED
        );
        imgAnalysis.setLikelyCategory(CivicVisualCategory.ROADS_POTHOLES);
        imgAnalysis.setCategoryConfidence(90);
        imgAnalysis.setVisualProblemTypes("[\"POTHOLE\", \"ROAD_CAVE_IN\"]");
        imgAnalysis.setVisualSeveritySignals("[\"DEEP_POTHOLE\", \"LARGE_ROAD_OBSTRUCTION\"]");
        imgAnalysis.setSafetyConcern(VisualSafetyConcern.HIGH);
        imageAiAnalysisRepository.save(imgAnalysis);

        // Run priority AI assessment
        mockMvc.perform(post("/api/issues/{issueId}/priority/ai-assess", issue.getId())
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.suggestedSeverity", greaterThanOrEqualTo(25)))
                .andExpect(jsonPath("$.suggestedSafety", greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.confidence", greaterThanOrEqualTo(80)))
                .andExpect(jsonPath("$.signals", hasItem(containsString("Task 47"))));
    }

    @Test
    void testAntiCriticalEscalation_AiAloneCannotForceCritical() {
        priorityAiService.setInfluenceMode(PriorityInfluenceMode.BLENDED);
        priorityAiService.setEnabled(true);
        priorityAiService.setConfidenceThreshold(70);

        IssueEntity issue = new IssueEntity(citizenOwner, roadCategory, location, "Title", "Desc");
        issue.setId(UUID.randomUUID());

        // Create high AI recommendation
        IssueAiPriorityEntity rec = new IssueAiPriorityEntity(issue, "LOCAL", "v1", "1.0", "v1", PriorityAiStatus.COMPLETED);
        rec.setSuggestedSeverity(30);
        rec.setSuggestedImpact(25);
        rec.setSuggestedSafety(25);
        rec.setConfidence(95);

        // Case A: Deterministic baseline is 70 (HIGH, but NOT critical < 75)
        // Even with max positive adjustment, blended score MUST BE CAPPED AT 74!
        int blended = priorityAiService.calculateBlendedScore(70, 15, 10, 5, rec);
        assertEquals(74, blended, "AI alone MUST NEVER escalate a non-critical baseline into CRITICAL (capped at 74)");

        // Case B: Deterministic baseline is ALREADY 78 (CRITICAL)
        // Blended score can remain CRITICAL because human/deterministic signals already reached CRITICAL
        int criticalBlended = priorityAiService.calculateBlendedScore(78, 25, 20, 20, rec);
        assertTrue(criticalBlended >= 75, "Deterministic critical baseline is preserved");
        assertTrue(criticalBlended <= 100, "Score strictly bounded by 100");
    }

    @Test
    void testBlendedMode_AdjustmentCap_StrictlyBoundedToMax10Points() {
        priorityAiService.setInfluenceMode(PriorityInfluenceMode.BLENDED);
        priorityAiService.setEnabled(true);

        IssueEntity issue = new IssueEntity(citizenOwner, roadCategory, location, "Title", "Desc");
        issue.setId(UUID.randomUUID());

        IssueAiPriorityEntity rec = new IssueAiPriorityEntity(issue, "LOCAL", "v1", "1.0", "v1", PriorityAiStatus.COMPLETED);
        rec.setSuggestedSeverity(30); // delta +25 from 5, capped at +5
        rec.setSuggestedImpact(25);   // delta +20 from 5, capped at +4
        rec.setSuggestedSafety(25);   // delta +25 from 0, capped at +4
        rec.setConfidence(90);

        // Baseline: 5 + 5 + 0 + 0 + 0 = 10
        int deterministicBaseline = 10;
        int blended = priorityAiService.calculateBlendedScore(deterministicBaseline, 5, 5, 0, rec);

        // Total adjustment cap is max ±10 points
        assertEquals(20, blended, "Total AI adjustment must be capped at max +10 points (10 + 10 = 20)");
    }

    @Test
    void testLowConfidenceAi_DoesNotInfluenceScoreInBlendedMode() {
        priorityAiService.setInfluenceMode(PriorityInfluenceMode.BLENDED);
        priorityAiService.setEnabled(true);
        priorityAiService.setConfidenceThreshold(75);

        IssueEntity issue = new IssueEntity(citizenOwner, roadCategory, location, "Title", "Desc");
        issue.setId(UUID.randomUUID());

        IssueAiPriorityEntity rec = new IssueAiPriorityEntity(issue, "LOCAL", "v1", "1.0", "v1", PriorityAiStatus.COMPLETED);
        rec.setSuggestedSeverity(30);
        rec.setSuggestedImpact(25);
        rec.setSuggestedSafety(25);
        rec.setConfidence(60); // Below 75 threshold

        int deterministicBaseline = 40;
        int blended = priorityAiService.calculateBlendedScore(deterministicBaseline, 15, 10, 5, rec);

        assertEquals(40, blended, "Low confidence AI (< threshold) must not modify the deterministic score");
    }

    @Test
    void testDeterministicComponents_AgeAndSupportNeverAlteredByAi() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Hazardous pothole",
                "Deep crater on Drive In Road"
        );

        // Add 5 supports
        for (int i = 1; i <= 5; i++) {
            UserEntity supporter = userRepository.save(new UserEntity("+9198765432" + (10 + i), "Supporter " + i));
            supportRepository.save(new SupportEntity(issue, supporter));
        }

        // Deterministic support calculation: 5 supports -> support_score = 4
        issuePriorityService.calculatePriority(issue);
        IssuePriorityEntity baseline = issuePriorityRepository.findByIssue_Id(issue.getId()).orElseThrow();
        assertEquals(4, baseline.getSupportScore(), "5 supports must yield deterministic score 4");

        // Run AI assessment in BLENDED mode
        priorityAiService.setInfluenceMode(PriorityInfluenceMode.BLENDED);
        priorityAiService.setEnabled(true);
        priorityAiService.assessPriority(issue.getId(), officerUser.getId(), "OFFICER");

        issuePriorityService.calculatePriority(issue);
        IssuePriorityEntity afterAi = issuePriorityRepository.findByIssue_Id(issue.getId()).orElseThrow();

        // Support and age scores MUST REMAIN IDENTICAL
        assertEquals(4, afterAi.getSupportScore(), "AI must never alter support score");
        assertEquals(baseline.getAgeScore(), afterAi.getAgeScore(), "AI must never alter age score");
    }

    @Test
    void testGetRecommendation_Rbac_CitizenForbiddenFromOtherIssues() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Road defect",
                "Description"
        );

        priorityAiService.assessPriority(issue.getId(), officerUser.getId(), "OFFICER");

        // Owner CAN inspect recommendation
        mockMvc.perform(get("/api/issues/{issueId}/priority/ai-recommendation", issue.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Officer CAN inspect recommendation
        mockMvc.perform(get("/api/issues/{issueId}/priority/ai-recommendation", issue.getId())
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        // Other citizen FORBIDDEN
        mockMvc.perform(get("/api/issues/{issueId}/priority/ai-recommendation", issue.getId())
                        .header("Authorization", "Bearer " + otherCitizenToken))
                .andExpect(status().isForbidden());

        // Unauthenticated -> 403 Forbidden by method security
        mockMvc.perform(get("/api/issues/{issueId}/priority/ai-recommendation", issue.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAssessPriority_Rbac_OrdinaryCitizenCannotTriggerAssessment() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Road defect",
                "Description"
        );

        // Ordinary citizen cannot trigger reassessment -> 403 Forbidden
        mockMvc.perform(post("/api/issues/{issueId}/priority/ai-assess", issue.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // Officer CAN trigger reassessment -> 200 OK
        mockMvc.perform(post("/api/issues/{issueId}/priority/ai-assess", issue.getId())
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }
}
