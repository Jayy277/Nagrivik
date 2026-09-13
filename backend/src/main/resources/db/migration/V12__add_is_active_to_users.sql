-- Nagrivic Database Migration: Add is_active flag to users table
-- Purpose: Support active user verification and account status enforcement for civic reporting

ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
