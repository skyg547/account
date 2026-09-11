-- ECL target 4808.5714 is stored by journal_details NUMERIC(19,2) as 4808.57.
SELECT (SELECT count(*)=1 FROM journal_entries WHERE lineage_source_type='ECL_PROVISION'
 AND lineage_source_id='6900001|GH690-ALLOWANCE|KRW' AND accounting_date='2090-01-15' AND currency_code='KRW' AND status='DRAFT')
AND EXISTS (SELECT 1 FROM journal_entries j WHERE j.lineage_source_type='ECL_PROVISION' AND j.lineage_source_id='6900001|GH690-ALLOWANCE|KRW'
 AND (SELECT count(*)=2 AND sum(CASE WHEN side='DEBIT' THEN base_amount ELSE -base_amount END)=0 FROM journal_details WHERE journal_entry_id=j.id)
 AND EXISTS (SELECT 1 FROM journal_details WHERE journal_entry_id=j.id AND account_code='GH690-BADDEBT' AND side='DEBIT' AND amount=4808.57 AND base_amount=4808.57)
 AND EXISTS (SELECT 1 FROM journal_details WHERE journal_entry_id=j.id AND account_code='GH690-ALLOWANCE' AND side='CREDIT' AND amount=4808.57 AND base_amount=4808.57));
