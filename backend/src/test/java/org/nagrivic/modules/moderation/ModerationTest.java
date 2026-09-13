package org.nagrivic.modules.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.comments.dto.CreateCommentRequest;
import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.comments.repository.CommentRepository;
import org.nagrivic.modules.comments.service.CommentService;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.dto.CreateReportRequest;
import org.nagrivic.modules.moderation.dto.ResolveReportRequest;
import org.nagrivic.modules.moderation.dto.RestrictUserRequest;
import org.nagrivic.modules.moderation.entity.ModerationActionEntity;
import org.nagrivic.modules.moderation.entity.ModerationReportEntity;
import org.nagrivic.modules.moderation.model.*;
import org.nagrivic.modules.moderation.ratelimit.InMemoryRateLimiter;
import org.nagrivic.modules.moderation.repository.ModerationActionRepository;
import org.nagrivic.modules.moderation.repository.ModerationReportRepository;
import org.nagrivic.modules.moderation.service.ContentAbuseValidator;
import org.nagrivic.modules.moderation.service.ModerationService;
import org.nagrivic.modules.supports.service.SupportService;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ModerationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private ModerationReportRepository reportRepository;

    @Autowired
    private ModerationActionRepository actionRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private SupportService supportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private InMemoryRateLimiter rateLimiter;

    @Autowired
    private ContentAbuseValidator contentAbuseValidator;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizenReporter;
    private String citizenReporterToken;

    private UserEntity citizenVictim;
    private String citizenVictimToken;

    private UserEntity officerUser;
    private String officerToken;

    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    private UserEntity createUniqueUser(String name, String role) {
        long uniqueNum = Math.abs(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
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

        rateLimiter.reset();
        contentAbuseValidator.reset();

        citizenReporter = createUniqueUser("Citizen Aarav", "CITIZEN");
        citizenReporterToken = jwtService.generateAccessToken(citizenReporter.getId(), citizenReporter.getRole());

        citizenVictim = createUniqueUser("Citizen Diya", "CITIZEN");
        citizenVictimToken = jwtService.generateAccessToken(citizenVictim.getId(), citizenVictim.getRole());

        officerUser = createUniqueUser("Officer Sharma", "OFFICER");
        officerToken = jwtService.generateAccessToken(officerUser.getId(), officerUser.getRole());

        testCategory = categoryRepository.save(new CategoryEntity(
                "Sanitation & Waste",
                "sanitation-" + System.nanoTime(),
                "Sanitation issues",
                1
        ));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @AfterEach
    void tearDown() {
        actionRepository.deleteAll();
        reportRepository.deleteAll();
    }

    @Test
    @DisplayName("A. Submit moderation report: Authenticated citizen can report valid issue and comment")
    void testSubmitReportHappyPath() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Inappropriate Issue Title", "Description"
        );

        CreateReportRequest issueReportReq = new CreateReportRequest(
                ModerationTargetType.ISSUE,
                issue.getId(),
                ModerationReason.ABUSIVE_OR_HARASSING,
                "Offensive language used in description"
        );

        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReportReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.targetType").value("ISSUE"))
                .andExpect(jsonPath("$.targetId").value(issue.getId().toString()))
                .andExpect(jsonPath("$.reason").value("ABUSIVE_OR_HARASSING"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Verify in database
        assertThat(reportRepository.existsByReporter_IdAndTargetTypeAndTargetIdAndStatusIn(
                citizenReporter.getId(), ModerationTargetType.ISSUE, issue.getId(), List.of(ReportStatus.OPEN)
        )).isTrue();
    }

    @Test
    @DisplayName("B. Unauthenticated report submission rejected with 401")
    void testUnauthenticatedReportRejected() throws Exception {
        CreateReportRequest req = new CreateReportRequest(
                ModerationTargetType.ISSUE,
                UUID.randomUUID(),
                ModerationReason.SPAM,
                "Spamming links"
        );

        mockMvc.perform(post("/api/moderation/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("C. Invalid target ID returns 404")
    void testInvalidTargetNotFound() throws Exception {
        CreateReportRequest req = new CreateReportRequest(
                ModerationTargetType.ISSUE,
                UUID.randomUUID(),
                ModerationReason.SPAM,
                "Non-existent issue"
        );

        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D. Invalid reason returns validation error (400)")
    void testInvalidReasonRejected() throws Exception {
        String invalidPayload = """
                {
                    "targetType": "ISSUE",
                    "targetId": "%s",
                    "reason": "ANTI_GOVERNMENT_POLITICAL_OPINION",
                    "description": "Political criticism"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("E. Duplicate open report from same citizen returns 409 Conflict")
    void testDuplicateReportRejected() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Problem Title", "Description"
        );

        CreateReportRequest req = new CreateReportRequest(
                ModerationTargetType.ISSUE,
                issue.getId(),
                ModerationReason.SPAM,
                "First report"
        );

        // First report succeeds
        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second report against same target while first is open rejected with 409
        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("F. Self-reporting: Citizen cannot report their own issue or comment (400)")
    void testCannotReportOwnContent() throws Exception {
        IssueEntity ownIssue = issueService.createIssue(
                citizenReporter, testCategory.getId(), testLocation.getId(), "My own issue", "Description"
        );

        CreateReportRequest req = new CreateReportRequest(
                ModerationTargetType.ISSUE,
                ownIssue.getId(),
                ModerationReason.SPAM,
                "Reporting myself"
        );

        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("G. Moderation authorization: Normal citizens cannot execute moderation actions (403)")
    void testCitizenForbiddenFromModeratorEndpoints() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/moderation/reports/" + randomId + "/review")
                        .header("Authorization", "Bearer " + citizenReporterToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/moderation/content/ISSUE/" + randomId + "/hide?reason=test")
                        .header("Authorization", "Bearer " + citizenReporterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("H. Hidden issue does not appear in public issue list or detail API")
    void testHiddenIssueExcludedFromPublicApis() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Public or Hidden", "Description"
        );

        // Verify publicly visible initially
        mockMvc.perform(get("/api/issues/" + issue.getId()))
                .andExpect(status().isOk());

        // Moderator hides content
        mockMvc.perform(post("/api/moderation/content/ISSUE/" + issue.getId() + "/hide")
                        .header("Authorization", "Bearer " + officerToken)
                        .param("reason", "Contains dangerous personal information"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("HIDE_CONTENT"));

        // Public detail returns 404
        mockMvc.perform(get("/api/issues/" + issue.getId()))
                .andExpect(status().isNotFound());

        // Public list excludes hidden issue
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + issue.getId() + "')]").doesNotExist());
    }

    @Test
    @DisplayName("I. Hidden comment does not appear publicly in comments list")
    void testHiddenCommentExcludedFromPublicApis() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("This is abusive!"))))
                .andExpect(status().isCreated());

        List<CommentEntity> comments = commentRepository.findAll();
        CommentEntity comment = comments.get(comments.size() - 1);

        // Moderator hides comment
        mockMvc.perform(post("/api/moderation/content/COMMENT/" + comment.getId() + "/hide")
                        .header("Authorization", "Bearer " + officerToken)
                        .param("reason", "Abusive harassment"))
                .andExpect(status().isOk());

        // Public get comments returns empty or excludes hidden comment
        mockMvc.perform(get("/api/issues/" + issue.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + comment.getId() + "')]").doesNotExist());
    }

    @Test
    @DisplayName("J. Restore: Restored content becomes publicly visible again")
    void testRestoreContent() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Restoration test", "Description"
        );

        // Hide
        mockMvc.perform(post("/api/moderation/content/ISSUE/" + issue.getId() + "/hide")
                        .header("Authorization", "Bearer " + officerToken)
                        .param("reason", "Pending investigation"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/issues/" + issue.getId())).andExpect(status().isNotFound());

        // Restore
        mockMvc.perform(post("/api/moderation/content/ISSUE/" + issue.getId() + "/restore")
                        .header("Authorization", "Bearer " + officerToken)
                        .param("reason", "Cleared upon review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("RESTORE_CONTENT"));

        // Public access restored
        mockMvc.perform(get("/api/issues/" + issue.getId())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("K. User restriction: Restricted user cannot create issues, comments, support, or reports")
    void testRestrictedUserBlockedFromContributing() throws Exception {
        // Restrict citizen
        mockMvc.perform(post("/api/moderation/users/" + citizenReporter.getId() + "/restrict")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestrictUserRequest(60L, "Repeated harassment"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("RESTRICT_USER"));

        // 1. Blocked from creating issue
        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateIssueRequest(
                                "Pothole", "Desc", testCategory.getId(), testLocation.getId(), null
                        ))))
                .andExpect(status().isForbidden());

        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Existing issue", "Description"
        );

        // 2. Blocked from commenting
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Hello"))))
                .andExpect(status().isForbidden());

        // 3. Blocked from supporting
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/support")
                        .header("Authorization", "Bearer " + citizenReporterToken))
                .andExpect(status().isForbidden());

        // 4. Blocked from submitting reports
        mockMvc.perform(post("/api/moderation/reports")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReportRequest(
                                ModerationTargetType.ISSUE, issue.getId(), ModerationReason.SPAM, "Spam"
                        ))))
                .andExpect(status().isForbidden());

        // Unrestrict user
        mockMvc.perform(post("/api/moderation/users/" + citizenReporter.getId() + "/unrestrict")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        // Now supporting works
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/support")
                        .header("Authorization", "Bearer " + citizenReporterToken))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("L. Comment abuse detection: Excessive repeated characters and rapid identical comments rejected")
    void testCommentAbuseDetection() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        // 1. Excessive repeated characters (e.g. 10 identical 'a's)
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Garbage aaaaaaaaaaaaaa"))))
                .andExpect(status().isBadRequest());

        // 2. First normal comment succeeds
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Road repaired properly yesterday."))))
                .andExpect(status().isCreated());

        // 3. Rapid identical comment within 60s rejected with 409 Conflict
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Road repaired properly yesterday."))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("M. Rate limiting: Exceeding configured rate limit returns 429 Too Many Requests")
    void testRateLimitingReturns429() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        // Configured limit is 5 comments per minute
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                            .header("Authorization", "Bearer " + citizenReporterToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateCommentRequest("Comment number " + i))))
                    .andExpect(status().isCreated());
        }

        // 6th comment exceeds limit -> returns 429 with Retry-After header
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                        .header("Authorization", "Bearer " + citizenReporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Comment number 6"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Q. Moderation audit: Resolving or dismissing report records immutable audit row")
    void testModerationAuditLogging() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizenVictim, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        ModerationReportEntity report = reportRepository.save(new ModerationReportEntity(
                citizenReporter, ModerationTargetType.ISSUE, issue.getId(), ModerationReason.SPAM, "Commercial advertisement"
        ));

        // Moderator resolves report
        mockMvc.perform(post("/api/moderation/reports/" + report.getId() + "/resolve")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveReportRequest(
                                ModerationActionType.HIDE_CONTENT,
                                "Spam confirmed",
                                "Third offense by poster"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Verify audit entity created
        List<ModerationActionEntity> actions = actionRepository.findAll();
        assertThat(actions).anyMatch(a ->
                a.getReport() != null &&
                        a.getReport().getId().equals(report.getId()) &&
                        a.getAction() == ModerationActionType.HIDE_CONTENT &&
                        a.getModerator().getId().equals(officerUser.getId()) &&
                        "Spam confirmed".equals(a.getReason())
        );
    }
}
