-- V1: 고정자산 및 리스 회계 초기 스키마 (IFRS 고도화 반영)

-- 1. 고정자산 마스터
CREATE TABLE fixed_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_code VARCHAR(20) NOT NULL UNIQUE,
    asset_name VARCHAR(100) NOT NULL,
    account_code VARCHAR(20) NOT NULL,
    accumulated_account_code VARCHAR(20),
    expense_account_code VARCHAR(20),
    dept_code VARCHAR(20),
    acquisition_date DATE NOT NULL,
    acquisition_cost DECIMAL(19, 2) NOT NULL,
    useful_life INT NOT NULL,
    depreciation_method VARCHAR(20),
    residual_value DECIMAL(19, 2) DEFAULT 0,
    accumulated_depreciation DECIMAL(19, 2) DEFAULT 0,
    accumulated_impairment DECIMAL(19, 2) DEFAULT 0, -- [고도화] 손상차손 누계액 추가
    revaluation_surplus DECIMAL(19, 2) DEFAULT 0,    -- [고도화] 재평가 잉여금 추가
    current_book_value DECIMAL(19, 2) NOT NULL,
    depreciation_amount_per_period DECIMAL(19, 2),
    last_depreciation_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 자산 이력 (Audit Trail)
CREATE TABLE asset_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    history_type VARCHAR(50) NOT NULL,
    old_dept_code VARCHAR(20),
    new_dept_code VARCHAR(20),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    description TEXT,
    event_at DATETIME NOT NULL,
    audit_user VARCHAR(50) NOT NULL,
    FOREIGN KEY (asset_id) REFERENCES fixed_assets(id)
);

-- 3. 리스 계약
CREATE TABLE lease_contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_no VARCHAR(50) NOT NULL UNIQUE,
    contract_name VARCHAR(100) NOT NULL,
    lessor_code VARCHAR(20),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_payment DECIMAL(19, 2) NOT NULL,
    discount_rate DECIMAL(5, 2) NOT NULL,
    initial_rou_value DECIMAL(19, 2),
    initial_lease_liability_value DECIMAL(19, 2),
    status VARCHAR(20),
    dept_code VARCHAR(20),
    ifrs16_applicable BOOLEAN DEFAULT TRUE
);

-- 4. 리스 상환 스케줄
CREATE TABLE lease_payment_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    scheduled_payment_amount DECIMAL(19, 2),
    interest_portion DECIMAL(19, 2),
    principal_portion DECIMAL(19, 2),
    remaining_lease_liability DECIMAL(19, 2),
    status VARCHAR(20),
    FOREIGN KEY (contract_id) REFERENCES lease_contracts(id)
);
