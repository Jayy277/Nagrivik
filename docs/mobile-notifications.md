# Nagrivic Mobile Notification Center & Backend Integration (Task 34)

## 1. Overview & Architectural Philosophy

The Nagrivic Mobile Notification Center connects the citizen mobile client to the real Spring Boot notification module implemented in Task 23. It provides citizens with timely, actionable awareness signals regarding municipal inspection progress, status resolutions, civic responsibility assignments, priority escalations, and official activity on issues they reported or supported.

### Core Principles
1. **Real Backend Integration**: Pure production client with zero mock data. Directly interfaces with authenticated Spring Boot REST endpoints.
2. **Synchronized Unread State**: Immediate synchronization between the Notifications tab badge, screen header unread counter, and list read states via `NotificationContext`.
3. **Idempotent Actions with Optimistic UI**: Marking individual or all notifications as read updates local UI immediately, protected by rollback if the network request fails.
4. **Resilient Deep-Linking**: Tapping notifications routes directly to canonical `/issue/[id]` detail views while safely handling missing or deleted issues.
5. **Channel Preferences**: Citizens can configure delivery channel preferences (`inAppEnabled`, `pushEnabled`, `emailEnabled`, `smsEnabled`) stored securely on the server.
6. **No Background Polling or FCM**: Device push infrastructure (FCM/APNS) and background socket connections are intentionally deferred to dedicated push tasks.

---

## 2. API Contract & Endpoint Integration

All mobile notification API methods are centralized in `src/services/api/notificationApi.ts` and powered by `ApiClient` (`src/services/api/client.ts`). The citizen's identity is derived strictly from the authenticated JWT Bearer token on the server.

| Endpoint | Method | Purpose | Response Format |
|---|---|---|---|
| `/api/notifications` | `GET` | Paginated notification history (newest first) | `PagedResponse<AppNotification>` |
| `/api/notifications/unread-count` | `GET` | Total unread notification count | `UnreadCountResponse` (`{ count: number }`) |
| `/api/notifications/{id}/read` | `PATCH` | Marks an individual notification as read | `AppNotification` |
| `/api/notifications/read-all` | `PATCH` | Marks all notifications for user as read | `{ message: string }` |
| `/api/notifications/preferences` | `GET` | Retrieves citizen delivery preferences | `NotificationPreferenceResponse` |
| `/api/notifications/preferences` | `PATCH` | Updates citizen delivery preferences | `NotificationPreferenceResponse` |

### Authentication & Token Rotation
All notification endpoints require an authenticated session (`status === 'AUTHENTICATED'`). `ApiClient.patch` and `ApiClient.get` automatically attach `Authorization: Bearer <token>`. In the event of an access token expiration (HTTP 401), `ApiClient` automatically intercepts the failure, triggers token rotation via `POST /api/auth/refresh`, and transparently replays the request without disrupting the citizen experience.

---

## 3. Data Model & Types

Defined in `src/types/notification.ts`:

```typescript
export type NotificationType =
  | 'ISSUE_STATUS_CHANGED'
  | 'ISSUE_RESOLVED'
  | 'ISSUE_NOT_FIXED'
  | 'ISSUE_VERIFIED'
  | 'ISSUE_ACKNOWLEDGED'
  | 'ISSUE_IN_PROGRESS'
  | 'ISSUE_CITIZEN_VERIFIED'
  | 'ISSUE_RESPONSIBILITY_RESOLVED'
  | 'ISSUE_PRIORITY_CHANGED'
  | 'ISSUE_COMMENT_ACTIVITY'
  | 'ISSUE_DUPLICATE_DETECTED';

export interface AppNotification {
  id: string;
  type: NotificationType | string;
  title: string;
  body: string;
  issueId?: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationPreferenceResponse {
  inAppEnabled: boolean;
  pushEnabled: boolean;
  emailEnabled: boolean;
  smsEnabled: boolean;
}

export interface UpdateNotificationPreferenceRequest {
  inAppEnabled?: boolean;
  pushEnabled?: boolean;
  emailEnabled?: boolean;
  smsEnabled?: boolean;
}
```

---

## 4. Centralized Notification Icon & Palette Mapping

Each notification lifecycle event is mapped to a tailored civic icon and color scheme in `src/components/items/NotificationItem.tsx`:

| Notification Event | Icon | Foreground Color | Background Tint | Meaning |
|---|---|---|---|---|
| `ISSUE_STATUS_CHANGED` | `refresh` | `#1D4ED8` (Blue) | `#EFF6FF` | General municipal workflow transition |
| `ISSUE_RESOLVED` | `check-circle` | `#059669` (Emerald) | `#D1FAE5` | Official municipal work completion |
| `ISSUE_NOT_FIXED` | `alert-circle` | `#DC2626` (Red) | `#FEE2E2` | Inspection failure or incomplete fix |
| `ISSUE_VERIFIED` | `check-circle` | `#0284C7` (Sky) | `#E0F2FE` | Authority inspection verified issue |
| `ISSUE_ACKNOWLEDGED` | `clock` | `#D97706` (Amber) | `#FEF3C7` | Municipal intake & acknowledgement |
| `ISSUE_IN_PROGRESS` | `clock` | `#2563EB` (Blue) | `#DBEAFE` | Field work crew dispatched |
| `ISSUE_CITIZEN_VERIFIED`| `check` | `#16A34A` (Green) | `#DCFCE7` | Citizen verified resolution |
| `ISSUE_RESPONSIBILITY_RESOLVED` | `shield` | `#7C3AED` (Purple) | `#EDE9FE` | Jurisdiction/department assigned |
| `ISSUE_PRIORITY_CHANGED`| `zap` | `#EA580C` (Orange) | `#FFEDD5` | Escalated priority rating |
| `ISSUE_COMMENT_ACTIVITY`| `message-square`| `#6366F1` (Indigo) | `#EEF2FF` | New comment or community question |
| `ISSUE_DUPLICATE_DETECTED`| `info` | `#64748B` (Slate) | `#F1F5F9` | Merged duplicate report notification |
| *Unknown / Future types* | `bell` | Primary Theme | Primary Light | Safe fallback, zero crash guarantee |

---

## 5. UI Features & Interactions

### 5.1 Unread Count Badge on Tabs (`_layout.tsx`)
- Reads directly from `useNotification().unreadCount`.
- Formatted gracefully:
  - `0`: No badge (`tabBarBadge: undefined`)
  - `1–99`: Exact count (`1` to `99`)
  - `100+`: Capped badge (`99+`)
- Immediate reactivity without requiring a full app reload.

### 5.2 Notification Center (`notifications.tsx`)
- **Virtualization & Pagination**: Backed by `FlatList` with `onEndReached` threshold fetching page increments (`page + 1`, `PAGE_SIZE = 20`) and deduplicating items by `id`.
- **Pull-to-Refresh**: Native `RefreshControl` resetting to page 0 and syncing unread badge.
- **Filter Pills**: "All (`count`)" and "Unread (`count`)" pills for rapid triage.
- **Mark Single as Read**: Tapping an unread item immediately decrements unread count, marks it read in local state, and fires `PATCH /api/notifications/{id}/read`.
- **Mark All as Read**: Header button visible when `unreadCount > 0` calling `PATCH /api/notifications/read-all` with optimistic badge zeroing and failure rollback.
- **Deep Linking**: If `notification.issueId` is present, router navigates to `/issue/[id]`. If absent, marks as read without navigation.
- **Safe Handling for Missing/Deleted Issues**: When deep-linking to an issue that was removed or hidden, `IssueDetailScreen` displays a secure `ErrorState` rather than exposing raw server errors or moderation notes.

### 5.3 Notification Preferences Modal
- Accessible via the filter/gear icon in the Notifications screen header.
- Displays switches for the 4 backend-supported channels:
  1. **In-App Notifications** (`inAppEnabled`)
  2. **Push Notifications** (`pushEnabled`)
  3. **Email Updates** (`emailEnabled`)
  4. **SMS Alerts** (`smsEnabled`)
- Toggling triggers `notificationApi.updatePreferences` with optimistic UI update, individual switch disable/loading states, and rollback on network failure.
- Explicitly informs citizens that Push, Email, and SMS preferences are saved on the server for future municipal dispatch channels.

---

## 6. Accessibility & Privacy

- **Visual + Semantic Accessibility**:
  - `accessibilityRole="button"` and `accessibilityRole="tab"` on all interactive elements.
  - `accessibilityLabel` comprehensively conveys notification title, read/unread state, description, and relative timestamp.
  - Unread items are conveyed through visual tint, bold font, and an unread dot indicator, avoiding reliance on color alone.
- **Privacy Enforcement**:
  - No citizen phone numbers, private email addresses, or JWT secrets are ever displayed in notifications or printed in console logs.
  - Development builds maintain clean logging with zero authorization header leakage.

---

## 7. Automated Testing Suite

Automated verification script located in `scripts/verify-notifications-flow.js` and executed via `npm run test:notifications` or `npm test`:

- `[Test A]` Notification list: Real API queries, no mock data, loading, error, empty states
- `[Test B]` Read state: Visual/semantic distinction, mark-read on tap, idempotent guard
- `[Test C]` Unread count & Tab Badge: Synchronized context, 0 -> none, 1-99, 100+ -> "99+"
- `[Test D]` Mark all read: Backend call, local sync, badge zeroed, optimistic rollback
- `[Test E]` Deep linking: IssueId navigation to `/issue/[id]`, safe handling of missing/hidden issues
- `[Test F]` Icon mapping & Unknown types: Centralized mapping for all 11 types, safe fallback
- `[Test G]` Notification Preferences: GET/PATCH contract, 4 channels, optimistic toggle & rollback
- `[Test H]` Authentication & Token Refresh: Centralized auth, Bearer attachment, 401 refresh
- `[Test I]` Pagination & Pull-to-Refresh: Server-side paging, deduplication, native refresh
- `[Test J]` Accessibility, Privacy & Code Cleanliness: Role/labels, no token leaks, clean code
