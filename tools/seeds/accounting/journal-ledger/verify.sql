WITH expected(id,entry_id,side,account_code,amount,base_amount) AS (VALUES
 (6900001::bigint,6900001::bigint,'DEBIT','GH690-CASH',1000000::numeric,1000000::numeric),
 (6900002,6900001,'CREDIT','GH690-EQUITY',1000000,1000000),
 (6900003,6900002,'DEBIT','GH690-FX',100,130000),
 (6900004,6900002,'CREDIT','GH690-FXLIAB',100,130000),
 (6900005,6900003,'DEBIT','GH690-CASH',999,999),
 (6900006,6900003,'CREDIT','GH690-EQUITY',999,999))
SELECT (SELECT count(*)=3 FROM journal_entries WHERE id IN (6900001,6900002,6900003) AND lineage_source_type='GH690' AND accounting_date='2090-01-15')
AND EXISTS (SELECT 1 FROM journal_entries WHERE id=6900001 AND status='POSTED' AND currency_code='KRW' AND exchange_rate=1 AND slip_no='GH690-KRW-001')
AND EXISTS (SELECT 1 FROM journal_entries WHERE id=6900002 AND status='POSTED' AND currency_code='USD' AND exchange_rate=1300 AND slip_no='GH690-USD-001')
AND EXISTS (SELECT 1 FROM journal_entries WHERE id=6900003 AND status='DRAFT' AND currency_code='KRW' AND slip_no='GH690-DRAFT-001')
AND NOT EXISTS (SELECT 1 FROM expected e LEFT JOIN journal_details d ON d.id=e.id WHERE d.id IS NULL OR d.journal_entry_id<>e.entry_id
 OR d.account_code<>e.account_code OR d.side<>e.side OR d.amount<>e.amount OR d.base_amount<>e.base_amount
 OR d.audit_user IS DISTINCT FROM 'GH690' OR d.dept_code IS DISTINCT FROM 'GH690-DEPT' OR d.business_partner_code IS DISTINCT FROM 'GH690-CUSTOMER')
AND (SELECT count(*)=6 FROM journal_details WHERE journal_entry_id IN (6900001,6900002,6900003))
AND NOT EXISTS (SELECT journal_entry_id FROM journal_details WHERE journal_entry_id IN (6900001,6900002,6900003) GROUP BY journal_entry_id HAVING sum(CASE WHEN side='DEBIT' THEN amount ELSE -amount END)<>0 OR sum(CASE WHEN side='DEBIT' THEN base_amount ELSE -base_amount END)<>0)
AND (SELECT count(*)=4 AND sum(base_dr_amount)=1130000 AND sum(base_cr_amount)=1130000 FROM gl_entries WHERE id BETWEEN 6900001 AND 6900004 AND lineage_source_type='GH690')
AND (SELECT count(*)=4 AND sum(base_dr_amount)=1130000 AND sum(base_cr_amount)=1130000 FROM sl_entries WHERE id BETWEEN 6900001 AND 6900004 AND lineage_source_type='GH690');
