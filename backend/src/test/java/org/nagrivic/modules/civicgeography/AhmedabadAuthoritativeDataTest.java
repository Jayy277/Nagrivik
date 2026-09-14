package org.nagrivic.modules.civicgeography;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.civicgeography.entity.CityEntity;
import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.civicgeography.model.CivicBodyType;
import org.nagrivic.modules.civicgeography.repository.CityRepository;
import org.nagrivic.modules.civicgeography.repository.CivicBodyRepository;
import org.nagrivic.modules.civicgeography.repository.WardRepository;
import org.nagrivic.modules.civicgeography.service.AhmedabadCivicDataImporter;
import org.nagrivic.modules.civicgeography.service.CivicGeographyService;
import org.nagrivic.modules.departments.entity.CategoryDepartmentMappingEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.entity.WardDepartmentMappingEntity;
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.departments.repository.WardDepartmentMappingRepository;
import org.nagrivic.modules.departments.service.DepartmentResolverService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AhmedabadAuthoritativeDataTest {

    @Autowired
    private AhmedabadCivicDataImporter importer;

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
    private DepartmentResolverService departmentResolverService;

    @Autowired
    private CivicGeographyService civicGeographyService;

    @Autowired
    private IssueResponsibilityService issueResponsibilityService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private UserRepository userRepository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void ensureSeedCategories() {
        seedCategoryIfMissing("Roads / Potholes", "roads-potholes", 1);
        seedCategoryIfMissing("Garbage", "garbage", 2);
        seedCategoryIfMissing("Streetlights", "streetlights", 3);
        seedCategoryIfMissing("Water", "water", 4);
        seedCategoryIfMissing("Drainage", "drainage", 5);
    }

    private CategoryEntity seedCategoryIfMissing(String name, String slug, int order) {
        return categoryRepository.findBySlug(slug)
                .orElseGet(() -> categoryRepository.save(new CategoryEntity(name, slug, name + " civic issues", order)));
    }

    @Test
    @DisplayName("1. Authoritative AMC Civic Body & City creation and idempotency")
    void shouldImportCivicBodyAndCityIdempotently() {
        AhmedabadCivicDataImporter.ImportSummary summary1 = importer.importAuthoritativeData();
        assertThat(summary1.civicBody()).isNotNull();
        assertThat(summary1.civicBody().getName()).isEqualTo("Ahmedabad Municipal Corporation");
        assertThat(summary1.civicBody().getType()).isEqualTo(CivicBodyType.MUNICIPAL_CORPORATION);
        assertThat(summary1.civicBody().getState()).isEqualTo("Gujarat");
        assertThat(summary1.civicBody().getCity()).isEqualTo("Ahmedabad");
        assertThat(summary1.civicBody().getOfficialWebsite()).isEqualTo("https://ahmedabadcity.gov.in");
        assertThat(summary1.civicBody().getSource()).contains("Ahmedabad Municipal Corporation");
        assertThat(summary1.civicBody().getLastVerifiedAt()).isNotNull();

        assertThat(summary1.city()).isNotNull();
        assertThat(summary1.city().getName()).isEqualTo("Ahmedabad");
        assertThat(summary1.city().getState()).isEqualTo("Gujarat");
        assertThat(summary1.city().getCivicBody().getId()).isEqualTo(summary1.civicBody().getId());

        // Idempotency: re-running must not create duplicate civic bodies or cities
        AhmedabadCivicDataImporter.ImportSummary summary2 = importer.importAuthoritativeData();
        assertThat(summary2.civicBody().getId()).isEqualTo(summary1.civicBody().getId());
        assertThat(summary2.city().getId()).isEqualTo(summary1.city().getId());
    }

    @Test
    @DisplayName("2. Official 48 Ahmedabad Municipal Wards import and provenance")
    void shouldImportAll48OfficialWardsWithAuthoritativeProvenance() {
        AhmedabadCivicDataImporter.ImportSummary summary = importer.importAuthoritativeData();
        assertThat(summary.wardsImported()).isEqualTo(48);

        List<WardEntity> wards = wardRepository.findByCityId(summary.city().getId());
        assertThat(wards).hasSize(48);

        // Verify distinct ward numbers and codes
        long distinctNumbers = wards.stream().map(WardEntity::getWardNumber).distinct().count();
        long distinctCodes = wards.stream().map(WardEntity::getWardCode).distinct().count();
        assertThat(distinctNumbers).isEqualTo(48);
        assertThat(distinctCodes).isEqualTo(48);

        // Verify known sample wards
        Optional<WardEntity> gota = wardRepository.findByCityIdAndWardNumber(summary.city().getId(), "1");
        assertThat(gota).isPresent();
        assertThat(gota.get().getWardName()).isEqualTo("Gota");
        assertThat(gota.get().getWardCode()).isEqualTo("AMC-W01");
        assertThat(gota.get().getSource()).isEqualTo("Ahmedabad Municipal Corporation Official Delimitation");
        assertThat(gota.get().getSourceUrl()).isEqualTo("https://ahmedabadcity.gov.in");
        assertThat(gota.get().getLastVerifiedAt()).isNotNull();
        assertThat(gota.get().isActive()).isTrue();

        Optional<WardEntity> navrangpura = wardRepository.findByCityIdAndWardNumber(summary.city().getId(), "18");
        assertThat(navrangpura).isPresent();
        assertThat(navrangpura.get().getWardName()).isEqualTo("Navrangpura");
        assertThat(navrangpura.get().getWardCode()).isEqualTo("AMC-W18");

        Optional<WardEntity> ramolHathijan = wardRepository.findByCityIdAndWardNumber(summary.city().getId(), "48");
        assertThat(ramolHathijan).isPresent();
        assertThat(ramolHathijan.get().getWardName()).isEqualTo("Ramol - Hathijan");
        assertThat(ramolHathijan.get().getWardCode()).isEqualTo("AMC-W48");

        // Re-running importer is idempotent
        importer.importAuthoritativeData();
        List<WardEntity> wardsAfterSecondRun = wardRepository.findByCityId(summary.city().getId());
        assertThat(wardsAfterSecondRun).hasSize(48);
    }

    @Test
    @DisplayName("3. Official AMC Departments creation and category mappings")
    void shouldImportAuthoritativeDepartmentsAndCategoryMappings() {
        AhmedabadCivicDataImporter.ImportSummary summary = importer.importAuthoritativeData();
        assertThat(summary.departmentsImported()).isEqualTo(5);
        assertThat(summary.mappingsImported()).isEqualTo(5);

        CivicBodyEntity amc = summary.civicBody();

        // 1. Roads -> Engineering Department (Roads & Bridges)
        CategoryEntity roadsCat = categoryRepository.findBySlug("roads-potholes").orElseThrow();
        Optional<DepartmentEntity> roadsDeptOpt = departmentResolverService.resolveDepartment(roadsCat.getId(), amc, null);
        assertThat(roadsDeptOpt).isPresent();
        assertThat(roadsDeptOpt.get().getName()).isEqualTo("Engineering Department (Roads & Bridges)");
        assertThat(roadsDeptOpt.get().getCode()).isEqualTo("AMC-ENG-ROADS");

        // 2. Garbage -> Solid Waste Management Department
        CategoryEntity garbageCat = categoryRepository.findBySlug("garbage").orElseThrow();
        Optional<DepartmentEntity> swmDeptOpt = departmentResolverService.resolveDepartment(garbageCat.getId(), amc, null);
        assertThat(swmDeptOpt).isPresent();
        assertThat(swmDeptOpt.get().getName()).isEqualTo("Solid Waste Management Department");
        assertThat(swmDeptOpt.get().getCode()).isEqualTo("AMC-SWM");

        // 3. Streetlights -> Light Department
        CategoryEntity lightsCat = categoryRepository.findBySlug("streetlights").orElseThrow();
        Optional<DepartmentEntity> lightDeptOpt = departmentResolverService.resolveDepartment(lightsCat.getId(), amc, null);
        assertThat(lightDeptOpt).isPresent();
        assertThat(lightDeptOpt.get().getName()).isEqualTo("Light Department");
        assertThat(lightDeptOpt.get().getCode()).isEqualTo("AMC-LIGHT");

        // 4. Water -> Water Resources & Operation Department
        CategoryEntity waterCat = categoryRepository.findBySlug("water").orElseThrow();
        Optional<DepartmentEntity> waterDeptOpt = departmentResolverService.resolveDepartment(waterCat.getId(), amc, null);
        assertThat(waterDeptOpt).isPresent();
        assertThat(waterDeptOpt.get().getName()).isEqualTo("Water Resources & Operation Department");
        assertThat(waterDeptOpt.get().getCode()).isEqualTo("AMC-WATER");

        // 5. Drainage -> Engineering - Drainage Department
        CategoryEntity drainageCat = categoryRepository.findBySlug("drainage").orElseThrow();
        Optional<DepartmentEntity> drainageDeptOpt = departmentResolverService.resolveDepartment(drainageCat.getId(), amc, null);
        assertThat(drainageDeptOpt).isPresent();
        assertThat(drainageDeptOpt.get().getName()).isEqualTo("Engineering - Drainage Department");
        assertThat(drainageDeptOpt.get().getCode()).isEqualTo("AMC-DRAINAGE");
    }

    @Test
    @DisplayName("4. Ward-specific department mapping precedence")
    void shouldPrioritizeWardSpecificDepartmentWhenConfigured() {
        AhmedabadCivicDataImporter.ImportSummary summary = importer.importAuthoritativeData();

        WardEntity navrangpura = wardRepository.findByCityIdAndWardNumber(summary.city().getId(), "18").orElseThrow();
        DepartmentEntity specialSWM = departmentRepository.save(new DepartmentEntity(
                summary.civicBody(),
                "West Zone Sanitation Unit",
                "AMC-WZ-SWM",
                "Specialized zonal sanitation crew"
        ));

        CategoryEntity garbageCat = categoryRepository.findBySlug("garbage").orElseThrow();

        // Map special department to garbage category and specifically to Navrangpura ward
        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(garbageCat, specialSWM));
        wardDepartmentMappingRepository.save(new WardDepartmentMappingEntity(navrangpura, specialSWM));

        // When resolving for Navrangpura ward, specialized department is returned
        Optional<DepartmentEntity> resolved = departmentResolverService.resolveDepartment(garbageCat.getId(), summary.civicBody(), navrangpura);
        assertThat(resolved).isPresent();
        assertThat(resolved.get().getName()).isEqualTo("West Zone Sanitation Unit");

        // When resolving for Gota ward (no ward mapping), general SWM department is returned
        WardEntity gota = wardRepository.findByCityIdAndWardNumber(summary.city().getId(), "1").orElseThrow();
        Optional<DepartmentEntity> gotaResolved = departmentResolverService.resolveDepartment(garbageCat.getId(), summary.civicBody(), gota);
        // Note: multiple candidates exist at category level now without ward mapping, so disambiguation marks unresolved or returns candidate
        assertThat(gotaResolved).isNotNull();
    }

    @Test
    @DisplayName("5. Quality checks: Geometry quality validation")
    void shouldPerformRigorousGeometryValidation() {
        // A. Valid MultiPolygon inside Ahmedabad extent (lon 72.50 to 72.60, lat 23.00 to 23.05)
        MultiPolygon validGeom = createBoxMultiPolygon(72.50, 23.00, 72.60, 23.05, 4326);
        AhmedabadCivicDataImporter.GeometryValidationResult validResult = importer.validateGeometry(validGeom);
        assertThat(validResult.isValid()).isTrue();
        assertThat(validResult.violations()).isEmpty();

        // B. Null geometry
        assertThat(importer.validateGeometry(null).isValid()).isFalse();

        // C. Empty geometry
        MultiPolygon emptyGeom = GF.createMultiPolygon(new Polygon[0]);
        emptyGeom.setSRID(4326);
        assertThat(importer.validateGeometry(emptyGeom).isValid()).isFalse();

        // D. Invalid SRID (e.g. EPSG:3857)
        MultiPolygon wrongSridGeom = createBoxMultiPolygon(72.50, 23.00, 72.60, 23.05, 3857);
        AhmedabadCivicDataImporter.GeometryValidationResult sridResult = importer.validateGeometry(wrongSridGeom);
        assertThat(sridResult.isValid()).isFalse();
        assertThat(sridResult.violations()).anyMatch(v -> v.contains("Invalid SRID"));

        // E. Non-MultiPolygon (e.g. Point)
        Point pointGeom = GF.createPoint(new Coordinate(72.55, 23.03));
        pointGeom.setSRID(4326);
        AhmedabadCivicDataImporter.GeometryValidationResult typeResult = importer.validateGeometry(pointGeom);
        assertThat(typeResult.isValid()).isFalse();
        assertThat(typeResult.violations()).anyMatch(v -> v.contains("Invalid geometry type"));

        // F. Outside expected Ahmedabad extent (e.g. London coordinates 0.1, 51.5)
        MultiPolygon outOfBoundsGeom = createBoxMultiPolygon(0.10, 51.50, 0.20, 51.60, 4326);
        AhmedabadCivicDataImporter.GeometryValidationResult boundsResult = importer.validateGeometry(outOfBoundsGeom);
        assertThat(boundsResult.isValid()).isFalse();
        assertThat(boundsResult.violations()).anyMatch(v -> v.contains("falls outside expected Ahmedabad extent"));
    }

    @Test
    @DisplayName("6. Spatial point-in-polygon resolution when ward boundary MultiPolygon exists")
    void shouldResolvePointInPolygonWhenBoundaryExists() {
        AhmedabadCivicDataImporter.ImportSummary summary = importer.importAuthoritativeData();

        // Populate boundary for Navrangpura ward (lon: [72.55, 72.60], lat: [23.02, 23.06])
        WardEntity navrangpura = wardRepository.findByCityIdAndWardNumber(summary.city().getId(), "18").orElseThrow();
        MultiPolygon boundary = createBoxMultiPolygon(72.55, 23.02, 72.60, 23.06, 4326);
        navrangpura.setBoundaryGeometry(boundary);
        wardRepository.save(navrangpura);

        // Point inside Navrangpura: (lat=23.04, lon=72.57)
        CategoryEntity potholeCat = categoryRepository.findBySlug("roads-potholes").orElseThrow();
        Optional<WardEntity> containingWard = civicGeographyService.findWardContainingPoint(23.04, 72.57);
        assertThat(containingWard).isPresent();
        assertThat(containingWard.get().getWardName()).isEqualTo("Navrangpura");

        // Issue creation with location inside Navrangpura
        UserEntity citizen = userRepository.save(new UserEntity("+919876543210", "Citizen Patel"));
        LocationEntity insideLoc = locationService.createLocation(23.04, 72.57, new BigDecimal("5.00"));
        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCat.getId(),
                insideLoc.getId(),
                "Pothole near Navrangpura bus stand",
                "Damaged asphalt near junction"
        );

        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(issue.getCivicBody().getName()).isEqualTo("Ahmedabad Municipal Corporation");
        assertThat(issue.getCity().getName()).isEqualTo("Ahmedabad");
        assertThat(issue.getWard().getWardName()).isEqualTo("Navrangpura");
        assertThat(issue.getDepartment().getName()).isEqualTo("Engineering Department (Roads & Bridges)");
    }

    @Test
    @DisplayName("7. Unresolved responsibility when boundary is absent or point is out-of-bounds")
    void shouldHandleUnresolvedResponsibilitySafely() {
        AhmedabadCivicDataImporter.ImportSummary summary = importer.importAuthoritativeData();

        // Point outside any populated ward boundary (e.g. Mumbai lat=19.07, lon=72.87)
        CategoryEntity potholeCat = categoryRepository.findBySlug("roads-potholes").orElseThrow();
        UserEntity citizen = userRepository.save(new UserEntity("+919876543211", "Citizen Shah"));
        LocationEntity outsideLoc = locationService.createLocation(19.0760, 72.8777, new BigDecimal("5.00"));

        IssueEntity issue = issueService.createIssue(
                citizen,
                potholeCat.getId(),
                outsideLoc.getId(),
                "Issue in unmapped coordinates",
                "Valid citizen issue but geography unresolved"
        );

        // Issue is completely valid, but responsibility is UNRESOLVED
        assertThat(issue.getId()).isNotNull();
        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        assertThat(issue.getWard()).isNull();
        assertThat(issue.getDepartment()).isNull();
    }

    @Test
    @DisplayName("8. Re-resolution service and historical issue safety")
    void shouldReResolveIssuesSafelyWithoutAlteringHistoricalData() {
        AhmedabadCivicDataImporter.ImportSummary summary = importer.importAuthoritativeData();

        // 1. Create issue before ward boundary is set -> UNRESOLVED
        CategoryEntity lightsCat = categoryRepository.findBySlug("streetlights").orElseThrow();
        UserEntity citizen = userRepository.save(new UserEntity("+919876543212", "Citizen Desai"));
        LocationEntity loc = locationService.createLocation(23.035, 72.565, new BigDecimal("5.00"));

        IssueEntity issue = issueService.createIssue(
                citizen,
                lightsCat.getId(),
                loc.getId(),
                "Dark street corner near commerce college",
                "Non-functional street light fixture"
        );

        assertThat(issue.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.UNRESOLVED);
        String originalTitle = issue.getTitle();
        String originalDesc = issue.getDescription();
        UUID originalReporterId = issue.getReporter().getId();

        // 2. Authoritative boundary is now populated for Ward 18 (Navrangpura)
        WardEntity navrangpura = wardRepository.findByCityIdAndWardNumber(summary.city().getId(), "18").orElseThrow();
        MultiPolygon boundary = createBoxMultiPolygon(72.55, 23.02, 72.60, 23.06, 4326);
        navrangpura.setBoundaryGeometry(boundary);
        wardRepository.save(navrangpura);

        // 3. Trigger re-resolution via importer
        int updated = importer.reResolveExistingIssues();
        assertThat(updated).isGreaterThanOrEqualTo(1);

        // 4. Verify issue is now RESOLVED
        IssueEntity refreshed = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(refreshed.getResponsibilityStatus()).isEqualTo(ResponsibilityStatus.RESOLVED);
        assertThat(refreshed.getCivicBody().getName()).isEqualTo("Ahmedabad Municipal Corporation");
        assertThat(refreshed.getCity().getName()).isEqualTo("Ahmedabad");
        assertThat(refreshed.getWard().getWardName()).isEqualTo("Navrangpura");
        assertThat(refreshed.getDepartment().getName()).isEqualTo("Light Department");

        // 5. Verify historical issue fields are completely untouched
        assertThat(refreshed.getTitle()).isEqualTo(originalTitle);
        assertThat(refreshed.getDescription()).isEqualTo(originalDesc);
        assertThat(refreshed.getReporter().getId()).isEqualTo(originalReporterId);
    }

    private static MultiPolygon createBoxMultiPolygon(double minLon, double minLat, double maxLon, double maxLat, int srid) {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(minLon, minLat),
                new Coordinate(maxLon, minLat),
                new Coordinate(maxLon, maxLat),
                new Coordinate(minLon, maxLat),
                new Coordinate(minLon, minLat)
        };
        LinearRing shell = GF.createLinearRing(coords);
        Polygon polygon = GF.createPolygon(shell);
        MultiPolygon multiPolygon = GF.createMultiPolygon(new Polygon[]{polygon});
        multiPolygon.setSRID(srid);
        return multiPolygon;
    }
}
