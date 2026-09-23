-- Local H2 baseline reference data. Flyway creates the schema before this script runs.

INSERT INTO currencies (
    currency_code, currency_name, valid_from, valid_to, created_at, updated_at
) VALUES
    ('KRW', '원화', DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('USD', '달러', DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('EUR', '유로', DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW());

INSERT INTO departments (
    code, name, type, valid_from, valid_to, created_at, updated_at
) VALUES
    ('DEPT_FINANCE', '재무팀', 'COST_CENTER', DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('DEPT_ACCOUNTING', '회계팀', 'COST_CENTER', DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('DEPT_MGMT', '경영관리팀', 'PROFIT_CENTER', DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW());

INSERT INTO account_subjects (
    code, name, account_type, balance_type, category, fixed_asset, unsettled,
    valid_from, valid_to, created_at, updated_at
) VALUES
    ('10100', '현금', 'ASSETS', 'DEBIT', 'ASSETS', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('10200', '보통예금', 'ASSETS', 'DEBIT', 'ASSETS', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('20100', '외상매입금', 'LIABILITIES', 'CREDIT', 'LIABILITIES', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('20200', '미지급금', 'LIABILITIES', 'CREDIT', 'LIABILITIES', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('30100', '자본금', 'EQUITY', 'CREDIT', 'EQUITY', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('40100', '매출액', 'REVENUE', 'CREDIT', 'REVENUE', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('50100', '급여', 'EXPENSES', 'DEBIT', 'EXPENSES', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('50200', '복리후생비', 'EXPENSES', 'DEBIT', 'EXPENSES', FALSE, FALSE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW());

INSERT INTO business_partners (
    business_partner_code, business_partner_name, partner_type, risk_rating,
    kyc_status, use_yn, valid_from, valid_to, created_at, updated_at
) VALUES
    ('BP_SHINHAN', '신한은행', 'BANK', 'LOW', 'APPROVED', TRUE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('BP_SAMSUNG', '삼성전자', 'CUSTOMER', 'LOW', 'APPROVED', TRUE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW()),
    ('BP_COUPANG', '쿠팡', 'VENDOR', 'LOW', 'APPROVED', TRUE, DATE '2020-01-01', DATE '9999-12-31', NOW(), NOW());

INSERT INTO fiscal_periods (
    fiscal_year, fiscal_period, start_date, end_date, closing_status, created_at, updated_at
) VALUES
    ('2026', '01', DATE '2026-01-01', DATE '2026-01-31', 'OPEN', NOW(), NOW()),
    ('2026', '02', DATE '2026-02-01', DATE '2026-02-28', 'OPEN', NOW(), NOW()),
    ('2026', '03', DATE '2026-03-01', DATE '2026-03-31', 'OPEN', NOW(), NOW()),
    ('2026', '04', DATE '2026-04-01', DATE '2026-04-30', 'OPEN', NOW(), NOW()),
    ('2026', '05', DATE '2026-05-01', DATE '2026-05-31', 'OPEN', NOW(), NOW()),
    ('2026', '06', DATE '2026-06-01', DATE '2026-06-30', 'OPEN', NOW(), NOW()),
    ('2026', '07', DATE '2026-07-01', DATE '2026-07-31', 'OPEN', NOW(), NOW()),
    ('2026', '08', DATE '2026-08-01', DATE '2026-08-31', 'OPEN', NOW(), NOW()),
    ('2026', '09', DATE '2026-09-01', DATE '2026-09-30', 'OPEN', NOW(), NOW()),
    ('2026', '10', DATE '2026-10-01', DATE '2026-10-31', 'OPEN', NOW(), NOW()),
    ('2026', '11', DATE '2026-11-01', DATE '2026-11-30', 'OPEN', NOW(), NOW()),
    ('2026', '12', DATE '2026-12-01', DATE '2026-12-31', 'OPEN', NOW(), NOW());
