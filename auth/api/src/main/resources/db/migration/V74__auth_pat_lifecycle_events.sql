-- PAT ownership uses the same username width as auth_users.
ALTER TABLE auth_personal_access_tokens
    ALTER COLUMN username SET DATA TYPE VARCHAR(80);

CREATE TABLE IF NOT EXISTS auth_pat_lifecycle_events (
    event_id VARCHAR(36) PRIMARY KEY,
    token_id VARCHAR(36) NOT NULL,
    action VARCHAR(16) NOT NULL,
    actor_username VARCHAR(80) NOT NULL,
    owner_username VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_auth_pat_lifecycle_events_action
        CHECK (action IN ('CREATED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_auth_pat_lifecycle_events_token
    ON auth_pat_lifecycle_events (token_id);
