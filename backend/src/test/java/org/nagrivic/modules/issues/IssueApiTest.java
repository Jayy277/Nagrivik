package org.nagrivic.modules.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.OtpVerificationRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.dto.LocationPayload;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class IssueApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    private UserEntity testUser;
    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(new UserEntity("+919876543210", "Aarav Patel"));
        testCategory = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @AfterEach
    void tearDown() {
        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String tokenFor(UserEntity user) {
        return jwtService.generateAccessToken(user.getId(), user.getRole());
    }

    // ==========================================
    // 1. AUTHENTICATION & SECURITY (POST /api/issues)
    // ==========================================

    @Test
    void shouldRejectCreateIssueWithoutJwt() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole on Highway",
                "Deep pothole",
                testCategory.getId(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectCreateIssueWithInvalidJwt() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole on Highway",
                "Deep pothole",
                testCategory.getId(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer invalid-tampered-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectCreateIssueWithExpiredJwt() throws Exception {
        // Build an expired token using expired timestamp logic or malformed claims
        String expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJlYmFmZWE2Ny1lMzA3LTRiOWEtYjcyMy04OTQyYjhjMDJmNmMiLCJyb2xlIjoiQ0lUSVpFTiIsImlhdCI6MTYwMDAwMDAwMCwiZXhwIjoxNjAwMDAwOTAwfQ.dummy";

        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole on Highway",
                "Deep pothole",
                testCategory.getId(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldRejectCreateIssueWhenAuthenticatedUserIsInactive() throws Exception {
        testUser.setActive(false);
        userRepository.save(testUser);

        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole on Highway",
                "Deep pothole",
                testCategory.getId(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message", containsString("inactive")));
    }

    // ==========================================
    // 2. CREATE ISSUE WITH AUTHENTICATED USER
    // ==========================================

    @Test
    void shouldCreateIssueAndReturn201WithLocationHeader() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Large pothole near SG Highway",
                "Deep pothole in the left lane causing traffic slowdown.",
                testCategory.getId(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/issues/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Large pothole near SG Highway"))
                .andExpect(jsonPath("$.description").value("Deep pothole in the left lane causing traffic slowdown."))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.reportedBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.category.id").value(testCategory.getId().toString()))
                .andExpect(jsonPath("$.category.name").value("Roads / Potholes"))
                .andExpect(jsonPath("$.category.slug").value("roads-potholes"))
                .andExpect(jsonPath("$.location.id").value(testLocation.getId().toString()))
                .andExpect(jsonPath("$.location.latitude").value(23.0225))
                .andExpect(jsonPath("$.location.longitude").value(72.5714))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void shouldCreateIssueWithInlineLocationCoordinates() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole with inline GPS",
                "Coordinates provided directly by mobile GPS",
                testCategory.getId(),
                new LocationPayload(23.0225, 72.5714, 14.5)
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Pothole with inline GPS"))
                .andExpect(jsonPath("$.location.latitude").value(23.0225))
                .andExpect(jsonPath("$.location.longitude").value(72.5714))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    @Test
    void shouldRejectCreateIssueWithInvalidInlineCoordinates() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole with invalid GPS",
                "Latitude out of bounds",
                testCategory.getId(),
                new LocationPayload(195.0, 72.5714, 14.5)
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreventUserImpersonationWhenClientAttemptsToSupplyReportedBy() throws Exception {
        UserEntity victimUser = userRepository.save(new UserEntity("+919111111111", "Victim User"));

        // User A (testUser) authenticates, but request payload includes "reportedBy" pointing to User B (victimUser)
        Map<String, Object> payloadWithImpersonation = Map.of(
                "title", "Pothole attempt with fake reporter",
                "description", "Attempting to report on behalf of another user",
                "categoryId", testCategory.getId().toString(),
                "locationId", testLocation.getId().toString(),
                "reportedBy", victimUser.getId().toString()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadWithImpersonation)))
                .andExpect(status().isCreated())
                // Verify reporter is strictly User A (testUser), NOT User B (victimUser)
                .andExpect(jsonPath("$.reportedBy").value(testUser.getId().toString()));

        // Double check database state
        IssueEntity savedIssue = issueRepository.findAll().get(0);
        assertEquals(testUser.getId(), savedIssue.getReporter().getId());
    }

    // ==========================================
    // 3. VALIDATION TESTS
    // ==========================================

    @Test
    void shouldRejectCreateIssueWithMissingTitle() throws Exception {
        Map<String, Object> body = Map.of(
                "description", "Some description",
                "categoryId", testCategory.getId().toString(),
                "locationId", testLocation.getId().toString()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("title")));
    }

    @Test
    void shouldRejectCreateIssueWithBlankTitle() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "    ",
                "Some description",
                testCategory.getId(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("title")));
    }

    @Test
    void shouldRejectCreateIssueWithMissingCategory() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Valid Title",
                "locationId", testLocation.getId().toString()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("categoryId")));
    }

    @Test
    void shouldRejectCreateIssueWithNonExistentCategory() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Description",
                UUID.randomUUID(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Category not found")));
    }

    @Test
    void shouldRejectCreateIssueWithInactiveCategory() throws Exception {
        CategoryEntity inactiveCategory = new CategoryEntity("Deprecated", "deprecated-cat", "Old category", 99);
        inactiveCategory.setActive(false);
        inactiveCategory = categoryRepository.save(inactiveCategory);

        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Description",
                inactiveCategory.getId(),
                testLocation.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Cannot report an issue under an inactive category")));
    }

    @Test
    void shouldRejectCreateIssueWithNonExistentLocation() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Valid Title",
                "Description",
                testCategory.getId(),
                UUID.randomUUID()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Location not found")));
    }

    @Test
    void shouldIgnoreOrDisallowClientSuppliedStatusAndAlwaysSetReported() throws Exception {
        Map<String, Object> bodyWithStatus = Map.of(
                "title", "Pothole report",
                "description", "Pothole details",
                "categoryId", testCategory.getId().toString(),
                "locationId", testLocation.getId().toString(),
                "status", "RESOLVED"
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenFor(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWithStatus)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    // ==========================================
    // 4. PUBLIC GET ISSUE BY ID (GET /api/issues/{id})
    // ==========================================

    @Test
    void shouldGetIssueByIdPubliclyWithoutAuthentication() throws Exception {
        IssueEntity issue = issueService.createIssue(
                testUser.getId(),
                testCategory.getId(),
                testLocation.getId(),
                "Streetlight broken",
                "Streetlight #42 is flickering and dark at night"
        );

        // No Authorization header
        mockMvc.perform(get("/api/issues/{id}", issue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(issue.getId().toString()))
                .andExpect(jsonPath("$.title").value("Streetlight broken"))
                .andExpect(jsonPath("$.description").value("Streetlight #42 is flickering and dark at night"))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.reportedBy").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.category.name").value("Roads / Potholes"))
                .andExpect(jsonPath("$.location.latitude").value(23.0225));
    }

    @Test
    void shouldReturn404ForNonExistentIssueId() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/issues/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("Issue not found with identifier: " + nonExistentId)))
                .andExpect(jsonPath("$.path").value("/api/issues/" + nonExistentId));
    }

    @Test
    void shouldReturn400ForMalformedIssueId() throws Exception {
        mockMvc.perform(get("/api/issues/{id}", "not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("Invalid parameter: 'id'")));
    }

    // ==========================================
    // 5. PUBLIC LIST ISSUES (GET /api/issues)
    // ==========================================

    @Test
    void shouldListIssuesPubliclyWithoutAuthentication() throws Exception {
        IssueEntity issue1 = issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue 1", "Desc 1");
        Thread.sleep(10);
        IssueEntity issue2 = issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue 2", "Desc 2");

        // No Authorization header
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(issue2.getId().toString())) // Newest first
                .andExpect(jsonPath("$.content[1].id").value(issue1.getId().toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void shouldListIssuesWithCustomPagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Issue " + i, "Desc " + i);
        }

        mockMvc.perform(get("/api/issues?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void shouldClampMaxPageSizeTo100() throws Exception {
        mockMvc.perform(get("/api/issues?size=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void shouldFilterIssuesByCategory() throws Exception {
        CategoryEntity waterCategory = categoryRepository.save(new CategoryEntity("Water", "water", "Water issues", 2));

        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Road Issue", "Desc");
        issueService.createIssue(testUser.getId(), waterCategory.getId(), testLocation.getId(), "Water Pipe Leak", "Desc");

        mockMvc.perform(get("/api/issues?categoryId=" + waterCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Water Pipe Leak"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterIssuesByReporter() throws Exception {
        UserEntity otherUser = userRepository.save(new UserEntity("+919999999999", "Other User"));

        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "My Issue", "Desc");
        issueService.createIssue(otherUser.getId(), testCategory.getId(), testLocation.getId(), "Other's Issue", "Desc");

        mockMvc.perform(get("/api/issues?reportedBy=" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("My Issue"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterIssuesByStatus() throws Exception {
        issueService.createIssue(testUser.getId(), testCategory.getId(), testLocation.getId(), "Reported Issue", "Desc");

        mockMvc.perform(get("/api/issues?status=REPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Reported Issue"));

        mockMvc.perform(get("/api/issues?status=RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ==========================================
    // 6. SECURITY & HYGIENE CHECKS
    // ==========================================

    @Test
    void shouldNeverExposeSensitiveUserFieldsInIssueResponses() throws Exception {
        IssueEntity issue = issueService.createIssue(
                testUser.getId(),
                testCategory.getId(),
                testLocation.getId(),
                "Public report",
                "No private info"
        );

        mockMvc.perform(get("/api/issues/{id}", issue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportedBy").value(testUser.getId().toString()))
                // Assert no phone number, password, or profile information leaked
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.rawGeometry").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }
}
