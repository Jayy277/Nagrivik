-- V15: Add duplicate_of_issue_id to issues table for duplicate tracking and linking
ALTER TABLE issues
    ADD COLUMN duplicate_of_issue_id UUID NULL,
    ADD CONSTRAINT fk_issues_duplicate_of FOREIGN KEY (duplicate_of_issue_id) REFERENCES issues(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_issues_not_self_duplicate CHECK (duplicate_of_issue_id IS NULL OR duplicate_of_issue_id <> id);

CREATE INDEX idx_issues_duplicate_of_issue_id ON issues(duplicate_of_issue_id);
