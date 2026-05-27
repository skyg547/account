-- =================================================================================
-- V3__add_allowance_summary.sql
-- Accounting-facing allowance summary generated from completed ECL results.
-- =================================================================================

CREATE TABLE IF NOT EXISTS allowance_account_mappings (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL,
    biz_unit_code VARCHAR(20),
    currency_code VARCHAR(3),
    legal_entity_code VARCHAR(20) NOT NULL DEFAULT 'DEFAULT',
    exposure_account_code VARCHAR(20) NOT NULL,
    allowance_account_code VARCHAR(20) NOT NULL,
    bad_debt_expense_account_code VARCHAR(20) NOT NULL,
    reversal_income_account_code VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_allowance_account_mappings_lookup
    ON allowance_account_mappings(product_code, biz_unit_code, currency_code, active);

CREATE TABLE IF NOT EXISTS allowance_summary (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    run_id VARCHAR(80) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    legal_entity_code VARCHAR(20) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exposure_account_code VARCHAR(20) NOT NULL,
    allowance_account_code VARCHAR(20) NOT NULL,
    bad_debt_expense_account_code VARCHAR(20) NOT NULL,
    reversal_income_account_code VARCHAR(20) NOT NULL,
    target_allowance_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    source_exposure_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    stage1_allowance_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    stage2_allowance_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    stage3_allowance_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_allowance_summary_run_key UNIQUE (
        base_date,
        run_id,
        legal_entity_code,
        currency_code,
        exposure_account_code,
        allowance_account_code
    )
);

CREATE INDEX IF NOT EXISTS idx_allowance_summary_base_date
    ON allowance_summary(base_date);

CREATE INDEX IF NOT EXISTS idx_allowance_summary_run
    ON allowance_summary(base_date, run_id);
