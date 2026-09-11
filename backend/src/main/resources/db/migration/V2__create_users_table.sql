-- Nagrivic Database Foundation: Create Users Table
-- Purpose: User identity and accountability entity to establish referential integrity for reported issues

CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    role VARCHAR(32) NOT NULL DEFAULT 'CITIZEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users(phone_number);
