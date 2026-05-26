-- =================================================================================
-- Risk Data Mart CDM readiness patch
-- 현재 IntegratedRiskPosition 엔티티와 기존 dim_integrated_position_master 테이블을 맞춘다.
-- =================================================================================

\c market_data_db;

ALTER TABLE dim_integrated_position_master ADD COLUMN IF NOT EXISTS country_cd VARCHAR(10);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'dim_integrated_position_master'
          AND column_name = 'country_code'
    ) THEN
        EXECUTE 'UPDATE dim_integrated_position_master SET country_cd = country_code WHERE country_cd IS NULL';
    END IF;
END $$;

ALTER TABLE dim_integrated_position_master ADD COLUMN IF NOT EXISTS warning_level VARCHAR(20);
ALTER TABLE dim_integrated_position_master ADD COLUMN IF NOT EXISTS is_debt_restructured BOOLEAN DEFAULT FALSE;
ALTER TABLE dim_integrated_position_master ADD COLUMN IF NOT EXISTS customer_level VARCHAR(20);
ALTER TABLE dim_integrated_position_master ADD COLUMN IF NOT EXISTS kb_price NUMERIC(19,4);
ALTER TABLE dim_integrated_position_master ADD COLUMN IF NOT EXISTS prior_lien_amt NUMERIC(19,4);
ALTER TABLE dim_integrated_position_master ADD COLUMN IF NOT EXISTS ltv_ratio NUMERIC(10,6);

DELETE FROM dim_integrated_position_master WHERE base_dt IN (DATE '2026-04-15', DATE '2026-04-16');
