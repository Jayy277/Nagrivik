-- Nagrivic Category Domain: Seed Initial Civic Categories
-- Purpose: Populate standard reference categories with deterministic IDs and display order

INSERT INTO categories (id, name, slug, description, is_active, display_order, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Roads / Potholes', 'roads-potholes', 'Craters, road cave-ins, damaged asphalt, and resurfacing hazards.', TRUE, 1, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000002', 'Garbage', 'garbage', 'Overflowing community bins, illegal dumping, uncollected domestic waste.', TRUE, 2, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000003', 'Streetlights', 'streetlights', 'Non-functional lamps, broken light poles, dark accident-prone zones.', TRUE, 3, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000004', 'Water', 'water', 'Pipeline leaks, drinking water contamination, low pressure, supply disruption.', TRUE, 4, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000005', 'Drainage', 'drainage', 'Overflowing sewer manholes, stormwater drain blockages, street waterlogging.', TRUE, 5, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;
