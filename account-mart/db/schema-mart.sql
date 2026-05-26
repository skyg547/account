-- =================================================================================
-- Risk Data Mart Service Schema (ODS & CDM) - v5.0 (IRRBB Enhancement)
-- 
-- 💡 [초보자를 위한 가이드]
-- 이 SQL 파일은 은행 원천 데이터(ODS)를 리스크 산출용 통합 데이터(CDM)로 변환하기 위한 저장소입니다.
-- v5.0 고도화: 금리리스크(IRRBB)의 정밀 산출을 위해 상환 스케줄, 금리 상/하한, 
-- 참조 인덱스 매핑 정보를 추가하여 데이터 품질을 극대화했습니다.
-- =================================================================================

\c market_data_db;

-- Section 1. ODS 원천 테이블
CREATE TABLE ods_account_mst (
    subj_cd VARCHAR(20) PRIMARY KEY,
    subj_nm VARCHAR(100) NOT NULL,
    acct_type VARCHAR(20) NOT NULL,
    bs_class VARCHAR(10) NOT NULL,
    is_asset BOOLEAN NOT NULL,
    biz_unit_cd VARCHAR(10)
);

CREATE TABLE ods_product_mst (
    prod_cd VARCHAR(20) PRIMARY KEY,
    prod_nm VARCHAR(100) NOT NULL,
    prod_category VARCHAR(30),
    subj_cd VARCHAR(20) REFERENCES ods_account_mst(subj_cd),
    default_rate_type VARCHAR(10),
    default_payment_freq INT,
    is_off_balance BOOLEAN DEFAULT FALSE,
    default_ccf NUMERIC(5,4)
);

CREATE TABLE ods_customer_mst (
    customer_code VARCHAR(50) PRIMARY KEY,
    cust_nm VARCHAR(100) NOT NULL,
    cust_type VARCHAR(20) NOT NULL,
    biz_no VARCHAR(20),
    industry_cd VARCHAR(20),
    industry_nm VARCHAR(100),
    internal_rating VARCHAR(10),
    external_rating VARCHAR(10),
    rating_dt DATE,
    country_cd VARCHAR(3) DEFAULT 'KR',
    is_sme BOOLEAN DEFAULT FALSE,
    branch_cd VARCHAR(10),
    credit_status_cd VARCHAR(10) DEFAULT 'NORMAL'
);

CREATE TABLE ods_acc_ledger (
    acc_no VARCHAR(50) PRIMARY KEY,
    customer_code VARCHAR(50) REFERENCES ods_customer_mst(customer_code),
    subj_cd VARCHAR(20) REFERENCES ods_account_mst(subj_cd),
    prod_cd VARCHAR(20) REFERENCES ods_product_mst(prod_cd),
    currency VARCHAR(3) DEFAULT 'KRW',
    limit_amt NUMERIC(19,4),
    outstd_amt NUMERIC(19,4),
    open_dt DATE,
    maturity_dt DATE,
    close_dt DATE,
    delinquent_days INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    branch_cd VARCHAR(10),
    dept_cd VARCHAR(10),
    biz_unit_cd VARCHAR(10),
    int_rate NUMERIC(10,6),
    base_rate_cd VARCHAR(20),
    spread NUMERIC(10,6),
    next_reset_dt DATE,
    repayment_method VARCHAR(20),
    -- [v5.0 추가] 현금흐름 정밀 생성을 위한 상환 정보
    grace_period INTEGER DEFAULT 0,
    repayment_freq INTEGER DEFAULT 1,
    overdraft_limit NUMERIC(19,4) DEFAULT 0
);

CREATE TABLE ods_balance_hist (
    base_dt DATE NOT NULL,
    acc_no VARCHAR(50) NOT NULL,
    cur_bal NUMERIC(19,4) NOT NULL,
    avg_bal_mtd NUMERIC(19,4),
    int_accrued NUMERIC(19,4),
    fx_rate NUMERIC(15,6) DEFAULT 1.0,
    valuation_amt_lcy NUMERIC(19,4),
    PRIMARY KEY (base_dt, acc_no)
) PARTITION BY RANGE (base_dt);

CREATE TABLE ods_balance_hist_2026m03 PARTITION OF ods_balance_hist FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE ods_balance_hist_2026m04 PARTITION OF ods_balance_hist FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE ods_rate_info (
    acc_no VARCHAR(50) PRIMARY KEY,
    rate_type VARCHAR(10) NOT NULL,
    base_rate_cd VARCHAR(20),
    spread NUMERIC(10,6),
    applied_rate NUMERIC(10,6),
    coupon_rate NUMERIC(10,6),
    payment_freq INT,
    rate_reset_cycle INT,
    last_reset_dt DATE,
    next_reset_dt DATE,
    interest_calc_method VARCHAR(20),
    -- [v5.0 추가] ΔNII 및 멀티커브 정밀 산출 정보
    int_rate_floor NUMERIC(10,6),
    int_rate_cap NUMERIC(10,6),
    ref_index_cd VARCHAR(20)
);

CREATE TABLE ods_collateral_mst (
    coll_id VARCHAR(50) PRIMARY KEY,
    acc_no VARCHAR(50) NOT NULL,
    coll_type VARCHAR(20),
    coll_detail VARCHAR(50),
    coll_amt NUMERIC(19,4),
    pledge_rate NUMERIC(5,4),
    recognized_amt NUMERIC(19,4),
    appraisal_dt DATE,
    expiry_dt DATE
);

-- Section 1.5 행동 모델링용 이력 테이블 (NEW)
-- 💡 [초보자를 위한 가이드]
-- 리스크 시스템의 '기억 장치'입니다. 
-- 통계적으로 고객이 언제 돈을 뺄지(NMD), 언제 대출을 미리 갚을지(CPR)를 예측하려면 
-- 최소 3~5년치 과거 데이터가 필요합니다. 이 테이블은 그 분석을 위한 시계열 데이터를 저장합니다.
CREATE TABLE ods_behavioral_history (
    id BIGSERIAL PRIMARY KEY,
    base_dt DATE NOT NULL,
    acc_no VARCHAR(50) NOT NULL,
    event_type VARCHAR(20) NOT NULL, -- SNAPSHOT, PREPAYMENT, WITHDRAWAL
    balance_amt NUMERIC(19,4),
    event_amt NUMERIC(19,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bh_acc_date ON ods_behavioral_history(acc_no, base_dt);

CREATE TABLE ods_general_ledger (
    base_dt DATE NOT NULL,
    subj_cd VARCHAR(20) NOT NULL,
    br_cd VARCHAR(10) NOT NULL,
    curr_cd VARCHAR(3) NOT NULL,
    dr_bal NUMERIC(19,4) DEFAULT 0,
    cr_bal NUMERIC(19,4) DEFAULT 0,
    net_bal NUMERIC(19,4) DEFAULT 0,
    PRIMARY KEY (base_dt, subj_cd, br_cd, curr_cd)
);

-- Section 2. 대사 및 품질 감사 테이블
CREATE TABLE ods_reconcile_hist (
    id BIGSERIAL PRIMARY KEY,
    base_dt DATE NOT NULL,
    recon_type VARCHAR(30) NOT NULL,
    subj_cd VARCHAR(20),
    curr_cd VARCHAR(3),
    gl_amt NUMERIC(19,4) DEFAULT 0,
    sl_amt NUMERIC(19,4) DEFAULT 0,
    diff_amt NUMERIC(19,4) DEFAULT 0,
    gl_cnt INT DEFAULT 0,
    sl_cnt INT DEFAULT 0,
    status VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ods_dq_audit (
    id BIGSERIAL PRIMARY KEY,
    base_dt DATE NOT NULL,
    table_name VARCHAR(50),
    acc_no VARCHAR(50),
    error_type VARCHAR(30),
    error_msg TEXT,
    severity VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Section 4. 통합 리스크 마트 (Integrated Risk Mart - CDM)
CREATE TABLE dim_integrated_position_master (
    base_dt DATE NOT NULL,
    acc_no VARCHAR(50) NOT NULL,
    customer_code VARCHAR(50) NOT NULL,
    cust_type VARCHAR(20),
    is_sme BOOLEAN DEFAULT FALSE,
    country_cd VARCHAR(10),
    prod_cd VARCHAR(20),
    prod_category VARCHAR(30),
    currency VARCHAR(3) NOT NULL,
    bs_class VARCHAR(10),
    cur_bal NUMERIC(19,4),
    limit_amt NUMERIC(19,4),
    outstd_amt NUMERIC(19,4),
    market_val NUMERIC(19,4),
    int_rate NUMERIC(10,6),
    coupon_rate NUMERIC(10,6),
    rate_type VARCHAR(10),
    base_rate_cd VARCHAR(20),
    spread NUMERIC(10,6),
    payment_freq INT,
    next_reset_dt DATE,
    open_dt DATE,
    maturity_dt DATE,
    -- [v5.0 추가] 현금흐름 및 금리조건 정밀 정보 연동
    repayment_method VARCHAR(20),
    grace_period INTEGER,
    repayment_freq INTEGER,
    int_rate_floor NUMERIC(10,6),
    int_rate_cap NUMERIC(10,6),
    ref_index_cd VARCHAR(20),
    internal_rating VARCHAR(10),
    external_rating VARCHAR(10),
    industry_cd VARCHAR(20),
    warning_level VARCHAR(20),
    is_debt_restructured BOOLEAN DEFAULT FALSE,
    delinquent_days INTEGER DEFAULT 0,
    staging VARCHAR(20),
    coll_amt NUMERIC(19,4),
    recognized_coll_amt NUMERIC(19,4),
    coll_type VARCHAR(20),
    pd NUMERIC(10,8),
    lgd NUMERIC(10,8),
    expected_loss NUMERIC(19,4),
    unexpected_loss NUMERIC(19,4),
    rwa_sa NUMERIC(19,4),
    rwa_irb NUMERIC(19,4),
    branch_cd VARCHAR(10),
    biz_unit_cd VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (base_dt, acc_no)
) PARTITION BY RANGE (base_dt);

CREATE TABLE dim_integrated_position_master_2026m04 PARTITION OF dim_integrated_position_master 
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- Section 2.5 배치 감사 원장
CREATE TABLE batch_job_audit (
    job_id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(50) NOT NULL,
    base_dt DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    target_cnt INT DEFAULT 0,
    success_cnt INT DEFAULT 0,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP
);

-- Section 5. 인덱스
CREATE INDEX idx_mart_base_dt ON dim_integrated_position_master(base_dt);
CREATE INDEX idx_mart_acc_no ON dim_integrated_position_master(acc_no);
CREATE INDEX idx_recon_base_dt ON ods_reconcile_hist(base_dt);
CREATE INDEX idx_dq_base_dt ON ods_dq_audit(base_dt);
