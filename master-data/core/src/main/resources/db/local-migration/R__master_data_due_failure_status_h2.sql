-- Local H2 Flyway target is V8. This repeatable is kept out of the production migration-runner
-- and its staged V5-to-V6 upgrade path, while retaining the same application-visible schema.
ALTER TABLE master_data_change_requests
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);
ALTER TABLE master_data_change_requests
    ADD COLUMN IF NOT EXISTS failed_at TIMESTAMP;

ALTER TABLE master_data_change_requests
    DROP CONSTRAINT IF EXISTS ck_mdc_status;
ALTER TABLE master_data_change_requests
    ADD CONSTRAINT ck_mdc_status CHECK (status IN (
        'REQUESTED', 'APPROVED', 'REJECTED', 'APPLIED',
        'FAILED_CONFLICT', 'FAILED_INVALID'
    ));

CREATE INDEX IF NOT EXISTS idx_mdc_due_queue
    ON master_data_change_requests (status, effective_date, requested_at, id);
