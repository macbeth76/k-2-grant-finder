-- PostgreSQL-compatible version of V1 migration.
-- Uses GENERATED ALWAYS AS IDENTITY instead of H2's AUTO_INCREMENT.

CREATE TABLE grants (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    min_amount  DECIMAL(12, 2),
    max_amount  DECIMAL(12, 2),
    deadline    DATE,
    eligibility TEXT,
    category    VARCHAR(50) NOT NULL,
    grant_type  VARCHAR(50) NOT NULL,
    website     VARCHAR(500),
    active      BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_grants_category ON grants(category);
CREATE INDEX idx_grants_grant_type ON grants(grant_type);
CREATE INDEX idx_grants_active ON grants(active);
