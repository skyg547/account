-- Closing-owned immutable source evidence. The digest reference in Journal lineage resolves here
-- even if ecl.allowance_summary is rebuilt. Operation keys reject a different source on replay.
CREATE TABLE ecl_provision_snapshots (
    snapshot_reference VARCHAR(72) PRIMARY KEY,
    source_fingerprint VARCHAR(64) NOT NULL,
    base_date DATE NOT NULL,
    run_id VARCHAR(80) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    legal_entity_code VARCHAR(20) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    allowance_account_code VARCHAR(50) NOT NULL,
    target_allowance_amount NUMERIC(38, 8) NOT NULL,
    source_exposure_amount NUMERIC(38, 8) NOT NULL,
    stage1_allowance_amount NUMERIC(38, 8) NOT NULL,
    stage2_allowance_amount NUMERIC(38, 8) NOT NULL,
    stage3_allowance_amount NUMERIC(38, 8) NOT NULL,
    existing_transaction_amount NUMERIC(19, 2) NOT NULL,
    existing_base_amount NUMERIC(19, 2) NOT NULL,
    closing_rate NUMERIC(19, 8) NOT NULL,
    adjustment_transaction_amount NUMERIC(19, 2) NOT NULL,
    adjustment_base_amount NUMERIC(19, 2) NOT NULL
);
CREATE TABLE ecl_provision_snapshot_bindings (
    operation_key VARCHAR(64) PRIMARY KEY,
    provision_batch_id BIGINT NOT NULL,
    snapshot_reference VARCHAR(72) NOT NULL,
    CONSTRAINT fk_ecl_provision_snapshot_binding
        FOREIGN KEY (snapshot_reference) REFERENCES ecl_provision_snapshots (snapshot_reference)
);
CREATE TABLE ecl_provision_run_bindings (
    base_date DATE NOT NULL,
    provision_batch_id BIGINT NOT NULL,
    snapshot_set_digest VARCHAR(64) NOT NULL,
    CONSTRAINT pk_ecl_provision_run_binding PRIMARY KEY (base_date, provision_batch_id)
);
CREATE INDEX idx_ecl_provision_snapshot_binding_reference
    ON ecl_provision_snapshot_bindings (snapshot_reference);
