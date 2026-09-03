-- Append-only audit trail and actor lineage for internal audit RCM, evaluations, and deficiencies (H2 mode).
CREATE TABLE internal_audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    action_timestamp TIMESTAMP NOT NULL,
    correlation_id VARCHAR(255),
    idempotency_key VARCHAR(255),
    details_json TEXT,
    CONSTRAINT uq_internal_audit_log_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_internal_audit_log_aggregate ON internal_audit_log(aggregate_type, aggregate_id);
CREATE INDEX idx_internal_audit_log_correlation ON internal_audit_log(correlation_id);
