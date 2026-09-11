-- Nagrivic Category Domain: Create Categories Table
-- Purpose: Reference taxonomy for classifying civic issues (Roads/Potholes, Garbage, Streetlights, Water, Drainage)

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_categories_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_categories_slug_not_empty CHECK (LENGTH(TRIM(slug)) > 0)
);

CREATE INDEX idx_categories_is_active ON categories(is_active);
CREATE INDEX idx_categories_display_order ON categories(display_order);
