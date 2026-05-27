-- =================================================================================
-- Credit Risk Service Mock Data v2.1 (Fixed IDs)
-- For PostgreSQL - credit_risk_db
-- =================================================================================

\c credit_risk_db;

-- 1. 신용등급 마스터 (cr_grade_masters)
TRUNCATE cr_grade_masters RESTART IDENTITY CASCADE;
INSERT INTO cr_grade_masters (rating_code, pd_value, notch_order, description) VALUES
('AAA', 0.0001, 1, '최상위 신용등급'),
('AA+', 0.0002, 2, '우량 신용등급'),
('AA',  0.0003, 3, '우량 신용등급'),
('AA-', 0.0005, 4, '우량 신용등급'),
('A+',  0.0010, 5, '상위 신용등급'),
('A',   0.0015, 6, '상위 신용등급'),
('A-',  0.0020, 7, '상위 신용등급'),
('BBB+', 0.0050, 8, '중위 신용등급'),
('BBB',  0.0080, 9, '중위 신용등급'),
('BBB-', 0.0120, 10, '중위 신용등급'),
('BB+',  0.0250, 11, '주의 신용등급'),
('BB',   0.0400, 12, '주의 신용등급'),
('BB-',  0.0600, 13, '주의 신용등급'),
('B+',   0.1000, 14, '투기 신용등급'),
('B',    0.1500, 15, '투기 신용등급'),
('B-',   0.2000, 16, '투기 신용등급'),
('CCC',  0.4000, 17, '위험 신용등급'),
('D',    1.0000, 18, '부도 등급');

-- 2. LGD 세그먼트 마스터 (cr_lgd_segment_masters)
TRUNCATE cr_lgd_segment_masters RESTART IDENTITY CASCADE;
INSERT INTO cr_lgd_segment_masters (segment_name, customer_type, collateral_type, lgd_value) VALUES
('기업-무담보', 'CORPORATE', 'NONE', 0.450000),
('기업-부동산담보', 'CORPORATE', 'REAL_ESTATE', 0.350000),
('기업-금융담보', 'CORPORATE', 'CASH', 0.000000),
('소매-무담보', 'RETAIL', 'NONE', 0.750000),
('소매-주택담보', 'RETAIL', 'RESIDENTIAL', 0.150000),
('중소기업-무담보', 'SME', 'NONE', 0.450000),
('공공기관-무담보', 'PUBLIC_SECTOR', 'NONE', 0.050000);

-- 3. SA 위험가중치 마스터 (cr_sa_rw_masters)
TRUNCATE cr_sa_rw_masters RESTART IDENTITY CASCADE;
INSERT INTO cr_sa_rw_masters (customer_type, rating_code, risk_weight, description) VALUES
('CORPORATE', 'AAA', 0.2000, '기업-AAA'),
('CORPORATE', 'AA+', 0.2000, '기업-AA+'),
('CORPORATE', 'A+', 0.5000, '기업-A+'),
('CORPORATE', 'BBB', 1.0000, '기업-BBB'),
('RETAIL', 'NONE', 0.7500, '소매 표준'),
('PUBLIC_SECTOR', 'AAA', 0.0000, '국가/중앙은행');

-- 4. 규제 파라미터 (cr_regulatory_parameters)
DELETE FROM cr_regulatory_parameters;
INSERT INTO cr_regulatory_parameters (param_key, param_value, description) VALUES
('국제 금융 규제_CONFIDENCE_LEVEL', 0.999, '국제 금융 규제 신뢰수준'),
('CORRELATION_FACTOR_MIN', 0.12, '상관계수 최소값'),
('CORRELATION_FACTOR_MAX', 0.24, '상관계수 최대값'),
('AVG_MATURITY', 2.5, '평균 만기(연)');

-- 5. 상품 마스터 (cr_product_masters)
TRUNCATE cr_product_masters RESTART IDENTITY CASCADE;
INSERT INTO cr_product_masters (product_code, product_name, ccf_rate, standard_rw) VALUES
('LN-CORP', '기업일반대출', 0.0000, 1.0000),
('LN-MTG',  '주택담보대출', 0.0000, 0.3500),
('LN-RETL', '소매신용대출', 0.0000, 0.7500),
('LC-CORP', '기업수입신용장', 0.2000, 1.0000),
('OD-CORP', '기업한도대출(마이너스)', 0.5000, 1.0000);

-- 6. 차주 (cr_customers)
TRUNCATE cr_customers RESTART IDENTITY CASCADE;
INSERT INTO cr_customers (id, customer_code, customer_name, customer_type, internal_rating, industry_code, country_code, is_sme, annual_sales) VALUES
(1, 'C001', '(주)테스트기업A', 'CORPORATE', 'AAA', 'C262', 'KR', FALSE, 500000000000),
(2, 'C002', '(주)테스트기업B', 'CORPORATE', 'BBB', 'C301', 'KR', FALSE, 100000000000),
(3, 'C003', '개인차주X', 'RETAIL', 'A-', NULL, 'KR', FALSE, 50000000),
(4, 'C004', '(주)소상공인Y', 'SME', 'BB+', 'J620', 'KR', TRUE, 1000000000),
(5, 'C005', '공공기관Z', 'PUBLIC_SECTOR', 'AAA', NULL, 'KR', FALSE, NULL);
-- 시퀀스 보정
SELECT setval('cr_customers_id_seq', 5);

-- 7. 계좌 (cr_accounts)
TRUNCATE cr_accounts RESTART IDENTITY CASCADE;
INSERT INTO cr_accounts (id, customer_id, account_no, product_code, outstanding_amt, notional_amt, currency, open_date, maturity_date, delinquent_days, staging, internal_rating) VALUES
(1, 1, 'A001', 'LN-CORP', 100000000.00, 100000000.00, 'KRW', '2025-01-01', '2030-01-01', 0, 'STAGE1', 'AAA'),
(2, 2, 'A002', 'OD-CORP', 50000000.00, 100000000.00, 'KRW', '2025-06-01', '2026-06-01', 0, 'STAGE1', 'BBB'),
(3, 3, 'A003', 'LN-MTG',  300000000.00, 300000000.00, 'KRW', '2024-01-01', '2054-01-01', 0, 'STAGE1', 'A-'),
(4, 4, 'A004', 'LN-RETL', 20000000.00, 20000000.00, 'KRW', '2025-01-01', '2026-01-01', 45, 'STAGE2', 'BB+'),
(5, 5, 'A005', 'LN-CORP', 1000000000.00, 1000000000.00, 'KRW', '2020-01-01', '2040-01-01', 0, 'STAGE1', 'AAA');
-- 시퀀스 보정
SELECT setval('cr_accounts_id_seq', 5);

-- 8. 담보 (cr_collaterals)
TRUNCATE cr_collaterals RESTART IDENTITY CASCADE;
INSERT INTO cr_collaterals (id, customer_id, collateral_code, collateral_type, appraisal_amt, base_haircut, prior_lien_amt, ltv_limit) VALUES
(1, 1, 'COL-001', 'REAL_ESTATE', 150000000.00, 0.1000, 20000000.00, 0.7000),
(2, 3, 'COL-002', 'RESIDENTIAL', 500000000.00, 0.1500, 0.00, 0.6000);
-- 시퀀스 보정
SELECT setval('cr_collaterals_id_seq', 2);

-- 9. 계좌-담보 매핑 (cr_account_collaterals)
TRUNCATE cr_account_collaterals RESTART IDENTITY CASCADE;
INSERT INTO cr_account_collaterals (account_id, collateral_id, allocation_amount, priority) VALUES
(1, 1, 80000000.00, 1),
(3, 2, 250000000.00, 1);

-- 10. 거시경제 시나리오 (cr_macro_scenario)
TRUNCATE cr_macro_scenario RESTART IDENTITY CASCADE;
INSERT INTO cr_macro_scenario (apply_year, scenario_type, probability_weight, pd_adjustment_factor, description) VALUES
(2026, 'BOOM',      0.2000, 0.8000, '경제 호황기'),
(2026, 'BASE',      0.5000, 1.0000, '평시 상태'),
(2026, 'RECESSION', 0.3000, 1.3000, '경제 침체기');

-- 11. 등급 이력 (cr_cust_rating_hist)
TRUNCATE cr_cust_rating_hist RESTART IDENTITY CASCADE;
INSERT INTO cr_cust_rating_hist (cust_id, rating_cd, base_dt) VALUES
(1, 'AAA', '2026-03-31'),
(2, 'AA-', '2026-03-31'), -- BBB로 하락함 (Stage 2 유력)
(3, 'A-',  '2026-03-31'),
(4, 'A-',  '2026-03-31'), -- BB+로 하락함 (Stage 2)
(5, 'AAA', '2026-03-31');


-- 12. 필수 규제 파라미터 추가 (v2.2 패치)
INSERT INTO cr_regulatory_parameters (param_key, param_value, description) VALUES
('PD_FLOOR', 0.0003, '부도율 하한선 (0.03%)'),
('SECURED_LGD_FLOOR', 0.1000, '담보대출 손실률 하한선 (10%)'),
('UNSECURED_LGD_FLOOR', 0.4500, '신용대출 손실률 하한선 (45%)')
ON CONFLICT (param_key) DO UPDATE SET param_value = EXCLUDED.param_value;

