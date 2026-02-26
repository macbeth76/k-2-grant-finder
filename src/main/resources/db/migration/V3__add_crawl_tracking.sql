ALTER TABLE grants ADD COLUMN source VARCHAR(50) DEFAULT 'MANUAL';
ALTER TABLE grants ADD COLUMN source_url VARCHAR(500);
ALTER TABLE grants ADD COLUMN last_crawled DATE;

CREATE TABLE crawl_runs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    source         VARCHAR(50) NOT NULL,
    started_at     TIMESTAMP NOT NULL,
    completed_at   TIMESTAMP,
    grants_found   INT DEFAULT 0,
    grants_created INT DEFAULT 0,
    grants_updated INT DEFAULT 0,
    errors         INT DEFAULT 0,
    error_message  TEXT
);
