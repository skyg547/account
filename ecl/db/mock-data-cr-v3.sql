-- =================================================================================
-- [Credit Risk] Full Mock Data v3.0 (PostgreSQL)
-- Target Database: credit_risk_db
-- =================================================================================

\c credit_risk_db;

-- 0. 기존 데이터 청소 (CASCADE로 연쇄 삭제)
TRUNCATE cr_customers, cr_accounts, cr_collaterals, cr_grade_masters, cr_lgd_segment_masters, cr_sa_rw_masters, cr_product_masters CASCADE;

-- 1. 신용등급 마스터 (cr_grade_masters)
INSERT INTO cr_grade_masters (rating_code, pd_value, notch_order, description) VALUES
('AAA', 0.0003, 1, '최우량'), ('AA+', 0.0005, 2, '우량'), ('AA', 0.0007, 3, '우량'),
('A+', 0.0015, 5, '상위'), ('BBB', 0.0080, 9, '중위'), ('BB+', 0.0250, 11, '주의'),
('B', 0.1500, 15, '투기'), ('D', 1.0000, 18, '부도');

-- 2. LGD 세그먼트 마스터
INSERT INTO cr_lgd_segment_masters (segment_name, customer_type, collateral_type, lgd_value) VALUES
('기업-무담보', 'CORPORATE', 'NONE', 0.45),
('기업-부동산', 'CORPORATE', 'REAL_ESTATE', 0.35),
('소매-주택담보', 'RETAIL', 'RESIDENTIAL', 0.15);

-- 3. SA 위험가중치(RW) 마스터
INSERT INTO cr_sa_rw_masters (customer_type, rating_code, risk_weight, description) VALUES
('CORPORATE', 'AAA', 0.20, '기업-AAA'),
('CORPORATE', 'BBB', 1.00, '기업-BBB'),
('RETAIL', 'NONE', 0.75, '소매 표준');

-- 4. 상품 마스터 (CCF 포함)
INSERT INTO cr_product_masters (product_code, product_name, ccf_rate, standard_rw) VALUES
('LN-CORP', '기업일반대출', 0.0, 1.0),
('LN-MTG', '주택담보대출', 0.0, 0.35),
('OD-CORP', '기업한도대출', 0.5, 1.0);

-- 5. 규제 파라미터 (Conflict 발생 시 업데이트)
INSERT INTO cr_regulatory_parameters (param_key, param_value, description) VALUES
('PD_FLOOR', 0.0003, '부도율 하한'),
('UNSECURED_LGD_FLOOR', 0.45, '신용 LGD 하한')
ON CONFLICT (param_key) DO UPDATE SET param_value = EXCLUDED.param_value;

-- 6. 차주 데이터 (ID 명시적 지정)
INSERT INTO cr_customers (id, customer_code, customer_name, customer_type, internal_rating, industry_code, is_sme) VALUES
(1, 'C-001', '(주)글로벌기술', 'CORPORATE', 'AAA', 'C262', FALSE),
(2, 'C-002', '김은행', 'RETAIL', 'A+', NULL, FALSE),
(3, 'C-003', '(주)벤처스타', 'CORPORATE', 'BBB', 'J620', TRUE);

SELECT setval('cr_customers_id_seq', 3);

-- 7. 계좌 데이터 (Outstanding 반영)
INSERT INTO cr_accounts (id, customer_id, account_no, product_code, outstanding_amt, limit_amt, currency, staging, internal_rating, is_active) VALUES
(1, 1, 'ACC-CR-001', 'LN-CORP', 1000000000.00, 1000000000.00, 'KRW', 'STAGE1', 'AAA', TRUE),
(2, 2, 'ACC-CR-002', 'LN-MTG', 500000000.00, 500000000.00, 'KRW', 'STAGE1', 'A+', TRUE),
(3, 3, 'ACC-CR-003', 'OD-CORP', 200000000.00, 1000000000.00, 'KRW', 'STAGE1', 'BBB', TRUE);

SELECT setval('cr_accounts_id_seq', 3);

-- 8. 담보 데이터
INSERT INTO cr_collaterals (id, customer_id, collateral_code, collateral_type, appraisal_amt, ltv_limit) VALUES
(1, 1, 'COL-CR-001', 'REAL_ESTATE', 1200000000.00, 0.70),
(2, 2, 'COL-CR-002', 'RESIDENTIAL', 400000000.00, 0.60);

SELECT setval('cr_collaterals_id_seq', 2);

-- 9. 계좌-담보 매핑
INSERT INTO cr_account_collaterals (account_id, collateral_id, allocation_amount, priority) VALUES
(1, 1, 700000000.00, 1),
(2, 2, 300000000.00, 1);

