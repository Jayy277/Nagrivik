package org.nagrivic.modules.duplicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.activity.repository.IssueActivityRepository;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.OtpVerificationRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.duplicates.ai.DuplicateAiRequest;
import org.nagrivic.modules.duplicates.ai.LocalSemanticDuplicateAiProvider;
import org.nagrivic.modules.duplicates.dto.CheckDuplicatesRequest;
import org.nagrivic.modules.duplicates.dto.DismissDuplicateSuggestionRequest;
import org.nagrivic.modules.duplicates.entity.AiDuplicateSuggestionEntity;
import org.nagrivic.modules.duplicates.model.DuplicateConfidence;
import org.nagrivic.modules.duplicates.model.DuplicateSuggestionStatus;
import org.nagrivic.modules.duplicates.repository.AiDuplicateSuggestionRepository;
import org.nagrivic.modules.duplicates.service.AiDuplicateDetectionService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class AiDuplicateDetectionTest {

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
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private SupportRepository supportRepository;

    @Autowired
    private IssueActivityRepository issueActivityRepository;

    @Autowired
    private AiDuplicateSuggestionRepository suggestionRepository;

    @Autowired
    private AiDuplicateDetectionService aiDuplicateDetectionService;

    private UserEntity citizenUser;
    private UserEntity moderatorUser;
    private UserEntity officerUser;
    private String citizenToken;
    private String moderatorToken;
    private String officerToken;

    private CategoryEntity roadCategory;
    private CategoryEntity garbageCategory;
    private LocationEntity baseLocation;
    private IssueEntity existingPothole;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanupDatabase();

        citizenUser = userRepository.save(new UserEntity("+919876543201", "Citizen User"));
        citizenToken = jwtService.generateAccessToken(citizenUser.getId(), "CITIZEN");

        moderatorUser = new UserEntity("+919876543202", "Moderator User");
        moderatorUser.setRole("MODERATOR");
        moderatorUser = userRepository.save(moderatorUser);
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), "MODERATOR");

        officerUser = new UserEntity("+919876543203", "Officer User");
        officerUser.setRole("OFFICER");
        officerUser = userRepository.save(officerUser);
        officerToken = jwtService.generateAccessToken(officerUser.getId(), "OFFICER");

        roadCategory = categoryRepository.save(new CategoryEntity("Roads & Potholes", "roads-potholes", "Potholes and broken roads", 1));
        garbageCategory = categoryRepository.save(new CategoryEntity("Solid Waste", "solid-waste", "Garbage overflow", 2));

        baseLocation = locationService.createLocation(23.0225, 72.5714, BigDecimal.valueOf(5.0));

        existingPothole = new IssueEntity(
                citizenUser,
                roadCategory,
                baseLocation,
                "Massive dangerous pothole on CG Road",
                "Deep crater right in front of Municipal Market causing severe hazard"
        );
        existingPothole.setStatus(IssueStatus.REPORTED);
        existingPothole = issueRepository.save(existingPothole);
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
        aiDuplicateDetectionService.setEnabled(false);
    }

    private void cleanupDatabase() {
        suggestionRepository.deleteAll();
        issueActivityRepository.deleteAll();
        supportRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    void testAiDisabled_ReturnsDeterministicResultsWithDisabledStatus() throws Exception {
        aiDuplicateDetectionService.setEnabled(false);

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Large crater on CG road",
                "Dangerous pothole near market",
                roadCategory.getId(),
                baseLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(true))
                .andExpect(jsonPath("$.ai.enabled").value(false))
                .andExpect(jsonPath("$.ai.status").value("DISABLED"))
                .andExpect(jsonPath("$.candidates[0].issueId").value(existingPothole.getId().toString()))
                .andExpect(jsonPath("$.candidates[0].deterministicMatch").value(true))
                .andExpect(jsonPath("$.candidates[0].matchType").value("DETERMINISTIC_MATCH"))
                .andExpect(jsonPath("$.candidates[0].aiScore").doesNotExist());
    }

    @Test
    void testAiEnabled_ComputesSemanticScoreConfidenceAndExplainableSignals() throws Exception {
        aiDuplicateDetectionService.setEnabled(true);

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Massive crater pothole on CG road",
                "Deep crater right in front of Municipal Market",
                roadCategory.getId(),
                baseLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(true))
                .andExpect(jsonPath("$.ai.enabled").value(true))
                .andExpect(jsonPath("$.ai.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.candidates[0].issueId").value(existingPothole.getId().toString()))
                .andExpect(jsonPath("$.candidates[0].deterministicMatch").value(true))
                .andExpect(jsonPath("$.candidates[0].matchType").value("BOTH"))
                .andExpect(jsonPath("$.candidates[0].aiScore", greaterThanOrEqualTo(70)))
                .andExpect(jsonPath("$.candidates[0].confidence", isOneOf("LIKELY", "HIGH")))
                .andExpect(jsonPath("$.candidates[0].signals", hasItem(containsString("Same category"))))
                .andExpect(jsonPath("$.candidates[0].signals", hasItem(containsString("proximity"))));
    }

    @Test
    void testLocalSemanticProvider_ScoringAndConfidenceBrackets() {
        LocalSemanticDuplicateAiProvider provider = new LocalSemanticDuplicateAiProvider("test-model", "v1", 100.0);

        var target = new DuplicateAiRequest.NewIssueInfo(
                "Dangerous pothole on highway",
                "Large crater in lane 2",
                roadCategory.getId(),
                "Roads & Potholes"
        );

        // Identical candidate
        var candidateHigh = new DuplicateAiRequest.CandidateInfo(
                UUID.randomUUID(),
                "Dangerous pothole on highway",
                "Large crater in lane 2",
                roadCategory.getId(),
                "Roads & Potholes",
                2.0
        );

        // Low similarity candidate (far away, different words)
        var candidateLow = new DuplicateAiRequest.CandidateInfo(
                UUID.randomUUID(),
                "Streetlight blinking at corner",
                "Lamp flickers every night",
                roadCategory.getId(),
                "Roads & Potholes",
                95.0
        );

        DuplicateAiRequest request = new DuplicateAiRequest(target, List.of(candidateHigh, candidateLow));
        var result = provider.analyze(request);

        assertEquals(2, result.candidateScores().size());

        var scoreHigh = result.candidateScores().get(0);
        assertTrue(scoreHigh.score() >= 85, "High candidate score should be >= 85");
        assertEquals(DuplicateConfidence.HIGH, scoreHigh.confidence());
        assertFalse(scoreHigh.signals().isEmpty(), "Signals must not be empty");

        var scoreLow = result.candidateScores().get(1);
        assertTrue(scoreLow.score() < 50, "Low candidate score should be < 50");
        assertTrue(scoreLow.confidence() == DuplicateConfidence.LOW || scoreLow.confidence() == DuplicateConfidence.POSSIBLE);
    }

    @Test
    void testCheckDuplicates_DoesNotPersistAnyIssueOrAlterDatabase() throws Exception {
        aiDuplicateDetectionService.setEnabled(true);
        long issueCountBefore = issueRepository.count();

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Pothole report test",
                "Checking duplicates without writing issue",
                roadCategory.getId(),
                baseLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        long issueCountAfter = issueRepository.count();
        assertEquals(issueCountBefore, issueCountAfter, "check-duplicates MUST NOT create issues");
    }

    @Test
    void testAdminDuplicateReview_CitizenForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/duplicates/suggestions")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminDuplicateReview_ModeratorCanViewAndDismiss() throws Exception {
        aiDuplicateDetectionService.setEnabled(true);

        // Create a suggestion record
        AiDuplicateSuggestionEntity suggestion = new AiDuplicateSuggestionEntity(
                existingPothole,
                existingPothole,
                88,
                DuplicateConfidence.HIGH,
                "[\"Same category\", \"Immediate proximity (<5m)\"]",
                "LOCAL_SEMANTIC",
                "test-model",
                "duplicate-v1"
        );
        // Ensure issue and candidate are distinct for valid testing
        IssueEntity secondIssue = new IssueEntity(citizenUser, roadCategory, baseLocation, "Another pothole on CG Road", "Same hole");
        secondIssue = issueRepository.save(secondIssue);
        suggestion.setCandidateIssue(secondIssue);
        suggestion = suggestionRepository.save(suggestion);

        // 1. List suggestions
        mockMvc.perform(get("/api/admin/duplicates/suggestions")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(suggestion.getId().toString()))
                .andExpect(jsonPath("$.content[0].score").value(88))
                .andExpect(jsonPath("$.content[0].confidence").value("HIGH"))
                .andExpect(jsonPath("$.content[0].status").value("SUGGESTED"));

        // 2. Dismiss suggestion
        DismissDuplicateSuggestionRequest dismissReq = new DismissDuplicateSuggestionRequest("Inspected on-site: two separate potholes");

        mockMvc.perform(post("/api/admin/duplicates/suggestions/" + suggestion.getId() + "/dismiss")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dismissReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.dismissReason").value("Inspected on-site: two separate potholes"));

        AiDuplicateSuggestionEntity reloaded = suggestionRepository.findById(suggestion.getId()).orElseThrow();
        assertEquals(DuplicateSuggestionStatus.DISMISSED, reloaded.getStatus());
        assertEquals("Inspected on-site: two separate potholes", reloaded.getDismissReason());
        assertEquals(moderatorUser.getId(), reloaded.getReviewedBy().getId());
    }

    @Test
    void testAdminDuplicateReview_OfficerCanLinkDuplicate() throws Exception {
        aiDuplicateDetectionService.setEnabled(true);

        IssueEntity secondIssue = new IssueEntity(citizenUser, roadCategory, baseLocation, "Duplicate pothole on CG Road", "Crater nearby");
        secondIssue = issueRepository.save(secondIssue);

        AiDuplicateSuggestionEntity suggestion = new AiDuplicateSuggestionEntity(
                secondIssue,
                existingPothole,
                92,
                DuplicateConfidence.HIGH,
                "[\"Same category: Roads & Potholes\", \"Location 5m away\", \"Similar problem description\"]",
                "LOCAL_SEMANTIC",
                "test-model",
                "duplicate-v1"
        );
        suggestion = suggestionRepository.save(suggestion);

        // Link as duplicate
        mockMvc.perform(post("/api/admin/duplicates/suggestions/" + suggestion.getId() + "/link")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDuplicate").value(true))
                .andExpect(jsonPath("$.primaryIssueId").value(existingPothole.getId().toString()));

        // Verify issue entity was updated with duplicate_of_issue_id
        IssueEntity linkedSource = issueRepository.findById(secondIssue.getId()).orElseThrow();
        assertNotNull(linkedSource.getDuplicateOf());
        assertEquals(existingPothole.getId(), linkedSource.getDuplicateOf().getId());

        // Verify suggestion status updated to LINKED
        AiDuplicateSuggestionEntity reloadedSuggestion = suggestionRepository.findById(suggestion.getId()).orElseThrow();
        assertEquals(DuplicateSuggestionStatus.LINKED, reloadedSuggestion.getStatus());
        assertEquals(officerUser.getId(), reloadedSuggestion.getReviewedBy().getId());
    }
}
