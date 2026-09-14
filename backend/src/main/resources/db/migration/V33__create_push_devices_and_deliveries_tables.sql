-- V33: Create push_devices and notification_push_deliveries tables
-- Purpose: Push notification device registration and delivery audit foundation (Task 50).

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
