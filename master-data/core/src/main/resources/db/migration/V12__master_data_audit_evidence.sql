-- The original rationale remains in reason; the rejection rationale is a separate decision.
ALTER TABLE master_data_change_requests
    ADD COLUMN IF NOT EXISTS decision_reason VARCHAR(500);

CREATE SEQUENCE IF NOT EXISTS master_data_audit_event_ids START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS master_data_audit_events (
    id BIGINT DEFAULT nextval('master_data_audit_event_ids') PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    request_id BIGINT,
    source_reference VARCHAR(120),
    service_actor VARCHAR(80),
    business_actor VARCHAR(80) NOT NULL,
    reason VARCHAR(500),
    before_version_id BIGINT,
    after_version_id BIGINT,
    integrity_hash VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_master_data_audit_request ON master_data_audit_events (request_id, id);
CREATE INDEX IF NOT EXISTS idx_master_data_audit_target ON master_data_audit_events (target_type, target_key, id);
