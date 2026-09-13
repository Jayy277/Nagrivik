package org.nagrivic.modules.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.activity.dto.ActivityResponse;
import org.nagrivic.modules.activity.entity.IssueActivityEntity;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.activity.repository.IssueActivityRepository;
import org.nagrivic.modules.activity.service.IssueActivityService;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.comments.dto.CreateCommentRequest;
import org.nagrivic.modules.comments.service.CommentService;
import org.nagrivic.modules.duplicates.service.DuplicateDetectionService;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueResponsibilityService;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.media.service.MediaService;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.priority.service.IssuePriorityService;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.repository.StatusHistoryRepository;
import org.nagrivic.modules.statushistory.service.StatusHistoryService;
import org.nagrivic.modules.supports.service.SupportService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IssueActivityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IssueActivityService issueActivityService;

    @Autowired
    private IssueActivityRepository issueActivityRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private StatusHistoryService statusHistoryService;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private SupportService supportService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    @Autowired
    private IssueResponsibilityService issueResponsibilityService;

    @Autowired
    private IssuePriorityService issuePriorityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizen;
    private String citizenToken;
    private UserEntity officer;
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

        citizen = createUniqueUser("Citizen Aarav", "CITIZEN");
        citizenToken = jwtService.generateAccessToken(citizen.getId(), citizen.getRole());

        officer = createUniqueUser("Officer Sharma", "OFFICER");
        officerToken = jwtService.generateAccessToken(officer.getId(), officer.getRole());

        testCategory = categoryRepository.save(new CategoryEntity(
                "Roads / Potholes",
                "roads-potholes-" + System.nanoTime(),
                "Pothole issues",
                1
        ));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("A. Issue creation records ISSUE_REPORTED activity")
    void testIssueCreationActivity() {
        IssueEntity issue = issueService.createIssue(
                citizen,
                testCategory.getId(),
                testLocation.getId(),
                "Crater on crossroad",
                "Deep crater near school"
        );

        List<IssueActivityEntity> activities = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .toList();

        assertThat(activities).anyMatch(a ->
                a.getEventType() == IssueActivityType.ISSUE_REPORTED &&
                        a.getActor() != null &&
                        a.getActor().getId().equals(citizen.getId()) &&
                        "Crater on crossroad".equals(a.getEventData().get("title"))
        );
    }

    @Test
    @DisplayName("B. Status change records STATUS_CHANGED activity alongside status_history")
    void testStatusChangeActivity() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.VERIFIED, "Inspected on site"))))
                .andExpect(status().isOk());

        // Verify status_history exists
        assertThat(statusHistoryRepository.findByIssue_IdOrderByCreatedAtAsc(issue.getId())).isNotEmpty();

        // Verify STATUS_CHANGED activity
        List<IssueActivityEntity> activities = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.STATUS_CHANGED)
                .toList();

        assertThat(activities).hasSize(1);
        IssueActivityEntity activity = activities.get(0);
        assertThat(activity.getActor().getId()).isEqualTo(officer.getId());
        assertThat(activity.getEventData().get("from")).isEqualTo("REPORTED");
        assertThat(activity.getEventData().get("to")).isEqualTo("VERIFIED");
        assertThat(activity.getEventData().get("reason")).isEqualTo("Inspected on site");
    }

    @Test
    @DisplayName("C. Failed status transition does not create STATUS_CHANGED activity")
    void testFailedStatusTransitionNoActivity() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        long countBefore = issueActivityRepository.count();

        // Illegal jump: REPORTED -> RESOLVED directly without VERIFIED
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.RESOLVED, "Illegal jump"))))
                .andExpect(status().isBadRequest());

        long countAfter = issueActivityRepository.count();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("D. Support added and removed records activities; supporter identity is masked")
    void testSupportActivitiesAndPrivacyMasking() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        UserEntity supporter = createUniqueUser("Supporter Rohan", "CITIZEN");
        String supporterToken = jwtService.generateAccessToken(supporter.getId(), supporter.getRole());

        // Add support
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/support")
                        .header("Authorization", "Bearer " + supporterToken))
                .andExpect(status().isCreated());

        // Verify SUPPORT_ADDED activity recorded
        List<IssueActivityEntity> activities = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.SUPPORT_ADDED)
                .toList();
        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getEventData().get("supportCount")).isEqualTo(1);

        // Remove support
        mockMvc.perform(delete("/api/issues/" + issue.getId() + "/support")
                        .header("Authorization", "Bearer " + supporterToken))
                .andExpect(status().isNoContent());

        activities = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.SUPPORT_REMOVED)
                .toList();
        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getEventData().get("supportCount")).isEqualTo(0);

        // Verify public activity endpoint masks supporter identity
        mockMvc.perform(get("/api/issues/" + issue.getId() + "/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.eventType == 'SUPPORT_ADDED')].actor.displayName", hasItem("Citizen")))
                .andExpect(jsonPath("$.content[?(@.eventType == 'SUPPORT_ADDED')].actor.id", hasItem(nullValue())))
                .andExpect(jsonPath("$.content[?(@.eventType == 'SUPPORT_REMOVED')].actor.displayName", hasItem("Citizen")))
                .andExpect(jsonPath("$.content[?(@.eventType == 'SUPPORT_REMOVED')].actor.id", hasItem(nullValue())));
    }

    @Test
    @DisplayName("E. Comments added and deleted record activities; comment body is omitted")
    void testCommentActivities() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        // Add comment
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/comments")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Private details: 123 Street"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        List<IssueActivityEntity> commentAddedList = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.COMMENT_ADDED)
                .toList();
        assertThat(commentAddedList).hasSize(1);
        // Ensure comment text is NOT stored in activity data
        assertThat(commentAddedList.get(0).getEventData().get("commentId")).isNotNull();
        assertThat(commentAddedList.get(0).getEventData().containsKey("content")).isFalse();
        assertThat(commentAddedList.get(0).getEventData().containsKey("text")).isFalse();

        UUID commentId = UUID.fromString(commentAddedList.get(0).getEventData().get("commentId").toString());

        // Delete comment
        mockMvc.perform(delete("/api/issues/" + issue.getId() + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isNoContent());

        List<IssueActivityEntity> commentDeletedList = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.COMMENT_DELETED)
                .toList();
        assertThat(commentDeletedList).hasSize(1);
        assertThat(commentDeletedList.get(0).getEventData().get("commentId")).isEqualTo(commentId.toString());
    }

    @Test
    @DisplayName("F. Media added records MEDIA_ADDED activity without private storage details")
    void testMediaAddedActivity() {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Road hazard", "Description"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        // Upload media via service
        mediaService.createMedia(
                issue.getId(),
                "issues/" + issue.getId() + "/evidence.jpg",
                "evidence.jpg",
                "image/jpeg",
                4L,
                org.nagrivic.modules.media.model.MediaType.IMAGE,
                0
        );

        List<IssueActivityEntity> mediaActivities = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.MEDIA_ADDED)
                .toList();

        assertThat(mediaActivities).hasSize(1);
        assertThat(mediaActivities.get(0).getEventData().get("mediaId")).isNotNull();
        // Zero storage credentials or private paths
        assertThat(mediaActivities.get(0).getEventData().containsKey("storageKey")).isFalse();
        assertThat(mediaActivities.get(0).getEventData().containsKey("url")).isFalse();
    }

    @Test
    @DisplayName("G. Duplicate linked records DUPLICATE_LINKED activity")
    void testDuplicateLinkedActivity() throws Exception {
        IssueEntity primary = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Primary Pothole", "Description"
        );
        IssueEntity duplicate = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Duplicate Pothole", "Description"
        );

        mockMvc.perform(post("/api/issues/" + duplicate.getId() + "/duplicate")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new org.nagrivic.modules.duplicates.dto.LinkDuplicateRequest(primary.getId()))))
                .andExpect(status().isOk());

        List<IssueActivityEntity> dupActivities = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(duplicate.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.DUPLICATE_LINKED)
                .toList();

        assertThat(dupActivities).hasSize(1);
        assertThat(dupActivities.get(0).getEventData().get("duplicateIssueId")).isEqualTo(duplicate.getId().toString());
        assertThat(dupActivities.get(0).getEventData().get("primaryIssueId")).isEqualTo(primary.getId().toString());
    }

    @Test
    @DisplayName("H. Priority recalculated records activity and ignores repeated identical recalculations")
    void testPriorityRecalculatedNonFlooding() {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Pothole for priority", "Description", IssueSeverity.LOW
        );

        // Initial priority calculation recorded
        long initialActivities = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.PRIORITY_RECALCULATED)
                .count();
        assertThat(initialActivities).isEqualTo(1);

        // Repeated identical recalculations should NOT create new timeline events
        issuePriorityService.recalculatePriority(issue.getId());
        issuePriorityService.recalculatePriority(issue.getId());
        issuePriorityService.recalculatePriority(issue.getId());

        long afterDuplicateRecalcs = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.PRIORITY_RECALCULATED)
                .count();
        assertThat(afterDuplicateRecalcs).isEqualTo(initialActivities);

        // Deliberately change severity to CRITICAL -> score and level jump -> triggers PRIORITY_RECALCULATED
        issue.setSeverity(IssueSeverity.CRITICAL);
        issueRepository.save(issue);
        issuePriorityService.calculatePriority(issue);

        long afterChange = issueActivityRepository.findAll().stream()
                .filter(a -> a.getIssue().getId().equals(issue.getId()))
                .filter(a -> a.getEventType() == IssueActivityType.PRIORITY_RECALCULATED)
                .count();
        assertThat(afterChange).isEqualTo(initialActivities + 1);
    }

    @Test
    @DisplayName("I. Privacy & immutability: Public GET activity timeline is read-only and leaks zero PII")
    void testPublicActivityPrivacyAndImmutability() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Privacy test", "Description"
        );

        // Public read
        mockMvc.perform(get("/api/issues/" + issue.getId() + "/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[?(@.eventType == 'ISSUE_REPORTED')].actor.displayName", hasItem("Citizen Aarav")))
                // Ensure NO phone numbers or sensitive data leaked in JSON response
                .andExpect(jsonPath("$.content[*].actor.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.content[*].actor.password").doesNotExist())
                .andExpect(jsonPath("$.content[*].actor.email").doesNotExist());

        // Verify PUT and DELETE are rejected with 405 Method Not Allowed (read-only even for authenticated users)
        mockMvc.perform(put("/api/issues/" + issue.getId() + "/activity")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"tampered\"}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/issues/" + issue.getId() + "/activity")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("J. Pagination and deterministic ordering")
    void testPaginationAndDeterministicOrdering() throws Exception {
        IssueEntity issue = issueService.createIssue(
                citizen, testCategory.getId(), testLocation.getId(), "Ordering test", "Description"
        );

        // Manually record 25 activities
        for (int i = 1; i <= 25; i++) {
            issueActivityService.recordActivity(
                    issue,
                    IssueActivityType.STATUS_CHANGED,
                    officer,
                    Map.of("step", i)
            );
        }

        // Test default page size (clamped to 20)
        mockMvc.perform(get("/api/issues/" + issue.getId() + "/activity")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content", hasSize(10)));

        // Test maximum size clamped to 100
        mockMvc.perform(get("/api/issues/" + issue.getId() + "/activity")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        // Test deterministic ordering (newest first)
        Page<ActivityResponse> page = issueActivityService.getIssueActivities(
                issue.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        List<ActivityResponse> content = page.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).createdAt().compareTo(content.get(i + 1).createdAt())).isGreaterThanOrEqualTo(0);
        }
    }
}
