-- =================================================================================
-- Risk Data Mart batch seed and reconciliation data
-- 기준일: 2026-04-15
--
-- 목적:
-- 1. DataPopulator가 만든 ODS 계좌 데이터의 고객/상품/계정과목 매핑을 보강한다.
-- 2. ods_balance_hist(SL)와 ods_general_ledger(GL)를 같은 기준으로 생성해 GL-SL 대사를 통과시킨다.
-- 3. 반복 실행이 가능하도록 ON CONFLICT/DELETE 후 INSERT 패턴을 사용한다.
-- =================================================================================

\c market_data_db;

INSERT INTO ods_account_mst (subj_cd, subj_nm, acct_type, bs_class, is_asset, biz_unit_cd) VALUES
('1101', '기업 운전자금대출', 'LOAN', 'ASSET', TRUE, 'CORP'),
('1102', '개인 신용대출', 'LOAN', 'ASSET', TRUE, 'RETAIL'),
('1301', '신용카드채권', 'CARD', 'ASSET', TRUE, 'RETAIL'),
('9101', '기업 한도약정', 'OFF_BALANCE', 'ASSET', TRUE, 'CORP')
ON CONFLICT (subj_cd) DO UPDATE SET
    subj_nm = EXCLUDED.subj_nm,
    acct_type = EXCLUDED.acct_type,
    bs_class = EXCLUDED.bs_class,
    is_asset = EXCLUDED.is_asset,
    biz_unit_cd = EXCLUDED.biz_unit_cd;

UPDATE ods_product_mst
SET subj_cd = CASE prod_cd
    WHEN 'CORP_LOAN' THEN '1101'
    WHEN 'RETAIL_LOAN' THEN '1102'
    WHEN 'CREDIT_CARD' THEN '1301'
    WHEN 'CORP_LIMIT' THEN '9101'
    ELSE subj_cd
END
WHERE subj_cd IS NULL
  AND prod_cd IN ('CORP_LOAN', 'RETAIL_LOAN', 'CREDIT_CARD', 'CORP_LIMIT');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'ods_acc_ledger'
          AND column_name = 'cust_cd'
    ) THEN
        EXECUTE 'UPDATE ods_acc_ledger SET customer_code = COALESCE(customer_code, cust_cd) WHERE customer_code IS NULL';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_name = 'ods_coll_mst'
    ) THEN
        EXECUTE 'UPDATE ods_coll_mst SET coll_type = ''CASH_DEPOSIT'' WHERE coll_type = ''CASH''';
    END IF;
END $$;

DELETE FROM ods_balance_hist WHERE base_dt = DATE '2026-04-15';

INSERT INTO ods_balance_hist (base_dt, acc_no, cur_bal, fx_rate, valuation_amt_lcy)
SELECT DATE '2026-04-15',
       acc_no,
       COALESCE(outstd_amt, 0),
       1.0,
       COALESCE(outstd_amt, 0)
FROM ods_acc_ledger
WHERE COALESCE(is_active, TRUE) = TRUE;

DELETE FROM ods_general_ledger WHERE base_dt = DATE '2026-04-15';

INSERT INTO ods_general_ledger (base_dt, subj_cd, br_cd, curr_cd, dr_bal, cr_bal, net_bal)
SELECT DATE '2026-04-15',
       COALESCE(p.subj_cd, '9999') AS subj_cd,
       COALESCE(l.branch_cd, '0000') AS br_cd,
       COALESCE(l.currency, 'KRW') AS curr_cd,
       SUM(COALESCE(l.outstd_amt, 0)) AS dr_bal,
       0 AS cr_bal,
       SUM(COALESCE(l.outstd_amt, 0)) AS net_bal
FROM ods_acc_ledger l
LEFT JOIN ods_product_mst p ON p.prod_cd = l.prod_cd
WHERE COALESCE(l.is_active, TRUE) = TRUE
GROUP BY COALESCE(p.subj_cd, '9999'), COALESCE(l.branch_cd, '0000'), COALESCE(l.currency, 'KRW');

DELETE FROM dim_integrated_position_master WHERE base_dt = DATE '2026-04-15';
