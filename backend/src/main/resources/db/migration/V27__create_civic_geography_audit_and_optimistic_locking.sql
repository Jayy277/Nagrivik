-- V27: Civic Geography Audit Trail and Concurrency Protection

-- 1. Add version column to civic geography and responsibility tables for optimistic locking
ALTER TABLE civic_bodies ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE cities ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cities ADD COLUMN IF NOT EXISTS source VARCHAR(255) NULL;
ALTER TABLE cities ADD COLUMN IF NOT EXISTS source_url VARCHAR(500) NULL;
ALTER TABLE cities ADD COLUMN IF NOT EXISTS last_verified_at TIMESTAMPTZ NULL;

ALTER TABLE wards ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE departments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE departments ADD COLUMN IF NOT EXISTS source VARCHAR(255) NULL;
ALTER TABLE departments ADD COLUMN IF NOT EXISTS source_url VARCHAR(500) NULL;
ALTER TABLE departments ADD COLUMN IF NOT EXISTS last_verified_at TIMESTAMPTZ NULL;

ALTER TABLE category_department_mappings ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE category_department_mappings ADD COLUMN IF NOT EXISTS source VARCHAR(255) NULL;
ALTER TABLE category_department_mappings ADD COLUMN IF NOT EXISTS source_url VARCHAR(500) NULL;
ALTER TABLE category_department_mappings ADD COLUMN IF NOT EXISTS last_verified_at TIMESTAMPTZ NULL;
ALTER TABLE category_department_mappings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE category_department_mappings ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE ward_department_mappings ADD COLUMN IF NOT EXISTS source VARCHAR(255) NULL;
ALTER TABLE ward_department_mappings ADD COLUMN IF NOT EXISTS source_url VARCHAR(500) NULL;
ALTER TABLE ward_department_mappings ADD COLUMN IF NOT EXISTS last_verified_at TIMESTAMPTZ NULL;
ALTER TABLE ward_department_mappings ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE ward_department_mappings ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Append-only Civic Geography Audit Trail Table
CREATE TABLE IF NOT EXISTS civic_geography_audits (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_state TEXT NULL,
    new_state TEXT NULL,
    reason VARCHAR(500) NULL,
    source VARCHAR(255) NULL,
    source_url VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_civic_geo_audits_entity ON civic_geography_audits(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_civic_geo_audits_actor ON civic_geography_audits(actor_id);
CREATE INDEX IF NOT EXISTS idx_civic_geo_audits_created ON civic_geography_audits(created_at);
