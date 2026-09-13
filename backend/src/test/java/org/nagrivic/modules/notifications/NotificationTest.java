package org.nagrivic.modules.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.comments.dto.CreateCommentRequest;
import org.nagrivic.modules.comments.service.CommentService;
import org.nagrivic.modules.duplicates.service.DuplicateDetectionService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.notifications.dto.UpdateNotificationPreferenceRequest;
import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.nagrivic.modules.notifications.entity.NotificationPreferenceEntity;
import org.nagrivic.modules.notifications.model.NotificationType;
import org.nagrivic.modules.notifications.repository.NotificationPreferenceRepository;
import org.nagrivic.modules.notifications.repository.NotificationRepository;
import org.nagrivic.modules.notifications.service.NotificationService;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.priority.service.IssuePriorityService;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.dto.VerifyResolutionRequest;
import org.nagrivic.modules.statushistory.service.StatusHistoryService;
import org.nagrivic.modules.supports.entity.SupportEntity;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class NotificationTest {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private SupportRepository supportRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private StatusHistoryService statusHistoryService;

    @Autowired
    private IssuePriorityService issuePriorityService;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    private UserEntity citizen1;
    private UserEntity citizen2;
    private UserEntity officer;
    private CategoryEntity category;
    private LocationEntity location;

    private String citizen1Token;
    private String citizen2Token;
    private String officerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        String suffix = UUID.randomUUID().toString().substring(0, 5);

        citizen1 = userRepository.save(new UserEntity("+9190000" + suffix, "Citizen One"));
        citizen2 = userRepository.save(new UserEntity("+9190001" + suffix, "Citizen Two"));
        
        officer = new UserEntity("+9190002" + suffix, "Officer Smith");
        officer.setRole("OFFICER");
        officer = userRepository.save(officer);

        citizen1Token = jwtService.generateAccessToken(citizen1.getId(), citizen1.getRole());
        citizen2Token = jwtService.generateAccessToken(citizen2.getId(), citizen2.getRole());
        officerToken = jwtService.generateAccessToken(officer.getId(), officer.getRole());

        category = categoryRepository.save(new CategoryEntity("Infrastructure " + suffix, "infra-" + suffix, "Roads and bridges", 1));
        location = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @Test
    void testNotificationCreationAndRetrieval() {
        // A. Notification Creation
        NotificationEntity notif = notificationService.createNotification(
                citizen1,
                NotificationType.ISSUE_STATUS_CHANGED,
                "Status Updated",
                "Your issue is now in progress.",
                null,
                "event-key-1",
                null
        );
        assertNotNull(notif);
        assertEquals(citizen1.getId(), notif.getUser().getId());

        // B. Notification Retrieval & Q. Pagination & R. Privacy
        try {
            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", "Bearer " + citizen1Token)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(notif.getId().toString())))
                    .andExpect(jsonPath("$.content[0].type", is("ISSUE_STATUS_CHANGED")))
                    .andExpect(jsonPath("$.content[0].title", is("Status Updated")))
                    .andExpect(jsonPath("$.content[0].read", is(false)))
                    .andExpect(jsonPath("$.content[0].phone").doesNotExist())
                    .andExpect(jsonPath("$.content[0].supporterIds").doesNotExist());
        } catch (Exception e) {
            fail("API call failed: " + e.getMessage());
        }
    }

    @Test
    void testCrossUserProtection() throws Exception {
        // C. Cross-user protection
        notificationService.createNotification(
                citizen1,
                NotificationType.ISSUE_STATUS_CHANGED,
                "Private Alert",
                "For citizen 1 only",
                null,
                "key-c1",
                null
        );

        // Citizen 2 retrieves notifications -> should get empty content
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + citizen2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void testMarkAsReadAndCrossUserProtection() throws Exception {
        NotificationEntity notif1 = notificationService.createNotification(
                citizen1,
                NotificationType.ISSUE_VERIFIED,
                "Verified",
                "Issue verified",
                null,
                "key-m1",
                null
        );

        // E. Cross-user mark protection: citizen 2 attempts to mark citizen 1's notification read
        mockMvc.perform(patch("/api/notifications/" + notif1.getId() + "/read")
                        .header("Authorization", "Bearer " + citizen2Token))
                .andExpect(status().isForbidden());

        // D. Mark as read: citizen 1 marks their own notification as read
        mockMvc.perform(patch("/api/notifications/" + notif1.getId() + "/read")
                        .header("Authorization", "Bearer " + citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read", is(true)));

        // Verify idempotency: calling mark as read again remains 200 OK
        mockMvc.perform(patch("/api/notifications/" + notif1.getId() + "/read")
                        .header("Authorization", "Bearer " + citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read", is(true)));
    }

    @Test
    void testMarkAllAsReadAndUnreadCount() throws Exception {
        notificationService.createNotification(citizen1, NotificationType.ISSUE_ACKNOWLEDGED, "Ack 1", "Body 1", null, "k1", null);
        notificationService.createNotification(citizen1, NotificationType.ISSUE_IN_PROGRESS, "Prog 2", "Body 2", null, "k2", null);

        // G. Unread count
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)));

        // F. Mark all read
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + citizen1Token))
                .andExpect(status().isOk());

        // Verify unread count becomes 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(0)));
    }

    @Test
    void testStatusTransitionsAndResolutionNotifications() {
        IssueEntity issue = issueService.createIssue(citizen1, category.getId(), location.getId(), "Pothole on Main St", "Deep hole");

        // Citizen 2 supports the issue
        supportRepository.save(new SupportEntity(issue, citizen2));

        // Officer transitions status: REPORTED -> VERIFIED
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        officer.getId(), null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OFFICER"))
                )
        );
        statusHistoryService.changeStatus(issue.getId(), new ChangeStatusRequest(IssueStatus.VERIFIED, null));

        // H. Status transition creates notification for reporter (citizen1) and supporter (citizen2)
        List<NotificationEntity> c1Notifs = notificationRepository.findByUser_IdOrderByCreatedAtDesc(citizen1.getId(), null).getContent();
        assertTrue(c1Notifs.stream().anyMatch(n -> n.getType() == NotificationType.ISSUE_VERIFIED));

        List<NotificationEntity> c2Notifs = notificationRepository.findByUser_IdOrderByCreatedAtDesc(citizen2.getId(), null).getContent();
        assertTrue(c2Notifs.stream().anyMatch(n -> n.getType() == NotificationType.ISSUE_VERIFIED));

        // Officer transitions: VERIFIED -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED
        statusHistoryService.changeStatus(issue.getId(), new ChangeStatusRequest(IssueStatus.ACKNOWLEDGED, null));
        statusHistoryService.changeStatus(issue.getId(), new ChangeStatusRequest(IssueStatus.IN_PROGRESS, null));
        statusHistoryService.changeStatus(issue.getId(), new ChangeStatusRequest(IssueStatus.RESOLVED, null));

        // J. Resolution creates ISSUE_RESOLVED notification
        List<NotificationEntity> c1ResolvedNotifs = notificationRepository.findByUser_IdOrderByCreatedAtDesc(citizen1.getId(), null).getContent();
        assertTrue(c1ResolvedNotifs.stream().anyMatch(n -> n.getType() == NotificationType.ISSUE_RESOLVED));

        // K. Reporter marks NOT_FIXED -> creates ISSUE_NOT_FIXED notification
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        citizen1.getId(), null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CITIZEN"))
                )
        );
        statusHistoryService.verifyResolution(issue.getId(), new VerifyResolutionRequest(false, "Still present"));
        List<NotificationEntity> notFixedNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.ISSUE_NOT_FIXED)
                .toList();
        assertFalse(notFixedNotifs.isEmpty());
    }

    @Test
    void testFailedStatusTransitionCreatesNoNotification() {
        IssueEntity issue = issueService.createIssue(citizen1, category.getId(), location.getId(), "Broken streetlight", "Dark alley");

        long countBefore = notificationRepository.count();

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        officer.getId(), null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OFFICER"))
                )
        );

        // I. Invalid transition (REPORTED cannot transition directly to RESOLVED)
        assertThrows(IllegalArgumentException.class, () ->
                statusHistoryService.changeStatus(issue.getId(), new ChangeStatusRequest(IssueStatus.RESOLVED, null))
        );

        long countAfter = notificationRepository.count();
        assertEquals(countBefore, countAfter);
    }

    @Test
    void testCommentNotifications() {
        IssueEntity issue = issueService.createIssue(citizen1, category.getId(), location.getId(), "Garbage dump", "Piles of trash");

        // Citizen 2 comments on Citizen 1's issue -> N. Comment on another user's issue notifies reporter
        notificationService.handleCommentAdded(issue, citizen2);

        List<NotificationEntity> c1Notifs = notificationRepository.findByUser_IdOrderByCreatedAtDesc(citizen1.getId(), null).getContent();
        assertTrue(c1Notifs.stream().anyMatch(n -> n.getType() == NotificationType.ISSUE_COMMENT_ACTIVITY));

        // Reporter (citizen1) comments on their own issue -> should NOT notify citizen1 again for own comment
        long c1CountBefore = notificationRepository.countByUser_IdAndReadAtIsNull(citizen1.getId());
        notificationService.handleCommentAdded(issue, citizen1);
        long c1CountAfter = notificationRepository.countByUser_IdAndReadAtIsNull(citizen1.getId());
        assertEquals(c1CountBefore, c1CountAfter);
    }

    @Test
    void testPriorityChangeNotification() {
        IssueEntity issue = issueService.createIssue(citizen1, category.getId(), location.getId(), "Flooding", "Waterlogging", IssueSeverity.LOW);

        // M. Change severity/priority to HIGH
        issue.setSeverity(IssueSeverity.CRITICAL);
        issueRepository.save(issue);

        issuePriorityService.calculatePriority(issue);

        List<NotificationEntity> c1Notifs = notificationRepository.findByUser_IdOrderByCreatedAtDesc(citizen1.getId(), null).getContent();
        assertTrue(c1Notifs.stream().anyMatch(n -> n.getType() == NotificationType.ISSUE_PRIORITY_CHANGED));

        // Same priority level recalculation -> no new notification
        int notifCountBefore = c1Notifs.size();
        issuePriorityService.calculatePriority(issue);
        int notifCountAfter = notificationRepository.findByUser_IdOrderByCreatedAtDesc(citizen1.getId(), null).getContent().size();
        assertEquals(notifCountBefore, notifCountAfter);
    }

    @Test
    void testNotificationPreferences() throws Exception {
        // O. Disabled in-app notifications prevent creation
        mockMvc.perform(patch("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + citizen1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNotificationPreferenceRequest(false, false, false, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled", is(false)));

        // Attempt creation when in-app is disabled
        NotificationEntity notif = notificationService.createNotification(
                citizen1,
                NotificationType.ISSUE_STATUS_CHANGED,
                "Suppressed Title",
                "Suppressed Body",
                null,
                "suppressed-key",
                null
        );
        assertNull(notif);

        // GET preferences
        mockMvc.perform(get("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + citizen1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inAppEnabled", is(false)));
    }

    @Test
    void testDuplicateNotificationProtection() {
        // P. Idempotency protection
        NotificationEntity n1 = notificationService.createNotification(
                citizen1,
                NotificationType.ISSUE_VERIFIED,
                "Title",
                "Body",
                null,
                "idempotent-event-key-100",
                null
        );
        assertNotNull(n1);

        NotificationEntity n2 = notificationService.createNotification(
                citizen1,
                NotificationType.ISSUE_VERIFIED,
                "Title",
                "Body",
                null,
                "idempotent-event-key-100",
                null
        );
        assertNull(n2);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        supportRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
    }
}
