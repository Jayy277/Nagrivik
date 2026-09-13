# Departments & Responsible Authority Mapping Architecture

## 1. Domain Purpose

A municipal civic body delivers services through specialized operational departments (e.g., Roads & Buildings, Solid Waste Management, Water Supply & Drainage, Street Lighting).

When an issue is reported under a specific category (such as `"Roads / Potholes"`) in a given ward, Nagrivic derives the responsible department through a multi-tier mapping hierarchy:

$$\text{Category} + \text{Civic Body} + \text{Ward} \longrightarrow \text{Responsible Department}$$

### Invariant Principles
1. **Never Trust Client Department Claims**: The client (citizen mobile app or web) is never allowed to specify or override the responsible department. The assignment is 100% server-derived.
2. **Civic-Body Scoping**: Departments belong to a specific civic body (`departments.civic_body_id`), because departmental naming, structure, and operational duties differ across municipalities.
3. **Decoupled Category Mapping**: Categories do not have a hardcoded `department_id` column. Instead, `category_department_mappings` maps categories to departments per civic body.
4. **Ward-Level Specialization**: Where specific operational crews or zones service particular wards, `ward_department_mappings` refines the assignment.

---

## 2. Database Schema

### 2.1 Departments (`departments`)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` | Globally unique identifier (UUIDv4). |
| `civic_body_id`| `UUID` | `FK -> civic_bodies(id) ON DELETE CASCADE` | Governing civic body. |
| `name` | `VARCHAR(255)` | `NOT NULL` | Official department name (e.g., "Roads & Buildings"). |
| `code` | `VARCHAR(50)` | `NULL` | Department code (e.g., "R&B"). |
| `description` | `TEXT` | `NULL` | Scope of operational duties. |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Active operational status. |

*Unique Constraint*: `CONSTRAINT uq_departments_civic_body_name UNIQUE (civic_body_id, name)`.

### 2.2 Category-Department Mappings (`category_department_mappings`)
Maps standard platform grievance categories to responsible municipal departments.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` | Globally unique identifier (UUIDv4). |
| `category_id` | `UUID` | `FK -> categories(id) ON DELETE CASCADE` | Associated issue category. |
| `department_id`| `UUID` | `FK -> departments(id) ON DELETE CASCADE` | Responsible department. |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` | Creation timestamp. |

*Unique Constraint*: `CONSTRAINT uq_cat_dept UNIQUE (category_id, department_id)`.

### 2.3 Ward-Department Mappings (`ward_department_mappings`)
Defines ward-specific service areas for departments (e.g., zone-specific maintenance divisions).

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` | Globally unique identifier (UUIDv4). |
| `ward_id` | `UUID` | `FK -> wards(id) ON DELETE CASCADE` | Associated ward. |
| `department_id`| `UUID` | `FK -> departments(id) ON DELETE CASCADE` | Department servicing this ward. |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Active status flag. |

*Unique Constraint*: `CONSTRAINT uq_ward_dept UNIQUE (ward_id, department_id)`.

---

## 3. Resolution Algorithm (`DepartmentResolverService`)

The responsible department is resolved using the following order of precedence:

1. **Category Mapping Lookup**: Retrieve all departments mapped to the given `categoryId`.
2. **Civic Body Filter**: Retain only departments belonging to the resolved `civicBodyId` with `isActive = true`.
3. **Ward-Level Refinement**:
   - If the ward has active `ward_department_mappings`, filter candidate departments to those specifically assigned to that ward.
   - If a matching ward-specific department is found, return it.
4. **Civic-Body Level Fallback**: If no ward-specific restriction exists, return the primary department for the category in that civic body.
5. **Unmapped Graceful Degradation**: If no mapping exists, return `Optional.empty()` (`department: null` in public response). Issue reporting continues uninterrupted.
