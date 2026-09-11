WITH expected(source_id,account_code,side,pnl_account,pnl_side) AS (VALUES
 ('6900001|GH690-FX|USD','GH690-FX','DEBIT','GH690-FXGAIN','CREDIT'),
 ('6900001|GH690-FXLIAB|USD','GH690-FXLIAB','CREDIT','GH690-FXLOSS','DEBIT'))
SELECT (SELECT count(*)=2 FROM journal_entries j JOIN expected e ON e.source_id=j.lineage_source_id
 WHERE j.lineage_source_type='FX_VALUATION' AND j.accounting_date='2090-01-15' AND j.currency_code='KRW' AND j.status='DRAFT')
AND NOT EXISTS (SELECT 1 FROM expected e WHERE NOT EXISTS (
 SELECT 1 FROM journal_entries j WHERE j.lineage_source_id=e.source_id AND j.lineage_source_type='FX_VALUATION'
 AND (SELECT count(*)=2 AND sum(CASE WHEN side='DEBIT' THEN base_amount ELSE -base_amount END)=0 FROM journal_details WHERE journal_entry_id=j.id)
 AND EXISTS (SELECT 1 FROM journal_details WHERE journal_entry_id=j.id AND account_code=e.account_code AND side=e.side AND amount=10000 AND base_amount=10000)
 AND EXISTS (SELECT 1 FROM journal_details WHERE journal_entry_id=j.id AND account_code=e.pnl_account AND side=e.pnl_side AND amount=10000 AND base_amount=10000)));
