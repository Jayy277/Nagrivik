# Nagrivic Database Conventions & Infrastructure

> **Notice**: This document outlines PostgreSQL and PostGIS data modeling conventions, infrastructure configurations, and migration standards for Nagrivic. Business tables (`users`, `issues`, `categories`, `departments`, etc.) will be introduced incrementally in future tasks.

---

## 1. Database Technologies

- **Database Engine**: PostgreSQL 15+ (PostgreSQL 16 in Docker Compose)
- **Spatial Extension**: PostGIS 3.3+ (PostGIS 3.4 in Docker Compose)
- **Coordinate Reference System (CRS)**: WGS 84 (EPSG: 4326)
- **Migration Management**: Flyway
- **ORM / Persistence**: Spring Data JPA / Hibernate (with DDL auto-generation disabled)

---

## 2. Local Development Database (Docker Compose)

The local development database is defined in `docker-compose.yml` at the repository root using the official PostGIS image:

```yaml
services:
  db:
    image: postgis/postgis:16-3.4
    container_name: nagrivic-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-nagrivic}
      POSTGRES_USER: ${DB_USERNAME:-nagrivic}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-nagrivic_dev_secret}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - nagrivic_pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-nagrivic} -d ${DB_NAME:-nagrivic}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

volumes:
  nagrivic_pgdata:
    name: nagrivic_pgdata
```

### Local Database Management Commands

| Action | Command |
|---|---|
| **Start Database** | `docker compose up -d` |
| **Stop Database** | `docker compose down` |
| **Check Database Status** | `docker compose ps` |
| **View Database Logs** | `docker compose logs -f db` |
| **Connect via psql** | `docker compose exec -it db psql -U nagrivic -d nagrivic` |

---

## 3. Environment Variable Configuration

All database connection parameters are injected via environment variables. Real production secrets must never be committed to Git.

| Variable | Default (Local Dev) | Description |
|---|---|---|
| `DB_HOST` | `localhost` | Database host address. |
| `DB_PORT` | `5432` | Database port number. |
| `DB_NAME` | `nagrivic` | PostgreSQL database name. |
| `DB_USERNAME` | `nagrivic` | Database username. |
| `DB_PASSWORD` | `nagrivic_dev_secret` | Database user password (local-only default). |
| `DB_URL` | `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}` | JDBC connection URL. |
| `FLYWAY_ENABLED`| `true` | Enables or disables automated Flyway migrations on startup. |
| `JPA_DDL_AUTO` | `validate` | Hibernate DDL validation mode (`validate` or `none`). |

---

## 4. Migration Strategy (Flyway)

Nagrivic uses **Flyway** for deterministic, repeatable database schema versioning:

- **Location**: `backend/src/main/resources/db/migration`
- **Naming Convention**: `V<version>__<description>.sql` (e.g., `V1__enable_postgis.sql`, `V2__create_users_table.sql`).
- **Initial Migration (`V1__enable_postgis.sql`)**:
  ```sql
  CREATE EXTENSION IF NOT EXISTS postgis;
  ```
  *Note: The initial migration exclusively enables the PostGIS extension. Zero business tables are created in Task 3.*
- **Schema Management Rules**:
  1. Hibernate `ddl-auto` is set to `validate` (or `none`). Hibernate must **never** create or alter database tables automatically.
  2. All DDL changes must be authored as immutable, forward-only Flyway SQL scripts.
  3. Once applied, migration files must never be edited. Changes require a new versioned script.

---

## 5. Naming Conventions

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

## 6. Spatial Data Strategy (PostGIS)

Nagrivic uses **PostGIS** with the WGS 84 geographic coordinate reference system (EPSG: 4326):

### 1. Point Locations (`locations` table)
- Civic problem points are stored in the dedicated `locations` table:
  ```sql
  location_point geometry(Point, 4326) NOT NULL
  ```
- **Coordinate Order**: Strictly `POINT(longitude latitude)` (X=longitude `[-180.0, +180.0]`, Y=latitude `[-90.0, +90.0]`).
- **Single Source of Truth**: Coordinates are stored once in `locations.location_point`. Issues reference `locations.id` via `location_id UUID REFERENCES locations(id) ON DELETE RESTRICT`.
- **Proximity Calculations**: Future spatial distance queries can use `ST_DWithin(location_point::geography, target_point::geography, radius_meters)`.

### 2. Boundary Polygons (Wards, Zones, Jurisdictions - Future Scope)
- Use `geometry(Polygon, 4326)` or `geometry(MultiPolygon, 4326)` for administrative boundaries:
  ```sql
  geom geometry(MultiPolygon, 4326) NOT NULL
  ```
- Boundary containment queries use `ST_Contains`:
  ```sql
  -- Find which ward contains a given issue point
  SELECT ward_id, ward_name FROM ward_boundaries
  WHERE ST_Contains(geom, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326));
  ```

### Spatial Indexing (Mandatory)
Every spatial column must have a GiST index to support sub-millisecond queries:
```sql
CREATE INDEX idx_locations_location_point_gist ON locations USING GIST (location_point);
```

---

## 7. Audit & Deletion Patterns

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

## 8. Binary Data & Media Storage Policy

- **No Binary Storage in PostgreSQL**: PostgreSQL must **never** store raw image bytes, video files, or BLOBs. Storing large binary objects degrades database caching, balloons WAL size, and slows backup/restore cycles.
- **Metadata in PostgreSQL**: The `media` table stores only lightweight file metadata (`storage_key`, `content_type`, `file_size_bytes`, `media_type`, `display_order`, `created_at`).
- **Object Storage for Assets**: Actual binary assets reside in private, S3-compatible object storage (e.g., Cloudflare R2, MinIO, or AWS S3).
- **Server-Controlled Storage Keys**: The `storage_key` is generated server-side using UUIDs and parent entity identifiers (`issues/{issueId}/{uuid}.jpg`). Clients are never permitted to supply or manipulate storage paths.
- **Privacy Boundary**: Media metadata records must not store unnecessary personally identifiable information (PII), residential addresses, or raw client EXIF GPS payloads.
