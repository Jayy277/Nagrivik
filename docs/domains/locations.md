# Location Domain & PostGIS Spatial Model

## 1. Domain Purpose

Nagrivic is a location-first civic platform. In citizen engagement and municipal accountability, every reported civic problem (e.g., potholes, overflowing waste bins, broken streetlights, water pipeline leaks) takes place at a specific physical location in the real world.

The **Location Domain** provides the authoritative spatial foundation for the Nagrivic platform. It owns geographic coordinate persistence, spatial indexing, coordinate validation, and geometric abstractions, enabling future spatial capabilities such as:
- Proximity-based duplicate issue detection (`ST_DWithin`)
- Administrative ward, zone, and municipality boundary containment (`ST_Contains`)
- Proximity-based issue discovery and civic feeds
- Civic authority routing and representative mapping

---

## 2. PostGIS Spatial Representation

### Single Authoritative Representation: `geometry(Point, 4326)`
In PostgreSQL, the `locations` table stores one single authoritative spatial representation:
```sql
location_point geometry(Point, 4326) NOT NULL
```

### Spatial Reference System (SRS): WGS 84 (SRID 4326)
- **EPSG / SRID 4326** represents the World Geodetic System 1984 (WGS 84).
- It is the global standard coordinate frame used by GPS satellites, modern smartphones, OpenStreetMap, and map service providers.

### Coordinate Order Convention
PostGIS adheres to the standard Cartesian / GIS coordinate ordering convention:
- **X axis**: Longitude (East/West, valid range: `[-180.0, +180.0]`)
- **Y axis**: Latitude (North/South, valid range: `[-90.0, +90.0]`)

In SQL:
```sql
ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
-- Output WKT: POINT(longitude latitude)
```

> [!IMPORTANT]
> **Never invert the coordinate order**:
> - **Correct**: `POINT(longitude latitude)` (e.g., `POINT(72.5714 23.0225)`)
> - **Incorrect**: `POINT(latitude longitude)`

---

## 3. Database Schema (`locations`)

Defined in Flyway migration `V7__create_locations_table.sql`:

```sql
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
```

### Spatial Indexing (GiST)
- Spatial queries cannot use traditional B-tree indexes.
- A **GiST (Generalized Search Tree)** index is created on `location_point`.
- GiST indexes index bounding boxes (R-tree style) to accelerate proximity detection, point-in-polygon checks, and spatial bounding box intersections.

---

## 4. Coordinate Validation Rules

Geographic coordinates and GPS metadata are validated at both the application boundary and the database constraint layer:

| Property | Rule | Failure Behavior |
|---|---|---|
| **Latitude** | `-90.0 <= latitude <= +90.0` | Throws `IllegalArgumentException` / DB Check constraint violation |
| **Longitude** | `-180.0 <= longitude <= +180.0` | Throws `IllegalArgumentException` / DB Check constraint violation |
| **Accuracy (Meters)** | Optional, must be `>= 0.0` if specified | Throws `IllegalArgumentException` / DB Check constraint violation |

---

## 5. Issue → Location Relationship

An Issue references its geographic location via a mandatory foreign key relationship:

```
User (reporter)
  └── Issue
        ├── Category (category_id)
        └── Location (location_id)
```

In Flyway migration `V8__add_location_to_issues.sql`:
```sql
ALTER TABLE issues ADD COLUMN location_id UUID;
-- Backfill existing dev issues if present
UPDATE issues SET location_id = ... WHERE location_id IS NULL;
ALTER TABLE issues ALTER COLUMN location_id SET NOT NULL;
ALTER TABLE issues ADD CONSTRAINT fk_issues_location_id FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE RESTRICT;
CREATE INDEX idx_issues_location_id ON issues(location_id);
```

### Referential Integrity (`ON DELETE RESTRICT`)
A location tied to a reported civic issue cannot be deleted while the issue exists. This guarantees that every civic issue in Nagrivic permanently retains its geographic anchor.

---

## 6. Privacy & Security Principles

Location data is sensitive. Nagrivic applies strict architectural privacy boundaries:
1. **Civic Problem Location Only**: We record the location of the *reported public problem*, never continuous user movement, background GPS trails, or user location history.
2. **No Residential Address Collection**: Reporting an issue does not require entering a home address or residential personal data.
3. **No Unnecessary Device Metadata**: We explicitly avoid capturing device identifiers, MAC addresses, cell tower IDs, or Wi-Fi SSIDs.
4. **Data Minimization in APIs**: Application DTOs (`LocationResponse`) expose only `latitude`, `longitude`, and `accuracyMeters`, avoiding raw internal binary geometry leaks.

---

## 7. Deferred Geographic Features (Future Tasks)

To maintain clean modular boundaries, the following features are intentionally out of scope for Task 7 and deferred to future tasks:
- Administrative polygon boundaries (`wards`, `zones`, `municipalities`, `constituencies`)
- Reverse geocoding (converting coordinates to human-readable street addresses)
- Map tile rendering and UI integrations (Mapbox, Google Maps, Mappls)
- Proximity discovery endpoints (`GET /api/issues/nearby`)
- Mobile GPS permission flows and device hardware location hooks
- Proximity-based duplicate detection algorithms
