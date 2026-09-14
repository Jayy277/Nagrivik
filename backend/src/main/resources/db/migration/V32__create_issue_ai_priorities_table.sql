-- V32: Create Issue AI Priorities Table (Task 48)

CREATE TABLE issue_ai_priorities (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL UNIQUE REFERENCES issues(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    calculation_version VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    suggested_severity INT,
    suggested_impact INT,
    suggested_safety INT,
    confidence INT,
    severity_confidence INT,
    impact_confidence INT,
    safety_confidence INT,
    signals TEXT NOT NULL,
    failure_reason VARCHAR(500),
    applied_to_calculation BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,

    CONSTRAINT chk_issue_ai_priorities_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'UNAVAILABLE')),
    CONSTRAINT chk_issue_ai_priorities_severity
        CHECK (suggested_severity IS NULL OR (suggested_severity >= 0 AND suggested_severity <= 30)),
    CONSTRAINT chk_issue_ai_priorities_impact
        CHECK (suggested_impact IS NULL OR (suggested_impact >= 0 AND suggested_impact <= 25)),
    CONSTRAINT chk_issue_ai_priorities_safety
        CHECK (suggested_safety IS NULL OR (suggested_safety >= 0 AND suggested_safety <= 25)),
    CONSTRAINT chk_issue_ai_priorities_confidence
        CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 100))
);

CREATE INDEX idx_issue_ai_priorities_issue_id ON issue_ai_priorities(issue_id);
CREATE INDEX idx_issue_ai_priorities_status ON issue_ai_priorities(status);
CREATE INDEX idx_issue_ai_priorities_created_at ON issue_ai_priorities(created_at DESC);
