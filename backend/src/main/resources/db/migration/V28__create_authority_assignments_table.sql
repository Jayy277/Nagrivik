-- V28: Create Authority Assignments Table for Scoped Municipal Officers
-- Scopes authority users by Civic Body, City, Ward, Department, or Ward + Department

CREATE TABLE IF NOT EXISTS authority_assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    civic_body_id UUID NULL REFERENCES civic_bodies(id) ON DELETE CASCADE,
    city_id UUID NULL REFERENCES cities(id) ON DELETE SET NULL,
    ward_id UUID NULL REFERENCES wards(id) ON DELETE SET NULL,
    department_id UUID NULL REFERENCES departments(id) ON DELETE CASCADE,
    designation VARCHAR(100) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_authority_assignments_user ON authority_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_authority_assignments_civic_body ON authority_assignments(civic_body_id);
CREATE INDEX IF NOT EXISTS idx_authority_assignments_city ON authority_assignments(city_id);
CREATE INDEX IF NOT EXISTS idx_authority_assignments_ward ON authority_assignments(ward_id);
CREATE INDEX IF NOT EXISTS idx_authority_assignments_dept ON authority_assignments(department_id);
CREATE INDEX IF NOT EXISTS idx_authority_assignments_active ON authority_assignments(is_active);
