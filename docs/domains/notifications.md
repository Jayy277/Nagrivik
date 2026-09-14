# Notifications Domain Architecture

## 1. Purpose

The **Notifications Domain** provides a durable, channel-independent backend foundation for user-facing alerts in the Nagrivic civic reporting platform.

It decouples business domain operations (issue status transitions, responsibility assignments, priority updates, comments, and duplicate detection) from notification creation and delivery providers.

> [!IMPORTANT]
> **Notifications are user-facing alerts, not the authoritative audit history.**
> The **Issue Activity Timeline** remains the complete, immutable, system-of-record audit trail for issue lifecycle events. Notifications exist solely to provide actionable or relevant awareness to affected citizens.

---

## 2. Notification vs. Activity Timeline

| Attribute | Issue Activity Timeline | Notification |
| :--- | :--- | :--- |
| **Audience** | Public / Audit System | Specific Individual User |
| **Scope** | Complete, detailed event log | High-value, filtered attention signals |
| **Persistence** | Immutable append-only audit trail | User mailbox (read/unread state) |
| **Actor Filter** | Logs actor explicitly | Suppresses redundant alerts to the actor |

Not every activity event triggers a notification. For example, a user's own comment or support action will not notify themselves.

---

## 3. Controlled Notification Types

The notification system uses a controlled enumeration (`NotificationType`) to prevent noise:

- `ISSUE_STATUS_CHANGED`: Generic fallback status change notification.
- `ISSUE_VERIFIED`: Issued when an issue status moves to `VERIFIED`.
- `ISSUE_ACKNOWLEDGED`: Issued when an issue status moves to `ACKNOWLEDGED`.
- `ISSUE_IN_PROGRESS`: Issued when work begins on an issue (`IN_PROGRESS`).
- `ISSUE_RESOLVED`: Issued when an issue reaches `RESOLVED`.
- `ISSUE_NOT_FIXED`: Issued when a resolved issue is marked `NOT_FIXED`.
- `ISSUE_CITIZEN_VERIFIED`: Issued when the reporter confirms fix (`CITIZEN_VERIFIED`).
- `ISSUE_RESPONSIBILITY_RESOLVED`: Issued when municipal responsibility (department & civic body) is assigned.
- `ISSUE_PRIORITY_CHANGED`: Issued when issue priority level changes (e.g., `MEDIUM` -> `HIGH`).
- `ISSUE_COMMENT_ACTIVITY`: Issued to the issue reporter when another user posts a comment.
- `ISSUE_DUPLICATE_DETECTED`: Issued to the reporter when their issue is explicitly linked as a duplicate.

---

## 4. Recipient Rules

1. **Issue Reporter**: Receives notifications for status transitions, responsibility resolution, priority level changes, new comments from other citizens/officers, and duplicate issue linking.
2. **Citizen Supporters**: Receive notifications for major lifecycle status changes (`VERIFIED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`, `NOT_FIXED`, `CITIZEN_VERIFIED`).
3. **Actor Suppression Rule**: If the user performing the domain action matches the recipient (e.g. reporter verifying resolution or commenter posting on their own issue), the notification is automatically skipped.
4. **Privacy Rule**: Supporter identities are never exposed in public APIs or notification payloads.

---

## 5. Notification Preferences

User channel preferences are managed via `notification_preferences`:

- `in_app_enabled`: `true` (default). Controls in-app notification persistence.
- `push_enabled`: `false` (default for future FCM/APNs integration).
- `email_enabled`: `false` (default for future email provider integration).
- `sms_enabled`: `false` (default for future SMS provider integration).

If a user disables `in_app_enabled`, the system suppresses in-app notification creation for that user.

---

## 6. Read / Unread Behavior

- **State Tracking**: `read_at` timestamp column. `NULL` indicates unread; non-null timestamp indicates read.
- **Unread Count**: Calculated server-side via `COUNT(*)` query (`GET /api/notifications/unread-count`).
- **Mark as Read**: Idempotent single notification update (`PATCH /api/notifications/{id}/read`).
- **Mark All Read**: Idempotent bulk update (`PATCH /api/notifications/read-all`) for all unread notifications belonging to the authenticated user.

---

## 7. Idempotency & Duplicate Protection

Duplicate notifications resulting from network retries, repeated service calls, or concurrent transactions are prevented using a deterministic `event_key` (e.g., `status:{issueId}:{toStatus}`).

Before inserting a notification, `NotificationService` verifies whether a notification with the same `(user_id, event_key)` already exists in the database.

---

## 8. Transaction & Rollback Boundaries

- Notification persistence occurs within the caller's `@Transactional` boundary.
- If a domain operation fails or rolls back, all associated notifications roll back atomically.
- Notifications are never persisted before successful domain state transitions.

---

## 9. Privacy & Security

- **Ownership Access Control**: Users can retrieve, view, and modify only their own notifications and preferences (`user_id == currentUserId`).
- **JWT Identity**: Authenticated identity is strictly derived from JWT claims. API request bodies do not accept authority selectors like `userId`.
- **Payload Safety**: Notification titles and bodies are server-generated. Private data (phone numbers, email addresses, moderation notes, supporter lists) is strictly omitted.

---

## 10. Performance & Fan-out Limitations

- Supporter lookup uses optimized single-column database queries (`SELECT s.user.id FROM SupportEntity s WHERE s.issue.id = :issueId`) to avoid N+1 entity hydration.
- Supporter fan-out is bounded to a safety threshold (`MAX_SUPPORTER_FANOUT = 500`) to prevent synchronous transaction delays.
- A future task will move large supporter fan-out delivery to background async queues.

---

---

## 11. Push Delivery Channel (Task 50)

Push notification delivery is implemented via Firebase Cloud Messaging (FCM):
- **Provider Abstraction**: Decoupled via `PushNotificationProvider` (`NoOpPushNotificationProvider` and `FcmPushNotificationProvider`).
- **Post-Commit Event Flow**: Dispatched strictly `AFTER_COMMIT` via `NotificationCreatedEvent` so database transactions remain isolated from external network I/O.
- **Preferences & Devices**: Respects `notification_preferences.push_enabled` and queries active devices in `push_devices`.
- **Token Hygiene**: Inactive/unregistered tokens are automatically deactivated.
- **Detailed Specification**: See [Push Notifications Domain](file:///j:/Nagrivic/docs/domains/push-notifications.md).

---

## 12. Deferred Delivery Channels (Future Scope)

The following delivery channels remain deferred:
- Email delivery
- SMS notifications
- WhatsApp / Telegram delivery
- Web Push

