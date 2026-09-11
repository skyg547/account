WITH expected(id,code,kind,side) AS (VALUES (6900001::bigint,'GH690-CASH','ASSETS','DEBIT'),
(6900002::bigint,'GH690-FX','ASSETS','DEBIT'),
(6900003::bigint,'GH690-EQUITY','EQUITY','CREDIT'),
(6900004::bigint,'GH690-EXPENSE','EXPENSES','DEBIT'),
(6900005::bigint,'GH690-FXGAIN','REVENUE','CREDIT'),
(6900006::bigint,'GH690-FXLOSS','EXPENSES','DEBIT'),
(6900007::bigint,'GH690-LOAN','ASSETS','DEBIT'),
(6900008::bigint,'GH690-ALLOWANCE','ASSETS','CREDIT'),
(6900009::bigint,'GH690-BADDEBT','EXPENSES','DEBIT'),
(6900010::bigint,'GH690-REVERSAL','REVENUE','CREDIT'),
(6900011::bigint,'GH690-DEPR','EXPENSES','DEBIT'),
(6900012::bigint,'GH690-ACCDEPR','ASSETS','CREDIT'),
(6900013::bigint,'GH690-INTINC','REVENUE','CREDIT'),
(6900014::bigint,'GH690-INTREC','ASSETS','DEBIT'),
(6900015::bigint,'GH690-FXLIAB','LIABILITIES','CREDIT'),
(6900016::bigint,'115010','ASSETS','DEBIT'),
(6900017::bigint,'410100','REVENUE','CREDIT'),
(6900018::bigint,'GH690-REPAYCASH','ASSETS','DEBIT'),
(6900019::bigint,'GH690-ASSET','ASSETS','DEBIT'))
SELECT NOT EXISTS (SELECT 1 FROM expected x LEFT JOIN account_subjects a ON a.id=x.id
 WHERE a.id IS NULL OR a.code<>x.code OR a.account_type<>x.kind OR a.balance_type<>x.side OR a.audit_user IS DISTINCT FROM 'GH690'
 OR a.valid_from<>DATE '2090-01-01' OR a.valid_to<>DATE '2090-01-31')
AND NOT EXISTS (SELECT x.code FROM expected x JOIN account_subjects a ON a.code=x.code
 WHERE DATE '2090-01-15' BETWEEN a.valid_from AND a.valid_to GROUP BY x.code HAVING count(*)<>1)
AND (SELECT count(*)=2 FROM currencies WHERE id IN (6900001,6900002) AND audit_user='GH690' AND currency_code IN ('KRW','USD') AND DATE '2090-01-15' BETWEEN valid_from AND valid_to)
AND NOT EXISTS (SELECT currency_code FROM currencies WHERE currency_code IN ('KRW','USD') AND DATE '2090-01-15' BETWEEN valid_from AND valid_to GROUP BY currency_code HAVING count(*)<>1)
AND EXISTS (SELECT 1 FROM exchange_rates WHERE id=6900001 AND effective_date='2090-01-15' AND from_currency_code='USD' AND to_currency_code='KRW' AND rate=1400)
AND EXISTS (SELECT 1 FROM fiscal_periods WHERE id=6900001 AND fiscal_year='2090' AND fiscal_period='01' AND closing_status='OPEN' AND audit_user='GH690')
AND (SELECT count(*)=2 FROM business_partners WHERE id IN (6900001,6900002) AND audit_user='GH690' AND use_yn AND kyc_status='APPROVED')
AND EXISTS (SELECT 1 FROM departments WHERE id=6900001 AND code='GH690-DEPT' AND audit_user='GH690');
