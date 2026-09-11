-- Nagrivic Issue Domain: Add Category Relationship
-- Purpose: Associate every reported issue with a civic category, preserving referential integrity

ALTER TABLE issues ADD COLUMN category_id UUID;

-- Safe backfill for any existing development/test issues
UPDATE issues
SET category_id = 'c0000000-0000-0000-0000-000000000001'
WHERE category_id IS NULL;

ALTER TABLE issues ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE issues
ADD CONSTRAINT fk_issues_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT;

CREATE INDEX idx_issues_category_id ON issues(category_id);
