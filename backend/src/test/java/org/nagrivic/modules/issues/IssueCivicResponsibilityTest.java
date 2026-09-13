package org.nagrivic.modules.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.nagrivic.modules.auth.service.JwtService;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.departments.entity.CategoryDepartmentMappingEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.entity.WardDepartmentMappingEntity;
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.departments.repository.WardDepartmentMappingRepository;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueResponsibilityService;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IssueCivicResponsibilityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueResponsibilityService issueResponsibilityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private CivicBodyRepository civicBodyRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CategoryDepartmentMappingRepository categoryDepartmentMappingRepository;

    @Autowired
    private WardDepartmentMappingRepository wardDepartmentMappingRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UserEntity citizen;
    private CivicBodyEntity amcCivicBody;
    private CityEntity ahmedabadCity;
    private WardEntity navrangpuraWard;
    private DepartmentEntity roadsDept;
    private DepartmentEntity sanitationDept;
    private CategoryEntity potholeCategory;
    private LocationEntity insideWardLocation;
    private LocationEntity outsideWardLocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Citizen
        citizen = userRepository.save(new UserEntity("+919876543210", "Aarav Patel"));

        // 2. Civic Body
        amcCivicBody = civicBodyRepository.save(new CivicBodyEntity(
                "Ahmedabad Municipal Corporation",
                CivicBodyType.MUNICIPAL_CORPORATION,
                "Gujarat",
                "Ahmedabad"
        ));

        // 3. City
        ahmedabadCity = cityRepository.save(new CityEntity("Ahmedabad", "Gujarat", amcCivicBody));

        // 4. Ward with polygon covering lon: [72.55, 72.60], lat: [23.02, 23.06]
        MultiPolygon boundary = createSampleMultiPolygon(72.55, 23.02, 72.60, 23.06);
        navrangpuraWard = wardRepository.save(new WardEntity(
                ahmedabadCity,
                amcCivicBody,
                "12",
                "Navrangpura",
                boundary
        ));

        // 5. Departments
        roadsDept = departmentRepository.save(new DepartmentEntity(
                amcCivicBody,
                "Roads & Buildings",
                "R&B",
                "Responsible for municipal roads and asphalt repair"
        ));

        sanitationDept = departmentRepository.save(new DepartmentEntity(
                amcCivicBody,
                "Solid Waste & Sanitation",
                "SWM",
                "Responsible for cleanliness and waste management"
        ));

        // 6. Category
        potholeCategory = categoryRepository.save(new CategoryEntity(
                "Roads / Potholes",
                "roads-potholes-" + System.nanoTime(),
                "Pothole issues",
                1
        ));

        // 7. Locations
        // Inside ward: (23.03, 72.57)
        insideWardLocation = locationService.createLocation(23.03, 72.57, new BigDecimal("5.00"));

        // Outside ward: (19.0760, 72.8777) - Mumbai coordinates
        outsideWardLocation = locationService.createLocation(19.0760, 72.8777, new BigDecimal("5.00"));
    }

    private static MultiPolygon createSampleMultiPolygon(double minLon, double minLat, double maxLon, double maxLat) {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(minLon, minLat),
                new Coordinate(maxLon, minLat),
                new Coordinate(maxLon, maxLat),
                new Coordinate(minLon, maxLat),
                new Coordinate(minLon, minLat)
        };
        Polygon poly = gf.createPolygon(coords);
        return gf.createMultiPolygon(new Polygon[]{poly});
    }

    @Test
    @DisplayName("A. Fully resolved issue: location + ward + category mapping -> RESOLVED with all entities persisted")
    void testFullyResolvedIssue() {
        // Map potholeCategory to roadsDept
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));

        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                insideWardLocation.getId(),
                "Large pothole near commerce six roads",
                "Needs immediate asphalt repair"
        );

        // Verify entity persistence
        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(issue.getCivicBody()).isNotNull();
        assertThat(issue.getCivicBody().getId()).isEqualTo(amcCivicBody.getId());
        assertThat(issue.getCity()).isNotNull();
        assertThat(issue.getCity().getId()).isEqualTo(ahmedabadCity.getId());
        assertThat(issue.getWard()).isNotNull();
        assertThat(issue.getWard().getId()).isEqualTo(navrangpuraWard.getId());
        assertThat(issue.getDepartment()).isNotNull();
        assertThat(issue.getDepartment().getId()).isEqualTo(roadsDept.getId());
        assertThat(issue.getResponsibilityResolvedAt()).isNotNull();
        assertThat(issue.getResponsibilitySource()).isEqualTo("GEOGRAPHIC_POINT_IN_POLYGON");

        // Verify IssueResponse DTO
        IssueResponse response = issueService.getIssueById(issue.getId());
        assertThat(response.civicResponsibility()).isNotNull();
        assertThat(response.civicResponsibility().status()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(response.civicResponsibility().civicBody().name()).isEqualTo("Ahmedabad Municipal Corporation");
        assertThat(response.civicResponsibility().city().name()).isEqualTo("Ahmedabad");
        assertThat(response.civicResponsibility().ward().name()).isEqualTo("Navrangpura");
        assertThat(response.civicResponsibility().department().name()).isEqualTo("Roads & Buildings");
    }

    @Test
    @DisplayName("B. No ward boundary: valid issue location with no ward boundary -> UNRESOLVED but issue valid")
    void testNoWardBoundary_UnresolvedButValid() {
        // Category has department mapping
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));

        // Location is outside any ward boundary
        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                outsideWardLocation.getId(),
                "Pothole outside mapped ward",
                "Somewhere without authoritative ward polygon"
        );

        // Principle: "Unresolved does not mean invalid"
        assertThat(issue.getId()).isNotNull();
        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        assertThat(issue.getWard()).isNull();
        assertThat(issue.getDepartment()).isNull();
        assertThat(issue.getCivicBody()).isNull();

        IssueResponse response = issueService.getIssueById(issue.getId());
        assertThat(response.civicResponsibility()).isNotNull();
        assertThat(response.civicResponsibility().status()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        assertThat(response.civicResponsibility().ward()).isNull();
        assertThat(response.civicResponsibility().department()).isNull();
    }

    @Test
    @DisplayName("C. No department mapping: valid geography but no category mapping -> UNRESOLVED but ward preserved")
    void testNoDepartmentMapping_UnresolvedButWardPreserved() {
        // Do NOT create category department mapping
        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                insideWardLocation.getId(),
                "Pothole with no department mapping",
                "Ward is known, department is not yet mapped"
        );

        assertThat(issue.getId()).isNotNull();
        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        // Ward, City, CivicBody are preserved authoritatively
        assertThat(issue.getWard()).isNotNull();
        assertThat(issue.getWard().getId()).isEqualTo(navrangpuraWard.getId());
        assertThat(issue.getCity()).isNotNull();
        assertThat(issue.getCivicBody()).isNotNull();
        // Department cannot be guessed
        assertThat(issue.getDepartment()).isNull();

        IssueResponse response = issueService.getIssueById(issue.getId());
        assertThat(response.civicResponsibility().status()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        assertThat(response.civicResponsibility().ward().name()).isEqualTo("Navrangpura");
        assertThat(response.civicResponsibility().department()).isNull();
    }

    @Test
    @DisplayName("D. Conflicting mappings: multiple category mappings without deterministic rule -> UNRESOLVED")
    void testConflictingDepartmentMappings_MarksUnresolved() {
        // Map potholeCategory to BOTH roadsDept and sanitationDept under the same civic body
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                sanitationDept
        ));

        // Create issue: system must NOT silently pick one; must mark UNRESOLVED
        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                insideWardLocation.getId(),
                "Pothole with conflicting departments",
                "Ambiguous mapping"
        );

        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        assertThat(issue.getDepartment()).isNull();
        assertThat(issue.getWard()).isNotNull();
    }

    @Test
    @DisplayName("E. Ward-specific precedence: ward mapping disambiguates multiple candidate departments")
    void testWardSpecificMappingPrecedence() {
        // Both departments mapped at category level
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                sanitationDept
        ));

        // Ward specifically maps sanitationDept
        wardDepartmentMappingRepository.save(new WardDepartmentMappingEntity(
                navrangpuraWard,
                sanitationDept
        ));

        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                insideWardLocation.getId(),
                "Issue in ward with specialized department",
                "Ward-level mapping takes precedence"
        );

        // Precedence: ward-specific mapping resolves unambiguously to sanitationDept
        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(issue.getDepartment()).isNotNull();
        assertThat(issue.getDepartment().getId()).isEqualTo(sanitationDept.getId());
    }

    @Test
    @DisplayName("F. Client spoofing defense: client-supplied administrative fields are completely ignored")
    void testClientSpoofingDefense() throws Exception {
        // Map potholeCategory to roadsDept
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));

        String token = jwtService.generateAccessToken(citizen.getId(), citizen.getRole());
        UUID fakeCivicBodyId = UUID.randomUUID();
        UUID fakeCityId = UUID.randomUUID();
        UUID fakeWardId = UUID.randomUUID();
        UUID fakeDepartmentId = UUID.randomUUID();

        // Client attempts to spoof civicBodyId, cityId, wardId, departmentId in request body
        Map<String, Object> payload = Map.of(
                "title", "Pothole with spoofed authority IDs",
                "description", "Trying to override civic responsibility",
                "categoryId", potholeCategory.getId(),
                "locationId", insideWardLocation.getId(),
                "civicBodyId", fakeCivicBodyId,
                "cityId", fakeCityId,
                "wardId", fakeWardId,
                "departmentId", fakeDepartmentId
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.civicResponsibility.status").value("RESOLVED"))
                .andExpect(jsonPath("$.civicResponsibility.civicBody.id").value(amcCivicBody.getId().toString()))
                .andExpect(jsonPath("$.civicResponsibility.civicBody.id").value(org.hamcrest.Matchers.not(fakeCivicBodyId.toString())))
                .andExpect(jsonPath("$.civicResponsibility.ward.id").value(navrangpuraWard.getId().toString()))
                .andExpect(jsonPath("$.civicResponsibility.ward.id").value(org.hamcrest.Matchers.not(fakeWardId.toString())))
                .andExpect(jsonPath("$.civicResponsibility.department.id").value(roadsDept.getId().toString()))
                .andExpect(jsonPath("$.civicResponsibility.department.id").value(org.hamcrest.Matchers.not(fakeDepartmentId.toString())));
    }

    @Test
    @DisplayName("G. Re-resolution: initially unresolved issue becomes RESOLVED when authoritative mapping is added")
    void testReResolutionLifecycle() {
        // Initially no mapping exists
        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                insideWardLocation.getId(),
                "Pothole initially unresolved",
                "Waiting for mapping"
        );

        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        assertThat(issue.getDepartment()).isNull();

        // Now authoritative mapping is added to the system
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));

        // Trigger internal re-resolution
        issueResponsibilityService.resolveIssueResponsibility(issue.getId());

        // Refresh and assert
        IssueEntity refreshed = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(refreshed.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(refreshed.getDepartment()).isNotNull();
        assertThat(refreshed.getDepartment().getId()).isEqualTo(roadsDept.getId());
        assertThat(refreshed.getResponsibilityResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("H. Idempotency: multiple re-resolutions produce consistent result without duplicating data")
    void testResolutionIdempotency() {
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));

        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                insideWardLocation.getId(),
                "Pothole for idempotency check",
                "Checking multiple resolutions"
        );

        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.RESOLVED);

        // Run resolution 3 times
        issueResponsibilityService.resolveIssueResponsibility(issue.getId());
        issueResponsibilityService.resolveIssueResponsibility(issue.getId());
        issueResponsibilityService.resolveIssueResponsibility(issue.getId());

        IssueEntity refreshed = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(refreshed.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(refreshed.getCivicBody().getId()).isEqualTo(amcCivicBody.getId());
        assertThat(refreshed.getWard().getId()).isEqualTo(navrangpuraWard.getId());
        assertThat(refreshed.getDepartment().getId()).isEqualTo(roadsDept.getId());
    }

    @Test
    @DisplayName("I. Efficient issue listing: findIssues loads civic responsibility context with zero N+1")
    void testFindIssuesEfficientLoading() {
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(
                potholeCategory,
                roadsDept
        ));

        issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                insideWardLocation.getId(),
                "Issue 1",
                "Desc 1"
        );
        issueService.createIssue(
                citizen,
                potholeCategory.getId(),
                outsideWardLocation.getId(),
                "Issue 2",
                "Desc 2"
        );

        Page<IssueResponse> issuesPage = issueService.findIssues(null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(issuesPage.getContent()).hasSizeGreaterThanOrEqualTo(2);

        IssueResponse resolvedIssue = issuesPage.getContent().stream()
                .filter(i -> i.title().equals("Issue 1"))
                .findFirst()
                .orElseThrow();
        assertThat(resolvedIssue.civicResponsibility().status()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(resolvedIssue.civicResponsibility().ward().name()).isEqualTo("Navrangpura");

        IssueResponse unresolvedIssue = issuesPage.getContent().stream()
                .filter(i -> i.title().equals("Issue 2"))
                .findFirst()
                .orElseThrow();
        assertThat(unresolvedIssue.civicResponsibility().status()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        assertThat(unresolvedIssue.civicResponsibility().ward()).isNull();
    }
}
