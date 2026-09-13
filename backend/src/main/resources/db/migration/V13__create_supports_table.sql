-- Migration V13: Create supports table for citizen issue support system
CREATE TABLE supports (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_supports_issue_id FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_supports_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_supports_issue_user UNIQUE (issue_id, user_id)
);

CREATE INDEX idx_supports_issue_id ON supports(issue_id);
CREATE INDEX idx_supports_user_id ON supports(user_id);
