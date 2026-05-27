-- =================================================================================
-- Credit Risk Service Schema (CR) - v4.5 (PostgreSQL & Entity Sync)
-- 
-- 💡 [초보자를 위한 가이드]
-- 이 SQL 파일은 Java 엔티티(CrCustomer, CrAccount 등)를 기준으로 작성된 물리 DB 설계도입니다.
-- PostgreSQL의 고유한 타입인 BIGSERIAL을 사용하여 대용량 데이터 환경에서도 안전하게 작동합니다.
-- =================================================================================

\c credit_risk_db;

-- ─────────────────────────────────────────────────────────────
-- 섹션 1: 핵심 비즈니스 테이블
-- ─────────────────────────────────────────────────────────────

-- 1-1. 대손충당금(IFRS9) 차주 마스터 (CrCustomer)
-- 💡 [정합성 보정] id 타입을 SERIAL에서 BIGSERIAL로 확장 (Long 매핑)
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
    fin_sector_cd VARCHAR(20), -- [v4.1 고도화] 금융업권 세부 코드
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
-- 💡 [정합성 보정] customer_id를 BIGINT로 선언하여 BIGSERIAL PK와 일치시킴
CREATE TABLE cr_accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES cr_customers(id),
    account_no VARCHAR(50) NOT NULL UNIQUE,
    product_code VARCHAR(20) NOT NULL,
    outstanding_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
    notional_amt NUMERIC(19,4) NOT NULL DEFAULT 0,
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
-- 💡 [정합성 보정] id BIGINT로 수동 채번 지원 및 컬럼 정밀도 보강
CREATE TABLE cr_risk_results (
    id BIGINT NOT NULL, -- 애플리케이션 수동 ID 할당 (Sequence 충돌 방지)
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

-- ─────────────────────────────────────────────────────────────
-- 섹션 2: IRB 및 공통 마스터 테이블
-- ─────────────────────────────────────────────────────────────

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

-- 2-4. 배치 감사 원장
CREATE TABLE cr_batch_audits (
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

-- 2-5. 거시경제 시나리오 마스터
CREATE TABLE cr_macro_scenario (
    id BIGSERIAL PRIMARY KEY,
    apply_year INTEGER NOT NULL,
    scenario_type VARCHAR(20) NOT NULL,
    probability_weight NUMERIC(10,6) NOT NULL,
    pd_adjustment_factor NUMERIC(10,6) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2-6. 고객 신용등급 변동 이력
CREATE TABLE cr_cust_rating_hist (
    id BIGSERIAL PRIMARY KEY,
    cust_id BIGINT NOT NULL REFERENCES cr_customers(id),
    rating_cd VARCHAR(10) NOT NULL,
    base_dt DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2-7. SA 위험가중치 마스터
CREATE TABLE cr_sa_rw_masters (
    id BIGSERIAL PRIMARY KEY,
    customer_type VARCHAR(30) NOT NULL,
    rating_code VARCHAR(10) NOT NULL,
    risk_weight NUMERIC(10,6) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_type, rating_code)
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

-- ─────────────────────────────────────────────────────────────
-- 섹션 3: 인덱스
-- ─────────────────────────────────────────────────────────────
CREATE INDEX idx_cr_acc_cust ON cr_accounts(customer_id);
CREATE INDEX idx_cr_res_date ON cr_risk_results(base_date);
CREATE INDEX idx_cr_res_acc ON cr_risk_results(account_id);
CREATE INDEX idx_cr_monthly_date ON cr_monthly_summaries(base_date);
