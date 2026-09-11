-- Nagrivic Media Domain: Create Media Table
-- Purpose: Authoritative metadata model for issue visual evidence attachments

CREATE TABLE media (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(128) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    media_type VARCHAR(32) NOT NULL DEFAULT 'IMAGE',
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_media_issue_id FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT uq_media_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_media_file_size_positive CHECK (file_size_bytes >= 0),
    CONSTRAINT chk_media_display_order_non_negative CHECK (display_order >= 0)
);

CREATE INDEX idx_media_issue_id ON media(issue_id);
CREATE INDEX idx_media_issue_display_order ON media(issue_id, display_order ASC);
