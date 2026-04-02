-- 대출 회계 (Loan Accounting) 스키마

-- 1. 대출 이연 항목 유형
CREATE TABLE deferred_item_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    deferral_method VARCHAR(30) NOT NULL, -- EIR_METHOD, STRAIGHT_LINE
    deferred_asset_account_code VARCHAR(20) NOT NULL,
    recognized_income_account_code VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 2. 대출 계약
CREATE TABLE loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_number VARCHAR(50) NOT NULL UNIQUE,
    business_partner_id BIGINT NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    principal_amount DECIMAL(19, 2) NOT NULL,
    interest_rate DECIMAL(5, 4) NOT NULL,
    disbursal_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    payment_frequency VARCHAR(30) NOT NULL,
    initial_eir DECIMAL(5, 4),
    current_eir DECIMAL(5, 4),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50)
);

-- 3. 대출 실행 기록
CREATE TABLE loan_disbursals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    disbursal_date DATE NOT NULL,
    disbursed_amount DECIMAL(19, 2) NOT NULL,
    journal_entry_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (loan_id) REFERENCES loans(id)
);

-- 4. 이연 항목 (수수료, 비용 등)
CREATE TABLE deferred_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    deferred_item_type_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    deferral_date DATE NOT NULL,
    amortization_start_date DATE NOT NULL,
    amortization_end_date DATE NOT NULL,
    remaining_amount DECIMAL(19, 2) NOT NULL,
    initial_journal_entry_id BIGINT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (deferred_item_type_id) REFERENCES deferred_item_types(id)
);

-- 5. EIR 상각 스케줄
CREATE TABLE eir_amortization_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    beginning_balance DECIMAL(19, 2) NOT NULL,
    interest_income DECIMAL(19, 2) NOT NULL,
    principal_repayment DECIMAL(19, 2) NOT NULL,
    ending_balance DECIMAL(19, 2) NOT NULL,
    deferred_item_amortization DECIMAL(19, 2),
    cash_flow DECIMAL(19, 2),
    amortization_journal_entry_id BIGINT,
    is_recalculated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (loan_id) REFERENCES loans(id)
);

-- 6. 대출 이벤트 (중도상환, 조건변경 등)
CREATE TABLE loan_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_date DATE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (loan_id) REFERENCES loans(id)
);

-- 7. 재계산 실행 기록
CREATE TABLE recalculation_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    recalculation_date DATE NOT NULL,
    reason VARCHAR(50) NOT NULL,
    old_eir DECIMAL(5, 4),
    new_eir DECIMAL(5, 4),
    old_maturity_date DATE,
    new_maturity_date DATE,
    recalculated_amortization_schedule_start_id BIGINT,
    adjustment_journal_entry_id BIGINT,
    impact_analysis TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50),
    FOREIGN KEY (loan_id) REFERENCES loans(id)
);
