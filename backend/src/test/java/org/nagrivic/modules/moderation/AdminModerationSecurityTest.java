package org.nagrivic.modules.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.dto.CreateReportRequest;
import org.nagrivic.modules.moderation.dto.ResolveReportRequest;
import org.nagrivic.modules.moderation.dto.RestrictUserRequest;
import org.nagrivic.modules.moderation.entity.ModerationReportEntity;
import org.nagrivic.modules.moderation.model.ModerationActionType;
import org.nagrivic.modules.moderation.model.ModerationReason;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.repository.ModerationReportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AdminModerationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ModerationReportRepository reportRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizenUser;
    private String citizenToken;

    private UserEntity moderatorUser;
    private String moderatorToken;

    private UserEntity adminUser;
    private String adminToken;

    private ModerationReportEntity testReport;

    private UserEntity createUniqueUser(String name, String role) {
        long uniqueNum = Math.abs(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
        UserEntity u = new UserEntity("+91" + uniqueNum, name);
        u.setRole(role);
        return userRepository.save(u);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        citizenUser = createUniqueUser("Citizen Ramesh", "CITIZEN");
        citizenToken = jwtService.generateAccessToken(citizenUser.getId(), "CITIZEN");

        moderatorUser = createUniqueUser("Moderator Priya", "MODERATOR");
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), "MODERATOR");

        adminUser = createUniqueUser("Admin Vikram", "ADMIN");
        adminToken = jwtService.generateAccessToken(adminUser.getId(), "ADMIN");

        CategoryEntity category = categoryRepository.save(new CategoryEntity("Security Test Cat", "sec-cat-" + System.nanoTime(), "Desc", 1));
        LocationEntity location = locationService.createLocation(19.0760, 72.8777, new BigDecimal("10.00"));
        IssueEntity issue = issueService.createIssue(citizenUser, category.getId(), location.getId(), "Test Issue Title", "Description");

        testReport = reportRepository.save(new ModerationReportEntity(
                citizenUser,
                ModerationTargetType.ISSUE,
                issue.getId(),
                ModerationReason.SPAM,
                "Looks like spam"
        ));
    }

    @Test
    @DisplayName("Unauthenticated request to moderation queue is rejected with 401 Unauthorized")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/moderation/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CITIZEN cannot access moderation reports queue (403 Forbidden)")
    void testCitizenCannotAccessQueue() throws Exception {
        mockMvc.perform(get("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CITIZEN cannot access moderation summary (403 Forbidden)")
    void testCitizenCannotAccessSummary() throws Exception {
        mockMvc.perform(get("/api/moderation/summary")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CITIZEN cannot access moderation report detail (403 Forbidden)")
    void testCitizenCannotAccessReportDetail() throws Exception {
        mockMvc.perform(get("/api/moderation/reports/" + testReport.getId())
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CITIZEN cannot review, resolve, or dismiss report (403 Forbidden)")
    void testCitizenCannotActOnReport() throws Exception {
        mockMvc.perform(post("/api/moderation/reports/" + testReport.getId() + "/review")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        ResolveReportRequest resolveReq = new ResolveReportRequest(ModerationActionType.NO_ACTION, "Dismissing", null);
        mockMvc.perform(post("/api/moderation/reports/" + testReport.getId() + "/resolve")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/moderation/reports/" + testReport.getId() + "/dismiss")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CITIZEN can submit a moderation report via POST /api/moderation/reports")
    void testCitizenCanSubmitReport() throws Exception {
        CreateReportRequest req = new CreateReportRequest(
                ModerationTargetType.ISSUE,
                testReport.getTargetId(),
                ModerationReason.ABUSIVE_OR_HARASSING,
                "Inappropriate language used"
        );

        // Submitting as different user to avoid duplicate self-report collision
        UserEntity secondCitizen = createUniqueUser("Citizen Ananya", "CITIZEN");
        String secondToken = jwtService.generateAccessToken(secondCitizen.getId(), "CITIZEN");

        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("MODERATOR can access moderation queue and report detail")
    void testModeratorCanAccessQueueAndDetail() throws Exception {
        mockMvc.perform(get("/api/moderation/reports")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/moderation/reports/" + testReport.getId())
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MODERATOR can perform content actions (hide content)")
    void testModeratorCanHideContent() throws Exception {
        mockMvc.perform(post("/api/moderation/content/ISSUE/" + testReport.getTargetId() + "/hide")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .param("reason", "Spam content violation"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MODERATOR CANNOT execute user restriction (403 Forbidden)")
    void testModeratorCannotRestrictUser() throws Exception {
        RestrictUserRequest req = new RestrictUserRequest(60L, "Spam harassment");
        mockMvc.perform(post("/api/moderation/users/" + citizenUser.getId() + "/restrict")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can execute user restriction and access all moderation APIs")
    void testAdminCanRestrictUser() throws Exception {
        RestrictUserRequest req = new RestrictUserRequest(60L, "Repeated violations");
        mockMvc.perform(post("/api/moderation/users/" + citizenUser.getId() + "/restrict")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/moderation/users/" + citizenUser.getId() + "/unrestrict")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
