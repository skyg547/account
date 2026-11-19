-- H2 local equivalent of PostgreSQL V14, which is beyond the local Flyway target.
CREATE TABLE IF NOT EXISTS fiscal_period_transitions (
    operation_id VARCHAR(128) PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_periods(id),
    expected_status VARCHAR(20) NOT NULL CHECK (expected_status IN ('OPEN', 'CLOSED', 'PERMANENTLY_CLOSED')),
    target_status VARCHAR(20) NOT NULL CHECK (target_status IN ('OPEN', 'CLOSED', 'PERMANENTLY_CLOSED')),
    audit_user VARCHAR(42) NOT NULL,
    phase VARCHAR(12) NOT NULL CHECK (phase IN ('PREPARED', 'APPLIED', 'CANCELLED', 'CONFIRMED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_fiscal_transition_phase_time CHECK (
        (phase = 'PREPARED' AND applied_at IS NULL AND cancelled_at IS NULL AND confirmed_at IS NULL) OR
        (phase = 'APPLIED' AND applied_at IS NOT NULL AND cancelled_at IS NULL AND confirmed_at IS NULL) OR
        (phase = 'CANCELLED' AND applied_at IS NULL AND cancelled_at IS NOT NULL AND confirmed_at IS NULL) OR
        (phase = 'CONFIRMED' AND applied_at IS NOT NULL AND cancelled_at IS NULL AND confirmed_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_fiscal_period_transitions_period ON fiscal_period_transitions(fiscal_period_id);
