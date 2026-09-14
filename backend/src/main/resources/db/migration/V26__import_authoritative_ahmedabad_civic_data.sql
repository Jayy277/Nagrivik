-- V26: Authoritative Ahmedabad Civic Data Import
-- Sources:
-- 1. Ahmedabad Municipal Corporation (AMC) Official Portal: https://ahmedabadcity.gov.in
-- 2. AMC Councillor and Delimitation Directory: https://ahmedabadcity.gov.in/ViewFile/ViewFile?TYPE=FileRepository,2366
-- 3. AMC Zonal and Ward Administrative Offices: https://ahmedabadcity.gov.in/ViewFile/ViewFile?TYPE=FileRepository,2343
-- 4. AMC Official Department Directory & Citizen Charter: https://ahmedabadcity.gov.in/StaticPage/Departments

-- Ensure unique constraints/indexes exist for idempotent execution
CREATE UNIQUE INDEX IF NOT EXISTS uq_civic_bodies_name_state ON civic_bodies(name, state);
CREATE UNIQUE INDEX IF NOT EXISTS uq_wards_city_ward_number ON wards(city_id, ward_number);
CREATE UNIQUE INDEX IF NOT EXISTS uq_wards_city_ward_code ON wards(city_id, ward_code);

-- 1. Authoritative Civic Body: Ahmedabad Municipal Corporation
INSERT INTO civic_bodies (
    id, name, type, state, city, official_website, source, source_url, last_verified_at, is_active, created_at, updated_at
) VALUES (
    'cb000000-0000-0000-0000-000000000001',
    'Ahmedabad Municipal Corporation',
    'MUNICIPAL_CORPORATION',
    'Gujarat',
    'Ahmedabad',
    'https://ahmedabadcity.gov.in',
    'Ahmedabad Municipal Corporation Official Portal',
    'https://ahmedabadcity.gov.in',
    NOW(),
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (name, state) DO UPDATE
SET official_website = EXCLUDED.official_website,
    source = EXCLUDED.source,
    source_url = EXCLUDED.source_url,
    last_verified_at = EXCLUDED.last_verified_at,
    is_active = TRUE,
    updated_at = NOW();

-- 2. Authoritative City: Ahmedabad
INSERT INTO cities (
    id, name, state, country_code, civic_body_id, is_active, created_at, updated_at
)
SELECT
    'ca000000-0000-0000-0000-000000000001',
    'Ahmedabad',
    'Gujarat',
    'IN',
    cb.id,
    TRUE,
    NOW(),
    NOW()
FROM civic_bodies cb
WHERE cb.name = 'Ahmedabad Municipal Corporation' AND cb.state = 'Gujarat'
LIMIT 1
ON CONFLICT (name, state) DO UPDATE
SET civic_body_id = EXCLUDED.civic_body_id,
    country_code = EXCLUDED.country_code,
    is_active = TRUE,
    updated_at = NOW();

-- 3. Authoritative Departments
-- (A) Roads / Potholes: Engineering Department (Roads & Bridges)
INSERT INTO departments (id, civic_body_id, name, code, description, is_active, created_at, updated_at)
SELECT
    'd0000000-0000-0000-0000-000000000001',
    cb.id,
    'Engineering Department (Roads & Bridges)',
    'AMC-ENG-ROADS',
    'Responsible for municipal roads, pothole repair, asphalt resurfacing, and bridge infrastructure.',
    TRUE,
    NOW(),
    NOW()
FROM civic_bodies cb WHERE cb.name = 'Ahmedabad Municipal Corporation' AND cb.state = 'Gujarat' LIMIT 1
ON CONFLICT (civic_body_id, name) DO UPDATE
SET code = EXCLUDED.code,
    description = EXCLUDED.description,
    is_active = TRUE,
    updated_at = NOW();

-- (B) Garbage: Solid Waste Management Department
INSERT INTO departments (id, civic_body_id, name, code, description, is_active, created_at, updated_at)
SELECT
    'd0000000-0000-0000-0000-000000000002',
    cb.id,
    'Solid Waste Management Department',
    'AMC-SWM',
    'Responsible for street sweeping, door-to-door waste collection, bulk refuse, and public sanitation.',
    TRUE,
    NOW(),
    NOW()
FROM civic_bodies cb WHERE cb.name = 'Ahmedabad Municipal Corporation' AND cb.state = 'Gujarat' LIMIT 1
ON CONFLICT (civic_body_id, name) DO UPDATE
SET code = EXCLUDED.code,
    description = EXCLUDED.description,
    is_active = TRUE,
    updated_at = NOW();

-- (C) Streetlights: Light Department
INSERT INTO departments (id, civic_body_id, name, code, description, is_active, created_at, updated_at)
SELECT
    'd0000000-0000-0000-0000-000000000003',
    cb.id,
    'Light Department',
    'AMC-LIGHT',
    'Responsible for public street lighting, high-mast illumination, pole repairs, and electrical energy efficiency.',
    TRUE,
    NOW(),
    NOW()
FROM civic_bodies cb WHERE cb.name = 'Ahmedabad Municipal Corporation' AND cb.state = 'Gujarat' LIMIT 1
ON CONFLICT (civic_body_id, name) DO UPDATE
SET code = EXCLUDED.code,
    description = EXCLUDED.description,
    is_active = TRUE,
    updated_at = NOW();

-- (D) Water: Water Resources & Operation Department
INSERT INTO departments (id, civic_body_id, name, code, description, is_active, created_at, updated_at)
SELECT
    'd0000000-0000-0000-0000-000000000004',
    cb.id,
    'Water Resources & Operation Department',
    'AMC-WATER',
    'Responsible for drinking water supply, distribution networks, pipeline repairs, and water treatment operations.',
    TRUE,
    NOW(),
    NOW()
FROM civic_bodies cb WHERE cb.name = 'Ahmedabad Municipal Corporation' AND cb.state = 'Gujarat' LIMIT 1
ON CONFLICT (civic_body_id, name) DO UPDATE
SET code = EXCLUDED.code,
    description = EXCLUDED.description,
    is_active = TRUE,
    updated_at = NOW();

-- (E) Drainage: Engineering - Drainage Department
INSERT INTO departments (id, civic_body_id, name, code, description, is_active, created_at, updated_at)
SELECT
    'd0000000-0000-0000-0000-000000000005',
    cb.id,
    'Engineering - Drainage Department',
    'AMC-DRAINAGE',
    'Responsible for underground sewerage networks, stormwater drains, manholes, pumping stations, and STPs.',
    TRUE,
    NOW(),
    NOW()
FROM civic_bodies cb WHERE cb.name = 'Ahmedabad Municipal Corporation' AND cb.state = 'Gujarat' LIMIT 1
ON CONFLICT (civic_body_id, name) DO UPDATE
SET code = EXCLUDED.code,
    description = EXCLUDED.description,
    is_active = TRUE,
    updated_at = NOW();

-- 4. Authoritative Category -> Department Mappings
-- Roads / Potholes -> Engineering Department (Roads & Bridges)
INSERT INTO category_department_mappings (id, category_id, department_id, created_at)
SELECT
    'ed000000-0000-0000-0000-000000000001',
    c.id,
    d.id,
    NOW()
FROM categories c
CROSS JOIN departments d
JOIN civic_bodies cb ON d.civic_body_id = cb.id
WHERE c.slug = 'roads-potholes'
  AND d.code = 'AMC-ENG-ROADS'
  AND cb.name = 'Ahmedabad Municipal Corporation'
ON CONFLICT (category_id, department_id) DO NOTHING;

-- Garbage -> Solid Waste Management Department
INSERT INTO category_department_mappings (id, category_id, department_id, created_at)
SELECT
    'ed000000-0000-0000-0000-000000000002',
    c.id,
    d.id,
    NOW()
FROM categories c
CROSS JOIN departments d
JOIN civic_bodies cb ON d.civic_body_id = cb.id
WHERE c.slug = 'garbage'
  AND d.code = 'AMC-SWM'
  AND cb.name = 'Ahmedabad Municipal Corporation'
ON CONFLICT (category_id, department_id) DO NOTHING;

-- Streetlights -> Light Department
INSERT INTO category_department_mappings (id, category_id, department_id, created_at)
SELECT
    'ed000000-0000-0000-0000-000000000003',
    c.id,
    d.id,
    NOW()
FROM categories c
CROSS JOIN departments d
JOIN civic_bodies cb ON d.civic_body_id = cb.id
WHERE c.slug = 'streetlights'
  AND d.code = 'AMC-LIGHT'
  AND cb.name = 'Ahmedabad Municipal Corporation'
ON CONFLICT (category_id, department_id) DO NOTHING;

-- Water -> Water Resources & Operation Department
INSERT INTO category_department_mappings (id, category_id, department_id, created_at)
SELECT
    'ed000000-0000-0000-0000-000000000004',
    c.id,
    d.id,
    NOW()
FROM categories c
CROSS JOIN departments d
JOIN civic_bodies cb ON d.civic_body_id = cb.id
WHERE c.slug = 'water'
  AND d.code = 'AMC-WATER'
  AND cb.name = 'Ahmedabad Municipal Corporation'
ON CONFLICT (category_id, department_id) DO NOTHING;

-- Drainage -> Engineering - Drainage Department
INSERT INTO category_department_mappings (id, category_id, department_id, created_at)
SELECT
    'ed000000-0000-0000-0000-000000000005',
    c.id,
    d.id,
    NOW()
FROM categories c
CROSS JOIN departments d
JOIN civic_bodies cb ON d.civic_body_id = cb.id
WHERE c.slug = 'drainage'
  AND d.code = 'AMC-DRAINAGE'
  AND cb.name = 'Ahmedabad Municipal Corporation'
ON CONFLICT (category_id, department_id) DO NOTHING;

-- 5. Authoritative 48 Ahmedabad Wards
INSERT INTO wards (
    id, city_id, civic_body_id, ward_number, ward_name, ward_code, boundary_geometry, source, source_url, last_verified_at, is_active, created_at, updated_at
)
SELECT
    w_data.id,
    ct.id,
    cb.id,
    w_data.ward_number,
    w_data.ward_name,
    w_data.ward_code,
    NULL,
    'Ahmedabad Municipal Corporation Official Delimitation',
    'https://ahmedabadcity.gov.in',
    NOW(),
    TRUE,
    NOW(),
    NOW()
FROM (VALUES
    ('ea000000-0000-0000-0000-000000000001'::uuid, '1', 'Gota', 'AMC-W01'),
    ('ea000000-0000-0000-0000-000000000002'::uuid, '2', 'Chandlodiya', 'AMC-W02'),
    ('ea000000-0000-0000-0000-000000000003'::uuid, '3', 'Chandkheda', 'AMC-W03'),
    ('ea000000-0000-0000-0000-000000000004'::uuid, '4', 'Sabarmati', 'AMC-W04'),
    ('ea000000-0000-0000-0000-000000000005'::uuid, '5', 'Ranip', 'AMC-W05'),
    ('ea000000-0000-0000-0000-000000000006'::uuid, '6', 'Nawa Vadaj', 'AMC-W06'),
    ('ea000000-0000-0000-0000-000000000007'::uuid, '7', 'Ghatlodiya', 'AMC-W07'),
    ('ea000000-0000-0000-0000-000000000008'::uuid, '8', 'Thaltej', 'AMC-W08'),
    ('ea000000-0000-0000-0000-000000000009'::uuid, '9', 'Naranpura', 'AMC-W09'),
    ('ea000000-0000-0000-0000-000000000010'::uuid, '10', 'Sardar Patel Stadium', 'AMC-W10'),
    ('ea000000-0000-0000-0000-000000000011'::uuid, '11', 'Sardarnagar', 'AMC-W11'),
    ('ea000000-0000-0000-0000-000000000012'::uuid, '12', 'Naroda', 'AMC-W12'),
    ('ea000000-0000-0000-0000-000000000013'::uuid, '13', 'Saijpur Bogha', 'AMC-W13'),
    ('ea000000-0000-0000-0000-000000000014'::uuid, '14', 'Kubernagar', 'AMC-W14'),
    ('ea000000-0000-0000-0000-000000000015'::uuid, '15', 'Asarwa', 'AMC-W15'),
    ('ea000000-0000-0000-0000-000000000016'::uuid, '16', 'Shahibaug', 'AMC-W16'),
    ('ea000000-0000-0000-0000-000000000017'::uuid, '17', 'Shahpur', 'AMC-W17'),
    ('ea000000-0000-0000-0000-000000000018'::uuid, '18', 'Navrangpura', 'AMC-W18'),
    ('ea000000-0000-0000-0000-000000000019'::uuid, '19', 'Bodakdev', 'AMC-W19'),
    ('ea000000-0000-0000-0000-000000000020'::uuid, '20', 'Jodhpur', 'AMC-W20'),
    ('ea000000-0000-0000-0000-000000000021'::uuid, '21', 'Dariapur', 'AMC-W21'),
    ('ea000000-0000-0000-0000-000000000022'::uuid, '22', 'India Colony', 'AMC-W22'),
    ('ea000000-0000-0000-0000-000000000023'::uuid, '23', 'Thakkarbapanagar', 'AMC-W23'),
    ('ea000000-0000-0000-0000-000000000024'::uuid, '24', 'Nikol', 'AMC-W24'),
    ('ea000000-0000-0000-0000-000000000025'::uuid, '25', 'Viratnagar', 'AMC-W25'),
    ('ea000000-0000-0000-0000-000000000026'::uuid, '26', 'Bapunagar', 'AMC-W26'),
    ('ea000000-0000-0000-0000-000000000027'::uuid, '27', 'Saraspur', 'AMC-W27'),
    ('ea000000-0000-0000-0000-000000000028'::uuid, '28', 'Khadia', 'AMC-W28'),
    ('ea000000-0000-0000-0000-000000000029'::uuid, '29', 'Jamalpur', 'AMC-W29'),
    ('ea000000-0000-0000-0000-000000000030'::uuid, '30', 'Paldi', 'AMC-W30'),
    ('ea000000-0000-0000-0000-000000000031'::uuid, '31', 'Vasna', 'AMC-W31'),
    ('ea000000-0000-0000-0000-000000000032'::uuid, '32', 'Vejalpur', 'AMC-W32'),
    ('ea000000-0000-0000-0000-000000000033'::uuid, '33', 'Sarkhej', 'AMC-W33'),
    ('ea000000-0000-0000-0000-000000000034'::uuid, '34', 'Maktampura', 'AMC-W34'),
    ('ea000000-0000-0000-0000-000000000035'::uuid, '35', 'Behrampura', 'AMC-W35'),
    ('ea000000-0000-0000-0000-000000000036'::uuid, '36', 'Danilimda', 'AMC-W36'),
    ('ea000000-0000-0000-0000-000000000037'::uuid, '37', 'Maninagar', 'AMC-W37'),
    ('ea000000-0000-0000-0000-000000000038'::uuid, '38', 'Gomtipur', 'AMC-W38'),
    ('ea000000-0000-0000-0000-000000000039'::uuid, '39', 'Amraiwadi', 'AMC-W39'),
    ('ea000000-0000-0000-0000-000000000040'::uuid, '40', 'Odhav', 'AMC-W40'),
    ('ea000000-0000-0000-0000-000000000041'::uuid, '41', 'Vastral', 'AMC-W41'),
    ('ea000000-0000-0000-0000-000000000042'::uuid, '42', 'Indrapuri', 'AMC-W42'),
    ('ea000000-0000-0000-0000-000000000043'::uuid, '43', 'Bhaipura - Hatkeshwar', 'AMC-W43'),
    ('ea000000-0000-0000-0000-000000000044'::uuid, '44', 'Khokhra', 'AMC-W44'),
    ('ea000000-0000-0000-0000-000000000045'::uuid, '45', 'Isanpur', 'AMC-W45'),
    ('ea000000-0000-0000-0000-000000000046'::uuid, '46', 'Lambha', 'AMC-W46'),
    ('ea000000-0000-0000-0000-000000000047'::uuid, '47', 'Vatva', 'AMC-W47'),
    ('ea000000-0000-0000-0000-000000000048'::uuid, '48', 'Ramol - Hathijan', 'AMC-W48')
) AS w_data(id, ward_number, ward_name, ward_code)
CROSS JOIN (SELECT id FROM cities WHERE name = 'Ahmedabad' AND state = 'Gujarat' LIMIT 1) ct
CROSS JOIN (SELECT id FROM civic_bodies WHERE name = 'Ahmedabad Municipal Corporation' AND state = 'Gujarat' LIMIT 1) cb
ON CONFLICT (city_id, ward_number) DO UPDATE
SET ward_name = EXCLUDED.ward_name,
    ward_code = EXCLUDED.ward_code,
    civic_body_id = EXCLUDED.civic_body_id,
    source = EXCLUDED.source,
    source_url = EXCLUDED.source_url,
    last_verified_at = EXCLUDED.last_verified_at,
    is_active = TRUE,
    updated_at = NOW();
