package org.nagrivic.modules.authorities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.activity.entity.IssueActivityEntity;
import org.nagrivic.modules.activity.model.IssueActivityType;
import org.nagrivic.modules.activity.repository.IssueActivityRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.authorities.entity.AuthorityAssignmentEntity;
import org.nagrivic.modules.authorities.model.ResolutionEvidenceType;
import org.nagrivic.modules.authorities.repository.AuthorityAssignmentRepository;
import org.nagrivic.modules.authorities.repository.ResolutionEvidenceRepository;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.repository.LocationRepository;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.moderation.service.ContentAbuseValidator;
import org.nagrivic.modules.statushistory.repository.StatusHistoryRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ResolutionEvidenceTest {

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
    private AuthorityAssignmentRepository authorityAssignmentRepository;

    @Autowired
    private ResolutionEvidenceRepository resolutionEvidenceRepository;

    @Autowired
    private CivicBodyRepository civicBodyRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

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
    private IssueActivityRepository issueActivityRepository;

    @Autowired
    private ContentAbuseValidator contentAbuseValidator;

    private static final AtomicInteger ISSUE_SEQ = new AtomicInteger(100);

    private UserEntity citizenUser;
    private String citizenToken;

    private UserEntity moderatorUser;
    private String moderatorToken;

    private UserEntity officerWard1;
    private String officerWard1Token;

    private UserEntity officerWard2;
    private String officerWard2Token;

    private UserEntity adminUser;
    private String adminToken;

    private CivicBodyEntity amcBody;
    private CityEntity ahmedabadCity;
    private WardEntity wardNavrangpura;
    private WardEntity wardBodakdev;
    private DepartmentEntity deptRoads;
    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    // Valid JPEG byte header
    private static final byte[] VALID_JPEG = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    // Valid PNG byte header
    private static final byte[] VALID_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        contentAbuseValidator.reset();
        resolutionEvidenceRepository.deleteAll();
        authorityAssignmentRepository.deleteAll();
        issueActivityRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        departmentRepository.deleteAll();
        wardRepository.deleteAll();
        cityRepository.deleteAll();
        civicBodyRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        // Users
        citizenUser = userRepository.save(new UserEntity("+919700000001", "Citizen Reporter"));
        citizenToken = jwtService.generateAccessToken(citizenUser.getId(), citizenUser.getRole());

        moderatorUser = new UserEntity("+919700000002", "Moderator Mary");
        moderatorUser.setRole("MODERATOR");
        moderatorUser = userRepository.save(moderatorUser);
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), moderatorUser.getRole());

        officerWard1 = new UserEntity("+919700000003", "Officer Navrangpura");
        officerWard1.setRole("OFFICER");
        officerWard1 = userRepository.save(officerWard1);
        officerWard1Token = jwtService.generateAccessToken(officerWard1.getId(), officerWard1.getRole());

        officerWard2 = new UserEntity("+919700000004", "Officer Bodakdev");
        officerWard2.setRole("OFFICER");
        officerWard2 = userRepository.save(officerWard2);
        officerWard2Token = jwtService.generateAccessToken(officerWard2.getId(), officerWard2.getRole());

        adminUser = new UserEntity("+919700000005", "Admin Superuser");
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getRole());

        // Geography & Depts
        amcBody = civicBodyRepository.save(new CivicBodyEntity("AMC", CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Ahmedabad"));
        ahmedabadCity = cityRepository.save(new CityEntity("Ahmedabad", "Gujarat", amcBody));
        wardNavrangpura = wardRepository.save(new WardEntity(ahmedabadCity, amcBody, "014", "Navrangpura"));
        wardBodakdev = wardRepository.save(new WardEntity(ahmedabadCity, amcBody, "015", "Bodakdev"));
        deptRoads = departmentRepository.save(new DepartmentEntity(amcBody, "Roads Dept", "ENG-ROADS", "Roads"));

        testCategory = categoryRepository.save(new CategoryEntity("Potholes", "potholes", "Potholes", 1));
        testLocation = locationService.createLocation(23.0338, 72.5644, new BigDecimal("5.00"));

        // officerWard1 -> Navrangpura
        authorityAssignmentRepository.save(new AuthorityAssignmentEntity(
                officerWard1, amcBody, ahmedabadCity, wardNavrangpura, null, "Ward Inspector"
        ));

        // officerWard2 -> Bodakdev
        authorityAssignmentRepository.save(new AuthorityAssignmentEntity(
                officerWard2, amcBody, ahmedabadCity, wardBodakdev, null, "Ward Inspector"
        ));
    }

    private IssueEntity createTestIssue(WardEntity ward, IssueStatus status) {
        contentAbuseValidator.reset();
        int seq = ISSUE_SEQ.incrementAndGet();
        IssueEntity issue = issueService.createIssue(
                citizenUser,
                testCategory.getId(),
                testLocation.getId(),
                "Damaged asphalt " + seq,
                "Deep crater " + seq
        );
        issue.setCivicBody(amcBody);
        issue.setCity(ahmedabadCity);
        issue.setWard(ward);
        issue.setDepartment(deptRoads);
        issue.setResponsibilityStatus(ResponsibilityStatus.RESOLVED);
        issue.setResponsibilityResolvedAt(Instant.now());
        issue.setResponsibilitySource("TEST");
        issue.setStatus(status);
        return issueRepository.save(issue);
    }

    @Test
    @DisplayName("Should block unauthorized roles from uploading resolution evidence")
    void testRbacUpload() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, IssueStatus.IN_PROGRESS);

        MockMultipartFile file = new MockMultipartFile("file", "completion.jpg", "image/jpeg", VALID_JPEG);

        // 1. Unauthenticated -> 401
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO"))
                .andExpect(status().isUnauthorized());

        // 2. Citizen role -> 403 Forbidden
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // 3. Moderator role -> 403 Forbidden
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should prevent IDOR: Officer outside ward scope cannot upload evidence")
    void testIdorScopeProtection() throws Exception {
        // Issue belongs to Bodakdev ward
        IssueEntity issueBodakdev = createTestIssue(wardBodakdev, IssueStatus.IN_PROGRESS);

        MockMultipartFile file = new MockMultipartFile("file", "completion.jpg", "image/jpeg", VALID_JPEG);

        // Officer 1 (Navrangpura) attempts to upload to Bodakdev issue -> 403 Forbidden
        mockMvc.perform(multipart("/api/authority/issues/" + issueBodakdev.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isForbidden());

        // Officer 2 (Bodakdev) can upload -> 201 Created
        mockMvc.perform(multipart("/api/authority/issues/" + issueBodakdev.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .param("note", "Road surface reconstructed")
                        .header("Authorization", "Bearer " + officerWard2Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenceType").value("COMPLETION_PHOTO"))
                .andExpect(jsonPath("$.mediaUrl", notNullValue()));
    }

    @Test
    @DisplayName("Should enforce state rules: Evidence allowed in IN_PROGRESS and RESOLVED, rejected in other states")
    void testIssueStatePolicy() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "done.jpg", "image/jpeg", VALID_JPEG);

        // 1. REPORTED status -> Rejected (400 Bad Request)
        IssueEntity issueReported = createTestIssue(wardNavrangpura, IssueStatus.REPORTED);
        mockMvc.perform(multipart("/api/authority/issues/" + issueReported.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());

        // 2. VERIFIED status -> Rejected (400)
        IssueEntity issueVerified = createTestIssue(wardNavrangpura, IssueStatus.VERIFIED);
        mockMvc.perform(multipart("/api/authority/issues/" + issueVerified.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());

        // 3. ACKNOWLEDGED status -> Rejected (400)
        IssueEntity issueAck = createTestIssue(wardNavrangpura, IssueStatus.ACKNOWLEDGED);
        mockMvc.perform(multipart("/api/authority/issues/" + issueAck.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());

        // 4. IN_PROGRESS status -> Allowed (201)
        IssueEntity issueInProgress = createTestIssue(wardNavrangpura, IssueStatus.IN_PROGRESS);
        mockMvc.perform(multipart("/api/authority/issues/" + issueInProgress.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isCreated());

        // 5. RESOLVED status -> Allowed (201)
        IssueEntity issueResolved = createTestIssue(wardNavrangpura, IssueStatus.RESOLVED);
        mockMvc.perform(multipart("/api/authority/issues/" + issueResolved.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "BEFORE_AFTER_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isCreated());

        // Verify status remains unchanged when evidence is attached
        IssueEntity refreshed = issueInProgress = issueRepository.findById(issueInProgress.getId()).orElseThrow();
        assertEquals(IssueStatus.IN_PROGRESS, refreshed.getStatus());
    }

    @Test
    @DisplayName("Should validate evidence types: COMPLETION_NOTE (file optional, note required) and COMPLETION_PHOTO (file required)")
    void testEvidenceTypeValidations() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, IssueStatus.IN_PROGRESS);

        // COMPLETION_NOTE without note -> 400 Bad Request
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .param("evidenceType", "COMPLETION_NOTE")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());

        // COMPLETION_NOTE with valid note -> 201 Created (no file needed)
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .param("evidenceType", "COMPLETION_NOTE")
                        .param("note", "Pothole filled with asphalt, steamroller compacted.")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenceType").value("COMPLETION_NOTE"))
                .andExpect(jsonPath("$.note").value("Pothole filled with asphalt, steamroller compacted."))
                .andExpect(jsonPath("$.mediaUrl").doesNotExist());

        // COMPLETION_PHOTO without file -> 400 Bad Request
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject invalid image formats and corrupt magic bytes")
    void testMediaValidation() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, IssueStatus.IN_PROGRESS);

        // 1. Text file pretending to be jpg -> 400 Bad Request (Magic byte mismatch)
        MockMultipartFile fakeFile = new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not a real image".getBytes());
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(fakeFile)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());

        // 2. Unsupported MIME type (PDF) -> 400 Bad Request
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-1.4".getBytes());
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(pdfFile)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());

        // 3. Valid PNG -> 201 Created
        MockMultipartFile pngFile = new MockMultipartFile("file", "photo.png", "image/png", VALID_PNG);
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(pngFile)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaUrl", containsString("/api/media/resolution-evidence/")));
    }

    @Test
    @DisplayName("Should validate note constraints and future captured timestamps")
    void testNoteAndTimestampValidation() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, IssueStatus.IN_PROGRESS);

        // 1. Whitespace-only note -> 400 Bad Request
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .param("evidenceType", "COMPLETION_NOTE")
                        .param("note", "   \t\n  ")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());

        // 2. Future timestamp (> 15 min ahead) -> 400 Bad Request
        Instant future = Instant.now().plus(2, ChronoUnit.HOURS);
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .param("evidenceType", "COMPLETION_NOTE")
                        .param("note", "Valid note")
                        .param("capturedAt", future.toString())
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should record RESOLUTION_EVIDENCE_ADDED in activity timeline and not alter status history")
    void testActivityTimelineAndStatusHistoryIntegrity() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, IssueStatus.IN_PROGRESS);
        int initialHistoryCount = statusHistoryRepository.findByIssue_IdOrderByCreatedAtAsc(issue.getId()).size();

        MockMultipartFile file = new MockMultipartFile("file", "done.jpg", "image/jpeg", VALID_JPEG);

        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .param("note", "Field repairs completed satisfactorily")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isCreated());

        // Activity timeline check
        List<IssueActivityEntity> activities = issueActivityRepository.findByIssue_Id(issue.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        boolean hasEvidenceActivity = activities.stream()
                .anyMatch(a -> a.getEventType() == IssueActivityType.RESOLUTION_EVIDENCE_ADDED);
        assertTrue(hasEvidenceActivity, "Activity stream must contain RESOLUTION_EVIDENCE_ADDED event");

        // Status history must NOT have recorded a fake status change
        int finalHistoryCount = statusHistoryRepository.findByIssue_IdOrderByCreatedAtAsc(issue.getId()).size();
        assertEquals(initialHistoryCount, finalHistoryCount, "Adding evidence must not create a status history transition");
    }

    @Test
    @DisplayName("Public API check: Citizen can query resolution evidence safely with zero PII")
    void testPublicEvidenceList() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, IssueStatus.RESOLVED);

        MockMultipartFile file = new MockMultipartFile("file", "done.jpg", "image/jpeg", VALID_JPEG);

        // Upload evidence as officer
        mockMvc.perform(multipart("/api/authority/issues/" + issue.getId() + "/resolution-evidence")
                        .file(file)
                        .param("evidenceType", "COMPLETION_PHOTO")
                        .param("note", "All debris cleared from roadway")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isCreated());

        // Query public endpoint without auth
        mockMvc.perform(get("/api/issues/" + issue.getId() + "/resolution-evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].evidenceType").value("COMPLETION_PHOTO"))
                .andExpect(jsonPath("$[0].note").value("All debris cleared from roadway"))
                .andExpect(jsonPath("$[0].submittedByRole").value("OFFICER"))
                .andExpect(content().string(not(containsString("+919700000003")))) // No officer phone
                .andExpect(content().string(not(containsString("+919700000001")))); // No citizen phone
    }

    @AfterEach
    void tearDown() {
        resolutionEvidenceRepository.deleteAll();
        authorityAssignmentRepository.deleteAll();
        issueActivityRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        issueRepository.deleteAll();
        categoryRepository.deleteAll();
        departmentRepository.deleteAll();
        wardRepository.deleteAll();
        cityRepository.deleteAll();
        civicBodyRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
    }
}
