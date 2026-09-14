package org.nagrivic.modules.civicgeography;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.dto.admin.*;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.CivicGeographyAuditRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.departments.repository.WardDepartmentMappingRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
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
class AdminCivicGeographyManagementTest {

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
    private WardDepartmentMappingRepository wardDepartmentMappingRepository;

    @Autowired
    private CivicGeographyAuditRepository auditRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private org.nagrivic.modules.issues.repository.IssueRepository issueRepository;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminUser = createUniqueUser("Admin Dev", "ADMIN");
        adminToken = jwtService.generateAccessToken(adminUser.getId(), "ADMIN");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        auditRepository.deleteAll();
        issueRepository.deleteAll();
    }

    @Test
    @DisplayName("Civic Body lifecycle: create, update, duplicate rejection, toggle active, audit trail")
    void testCivicBodyLifecycle() throws Exception {
        String uniqueName = "Surat Municipal Corporation " + System.nanoTime();
        CreateCivicBodyRequest createReq = new CreateCivicBodyRequest(
                uniqueName,
                CivicBodyType.MUNICIPAL_CORPORATION,
                "Gujarat",
                "Surat",
                "https://www.suratmunicipal.gov.in",
                "Official Portal",
                "https://www.suratmunicipal.gov.in/about"
        );

        MvcResult result = mockMvc.perform(post("/api/admin/geography/civic-bodies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(uniqueName))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String resJson = result.getResponse().getContentAsString();
        UUID createdId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(resJson, "$.id"));
        Long createdVersion = ((Number) com.jayway.jsonpath.JsonPath.read(resJson, "$.version")).longValue();
        assertNotNull(createdId);

        // Duplicate name rejection
        mockMvc.perform(post("/api/admin/geography/civic-bodies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict());

        // Update metadata
        UpdateCivicBodyRequest updateReq = new UpdateCivicBodyRequest(
                uniqueName + " Updated",
                CivicBodyType.MUNICIPAL_CORPORATION,
                "Gujarat",
                "Surat",
                "https://new.surat.gov.in",
                "Updated Source",
                "https://new.surat.gov.in",
                true,
                createdVersion
        );

        mockMvc.perform(put("/api/admin/geography/civic-bodies/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(uniqueName + " Updated"));

        // Toggle active
        mockMvc.perform(patch("/api/admin/geography/civic-bodies/" + createdId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false")
                        .param("reason", "Administrative reorganization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // Verify audit created
        assertTrue(auditRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("CIVIC_BODY", createdId, null).getTotalElements() >= 3);
    }

    @Test
    @DisplayName("Referenced Civic Body cannot be deleted when referenced by cities or issues")
    void testCivicBodyDeletionProtection() throws Exception {
        CivicBodyEntity cb = civicBodyRepository.save(new CivicBodyEntity("Protected Body " + System.nanoTime(), CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Vadodara"));
        CityEntity city = cityRepository.save(new CityEntity("Vadodara " + System.nanoTime(), "Gujarat", cb));

        // Trying to delete civic body should fail with 409 Conflict because of referencing city
        mockMvc.perform(delete("/api/admin/geography/civic-bodies/" + cb.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // City referenced by issues cannot be deleted
        CategoryEntity cat = categoryRepository.save(new CategoryEntity("City Cat", "city-cat-" + System.nanoTime(), "Desc", 1));
        LocationEntity loc = locationService.createLocation(22.3072, 73.1812, new BigDecimal("5.00"));
        IssueEntity issue = issueService.createIssue(adminUser, cat.getId(), loc.getId(), "City Protected Issue", "Desc");
        issue.setResolvedResponsibility(cb, city, null, null, "TEST");
        issueRepository.save(issue);

        mockMvc.perform(delete("/api/admin/geography/cities/" + city.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Department lifecycle: create, update, duplicate prevention, toggle active, historical issue deletion protection")
    void testDepartmentLifecycleAndProtection() throws Exception {
        CivicBodyEntity cb = civicBodyRepository.save(new CivicBodyEntity("Dept Test CB " + System.nanoTime(), CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Rajkot"));

        String deptName = "Rajkot Health Dept " + System.nanoTime();
        CreateDepartmentRequest createReq = new CreateDepartmentRequest(
                cb.getId(),
                deptName,
                "RMC-HLTH-" + System.currentTimeMillis() % 10000,
                "Health and sanitation services",
                "Official RMC",
                "https://rmc.gov.in"
        );

        MvcResult res = mockMvc.perform(post("/api/admin/geography/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(deptName))
                .andReturn();

        String deptJson = res.getResponse().getContentAsString();
        UUID deptId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(deptJson, "$.id"));

        // Duplicate name in same civic body is rejected
        mockMvc.perform(post("/api/admin/geography/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict());

        // Toggle active
        mockMvc.perform(patch("/api/admin/geography/departments/" + deptId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false")
                        .param("reason", "Merged with another department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // If department is referenced by an issue, deletion must return 409 Conflict
        CityEntity city = cityRepository.save(new CityEntity("Rajkot " + System.nanoTime(), "Gujarat", cb));
        CategoryEntity cat = categoryRepository.save(new CategoryEntity("Dept Cat", "dept-cat-" + System.nanoTime(), "Desc", 1));
        LocationEntity loc = locationService.createLocation(22.3039, 70.8022, new BigDecimal("5.00"));
        IssueEntity issue = issueService.createIssue(adminUser, cat.getId(), loc.getId(), "Dept Issue", "Desc");
        DepartmentEntity deptEntity = departmentRepository.findById(deptId).orElseThrow();
        issue.setResolvedResponsibility(cb, city, null, deptEntity, "TEST");
        issueRepository.save(issue);

        mockMvc.perform(delete("/api/admin/geography/departments/" + deptId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Category and Ward Mapping lifecycle: create, duplicate prevention, toggle active")
    void testMappingLifecycle() throws Exception {
        CivicBodyEntity cb = civicBodyRepository.save(new CivicBodyEntity("Map Test CB " + System.nanoTime(), CivicBodyType.MUNICIPAL_CORPORATION, "Gujarat", "Gandhinagar"));
        CityEntity city = cityRepository.save(new CityEntity("Gandhinagar " + System.nanoTime(), "Gujarat", cb));
        WardEntity ward = wardRepository.save(new WardEntity(city, cb, "W101", "Gandhinagar Sector 1"));
        DepartmentEntity dept = departmentRepository.save(new DepartmentEntity(cb, "Gandhinagar Sanitation " + System.nanoTime(), "GMC-SAN-" + System.nanoTime(), "Sanitation"));
        CategoryEntity category = categoryRepository.save(new CategoryEntity("Garbage Sector 1", "garb-sec1-" + System.nanoTime(), "Desc", 1));

        // 1. Create Category Mapping
        CreateCategoryDepartmentMappingRequest catMapReq = new CreateCategoryDepartmentMappingRequest(
                category.getId(),
                dept.getId(),
                "Authoritative Circular",
                "https://gmc.gov.in/rules"
        );

        MvcResult catMapRes = mockMvc.perform(post("/api/admin/geography/category-mappings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catMapReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String catMapJson = catMapRes.getResponse().getContentAsString();
        UUID catMapId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(catMapJson, "$.id"));

        // Duplicate category mapping rejected
        mockMvc.perform(post("/api/admin/geography/category-mappings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catMapReq)))
                .andExpect(status().isConflict());

        // Toggle category mapping active
        mockMvc.perform(patch("/api/admin/geography/category-mappings/" + catMapId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // 2. Create Ward Mapping
        CreateWardDepartmentMappingRequest wardMapReq = new CreateWardDepartmentMappingRequest(
                ward.getId(),
                dept.getId(),
                "Ward Circular",
                "https://gmc.gov.in/wards"
        );

        MvcResult wardMapRes = mockMvc.perform(post("/api/admin/geography/ward-mappings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wardMapReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String wardMapJson = wardMapRes.getResponse().getContentAsString();
        UUID wardMapId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(wardMapJson, "$.id"));

        // Duplicate ward mapping rejected
        mockMvc.perform(post("/api/admin/geography/ward-mappings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wardMapReq)))
                .andExpect(status().isConflict());

        // Toggle ward mapping active
        mockMvc.perform(patch("/api/admin/geography/ward-mappings/" + wardMapId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }
}
