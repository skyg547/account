CREATE TABLE payable_offset_operations (
    operation_id VARCHAR(60) NOT NULL,
    payable_id BIGINT NOT NULL REFERENCES payables(id),
    advance_payment_id BIGINT NOT NULL REFERENCES advance_payments(id),
    amount NUMERIC(19, 2) NOT NULL,
    accounting_date DATE NOT NULL,
    journal_entry_id BIGINT NOT NULL,
    CONSTRAINT pk_payable_offset_operations PRIMARY KEY (payable_id, operation_id),
    CONSTRAINT ck_payable_offset_operation_amount CHECK (amount > 0)
);

CREATE INDEX idx_payable_offset_operations_advance ON payable_offset_operations(advance_payment_id);
