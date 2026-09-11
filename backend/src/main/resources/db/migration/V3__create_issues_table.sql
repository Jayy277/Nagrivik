-- Nagrivic Issue Domain: Create Issues Table
-- Purpose: Foundational entity representing a citizen-reported civic issue

CREATE TABLE issues (
    id UUID PRIMARY KEY,
    reported_by UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'REPORTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_issues_reported_by FOREIGN KEY (reported_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_issues_title_not_empty CHECK (LENGTH(TRIM(title)) > 0)
);

CREATE INDEX idx_issues_reported_by ON issues(reported_by);
CREATE INDEX idx_issues_status ON issues(status);
CREATE INDEX idx_issues_created_at ON issues(created_at DESC);
