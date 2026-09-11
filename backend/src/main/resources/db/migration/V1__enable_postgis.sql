-- Nagrivic Database Foundation: Initial Migration
-- Purpose: Enable the PostGIS spatial database extension
-- Notice: Business tables (users, issues, categories, etc.) are strictly deferred to future tasks.

CREATE EXTENSION IF NOT EXISTS postgis;
