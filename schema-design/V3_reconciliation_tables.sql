CREATE TABLE reconciliation_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reconciliation_date DATE NOT NULL,
    reconciliation_type VARCHAR(50) NOT NULL,
    closing_period_id BIGINT,
    status VARCHAR(50) NOT NULL,
    total_count_source BIGINT NOT NULL,
    total_amount_source DECIMAL(19, 2) NOT NULL,
    total_count_target BIGINT NOT NULL,
    total_amount_target DECIMAL(19, 2) NOT NULL,
    variance_count BIGINT NOT NULL,
    variance_amount DECIMAL(19, 2) NOT NULL,
    run_by VARCHAR(50),
    run_at DATETIME,
    FOREIGN KEY (closing_period_id) REFERENCES closing_period(id)
);

CREATE TABLE reconciliation_variances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reconciliation_result_id BIGINT NOT NULL,
    variance_code VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    dr_cr_type VARCHAR(10),
    source_reference VARCHAR(255),
    target_reference VARCHAR(255),
    adjustment_journal_entry_id BIGINT,
    status VARCHAR(50) NOT NULL,
    resolved_by VARCHAR(50),
    resolved_at DATETIME,
    FOREIGN KEY (reconciliation_result_id) REFERENCES reconciliation_results(id),
    FOREIGN KEY (adjustment_journal_entry_id) REFERENCES journal_entry(id)
);
