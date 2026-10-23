CREATE TABLE payable_offset_operations (
    operation_id VARCHAR(64) PRIMARY KEY,
    payable_id BIGINT NOT NULL REFERENCES payables(id),
    advance_payment_id BIGINT NOT NULL REFERENCES advance_payments(id),
    amount NUMERIC(19, 2) NOT NULL,
    journal_entry_id BIGINT NOT NULL,
    CONSTRAINT ck_payable_offset_operation_amount CHECK (amount > 0)
);

CREATE INDEX idx_payable_offset_operations_payable ON payable_offset_operations(payable_id);
CREATE INDEX idx_payable_offset_operations_advance ON payable_offset_operations(advance_payment_id);
