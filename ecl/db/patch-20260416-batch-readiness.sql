-- =================================================================================
-- Credit Risk batch readiness patch
-- 기준일 배치 재실행 전, 현재 엔티티가 요구하는 결과 컬럼을 보강한다.
-- =================================================================================

\c credit_risk_db;

ALTER TABLE cr_risk_results ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE cr_risk_results ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE cr_risk_results ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

DELETE FROM cr_risk_results WHERE base_date = DATE '2026-04-15';
