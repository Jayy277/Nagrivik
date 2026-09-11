# Category Domain Specification

## 1. Domain Purpose

The **Category** domain represents the civic problem taxonomy within Nagrivic. Every reported issue belongs to exactly one category, enabling:

- Intuitive mobile issue filing and filtering
- Automated routing to responsible municipal departments (e.g., Solid Waste Management, Roads, Streetlight, Water & Drainage)
- Category-specific service level agreements (SLAs)
- Aggregated civic problem analytics per neighborhood and ward

---

## 2. Initial Civic Categories

Nagrivic launches with five initial categories in Ahmedabad, Gujarat:

| Display Order | Category Name | Slug | Description |
|---|---|---|---|
| `1` | **Roads / Potholes** | `roads-potholes` | Craters, road cave-ins, damaged asphalt, and resurfacing hazards. |
| `2` | **Garbage** | `garbage` | Overflowing community bins, illegal dumping, uncollected domestic waste. |
| `3` | **Streetlights** | `streetlights` | Non-functional lamps, broken light poles, dark accident-prone zones. |
| `4` | **Water** | `water` | Pipeline leaks, drinking water contamination, low pressure, supply disruption. |
| `5` | **Drainage** | `drainage` | Overflowing sewer manholes, stormwater drain blockages, street waterlogging. |

### Display Order Justification
An explicit `display_order` integer guarantees that client applications (React Native mobile app and Next.js web portal) render categories in a consistent, intuitive priority sequence rather than an unpredictable database retrieval order.

---

## 3. Data Model & Field Definitions

| Field | Database Column | Type | Nullable | Description |
|---|---|---|---|---|
| `id` | `id` | `UUID` | No | Primary key identifier (deterministic UUIDv4 for system seeds). |
| `name` | `name` | `VARCHAR(100)` | No | Human-readable category display label (non-empty). |
| `slug` | `slug` | `VARCHAR(100)` | No | Machine-friendly, URL-safe identifier (unique, non-empty). |
| `description` | `description` | `TEXT` | Yes | Descriptive explanation of issues covered under this category. |
| `isActive` | `is_active` | `BOOLEAN` | No | Flag indicating if citizens can currently report new issues under this category (default: `TRUE`). |
| `displayOrder`| `display_order` | `INT` | No | Sort sequence for presentation in client user interfaces (default: `0`). |
| `createdAt` | `created_at` | `TIMESTAMPTZ` | No | Timestamp of category registration (UTC). |
| `updatedAt` | `updated_at` | `TIMESTAMPTZ` | No | Timestamp of last modification (UTC). |

---

## 4. Slug Strategy & URL Stability

- Slugs are lowercase, hyphenated strings: e.g., `roads-potholes`, `streetlights`.
- A `UNIQUE` database constraint prevents duplicate slugs.
- Slugs are permanent and immutable to ensure stable REST API filtering (`/api/issues?category=roads-potholes`) and SEO-friendly public web URLs (`/categories/roads-potholes`).

---

## 5. Active / Inactive Lifecycle

- Categories are **never hard-deleted** from the database if issues reference them.
- Setting `is_active = FALSE` soft-retires a category:
  - Existing historical issues remain linked and queryable.
  - New issue submissions under an inactive category are strictly rejected by `IssueService`.
- Active categories are queried via `findByIsActiveTrueOrderByDisplayOrderAsc()`.

---

## 6. Relationship with the Issue Domain

- **One-to-Many Relationship**: One Category contains many Issues. Each Issue references exactly one Category.
- **Foreign Key**: `issues.category_id REFERENCES categories(id) ON DELETE RESTRICT`.
- **Referential Integrity**: `ON DELETE RESTRICT` prevents accidental deletion of categories that have linked citizen reports.

---

## 7. Category Management Status

- In the current foundation phase, system categories are managed **strictly through versioned Flyway database migrations** (`V4__create_categories_table.sql` and `V5__seed_initial_categories.sql`).
- Public category management endpoints and admin CRUD interfaces are **intentionally not implemented** in this task and will be designed in a future administrative task.
