package org.nagrivic.modules.civicgeography.service;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
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
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.DepartmentRepository;
import org.nagrivic.modules.issues.service.IssueResponsibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Reproducible, idempotent importer for Authoritative Ahmedabad Civic Data.
 * Sourced directly from Ahmedabad Municipal Corporation (AMC) official records:
 * - AMC Official Delimitation: 48 Municipal Wards across 7 administrative zones
 * - AMC Official Departments: Engineering (Roads & Bridges), Solid Waste Management,
 *   Light Department, Water Resources & Operation, Engineering - Drainage
 * - Category to Department responsibility mappings for core Nagrivic categories
 */
@Service
public class AhmedabadCivicDataImporter {

    private static final Logger log = LoggerFactory.getLogger(AhmedabadCivicDataImporter.class);

    public static final String AMC_NAME = "Ahmedabad Municipal Corporation";
    public static final String AMC_STATE = "Gujarat";
    public static final String AMC_CITY = "Ahmedabad";
    public static final String AMC_WEBSITE = "https://ahmedabadcity.gov.in";
    public static final String AMC_SOURCE = "Ahmedabad Municipal Corporation Official Delimitation";
    public static final String AMC_SOURCE_URL = "https://ahmedabadcity.gov.in";

    // Expected geographical bounding box for Ahmedabad municipal jurisdiction (WGS84)
    public static final double AHMEDABAD_MIN_LON = 72.30;
    public static final double AHMEDABAD_MAX_LON = 72.85;
    public static final double AHMEDABAD_MIN_LAT = 22.80;
    public static final double AHMEDABAD_MAX_LAT = 23.30;

    // Official 48 Ahmedabad Municipal Wards (delimited under Gujarat Provincial Municipal Corporations Act)
    public static final List<OfficialWardRecord> OFFICIAL_WARDS = List.of(
            new OfficialWardRecord("1", "Gota", "AMC-W01", "North West Zone"),
            new OfficialWardRecord("2", "Chandlodiya", "AMC-W02", "North West Zone"),
            new OfficialWardRecord("3", "Chandkheda", "AMC-W03", "West Zone"),
            new OfficialWardRecord("4", "Sabarmati", "AMC-W04", "West Zone"),
            new OfficialWardRecord("5", "Ranip", "AMC-W05", "West Zone"),
            new OfficialWardRecord("6", "Nawa Vadaj", "AMC-W06", "West Zone"),
            new OfficialWardRecord("7", "Ghatlodiya", "AMC-W07", "North West Zone"),
            new OfficialWardRecord("8", "Thaltej", "AMC-W08", "North West Zone"),
            new OfficialWardRecord("9", "Naranpura", "AMC-W09", "West Zone"),
            new OfficialWardRecord("10", "Sardar Patel Stadium", "AMC-W10", "West Zone"),
            new OfficialWardRecord("11", "Sardarnagar", "AMC-W11", "North Zone"),
            new OfficialWardRecord("12", "Naroda", "AMC-W12", "North Zone"),
            new OfficialWardRecord("13", "Saijpur Bogha", "AMC-W13", "North Zone"),
            new OfficialWardRecord("14", "Kubernagar", "AMC-W14", "North Zone"),
            new OfficialWardRecord("15", "Asarwa", "AMC-W15", "Central Zone"),
            new OfficialWardRecord("16", "Shahibaug", "AMC-W16", "Central Zone"),
            new OfficialWardRecord("17", "Shahpur", "AMC-W17", "Central Zone"),
            new OfficialWardRecord("18", "Navrangpura", "AMC-W18", "West Zone"),
            new OfficialWardRecord("19", "Bodakdev", "AMC-W19", "North West Zone"),
            new OfficialWardRecord("20", "Jodhpur", "AMC-W20", "South West Zone"),
            new OfficialWardRecord("21", "Dariapur", "AMC-W21", "Central Zone"),
            new OfficialWardRecord("22", "India Colony", "AMC-W22", "North Zone"),
            new OfficialWardRecord("23", "Thakkarbapanagar", "AMC-W23", "North Zone"),
            new OfficialWardRecord("24", "Nikol", "AMC-W24", "East Zone"),
            new OfficialWardRecord("25", "Viratnagar", "AMC-W25", "East Zone"),
            new OfficialWardRecord("26", "Bapunagar", "AMC-W26", "North Zone"),
            new OfficialWardRecord("27", "Saraspur", "AMC-W27", "North Zone"),
            new OfficialWardRecord("28", "Khadia", "AMC-W28", "Central Zone"),
            new OfficialWardRecord("29", "Jamalpur", "AMC-W29", "Central Zone"),
            new OfficialWardRecord("30", "Paldi", "AMC-W30", "West Zone"),
            new OfficialWardRecord("31", "Vasna", "AMC-W31", "West Zone"),
            new OfficialWardRecord("32", "Vejalpur", "AMC-W32", "South West Zone"),
            new OfficialWardRecord("33", "Sarkhej", "AMC-W33", "South West Zone"),
            new OfficialWardRecord("34", "Maktampura", "AMC-W34", "South West Zone"),
            new OfficialWardRecord("35", "Behrampura", "AMC-W35", "South Zone"),
            new OfficialWardRecord("36", "Danilimda", "AMC-W36", "South Zone"),
            new OfficialWardRecord("37", "Maninagar", "AMC-W37", "South Zone"),
            new OfficialWardRecord("38", "Gomtipur", "AMC-W38", "East Zone"),
            new OfficialWardRecord("39", "Amraiwadi", "AMC-W39", "East Zone"),
            new OfficialWardRecord("40", "Odhav", "AMC-W40", "East Zone"),
            new OfficialWardRecord("41", "Vastral", "AMC-W41", "East Zone"),
            new OfficialWardRecord("42", "Indrapuri", "AMC-W42", "South Zone"),
            new OfficialWardRecord("43", "Bhaipura - Hatkeshwar", "AMC-W43", "East Zone"),
            new OfficialWardRecord("44", "Khokhra", "AMC-W44", "South Zone"),
            new OfficialWardRecord("45", "Isanpur", "AMC-W45", "South Zone"),
            new OfficialWardRecord("46", "Lambha", "AMC-W46", "South Zone"),
            new OfficialWardRecord("47", "Vatva", "AMC-W47", "South Zone"),
            new OfficialWardRecord("48", "Ramol - Hathijan", "AMC-W48", "East Zone")
    );

    // Official AMC Departments
    public record OfficialDepartmentSpec(String name, String code, String description, String categorySlug) {}

    public static final List<OfficialDepartmentSpec> OFFICIAL_DEPARTMENTS = List.of(
            new OfficialDepartmentSpec(
                    "Engineering Department (Roads & Bridges)",
                    "AMC-ENG-ROADS",
                    "Responsible for municipal roads, pothole repair, asphalt resurfacing, and bridge infrastructure.",
                    "roads-potholes"
            ),
            new OfficialDepartmentSpec(
                    "Solid Waste Management Department",
                    "AMC-SWM",
                    "Responsible for street sweeping, door-to-door waste collection, bulk refuse, and public sanitation.",
                    "garbage"
            ),
            new OfficialDepartmentSpec(
                    "Light Department",
                    "AMC-LIGHT",
                    "Responsible for public street lighting, high-mast illumination, pole repairs, and electrical energy efficiency.",
                    "streetlights"
            ),
            new OfficialDepartmentSpec(
                    "Water Resources & Operation Department",
                    "AMC-WATER",
                    "Responsible for drinking water supply, distribution networks, pipeline repairs, and water treatment operations.",
                    "water"
            ),
            new OfficialDepartmentSpec(
                    "Engineering - Drainage Department",
                    "AMC-DRAINAGE",
                    "Responsible for underground sewerage networks, stormwater drains, manholes, pumping stations, and STPs.",
                    "drainage"
            )
    );

    public record OfficialWardRecord(String number, String name, String code, String zone) {}

    public record ImportSummary(
            CivicBodyEntity civicBody,
            CityEntity city,
            int wardsImported,
            int departmentsImported,
            int mappingsImported
    ) {}

    public record GeometryValidationResult(boolean isValid, List<String> violations) {}

    private final CivicBodyRepository civicBodyRepository;
    private final CityRepository cityRepository;
    private final WardRepository wardRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryDepartmentMappingRepository categoryDepartmentMappingRepository;
    private final IssueResponsibilityService issueResponsibilityService;

    public AhmedabadCivicDataImporter(
            CivicBodyRepository civicBodyRepository,
            CityRepository cityRepository,
            WardRepository wardRepository,
            DepartmentRepository departmentRepository,
            CategoryRepository categoryRepository,
            CategoryDepartmentMappingRepository categoryDepartmentMappingRepository,
            IssueResponsibilityService issueResponsibilityService
    ) {
        this.civicBodyRepository = civicBodyRepository;
        this.cityRepository = cityRepository;
        this.wardRepository = wardRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.categoryDepartmentMappingRepository = categoryDepartmentMappingRepository;
        this.issueResponsibilityService = issueResponsibilityService;
    }

    /**
     * Executes deterministic, idempotent import of Ahmedabad civic body, city, departments,
     * wards, and category mappings.
     */
    @Transactional
    public ImportSummary importAuthoritativeData() {
        log.info("Starting authoritative Ahmedabad civic data import...");

        // 1. Civic Body (Idempotent upsert)
        CivicBodyEntity civicBody = civicBodyRepository.findByName(AMC_NAME)
                .orElseGet(() -> {
                    CivicBodyEntity cb = new CivicBodyEntity(
                            AMC_NAME,
                            CivicBodyType.MUNICIPAL_CORPORATION,
                            AMC_STATE,
                            AMC_CITY
                    );
                    return cb;
                });
        civicBody.setOfficialWebsite(AMC_WEBSITE);
        civicBody.setSource(AMC_SOURCE);
        civicBody.setSourceUrl(AMC_SOURCE_URL);
        civicBody.setLastVerifiedAt(Instant.now());
        civicBody.setActive(true);
        civicBody = civicBodyRepository.save(civicBody);

        // 2. City (Idempotent upsert)
        final CivicBodyEntity finalCivicBody = civicBody;
        CityEntity city = cityRepository.findByNameAndState(AMC_CITY, AMC_STATE)
                .orElseGet(() -> new CityEntity(AMC_CITY, AMC_STATE, finalCivicBody));
        city.setCivicBody(civicBody);
        city.setActive(true);
        city = cityRepository.save(city);

        // 3. Departments (Idempotent upsert for the 5 authoritative municipal departments)
        Map<String, DepartmentEntity> deptMap = new HashMap<>();
        for (OfficialDepartmentSpec spec : OFFICIAL_DEPARTMENTS) {
            DepartmentEntity dept = departmentRepository.findByCivicBodyIdAndName(civicBody.getId(), spec.name())
                    .orElseGet(() -> new DepartmentEntity(finalCivicBody, spec.name(), spec.code(), spec.description()));
            dept.setCode(spec.code());
            dept.setDescription(spec.description());
            dept.setActive(true);
            dept = departmentRepository.save(dept);
            deptMap.put(spec.categorySlug(), dept);
        }

        // 4. Category -> Department Mappings (Idempotent creation)
        int mappingsCount = 0;
        for (OfficialDepartmentSpec spec : OFFICIAL_DEPARTMENTS) {
            Optional<CategoryEntity> catOpt = categoryRepository.findBySlug(spec.categorySlug());
            if (catOpt.isPresent()) {
                CategoryEntity cat = catOpt.get();
                DepartmentEntity dept = deptMap.get(spec.categorySlug());
                if (dept != null) {
                    Optional<CategoryDepartmentMappingEntity> existing =
                            categoryDepartmentMappingRepository.findByCategoryIdAndDepartmentId(cat.getId(), dept.getId());
                    if (existing.isEmpty()) {
                        categoryDepartmentMappingRepository.save(new CategoryDepartmentMappingEntity(cat, dept));
                    }
                    mappingsCount++;
                }
            } else {
                log.warn("Category with slug '{}' not found in database; mapping skipped", spec.categorySlug());
            }
        }

        // 5. Official Wards (Idempotent upsert for all 48 wards)
        int wardsCount = 0;
        for (OfficialWardRecord wr : OFFICIAL_WARDS) {
            Optional<WardEntity> existingOpt = wardRepository.findByCityIdAndWardNumber(city.getId(), wr.number());
            WardEntity ward;
            if (existingOpt.isPresent()) {
                ward = existingOpt.get();
                ward.setWardName(wr.name());
                ward.setWardCode(wr.code());
            } else {
                ward = new WardEntity(city, civicBody, wr.number(), wr.name());
                ward.setWardCode(wr.code());
            }
            ward.setCivicBody(civicBody);
            ward.setSource(AMC_SOURCE);
            ward.setSourceUrl(AMC_SOURCE_URL);
            ward.setLastVerifiedAt(Instant.now());
            ward.setActive(true);
            wardRepository.save(ward);
            wardsCount++;
        }

        log.info("Authoritative Ahmedabad civic import complete: 1 civic body, 1 city, {} departments, {} wards, {} mappings",
                deptMap.size(), wardsCount, mappingsCount);

        return new ImportSummary(civicBody, city, wardsCount, deptMap.size(), mappingsCount);
    }

    /**
     * Rigorously validates geometry quality against administrative standards:
     * - MultiPolygon type
     * - SRID = 4326
     * - ST_IsValid / JTS isValid
     * - Non-empty
     * - Bounds within expected Ahmedabad geographic extent
     */
    public GeometryValidationResult validateGeometry(Geometry geometry) {
        List<String> violations = new ArrayList<>();

        if (geometry == null) {
            violations.add("Geometry is NULL");
            return new GeometryValidationResult(false, violations);
        }

        if (geometry.isEmpty()) {
            violations.add("Geometry is EMPTY");
        }

        if (geometry.getSRID() != 4326) {
            violations.add("Invalid SRID: expected 4326, found " + geometry.getSRID());
        }

        if (!(geometry instanceof MultiPolygon)) {
            violations.add("Invalid geometry type: expected MultiPolygon, found " + geometry.getGeometryType());
        }

        if (!geometry.isValid()) {
            violations.add("Geometry is topologically INVALID (self-intersection, improper nesting, or degenerate rings)");
        }

        Envelope env = geometry.getEnvelopeInternal();
        if (env.getMinX() < AHMEDABAD_MIN_LON || env.getMaxX() > AHMEDABAD_MAX_LON
                || env.getMinY() < AHMEDABAD_MIN_LAT || env.getMaxY() > AHMEDABAD_MAX_LAT) {
            violations.add(String.format("Geometry envelope [%.4f, %.4f, %.4f, %.4f] falls outside expected Ahmedabad extent [%.2f, %.2f, %.2f, %.2f]",
                    env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(),
                    AHMEDABAD_MIN_LON, AHMEDABAD_MIN_LAT, AHMEDABAD_MAX_LON, AHMEDABAD_MAX_LAT));
        }

        return new GeometryValidationResult(violations.isEmpty(), violations);
    }

    /**
     * Executes re-resolution across existing civic issues using the imported geography and mappings.
     * Preserves all unrelated issue data (status, priority, reporter, media, activity).
     */
    @Transactional
    public int reResolveExistingIssues() {
        return issueResponsibilityService.reResolveAllIssues();
    }
}
