-- Nagrivic Issue Domain: Add Location Relationship
-- Purpose: Require every reported civic issue to have an authoritative geographic location

ALTER TABLE issues ADD COLUMN location_id UUID;

-- Safe backfill for any existing development/test issues
INSERT INTO locations (id, location_point, accuracy_meters, created_at, updated_at)
SELECT 
    '00000000-0000-0000-0000-000000000001',
    ST_SetSRID(ST_MakePoint(72.5714, 23.0225), 4326),
    5.0,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM issues WHERE location_id IS NULL)
ON CONFLICT (id) DO NOTHING;

UPDATE issues
SET location_id = '00000000-0000-0000-0000-000000000001'
WHERE location_id IS NULL;

ALTER TABLE issues ALTER COLUMN location_id SET NOT NULL;

ALTER TABLE issues
ADD CONSTRAINT fk_issues_location_id FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE RESTRICT;

CREATE INDEX idx_issues_location_id ON issues(location_id);
