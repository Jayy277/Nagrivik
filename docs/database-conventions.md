# Nagrivic Database Conventions

> **Notice**: This document outlines PostgreSQL and PostGIS data modeling conventions and standards for Nagrivic. The schema definitions shown here are illustrative specifications and must not be implemented until their respective database implementation phase.

---

## 1. Database Technologies

- **Database Engine**: PostgreSQL 15+
- **Spatial Extension**: PostGIS 3.3+
- **Coordinate Reference System (CRS)**: WGS 84 (EPSG: 4326)

---

## 2. Naming Conventions

| Element | Convention | Example | Rationale |
|---|---|---|---|
| **Tables** | Plural `snake_case` | `issues`, `users`, `categories`, `ward_boundaries` | Standard SQL convention; maps cleanly to collections. |
| **Columns** | Lowercase `snake_case` | `first_name`, `phone_number`, `created_at` | Consistency with PostgreSQL case-insensitivity. |
| **Primary Keys** | `id` | `id UUID PRIMARY KEY` | Standard, concise entity identifier. |
| **Foreign Keys** | `<singular_parent_table>_id` | `issue_id`, `category_id`, `author_id` | Immediate clarity of relationship target. |
| **Indexes (B-Tree)** | `idx_<table>_<columns>` | `idx_issues_status`, `idx_issues_created_at` | Easy identification during query plan analysis. |
| **Spatial Indexes (GiST)**| `idx_<table>_<column>_gist` | `idx_issues_location_gist`, `idx_wards_geom_gist` | Explicit demarcation of spatial index type. |
| **Unique Constraints** | `uq_<table>_<columns>` | `uq_supports_issue_user`, `uq_users_phone` | Enforces uniqueness invariants. |
| **Foreign Key Constraints**| `fk_<table>_<parent_table>` | `fk_issues_categories`, `fk_comments_issues` | Traceable constraint violation errors. |

---

## 3. Data Types & Column Standards

### Primary Keys
- Use `UUID` (UUIDv4) as primary keys for application entities (`issues`, `users`, `comments`, `media_attachments`).
- Rationale: Non-enumerable, globally unique, safe for client-side optimistic ID generation, prevents sequential scraping.

### Timestamps
- Use `TIMESTAMPTZ` (Timestamp with Time Zone) for all point-in-time fields.
- Store all timestamps in UTC (`DEFAULT NOW()` or `CURRENT_TIMESTAMP`).
- Standard audit timestamps on every mutable table:
  ```sql
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  ```

### Strings & Text
- Use `VARCHAR(n)` where a strict physical limit exists (e.g., `phone_number VARCHAR(15)`, `postal_code VARCHAR(10)`).
- Use `TEXT` for freeform content (e.g., `description`, `comment_text`) rather than arbitrary `VARCHAR(255)` limits.

### Status & Enumerations
- Store status as `VARCHAR(32)` representing uppercase enum keys (e.g., `'SUBMITTED'`, `'IN_PROGRESS'`, `'RESOLVED'`).
- Avoid PostgreSQL native `CREATE TYPE ... AS ENUM` in early phases because altering existing native enum values requires complex DDL migrations.

---

## 4. PostGIS Spatial Column Standards

Nagrivic handles two distinct geographic representations:

### 1. Point Locations (Issue Reports)
- Use `geography(Point, 4326)` for citizen issue coordinates:
  ```sql
  location geography(Point, 4326) NOT NULL
  ```
- **Why Geography?** Calculations on `geography` automatically compute true great-circle surface distances in meters (e.g., `ST_DWithin(location, target_point, 15)` measures exactly 15 meters) without requiring projection transformations.

### 2. Boundary Polygons (Wards, Zones, Jurisdictions)
- Use `geometry(Polygon, 4326)` or `geometry(MultiPolygon, 4326)` for administrative boundaries:
  ```sql
  geom geometry(MultiPolygon, 4326) NOT NULL
  ```
- Boundary containment queries use `ST_Contains`:
  ```sql
  -- Find which ward contains a given point
  SELECT ward_id, ward_name FROM ward_boundaries
  WHERE ST_Contains(geom, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326));
  ```

### Spatial Indexing (Mandatory)
Every spatial column must have a GiST index to support sub-millisecond queries:
```sql
CREATE INDEX idx_issues_location_gist ON issues USING GIST (location);
CREATE INDEX idx_ward_boundaries_geom_gist ON ward_boundaries USING GIST (geom);
```

---

## 5. Audit & Deletion Patterns

### Audit Columns
Entities that undergo administrative moderation or user edits should include:
```sql
created_by UUID NULL REFERENCES users(id),
updated_by UUID NULL REFERENCES users(id)
```

### Soft Deletion
- Soft deletion is used **only where legally or operationally justified** (e.g., moderation takedown of abusive issues or user account deactivation where historical audit logs must be preserved).
- Use a nullable timestamp:
  ```sql
  deleted_at TIMESTAMPTZ NULL
  ```
- High-frequency citizen engagement tables (e.g., `supports`) use hard deletion (`DELETE FROM supports WHERE ...`) to keep tables lean and indexes small.

---

## 6. Migration & Evolution Standards

- Schema changes will be managed via **Flyway** migration scripts (`V1__...sql`, `V2__...sql`) in future database tasks.
- Migrations must be backward-compatible with running backend instances.
- Never write destructive migrations (e.g., dropping columns) in the same release that deprecates application usage.
