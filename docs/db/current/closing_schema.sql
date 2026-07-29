-- 결산/마감 (Closing) 스키마

-- 1. 회계 기간 (FiscalPeriod - basic 모듈)
-- NOTE: FiscalPeriod는 basic 모듈에 정의되어 있으며, 여기서 다시 정의하지 않습니다.

-- 2. 결산 캘린더
CREATE TABLE closing_calendars (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fiscal_year VARCHAR(4) NOT NULL,
    fiscal_period VARCHAR(2) NOT NULL,
    name VARCHAR(100),
    status VARCHAR(30) NOT NULL, -- OPEN, IN_PROGRESS, CLOSED, PERMANENTLY_CLOSED
    close_initiated_at TIMESTAMP,
    close_initiated_by VARCHAR(50),
    closed_at TIMESTAMP,
    closed_by VARCHAR(50),
    reopened_at TIMESTAMP,
    reopened_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    UNIQUE (fiscal_year, fiscal_period)
);

-- 3. 날짜별 EOD/BOD 상태
-- 실행 가능한 forward migration은 closing:core의 db/closing-migration/V50을 기준으로 합니다.
CREATE TABLE daily_closing_status (
    date DATE PRIMARY KEY,
    state VARCHAR(30) NOT NULL, -- OPEN, PRE_CLOSING, CLOSING_IN_PROGRESS, CLOSED, BOD_IN_PROGRESS
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(80) NOT NULL,
    prepared_at TIMESTAMP,
    prepared_by VARCHAR(80),
    closing_started_at TIMESTAMP,
    closing_started_by VARCHAR(80),
    closed_at TIMESTAMP,
    closed_by VARCHAR(80),
    bod_started_at TIMESTAMP,
    bod_started_by VARCHAR(80),
    opened_at TIMESTAMP,
    opened_by VARCHAR(80)
);

CREATE INDEX idx_daily_closing_status_state_date
    ON daily_closing_status (state, date);

-- 4. 결산 태스크
CREATE TABLE closing_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    closing_calendar_id BIGINT NOT NULL,
    task_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    task_order INT NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    due_date DATE,
    status VARCHAR(30) NOT NULL, -- PENDING, IN_PROGRESS, COMPLETED, FAILED
    completed_at TIMESTAMP,
    completed_by VARCHAR(50),
    completion_condition_json TEXT, -- JSONB for complex conditions
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (closing_calendar_id) REFERENCES closing_calendars(id),
    UNIQUE (closing_calendar_id, task_code)
);

-- 5. 결산 게이트 (승인 지점)
CREATE TABLE closing_gates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    closing_calendar_id BIGINT NOT NULL,
    gate_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(30) NOT NULL, -- PENDING, PASSED, FAILED
    passed_at TIMESTAMP,
    passed_by VARCHAR(50),
    check_condition_json TEXT, -- JSONB for complex conditions (e.g., all tasks of type X completed)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (closing_calendar_id) REFERENCES closing_calendars(id),
    UNIQUE (closing_calendar_id, gate_code)
);

-- 6. 기간 잠금
CREATE TABLE period_locks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL,
    lock_type VARCHAR(30) NOT NULL, -- SOFT_LOCK, HARD_LOCK
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    locked_by VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id),
    UNIQUE (fiscal_period_id, lock_type)
);

-- 7. 재오픈 승인 요청
CREATE TABLE reopen_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    requested_by VARCHAR(50) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL, -- PENDING, APPROVED, REJECTED
    approved_at TIMESTAMP,
    approved_by VARCHAR(50),
    impact_analysis TEXT, -- 재오픈 영향도 분석 보고서 내용
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id)
);

-- 8. 평가 배치
CREATE TABLE valuation_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL,
    valuation_type VARCHAR(50) NOT NULL, -- FX_VALUATION, FINANCIAL_INSTRUMENT_VALUATION
    run_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL, -- PENDING, RUNNING, COMPLETED, FAILED
    run_by VARCHAR(50) NOT NULL,
    report_link VARCHAR(255),
    generated_journal_entry_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id)
);

-- 9. 충당/손상 배치
CREATE TABLE provision_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL,
    provision_type VARCHAR(50) NOT NULL, -- ECL_PROVISION, WARRANTY_PROVISION
    run_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL, -- PENDING, RUNNING, COMPLETED, FAILED
    run_by VARCHAR(50) NOT NULL,
    report_link VARCHAR(255),
    generated_journal_entry_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id)
);

-- 10. 결산 조정
CREATE TABLE closing_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fiscal_period_id BIGINT NOT NULL,
    journal_entry_id BIGINT NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL, -- ACCRUAL, DEFERRAL, RECLASSIFICATION
    description VARCHAR(255),
    approved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id)
);
