-- Nagrivic Location Domain: Create Locations Table
-- Purpose: Authoritative spatial model for civic problem locations using PostGIS geometry(Point, 4326)

CREATE TABLE locations (
    id UUID PRIMARY KEY,
    location_point geometry(Point, 4326) NOT NULL,
    accuracy_meters NUMERIC(6, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_locations_latitude CHECK (ST_Y(location_point) >= -90.0 AND ST_Y(location_point) <= 90.0),
    CONSTRAINT chk_locations_longitude CHECK (ST_X(location_point) >= -180.0 AND ST_X(location_point) <= 180.0),
    CONSTRAINT chk_locations_accuracy CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0.0)
);

CREATE INDEX idx_locations_location_point_gist ON locations USING GIST (location_point);
