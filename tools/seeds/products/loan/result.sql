-- An interest accrual must carry actual journal lineage, not merely a success flag.
SELECT (SELECT COUNT(*) FROM loan_accrual_log WHERE loan_id=6900001 AND accrual_date=DATE '2090-01-15')=1
AND EXISTS (SELECT 1 FROM loan_accrual_log WHERE loan_id=6900001 AND accrual_date=DATE '2090-01-15' AND status='SUCCESS' AND accrued_amount=60000 AND journal_entry_id IS NOT NULL AND journal_no IS NOT NULL)
AND EXISTS (SELECT 1 FROM loans WHERE id=6900001 AND loan_number='GH690-LOAN' AND principal_amount=12000000) AS verified;
