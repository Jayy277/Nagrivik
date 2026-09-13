-- V21: Add moderation columns to issues, comments, and users
-- Purpose: Support content safety, soft-hiding, and user restrictions without destroying audit history.

-- 1. Issues moderation status
ALTER TABLE issues ADD COLUMN moderation_status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE';
CREATE INDEX idx_issues_moderation_status ON issues(moderation_status);

-- 2. Comments moderation status
ALTER TABLE comments ADD COLUMN moderation_status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE';
CREATE INDEX idx_comments_moderation_status ON comments(moderation_status);

-- 3. Users moderation restriction fields
ALTER TABLE users ADD COLUMN moderation_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN restricted_until TIMESTAMPTZ NULL;
ALTER TABLE users ADD COLUMN restriction_reason VARCHAR(500) NULL;
CREATE INDEX idx_users_moderation_status ON users(moderation_status);
