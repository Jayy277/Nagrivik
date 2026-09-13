-- Nagrivic Authentication Foundation: Create OTP Verifications Table
-- Purpose: Ephemeral verification tokens for passwordless phone authentication

CREATE TABLE otp_verifications (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(32) NOT NULL DEFAULT 'LOGIN',
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_phone_purpose ON otp_verifications(phone_number, purpose);
CREATE INDEX idx_otp_expires_at ON otp_verifications(expires_at);
