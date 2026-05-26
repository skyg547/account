-- =================================================================================
-- [Risk Data Mart] Full Mock Data v3.1 (Fixed Partition Clear)
-- =================================================================================

\c market_data_db;

-- 0. 기존 데이터 물리적 완전 삭제
TRUNCATE ods_customer_mst, ods_account_mst, ods_product_mst, ods_acc_ledger, ods_general_ledger, ods_rate_info, ods_early_warning, ods_coll_mst, ods_base_rate CASCADE;
-- 파티션 테이블은 상위 테이블 트렁케이트로 해결되나, 명시적으로 한 번 더 클리어
DELETE FROM ods_balance_hist;

-- 1. 계정 과목
INSERT INTO ods_account_mst (subj_cd, subj_nm, acct_type, bs_class, is_asset, biz_unit_cd) VALUES
('11100', '현금및예치금', 'CASH', 'ASSET', TRUE, 'CM'),
('12100', '원화대출금', 'LOAN', 'ASSET', TRUE, 'RETAIL'),
('12200', '외화대출금', 'LOAN', 'ASSET', TRUE, 'IB'),
('21100', '원화예수금', 'DEPOSIT', 'LIABILITY', FALSE, 'RETAIL'),
('21200', '외화예수금', 'DEPOSIT', 'LIABILITY', FALSE, 'IB');

-- 2. 상품
INSERT INTO ods_product_mst (prod_cd, prod_nm, prod_category, subj_cd, default_rate_type, default_payment_freq, is_off_balance, default_ccf) VALUES
('P001', '가계주택담보대출', 'MORTGAGE', '12100', 'FIXED', 1, FALSE, 0.0),
('P002', '기업운전자금대출', 'CORP_LOAN', '12100', 'FLOATING', 3, FALSE, 0.0),
('P003', '정기예치금', 'TERM_DEP', '21100', 'FIXED', 0, FALSE, 0.0),
('P004', '보통예수금', 'DEMAND_DEP', '21100', 'FLOATING', 0, FALSE, 0.0);

-- 3. 차주
INSERT INTO ods_customer_mst (customer_code, cust_nm, cust_type, internal_rating, country_cd, is_sme) VALUES
('M-C001', '(주)엘지화학', 'CORPORATE', 'AA', 'KR', FALSE),
('M-C002', '박지성', 'RETAIL', 'A1', 'KR', FALSE),
('M-C003', '(주)디지털밸리', 'SME', 'BBB', 'KR', TRUE),
('M-C005', '(주)미래건설', 'CORPORATE', 'B+', 'KR', FALSE);

-- 4. 계좌 원장 (SL) - 총 63억 (12100 과목)
INSERT INTO ods_acc_ledger (acc_no, customer_code, subj_cd, prod_cd, currency, outstd_amt, open_dt, maturity_dt, is_active) VALUES
('M-ACC-001', 'M-C001', '12100', 'P002', 'KRW', 3000000000.00, '2024-01-10', '2027-01-10', TRUE),
('M-ACC-002', 'M-C002', '12100', 'P001', 'KRW', 500000000.00, '2025-02-15', '2045-02-15', TRUE),
('M-ACC-003', 'M-C003', '12100', 'P002', 'KRW', 800000000.00, '2024-11-20', '2025-11-20', TRUE),
('M-ACC-004', 'M-C002', '21100', 'P004', 'KRW', 200000000.00, '2023-05-05', '9999-12-31', TRUE),
('M-ACC-005', 'M-C005', '12100', 'P001', 'KRW', 2000000000.00, '2023-01-01', '2028-01-01', TRUE);

-- 5. 잔액 이력 (Reconciliation의 핵심 소스)
INSERT INTO ods_balance_hist (base_dt, acc_no, cur_bal, valuation_amt_lcy) VALUES
('2026-04-18', 'M-ACC-001', 3000000000.00, 3000000000.00),
('2026-04-18', 'M-ACC-002', 500000000.00, 500000000.00),
('2026-04-18', 'M-ACC-003', 800000000.00, 800000000.00),
('2026-04-18', 'M-ACC-004', 200000000.00, 200000000.00),
('2026-04-18', 'M-ACC-005', 2000000000.00, 2000000000.00);

-- 6. 총계정원장 (GL) - 합계 63억으로 SL과 일치시킴
INSERT INTO ods_general_ledger (base_dt, subj_cd, br_cd, curr_cd, dr_bal, cr_bal, net_bal) VALUES
('2026-04-18', '12100', 'ALL', 'KRW', 6300000000.00, 0.00, 6300000000.00),
('2026-04-18', '21100', 'ALL', 'KRW', 0.00, 200000000.00, 200000000.00);

-- 7. 금리 정보
INSERT INTO ods_rate_info (base_dt, acc_no, rate_type, base_rate_cd, applied_rate, rate_cd) VALUES
('2026-04-18', 'M-ACC-001', 'FLOATING', 'CD3M', 0.0480, 'CD3M'),
('2026-04-18', 'M-ACC-002', 'FIXED', 'GOV5Y', 0.0390, 'GOV5Y'),
('2026-04-18', 'M-ACC-003', 'FLOATING', 'CD3M', 0.0550, 'CD3M'),
('2026-04-18', 'M-ACC-004', 'FLOATING', 'DEMAND', 0.0010, 'DEMAND'),
('2026-04-18', 'M-ACC-005', 'FIXED', 'GOV5Y', 0.0650, 'GOV5Y');

