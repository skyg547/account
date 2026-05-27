-- =================================================================================
-- V2__add_allowance_exposure_snapshot.sql
-- CDM -> allowance/ECL calculation input snapshot
-- =================================================================================

CREATE TABLE IF NOT EXISTS allowance_exposure_snapshots (
    base_date DATE NOT NULL,
    exposure_id VARCHAR(80) NOT NULL,
    source_system VARCHAR(30) NOT NULL,
    source_account_no VARCHAR(50) NOT NULL,
    customer_code VARCHAR(50) NOT NULL,
    customer_type VARCHAR(30),
    is_sme BOOLEAN DEFAULT FALSE,
    country_code VARCHAR(10),
    industry_code VARCHAR(20),
    product_code VARCHAR(20),
    product_category VARCHAR(30),
    legal_entity_code VARCHAR(20) NOT NULL,
    branch_code VARCHAR(20),
    currency_code VARCHAR(3) NOT NULL,
    outstanding_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    undrawn_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    interest_rate NUMERIC(10,6),
    effective_interest_rate NUMERIC(10,6),
    open_date DATE,
    maturity_date DATE,
    delinquent_days INTEGER DEFAULT 0,
    staging VARCHAR(20),
    original_rating VARCHAR(20),
    current_rating VARCHAR(20),
    warning_level VARCHAR(20),
    debt_restructured BOOLEAN NOT NULL DEFAULT FALSE,
    collateral_value NUMERIC(19,4) NOT NULL DEFAULT 0,
    collateral_type VARCHAR(20),
    accounting_account_code VARCHAR(20),
    allowance_account_code VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (base_date, exposure_id)
);

CREATE INDEX IF NOT EXISTS idx_allowance_exposure_base
    ON allowance_exposure_snapshots(base_date);

CREATE INDEX IF NOT EXISTS idx_allowance_exposure_customer
    ON allowance_exposure_snapshots(customer_code);

CREATE INDEX IF NOT EXISTS idx_allowance_exposure_product_currency
    ON allowance_exposure_snapshots(product_code, currency_code);
