-- =================================================================================
-- Risk Data Mart (ODS/CDM) Mock Data v2.3 (Fixed Reconciliation)
-- For PostgreSQL - market_data_db
-- =================================================================================

\c market_data_db;

-- 1. 계정 과목 마스터 (ods_account_mst)
TRUNCATE ods_account_mst CASCADE;
INSERT INTO ods_account_mst (subj_cd, subj_nm, acct_type, bs_class, is_asset, biz_unit_cd) VALUES
('11100', '현금및예치금', 'CASH', 'ASSET', TRUE, 'CM'),
('12100', '원화대출금', 'LOAN', 'ASSET', TRUE, 'RETAIL'),
('12200', '외화대출금', 'LOAN', 'ASSET', TRUE, 'IB'),
('21100', '원화예수금', 'DEPOSIT', 'LIABILITY', FALSE, 'RETAIL'),
('21200', '외화예수금', 'DEPOSIT', 'LIABILITY', FALSE, 'IB');

-- 2. 상품 마스터 (ods_product_mst)
TRUNCATE ods_product_mst CASCADE;
INSERT INTO ods_product_mst (prod_cd, prod_nm, prod_category, subj_cd, default_rate_type, default_payment_freq, is_off_balance, default_ccf) VALUES
('P001', '가계주택담보대출', 'MORTGAGE', '12100', 'FIXED', 1, FALSE, 0.0),
('P002', '기업운전자금대출', 'CORP_LOAN', '12100', 'FLOATING', 3, FALSE, 0.0),
('P003', '정기예치금', 'TERM_DEP', '21100', 'FIXED', 0, FALSE, 0.0),
('P004', '보통예수금', 'DEMAND_DEP', '21100', 'FLOATING', 0, FALSE, 0.0),
('P005', '수입신용장', 'LC', '12100', 'FIXED', 0, TRUE, 0.2);

-- 3. 차주 마스터 (ods_customer_mst)
TRUNCATE ods_customer_mst CASCADE;
INSERT INTO ods_customer_mst (customer_code, cust_nm, cust_type, biz_no, industry_cd, industry_nm, internal_rating, country_cd, is_sme) VALUES
('M-C001', '(주)엘지화학', 'CORPORATE', '101-81-12345', 'C201', '기초 화학물질 제조업', 'AA', 'KR', FALSE),
('M-C002', '박지성', 'RETAIL', NULL, NULL, NULL, 'A1', 'KR', FALSE),
('M-C003', '(주)디지털밸리', 'SME', '202-86-54321', 'J620', '소프트웨어 개발업', 'BBB', 'KR', TRUE);

-- 4. 계좌 원장 (ods_acc_ledger)
TRUNCATE ods_acc_ledger CASCADE;
INSERT INTO ods_acc_ledger (acc_no, customer_code, subj_cd, prod_cd, currency, limit_amt, outstd_amt, open_dt, maturity_dt, int_rate) VALUES
('M-ACC-001', 'M-C001', '12100', 'P002', 'KRW', 5000000000.00, 3000000000.00, '2024-01-10', '2027-01-10', 0.0480),
('M-ACC-002', 'M-C002', '12100', 'P001', 'KRW', 500000000.00, 500000000.00, '2025-02-15', '2045-02-15', 0.0390),
('M-ACC-003', 'M-C003', '12100', 'P002', 'KRW', 1000000000.00, 800000000.00, '2024-11-20', '2025-11-20', 0.0550),
('M-ACC-004', 'M-C002', '21100', 'P004', 'KRW', 0.00, 200000000.00, '2023-05-05', '9999-12-31', 0.0010);

-- 5. 잔액 이력 (ods_balance_hist)
TRUNCATE ods_balance_hist CASCADE;
INSERT INTO ods_balance_hist (base_dt, acc_no, cur_bal, valuation_amt_lcy) VALUES
('2026-04-18', 'M-ACC-001', 3000000000.00, 3000000000.00),
('2026-04-18', 'M-ACC-002', 500000000.00, 500000000.00),
('2026-04-18', 'M-ACC-003', 800000000.00, 800000000.00),
('2026-04-18', 'M-ACC-004', 200000000.00, 200000000.00);

-- 6. 총계정원장 (ods_general_ledger)
TRUNCATE ods_general_ledger CASCADE;
INSERT INTO ods_general_ledger (base_dt, subj_cd, br_cd, curr_cd, dr_bal, cr_bal, net_bal) VALUES
('2026-04-18', '12100', '1001', 'KRW', 4300000000.00, 0.00, 4300000000.00),
('2026-04-18', '21100', '1001', 'KRW', 0.00, 200000000.00, 200000000.00); -- Fixed: Liability positive balance

-- 7. 계좌별 금리 정보 (ods_rate_info)
TRUNCATE ods_rate_info RESTART IDENTITY CASCADE;
INSERT INTO ods_rate_info (base_dt, acc_no, rate_type, base_rate_cd, spread, applied_rate, coupon_rate, payment_freq, rate_cd) VALUES
('2026-04-18', 'M-ACC-001', 'FLOATING', 'CD3M', 0.0150, 0.0480, 0.0480, 3, 'CD3M'),
('2026-04-18', 'M-ACC-002', 'FIXED', 'GOV5Y', 0.0050, 0.0390, 0.0390, 1, 'GOV5Y'),
('2026-04-18', 'M-ACC-003', 'FLOATING', 'CD3M', 0.0220, 0.0550, 0.0550, 3, 'CD3M'),
('2026-04-18', 'M-ACC-004', 'FLOATING', 'DEMAND', 0.0000, 0.0010, 0.0010, 0, 'DEMAND');

-- 8. 시장 기준 금리 (ods_base_rate)
TRUNCATE ods_base_rate CASCADE;
INSERT INTO ods_base_rate (rate_cd, base_dt, rate_val) VALUES
('CD3M', '2026-04-18', 0.0350),
('GOV5Y', '2026-04-18', 0.0380),
('DEMAND', '2026-04-18', 0.0010);

-- 9. 담보 원천 (ods_coll_mst) - DQ Step에서 사용함
TRUNCATE ods_coll_mst CASCADE;
INSERT INTO ods_coll_mst (coll_id, cust_cd, coll_type, coll_amt, haircut_ratio) VALUES
('M-COL-001', 'M-C001', 'REAL_ESTATE', 1000000000.00, 0.1),
('M-COL-002', 'M-C002', 'RESIDENTIAL', 400000000.00, 0.15);

