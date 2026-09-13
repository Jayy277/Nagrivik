-- V24: Public Issue Discovery and Search Optimization
-- Purpose: Add PostgreSQL Full-Text Search tsvector, GIN index, and discovery filtering indexes

-- 1. Add generated search_vector column to issues
ALTER TABLE issues ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, ''))
    ) STORED;

-- 2. GIN index for high-performance PostgreSQL full-text search
CREATE INDEX idx_issues_search_vector ON issues USING GIN (search_vector);

-- 3. Composite index for default public discovery filtering (non-hidden, non-duplicates, sorted by createdAt)
CREATE INDEX idx_issues_discovery_filtering ON issues (moderation_status, duplicate_of_issue_id, created_at DESC);
