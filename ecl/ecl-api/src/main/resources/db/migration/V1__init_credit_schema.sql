-- =================================================================================
-- V1__init_credit_schema.sql (Initial Credit Risk Schema)
-- =================================================================================

-- 1-1. 대손충당금(IFRS9) 차주 마스터 (CrCustomer)
CREATE TABLE cr_customers (
    id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(200) NOT NULL,
    customer_type VARCHAR(30) NOT NULL,
    internal_rating VARCHAR(10),
    external_rating VARCHAR(10),
    industry_code VARCHAR(20),
    country_code VARCHAR(2),
    is_sme BOOLEAN DEFAULT FALSE,
    fin_sector_cd VARCHAR(20),
    annual_sales NUMERIC(19,4),
    warning_level VARCHAR(20) DEFAULT 'NORMAL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 1-2. 상품별 리스크 규제 파라미터 (CrProductMaster)
CREATE TABLE cr_product_masters (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL UNIQUE,
    product_name VARCHAR(200) NOT NULL,
    ccf_rate NUMERIC(10,6) NOT NULL DEFAULT 0,
    standard_rw NUMERIC(10,6) NOT NULL DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 1-3. 대손충당금(IFRS9) 익스포저 원장 (CrAccount)
CREATE TABLE cr_accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES cr_customers(id),
    account_no VARCHAR(50) NOT NULL UNIQUE,
    product_code VARCHAR(20) NOT NULL,
    outstanding_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
    notional_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
    -- [고도화 확장 필드]
    prod_category VARCHAR(30),
    int_rate NUMERIC(19,6),
    repayment_method VARCHAR(30),
    grace_period INTEGER,
    repayment_freq INTEGER,
    branch_cd VARCHAR(20),
    biz_unit_cd VARCHAR(20),
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    open_date DATE NOT NULL,
    maturity_date DATE,
    delinquent_days INTEGER DEFAULT 0,
    original_rating VARCHAR(10),
    staging VARCHAR(20),
    internal_rating VARCHAR(10),
    is_debt_restructured BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 1-4. 대손충당금(IFRS9) 담보 마스터 (CrCollateral)
CREATE TABLE cr_collaterals (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES cr_customers(id),
    collateral_code VARCHAR(50) NOT NULL UNIQUE,
    collateral_type VARCHAR(30) NOT NULL,
    appraisal_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
    base_haircut NUMERIC(10,6) NOT NULL DEFAULT 0,
    prior_lien_amt NUMERIC(19,4) DEFAULT 0,
    ltv_limit NUMERIC(10,6),
    kb_market_price NUMERIC(19,4),
    district_code VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 1-5. 계좌-담보 매핑 (CrAccountCollateral)
CREATE TABLE cr_account_collaterals (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES cr_accounts(id),
    collateral_id BIGINT NOT NULL REFERENCES cr_collaterals(id),
    allocation_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    priority INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1-6. 대손충당금(IFRS9) 최종 산출 결과 (CrRiskResult)
CREATE TABLE cr_risk_results (
    id BIGINT NOT NULL,
    base_date DATE NOT NULL,
    account_id BIGINT NOT NULL REFERENCES cr_accounts(id),
    staging VARCHAR(20) NOT NULL,
    ead NUMERIC(19,4),
    applied_ccf NUMERIC(10,6),
    pd NUMERIC(15,10),
    lgd NUMERIC(15,10),
    expected_loss NUMERIC(19,4),
    unexpected_loss NUMERIC(19,4),
    ecl_boom NUMERIC(19,4),
    ecl_base NUMERIC(19,4),
    ecl_recession NUMERIC(19,4),
    weighted_ecl NUMERIC(19,4),
    k_value NUMERIC(15,10),
    r_value NUMERIC(15,10),
    maturity_adj NUMERIC(15,10),
    ead_star NUMERIC(19,4),
    crm_deduction NUMERIC(19,4),
    applied_rw NUMERIC(10,6),
    rwa_sa NUMERIC(19,4),
    rwa_irb NUMERIC(19,4),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    calculation_completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    PRIMARY KEY (base_date, id)
) PARTITION BY RANGE (base_date);

CREATE TABLE cr_risk_results_2026m04 PARTITION OF cr_risk_results 
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- 2-1. 신용등급 마스터 (CrGradeMaster)
CREATE TABLE cr_grade_masters (
    id BIGSERIAL PRIMARY KEY,
    rating_code VARCHAR(10) NOT NULL UNIQUE,
    pd_value NUMERIC(15,10) NOT NULL,
    notch_order INTEGER NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2-2. LGD 세그먼트 마스터
CREATE TABLE cr_lgd_segment_masters (
    id BIGSERIAL PRIMARY KEY,
    segment_name VARCHAR(100) NOT NULL,
    customer_type VARCHAR(30) NOT NULL,
    collateral_type VARCHAR(30) NOT NULL,
    lgd_value NUMERIC(15,10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_type, collateral_type)
);

-- 2-3. 규제 파라미터 컨피그
CREATE TABLE cr_regulatory_parameters (
    param_key VARCHAR(50) PRIMARY KEY,
    param_value NUMERIC(19,10) NOT NULL,
    description VARCHAR(200),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2-8. 월간 리스크 요약 마트
CREATE TABLE cr_monthly_summaries (
    id BIGSERIAL PRIMARY KEY,
    base_date DATE NOT NULL,
    product_group VARCHAR(50),
    customer_type VARCHAR(30),
    staging VARCHAR(20),
    total_count INTEGER DEFAULT 0,
    total_ead NUMERIC(19,4) DEFAULT 0,
    total_ead_star NUMERIC(19,4) DEFAULT 0,
    total_rwa_sa NUMERIC(19,4) DEFAULT 0,
    total_rwa_irb NUMERIC(19,4) DEFAULT 0,
    total_expected_loss NUMERIC(19,4) DEFAULT 0,
    avg_pd NUMERIC(15,10),
    avg_lgd NUMERIC(15,10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cr_acc_cust ON cr_accounts(customer_id);
CREATE INDEX idx_cr_res_date ON cr_risk_results(base_date);
CREATE INDEX idx_cr_res_acc ON cr_risk_results(account_id);
CREATE INDEX idx_cr_monthly_date ON cr_monthly_summaries(base_date);
