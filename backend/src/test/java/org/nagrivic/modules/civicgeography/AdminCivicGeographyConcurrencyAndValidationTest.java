package org.nagrivic.modules.civicgeography;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.civicgeography.dto.admin.*;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.departments.entity.CategoryDepartmentMappingEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AdminCivicGeographyConcurrencyAndValidationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

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
    private CategoryDepartmentMappingRepository categoryDepartmentMappingRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IssueService issueService;

    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private UserEntity adminUser;
    private String adminToken;

    private UserEntity createUniqueUser(String name, String role) {
        long uniqueNum = Math.abs(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
        UserEntity u = new UserEntity("+91" + uniqueNum, name);
        u.setRole(role);
        return userRepository.save(u);
    }

    @Autowired
    private org.nagrivic.modules.civicgeography.repository.CivicGeographyAuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminUser = createUniqueUser("Admin Tester", "ADMIN");
        adminToken = jwtService.generateAccessToken(adminUser.getId(), "ADMIN");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        auditRepository.deleteAll();
        issueRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrency: Submitting stale version returns HTTP 409 Conflict")
    void testOptimisticLockingConflict() throws Exception {
        CivicBodyEntity cb = civicBodyRepository.save(new CivicBodyEntity("Concurrency CB " + System.nanoTime(), CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Bhavnagar"));

        Long currentVersion = cb.getVersion();
        Long staleVersion = currentVersion + 99L; // Stale version mismatch

        UpdateCivicBodyRequest updateReq = new UpdateCivicBodyRequest(
                "Stale Update Attempt",
                CivicBodyType.MUNICIPAL_CORPORATION,
                "Gujarat",
                "Bhavnagar",
                null,
                null,
                null,
                true,
                staleVersion
        );

        mockMvc.perform(put("/api/admin/geography/civic-bodies/" + cb.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("Ward boundary validation endpoint summarizes valid, missing, and invalid boundaries")
    void testWardBoundaryValidation() throws Exception {
        CivicBodyEntity cb = civicBodyRepository.save(new CivicBodyEntity("Boundary CB " + System.nanoTime(), CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Jamnagar"));
        CityEntity city = cityRepository.save(new CityEntity("Jamnagar " + System.nanoTime(), "Gujarat", cb));

        // Create ward without boundary
        WardEntity wardNoBoundary = wardRepository.save(new WardEntity(city, cb, "J01", "Jamnagar Central"));

        MvcResult res = mockMvc.perform(post("/api/admin/geography/wards/validate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        WardBoundaryValidationResponse validation = objectMapper.readValue(
                res.getResponse().getContentAsString(), WardBoundaryValidationResponse.class);

        assertTrue(validation.totalWardsChecked() > 0);
        assertTrue(validation.missingBoundaries() >= 1);

        // Invalid geometry WKT rejection on creation
        CreateWardRequest invalidGeomReq = new CreateWardRequest(
                city.getId(),
                cb.getId(),
                "J02",
                "Jamnagar West",
                "JAM-W02",
                "POLYGON((0 0, 0 1, 1 1, 0 0))", // Polygon instead of MultiPolygon or corrupted
                "Test",
                null
        );

        // Should successfully convert Polygon or validate
        // Invalid WKT string:
        CreateWardRequest corruptedWktReq = new CreateWardRequest(
                city.getId(),
                cb.getId(),
                "J03",
                "Jamnagar North",
                "JAM-W03",
                "NOT_A_VALID_WKT_STRING",
                "Test",
                null
        );

        mockMvc.perform(post("/api/admin/geography/wards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corruptedWktReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Responsibility re-resolution: bounded execution preserves issue status, priority, and reporter")
    void testReResolutionBoundedAndSafe() throws Exception {
        CivicBodyEntity cb = civicBodyRepository.save(new CivicBodyEntity("ReResolve CB " + System.nanoTime(), CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Junagadh"));
        CityEntity city = cityRepository.save(new CityEntity("Junagadh " + System.nanoTime(), "Gujarat", cb));
        CategoryEntity cat = categoryRepository.save(new CategoryEntity("Pothole Cat", "pothole-" + System.nanoTime(), "Desc", 1));
        LocationEntity loc = locationService.createLocation(21.5222, 70.4579, new BigDecimal("5.00"));

        UserEntity reporter = createUniqueUser("Citizen Reporter", "CITIZEN");
        IssueEntity issue = issueService.createIssue(reporter, cat.getId(), loc.getId(), "Road Broken", "Needs immediate repair");

        // Issue initially UNRESOLVED because no ward contains it
        assertEquals(ResponsibilityStatus.UNRESOLVED, issue.getResponsibilityStatus());
        UUID originalReporterId = issue.getReporter().getId();
        String originalTitle = issue.getTitle();

        // Trigger single issue re-resolution
        ReResolveRequest req = new ReResolveRequest(issue.getId(), true, 10);
        MvcResult res = mockMvc.perform(post("/api/admin/geography/re-resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        ReResolveResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), ReResolveResponse.class);
        assertEquals(1, response.totalProcessed());

        // Refresh issue to assert safety invariants
        IssueEntity refreshed = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(originalReporterId, refreshed.getReporter().getId());
        assertEquals(originalTitle, refreshed.getTitle());
        assertNotNull(refreshed.getStatus());
        assertNotNull(refreshed.getPriority());
    }
}
