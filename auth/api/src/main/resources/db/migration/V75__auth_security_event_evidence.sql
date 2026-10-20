-- Append-only security evidence. Operational export/retention is described in auth/docs/schema.md.
ALTER TABLE auth_pat_lifecycle_events
    ADD COLUMN reason_code VARCHAR(40) NOT NULL DEFAULT 'LEGACY_ACTION';
ALTER TABLE auth_pat_lifecycle_events
    ADD COLUMN before_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE auth_pat_lifecycle_events
    ADD COLUMN after_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';

CREATE TABLE auth_login_security_events (
    event_id VARCHAR(36) PRIMARY KEY,
    claimed_username VARCHAR(80) NOT NULL,
    authenticated_username VARCHAR(80),
    event_type VARCHAR(16) NOT NULL,
    reason_code VARCHAR(120) NOT NULL,
    before_failure_count INTEGER NOT NULL,
    after_failure_count INTEGER NOT NULL,
    before_locked_until TIMESTAMP,
    after_locked_until TIMESTAMP,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_auth_login_security_event_type CHECK (event_type IN ('FAILURE', 'SUCCESS'))
);
CREATE INDEX idx_auth_login_security_events_subject_time
    ON auth_login_security_events (claimed_username, occurred_at);
CREATE INDEX idx_auth_login_security_events_export
    ON auth_login_security_events (occurred_at, event_id);
CREATE INDEX idx_auth_pat_lifecycle_events_export
    ON auth_pat_lifecycle_events (occurred_at, event_id);
