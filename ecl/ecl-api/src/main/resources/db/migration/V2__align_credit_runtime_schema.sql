-- =================================================================================
-- V2__align_credit_runtime_schema.sql
-- Reconciles the baseline V1 schema with the runtime schema currently expected by
-- credit-core and by local Docker init SQL.
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

-- 2. Batch audit ledger used by monitoring/reporting.
CREATE TABLE IF NOT EXISTS cr_batch_audits (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    job_name VARCHAR(100) NOT NULL,
    total_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    total_ead NUMERIC(19,4) DEFAULT 0,
    total_rwa NUMERIC(19,4) DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    start_at TIMESTAMP,
    end_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cr_batch_audits_base_date
    ON cr_batch_audits(base_date);

-- 3. Forward-looking ECL macro scenario master.
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

-- 4. Customer rating transition history.
CREATE TABLE IF NOT EXISTS cr_cust_rating_hist (
    id BIGSERIAL PRIMARY KEY,
    cust_id BIGINT NOT NULL REFERENCES cr_customers(id),
    rating_cd VARCHAR(10) NOT NULL,
    base_dt DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cr_cust_rating_hist_search
    ON cr_cust_rating_hist(cust_id, base_dt DESC);

-- 5. Standardized Approach risk weight mapping master.
CREATE TABLE IF NOT EXISTS cr_sa_rw_masters (
    id BIGSERIAL PRIMARY KEY,
    customer_type VARCHAR(30) NOT NULL,
    rating_code VARCHAR(10) NOT NULL,
    risk_weight NUMERIC(10,6) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cr_sa_rw_masters_customer_rating UNIQUE(customer_type, rating_code)
);

-- 6. Stress test simulation results.
CREATE TABLE IF NOT EXISTS cr_simulation_results (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    scenario VARCHAR(30) NOT NULL,
    account_id BIGINT NOT NULL REFERENCES cr_accounts(id),
    staging VARCHAR(20),
    ead_star NUMERIC(19,4),
    stress_pd NUMERIC(10,8),
    stress_lgd NUMERIC(10,8),
    stress_ecl NUMERIC(19,4),
    stress_rwa_irb NUMERIC(19,4),
    ecl_delta NUMERIC(19,4),
    rwa_delta NUMERIC(19,4),
    status VARCHAR(20),
    simulation_completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cr_simulation_results_base_date_scenario
    ON cr_simulation_results(base_date, scenario);

CREATE INDEX IF NOT EXISTS idx_cr_simulation_results_account_id
    ON cr_simulation_results(account_id);
