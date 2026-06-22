CREATE TABLE IF NOT EXISTS allowance_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    execution_param TEXT,
    executed_by VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_ms BIGINT
);

CREATE INDEX IF NOT EXISTS idx_allowance_audit_logs_created_at
    ON allowance_audit_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_allowance_audit_logs_service_name
    ON allowance_audit_logs(service_name, created_at DESC);
