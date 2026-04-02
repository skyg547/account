-- 재무보고/공시/감독보고 (Reporting) 스키마

-- 1. 보고 라인 매핑 (BS/IS/감독보고 라인과 계정 매핑)
CREATE TABLE rpt_line_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL, -- BS, IS, CF, REGULATORY
    line_code VARCHAR(50) NOT NULL,
    line_name VARCHAR(200) NOT NULL,
    account_code VARCHAR(20),
    aggregation_type VARCHAR(20) NOT NULL, -- SUM, DIFF, FORMULA
    formula_expression VARCHAR(500),
    display_order INT NOT NULL,
    parent_line_code VARCHAR(50),
    version INT NOT NULL DEFAULT 1,
    valid_from_date DATE NOT NULL,
    valid_to_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 2. 보고서 스냅샷 헤더 (생성된 보고서 이력)
CREATE TABLE rpt_snapshot_header (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    fiscal_year VARCHAR(4) NOT NULL,
    fiscal_period VARCHAR(2) NOT NULL,
    snapshot_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL, -- DRAFT, FINAL, SUBMITTED
    created_by VARCHAR(50),
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 3. 보고서 스냅샷 상세 (라인별 확정 금액)
CREATE TABLE rpt_snapshot_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    header_id BIGINT NOT NULL,
    line_code VARCHAR(50) NOT NULL,
    line_name VARCHAR(200) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    base_amount DECIMAL(19, 2),
    currency_code VARCHAR(3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (header_id) REFERENCES rpt_snapshot_header(id)
);

-- 4. 공시 마트 (주석 및 통계 데이터)
CREATE TABLE disclosure_mart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL, -- MATURITY, INTEREST_RATE, CURRENCY, RISK
    fiscal_year VARCHAR(4) NOT NULL,
    fiscal_period VARCHAR(2) NOT NULL,
    data_key VARCHAR(100) NOT NULL,
    data_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 5. 감독 보고 제출 이력
CREATE TABLE regulatory_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_type VARCHAR(50) NOT NULL,
    fiscal_year VARCHAR(4) NOT NULL,
    fiscal_period VARCHAR(2) NOT NULL,
    submitted_at TIMESTAMP,
    submitted_by VARCHAR(50),
    status VARCHAR(30) NOT NULL, -- SUCCESS, FAILED, PENDING
    response_msg TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);
