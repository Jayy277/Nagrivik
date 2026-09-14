-- Task 44: Resolution Evidence table
-- Supporting auditable authority resolution evidence for civic issues

CREATE TABLE resolution_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    submitted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    evidence_type VARCHAR(60) NOT NULL CHECK (evidence_type IN ('COMPLETION_PHOTO', 'COMPLETION_NOTE', 'BEFORE_AFTER_PHOTO')),
    storage_key VARCHAR(512),
    original_filename VARCHAR(255),
    content_type VARCHAR(128),
    file_size_bytes BIGINT,
    note TEXT,
    captured_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_resolution_evidence_issue_id ON resolution_evidence(issue_id);
CREATE INDEX idx_resolution_evidence_created_at ON resolution_evidence(created_at DESC);
CREATE INDEX idx_resolution_evidence_submitted_by ON resolution_evidence(submitted_by);
