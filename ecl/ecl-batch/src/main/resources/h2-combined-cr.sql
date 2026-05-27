-- =================================================================================
-- H2 Compatible Credit Risk Service Schema (CR)
-- =================================================================================

-- 1-1. 신용리스크 차주 마스터 (CrCustomer)
CREATE TABLE IF NOT EXISTS cr_customers (
    id SERIAL PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(200) NOT NULL,
    customer_type VARCHAR(30) NOT NULL,
    internal_rating VARCHAR(10),
    external_rating VARCHAR(10),
    industry_code VARCHAR(20),
    country_code VARCHAR(2),
    is_sme BOOLEAN DEFAULT FALSE,
    annual_sales NUMERIC(19,4),
    warning_level VARCHAR(20) DEFAULT 'NORMAL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1-2. 상품별 리스크 규제 파라미터 (CrProductMaster)
CREATE TABLE IF NOT EXISTS cr_product_masters (
    id SERIAL PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL UNIQUE,
    product_name VARCHAR(200) NOT NULL,
    ccf_rate NUMERIC(5,4) NOT NULL DEFAULT 0,
    standard_rw NUMERIC(5,4) NOT NULL DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1-3. 신용리스크 익스포저 원장 (CrAccount)
CREATE TABLE IF NOT EXISTS cr_accounts (
    id SERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES cr_customers(id),
    account_no VARCHAR(50) NOT NULL UNIQUE,
    product_code VARCHAR(20) NOT NULL,
    outstanding_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
    notional_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
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
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1-4. 신용리스크 담보 마스터 (CrCollateral)
CREATE TABLE IF NOT EXISTS cr_collaterals (
    id SERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES cr_customers(id),
    collateral_code VARCHAR(50) NOT NULL UNIQUE,
    collateral_type VARCHAR(30) NOT NULL,
    appraisal_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
    base_haircut NUMERIC(5,4) NOT NULL DEFAULT 0,
    prior_lien_amt NUMERIC(19,4) DEFAULT 0,
    ltv_limit NUMERIC(5,4),
    kb_market_price NUMERIC(19,4),
    district_code VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1-5. 계좌-담보 매핑 (CrAccountCollateral)
CREATE TABLE IF NOT EXISTS cr_account_collaterals (
    id SERIAL PRIMARY KEY,
    account_id BIGINT REFERENCES cr_accounts(id),
    collateral_id BIGINT REFERENCES cr_collaterals(id),
    allocation_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    priority INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1-6. 신용리스크 최종 산출 결과 (CrRiskResult)
-- H2 doesn't support PARTITION BY RANGE in this syntax, using regular table.
CREATE TABLE IF NOT EXISTS cr_risk_results (
    id SERIAL,
    base_date DATE NOT NULL,
    account_id BIGINT REFERENCES cr_accounts(id),
    staging VARCHAR(20) NOT NULL,
    ead NUMERIC(19,4),
    applied_ccf NUMERIC(5,4),
    pd NUMERIC(10,8),
    lgd NUMERIC(10,8),
    expected_loss NUMERIC(19,4),
    unexpected_loss NUMERIC(19,4),
    ecl_boom NUMERIC(19,4),
    ecl_base NUMERIC(19,4),
    ecl_recession NUMERIC(19,4),
    weighted_ecl NUMERIC(19,4),
    k_value NUMERIC(10,8),
    r_value NUMERIC(10,8),
    maturity_adj NUMERIC(10,8),
    ead_star NUMERIC(19,4),
    crm_deduction NUMERIC(19,4),
    applied_rw NUMERIC(5,4),
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
);

-- 2-1. 신용등급 마스터 (CrGradeMaster)
CREATE TABLE IF NOT EXISTS cr_grade_masters (
    id SERIAL PRIMARY KEY,
    rating_code VARCHAR(10) NOT NULL UNIQUE,
    pd_value NUMERIC(10,8) NOT NULL,
    notch_order INTEGER NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2-2. LGD 세그먼트 마스터 (CrLgdSegmentMaster)
CREATE TABLE IF NOT EXISTS cr_lgd_segment_masters (
    id SERIAL PRIMARY KEY,
    segment_name VARCHAR(100) NOT NULL,
    customer_type VARCHAR(30) NOT NULL,
    collateral_type VARCHAR(30) NOT NULL,
    lgd_value NUMERIC(10,8) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_type, collateral_type)
);

-- 2-3. 규제 파라미터 컨피그 (CrRegulatoryParameter)
CREATE TABLE IF NOT EXISTS cr_regulatory_parameters (
    param_key VARCHAR(50) PRIMARY KEY,
    param_value NUMERIC(19,8) NOT NULL,
    description VARCHAR(200),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2-4. 배치 감사 원장 (CrBatchAudit)
CREATE TABLE IF NOT EXISTS cr_batch_audits (
    id SERIAL PRIMARY KEY,
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

-- 2-5. 거시경제 시나리오 마스터 (CrMacroScenario)
CREATE TABLE IF NOT EXISTS cr_macro_scenario (
    id SERIAL PRIMARY KEY,
    apply_year INTEGER NOT NULL,
    scenario_type VARCHAR(20) NOT NULL, -- BOOM, BASE, RECESSION
    probability_weight NUMERIC(5,4) NOT NULL,
    pd_adjustment_factor NUMERIC(7,4) NOT NULL, -- Z-factor
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2-6. 고객 신용등급 변동 이력 (CrCustomerRatingHistory)
CREATE TABLE IF NOT EXISTS cr_cust_rating_hist (
    id SERIAL PRIMARY KEY,
    cust_id BIGINT REFERENCES cr_customers(id),
    rating_cd VARCHAR(10) NOT NULL,
    base_dt DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4-1. SA 위험가중치 마스터 (CrSaRwMaster)
CREATE TABLE IF NOT EXISTS cr_sa_rw_masters (
    id SERIAL PRIMARY KEY,
    customer_type VARCHAR(30) NOT NULL,
    rating_code VARCHAR(10) NOT NULL,
    risk_weight NUMERIC(5,4) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_type, rating_code)
);

-- 6-1. 월간 리스크 요약 마트 (Monthly Risk Summary Mart)
CREATE TABLE IF NOT EXISTS cr_monthly_summaries (
    id SERIAL PRIMARY KEY,
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
    avg_pd NUMERIC(10,8),
    avg_lgd NUMERIC(10,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS allowance_account_mappings (
    id SERIAL PRIMARY KEY,
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
    id SERIAL PRIMARY KEY,
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
    UNIQUE (base_date, run_id, legal_entity_code, currency_code, exposure_account_code, allowance_account_code)
);

CREATE INDEX IF NOT EXISTS idx_allowance_summary_base_date
    ON allowance_summary(base_date);

-- Initial Data
DELETE FROM cr_grade_masters;
INSERT INTO cr_grade_masters (rating_code, pd_value, notch_order, description) VALUES
('AAA',  0.0001,  1, '최상위 투자등급 (가장 우수)'),
('AA+',  0.0002,  2, '상위 투자등급'),
('AA',   0.0003,  3, '상위 투자등급'),
('AA-',  0.0004,  4, '상위 투자등급'),
('A+',   0.0005,  5, '투자등급'),
('A',    0.0007,  6, '투자등급'),
('A-',   0.0010,  7, '투자등급'),
('BBB+', 0.0015,  8, '중위 투자등급'),
('BBB',  0.0020,  9, '중위 투자등급'),
('BBB-', 0.0030, 10, '중위 투자등급'),
('BB+',  0.0050, 11, '투기등급 (위험 증가)'),
('BB',   0.0100, 12, '투기등급'),
('BB-',  0.0150, 13, '투기등급'),
('B+',   0.0250, 14, '고위험등급'),
('B',    0.0500, 15, '고위험등급'),
('B-',   0.0750, 16, '고위험등급'),
('CCC',  0.1500, 17, '부도위험 (매우 취약)'),
('CC',   0.2500, 18, '부도위험'),
('C',    0.5000, 19, '부도위험 (회수가능성 희박)'),
('D',    1.0000, 20, '부도상태 (Default)');

DELETE FROM cr_lgd_segment_masters;
INSERT INTO cr_lgd_segment_masters (segment_name, customer_type, collateral_type, lgd_value) VALUES
('기업_부동산담보',     'CORPORATE', 'REAL_ESTATE', 0.20),
('기업_무담보',         'CORPORATE', 'UNSECURED',   0.45),
('기업_예금담보',       'CORPORATE', 'CASH',        0.05),
('기업_보증',           'CORPORATE', 'GUARANTEE',   0.35),
('소매_주택담보',       'RETAIL',    'RESIDENTIAL', 0.15),
('소매_무담보',         'RETAIL',    'UNSECURED',   0.75),
('소매_예금담보',       'RETAIL',    'CASH',        0.05),
('중소기업_부동산담보', 'SME',       'REAL_ESTATE', 0.25),
('중소기업_무담보',     'SME',       'UNSECURED',   0.50),
('금융기관_무담보',     'FINANCIAL_INSTITUTION', 'UNSECURED', 0.45);

DELETE FROM cr_regulatory_parameters;
INSERT INTO cr_regulatory_parameters (param_key, param_value, description) VALUES
('PD_FLOOR',            0.0005,     '부도확률 하한선 (바젤 IV: 0.05%)'),
('ASSET_CORR_BASE',     0.12,       '기업 자산상관계수 하한 가중치 (12%)'),
('ASSET_CORR_HIGH',     0.24,       '기업 자산상관계수 상한 가중치 (24%)'),
('RETAIL_CORR_BASE',    0.03,       '소매 자산상관계수 하한 (3%)'),
('RETAIL_CORR_HIGH',    0.16,       '소매 자산상관계수 상한 (16%)'),
('MATURITY_ADJ_CONST',  0.11852,    '만기조정 산식 상수 1'),
('MATURITY_ADJ_CONST2', 0.05478,    '만기조정 산식 상수 2'),
('SME_SIZE_THRESHOLD',  50.0,       'SME 매출액 보정 상한 (50억)'),
('SECURED_LGD_FLOOR',   0.20,       '담보부 LGD 하한 (FIRB: 20%)'),
('UNSECURED_LGD_FLOOR', 0.45,       '무담보 LGD 기준값 (FIRB: 45%)'),
('FI_CORR_MULTIPLIER',  1.25,       '금융기관 상관계수 배수 (바젤 III: 125%)');

DELETE FROM cr_sa_rw_masters;
INSERT INTO cr_sa_rw_masters (customer_type, rating_code, risk_weight, description) VALUES
('RETAIL', 'ALL', 0.7500, '소매 익스포저 일괄 75%'),
('SME', 'ALL', 0.8500, '중소기업 익스포저 일괄 85%'),
('CORPORATE', 'AAA',  0.2000, '기업 우량등급'),
('CORPORATE', 'AA+',  0.2000, '기업 우량등급'),
('CORPORATE', 'AA',   0.2000, '기업 우량등급'),
('CORPORATE', 'AA-',  0.2000, '기업 우량등급'),
('CORPORATE', 'A+',   0.5000, '기업 양호등급'),
('CORPORATE', 'A',    0.5000, '기업 양호등급'),
('CORPORATE', 'A-',   0.5000, '기업 양호등급'),
('CORPORATE', 'BBB+', 0.7500, '기업 보통등급'),
('CORPORATE', 'BBB',  0.7500, '기업 보통등급'),
('CORPORATE', 'BBB-', 0.7500, '기업 보통등급'),
('CORPORATE', 'BB+',  1.0000, '기업 취약등급'),
('CORPORATE', 'BB',   1.0000, '기업 취약등급'),
('CORPORATE', 'BB-',  1.0000, '기업 취약등급'),
('CORPORATE', 'B+',   1.0000, '기업 취약등급'),
('CORPORATE', 'B',    1.0000, '기업 취약등급'),
('CORPORATE', 'B-',   1.0000, '기업 취약등급'),
('CORPORATE', 'CCC',  1.5000, '기업 고위험등급'),
('CORPORATE', 'CC',   1.5000, '기업 고위험등급'),
('CORPORATE', 'C',    1.5000, '기업 고위험등급'),
('CORPORATE', 'D',    1.5000, '기업 부도등급'),
('CORPORATE', 'UNRATED', 1.0000, '기업 무등급(기본)'),
('FINANCIAL_INSTITUTION', 'AAA',  0.2000, '은행 우량등급'),
('FINANCIAL_INSTITUTION', 'AA+',  0.2000, '은행 우량등급'),
('FINANCIAL_INSTITUTION', 'AA',   0.2000, '은행 우량등급'),
('FINANCIAL_INSTITUTION', 'AA-',  0.2000, '은행 우량등급'),
('FINANCIAL_INSTITUTION', 'A+',   0.3000, '은행 양호등급'),
('FINANCIAL_INSTITUTION', 'A',    0.3000, '은행 양호등급'),
('FINANCIAL_INSTITUTION', 'A-',   0.3000, '은행 양호등급'),
('FINANCIAL_INSTITUTION', 'BBB+', 0.5000, '은행 보통등급'),
('FINANCIAL_INSTITUTION', 'BBB',  0.5000, '은행 보통등급'),
('FINANCIAL_INSTITUTION', 'BBB-', 0.5000, '은행 보통등급'),
('FINANCIAL_INSTITUTION', 'UNRATED', 0.5000, '은행 무등급(기본)'),
('PUBLIC_SECTOR', 'ALL', 0.0000, '공공기관/정부/국책은행 익스포저 무위험');

DELETE FROM allowance_account_mappings;
INSERT INTO allowance_account_mappings (
    product_code,
    legal_entity_code,
    exposure_account_code,
    allowance_account_code,
    bad_debt_expense_account_code,
    reversal_income_account_code
) VALUES
('LN-CORP', 'DEFAULT', '110101', '110191', '550101', '450101'),
('LN-MTG',  'DEFAULT', '110102', '110192', '550101', '450101'),
('LN-RETL', 'DEFAULT', '110103', '110193', '550101', '450101'),
('LC-CORP', 'DEFAULT', '110104', '110194', '550101', '450101'),
('OD-CORP', 'DEFAULT', '110105', '110195', '550101', '450101');

-- Sample Data
DELETE FROM cr_customers;
INSERT INTO cr_customers (id, customer_code, customer_name, customer_type, internal_rating, industry_code, country_code, is_sme) VALUES
(1, 'CUST-001', '(주)삼성전자', 'CORPORATE', 'AAA', 'C262', 'KR', FALSE),
(2, 'CUST-002', '(주)현대자동차', 'CORPORATE', 'AA+', 'C301', 'KR', FALSE),
(3, 'CUST-003', '홍길동', 'RETAIL', 'A-', NULL, 'KR', FALSE),
(4, 'CUST-004', '강남IT자영업', 'SME', 'BB+', 'J620', 'KR', TRUE),
(5, 'CUST-005', '한국은행', 'PUBLIC_SECTOR', 'AAA', NULL, 'KR', FALSE);

DELETE FROM cr_product_masters;
INSERT INTO cr_product_masters (product_code, product_name, ccf_rate, standard_rw) VALUES
('LN-CORP', '기업일반대출', 0.0000, 1.0000),
('LN-MTG',  '주택담보대출', 0.0000, 0.3500),
('LN-RETL', '소매신용대출', 0.0000, 0.7500),
('LC-CORP', '기업수입신용장', 0.2000, 1.0000),
('OD-CORP', '기업한도대출(마이너스)', 0.5000, 1.0000);

DELETE FROM cr_accounts;
INSERT INTO cr_accounts (id, customer_id, account_no, product_code, outstanding_amt, notional_amt, currency, open_date, maturity_date, delinquent_days, staging) VALUES
(1, 1, 'ACC-SM-001', 'LN-CORP', 1000000000.00, 1000000000.00, 'KRW', '2024-01-01', '2027-01-01', 0, 'STAGE1'),
(2, 1, 'ACC-SM-002', 'OD-CORP', 500000000.00, 1000000000.00, 'KRW', '2024-06-01', '2025-06-01', 0, 'STAGE1'),
(3, 2, 'ACC-HD-001', 'LN-CORP', 2000000000.00, 2000000000.00, 'KRW', '2023-01-01', '2028-01-01', 0, 'STAGE1'),
(4, 3, 'ACC-HK-001', 'LN-MTG',  300000000.00, 300000000.00, 'KRW', '2025-01-15', '2055-01-15', 5, 'STAGE1'),
(5, 4, 'ACC-GN-001', 'LN-RETL', 50000000.00, 50000000.00, 'KRW', '2025-10-01', '2026-10-01', 45, 'STAGE2'),
(6, 5, 'ACC-CB-001', 'LN-CORP', 5000000000.00, 5000000000.00, 'KRW', '2020-01-01', '2030-01-01', 0, 'STAGE1');

DELETE FROM cr_collaterals;
INSERT INTO cr_collaterals (id, customer_id, collateral_code, collateral_type, appraisal_amt, base_haircut, prior_lien_amt, ltv_limit, kb_market_price, district_code) VALUES
(1, 1, 'COL-RE-001', 'REAL_ESTATE', 1500000000.00, 0.1000, 200000000.00, 0.7000, 1600000000.00, '1168010100'),
(2, 3, 'COL-RE-002', 'RESIDENTIAL', 500000000.00,  0.1500, 0.00,         0.6000, 520000000.00,  '1165010100'),
(3, 4, 'COL-CS-001', 'CASH',        100000000.00,  0.0000, 0.00,         1.0000, NULL,          NULL);

DELETE FROM cr_account_collaterals;
INSERT INTO cr_account_collaterals (account_id, collateral_id, allocation_amount) VALUES
(1, 1, 800000000.00),
(5, 3, 30000000.00);

DELETE FROM cr_macro_scenario;
INSERT INTO cr_macro_scenario (apply_year, scenario_type, probability_weight, pd_adjustment_factor, description) VALUES
(2026, 'BOOM',      0.2000, 0.8000, '경제 호황기: PD 20% 감소'),
(2026, 'BASE',      0.5000, 1.0000, '평시 상태: 기본 PD 적용'),
(2026, 'RECESSION', 0.3000, 1.3000, '경제 침체기: PD 30% 증가');

DELETE FROM cr_cust_rating_hist;
INSERT INTO cr_cust_rating_hist (cust_id, rating_cd, base_dt) VALUES
(1, 'AAA', '2026-03-31'),
(2, 'AA+', '2026-03-31'),
(3, 'A-',  '2026-03-31'),
(4, 'A-',  '2026-03-31'),
(5, 'AAA', '2026-03-31');
