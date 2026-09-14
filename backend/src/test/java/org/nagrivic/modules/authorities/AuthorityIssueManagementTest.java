package org.nagrivic.modules.authorities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.activity.repository.IssueActivityRepository;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.authorities.entity.AuthorityAssignmentEntity;
import org.nagrivic.modules.authorities.repository.AuthorityAssignmentRepository;
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
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.statushistory.repository.StatusHistoryRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthorityIssueManagementTest {

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

    private UserEntity citizenUser;
    private String citizenToken;

    private UserEntity moderatorUser;
    private String moderatorToken;

    private UserEntity officerWard1;
    private String officerWard1Token;

    private UserEntity officerWard2;
    private String officerWard2Token;

    private UserEntity officerDeptRoads;
    private String officerDeptRoadsToken;

    private UserEntity adminUser;
    private String adminToken;

    private CivicBodyEntity amcBody;
    private CityEntity ahmedabadCity;
    private WardEntity wardNavrangpura;
    private WardEntity wardBodakdev;
    private DepartmentEntity deptRoads;
    private DepartmentEntity deptWater;
    private CategoryEntity testCategory;
    private LocationEntity testLocation;

    @Autowired
    private org.nagrivic.modules.moderation.service.ContentAbuseValidator contentAbuseValidator;

    private static final java.util.concurrent.atomic.AtomicInteger ISSUE_SEQ = new java.util.concurrent.atomic.AtomicInteger(1);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        contentAbuseValidator.reset();
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
        citizenUser = userRepository.save(new UserEntity("+919800000001", "Citizen John"));
        citizenToken = jwtService.generateAccessToken(citizenUser.getId(), citizenUser.getRole());

        moderatorUser = new UserEntity("+919800000002", "Moderator Mary");
        moderatorUser.setRole("MODERATOR");
        moderatorUser = userRepository.save(moderatorUser);
        moderatorToken = jwtService.generateAccessToken(moderatorUser.getId(), moderatorUser.getRole());

        officerWard1 = new UserEntity("+919800000003", "Officer Navrangpura");
        officerWard1.setRole("OFFICER");
        officerWard1 = userRepository.save(officerWard1);
        officerWard1Token = jwtService.generateAccessToken(officerWard1.getId(), officerWard1.getRole());

        officerWard2 = new UserEntity("+919800000004", "Officer Bodakdev");
        officerWard2.setRole("OFFICER");
        officerWard2 = userRepository.save(officerWard2);
        officerWard2Token = jwtService.generateAccessToken(officerWard2.getId(), officerWard2.getRole());

        officerDeptRoads = new UserEntity("+919800000005", "Officer Roads Dept");
        officerDeptRoads.setRole("OFFICER");
        officerDeptRoads = userRepository.save(officerDeptRoads);
        officerDeptRoadsToken = jwtService.generateAccessToken(officerDeptRoads.getId(), officerDeptRoads.getRole());

        adminUser = new UserEntity("+919800000006", "Global Administrator");
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getRole());

        // Geography & Depts
        amcBody = civicBodyRepository.save(new CivicBodyEntity("Ahmedabad Municipal Corporation", CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Ahmedabad"));
        ahmedabadCity = cityRepository.save(new CityEntity("Ahmedabad", "Gujarat", amcBody));
        wardNavrangpura = wardRepository.save(new WardEntity(ahmedabadCity, amcBody, "014", "Navrangpura"));
        wardBodakdev = wardRepository.save(new WardEntity(ahmedabadCity, amcBody, "015", "Bodakdev"));
        deptRoads = departmentRepository.save(new DepartmentEntity(amcBody, "Road Project Department", "ENG-ROADS", "Roads department"));
        deptWater = departmentRepository.save(new DepartmentEntity(amcBody, "Water Supply & Sewerage", "WATER-OPS", "Water department"));

        // Categories & Location
        testCategory = categoryRepository.save(new CategoryEntity("Pothole / Road Repair", "roads-repair", "Damaged roads", 1));
        testLocation = locationService.createLocation(23.0338, 72.5644, new BigDecimal("5.00"));

        // Assignments
        // officerWard1 -> Navrangpura Ward
        authorityAssignmentRepository.save(new AuthorityAssignmentEntity(
                officerWard1, amcBody, ahmedabadCity, wardNavrangpura, null, "Ward Inspector"
        ));

        // officerWard2 -> Bodakdev Ward
        authorityAssignmentRepository.save(new AuthorityAssignmentEntity(
                officerWard2, amcBody, ahmedabadCity, wardBodakdev, null, "Ward Inspector"
        ));

        // officerDeptRoads -> Roads Department
        authorityAssignmentRepository.save(new AuthorityAssignmentEntity(
                officerDeptRoads, amcBody, ahmedabadCity, null, deptRoads, "Chief Engineer"
        ));
    }

    private IssueEntity createTestIssue(WardEntity ward, DepartmentEntity department, ResponsibilityStatus respStatus) {
        contentAbuseValidator.reset();
        int seq = ISSUE_SEQ.incrementAndGet();
        IssueEntity issue = issueService.createIssue(
                citizenUser,
                testCategory.getId(),
                testLocation.getId(),
                "Broken asphalt on street " + seq,
                "Deep pothole causing accidents " + seq
        );
        issue.setCivicBody(amcBody);
        issue.setCity(ahmedabadCity);
        issue.setWard(ward);
        issue.setDepartment(department);
        issue.setResponsibilityStatus(respStatus);
        issue.setResponsibilityResolvedAt(respStatus == ResponsibilityStatus.RESOLVED ? Instant.now() : null);
        issue.setResponsibilitySource("TEST");
        return issueRepository.save(issue);
    }

    @Test
    @DisplayName("Should block unauthorized roles from accessing authority dashboard and issues")
    void testRbacEndpoints() throws Exception {
        // Unauthenticated
        mockMvc.perform(get("/api/authority/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/authority/issues"))
                .andExpect(status().isUnauthorized());

        // Citizen role -> 403 Forbidden
        mockMvc.perform(get("/api/authority/dashboard")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/authority/issues")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // Moderator role -> 403 Forbidden
        mockMvc.perform(get("/api/authority/dashboard")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isForbidden());

        // Officer role -> 200 OK
        mockMvc.perform(get("/api/authority/dashboard")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedScopes", hasSize(1)))
                .andExpect(jsonPath("$.assignedScopes[0].wardName").value("Navrangpura"));
    }

    @Test
    @DisplayName("Should enforce authority scope filtering: Officer only sees issues within assigned ward")
    void testScopedIssueList() throws Exception {
        IssueEntity issueNavrangpura = createTestIssue(wardNavrangpura, deptRoads, ResponsibilityStatus.RESOLVED);
        IssueEntity issueBodakdev = createTestIssue(wardBodakdev, deptRoads, ResponsibilityStatus.RESOLVED);

        // Officer 1 (Navrangpura) should see issueNavrangpura, but not issueBodakdev
        mockMvc.perform(get("/api/authority/issues")
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(issueNavrangpura.getId().toString()))
                .andExpect(jsonPath("$.content[0].wardName").value("Navrangpura"));

        // Officer 2 (Bodakdev) should see issueBodakdev, but not issueNavrangpura
        mockMvc.perform(get("/api/authority/issues")
                        .header("Authorization", "Bearer " + officerWard2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(issueBodakdev.getId().toString()))
                .andExpect(jsonPath("$.content[0].wardName").value("Bodakdev"));

        // Officer Roads Dept should see both issues since both belong to deptRoads
        mockMvc.perform(get("/api/authority/issues")
                        .header("Authorization", "Bearer " + officerDeptRoadsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Admin should see both issues
        mockMvc.perform(get("/api/authority/issues")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Should prevent IDOR: Officer cannot view or modify issue outside assigned scope")
    void testIdorScopeProtection() throws Exception {
        IssueEntity issueBodakdev = createTestIssue(wardBodakdev, deptWater, ResponsibilityStatus.RESOLVED);

        // Officer 1 (Navrangpura) tries to access Bodakdev issue directly by ID
        mockMvc.perform(get("/api/authority/issues/" + issueBodakdev.getId())
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isForbidden());

        // Officer 1 tries to update status of Bodakdev issue
        ChangeStatusRequest changeRequest = new ChangeStatusRequest(IssueStatus.VERIFIED, "Verifying issue", null);
        mockMvc.perform(post("/api/authority/issues/" + issueBodakdev.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isForbidden());

        // Officer 2 (Bodakdev) can view detail
        mockMvc.perform(get("/api/authority/issues/" + issueBodakdev.getId())
                        .header("Authorization", "Bearer " + officerWard2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(issueBodakdev.getId().toString()))
                .andExpect(jsonPath("$.civicResponsibility.ward.name").value("Bodakdev"));
    }

    @Test
    @DisplayName("Should reject operation on issue with UNRESOLVED responsibility status")
    void testUnresolvedResponsibilityCannotBeManaged() throws Exception {
        IssueEntity issueUnresolved = createTestIssue(wardNavrangpura, deptRoads, ResponsibilityStatus.UNRESOLVED);

        mockMvc.perform(get("/api/authority/issues/" + issueUnresolved.getId())
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should execute valid operational workflow: REPORTED -> VERIFIED -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED")
    void testCompleteOperationalWorkflow() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, deptRoads, ResponsibilityStatus.RESOLVED);
        assertEquals(IssueStatus.REPORTED, issue.getStatus());

        // 1. REPORTED -> VERIFIED
        ChangeStatusRequest step1 = new ChangeStatusRequest(IssueStatus.VERIFIED, "Inspection verified", issue.getVersion());
        mockMvc.perform(post("/api/authority/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // Refresh issue
        issue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueStatus.VERIFIED, issue.getStatus());

        // 2. VERIFIED -> ACKNOWLEDGED
        ChangeStatusRequest step2 = new ChangeStatusRequest(IssueStatus.ACKNOWLEDGED, "Assigned to work order #124", issue.getVersion());
        mockMvc.perform(post("/api/authority/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        issue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueStatus.ACKNOWLEDGED, issue.getStatus());

        // 3. ACKNOWLEDGED -> IN_PROGRESS
        ChangeStatusRequest step3 = new ChangeStatusRequest(IssueStatus.IN_PROGRESS, "Crew dispatched to site", issue.getVersion());
        mockMvc.perform(post("/api/authority/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        issue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueStatus.IN_PROGRESS, issue.getStatus());

        // 4. IN_PROGRESS -> RESOLVED (Reason mandatory)
        ChangeStatusRequest step4MissingReason = new ChangeStatusRequest(IssueStatus.RESOLVED, "", issue.getVersion());
        mockMvc.perform(post("/api/authority/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step4MissingReason)))
                .andExpect(status().isBadRequest());

        ChangeStatusRequest step4Valid = new ChangeStatusRequest(IssueStatus.RESOLVED, "Asphalt repaved and cured completely", issue.getVersion());
        mockMvc.perform(post("/api/authority/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step4Valid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        issue = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueStatus.RESOLVED, issue.getStatus());
    }

    @Test
    @DisplayName("Should prevent authorities from transitioning to CITIZEN_VERIFIED or NOT_FIXED")
    void testCannotMarkCitizenVerified() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, deptRoads, ResponsibilityStatus.RESOLVED);

        // Advance to RESOLVED
        issue.setStatus(IssueStatus.RESOLVED);
        issue = issueRepository.save(issue);

        ChangeStatusRequest citizenVerifyAttempt = new ChangeStatusRequest(IssueStatus.CITIZEN_VERIFIED, "Authority self verification", issue.getVersion());
        mockMvc.perform(post("/api/authority/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citizenVerifyAttempt)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should detect optimistic locking conflict (409 Conflict) when version is stale")
    void testOptimisticLockingConflict() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, deptRoads, ResponsibilityStatus.RESOLVED);
        Long currentVersion = issue.getVersion();
        Long staleVersion = currentVersion - 1L;

        ChangeStatusRequest staleRequest = new ChangeStatusRequest(IssueStatus.VERIFIED, "Stale update", staleVersion);
        mockMvc.perform(post("/api/authority/issues/" + issue.getId() + "/status")
                        .header("Authorization", "Bearer " + officerWard1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Privacy check: Authority issue details must NEVER leak citizen phone or private details")
    void testPrivacyProtection() throws Exception {
        IssueEntity issue = createTestIssue(wardNavrangpura, deptRoads, ResponsibilityStatus.RESOLVED);

        mockMvc.perform(get("/api/authority/issues/" + issue.getId())
                        .header("Authorization", "Bearer " + officerWard1Token))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("+919800000001"))))
                .andExpect(content().string(not(containsString("otp"))));
    }

    @AfterEach
    void tearDown() {
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
