-- V16: Add version to issues for optimistic locking and create status_history table

ALTER TABLE issues
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE status_history (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    changed_by_user_id UUID NULL,
    reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_status_history_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_status_history_reason_length CHECK (reason IS NULL OR LENGTH(reason) <= 1000)
);

CREATE INDEX idx_status_history_issue_id ON status_history(issue_id);
CREATE INDEX idx_status_history_created_at ON status_history(created_at);
CREATE INDEX idx_status_history_changed_by ON status_history(changed_by_user_id);
