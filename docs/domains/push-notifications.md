# Push Notifications Domain & FCM Delivery Foundation

## 1. Domain Purpose & Philosophy

Nagrivic is a civic issue reporting platform where citizens report public issues (such as potholes, water leakages, garbage dumps, and streetlights) and track their resolution by municipal authorities. 

Timely notifications keep citizens informed about issue status changes, department acknowledgments, and resolution evidence. While Task 23 and Task 34 provide the authoritative in-app notification center, mobile citizens expect proactive lock-screen alerts.

Task 50 establishes a **production-ready, provider-independent push notification delivery channel** using Firebase Cloud Messaging (FCM) for Android and iOS devices.

```
Domain Event (Status Change, Resolution, Comment, etc.)
                   │
                   ▼
       NotificationService.createNotification(...)
                   │
                   ▼
       PostgreSQL Transaction Commit
     (notifications, notification_preferences)
                   │
                   ▼ (AFTER_COMMIT Spring Event)
       PushNotificationDispatcher
                   │
      ┌────────────┴────────────┐
      ▼                         ▼
Check Preferences        Query Active Devices
(isPushEnabled)          (push_devices)
      │                         │
      └────────────┬────────────┘
                   ▼
        PushNotificationProvider
                   │
       ┌───────────┴───────────┐
       ▼                       ▼
NoOpPushNotificationProvider  FcmPushNotificationProvider
(FCM_ENABLED=false)           (FCM_ENABLED=true)
- Dev & offline CI            - Firebase Admin SDK v9
- Zero cloud calls            - Android & iOS delivery
- Skips safely                - Token invalidation on error
```

---

## 2. Core Non-Negotiable Invariants

> [!IMPORTANT]
> **Push Notifications Architectural Rules**:
> 1. **In-App Database is the Sole Source of Truth**: The `notifications` table (Task 23) is the primary authoritative record of every user notification. FCM is strictly a secondary delivery channel.
> 2. **Post-Commit Delivery (Transaction Isolation)**: Network calls to FCM are strictly forbidden inside database transactions. Delivery triggers via Spring `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. Firebase outages or network timeouts never roll back or fail issue reporting or status transitions.
> 3. **Provider Abstraction**: Application services depend strictly on `PushNotificationProvider`. No Firebase SDK imports exist in domain entities, repositories, or controllers.
> 4. **Offline Development & CI by Default**: When `FCM_ENABLED=false` (the default), `NoOpPushNotificationProvider` skips delivery cleanly. Zero Firebase credentials or network connections are needed for `./gradlew.bat test` or local backend runs.
> 5. **Server-Derived Identity & Device Ownership**: The authenticated user ID is strictly extracted from JWT claims. Clients can never specify or spoof `userId` during registration. Device tokens belong to at most one active user.
> 6. **Lock-Screen Privacy**: Push payloads contain minimal, non-sensitive titles and bodies. No personal phone numbers, emails, addresses, citizen reporter identities, moderation reports, or internal AI scores are transmitted in push notifications.
> 7. **Controlled Deep Linking**: Deep links are restricted to application routes (`nagrivicapp://issue/{id}` or `nagrivicapp://notifications`). Arbitrary external URLs and script schemes are strictly suppressed.
> 8. **Automated Token Hygiene**: Tokens reported as `UNREGISTERED` or invalid by FCM are automatically deactivated (`is_active = false`), preventing retry loops on dead devices.

---

## 3. Database Schema

Defined in Flyway migration `V33__create_push_devices_and_deliveries_tables.sql`:

### 3.1 `push_devices` Table
```sql
CREATE TABLE push_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    device_token VARCHAR(512) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    app_version VARCHAR(64) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_push_devices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_push_devices_token UNIQUE (device_token),
    CONSTRAINT chk_push_devices_platform CHECK (platform IN ('ANDROID', 'IOS'))
);

CREATE INDEX idx_push_devices_user_active ON push_devices(user_id, is_active);
CREATE INDEX idx_push_devices_token ON push_devices(device_token);
```

### 3.2 `notification_push_deliveries` Table
```sql
CREATE TABLE notification_push_deliveries (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    device_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_message_id VARCHAR(255) NULL,
    error_code VARCHAR(128) NULL,
    sent_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_push_deliveries_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_push_deliveries_device FOREIGN KEY (device_id) REFERENCES push_devices(id) ON DELETE CASCADE
);

CREATE INDEX idx_push_deliveries_notification ON notification_push_deliveries(notification_id);
CREATE INDEX idx_push_deliveries_device ON notification_push_deliveries(device_id);
```

---

## 4. API Specification

All device registration endpoints require JWT authentication.

### 4.1 Register / Update Device
`POST /api/push/devices`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Request Body**:
```json
{
  "token": "fcm-device-registration-token",
  "platform": "ANDROID",
  "appVersion": "1.0.0"
}
```
- **Response (201 Created)**:
```json
{
  "id": "7ac1af2a-...",
  "platform": "ANDROID",
  "appVersion": "1.0.0",
  "isActive": true,
  "maskedToken": "fcm-de...token",
  "lastSeenAt": "2026-09-14T15:00:00Z",
  "createdAt": "2026-09-14T15:00:00Z"
}
```
*Note: Tokens are masked in API responses to avoid credential exposure.*

### 4.2 Deactivate Device by ID
`DELETE /api/push/devices/{deviceId}`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Behavior**: Verifies that the device belongs to the authenticated user. Sets `is_active = false`.
- **Response (200 OK)**:
```json
{
  "message": "Device registration deactivated successfully"
}
```

### 4.3 Deactivate Device by Token (Logout)
`POST /api/push/devices/deactivate`
- **Headers**: `Authorization: Bearer <accessToken>`
- **Request Body**:
```json
{
  "token": "fcm-device-registration-token"
}
```
- **Behavior**: Convenient for client logouts; deactivates the current device token if owned by user.
- **Response (200 OK)**:
```json
{
  "message": "Device token deactivated successfully"
}
```

---

## 5. Firebase Configuration & Environment Variables

| Variable | Default | Description |
|---|---|---|
| `FCM_ENABLED` | `false` | When `false`, uses `NoOpPushNotificationProvider`. Set to `true` for staging/production. |
| `FIREBASE_PROJECT_ID` | *(empty)* | Firebase project ID (e.g. `nagrivic-prod`). |
| `FIREBASE_CLIENT_EMAIL` | *(empty)* | Service account client email. |
| `FIREBASE_PRIVATE_KEY` | *(empty)* | Service account private key string (escaped newlines supported). |
| `FIREBASE_CREDENTIALS_PATH` | *(empty)* | Optional path to service account JSON file on server filesystem. |

### Development Mode Behavior
When `FCM_ENABLED=false`:
- Push notification provider status is `DISABLED`.
- `send()` returns `PushDeliveryResult.skipped("FCM push delivery is disabled")`.
- No network requests are made.
- Full test suite runs offline without Firebase accounts or mock servers.

---

## 6. Mobile Client Integration (Expo / React Native)

1. **Permissions (`requestPermissions`)**:
   - Requests `POST_NOTIFICATIONS` on Android 13+ (API 33+).
   - Requests user authorization on iOS.
   - If denied, app functions normally without push. In-app notification center remains fully accessible.
2. **Token Caching (`registerDeviceWithBackend`)**:
   - Token is cached in `SecureStore` (`nagrivic_cached_push_token`).
   - Redundant registrations on app restart are suppressed.
3. **Logout Deactivation (`unregisterDeviceOnLogout`)**:
   - Deactivates device token on backend.
   - Clears local cached token.
4. **Deep Linking (`resolveNavigationRoute`)**:
   - Validates notification payload.
   - Routes to `/issue/{id}` for issue events.
   - Routes to `/(tabs)/notifications` for general events.
   - Rejects arbitrary external URLs.
