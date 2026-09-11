-- Only seeded sources and their scoped balance outputs. Generated FX/ECL/loan journals require separate reviewed reversal/removal.
DELETE FROM sl_balances WHERE balance_date='2090-01-15' AND period='2090-01' AND account_code IN ('GH690-CASH','GH690-EQUITY','GH690-FX','GH690-FXLIAB') AND bp_code='GH690-CUSTOMER' AND dept_code='GH690-DEPT';
DELETE FROM gl_balances WHERE balance_date='2090-01-15' AND period='2090-01' AND account_code IN ('GH690-CASH','GH690-EQUITY','GH690-FX','GH690-FXLIAB');
DELETE FROM sl_entries WHERE id BETWEEN 6900001 AND 6900004 AND lineage_source_type='GH690';
DELETE FROM gl_entries WHERE id BETWEEN 6900001 AND 6900004 AND lineage_source_type='GH690';
DELETE FROM journal_details WHERE id BETWEEN 6900001 AND 6900006 AND journal_entry_id IN (6900001,6900002,6900003) AND audit_user='GH690';
DELETE FROM journal_entries WHERE id IN (6900001,6900002,6900003) AND lineage_source_type='GH690' AND created_by='GH690';
