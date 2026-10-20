-- Forward-only PostgreSQL upgrade. V1-V10 checksums and existing request/approval audit stay intact.
ALTER TABLE master_data_change_requests
    ADD COLUMN failure_reason VARCHAR(500);
ALTER TABLE master_data_change_requests
    ADD COLUMN failed_at TIMESTAMP;

-- V6's status check excluded quarantined requests. This replacement validates all existing rows.
ALTER TABLE master_data_change_requests
    DROP CONSTRAINT ck_mdc_status;
ALTER TABLE master_data_change_requests
    ADD CONSTRAINT ck_mdc_status CHECK (status IN (
        'REQUESTED', 'APPROVED', 'REJECTED', 'APPLIED',
        'FAILED_CONFLICT', 'FAILED_INVALID'
    ));

-- A bounded APPROVED scan follows effective date, request time, then ID for stable ties.
CREATE INDEX idx_mdc_due_queue
    ON master_data_change_requests (status, effective_date, requested_at, id);
