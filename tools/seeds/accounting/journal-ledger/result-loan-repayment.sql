SELECT count(*)=1 FROM journal_entries e WHERE e.accounting_date='2090-01-15'
AND e.lineage_source_type='LOAN_SCHEDULED_REPAYMENT' AND e.lineage_source_id='6900001:2090-01-15'
AND e.status='POSTED' AND e.currency_code='KRW'
AND (SELECT count(*) FROM journal_details d WHERE d.journal_entry_id=e.id)=3
AND EXISTS(SELECT 1 FROM journal_details d WHERE d.journal_entry_id=e.id AND d.side='DEBIT' AND d.account_code='GH690-REPAYCASH' AND d.base_amount=1060000)
AND EXISTS(SELECT 1 FROM journal_details d WHERE d.journal_entry_id=e.id AND d.side='CREDIT' AND d.account_code='GH690-LOAN' AND d.base_amount=1000000)
AND EXISTS(SELECT 1 FROM journal_details d WHERE d.journal_entry_id=e.id AND d.side='CREDIT' AND d.account_code='115010' AND d.base_amount=60000);
