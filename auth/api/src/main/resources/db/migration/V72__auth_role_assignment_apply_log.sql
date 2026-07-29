CREATE TABLE IF NOT EXISTS auth_role_assignment_apply_log (
    approval_trace_id VARCHAR(160) PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    applied_role_version BIGINT NOT NULL,
    applied_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auth_role_assignment_apply_log_user
    ON auth_role_assignment_apply_log (username, applied_at);
