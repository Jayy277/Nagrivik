package org.nagrivic.modules.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.comments.entity.CommentEntity;
import org.nagrivic.modules.comments.repository.CommentRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.entity.ModerationReportEntity;
import org.nagrivic.modules.moderation.model.ModerationReason;
import org.nagrivic.modules.moderation.model.ModerationTargetType;
import org.nagrivic.modules.moderation.model.ReportStatus;
import org.nagrivic.modules.moderation.repository.ModerationReportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AdminModerationQueueAndDetailTest {

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
    private CommentRepository commentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    private MockMvc mockMvc;

    private UserEntity moderatorUser;
    private String moderatorToken;

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

        reportRepository.deleteAll();

        moderatorUser = createUniqueUser("Moderator Rohit", "MODERATOR");
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), "MODERATOR");

        citizenReporter = createUniqueUser("Citizen Maya", "CITIZEN");

        CategoryEntity category = categoryRepository.save(new CategoryEntity("Potholes", "potholes-" + System.nanoTime(), "Pothole issues", 1));
        LocationEntity location = locationService.createLocation(18.5204, 73.8567, new BigDecimal("8.00"));

        testIssue = issueService.createIssue(citizenReporter, category.getId(), location.getId(), "Dangerous Pothole on FC Road", "Huge crater causing traffic jams");
        testComment = commentRepository.save(new CommentEntity(testIssue, citizenReporter, "Offensive harassment in comment text"));

        issueReport = reportRepository.save(new ModerationReportEntity(
                citizenReporter,
                ModerationTargetType.ISSUE,
                testIssue.getId(),
                ModerationReason.MISLEADING_OR_MANIPULATIVE,
                "Inaccurate location description"
        ));

        commentReport = reportRepository.save(new ModerationReportEntity(
                citizenReporter,
                ModerationTargetType.COMMENT,
                testComment.getId(),
                ModerationReason.ABUSIVE_OR_HARASSING,
                "Harassment targeting civic workers"
        ));
    }

    @Test
    @DisplayName("Queue listing supports pagination and default descending order by creation")
    void testQueuePagination() throws Exception {
        mockMvc.perform(get("/api/moderation/reports?page=0&size=10")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("Queue listing supports server-side filtering by targetType")
    void testQueueFilterByTargetType() throws Exception {
        mockMvc.perform(get("/api/moderation/reports?targetType=COMMENT")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].targetType").value("COMMENT"))
                .andExpect(jsonPath("$.content[0].targetId").value(testComment.getId().toString()));
    }

    @Test
    @DisplayName("Queue listing supports server-side filtering by reason")
    void testQueueFilterByReason() throws Exception {
        mockMvc.perform(get("/api/moderation/reports?reason=MISLEADING_OR_MANIPULATIVE")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reason").value("MISLEADING_OR_MANIPULATIVE"))
                .andExpect(jsonPath("$.content[0].targetType").value("ISSUE"));
    }

    @Test
    @DisplayName("Queue summary endpoint returns accurate metric counts")
    void testQueueSummaryMetrics() throws Exception {
        mockMvc.perform(get("/api/moderation/summary")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(2))
                .andExpect(jsonPath("$.inReviewCount").value(0))
                .andExpect(jsonPath("$.resolvedCount").value(0))
                .andExpect(jsonPath("$.dismissedCount").value(0))
                .andExpect(jsonPath("$.totalReports").value(2));
    }

    @Test
    @DisplayName("Report detail for ISSUE target contains issue context, safe author without phone/email")
    void testReportDetailForIssueTarget() throws Exception {
        mockMvc.perform(get("/api/moderation/reports/" + issueReport.getId())
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(issueReport.getId().toString()))
                .andExpect(jsonPath("$.targetType").value("ISSUE"))
                .andExpect(jsonPath("$.targetId").value(testIssue.getId().toString()))
                .andExpect(jsonPath("$.reason").value("MISLEADING_OR_MANIPULATIVE"))
                .andExpect(jsonPath("$.issueTarget").exists())
                .andExpect(jsonPath("$.issueTarget.title").value("Dangerous Pothole on FC Road"))
                .andExpect(jsonPath("$.issueTarget.reporter.fullName").value("Citizen Maya"))
                .andExpect(jsonPath("$.issueTarget.reporter.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.issueTarget.reporter.email").doesNotExist())
                .andExpect(jsonPath("$.reporter.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.reporter.email").doesNotExist());
    }

    @Test
    @DisplayName("Report detail for COMMENT target contains comment text, issue ID, and safe author")
    void testReportDetailForCommentTarget() throws Exception {
        mockMvc.perform(get("/api/moderation/reports/" + commentReport.getId())
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentReport.getId().toString()))
                .andExpect(jsonPath("$.targetType").value("COMMENT"))
                .andExpect(jsonPath("$.targetId").value(testComment.getId().toString()))
                .andExpect(jsonPath("$.commentTarget").exists())
                .andExpect(jsonPath("$.commentTarget.content").value("Offensive harassment in comment text"))
                .andExpect(jsonPath("$.commentTarget.issueId").value(testIssue.getId().toString()))
                .andExpect(jsonPath("$.commentTarget.isDeleted").value(false))
                .andExpect(jsonPath("$.commentTarget.author.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.commentTarget.author.email").doesNotExist());
    }

    @Test
    @DisplayName("Non-existent report ID returns 404 Not Found")
    void testNonExistentReportDetailReturns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/moderation/reports/" + randomId)
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isNotFound());
    }
}
