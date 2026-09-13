-- V20: Create issue_activities table for unified Issue Activity Timeline & Audit Foundation
-- Purpose: Provide an append-only, chronological audit history of meaningful domain events.

CREATE TABLE issue_activities (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_user_id UUID NULL,
    event_data JSONB NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_issue_activities_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_activities_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_issue_activities_issue_id ON issue_activities(issue_id);
CREATE INDEX idx_issue_activities_issue_order ON issue_activities(issue_id, created_at DESC, id DESC);
CREATE INDEX idx_issue_activities_event_type ON issue_activities(issue_id, event_type);
