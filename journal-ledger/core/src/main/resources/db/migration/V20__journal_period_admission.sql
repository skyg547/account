-- One authoritative journal database; all monetary writers must join this lock before deployment.
CREATE TABLE journal_monetary_revision (
    control_id SMALLINT PRIMARY KEY CHECK (control_id = 1),
    revision BIGINT NOT NULL CHECK (revision >= 0)
);
INSERT INTO journal_monetary_revision (control_id, revision) VALUES (1, 0);

-- Commands are never deleted or rebound: RELEASED rows defeat delayed fence requests after release.
CREATE TABLE journal_period_fences (
    operation_id VARCHAR(100) PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL CHECK (fiscal_period_id > 0),
    range_start DATE NOT NULL,
    range_end DATE NOT NULL,
    expected_revision BIGINT NOT NULL CHECK (expected_revision >= 0),
    actor VARCHAR(50) NOT NULL,
    state VARCHAR(16) NOT NULL CHECK (state IN ('ACTIVE', 'RELEASED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    CONSTRAINT ck_journal_fence_range CHECK (range_start <= range_end),
    CONSTRAINT ck_journal_fence_release CHECK (
        (state = 'ACTIVE' AND released_at IS NULL) OR (state = 'RELEASED' AND released_at IS NOT NULL))
);
CREATE INDEX ix_journal_fence_active_end ON journal_period_fences (state, range_end);
