-- =================================================================================
-- Risk Data Mart Service Full Test Data (Scenario-driven)
-- =================================================================================

-- 1. 영업일 및 시장 데이터 (Market Data)
INSERT INTO ods_biz_day (base_dt, is_biz_day, holiday_nm, country_cd) VALUES
('2026-04-15', TRUE, NULL, 'KR'),
('2026-04-14', TRUE, NULL, 'KR'),
('2026-04-13', TRUE, NULL, 'KR');

INSERT INTO ods_base_rate (rate_cd, base_dt, rate_val) VALUES
('KORIBOR_3M', '2026-04-15', 3.55),
('CD_91D', '2026-04-15', 3.62),
('TREASURY_3Y', '2026-04-15', 3.45);

-- 2. 거시경제 시나리오 (IFRS 9 Macro Scenario)
INSERT INTO ods_macro_scenario (scenario_id, base_dt, gdp_growth_rate, unemployment_rate, inflation_rate, scenario_weight) VALUES
('BASE', '2026-04-15', 2.5, 3.2, 2.0, 0.40),
('OPTIMISTIC', '2026-04-15', 3.8, 2.8, 1.8, 0.30),
('PESSIMISTIC', '2026-04-15', 0.5, 5.5, 4.5, 0.30);

-- 3. 규제 파스터 데이터 동기화 (Regulatory Master)
INSERT INTO ods_account_mst (subj_cd, subj_nm, acct_type, bs_class, is_asset, biz_unit_cd) VALUES
('1101', '중소기업일반대출', 'LOAN', 'ASSET', TRUE, 'CORPORATE'),
('1102', '가계담보대출', 'LOAN', 'ASSET', TRUE, 'RETAIL'),
('2101', '정기예금', 'DEPOSIT', 'LIABILITY', FALSE, 'RETAIL');

INSERT INTO ods_product_mst (prod_cd, prod_nm, prod_category, subj_cd, default_ccf) VALUES
('P001', 'SME 운전자금대출', 'SME_LOAN', '1101', 0.75),
('P002', '아파트담보대출', 'MORTGAGE', '1102', 1.00);

-- 4. 고객 및 계좌 정보 (Core Data)
-- 차주 1: 대기업 (등급 우량)
INSERT INTO ods_customer_mst (customer_code, cust_nm, cust_type, internal_rating, is_sme, industry_cd) VALUES
('C_CORP_001', '(주)한국전자', 'CORPORATE', 'AAA', FALSE, 'MANUFACTURING');

-- 차주 2: 중소기업 (등급 보통, SME)
INSERT INTO ods_customer_mst (customer_code, cust_nm, cust_type, internal_rating, is_sme, industry_cd) VALUES
('C_SME_001', '미래테크', 'CORPORATE', 'BBB', TRUE, 'IT_SERVICE');

-- 차주 3: 소매 고객 (연체 발생 가능성 있음)
INSERT INTO ods_customer_mst (customer_code, cust_nm, cust_type, internal_rating, is_sme) VALUES
('C_IND_001', '홍길동', 'RETAIL', 'B', FALSE);

-- 계좌 정보 (Ledger)
INSERT INTO ods_acc_ledger (acc_no, customer_code, prod_cd, subj_cd, currency, limit_amt, outstd_amt, open_dt, maturity_dt, delinquent_days) VALUES
('ACC_001', 'C_CORP_001', 'P001', '1101', 'KRW', 5000000000, 3000000000, '2025-01-01', '2027-01-01', 0),
('ACC_002', 'C_SME_001', 'P001', '1101', 'KRW', 1000000000, 800000000, '2025-06-01', '2026-06-01', 0),
('ACC_003', 'C_IND_001', 'P002', '1102', 'KRW', 500000000, 450000000, '2024-03-01', '2029-03-01', 5);

-- 5. 담보 정보 (Collateral)
INSERT INTO ods_collateral_mst (coll_id, acc_no, coll_type, coll_detail, coll_amt, recognized_amt) VALUES
('COLL_001', 'ACC_003', 'APARTMENT', '서초자이 101동', 800000000, 560000000);

INSERT INTO ods_apart_coll_detail (coll_id, kb_market_price, house_type, exclusive_area, is_speculative_area) VALUES
('COLL_001', 1200000000, 'APARTMENT', 84.50, TRUE);

-- 6. 초기 잔액 이력 (Balance History)
INSERT INTO ods_balance_hist (base_dt, acc_no, cur_bal) VALUES
('2026-04-15', 'ACC_001', 3000000000),
('2026-04-15', 'ACC_002', 800000000),
('2026-04-15', 'ACC_003', 450000000);

-- 7. 조기경보 및 채무조정 (Monitoring)
INSERT INTO ods_early_warning (base_dt, customer_code, warning_level, warning_reason) VALUES
('2026-04-15', 'C_IND_001', 'CAUTION', '연체 5일 발생');

-- 8. 규제 파라미터 (Regulatory Parameter Setup)
-- 이 데이터는 CreditRiskService에서 캐싱하여 사용함
INSERT INTO cr_regulatory_parameter (param_key, param_value, param_desc) VALUES
('PD_FLOOR', 0.0003, '부도율 최저한도 (0.03%)'),
('SECURED_LGD_FLOOR', 0.15, '담보부 LGD 최저한도 (15%)'),
('UNSECURED_LGD_FLOOR', 0.45, '무담보 LGD 기준값 (45%)'),
('ASSET_CORR_BASE', 0.12, '기업 상관계수 하한'),
('ASSET_CORR_HIGH', 0.24, '기업 상관계수 상한');
