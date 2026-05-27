-- =================================================================================
-- Advanced Risk Mart Schema (IFRS 9 & 국제 금융 규제)
-- =================================================================================

-- 1. 미래전망 거시경제 시나리오 (IFRS 9 Expected Credit Loss)
CREATE TABLE IF NOT EXISTS ods_macro_scenario (
    scenario_id VARCHAR(20) NOT NULL, -- BASE, OPTIMISTIC, PESSIMISTIC
    base_dt DATE NOT NULL,
    gdp_growth_rate NUMERIC(10,6),    -- GDP 성장률
    unemployment_rate NUMERIC(10,6), -- 실업률
    inflation_rate NUMERIC(10,6),    -- 소비자 물가 상승률
    interest_rate_avg NUMERIC(10,6), -- 평균 금리
    housing_price_index NUMERIC(10,6), -- 주택가격지수
    scenario_weight NUMERIC(5,4),    -- 시나리오별 가중치 (3:4:3 등)
    PRIMARY KEY (scenario_id, base_dt)
);

-- 2. 펀드/신탁 기초자산 상세 (Look-through Approach)
CREATE TABLE IF NOT EXISTS ods_fund_lookthrough (
    fund_cd VARCHAR(50) NOT NULL,
    asset_id VARCHAR(50) NOT NULL,
    asset_nm VARCHAR(100),
    asset_type VARCHAR(20), -- STOCK, BOND, LOAN, CASH
    issuer_rating VARCHAR(10),
    exposure_amt NUMERIC(19,4),
    weight NUMERIC(7,6), -- 펀드 내 비중
    PRIMARY KEY (fund_cd, asset_id)
);

-- 3. 신용등급 전이행렬 (Lifetime PD 산출용)
CREATE TABLE IF NOT EXISTS ods_rating_migration_matrix (
    matrix_version VARCHAR(20) NOT NULL,
    from_rating VARCHAR(10) NOT NULL,
    to_rating VARCHAR(10) NOT NULL,
    migration_prob NUMERIC(10,8) NOT NULL, -- 전이 확률
    PRIMARY KEY (matrix_version, from_rating, to_rating)
);

-- 3-1. 신용등급 마스터
-- Enum 대신 DB에서 등급 체계와 점수 구간, PD를 관리합니다.
CREATE TABLE cr_grade_master (
    id BIGSERIAL PRIMARY KEY,
    grade_code VARCHAR(20) NOT NULL UNIQUE,
    grade_name VARCHAR(100) NOT NULL,
    score_min NUMERIC(10,4),
    score_max NUMERIC(10,4),
    pd_value NUMERIC(10,8) NOT NULL,
    sort_order INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO cr_grade_master (grade_code, grade_name, score_min, score_max, pd_value, sort_order, description) VALUES
('AAA', 'AAA', 95.0000, 100.0000, 0.00010000, 1, '최상위 투자등급'),
('AA+', 'AA+', 90.0000, 94.9999, 0.00020000, 2, '상위 투자등급'),
('AA', 'AA', 85.0000, 89.9999, 0.00030000, 3, '상위 투자등급'),
('AA-', 'AA-', 80.0000, 84.9999, 0.00040000, 4, '상위 투자등급'),
('A+', 'A+', 75.0000, 79.9999, 0.00050000, 5, '투자등급'),
('A', 'A', 70.0000, 74.9999, 0.00070000, 6, '투자등급'),
('A-', 'A-', 65.0000, 69.9999, 0.00100000, 7, '투자등급'),
('BBB+', 'BBB+', 60.0000, 64.9999, 0.00150000, 8, '중위 투자등급'),
('BBB', 'BBB', 55.0000, 59.9999, 0.00200000, 9, '중위 투자등급'),
('BBB-', 'BBB-', 50.0000, 54.9999, 0.00300000, 10, '중위 투자등급'),
('BB+', 'BB+', 45.0000, 49.9999, 0.00500000, 11, '투기등급'),
('BB', 'BB', 40.0000, 44.9999, 0.01000000, 12, '투기등급'),
('BB-', 'BB-', 35.0000, 39.9999, 0.01500000, 13, '투기등급'),
('B+', 'B+', 30.0000, 34.9999, 0.02500000, 14, '고위험등급'),
('B', 'B', 25.0000, 29.9999, 0.05000000, 15, '고위험등급'),
('B-', 'B-', 20.0000, 24.9999, 0.07500000, 16, '고위험등급'),
('CCC', 'CCC', 15.0000, 19.9999, 0.15000000, 17, '부도위험'),
('CC', 'CC', 10.0000, 14.9999, 0.25000000, 18, '부도위험'),
('C', 'C', 5.0000, 9.9999, 0.50000000, 19, '부도임박'),
('D', 'D', 0.0000, 4.9999, 1.00000000, 20, '부도'),
('NR', '무등급', NULL, NULL, 0.10000000, 99, '미평가 또는 매핑 실패');

-- 4. 리스크 데이터 품질 검증 규칙 (DQ Rule Master)
CREATE TABLE IF NOT EXISTS cdm_dq_rule_master (
    rule_id VARCHAR(20) PRIMARY KEY,
    rule_nm VARCHAR(100) NOT NULL,
    target_table VARCHAR(50),
    target_column VARCHAR(50),
    check_type VARCHAR(20), -- NULL, RANGE, FORMAT, CODE, LOGIC
    min_val NUMERIC(19,4),
    max_val NUMERIC(19,4),
    regex_pattern TEXT,
    is_active BOOLEAN DEFAULT TRUE
);

-- 5. 대손충당금(IFRS9) 산출 배치 세션 관리
CREATE TABLE IF NOT EXISTS risk_calculation_session (
    session_id BIGSERIAL PRIMARY KEY,
    base_dt DATE NOT NULL,
    run_type VARCHAR(20), -- REGULAR, SIMULATION, AD-HOC
    status VARCHAR(20),    -- RUNNING, COMPLETED, FAILED
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    operator_id VARCHAR(20)
);
