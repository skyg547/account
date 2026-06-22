-- V30: Loan accounting schema after Loan/contract model consolidation.
-- Cross-module references such as journal_entries, business_partners, currencies,
-- and account_subjects are stored as IDs/codes here but are not FK-constrained in
-- this module migration because Flyway executes module resources by classpath order.

CREATE TABLE IF NOT EXISTS loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_number VARCHAR(50) NOT NULL UNIQUE,
    business_partner_id BIGINT NOT NULL,
    loan_product VARCHAR(100),
    loan_type VARCHAR(50) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    principal_amount DECIMAL(19, 2) NOT NULL,
    interest_rate DECIMAL(5, 4) NOT NULL,
    disbursal_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    payment_frequency VARCHAR(30) NOT NULL,
    repayment_method VARCHAR(50),
    initial_eir DECIMAL(5, 4),
    current_eir DECIMAL(5, 4),
    status VARCHAR(30) NOT NULL,
    current_principal_balance DECIMAL(19, 2),
    deferred_loan_fee DECIMAL(19, 2),
    total_interest_paid DECIMAL(19, 2) DEFAULT 0,
    total_principal_paid DECIMAL(19, 2) DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    audit_user VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS deferred_item_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    deferral_method VARCHAR(50) NOT NULL,
    eir_cash_flow_treatment VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER_FEE_INFLOW',
    deferred_asset_account_code BIGINT,
    recognized_income_account_code BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME,
    audit_user VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS loan_disbursals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    disbursal_date DATE NOT NULL,
    disbursed_amount DECIMAL(19, 2) NOT NULL,
    journal_entry_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    audit_user VARCHAR(50),
    CONSTRAINT fk_loan_disbursals_loan
        FOREIGN KEY (loan_id) REFERENCES loans(id)
);

CREATE TABLE IF NOT EXISTS deferred_items (
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
    created_at DATETIME,
    updated_at DATETIME,
    audit_user VARCHAR(50),
    CONSTRAINT fk_deferred_items_loan
        FOREIGN KEY (loan_id) REFERENCES loans(id),
    CONSTRAINT fk_deferred_items_type
        FOREIGN KEY (deferred_item_type_id) REFERENCES deferred_item_types(id)
);

CREATE TABLE IF NOT EXISTS eir_amortization_schedules (
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
    created_at DATETIME,
    updated_at DATETIME,
    audit_user VARCHAR(50),
    CONSTRAINT fk_eir_schedules_loan
        FOREIGN KEY (loan_id) REFERENCES loans(id)
);

CREATE TABLE IF NOT EXISTS recalculation_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    recalculation_date DATE NOT NULL,
    reason VARCHAR(50) NOT NULL,
    old_eir DECIMAL(5, 4),
    new_eir DECIMAL(5, 4),
    old_maturity_date DATE,
    new_maturity_date DATE,
    recalculated_amortization_schedule_start_id BIGINT,
    impact_analysis TEXT,
    adjustment_journal_entry_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    audit_user VARCHAR(50),
    CONSTRAINT fk_recalculation_runs_loan
        FOREIGN KEY (loan_id) REFERENCES loans(id),
    CONSTRAINT fk_recalculation_runs_schedule
        FOREIGN KEY (recalculated_amortization_schedule_start_id)
        REFERENCES eir_amortization_schedules(id)
);

CREATE TABLE IF NOT EXISTS loan_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_date DATE NOT NULL,
    description VARCHAR(1000),
    journal_entry_id BIGINT,
    recalculation_run_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    audit_user VARCHAR(50),
    CONSTRAINT fk_loan_events_loan
        FOREIGN KEY (loan_id) REFERENCES loans(id),
    CONSTRAINT fk_loan_events_recalculation_run
        FOREIGN KEY (recalculation_run_id) REFERENCES recalculation_runs(id)
);

CREATE TABLE IF NOT EXISTS loan_amortization_schedule_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    period_number INT NOT NULL,
    starting_balance DECIMAL(19, 2) NOT NULL,
    scheduled_payment_amount DECIMAL(19, 2) NOT NULL,
    interest_amount DECIMAL(19, 2) NOT NULL,
    principal_amount DECIMAL(19, 2) NOT NULL,
    ending_balance DECIMAL(19, 2) NOT NULL,
    deferred_fee_amortization DECIMAL(19, 2),
    entry_type VARCHAR(50),
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_loan_schedule_entries_loan
        FOREIGN KEY (loan_id) REFERENCES loans(id)
);

CREATE TABLE IF NOT EXISTS loan_accrual_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    accrual_date DATE NOT NULL,
    loan_id BIGINT NOT NULL,
    accrued_amount DECIMAL(19, 2) NOT NULL,
    journal_no VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audit_user VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_loan_accrual_log_loan
        FOREIGN KEY (loan_id) REFERENCES loans(id)
);
