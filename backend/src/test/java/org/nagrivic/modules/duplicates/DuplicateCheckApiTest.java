package org.nagrivic.modules.duplicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.duplicates.dto.CheckDuplicatesRequest;
import org.nagrivic.modules.duplicates.dto.LinkDuplicateRequest;
import org.nagrivic.modules.duplicates.service.DuplicateDetectionService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.supports.entity.SupportEntity;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.junit.jupiter.api.AfterEach;
import org.nagrivic.modules.auth.repository.AuthSessionRepository;
import org.nagrivic.modules.auth.repository.OtpVerificationRepository;
import org.nagrivic.modules.media.repository.MediaRepository;
import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DuplicateCheckApiTest {

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
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private org.nagrivic.modules.activity.repository.IssueActivityRepository issueActivityRepository;

    @Autowired
    private org.nagrivic.modules.statushistory.repository.StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private org.nagrivic.modules.notifications.repository.NotificationRepository notificationRepository;

    @Autowired
    private org.nagrivic.modules.notifications.repository.NotificationPreferenceRepository notificationPreferenceRepository;

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
    private DuplicateDetectionService duplicateDetectionService;

    private UserEntity citizen;
    private String citizenToken;

    private UserEntity officer;
    private String officerToken;

    private CategoryEntity potholeCategory;
    private CategoryEntity garbageCategory;
    private LocationEntity centralLocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanDatabase();

        citizen = userRepository.save(new UserEntity("+919876543210", "Citizen Test"));
        citizenToken = jwtService.generateAccessToken(citizen.getId(), citizen.getRole());

        officer = new UserEntity("+919876543211", "Officer Test");
        officer.setRole("OFFICER");
        officer = userRepository.save(officer);
        officerToken = jwtService.generateAccessToken(officer.getId(), officer.getRole());

        potholeCategory = categoryRepository.save(new CategoryEntity("Roads / Potholes", "roads-potholes", "Pothole issues", 1));
        garbageCategory = categoryRepository.save(new CategoryEntity("Waste Management", "waste-management", "Garbage issues", 2));

        // Central coordinate: Ahmedabad (23.0225, 72.5714)
        centralLocation = locationService.createLocation(23.0225, 72.5714, new BigDecimal("5.00"));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        mediaRepository.deleteAll();
        supportRepository.deleteAll();
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        issueActivityRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        authSessionRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    void shouldRejectUnauthenticatedDuplicateCheck() throws Exception {
        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Pothole on Main Road",
                "Deep crater",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {
        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Pothole on Main Road",
                "Deep crater",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer invalid-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInactiveCitizen() throws Exception {
        UserEntity inactiveUser = new UserEntity("+919876543299", "Inactive User");
        inactiveUser.setActive(false);
        inactiveUser = userRepository.save(inactiveUser);
        String inactiveToken = jwtService.generateAccessToken(inactiveUser.getId(), inactiveUser.getRole());

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Pothole on Main Road",
                "Deep crater",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + inactiveToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnEmptyWhenNoExistingIssues() throws Exception {
        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Pothole on Main Road",
                "Deep crater",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(false))
                .andExpect(jsonPath("$.candidates").isArray())
                .andExpect(jsonPath("$.candidates", hasSize(0)));
    }

    @Test
    void shouldNotCreateAnyIssueDuringDuplicateCheck() throws Exception {
        long countBefore = issueRepository.count();

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Pothole on Main Road",
                "Deep crater",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        long countAfter = issueRepository.count();
        assertEquals(countBefore, countAfter, "check-duplicates endpoint MUST NOT persist any issue");
    }

    @Test
    void shouldDetectNearbySameCategoryIssueAsCandidate() throws Exception {
        // Create an existing issue ~30m away
        LocationEntity nearbyLocation = locationService.createLocation(23.0227, 72.5716, new BigDecimal("5.00"));
        IssueEntity existingIssue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                nearbyLocation.getId(),
                "Existing Pothole Near Signal",
                "Needs filling"
        );

        // Add 2 supports to existing issue
        UserEntity supporter1 = userRepository.save(new UserEntity("+919876543201", "Supporter 1"));
        UserEntity supporter2 = userRepository.save(new UserEntity("+919876543202", "Supporter 2"));
        supportRepository.save(new SupportEntity(existingIssue, supporter1));
        supportRepository.save(new SupportEntity(existingIssue, supporter2));

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Dangerous pothole",
                "Report from citizen",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(true))
                .andExpect(jsonPath("$.candidates", hasSize(1)))
                .andExpect(jsonPath("$.candidates[0].issueId").value(existingIssue.getId().toString()))
                .andExpect(jsonPath("$.candidates[0].title").value("Existing Pothole Near Signal"))
                .andExpect(jsonPath("$.candidates[0].category.id").value(potholeCategory.getId().toString()))
                .andExpect(jsonPath("$.candidates[0].category.name").value("Roads / Potholes"))
                .andExpect(jsonPath("$.candidates[0].category.slug").value("roads-potholes"))
                .andExpect(jsonPath("$.candidates[0].status").value("REPORTED"))
                .andExpect(jsonPath("$.candidates[0].supportCount").value(2))
                .andExpect(jsonPath("$.candidates[0].distanceMeters").isNumber())
                // Ensure no private PII is leaked
                .andExpect(jsonPath("$.candidates[0].reporterPhone").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].userId").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].storagePath").doesNotExist());
    }

    @Test
    void shouldReturnDuplicateCandidatesUsingCoordinates() throws Exception {
        // Create an existing issue within ~50m of coordinates (23.0225, 72.5714)
        LocationEntity nearbyLocation = locationService.createLocation(23.0228, 72.5716, new BigDecimal("4.00"));
        IssueEntity existingIssue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                nearbyLocation.getId(),
                "Existing Coordinate Pothole",
                "Needs repair urgently"
        );

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Dangerous road crater",
                "Found pothole near crossroads",
                potholeCategory.getId(),
                null,
                23.0225,
                72.5714,
                100.0
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(true))
                .andExpect(jsonPath("$.candidates", hasSize(1)))
                .andExpect(jsonPath("$.candidates[0].issueId").value(existingIssue.getId().toString()))
                .andExpect(jsonPath("$.candidates[0].title").value("Existing Coordinate Pothole"))
                .andExpect(jsonPath("$.candidates[0].distanceMeters").isNumber());
    }

    @Test
    void shouldExcludeDifferentCategoryEvenIfNearby() throws Exception {
        // Garbage issue at exactly the same location
        issueService.createIssue(
                citizen,
                garbageCategory.getId(),
                centralLocation.getId(),
                "Garbage Dump Overflow",
                "Smelly dump"
        );

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Pothole issue",
                "Pothole description",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(false))
                .andExpect(jsonPath("$.candidates", hasSize(0)));
    }

    @Test
    void shouldExcludeFarAwayIssueEvenIfSameCategory() throws Exception {
        // Far away location in Mumbai (~450 km away)
        LocationEntity mumbaiLocation = locationService.createLocation(19.0760, 72.8777, new BigDecimal("5.00"));
        issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                mumbaiLocation.getId(),
                "Mumbai Pothole",
                "Far away"
        );

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Ahmedabad Pothole",
                "Central pothole",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(false))
                .andExpect(jsonPath("$.candidates", hasSize(0)));
    }

    @Test
    void shouldLimitCandidatesToMaximumFive() throws Exception {
        // Create 7 nearby issues in the same category
        for (int i = 1; i <= 7; i++) {
            LocationEntity loc = locationService.createLocation(
                    23.0225 + (i * 0.0001),
                    72.5714 + (i * 0.0001),
                    new BigDecimal("5.00")
            );
            issueService.createIssue(
                    citizen,
                    potholeCategory.getId(),
                    loc.getId(),
                    "Pothole " + i,
                    "Description " + i
            );
        }

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "New Pothole",
                "Testing candidate limit",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(true))
                .andExpect(jsonPath("$.candidates", hasSize(5))); // Capped at 5
    }

    @Test
    void shouldNotIncludeAlreadyLinkedDuplicateIssuesInCandidates() throws Exception {
        // Primary issue A
        IssueEntity primaryA = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                centralLocation.getId(),
                "Primary Pothole A",
                "Primary report"
        );

        // Duplicate issue B
        IssueEntity duplicateB = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                centralLocation.getId(),
                "Duplicate Pothole B",
                "Second report"
        );

        // Link B as duplicate of A
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        officer.getId(), null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OFFICER"))
                )
        );
        duplicateDetectionService.linkDuplicate(duplicateB.getId(), primaryA.getId());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        CheckDuplicatesRequest request = new CheckDuplicatesRequest(
                "Third Pothole",
                "New report",
                potholeCategory.getId(),
                centralLocation.getId()
        );

        // Candidate search should return primaryA, but NOT duplicateB
        mockMvc.perform(post("/api/issues/check-duplicates")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPotentialDuplicates").value(true))
                .andExpect(jsonPath("$.candidates", hasSize(1)))
                .andExpect(jsonPath("$.candidates[0].issueId").value(primaryA.getId().toString()));
    }

    @Test
    void shouldSupportCandidateIssueDirectlyViaExistingSupportApi() throws Exception {
        IssueEntity existingIssue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                centralLocation.getId(),
                "Existing Pothole",
                "Needs repair"
        );

        // Supporter citizen discovers existing issue from check-duplicates and supports it
        UserEntity newCitizen = userRepository.save(new UserEntity("+919876543222", "New Citizen"));
        String newCitizenToken = jwtService.generateAccessToken(newCitizen.getId(), newCitizen.getRole());

        mockMvc.perform(post("/api/issues/" + existingIssue.getId() + "/support")
                        .header("Authorization", "Bearer " + newCitizenToken))
                .andExpect(status().isCreated());

        // GET issue confirms support count and supportedByCurrentUser
        mockMvc.perform(get("/api/issues/" + existingIssue.getId())
                        .header("Authorization", "Bearer " + newCitizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportCount").value(1))
                .andExpect(jsonPath("$.supportedByCurrentUser").value(true))
                .andExpect(jsonPath("$.isDuplicate").value(false));
    }

    @Test
    void shouldRejectCitizenAttemptingToLinkDuplicatesDirectly() throws Exception {
        IssueEntity primary = issueService.createIssue(citizen, potholeCategory.getId(), centralLocation.getId(), "Issue 1", "Pothole");
        IssueEntity dup = issueService.createIssue(citizen, potholeCategory.getId(), centralLocation.getId(), "Issue 2", "Pothole dup");

        LinkDuplicateRequest linkRequest = new LinkDuplicateRequest(primary.getId());

        // Normal citizen attempting to merge issues directly must be rejected with 403 Forbidden
        mockMvc.perform(post("/api/issues/" + dup.getId() + "/duplicate")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOfficerToLinkDuplicate() throws Exception {
        IssueEntity primary = issueService.createIssue(citizen, potholeCategory.getId(), centralLocation.getId(), "Primary Road Issue", "Pothole");
        IssueEntity dup = issueService.createIssue(citizen, potholeCategory.getId(), centralLocation.getId(), "Duplicate Road Issue", "Pothole dup");

        LinkDuplicateRequest linkRequest = new LinkDuplicateRequest(primary.getId());

        mockMvc.perform(post("/api/issues/" + dup.getId() + "/duplicate")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDuplicate").value(true))
                .andExpect(jsonPath("$.primaryIssueId").value(primary.getId().toString()));
    }
}
