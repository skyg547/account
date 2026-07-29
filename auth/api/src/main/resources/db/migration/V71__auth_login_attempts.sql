CREATE TABLE IF NOT EXISTS auth_login_attempts (
    username VARCHAR(80) PRIMARY KEY,
    failure_count INTEGER NOT NULL,
    locked_until TIMESTAMP,
    last_failure_reason VARCHAR(120),
    last_failure_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
