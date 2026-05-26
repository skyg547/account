-- =================================================================================
-- Regulatory Extension Schema for Risk Data Mart
-- =================================================================================

\c market_data_db;

-- 1. Master Data Extensions
CREATE TABLE IF NOT EXISTS ods_biz_day (
    base_dt DATE PRIMARY KEY,
    is_biz_day BOOLEAN DEFAULT TRUE,
    holiday_nm VARCHAR(100),
    country_cd VARCHAR(3) DEFAULT 'KR'
);

CREATE TABLE IF NOT EXISTS ods_district_code (
    district_cd VARCHAR(10) PRIMARY KEY, -- 법정동코드
    province_nm VARCHAR(50), -- 시도
    city_nm VARCHAR(50),     -- 시군구
    district_nm VARCHAR(50), -- 읍면동
    area_type VARCHAR(20)    -- 투기과열지구, 조정대상지역 등 (LTV 산출용)
);

CREATE TABLE IF NOT EXISTS ods_base_rate (
    rate_cd VARCHAR(20) NOT NULL,
    base_dt DATE NOT NULL,
    rate_val NUMERIC(10,6) NOT NULL,
    PRIMARY KEY (rate_cd, base_dt)
);

-- 2. Loan Application & Customer Extensions
CREATE TABLE IF NOT EXISTS ods_loan_app_master (
    app_no VARCHAR(50) PRIMARY KEY,
    customer_code VARCHAR(50) REFERENCES ods_customer_mst(customer_code),
    app_dt DATE NOT NULL,
    app_amt NUMERIC(19,4),
    prod_cd VARCHAR(20),
    exam_result_cd VARCHAR(10), -- 심사결과코드 (정상, 거절, 조건부 등)
    exam_opinion TEXT
);

CREATE TABLE IF NOT EXISTS ods_guarantee_mst (
    guar_no VARCHAR(50) PRIMARY KEY,
    acc_no VARCHAR(50) REFERENCES ods_acc_ledger(acc_no),
    guar_org_cd VARCHAR(20), -- 보증기관 (주금공, 허그 등)
    guar_amt NUMERIC(19,4),
    guar_rate NUMERIC(5,4),
    expiry_dt DATE
);

-- 3. Advanced Credit Monitoring
CREATE TABLE IF NOT EXISTS ods_early_warning (
    base_dt DATE NOT NULL,
    customer_code VARCHAR(50) NOT NULL,
    warning_level VARCHAR(10), -- 정상, 관심, 주의, 심각
    warning_reason TEXT,
    warning_score INT,
    PRIMARY KEY (base_dt, customer_code)
);

CREATE TABLE IF NOT EXISTS ods_debt_restructuring (
    acc_no VARCHAR(50) PRIMARY KEY,
    restruct_dt DATE,
    restruct_type VARCHAR(20), -- 채권재조정, 프리워크아웃 등
    is_grace_period BOOLEAN DEFAULT FALSE
);

-- 4. Apartment Collateral Details
CREATE TABLE IF NOT EXISTS ods_apart_coll_detail (
    coll_id VARCHAR(50) PRIMARY KEY REFERENCES ods_collateral_mst(coll_id),
    district_cd VARCHAR(10) REFERENCES ods_district_code(district_cd),
    kb_market_price NUMERIC(19,4), -- KB 시세
    house_type VARCHAR(20), -- 아파트, 연립, 다세대
    exclusive_area NUMERIC(10,2), -- 전용면적
    floor_no INT,
    is_speculative_area BOOLEAN DEFAULT FALSE -- 투기지역 여부
);

-- 5. RWA Allocation Ledger (RDM Layer)
CREATE TABLE IF NOT EXISTS rdm_collateral_allocation_ledger (
    base_dt DATE NOT NULL,
    acc_no VARCHAR(50) NOT NULL,
    coll_id VARCHAR(50) NOT NULL,
    allocation_order INT NOT NULL, -- 배분 순위 (1순위, 2순위 등)
    allocated_amt NUMERIC(19,4) NOT NULL,
    applied_ltv NUMERIC(10,4),
    PRIMARY KEY (base_dt, acc_no, coll_id)
);
