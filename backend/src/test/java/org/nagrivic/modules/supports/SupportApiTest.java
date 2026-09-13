package org.nagrivic.modules.supports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.repository.MediaRepository;
import org.nagrivic.modules.supports.entity.SupportEntity;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.supports.service.SupportService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SupportApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SupportRepository supportRepository;

    @Autowired
    private SupportService supportService;

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
    private MediaRepository mediaRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    private MockMvc mockMvc;

    private UserEntity citizen1;
    private UserEntity citizen2;
    private UserEntity inactiveCitizen;
    private IssueEntity testIssue;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        supportRepository.deleteAll();
        mediaRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        userRepository.deleteAll();

        citizen1 = userRepository.save(new UserEntity("+919876543210", "Aarav Patel"));
        citizen2 = userRepository.save(new UserEntity("+919876543211", "Rohan Sharma"));

        inactiveCitizen = new UserEntity("+919876543212", "Inactive Citizen");
        inactiveCitizen.setActive(false);
        inactiveCitizen = userRepository.save(inactiveCitizen);

        CategoryEntity category = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        LocationEntity location = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));

        testIssue = issueService.createIssue(
                citizen1,
                category.getId(),
                location.getId(),
                "Large pothole near SG Highway",
                "Deep pothole causing vehicle damage"
        );
    }

    private String tokenFor(UserEntity user) {
        return jwtService.generateAccessToken(user.getId(), user.getRole());
    }

    // ==========================================
    // 1. BASIC SUPPORT & REMOVAL FLOW
    // ==========================================

    @Test
    void authenticatedUserCanSupportIssue() throws Exception {
        String token = tokenFor(citizen1);

        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        assertEquals(1, supportRepository.countByIssue_Id(testIssue.getId()));
        assertTrue(supportRepository.existsByIssue_IdAndUser_Id(testIssue.getId(), citizen1.getId()));
    }

    @Test
    void userCanRemoveTheirSupport() throws Exception {
        String token = tokenFor(citizen1);

        // Support first
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        assertEquals(1, supportRepository.countByIssue_Id(testIssue.getId()));

        // Remove support
        mockMvc.perform(delete("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertEquals(0, supportRepository.countByIssue_Id(testIssue.getId()));
        assertFalse(supportRepository.existsByIssue_IdAndUser_Id(testIssue.getId(), citizen1.getId()));
    }

    @Test
    void removingNonExistentSupportReturns404() throws Exception {
        String token = tokenFor(citizen1);

        mockMvc.perform(delete("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("not supported this issue")));
    }

    // ==========================================
    // 2. DUPLICATE SUPPORT PREVENTION
    // ==========================================

    @Test
    void duplicateSupportBySameUserReturns409Conflict() throws Exception {
        String token = tokenFor(citizen1);

        // First support
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // Duplicate support attempt
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("already supported this issue")));

        assertEquals(1, supportRepository.countByIssue_Id(testIssue.getId()));
    }

    @Test
    void databaseLevelUniqueConstraintPreventsDuplicateRecords() {
        SupportEntity support1 = new SupportEntity(testIssue, citizen1);
        supportRepository.saveAndFlush(support1);

        SupportEntity duplicate = new SupportEntity(testIssue, citizen1);
        assertThrows(DataIntegrityViolationException.class, () -> {
            supportRepository.saveAndFlush(duplicate);
        });
    }

    // ==========================================
    // 3. MULTIPLE USERS & AUTHORIZATION
    // ==========================================

    @Test
    void multipleDifferentUsersCanSupportSameIssue() throws Exception {
        String token1 = tokenFor(citizen1);
        String token2 = tokenFor(citizen2);

        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isCreated());

        assertEquals(2, supportRepository.countByIssue_Id(testIssue.getId()));
    }

    @Test
    void userCannotRemoveAnotherUsersSupport() throws Exception {
        String token1 = tokenFor(citizen1);
        String token2 = tokenFor(citizen2);

        // Citizen 1 supports
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isCreated());

        // Citizen 2 tries to remove support (they have not supported it)
        mockMvc.perform(delete("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));

        // Citizen 1's support remains intact
        assertEquals(1, supportRepository.countByIssue_Id(testIssue.getId()));
        assertTrue(supportRepository.existsByIssue_IdAndUser_Id(testIssue.getId(), citizen1.getId()));
    }

    @Test
    void inactiveUserCannotSupportIssue() throws Exception {
        String token = tokenFor(inactiveCitizen);

        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("User account is inactive")));
    }

    // ==========================================
    // 4. AUTHENTICATION & LOOKUP
    // ==========================================

    @Test
    void unauthenticatedSupportShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtSupportShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer invalid-tampered-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void supportNonExistentIssueShouldReturn404() throws Exception {
        String token = tokenFor(citizen1);
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(post("/api/issues/{issueId}/support", nonExistentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));
    }

    // ==========================================
    // 5. ISSUE DTO INTEGRATION & ZERO N+1 QUERIES
    // ==========================================

    @Test
    void issueDetailExposesSupportCountAndSupportedByCurrentUser() throws Exception {
        String token1 = tokenFor(citizen1);
        String token2 = tokenFor(citizen2);

        // Initially 0 supports
        mockMvc.perform(get("/api/issues/{id}", testIssue.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportCount", equalTo(0)))
                .andExpect(jsonPath("$.supportedByCurrentUser", equalTo(false)));

        // Citizen 1 supports
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isCreated());

        // Citizen 1 views issue: supportCount=1, supportedByCurrentUser=true
        mockMvc.perform(get("/api/issues/{id}", testIssue.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportCount", equalTo(1)))
                .andExpect(jsonPath("$.supportedByCurrentUser", equalTo(true)));

        // Citizen 2 views issue: supportCount=1, supportedByCurrentUser=false
        mockMvc.perform(get("/api/issues/{id}", testIssue.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportCount", equalTo(1)))
                .andExpect(jsonPath("$.supportedByCurrentUser", equalTo(false)));

        // Unauthenticated user views issue: supportCount=1, supportedByCurrentUser=false
        mockMvc.perform(get("/api/issues/{id}", testIssue.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportCount", equalTo(1)))
                .andExpect(jsonPath("$.supportedByCurrentUser", equalTo(false)))
                .andExpect(jsonPath("$.supporters").doesNotExist());
    }

    @Test
    void issueListExposesSupportCountAcrossIssues() throws Exception {
        String token1 = tokenFor(citizen1);

        // Citizen 1 supports testIssue
        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isCreated());

        // Verify issue listing contains support count
        mockMvc.perform(get("/api/issues")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].supportCount", equalTo(1)))
                .andExpect(jsonPath("$.content[0].supportedByCurrentUser", equalTo(true)));

        // Verify unauthenticated listing contains support count with supportedByCurrentUser=false
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].supportCount", equalTo(1)))
                .andExpect(jsonPath("$.content[0].supportedByCurrentUser", equalTo(false)));
    }

    @Test
    void selfSupportIsAllowed() throws Exception {
        // citizen1 is the reporter of testIssue
        assertEquals(citizen1.getId(), testIssue.getReporter().getId());

        String token = tokenFor(citizen1);

        mockMvc.perform(post("/api/issues/{issueId}/support", testIssue.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        assertEquals(1, supportRepository.countByIssue_Id(testIssue.getId()));
    }
}
