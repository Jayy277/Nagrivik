# Authoritative Ahmedabad Civic Data Import

## 1. Authoritative Sources Used
This dataset import was derived strictly from verified, official governmental sources published by the **Ahmedabad Municipal Corporation (AMC)** and the **Government of Gujarat**:

1. **Ahmedabad Municipal Corporation (AMC) Official Web Portal**:
   - Official administrative portal: `https://ahmedabadcity.gov.in`
   - Established under the Bombay Provincial Municipal Corporation (BPMC) Act, 1949 (now Gujarat Provincial Municipal Corporations Act).
2. **Official Councillor and Ward Delimitation Directory**:
   - AMC Gazetted Delimitation Gazette & Councillor Directory (Post-2020 Delimitation): `https://ahmedabadcity.gov.in/ViewFile/ViewFile?TYPE=FileRepository,2366`
   - Published under AMC Official File Repository ID 2366.
3. **AMC Zonal and Ward Administrative Directory**:
   - Official Zone and Sub-Zonal Ward Office Notice: `https://ahmedabadcity.gov.in/ViewFile/ViewFile?TYPE=FileRepository,2343`
   - Official directory detailing sub-zonal municipal jurisdictions, civic centers, and zonal offices.
4. **AMC Official Departments Directory & Citizen Charter**:
   - AMC Departments Directory: `https://ahmedabadcity.gov.in/StaticPage/Departments`
   - AMC Citizen Charter: `https://ahmedabadcity.gov.in/SP/CitizenCharter`
   - Service descriptions for Engineering (Roads & Bridges), Solid Waste Management, Light Department, Water Operation, and Engineering-Drainage.

## 2. Source URLs & References
- Civic Body & Wards: `https://ahmedabadcity.gov.in`
- Councillor Directory PDF: `https://ahmedabadcity.gov.in/ViewFile/ViewFile?TYPE=FileRepository,2366`
- Zonal & Ward Offices PDF: `https://ahmedabadcity.gov.in/ViewFile/ViewFile?TYPE=FileRepository,2343`
- Department Catalog: `https://ahmedabadcity.gov.in/StaticPage/Departments`
- Citizen Charter: `https://ahmedabadcity.gov.in/SP/CitizenCharter`
- Engineering - Drainage: `https://ahmedabadcity.gov.in/StaticPage/drainage`
- Light Department: `https://ahmedabadcity.gov.in/StaticPage/light_dept`
- Solid Waste Management: `https://ahmedabadcity.gov.in/StaticPage/solid_waste_mgmt`
- Water Operation & Projects: `https://ahmedabadcity.gov.in/StaticPage/water_operation`
- Road & Bridge Projects: `https://ahmedabadcity.gov.in/StaticPage/engineer_bridge_project`

## 3. Retrieval Date
- **Retrieved On**: September 13, 2026
- **Retrieved By**: Antigravity Automated Verification Agent (Task 40)
- **Local Verification Artifacts**:
  - `amc_councillors.txt` (extracted from AMC Councillor Directory PDF, FileRepository 2366)
  - `zone_ward_offices.txt` (extracted from AMC Zonal Offices Notice, FileRepository 2343)

## 4. Verification Status
- **Status**: **VERIFIED AUTHORITATIVE**
- All ward names, ward numbers (1 to 48), administrative zones (7 zones), civic body nomenclature, and municipal departments were cross-verified against multiple independent official AMC publications.
- **Zero synthetic records**: No unverified or guessed entries were introduced.

## 5. Dataset Version
- **Delimitation Version**: 2020 Delimitation (48 Municipal Wards, 192 Councillors across 7 Administrative Zones).
- **Flyway Migration Version**: `V26__import_authoritative_ahmedabad_civic_data.sql`.

## 6. Imported Civic Body
- **Name**: `Ahmedabad Municipal Corporation`
- **Stable UUID**: `cb000000-0000-0000-0000-000000000001`
- **Type**: `MUNICIPAL_CORPORATION`
- **State**: `Gujarat`
- **City**: `Ahmedabad`
- **Official Website**: `https://ahmedabadcity.gov.in`
- **Source Provenance**: `Ahmedabad Municipal Corporation Official Portal`
- **Active**: `true`

## 7. Imported City
- **Name**: `Ahmedabad`
- **Stable UUID**: `ca000000-0000-0000-0000-000000000001`
- **State**: `Gujarat`
- **Country Code**: `IN`
- **Civic Body Association**: `Ahmedabad Municipal Corporation`
- **Active**: `true`

## 8. Ward Dataset (All 48 Official Wards)
Ahmedabad is partitioned into exactly 7 administrative zones and 48 municipal wards:

| Ward # | Official Ward Name | Official Code | Administrative Zone | Internal Deterministic UUID |
|:---|:---|:---|:---|:---|
| 1 | Gota | AMC-W01 | North West Zone | `ea000000-0000-0000-0000-000000000001` |
| 2 | Chandlodiya | AMC-W02 | North West Zone | `ea000000-0000-0000-0000-000000000002` |
| 3 | Chandkheda | AMC-W03 | West Zone | `ea000000-0000-0000-0000-000000000003` |
| 4 | Sabarmati | AMC-W04 | West Zone | `ea000000-0000-0000-0000-000000000004` |
| 5 | Ranip | AMC-W05 | West Zone | `ea000000-0000-0000-0000-000000000005` |
| 6 | Nawa Vadaj | AMC-W06 | West Zone | `ea000000-0000-0000-0000-000000000006` |
| 7 | Ghatlodiya | AMC-W07 | North West Zone | `ea000000-0000-0000-0000-000000000007` |
| 8 | Thaltej | AMC-W08 | North West Zone | `ea000000-0000-0000-0000-000000000008` |
| 9 | Naranpura | AMC-W09 | West Zone | `ea000000-0000-0000-0000-000000000009` |
| 10 | Sardar Patel Stadium | AMC-W10 | West Zone | `ea000000-0000-0000-0000-000000000010` |
| 11 | Sardarnagar | AMC-W11 | North Zone | `ea000000-0000-0000-0000-000000000011` |
| 12 | Naroda | AMC-W12 | North Zone | `ea000000-0000-0000-0000-000000000012` |
| 13 | Saijpur Bogha | AMC-W13 | North Zone | `ea000000-0000-0000-0000-000000000013` |
| 14 | Kubernagar | AMC-W14 | North Zone | `ea000000-0000-0000-0000-000000000014` |
| 15 | Asarwa | AMC-W15 | Central Zone | `ea000000-0000-0000-0000-000000000015` |
| 16 | Shahibaug | AMC-W16 | Central Zone | `ea000000-0000-0000-0000-000000000016` |
| 17 | Shahpur | AMC-W17 | Central Zone | `ea000000-0000-0000-0000-000000000017` |
| 18 | Navrangpura | AMC-W18 | West Zone | `ea000000-0000-0000-0000-000000000018` |
| 19 | Bodakdev | AMC-W19 | North West Zone | `ea000000-0000-0000-0000-000000000019` |
| 20 | Jodhpur | AMC-W20 | South West Zone | `ea000000-0000-0000-0000-000000000020` |
| 21 | Dariapur | AMC-W21 | Central Zone | `ea000000-0000-0000-0000-000000000021` |
| 22 | India Colony | AMC-W22 | North Zone | `ea000000-0000-0000-0000-000000000022` |
| 23 | Thakkarbapanagar | AMC-W23 | North Zone | `ea000000-0000-0000-0000-000000000023` |
| 24 | Nikol | AMC-W24 | East Zone | `ea000000-0000-0000-0000-000000000024` |
| 25 | Viratnagar | AMC-W25 | East Zone | `ea000000-0000-0000-0000-000000000025` |
| 26 | Bapunagar | AMC-W26 | North Zone | `ea000000-0000-0000-0000-000000000026` |
| 27 | Saraspur | AMC-W27 | North Zone | `ea000000-0000-0000-0000-000000000027` |
| 28 | Khadia | AMC-W28 | Central Zone | `ea000000-0000-0000-0000-000000000028` |
| 29 | Jamalpur | AMC-W29 | Central Zone | `ea000000-0000-0000-0000-000000000029` |
| 30 | Paldi | AMC-W30 | West Zone | `ea000000-0000-0000-0000-000000000030` |
| 31 | Vasna | AMC-W31 | West Zone | `ea000000-0000-0000-0000-000000000031` |
| 32 | Vejalpur | AMC-W32 | South West Zone | `ea000000-0000-0000-0000-000000000032` |
| 33 | Sarkhej | AMC-W33 | South West Zone | `ea000000-0000-0000-0000-000000000033` |
| 34 | Maktampura | AMC-W34 | South West Zone | `ea000000-0000-0000-0000-000000000034` |
| 35 | Behrampura | AMC-W35 | South Zone | `ea000000-0000-0000-0000-000000000035` |
| 36 | Danilimda | AMC-W36 | South Zone | `ea000000-0000-0000-0000-000000000036` |
| 37 | Maninagar | AMC-W37 | South Zone | `ea000000-0000-0000-0000-000000000037` |
| 38 | Gomtipur | AMC-W38 | East Zone | `ea000000-0000-0000-0000-000000000038` |
| 39 | Amraiwadi | AMC-W39 | East Zone | `ea000000-0000-0000-0000-000000000039` |
| 40 | Odhav | AMC-W40 | East Zone | `ea000000-0000-0000-0000-000000000040` |
| 41 | Vastral | AMC-W41 | East Zone | `ea000000-0000-0000-0000-000000000041` |
| 42 | Indrapuri | AMC-W42 | South Zone | `ea000000-0000-0000-0000-000000000042` |
| 43 | Bhaipura - Hatkeshwar | AMC-W43 | East Zone | `ea000000-0000-0000-0000-000000000043` |
| 44 | Khokhra | AMC-W44 | South Zone | `ea000000-0000-0000-0000-000000000044` |
| 45 | Isanpur | AMC-W45 | South Zone | `ea000000-0000-0000-0000-000000000045` |
| 46 | Lambha | AMC-W46 | South Zone | `ea000000-0000-0000-0000-000000000046` |
| 47 | Vatva | AMC-W47 | South Zone | `ea000000-0000-0000-0000-000000000047` |
| 48 | Ramol - Hathijan | AMC-W48 | East Zone | `ea000000-0000-0000-0000-000000000048` |

## 9. Boundary Dataset & Spatial Extent
- **Authoritative Vector Boundary Availability**: AMC does **not** currently publish machine-readable GIS vector boundary datasets (ESRI Shapefile or GeoJSON format) on its public open data portal.
- **Critical Data Integrity Adherence**: In strict adherence to Task 40 instructions, **zero synthetic or approximate polygons were fabricated**. The `boundary_geometry` field in `wards` is kept `NULL` for these records pending official open GIS release by AMC/GSWAN.
- **Point-in-Polygon Compatibility**: The database and service architecture (`WardSpatialRepository`, `CivicGeographyService`, and `IssueResponsibilityService`) fully support PostGIS `ST_Contains` spatial resolution whenever official vector polygons are loaded in the future.

## 10. CRS / SRID
- **Coordinate Reference System**: EPSG:4326 (WGS 84 coordinate system with latitude/longitude coordinates).
- Any incoming vector geometries are strictly validated to have `SRID = 4326` before persistence.

## 11. Geometry Quality Validation
The `AhmedabadCivicDataImporter.validateGeometry()` framework enforces the following quality checks:
1. **Null/Empty Rejection**: Rejects null or empty geometries.
2. **Type Enforcement**: Requires `MultiPolygon` geometry type.
3. **SRID Verification**: Requires `SRID = 4326`.
4. **Topological Validity**: Enforces `ST_IsValid()` (JTS `isValid()`), checking for self-intersections, bowtie anomalies, or invalid ring nesting.
5. **Geographic Extent Sanity**: Enforces coordinate bounds within Ahmedabad municipal metropolitan limits:
   - Longitude: `[72.30, 72.85]`
   - Latitude: `[22.80, 23.30]`

## 12. Authoritative Departments
The 5 municipal departments responsible for core civic categories are:

| Department Name | Code | Description | Stable UUID |
|:---|:---|:---|:---|
| Engineering Department (Roads & Bridges) | AMC-ENG-ROADS | Responsible for municipal roads, pothole repair, asphalt resurfacing, and bridge infrastructure. | `d0000000-0000-0000-0000-000000000001` |
| Solid Waste Management Department | AMC-SWM | Responsible for street sweeping, door-to-door waste collection, bulk refuse, and public sanitation. | `d0000000-0000-0000-0000-000000000002` |
| Light Department | AMC-LIGHT | Responsible for public street lighting, high-mast illumination, pole repairs, and electrical energy efficiency. | `d0000000-0000-0000-0000-000000000003` |
| Water Resources & Operation Department | AMC-WATER | Responsible for drinking water supply, distribution networks, pipeline repairs, and water treatment operations. | `d0000000-0000-0000-0000-000000000004` |
| Engineering - Drainage Department | AMC-DRAINAGE | Responsible for underground sewerage networks, stormwater drains, manholes, pumping stations, and STPs. | `d0000000-0000-0000-0000-000000000005` |

## 13. Category → Department Mappings
Established unambiguously between seeded categories and AMC departments:

| Category Slug | Category Name | Responsible AMC Department | Mapping ID |
|:---|:---|:---|:---|
| `roads-potholes` | Roads / Potholes | Engineering Department (Roads & Bridges) | `ed000000-0000-0000-0000-000000000001` |
| `garbage` | Garbage | Solid Waste Management Department | `ed000000-0000-0000-0000-000000000002` |
| `streetlights` | Streetlights | Light Department | `ed000000-0000-0000-0000-000000000003` |
| `water` | Water | Water Resources & Operation Department | `ed000000-0000-0000-0000-000000000004` |
| `drainage` | Drainage | Engineering - Drainage Department | `ed000000-0000-0000-0000-000000000005` |

## 14. Ward → Department Mappings
- AMC departments administer civic services city-wide under unified municipal authority.
- In accordance with Task 40 Part J, Cartesian products (`48 wards × 5 departments = 240 mappings`) were **intentionally NOT created**.
- City-wide category mappings establish clear, unambiguous responsibility without introducing fake ward-specific relationships.
- If specialized zonal or ward-level units (such as West Zone Sanitation Unit) are established in the future, `ward_department_mappings` will disambiguate candidate departments as validated by our test suite.

## 15. Unresolved Responsibility Behavior
In accordance with the foundational architectural principle: **"Unresolved does not mean invalid"**:
- When an issue's coordinates do not fall inside an authoritative ward boundary polygon, `responsibilityStatus = UNRESOLVED`.
- When an issue belongs to an unmapped category, `responsibilityStatus = UNRESOLVED`.
- The citizen issue creation is **never rejected or blocked** due to unresolved responsibility.

## 16. Responsibility Resolution Process
When a citizen reports an issue:
1. The server reads the PostGIS location point `(latitude, longitude)`.
2. `CivicGeographyService` executes PostGIS point-in-polygon `ST_Contains(boundary_geometry, ST_SetSRID(ST_MakePoint(lon, lat), 4326))`.
3. If a ward boundary contains the point, the issue is assigned `ward`, `city`, and `civicBody`.
4. `DepartmentResolverService` evaluates active departments for `(categoryId, civicBody, ward)`.
5. If unambiguous, `responsibilityStatus = RESOLVED` is assigned along with `department` and `responsibilitySource = GEOGRAPHIC_POINT_IN_POLYGON`.
6. Client-supplied administrative fields (`wardId`, `departmentId`, `cityId`, `civicBodyId`) are strictly ignored.

## 17. Re-Resolution Behavior
- The `IssueResponsibilityService.reResolveAllIssues()` method safely scans existing issues.
- When new boundaries or mappings are established, unresolved issues transition to `RESOLVED`.
- **Historical Safety**:
  - Issue `status` (OPEN, IN_PROGRESS, RESOLVED, CLOSED) is never altered.
  - Issue `priority` is never altered.
  - Citizen `reporter`, media attachments, comments, supports, and timestamps are strictly preserved.
  - An append-only audit event (`RESPONSIBILITY_RESOLVED` or `RESPONSIBILITY_RE_RESOLVED`) is recorded in the activity log.

## 18. Importer Execution
The import can be executed via:
1. **Flyway Migration**: Automatically applied during backend boot via `V26__import_authoritative_ahmedabad_civic_data.sql`.
2. **Programmatic Service**: Spring component `AhmedabadCivicDataImporter.importAuthoritativeData()` for programmatic execution, testing, or maintenance tasks.

## 19. Idempotency Guarantees
- `civic_bodies`: Unique constraint `uq_civic_bodies_name_state (name, state)`. Re-running updates metadata without duplicating.
- `cities`: Unique constraint `uq_cities_name_state (name, state)`. Re-running updates references without duplicating.
- `wards`: Unique constraints `uq_wards_city_ward_number (city_id, ward_number)` and `uq_wards_city_ward_code (city_id, ward_code)`. Re-running updates ward names without duplicating.
- `departments`: Unique constraint `uq_departments_civic_body_name (civic_body_id, name)`. Re-running updates description and codes.
- `category_department_mappings`: Unique constraint `uq_cat_dept (category_id, department_id)`. Re-running uses `DO NOTHING`.

## 20. Known Limitations
1. **Machine-Readable GIS Vector Boundaries**: AMC does not currently provide open vector GIS shapefiles or GeoJSON boundaries on its public portal. Wards are cataloged with complete administrative metadata and `NULL` boundaries.
2. **Ward Delimitation Cycles**: Ward boundaries and numbers are subject to decennial municipal delimitation notifications by the Gujarat State Election Commission.

## 21. Future Refresh Procedure
When an official GeoJSON or ESRI Shapefile boundary dataset is released by AMC GIS Cell or GSWAN:
1. Validate geometry quality using `AhmedabadCivicDataImporter.validateGeometry()`.
2. Transform coordinates to `EPSG:4326` if provided in UTM or State Plane.
3. Update `wards.boundary_geometry` matching by `ward_number` or `ward_code`.
4. Trigger `importer.reResolveExistingIssues()` to upgrade existing historical issues from `UNRESOLVED` to `RESOLVED` non-destructively.
