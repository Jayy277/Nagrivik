package org.nagrivic.modules.civicgeography;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.dto.CivicAreaResponse;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.civicgeography.service.CivicGeographyService;
import org.nagrivic.modules.departments.entity.CategoryDepartmentMappingEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.entity.WardDepartmentMappingEntity;
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.departments.repository.WardDepartmentMappingRepository;
import org.nagrivic.modules.issues.dto.CreateIssueRequest;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.issues.service.IssueService;
import org.nagrivic.modules.locations.entity.LocationEntity;
import org.nagrivic.modules.locations.service.LocationService;
import org.nagrivic.modules.statushistory.repository.StatusHistoryRepository;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CivicGeographyTest {

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
    private CivicGeographyService civicGeographyService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private SupportRepository supportRepository;

    private CivicBodyEntity amcCivicBody;
    private CityEntity ahmedabadCity;
    private WardEntity navrangpuraWard;
    private DepartmentEntity roadsDept;
    private CategoryEntity potholeCategory;
    private UserEntity citizenUser;

    @BeforeEach
    void setUp() {
        // Create test civic body
        amcCivicBody = new CivicBodyEntity(
                "Ahmedabad Municipal Corporation",
                CivicBodyType.MUNICIPAL_CORPORATION,
                "Gujarat",
                "Ahmedabad"
        );
        amcCivicBody.setOfficialWebsite("https://ahmedabadcity.gov.in");
        amcCivicBody.setSource("AMC Portal Reference");
        amcCivicBody = civicBodyRepository.save(amcCivicBody);

        // Create test city
        ahmedabadCity = new CityEntity("Ahmedabad", "Gujarat", amcCivicBody);
        ahmedabadCity = cityRepository.save(ahmedabadCity);

        // Create test polygon covering lon: [72.55, 72.60], lat: [23.02, 23.06]
        MultiPolygon wardBoundary = createSampleMultiPolygon(72.55, 23.02, 72.60, 23.06);
        navrangpuraWard = new WardEntity(
                ahmedabadCity,
                amcCivicBody,
                "12",
                "Navrangpura",
                wardBoundary
        );
        navrangpuraWard.setWardCode("AMC-W12");
        navrangpuraWard.setSource("Pilot Test Ward Fixture");
        navrangpuraWard = wardRepository.save(navrangpuraWard);

        // Create test department
        roadsDept = new DepartmentEntity(
                amcCivicBody,
                "Roads & Buildings",
                "R&B",
                "Responsible for municipal roads and asphalt repair"
        );
        roadsDept = departmentRepository.save(roadsDept);

        // Create test category
        potholeCategory = categoryRepository.save(new CategoryEntity(
                "Roads / Potholes",
                "roads-potholes-" + System.nanoTime(),
                "Pothole issues",
                1
        ));

        // Create test user
        citizenUser = userRepository.save(new UserEntity("+919876543210", "Test Citizen"));
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
        LinearRing shell = gf.createLinearRing(coords);
        Polygon polygon = gf.createPolygon(shell);
        MultiPolygon multiPolygon = gf.createMultiPolygon(new Polygon[]{polygon});
        multiPolygon.setSRID(4326);
        return multiPolygon;
    }

    @Test
    @DisplayName("1. Database: Civic body entity creation and persistence")
    void shouldCreateCivicBody() {
        assertThat(amcCivicBody.getId()).isNotNull();
        assertThat(amcCivicBody.getName()).isEqualTo("Ahmedabad Municipal Corporation");
        assertThat(amcCivicBody.getType()).isEqualTo(CivicBodyType.MUNICIPAL_CORPORATION);
        assertThat(amcCivicBody.isActive()).isTrue();
    }

    @Test
    @DisplayName("2. Database: City entity relationship with civic body and unique constraint")
    void shouldCreateCityWithRelationship() {
        assertThat(ahmedabadCity.getId()).isNotNull();
        assertThat(ahmedabadCity.getCivicBody().getId()).isEqualTo(amcCivicBody.getId());

        // Unique constraint test: duplicate city in same state should fail
        CityEntity duplicate = new CityEntity("Ahmedabad", "Gujarat", amcCivicBody);
        assertThatThrownBy(() -> {
            cityRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("3. Database: Ward relationship with city and MultiPolygon geometry")
    void shouldCreateWardWithGeometry() {
        assertThat(navrangpuraWard.getId()).isNotNull();
        assertThat(navrangpuraWard.getCity().getId()).isEqualTo(ahmedabadCity.getId());
        assertThat(navrangpuraWard.getBoundaryGeometry()).isNotNull();
        assertThat(navrangpuraWard.getBoundaryGeometry().getSRID()).isEqualTo(4326);
        assertThat(navrangpuraWard.getBoundaryGeometry().getGeometryType()).isEqualTo("MultiPolygon");
    }

    @Test
    @DisplayName("4. Database: Department relationship with civic body and uniqueness")
    void shouldCreateDepartmentWithCivicBody() {
        assertThat(roadsDept.getId()).isNotNull();
        assertThat(roadsDept.getCivicBody().getId()).isEqualTo(amcCivicBody.getId());

        // Duplicate department name in same civic body should fail
        DepartmentEntity dupDept = new DepartmentEntity(amcCivicBody, "Roads & Buildings", "RB2", "Duplicate");
        assertThatThrownBy(() -> {
            departmentRepository.saveAndFlush(dupDept);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("5. Database: Category-Department unique mapping constraint")
    void shouldEnforceCategoryDepartmentUniqueness() {
        CategoryDepartmentMappingEntity mapping = new CategoryDepartmentMappingEntity(potholeCategory, roadsDept);
        categoryDepartmentMappingRepository.saveAndFlush(mapping);

        CategoryDepartmentMappingEntity duplicate = new CategoryDepartmentMappingEntity(potholeCategory, roadsDept);
        assertThatThrownBy(() -> {
            categoryDepartmentMappingRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("6. Database: Ward-Department unique mapping constraint")
    void shouldEnforceWardDepartmentUniqueness() {
        WardDepartmentMappingEntity mapping = new WardDepartmentMappingEntity(navrangpuraWard, roadsDept);
        wardDepartmentMappingRepository.saveAndFlush(mapping);

        WardDepartmentMappingEntity duplicate = new WardDepartmentMappingEntity(navrangpuraWard, roadsDept);
        assertThatThrownBy(() -> {
            wardDepartmentMappingRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("7. Spatial: Point inside ward resolves to correct ward, city, and civic body")
    void shouldResolvePointInsideWard() {
        // Inside navrangpuraWard: lon=72.57, lat=23.03
        Optional<CivicAreaResponse> responseOpt = civicGeographyService.resolvePoint(23.03, 72.57, null);

        assertThat(responseOpt).isPresent();
        CivicAreaResponse response = responseOpt.get();

        assertThat(response.city()).isEqualTo("Ahmedabad");
        assertThat(response.ward()).isNotNull();
        assertThat(response.ward().name()).isEqualTo("Navrangpura");
        assertThat(response.ward().number()).isEqualTo("12");
        assertThat(response.civicBody()).isNotNull();
        assertThat(response.civicBody().name()).isEqualTo("Ahmedabad Municipal Corporation");
        assertThat(response.department()).isNull(); // No mapping set yet
    }

    @Test
    @DisplayName("8. Spatial: Point outside ward boundaries returns unresolved (empty)")
    void shouldReturnEmptyForPointOutsideWard() {
        // Outside navrangpuraWard: lon=72.80, lat=23.30
        Optional<CivicAreaResponse> responseOpt = civicGeographyService.resolvePoint(23.30, 72.80, null);

        assertThat(responseOpt).isEmpty();
    }

    @Test
    @DisplayName("9. Spatial: Invalid coordinates throw validation exception")
    void shouldRejectInvalidCoordinates() {
        assertThatThrownBy(() -> civicGeographyService.resolvePoint(95.0, 72.57, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> civicGeographyService.resolvePoint(23.03, 190.0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("10. Spatial: Ward with null boundary geometry is safely skipped")
    void shouldHandleWardWithMissingBoundaryData() {
        WardEntity wardWithoutBoundary = new WardEntity(
                ahmedabadCity,
                amcCivicBody,
                "99",
                "Unmapped Ward",
                null
        );
        wardRepository.save(wardWithoutBoundary);

        Optional<CivicAreaResponse> responseOpt = civicGeographyService.resolvePoint(23.30, 72.80, null);
        assertThat(responseOpt).isEmpty();
    }

    @Test
    @DisplayName("11. Department Mapping: Resolves responsible department when mapped")
    void shouldResolveResponsibleDepartmentWhenMapped() {
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(potholeCategory, roadsDept));

        // Inside ward with pothole category
        Optional<CivicAreaResponse> responseOpt = civicGeographyService.resolvePoint(23.03, 72.57, potholeCategory.getId());

        assertThat(responseOpt).isPresent();
        CivicAreaResponse response = responseOpt.get();

        assertThat(response.department()).isNotNull();
        assertThat(response.department().name()).isEqualTo("Roads & Buildings");
    }

    @Test
    @DisplayName("12. Department Mapping: Ward-specific department mapping is prioritized")
    void shouldPrioritizeWardSpecificDepartmentMapping() {
        DepartmentEntity westZoneRoads = departmentRepository.save(new DepartmentEntity(
                amcCivicBody,
                "West Zone Roads",
                "WZ-ROADS",
                "West zone specific road crew"
        ));

        // Both departments mapped to pothole category
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(potholeCategory, roadsDept));
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(potholeCategory, westZoneRoads));

        // But only westZoneRoads is mapped to navrangpuraWard
        wardDepartmentMappingRepository.save(new WardDepartmentMappingEntity(navrangpuraWard, westZoneRoads));

        Optional<CivicAreaResponse> responseOpt = civicGeographyService.resolvePoint(23.03, 72.57, potholeCategory.getId());

        assertThat(responseOpt).isPresent();
        assertThat(responseOpt.get().department()).isNotNull();
        assertThat(responseOpt.get().department().name()).isEqualTo("West Zone Roads");
    }

    @Test
    @DisplayName("13. Department Mapping: Unmapped category returns null department without error")
    void shouldReturnNullDepartmentForUnmappedCategory() {
        CategoryEntity unmappedCat = categoryRepository.save(new CategoryEntity(
                "Unmapped Civic Issue",
                "unmapped-" + System.nanoTime(),
                "Description",
                9
        ));

        Optional<CivicAreaResponse> responseOpt = civicGeographyService.resolvePoint(23.03, 72.57, unmappedCat.getId());

        assertThat(responseOpt).isPresent();
        assertThat(responseOpt.get().ward().name()).isEqualTo("Navrangpura");
        assertThat(responseOpt.get().department()).isNull();
    }

    @Test
    @DisplayName("14. Issue Integration: Issue with mapped location exposes civicArea in IssueResponse")
    void shouldEnrichIssueWithCivicArea() {
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(potholeCategory, roadsDept));

        LocationEntity insideLoc = locationService.createLocation(23.03, 72.57, new BigDecimal("5.00"));
        IssueEntity issue = issueService.createIssue(
                citizenUser,
                potholeCategory.getId(),
                insideLoc.getId(),
                "Pothole near university crossroad",
                "Deep crater in the asphalt"
        );

        IssueResponse response = issueService.getIssueById(issue.getId());

        assertThat(response.civicArea()).isNotNull();
        assertThat(response.civicArea().city()).isEqualTo("Ahmedabad");
        assertThat(response.civicArea().ward().name()).isEqualTo("Navrangpura");
        assertThat(response.civicArea().civicBody().name()).isEqualTo("Ahmedabad Municipal Corporation");
        assertThat(response.civicArea().department().name()).isEqualTo("Roads & Buildings");
    }

    @Test
    @DisplayName("15. Issue Integration: Issue with unmapped location has null civicArea and creation is not blocked")
    void shouldNotBlockIssueCreationWhenLocationUnmapped() {
        LocationEntity outsideLoc = locationService.createLocation(23.85, 73.10, new BigDecimal("10.00"));
        IssueEntity issue = issueService.createIssue(
                citizenUser,
                potholeCategory.getId(),
                outsideLoc.getId(),
                "Pothole in rural area",
                "Rural district road pothole"
        );

        IssueResponse response = issueService.getIssueById(issue.getId());

        assertThat(response.id()).isEqualTo(issue.getId());
        assertThat(response.civicArea()).isNull();
    }

    @Test
    @DisplayName("16. Security: Client cannot provide wardId or departmentId in CreateIssueRequest")
    void verifyClientCannotOverrideAdministrativeData() {
        // CreateIssueRequest record only has: title, description, categoryId, locationId
        CreateIssueRequest request = new CreateIssueRequest(
                "Pothole",
                "Description",
                potholeCategory.getId(),
                locationService.createLocation(23.03, 72.57, new BigDecimal("5.00")).getId()
        );

        // Introspect record components: verify NO administrative fields exist on request
        java.lang.reflect.RecordComponent[] components = CreateIssueRequest.class.getRecordComponents();
        for (java.lang.reflect.RecordComponent comp : components) {
            assertThat(comp.getName()).isNotIn("wardId", "departmentId", "cityId", "civicBodyId", "authorityId");
        }
    }
}
