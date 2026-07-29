CREATE TABLE IF NOT EXISTS auth_personal_access_tokens (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    token_name VARCHAR(100) NOT NULL,
    token_prefix VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pat_username ON auth_personal_access_tokens(username);
CREATE INDEX idx_pat_status ON auth_personal_access_tokens(status);
