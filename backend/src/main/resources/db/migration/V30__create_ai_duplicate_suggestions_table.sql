-- V30: Create ai_duplicate_suggestions table for administrative AI duplicate suggestion review
CREATE TABLE ai_duplicate_suggestions (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL,
    candidate_issue_id UUID NOT NULL,
    score INT NOT NULL CHECK (score >= 0 AND score <= 100),
    confidence VARCHAR(20) NOT NULL,
    signals JSONB NOT NULL DEFAULT '[]',
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    calculation_version VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUGGESTED',
    dismiss_reason VARCHAR(255) NULL,
    reviewed_by UUID NULL,
    reviewed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ai_dup_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_dup_candidate FOREIGN KEY (candidate_issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_dup_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_ai_dup_not_self CHECK (issue_id <> candidate_issue_id),
    CONSTRAINT uq_ai_dup_issue_candidate UNIQUE (issue_id, candidate_issue_id)
);

CREATE INDEX idx_ai_dup_issue_id ON ai_duplicate_suggestions(issue_id);
CREATE INDEX idx_ai_dup_candidate_id ON ai_duplicate_suggestions(candidate_issue_id);
CREATE INDEX idx_ai_dup_status ON ai_duplicate_suggestions(status);
CREATE INDEX idx_ai_dup_score ON ai_duplicate_suggestions(score DESC);
