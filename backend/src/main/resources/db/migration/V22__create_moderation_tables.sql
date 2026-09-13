-- V22: Create moderation_reports and moderation_actions tables
-- Purpose: User reporting workflow and append-only internal moderation audit log.

CREATE TABLE moderation_reports (
    id UUID PRIMARY KEY,
    reporter_user_id UUID NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(64) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ NULL,
    resolved_by_user_id UUID NULL,
    CONSTRAINT fk_moderation_reports_reporter FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_reports_resolver FOREIGN KEY (resolved_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_moderation_reports_target ON moderation_reports(target_type, target_id);
CREATE INDEX idx_moderation_reports_reporter ON moderation_reports(reporter_user_id);
CREATE INDEX idx_moderation_reports_status ON moderation_reports(status);

CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY,
    report_id UUID NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id UUID NOT NULL,
    moderator_user_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_moderation_actions_report FOREIGN KEY (report_id) REFERENCES moderation_reports(id) ON DELETE SET NULL,
    CONSTRAINT fk_moderation_actions_moderator FOREIGN KEY (moderator_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_moderation_actions_target ON moderation_actions(target_type, target_id);
CREATE INDEX idx_moderation_actions_moderator ON moderation_actions(moderator_user_id);
CREATE INDEX idx_moderation_actions_created ON moderation_actions(created_at DESC);
