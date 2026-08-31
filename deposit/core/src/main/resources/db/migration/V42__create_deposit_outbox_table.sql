CREATE TABLE deposit_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    source_module VARCHAR(50) NOT NULL,
    lineage_source_type VARCHAR(50) NOT NULL,
    lineage_source_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_deposit_outbox_status_created ON deposit_outbox(status, created_at);
CREATE INDEX idx_deposit_outbox_idempotency ON deposit_outbox(idempotency_key);
