-- Existing documents retain NULL bindings; new retryable requests bind an explicit business operation.
ALTER TABLE deferred_items ADD COLUMN operation_key VARCHAR(100);
ALTER TABLE deferred_items ADD COLUMN source_request_fingerprint VARCHAR(64);
ALTER TABLE deferred_items ADD CONSTRAINT uk_deferred_item_operation UNIQUE (loan_id, operation_key);
ALTER TABLE deferred_items ADD CONSTRAINT ck_deferred_item_operation_binding CHECK (
    (operation_key IS NULL AND source_request_fingerprint IS NULL) OR
    (operation_key IS NOT NULL AND CHAR_LENGTH(operation_key) > 0
     AND source_request_fingerprint IS NOT NULL AND CHAR_LENGTH(source_request_fingerprint) = 64));
ALTER TABLE loan_events ADD COLUMN operation_key VARCHAR(100);
ALTER TABLE loan_events ADD COLUMN source_request_fingerprint VARCHAR(64);
ALTER TABLE loan_events ADD CONSTRAINT uk_loan_event_operation UNIQUE (loan_id, operation_key);
ALTER TABLE loan_events ADD CONSTRAINT ck_loan_event_operation_binding CHECK (
    (operation_key IS NULL AND source_request_fingerprint IS NULL) OR
    (operation_key IS NOT NULL AND CHAR_LENGTH(operation_key) > 0
     AND source_request_fingerprint IS NOT NULL AND CHAR_LENGTH(source_request_fingerprint) = 64));
