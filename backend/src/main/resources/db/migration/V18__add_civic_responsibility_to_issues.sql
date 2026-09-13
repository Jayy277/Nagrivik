-- V18: Add Civic Responsibility Enrichment fields to issues

ALTER TABLE issues
    ADD COLUMN civic_body_id UUID NULL REFERENCES civic_bodies(id) ON DELETE SET NULL,
    ADD COLUMN city_id UUID NULL REFERENCES cities(id) ON DELETE SET NULL,
    ADD COLUMN ward_id UUID NULL REFERENCES wards(id) ON DELETE SET NULL,
    ADD COLUMN department_id UUID NULL REFERENCES departments(id) ON DELETE SET NULL,
    ADD COLUMN responsibility_status VARCHAR(30) NOT NULL DEFAULT 'UNRESOLVED',
    ADD COLUMN responsibility_resolved_at TIMESTAMPTZ NULL,
    ADD COLUMN responsibility_source VARCHAR(255) NULL;

ALTER TABLE issues
    ADD CONSTRAINT chk_issues_responsibility_status
    CHECK (responsibility_status IN ('UNRESOLVED', 'RESOLVED'));

CREATE INDEX idx_issues_civic_body ON issues(civic_body_id);
CREATE INDEX idx_issues_city ON issues(city_id);
CREATE INDEX idx_issues_ward ON issues(ward_id);
CREATE INDEX idx_issues_department ON issues(department_id);
CREATE INDEX idx_issues_responsibility_status ON issues(responsibility_status);
