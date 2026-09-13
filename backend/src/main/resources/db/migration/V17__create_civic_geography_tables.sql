-- V17: Create Civic Geography, Wards, Departments, and Mapping Tables

-- 1. Civic Bodies (Municipal Corporations, Municipalities, Nagar Panchayats)
CREATE TABLE civic_bodies (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    state VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    official_website VARCHAR(255) NULL,
    source VARCHAR(255) NULL,
    source_url VARCHAR(500) NULL,
    last_verified_at TIMESTAMPTZ NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_civic_bodies_state_city ON civic_bodies(state, city);
CREATE INDEX idx_civic_bodies_is_active ON civic_bodies(is_active);

-- 2. Cities
CREATE TABLE cities (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country_code VARCHAR(10) NOT NULL DEFAULT 'IN',
    civic_body_id UUID NULL REFERENCES civic_bodies(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cities_name_state UNIQUE (name, state)
);

CREATE INDEX idx_cities_civic_body ON cities(civic_body_id);
CREATE INDEX idx_cities_is_active ON cities(is_active);

-- 3. Wards (with PostGIS MultiPolygon boundary geometry)
CREATE TABLE wards (
    id UUID PRIMARY KEY,
    city_id UUID NOT NULL REFERENCES cities(id) ON DELETE RESTRICT,
    civic_body_id UUID NULL REFERENCES civic_bodies(id) ON DELETE SET NULL,
    ward_number VARCHAR(50) NULL,
    ward_name VARCHAR(255) NOT NULL,
    ward_code VARCHAR(50) NULL,
    boundary_geometry geometry(MultiPolygon, 4326) NULL,
    source VARCHAR(255) NULL,
    source_url VARCHAR(500) NULL,
    last_verified_at TIMESTAMPTZ NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wards_boundary_geom ON wards USING GIST(boundary_geometry);
CREATE INDEX idx_wards_city ON wards(city_id);
CREATE INDEX idx_wards_civic_body ON wards(civic_body_id);
CREATE INDEX idx_wards_is_active ON wards(is_active);

-- 4. Departments
CREATE TABLE departments (
    id UUID PRIMARY KEY,
    civic_body_id UUID NOT NULL REFERENCES civic_bodies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_departments_civic_body_name UNIQUE (civic_body_id, name)
);

CREATE INDEX idx_departments_civic_body ON departments(civic_body_id);
CREATE INDEX idx_departments_is_active ON departments(is_active);

-- 5. Category -> Department Mappings
CREATE TABLE category_department_mappings (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cat_dept UNIQUE (category_id, department_id)
);

CREATE INDEX idx_cat_dept_category ON category_department_mappings(category_id);
CREATE INDEX idx_cat_dept_department ON category_department_mappings(department_id);

-- 6. Ward -> Department Mappings (Ward-specific department service coverage)
CREATE TABLE ward_department_mappings (
    id UUID PRIMARY KEY,
    ward_id UUID NOT NULL REFERENCES wards(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ward_dept UNIQUE (ward_id, department_id)
);

CREATE INDEX idx_ward_dept_ward ON ward_department_mappings(ward_id);
CREATE INDEX idx_ward_dept_department ON ward_department_mappings(department_id);
