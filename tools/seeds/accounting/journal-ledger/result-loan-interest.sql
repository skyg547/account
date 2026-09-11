SELECT count(*)=1 FROM journal_entries e WHERE e.accounting_date='2090-01-15'
AND e.lineage_source_type='LOAN' AND e.lineage_source_id='6900001'
AND e.status='POSTED' AND e.currency_code='KRW'
AND (SELECT count(*) FROM journal_details d WHERE d.journal_entry_id=e.id)=2
AND EXISTS(SELECT 1 FROM journal_details d WHERE d.journal_entry_id=e.id AND d.side='DEBIT' AND d.account_code='115010' AND d.base_amount=60000)
AND EXISTS(SELECT 1 FROM journal_details d WHERE d.journal_entry_id=e.id AND d.side='CREDIT' AND d.account_code='410100' AND d.base_amount=60000);
