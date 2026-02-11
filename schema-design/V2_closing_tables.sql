CREATE TABLE closing_period (
    id BIGINT PRIMARY KEY, -- YYYYMM or YYYY (for annual)
    period_type VARCHAR(10) NOT NULL, -- e.g., 'MONTHLY', 'ANNUAL'
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- e.g., 'OPEN', 'IN_PROGRESS', 'PENDING_APPROVAL', 'CLOSED', 'REOPENED', 'FAILED'
    closed_at DATETIME,
    closed_by VARCHAR(50),
    reopened_at DATETIME,
    reopened_by VARCHAR(50),
    reopen_reason VARCHAR(255),
    approval_status VARCHAR(20), -- 'PENDING', 'APPROVED', 'REJECTED'
    approver VARCHAR(50),
    approval_date DATETIME
);

CREATE TABLE closing_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    closing_period_id BIGINT NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(20) NOT NULL, -- 'BATCH', 'MANUAL', 'REPORT'
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'SKIPPED'
    completed_at DATETIME,
    completed_by VARCHAR(50),
    sequence_order INT NOT NULL,
    failure_reason VARCHAR(255),
    FOREIGN KEY (closing_period_id) REFERENCES closing_period(id)
);
