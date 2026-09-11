-- Completion marker and principal/interest counters must agree after the remote posting.
SELECT EXISTS (SELECT 1 FROM loans WHERE id=6900001 AND loan_number='GH690-LOAN'
 AND principal_amount=12000000 AND current_principal_balance=11000000
 AND total_principal_paid=1000000 AND total_interest_paid=60000 AND status='ACTIVE')
AND (SELECT count(*)=1 FROM loan_events WHERE loan_id=6900001
 AND event_date='2090-01-15' AND event_type='SCHEDULED_REPAYMENT'
 AND journal_entry_id IS NOT NULL AND journal_entry_slip_no IS NOT NULL)
AND NOT EXISTS (SELECT 1 FROM loan_events WHERE loan_id=6900001
 AND event_type='SCHEDULED_REPAYMENT_PENDING');
