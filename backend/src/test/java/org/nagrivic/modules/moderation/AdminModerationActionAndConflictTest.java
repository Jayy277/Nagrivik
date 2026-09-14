package org.nagrivic.modules.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.comments.repository.CommentRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.dto.ResolveReportRequest;
import org.nagrivic.modules.moderation.entity.ModerationActionEntity;
import org.nagrivic.modules.moderation.entity.ModerationReportEntity;
import org.nagrivic.modules.moderation.model.ModerationActionType;
import org.nagrivic.modules.moderation.model.ModerationReason;
import org.nagrivic.modules.moderation.model.ModerationStatus;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.model.ReportStatus;
import org.nagrivic.modules.moderation.repository.ModerationActionRepository;
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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AdminModerationActionAndConflictTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ModerationReportRepository reportRepository;

    @Autowired
    private ModerationActionRepository actionRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity moderatorA;
    private String moderatorAToken;

    private UserEntity moderatorB;
    private String moderatorBToken;

    private UserEntity citizenReporter;
    private IssueEntity testIssue;
    private CommentEntity testComment;
    private ModerationReportEntity issueReport;
    private ModerationReportEntity commentReport;

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

        actionRepository.deleteAll();
        reportRepository.deleteAll();

        moderatorA = createUniqueUser("Moderator Dev", "MODERATOR");
        moderatorAToken = jwtService.generateAccessToken(moderatorA.getId(), "MODERATOR");

        moderatorB = createUniqueUser("Moderator Sunil", "MODERATOR");
        moderatorBToken = jwtService.generateAccessToken(moderatorB.getId(), "MODERATOR");

        citizenReporter = createUniqueUser("Citizen Pooja", "CITIZEN");

        CategoryEntity category = categoryRepository.save(new CategoryEntity("Action Test Cat", "act-cat-" + System.nanoTime(), "Cat desc", 1));
        LocationEntity location = locationService.createLocation(28.6139, 77.2090, new BigDecimal("12.00"));

        testIssue = issueService.createIssue(citizenReporter, category.getId(), location.getId(), "Noise Pollution Issue", "Loud music violation");
        testComment = commentRepository.save(new CommentEntity(testIssue, citizenReporter, "Offensive harassment comment to remove"));

        issueReport = reportRepository.save(new ModerationReportEntity(
                citizenReporter,
                ModerationTargetType.ISSUE,
                testIssue.getId(),
                ModerationReason.SPAM,
                "Repetitive post"
        ));

        commentReport = reportRepository.save(new ModerationReportEntity(
                citizenReporter,
                ModerationTargetType.COMMENT,
                testComment.getId(),
                ModerationReason.ABUSIVE_OR_HARASSING,
                "Abusive text"
        ));
    }

    @Test
    @DisplayName("Review flow: report moves OPEN -> IN_REVIEW, then RESOLVED with HIDE_CONTENT and audit logged")
    void testResolveFlowWithHideContent() throws Exception {
        // Step 1: Review
        mockMvc.perform(post("/api/moderation/reports/" + issueReport.getId() + "/review")
                        .header("Authorization", "Bearer " + moderatorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        // Step 2: Resolve with HIDE_CONTENT
        ResolveReportRequest resolveReq = new ResolveReportRequest(
                ModerationActionType.HIDE_CONTENT,
                "Verified spam content",
                "Internal notes: Author warned"
        );

        mockMvc.perform(post("/api/moderation/reports/" + issueReport.getId() + "/resolve")
                        .header("Authorization", "Bearer " + moderatorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Content is hidden
        IssueEntity updatedIssue = issueRepository.findById(testIssue.getId()).orElseThrow();
        assertEquals(ModerationStatus.HIDDEN, updatedIssue.getModerationStatus());

        // Audit action recorded
        List<ModerationActionEntity> actions = actionRepository.findByReport_IdOrderByCreatedAtDesc(issueReport.getId());
        assertEquals(1, actions.size());
        assertEquals(ModerationActionType.HIDE_CONTENT, actions.get(0).getAction());
        assertEquals(moderatorA.getId(), actions.get(0).getModerator().getId());
    }

    @Test
    @DisplayName("REMOVE_COMMENT resolves report, soft-deletes comment record, and marks it HIDDEN")
    void testRemoveCommentActionSoftDeletes() throws Exception {
        ResolveReportRequest resolveReq = new ResolveReportRequest(
                ModerationActionType.REMOVE_COMMENT,
                "Gross harassment violation",
                "Soft-deleted via moderation"
        );

        mockMvc.perform(post("/api/moderation/reports/" + commentReport.getId() + "/resolve")
                        .header("Authorization", "Bearer " + moderatorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        CommentEntity updatedComment = commentRepository.findById(testComment.getId()).orElseThrow();
        assertTrue(updatedComment.isDeleted(), "Comment should be marked as soft-deleted");
        assertNotNull(updatedComment.getDeletedAt(), "Deleted timestamp must be present");
        assertEquals(ModerationStatus.HIDDEN, updatedComment.getModerationStatus());
    }

    @Test
    @DisplayName("Conflict handling: Resolving an already resolved or dismissed report returns 409 Conflict")
    void testConcurrentResolutionReturns409Conflict() throws Exception {
        // Moderator A resolves the report
        ResolveReportRequest resolveReq = new ResolveReportRequest(
                ModerationActionType.NO_ACTION,
                "First moderator resolution",
                null
        );

        mockMvc.perform(post("/api/moderation/reports/" + issueReport.getId() + "/resolve")
                        .header("Authorization", "Bearer " + moderatorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk());

        // Moderator B attempts to resolve the same report concurrently
        ResolveReportRequest conflictReq = new ResolveReportRequest(
                ModerationActionType.HIDE_CONTENT,
                "Second moderator resolution attempt",
                null
        );

        mockMvc.perform(post("/api/moderation/reports/" + issueReport.getId() + "/resolve")
                        .header("Authorization", "Bearer " + moderatorBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        // Moderator B attempts to dismiss the already resolved report -> 409 Conflict
        mockMvc.perform(post("/api/moderation/reports/" + issueReport.getId() + "/dismiss")
                        .header("Authorization", "Bearer " + moderatorBToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("Invalid action combination: REMOVE_COMMENT on an ISSUE target returns 400 Bad Request")
    void testInvalidActionTargetCombinationRejected() throws Exception {
        ResolveReportRequest invalidReq = new ResolveReportRequest(
                ModerationActionType.REMOVE_COMMENT,
                "Attempting comment removal on an issue",
                null
        );

        mockMvc.perform(post("/api/moderation/reports/" + issueReport.getId() + "/resolve")
                        .header("Authorization", "Bearer " + moderatorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
