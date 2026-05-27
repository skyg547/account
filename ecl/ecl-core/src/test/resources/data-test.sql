-- H2 Compatible Sample Data for Credit Risk Service Integration Test

-- 1. 대손충당금(IFRS9) 차주 마스터 (cr_customers)
INSERT INTO cr_customers (customer_code, customer_name, customer_type, internal_rating, industry_code, country_code, is_sme, annual_sales) VALUES
('CUST-001', '(주)삼성전자', 'CORPORATE', 'AAA', 'C262', 'KR', FALSE, 20000.0),
('CUST-002', '(주)현대자동차', 'CORPORATE', 'AA+', 'C301', 'KR', FALSE, 15000.0),
('CUST-003', '홍길동', 'RETAIL', 'A-', NULL, 'KR', FALSE, 0.5),
('CUST-004', '강남IT자영업', 'SME', 'BB+', 'J620', 'KR', TRUE, 10.0),
('CUST-005', '한국은행', 'PUBLIC_SECTOR', 'AAA', NULL, 'KR', FALSE, 0.0);

-- 2. 상품 마스터 샘플 (cr_product_masters)
INSERT INTO cr_product_masters (product_code, product_name, ccf_rate, standard_rw) VALUES
('LN-CORP', '기업일반대출', 0.0000, 1.0000),
('LN-MTG',  '주택담보대출', 0.0000, 0.3500),
('LN-RETL', '소매신용대출', 0.0000, 0.7500),
('LC-CORP', '기업수입신용장', 0.2000, 1.0000),
('OD-CORP', '기업한도대출(마이너스)', 0.5000, 1.0000);

-- 3. 익스포저 원장 (cr_accounts)
INSERT INTO cr_accounts (customer_id, account_no, product_code, outstanding_amt, notional_amt, currency, open_date, maturity_date, delinquent_days, staging, internal_rating) VALUES
(1, 'ACC-SM-001', 'LN-CORP', 1000000000.00, 1000000000.00, 'KRW', '2024-01-01', '2027-01-01', 0, 'STAGE1', NULL),
(1, 'ACC-SM-002', 'OD-CORP', 500000000.00, 1000000000.00, 'KRW', '2024-06-01', '2025-06-01', 0, 'STAGE1', NULL),
(2, 'ACC-HD-001', 'LN-CORP', 2000000000.00, 2000000000.00, 'KRW', '2023-01-01', '2028-01-01', 0, 'STAGE1', NULL),
(3, 'ACC-HK-001', 'LN-MTG',  300000000.00, 300000000.00, 'KRW', '2025-01-15', '2055-01-15', 5, 'STAGE1', NULL),
(4, 'ACC-GN-001', 'LN-RETL', 50000000.00, 50000000.00, 'KRW', '2025-10-01', '2026-10-01', 45, 'STAGE2', NULL),
(5, 'ACC-CB-001', 'LN-CORP', 5000000000.00, 5000000000.00, 'KRW', '2020-01-01', '2030-01-01', 0, 'STAGE1', NULL);

-- 4. 담보 마스터 (cr_collaterals)
INSERT INTO cr_collaterals (customer_id, collateral_code, collateral_type, appraisal_amt, base_haircut, prior_lien_amt, ltv_limit, kb_market_price, district_code) VALUES
(1, 'COL-RE-001', 'REAL_ESTATE', 1500000000.00, 0.1000, 200000000.00, 0.7000, 1600000000.00, '1168010100'),
(3, 'COL-RE-002', 'RESIDENTIAL', 500000000.00,  0.1500, 0.00,         0.6000, 520000000.00,  '1165010100'),
(4, 'COL-CS-001', 'CASH',        100000000.00,  0.0000, 0.00,         1.0000, NULL,          NULL);

-- 5. 계좌-담보 매핑 (cr_account_collaterals)
INSERT INTO cr_account_collaterals (account_id, collateral_id, allocation_amount) VALUES
(1, 1, 800000000.00),
(4, 3, 30000000.00);

-- 6. 거시경제 시나리오 (Forward-Looking ECL용)
INSERT INTO cr_macro_scenario (apply_year, scenario_type, probability_weight, pd_adjustment_factor, description) VALUES
(2026, 'BOOM',      0.2000, 0.8000, '경제 호황기: PD 20% 감소'),
(2026, 'BASE',      0.5000, 1.0000, '평시 상태: 기본 PD 적용'),
(2026, 'RECESSION', 0.3000, 1.3000, '경제 침체기: PD 30% 증가');

-- 7. 고객 신용등급 이력 (Staging 산출용)
INSERT INTO cr_cust_rating_hist (cust_id, rating_cd, base_dt) VALUES
(1, 'AAA', '2026-03-31'),
(2, 'AA+', '2026-03-31'),
(3, 'A-',  '2026-03-31'),
(4, 'A-',  '2026-03-31'),
(5, 'AAA', '2026-03-31');
