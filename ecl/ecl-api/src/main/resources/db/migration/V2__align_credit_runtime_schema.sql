-- =================================================================================
-- V2__align_credit_runtime_schema.sql
-- Reconciles the baseline V1 schema with the IFRS 9 allowance runtime schema.
-- =================================================================================

-- 1. Backfill V1-expanded account columns that are still missing in local init SQL.
ALTER TABLE cr_accounts
    ADD COLUMN IF NOT EXISTS prod_category VARCHAR(30),
    ADD COLUMN IF NOT EXISTS int_rate NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS repayment_method VARCHAR(30),
    ADD COLUMN IF NOT EXISTS grace_period INTEGER,
    ADD COLUMN IF NOT EXISTS repayment_freq INTEGER,
    ADD COLUMN IF NOT EXISTS branch_cd VARCHAR(20),
    ADD COLUMN IF NOT EXISTS biz_unit_cd VARCHAR(20);

-- 2. Forward-looking ECL macro scenario master.
CREATE TABLE IF NOT EXISTS cr_macro_scenario (
    id BIGSERIAL PRIMARY KEY,
    apply_year INTEGER NOT NULL,
    scenario_type VARCHAR(20) NOT NULL,
    probability_weight NUMERIC(10,6) NOT NULL,
    pd_adjustment_factor NUMERIC(10,6) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cr_macro_scenario_apply_year
    ON cr_macro_scenario(apply_year);

-- 3. Customer rating transition history.
CREATE TABLE IF NOT EXISTS cr_cust_rating_hist (
    id BIGSERIAL PRIMARY KEY,
    cust_id BIGINT NOT NULL REFERENCES cr_customers(id),
    rating_cd VARCHAR(10) NOT NULL,
    base_dt DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cr_cust_rating_hist_search
    ON cr_cust_rating_hist(cust_id, base_dt DESC);

