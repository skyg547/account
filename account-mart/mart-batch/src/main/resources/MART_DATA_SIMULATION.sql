-- [MART_DATA_SIMULATION.sql] mart-batch demo 원천 데이터 예시
-- 기준일: 2026-04-15
-- 실제 실행은 RegulatoryDataTasklet이 담당하며, 본 파일은 동일한 데이터 구조를 SQL로 참고하기 위한 샘플이다.

DELETE FROM ods_balance_hist WHERE base_dt = '2026-04-15' AND acc_no LIKE 'DEMO-ACC%';
DELETE FROM ods_general_ledger WHERE base_dt = '2026-04-15' AND br_cd = 'BR-DEMO';
DELETE FROM market_exchange_rate WHERE base_dt = '2026-04-15' AND base_currency = 'USD' AND quote_currency = 'KRW';
DELETE FROM ods_base_rate WHERE base_dt = '2026-04-15' AND rate_cd IN ('KORIBOR_3M', 'USD_LIBOR_3M');
DELETE FROM ods_biz_day WHERE base_dt = '2026-04-15';
DELETE FROM ods_coll_mst WHERE coll_id IN ('DEMO-COLL001', 'DEMO-COLL002');
DELETE FROM ods_acc_ledger WHERE acc_no IN ('DEMO-ACC001', 'DEMO-ACC002', 'DEMO-ACC003', 'DEMO-ACC004', 'DEMO-ACC005');
DELETE FROM ods_customer_mst WHERE customer_code IN ('DEMO-CUST001', 'DEMO-CUST002', 'DEMO-CUST003', 'DEMO-CUST004');
DELETE FROM ods_product_mst WHERE prod_cd IN ('DEMO-L001', 'DEMO-L002', 'DEMO-C001');

INSERT INTO ods_product_mst (prod_cd, prod_nm, prod_category, subj_cd, rate_type, payment_freq, is_excluded, is_off_balance, default_ccf) VALUES
('DEMO-L001', '일반가계대출', 'LOAN', 'L001', 'FLOATING', 1, FALSE, FALSE, 0.0000),
('DEMO-L002', '중소기업운전자금', 'LOAN', 'L002', 'FLOATING', 3, FALSE, FALSE, 0.0000),
('DEMO-C001', '개인신용카드', 'CARD', 'C001', 'FLOATING', 1, FALSE, FALSE, 0.2000);

INSERT INTO ods_customer_mst (customer_code, cust_nm, cust_type, internal_rating, rating_cd, is_sme, credit_status_cd, country_cd, industry_cd, branch_cd) VALUES
('DEMO-CUST001', '홍길동', 'RETAIL', 'A1', '1', FALSE, 'NORMAL', 'KR', 'RETAIL', 'BR-DEMO'),
('DEMO-CUST002', '김철수', 'RETAIL', 'B2', '5', FALSE, 'NORMAL', 'KR', 'RETAIL', 'BR-DEMO'),
('DEMO-CUST003', '(주)미래소프트', 'SME', 'BB+', '8', TRUE, 'NORMAL', 'KR', 'SME', 'BR-DEMO'),
('DEMO-CUST004', '이영희', 'RETAIL', 'D', '15', FALSE, 'DEFAULT', 'KR', 'RETAIL', 'BR-DEMO');

INSERT INTO market_exchange_rate (base_dt, base_currency, quote_currency, base_rate, bid_rate, ask_rate, change_amt, change_rate, source, created_at) VALUES
('2026-04-15', 'USD', 'KRW', 1350.0000, 1348.0000, 1352.0000, 10.0000, 0.7400, 'DEMO', CURRENT_TIMESTAMP);

INSERT INTO ods_base_rate (rate_cd, base_dt, rate_val) VALUES
('KORIBOR_3M', '2026-04-15', 0.035000),
('USD_LIBOR_3M', '2026-04-15', 0.025000);

INSERT INTO ods_biz_day (base_dt, is_biz_day, holiday_nm) VALUES
('2026-04-15', TRUE, '정상영업일');

INSERT INTO ods_acc_ledger (acc_no, customer_code, prod_cd, currency, outstd_amt, limit_amt, int_rate, base_rate_cd, spread, open_dt, maturity_dt, delinquent_days, repayment_method, branch_cd, biz_unit_cd, is_active) VALUES
('DEMO-ACC001', 'DEMO-CUST001', 'DEMO-L001', 'KRW', 100000000, 100000000, 0.0450, 'KORIBOR_3M', 0.0100, '2025-01-01', '2027-01-01', 0, 'BULLET', 'BR-DEMO', 'RB', TRUE),
('DEMO-ACC002', 'DEMO-CUST002', 'DEMO-L001', 'KRW', 50000000, 50000000, 0.0550, 'KORIBOR_3M', 0.0150, '2025-02-01', '2026-02-01', 45, 'BULLET', 'BR-DEMO', 'RB', TRUE),
('DEMO-ACC003', 'DEMO-CUST003', 'DEMO-L002', 'KRW', 500000000, 1000000000, 0.0600, 'KORIBOR_3M', 0.0200, '2024-05-15', '2026-05-15', 0, 'AMORTIZING', 'BR-DEMO', 'CB', TRUE),
('DEMO-ACC004', 'DEMO-CUST004', 'DEMO-C001', 'KRW', 10000000, 10000000, 0.1800, 'KORIBOR_3M', 0.1200, '2024-10-01', '2025-10-01', 120, 'REVOLVING', 'BR-DEMO', 'RB', TRUE),
('DEMO-ACC005', 'DEMO-CUST001', 'DEMO-L001', 'USD', 10000, 10000, 0.0350, 'USD_LIBOR_3M', 0.0050, '2025-03-01', '2026-03-01', 0, 'BULLET', 'BR-DEMO', 'IB', TRUE);

INSERT INTO ods_balance_hist (acc_no, base_dt, cur_bal, fx_rate) VALUES
('DEMO-ACC001', '2026-04-15', 100000000, 1.0000),
('DEMO-ACC002', '2026-04-15', 50000000, 1.0000),
('DEMO-ACC003', '2026-04-15', 500000000, 1.0000),
('DEMO-ACC004', '2026-04-15', 10000000, 1.0000),
('DEMO-ACC005', '2026-04-15', 10000, 1350.0000);

INSERT INTO ods_coll_mst (coll_id, cust_cd, coll_type, coll_amt, haircut_ratio) VALUES
('DEMO-COLL001', 'DEMO-CUST001', 'REAL_ESTATE', 150000000, 0.2000),
('DEMO-COLL002', 'DEMO-CUST003', 'GUARANTEE', 400000000, 0.1000);

INSERT INTO ods_general_ledger (base_dt, subj_cd, br_cd, curr_cd, net_bal) VALUES
('2026-04-15', 'L001', 'BR-DEMO', 'KRW', 150000000),
('2026-04-15', 'L002', 'BR-DEMO', 'KRW', 500000000),
('2026-04-15', 'C001', 'BR-DEMO', 'KRW', 10000000),
('2026-04-15', 'L001', 'BR-DEMO', 'USD', 10000);
