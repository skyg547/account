/**
 * [Enterprise-Scale] 신용 리스크 산출 성능 최적화 인덱스 전략
 * 
 * 💡 [비즈니스 가이드]
 * 억 단위의 리스크 데이터를 조회할 때 인덱스가 없으면 산출에 며칠이 걸릴 수도 있습니다.
 * 이 스크립트는 '고속 도로'를 깔아 산출 속도를 수십 배 이상 향상시키는 역할을 합니다.
 */

-- 1. [배치 파티셔닝 최적화] 
-- ⚙️ is_active와 id를 묶어 배치가 데이터를 읽어오는 Range Scan 속도를 극대화합니다.
CREATE INDEX idx_cr_accounts_partition 
ON cr_accounts (is_active, id);

-- 2. [조인 성능 최적화]
-- ⚙️ 프로세서가 계좌 처리 시 고객(Customer)과 상품(Product) 정보를 즉시 찾을 수 있도록 합니다.
CREATE INDEX idx_cr_accounts_customer_fk ON cr_accounts (customer_id);
CREATE INDEX idx_cr_accounts_product_fk ON cr_accounts (product_code);

-- 3. [담보 배분 및 조회 최적화]
-- ⚙️ 계좌별로 배분되어 있는 담보(CRM) 정보를 신속하게 집계하기 위한 필수 인덱스입니다.
CREATE INDEX idx_cr_account_collaterals_acc_fk ON cr_account_collaterals (account_id);
CREATE INDEX idx_cr_account_collaterals_coll_fk ON cr_account_collaterals (collateral_id);

-- 4. [리스크 결과 조회 최적화]
-- ⚙️ 특정 기준일(base_date)의 산출 결과를 빠르게 조회하거나 리포팅을 생성할 때 사용됩니다.
CREATE INDEX idx_cr_risk_results_base_dt ON cr_risk_results (base_date, status);

-- 5. [추가 권고: 차주 등급 이력 조회]
CREATE INDEX idx_cr_cust_rating_hist_search ON cr_cust_rating_hist (cust_id, base_dt DESC);

log.info("✅ Enterprise Indexing Strategy applied for performance tuning.");
