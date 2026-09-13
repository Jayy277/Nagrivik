# Public Issue Discovery & Search API

## 1. Purpose & Philosophy

The Public Issue Discovery & Search API provides a high-performance, filterable, geographic, and full-text search capability across published civic issues in Nagrivic. It forms the foundational backend service for:
- Citizen mobile feeds (Home, Explore, Nearby issues)
- Public municipal dashboards & civic discovery portals
- Interactive geographic civic maps
- Public issue search and categorization

Civic transparency requires issues to be discoverable by geographic proximity, municipal boundaries, category, severity, status, and textual keywords while respecting privacy and moderation safeguards.

---

## 2. Core Search & Discovery Principles

### 2.1 Server-Authoritative Discovery
- Discovery results are queried dynamically against PostgreSQL using PostGIS spatial indexing (`ST_DWithin`, `ST_DistanceSphere`) and PostgreSQL Full-Text Search (`tsvector`, `to_tsquery`, GIN indexing).
- Support counts, comment counts, priority scores, and civic responsibility are joined efficiently server-side. Clients do not compute distance or rankings.

### 2.2 Privacy & Moderation Safeguards
- Only public-safe issue data is returned. Supporter identities, citizen phone numbers, and private internal notes are excluded.
- Issues hidden or removed by moderation (`MODERATED`, `SPAM`, `REMOVED`) are excluded from public discovery feeds.
- Anonymous reports are presented with author information anonymized.

### 2.3 Duplicate Canonical Transparency
- Issues marked as duplicates can be discovered or filtered, but canonical issues represent the unified civic effort with aggregated or independent community metrics.

---

## 3. Query Parameters & Filters

| Parameter | Type | Description |
|---|---|---|
| `q` | String | Full-text search keyword over issue title and description (prefix/partial matching supported). |
| `categoryId` | UUID | Filter by issue category (e.g. Roads, Garbage, Streetlights, Water, Drainage). |
| `status` | IssueStatus | Filter by current status (`REPORTED`, `VERIFIED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`, `CITIZEN_VERIFIED`, `NOT_FIXED`). |
| `priority` | PriorityLevel | Filter by calculated priority level (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). |
| `cityId` | UUID | Filter by city. |
| `wardId` | UUID | Filter by municipal ward. |
| `civicBodyId` | UUID | Filter by civic body / municipal corporation. |
| `departmentId` | UUID | Filter by assigned responsible municipal department. |
| `latitude` | Double | Latitude for radial / proximity search (-90 to 90). Requires `longitude`. |
| `longitude` | Double | Longitude for radial / proximity search (-180 to 180). Requires `latitude`. |
| `radiusMeters` | Double | Search radius around coordinates in meters (default: 5,000m / 5km). |
| `sort` | IssueDiscoverySort | Sort ordering: `NEWEST` (default), `OLDEST`, `PRIORITY`, `MOST_SUPPORTED`, `NEAREST` (requires coordinates). |
| `page` | Integer | Zero-based page number (default: 0). |
| `size` | Integer | Page size (default: 20, max: 100). |

---

## 4. Sorting Strategies

1. **`NEWEST`**: Orders by `issues.created_at DESC`. Default chronological feed.
2. **`OLDEST`**: Orders by `issues.created_at ASC`. Highlights long-standing, unresolved issues.
3. **`PRIORITY`**: Orders by `issue_priorities.priority_score DESC, issues.created_at DESC`. Highlights critical civic hazards first.
4. **`MOST_SUPPORTED`**: Orders by active support count `DESC`. Displays issues with the highest community corroboration.
5. **`NEAREST`**: Orders by spherical geographic distance in meters (`ST_DistanceSphere(locations.point, ST_SetSRID(ST_MakePoint(lon, lat), 4326)) ASC`). When `NEAREST` is used or coordinates are supplied, the API injects `distanceMeters` into each response item.

---

## 5. Relational Schema & Indexing

Flyway Migration `V24__add_issue_search_vector_and_discovery_indexes.sql` introduces:
- A `search_vector tsvector` column on `issues` generated from `title` and `description`.
- A trigger updating `search_vector` on insert/update of title or description.
- A GIN index on `issues(search_vector)` for fast full-text search.
- A composite discovery index `idx_issues_discovery ON issues(category_id, current_status, created_at DESC)`.
- Spatial GiST index on `locations(point)` for fast radial bounding and distance queries.

---

## 6. Endpoints

### `GET /api/v1/issues` (Public Discovery)
Returns a paginated `Page<IssueResponse>` matching the supplied discovery criteria.

Example Request:
```http
GET /api/v1/issues?categoryId=8f6a9c12-3b4e-4f5a-9a1b-123456789abc&latitude=23.0225&longitude=72.5714&radiusMeters=3000&sort=NEAREST&page=0&size=10
```

Example Response:
```json
{
  "content": [
    {
      "id": "b1a2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "title": "Deep pothole on Ashram Road near junction",
      "description": "Large pothole causing vehicle swerving and traffic backup.",
      "category": {
        "id": "8f6a9c12-3b4e-4f5a-9a1b-123456789abc",
        "name": "Roads / Potholes"
      },
      "currentStatus": "IN_PROGRESS",
      "priority": {
        "score": 68,
        "level": "HIGH"
      },
      "supportCount": 14,
      "commentCount": 3,
      "distanceMeters": 342.5,
      "location": {
        "latitude": 23.0248,
        "longitude": 72.5731,
        "address": "Ashram Road, Navrangpura"
      },
      "createdAt": "2026-09-10T14:20:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1
}
```
