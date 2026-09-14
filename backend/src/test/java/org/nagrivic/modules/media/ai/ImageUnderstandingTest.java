package org.nagrivic.modules.media.ai;

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
import org.nagrivic.modules.media.ai.repository.ImageAiAnalysisRepository;
import org.nagrivic.modules.media.ai.service.ImageUnderstandingService;
import org.nagrivic.modules.media.entity.MediaEntity;
import org.nagrivic.modules.media.model.MediaType;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.media.storage.MediaStorageService;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ImageUnderstandingTest {

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
    private MediaRepository mediaRepository;

    @Autowired
    private ImageAiAnalysisRepository aiAnalysisRepository;

    @Autowired
    private ImageUnderstandingService imageUnderstandingService;

    @Autowired
    private IssueActivityRepository issueActivityRepository;

    private UserEntity citizenOwner;
    private UserEntity otherCitizen;
    private UserEntity moderatorUser;
    private UserEntity authorityUser;

    private String citizenOwnerToken;
    private String otherCitizenToken;
    private String moderatorToken;
    private String authorityToken;

    private CategoryEntity roadCategory;
    private CategoryEntity streetlightCategory;
    private CategoryEntity garbageCategory;
    private LocationEntity location;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanupDatabase();

        citizenOwner = userRepository.save(new UserEntity("+919876543201", "Citizen Owner"));
        citizenOwnerToken = jwtService.generateAccessToken(citizenOwner.getId(), "CITIZEN");

        otherCitizen = userRepository.save(new UserEntity("+919876543202", "Other Citizen"));
        otherCitizenToken = jwtService.generateAccessToken(otherCitizen.getId(), "CITIZEN");

        moderatorUser = new UserEntity("+919876543203", "Moderator User");
        moderatorUser.setRole("MODERATOR");
        moderatorUser = userRepository.save(moderatorUser);
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), "MODERATOR");

        authorityUser = new UserEntity("+919876543204", "Authority Officer");
        authorityUser.setRole("OFFICER");
        authorityUser = userRepository.save(authorityUser);
        authorityToken = jwtService.generateAccessToken(authorityUser.getId(), "OFFICER");

        roadCategory = categoryRepository.save(new CategoryEntity("Roads & Potholes", "roads-potholes", "Pothole issues", 1));
        streetlightCategory = categoryRepository.save(new CategoryEntity("Streetlights", "streetlights", "Lighting issues", 2));
        garbageCategory = categoryRepository.save(new CategoryEntity("Garbage & Sanitation", "garbage-sanitation", "Solid waste", 3));

        location = locationService.createLocation(23.0225, 72.5714, BigDecimal.valueOf(5.0));

        // Default test configuration: AI enabled
        imageUnderstandingService.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
        imageUnderstandingService.setEnabled(false);
    }

    private void cleanupDatabase() {
        aiAnalysisRepository.deleteAll();
        mediaRepository.deleteAll();
        issueActivityRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Autowired
    private MediaStorageService mediaStorageService;

    private static final byte[] VALID_JPEG_BYTES = new byte[2048];
    static {
        byte[] header = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
        System.arraycopy(header, 0, VALID_JPEG_BYTES, 0, header.length);
    }

    private MediaEntity createTestMedia(IssueEntity issue, String filename, String storagePath) {
        mediaStorageService.store(storagePath, new java.io.ByteArrayInputStream(VALID_JPEG_BYTES), VALID_JPEG_BYTES.length, "image/jpeg");
        MediaEntity media = new MediaEntity(
                issue,
                storagePath,
                filename,
                "image/jpeg",
                (long) VALID_JPEG_BYTES.length,
                MediaType.IMAGE,
                0
        );
        return mediaRepository.save(media);
    }

    @Test
    void testAnalyzeImage_WhenAiDisabled_ReturnsUnavailableWithoutError() throws Exception {
        imageUnderstandingService.setEnabled(false);

        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Broken asphalt on Drive In Road",
                "Deep pothole near helmet circle"
        );

        MediaEntity media = createTestMedia(issue, "pothole_cracked.jpg", "uploads/issues/" + issue.getId() + "/pothole.jpg");

        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + citizenOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.provider").value("DISABLED"))
                .andExpect(jsonPath("$.mediaId").value(media.getId().toString()))
                .andExpect(jsonPath("$.issueId").value(issue.getId().toString()));

        // Check issue was not touched
        IssueEntity unchangedIssue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueStatus.REPORTED, unchangedIssue.getStatus());
        assertEquals(roadCategory.getId(), unchangedIssue.getCategory().getId());
    }

    @Test
    void testAnalyzeImage_WhenAiEnabled_ReturnsVisualSignals() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Large crater pothole on CG road",
                "Massive road damage causing accidents"
        );

        MediaEntity media = createTestMedia(issue, "crater_pothole_obstruction.jpg", "uploads/issues/" + issue.getId() + "/photo1.jpg");

        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + citizenOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.provider").value("LOCAL_HEURISTIC"))
                .andExpect(jsonPath("$.likelyCategory").value("ROADS_POTHOLES"))
                .andExpect(jsonPath("$.categoryConfidence").isNumber())
                .andExpect(jsonPath("$.categoryConfidence", greaterThanOrEqualTo(70)))
                .andExpect(jsonPath("$.imageQuality").value("GOOD"))
                .andExpect(jsonPath("$.relevance").value("LIKELY_RELEVANT"))
                .andExpect(jsonPath("$.visualProblemTypes", hasItem("POTHOLE")))
                .andExpect(jsonPath("$.summary").isNotEmpty());

        // Verify entity persisted in DB
        ImageAiAnalysisEntity analysis = aiAnalysisRepository.findByMediaId(media.getId()).orElse(null);
        assertNotNull(analysis);
        assertEquals(ImageAnalysisStatus.COMPLETED, analysis.getStatus());
        assertEquals(CivicVisualCategory.ROADS_POTHOLES, analysis.getLikelyCategory());
    }

    @Test
    void testAnalyzeImage_CategoryMismatchSuggestion_DoesNotMutateIssueCategory() throws Exception {
        // Issue is filed under STREETLIGHTS
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                streetlightCategory.getId(),
                location.getId(),
                "Overflowing waste dump",
                "Heaps of uncollected garbage and waste bin spillover"
        );

        // Media indicates GARBAGE
        MediaEntity media = createTestMedia(issue, "garbage_dump_spill.jpg", "uploads/issues/" + issue.getId() + "/garbage.jpg");

        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.likelyCategory").value("GARBAGE"))
                .andExpect(jsonPath("$.visualProblemTypes", hasItem("GARBAGE_PILE")));

        // Critical check: Issue category MUST REMAIN STREETLIGHTS! AI is assistive only
        IssueEntity verifiedIssue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(streetlightCategory.getId(), verifiedIssue.getCategory().getId(), "Issue category MUST NOT be mutated by AI");
    }

    @Test
    void testAnalyzeImage_NonMutation_IssueStatusAndPriorityUntouched() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Broken road with severe hazard",
                "Pothole hazard requiring urgent attention"
        );

        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setStatus(IssueStatus.REPORTED);
        issueRepository.save(issue);

        MediaEntity media = createTestMedia(issue, "broken_pothole_hazard.jpg", "uploads/issues/" + issue.getId() + "/hazard.jpg");

        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + authorityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Status and severity must be identical
        IssueEntity currentIssue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueStatus.REPORTED, currentIssue.getStatus(), "Issue status must not be modified by AI");
        assertEquals(IssueSeverity.MEDIUM, currentIssue.getSeverity(), "Issue severity must not be modified by AI");
    }

    @Test
    void testAnalyzeImage_ZeroPii_ReporterInfoNeverLeakedInAnalysis() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Drainage overflow on sidewalk",
                "Water logging and blocked drain"
        );

        MediaEntity media = createTestMedia(issue, "water_logging_drain.jpg", "uploads/issues/" + issue.getId() + "/water.jpg");

        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + citizenOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensitiveVisualContentDetected").value(false))
                .andExpect(jsonPath("$.summary", not(containsString("+919876543201"))))
                .andExpect(jsonPath("$.summary", not(containsString("Citizen Owner"))));
    }

    @Test
    void testAnalyzeImage_Idempotency_SecondCallReturnsExistingAnalysis() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Road pothole",
                "Pothole issue on road"
        );

        MediaEntity media = createTestMedia(issue, "pothole_test.jpg", "uploads/issues/" + issue.getId() + "/pothole.jpg");

        // First call - analyzes and persists
        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + citizenOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        long countAfterFirst = aiAnalysisRepository.count();
        assertEquals(1, countAfterFirst);

        // Second call - returns existing analysis, no duplicate rows created
        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + citizenOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertEquals(1, aiAnalysisRepository.count(), "Idempotent analysis must not duplicate records");
    }

    @Test
    void testGetAnalysis_ReturnsExistingResultOrNotFound() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Dark street without light",
                "Dark streetlight pole damaged"
        );

        MediaEntity media = createTestMedia(issue, "damaged_pole_dark.jpg", "uploads/issues/" + issue.getId() + "/pole.jpg");

        // Before analysis, GET returns 404
        mockMvc.perform(get("/api/issues/{issueId}/media/{mediaId}/analysis", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + citizenOwnerToken))
                .andExpect(status().isNotFound());

        // Run analysis
        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + citizenOwnerToken))
                .andExpect(status().isOk());

        // After analysis, GET returns 200
        mockMvc.perform(get("/api/issues/{issueId}/media/{mediaId}/analysis", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + authorityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likelyCategory").value("STREETLIGHTS"))
                .andExpect(jsonPath("$.visualProblemTypes", hasItem("DAMAGED_LIGHT_POLE")));
    }

    @Test
    void testAnalyzeImage_Rbac_OtherCitizenDenied() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenOwner,
                roadCategory.getId(),
                location.getId(),
                "Road pothole",
                "Description"
        );

        MediaEntity media = createTestMedia(issue, "photo.jpg", "uploads/issues/" + issue.getId() + "/photo.jpg");

        // Other citizen attempts to trigger analysis -> 403 Forbidden
        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + otherCitizenToken))
                .andExpect(status().isForbidden());

        // Other citizen attempts to read analysis -> 403 Forbidden
        mockMvc.perform(get("/api/issues/{issueId}/media/{mediaId}/analysis", issue.getId(), media.getId())
                        .header("Authorization", "Bearer " + otherCitizenToken))
                .andExpect(status().isForbidden());

        // Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(post("/api/issues/{issueId}/media/{mediaId}/analyze", issue.getId(), media.getId()))
                .andExpect(status().isUnauthorized());
    }
}
