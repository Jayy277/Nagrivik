-- V19: Create Issue Priorities and Civic Impact Foundation

-- 1. Add severity, public impact, and safety impact signal fields to issues
ALTER TABLE issues
    ADD COLUMN severity VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN public_impact VARCHAR(30) NOT NULL DEFAULT 'LOW',
    ADD COLUMN safety_impact VARCHAR(30) NOT NULL DEFAULT 'LOW';

ALTER TABLE issues
    ADD CONSTRAINT chk_issues_severity
    CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

ALTER TABLE issues
    ADD CONSTRAINT chk_issues_public_impact
    CHECK (public_impact IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE issues
    ADD CONSTRAINT chk_issues_safety_impact
    CHECK (safety_impact IN ('NONE', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

-- 2. Create issue_priorities table for structured, explainable priority calculation
CREATE TABLE issue_priorities (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL UNIQUE REFERENCES issues(id) ON DELETE CASCADE,
    priority_level VARCHAR(30) NOT NULL,
    score INT NOT NULL,
    severity_score INT NOT NULL,
    impact_score INT NOT NULL,
    safety_score INT NOT NULL,
    age_score INT NOT NULL,
    support_score INT NOT NULL,
    calculation_version VARCHAR(50) NOT NULL DEFAULT 'v1',
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_priorities_level
        CHECK (priority_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_priorities_score
        CHECK (score >= 0 AND score <= 100),
    CONSTRAINT chk_priorities_severity_score
        CHECK (severity_score >= 0 AND severity_score <= 30),
    CONSTRAINT chk_priorities_impact_score
        CHECK (impact_score >= 0 AND impact_score <= 25),
    CONSTRAINT chk_priorities_safety_score
        CHECK (safety_score >= 0 AND safety_score <= 25),
    CONSTRAINT chk_priorities_age_score
        CHECK (age_score >= 0 AND age_score <= 10),
    CONSTRAINT chk_priorities_support_score
        CHECK (support_score >= 0 AND support_score <= 10)
);

CREATE INDEX idx_issue_priorities_issue_id ON issue_priorities(issue_id);
CREATE INDEX idx_issue_priorities_priority_level ON issue_priorities(priority_level);
CREATE INDEX idx_issue_priorities_score ON issue_priorities(score DESC);
