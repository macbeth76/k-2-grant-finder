-- =============================================================================
-- PostgreSQL initialization script for k2-grant-finder
-- =============================================================================
-- Run this against the existing PostgreSQL instance on royaloak (port 5433)
-- as the superuser or the testuser who has CREATE DATABASE / CREATE ROLE rights.
--
-- Usage:
--   psql -h 192.168.0.211 -p 5433 -U testuser -d testdb -f postgres-init.sql
--
-- Or from Docker:
--   docker exec -i <postgres-container> psql -U testuser -d testdb -f - < postgres-init.sql
-- =============================================================================

-- Create the application database user (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'grantfinder') THEN
        CREATE ROLE grantfinder WITH LOGIN PASSWORD 'grantfinder';
    END IF;
END
$$;

-- Create the grantfinder database (if not exists)
-- NOTE: CREATE DATABASE cannot run inside a transaction block, so this must be
-- run separately or via \gexec. The simplest approach is to run the SELECT
-- below in psql:
--
--   SELECT 'CREATE DATABASE grantfinder OWNER grantfinder'
--   WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'grantfinder')\gexec
--
-- For scripted use, we use a DO block with dblink or just attempt creation
-- and tolerate the error:

CREATE DATABASE grantfinder OWNER grantfinder;

-- Connect to the grantfinder database to set up permissions
\connect grantfinder

-- Grant all privileges on the database to the grantfinder user
GRANT ALL PRIVILEGES ON DATABASE grantfinder TO grantfinder;

-- Grant schema usage and creation rights
GRANT ALL ON SCHEMA public TO grantfinder;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO grantfinder;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO grantfinder;

-- =============================================================================
-- The application uses Flyway for schema migrations, so tables will be created
-- automatically on first startup. No need to create tables here.
--
-- IMPORTANT: The Flyway migrations (V1, V2, V3) currently use H2 syntax:
--   - BIGINT AUTO_INCREMENT PRIMARY KEY
-- For PostgreSQL, these need to be changed to:
--   - BIGSERIAL PRIMARY KEY
-- or:
--   - BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
--
-- You can either:
--   1. Update the existing migration files for Postgres compatibility, OR
--   2. Create a db/migration/postgres/ directory with Postgres-specific SQL
--      and configure Flyway locations per environment
-- =============================================================================
