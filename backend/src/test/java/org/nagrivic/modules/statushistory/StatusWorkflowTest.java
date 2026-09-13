package org.nagrivic.modules.statushistory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.dto.StatusHistoryResponse;
import org.nagrivic.modules.statushistory.dto.VerifyResolutionRequest;
import org.nagrivic.modules.statushistory.entity.StatusHistoryEntity;
import org.nagrivic.modules.statushistory.repository.StatusHistoryRepository;
import org.nagrivic.modules.statushistory.service.StatusHistoryService;
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
class StatusWorkflowTest {

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
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private StatusHistoryService statusHistoryService;

    private UserEntity reporterCitizen;
    private String reporterToken;

    private UserEntity otherCitizen;
    private String otherCitizenToken;

    private UserEntity officerUser;
    private String officerToken;

    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        statusHistoryRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        reporterCitizen = userRepository.save(new UserEntity("+919876543210", "Reporter Citizen"));
        reporterToken = jwtService.generateAccessToken(reporterCitizen.getId(), reporterCitizen.getRole());

        otherCitizen = userRepository.save(new UserEntity("+919876543211", "Other Citizen"));
        otherCitizenToken = jwtService.generateAccessToken(otherCitizen.getId(), otherCitizen.getRole());

        officerUser = new UserEntity("+919876543212", "Officer Patel");
        officerUser.setRole("OFFICER");
        officerUser = userRepository.save(officerUser);
        officerToken = jwtService.generateAccessToken(officerUser.getId(), officerUser.getRole());

        testCategory = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        testLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @Test
    void shouldInitializeNewIssueWithReportedStatusAndInitialHistoryRecord() {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Damaged Road",
                "Deep crater near school"
        );

        assertEquals(IssueStatus.REPORTED, issue.getStatus());

        List<StatusHistoryEntity> historyList = statusHistoryRepository.findByIssue_IdOrderByCreatedAtAsc(issue.getId());
        assertEquals(1, historyList.size());

        StatusHistoryEntity initial = historyList.get(0);
        assertNull(initial.getFromStatus());
        assertEquals(IssueStatus.REPORTED, initial.getToStatus());
        assertEquals(reporterCitizen.getId(), initial.getChangedByUser().getId());
        assertNull(initial.getReason());
        assertNotNull(initial.getCreatedAt());
    }

    @Test
    void shouldExecuteCompleteHappyPathWorkflow() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Main Street Pothole",
                "Needs repair"
        );

        // 1. REPORTED -> VERIFIED (Officer)
        ChangeStatusRequest verifyReq = new ChangeStatusRequest(IssueStatus.VERIFIED, "Verified by ground inspector");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // 2. VERIFIED -> ACKNOWLEDGED (Officer)
        ChangeStatusRequest ackReq = new ChangeStatusRequest(IssueStatus.ACKNOWLEDGED, "Assigned to Ward Road Repair Crew");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        // 3. ACKNOWLEDGED -> IN_PROGRESS (Officer)
        ChangeStatusRequest progressReq = new ChangeStatusRequest(IssueStatus.IN_PROGRESS, "Crew started hot-mix application");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progressReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 4. IN_PROGRESS -> RESOLVED (Officer)
        ChangeStatusRequest resolveReq = new ChangeStatusRequest(IssueStatus.RESOLVED, "Pothole filled and sealed");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // 5. RESOLVED -> CITIZEN_VERIFIED (Reporter Citizen)
        VerifyResolutionRequest citizenVerifyReq = new VerifyResolutionRequest(true, "Looks good, asphalt is dry");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/verify-resolution")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citizenVerifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CITIZEN_VERIFIED"));

        // Verify history has 6 entries (Initial + 5 transitions)
        List<StatusHistoryEntity> history = statusHistoryRepository.findByIssue_IdOrderByCreatedAtAsc(issue.getId());
        assertEquals(6, history.size());
        assertEquals(IssueStatus.CITIZEN_VERIFIED, history.get(5).getToStatus());
    }

    @Test
    void shouldExecuteReopeningFlowWhenCitizenReportsNotFixed() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Broken Water Pipe",
                "Leaking water"
        );

        // Advance to RESOLVED
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.VERIFIED, "Checked"))));
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.ACKNOWLEDGED, "Noted"))));
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.IN_PROGRESS, "Fixing"))));
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.RESOLVED, "Repaired"))));

        // Reporter reports: fixed = false (NOT_FIXED)
        VerifyResolutionRequest notFixedReq = new VerifyResolutionRequest(false, "Water is still leaking profusely from the joint");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/verify-resolution")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notFixedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_FIXED"));

        // Officer resumes work: NOT_FIXED -> IN_PROGRESS
        ChangeStatusRequest resumeReq = new ChangeStatusRequest(IssueStatus.IN_PROGRESS, "Crew re-dispatched with heavy replacement valve");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resumeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Check history timeline shows: RESOLVED -> NOT_FIXED -> IN_PROGRESS
        List<StatusHistoryEntity> history = statusHistoryRepository.findByIssue_IdOrderByCreatedAtAsc(issue.getId());
        assertEquals(7, history.size());
        assertEquals(IssueStatus.NOT_FIXED, history.get(5).getToStatus());
        assertEquals("Water is still leaking profusely from the joint", history.get(5).getReason());
        assertEquals(IssueStatus.IN_PROGRESS, history.get(6).getToStatus());
    }

    @Test
    void shouldRejectInvalidStatusTransitions() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Pothole",
                "Description"
        );

        // REPORTED -> RESOLVED (invalid skip)
        ChangeStatusRequest skipReq = new ChangeStatusRequest(IssueStatus.RESOLVED, "Direct resolve");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skipReq)))
                .andExpect(status().isBadRequest());

        // REPORTED -> CITIZEN_VERIFIED (invalid skip)
        ChangeStatusRequest citizenSkip = new ChangeStatusRequest(IssueStatus.CITIZEN_VERIFIED, null);
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citizenSkip)))
                .andExpect(status().isBadRequest());

        // Move to VERIFIED
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.VERIFIED, "Checked"))));

        // VERIFIED -> REPORTED (backward transition invalid)
        ChangeStatusRequest backwardReq = new ChangeStatusRequest(IssueStatus.REPORTED, "Go back");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backwardReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNormalCitizenCallingOperationalStatusTransition() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Pothole",
                "Description"
        );

        // Citizen attempts to mark issue VERIFIED
        ChangeStatusRequest req = new ChangeStatusRequest(IssueStatus.VERIFIED, "Citizen says verified");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedStatusChange() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Pothole",
                "Description"
        );

        ChangeStatusRequest req = new ChangeStatusRequest(IssueStatus.VERIFIED, "Anonymous");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectResolutionVerificationByNonReporterCitizen() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Pothole",
                "Description"
        );

        // Transition to RESOLVED
        issue.setStatus(IssueStatus.RESOLVED);
        issueRepository.save(issue);

        // Other citizen attempts to verify resolution
        VerifyResolutionRequest req = new VerifyResolutionRequest(true, null);
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/verify-resolution")
                        .header("Authorization", "Bearer " + otherCitizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectResolutionVerificationWhenIssueNotResolved() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Pothole",
                "Description"
        );
        // Current status is REPORTED

        VerifyResolutionRequest req = new VerifyResolutionRequest(true, null);
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/verify-resolution")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireReasonWhenCitizenReportsNotFixed() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Pothole",
                "Description"
        );
        issue.setStatus(IssueStatus.RESOLVED);
        issueRepository.save(issue);

        // fixed = false with blank reason must fail
        VerifyResolutionRequest emptyReason = new VerifyResolutionRequest(false, "   ");
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/verify-resolution")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReason)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExposePublicStatusHistoryWithoutLeakingPrivatePII() throws Exception {
        IssueEntity issue = issueService.createIssue(
                reporterCitizen,
                testCategory.getId(),
                testLocation.getId(),
                "Pothole",
                "Description"
        );

        // Move to VERIFIED
        mockMvc.perform(post("/api/issues/" + issue.getId() + "/status")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangeStatusRequest(IssueStatus.VERIFIED, "Inspected by road officer"))));

        // GET /api/issues/{id}/status-history without authentication
        mockMvc.perform(get("/api/issues/" + issue.getId() + "/status-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(issue.getId().toString()))
                .andExpect(jsonPath("$.history", hasSize(2)))
                .andExpect(jsonPath("$.history[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$.history[0].toStatus").value("REPORTED"))
                .andExpect(jsonPath("$.history[0].changedBy.displayName").value("Citizen"))
                .andExpect(jsonPath("$.history[1].fromStatus").value("REPORTED"))
                .andExpect(jsonPath("$.history[1].toStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.history[1].changedBy.displayName").value("Municipal Officer"))
                .andExpect(jsonPath("$.history[1].reason").value("Inspected by road officer"))
                // Ensure no phone numbers or JWTs leaked
                .andExpect(jsonPath("$.history[*].changedBy.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.history[*].changedBy.password").doesNotExist())
                .andExpect(jsonPath("$.history[*].changedBy.token").doesNotExist());
    }

    @Test
    void shouldRejectStatusChangeForNonExistentIssue() throws Exception {
        UUID randomId = UUID.randomUUID();
        ChangeStatusRequest req = new ChangeStatusRequest(IssueStatus.VERIFIED, "Test");

        mockMvc.perform(post("/api/issues/" + randomId + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
