-- Seed-only cleanup: preserve completed AND pending journal activity for reviewed reversal.
-- Deleting a successful log/event would erase the only local remote-journal lineage.
DO $gh690$ BEGIN
 LOCK TABLE loans, eir_amortization_schedules, loan_events, loan_accrual_log, loan_disbursals, deferred_items, recalculation_runs, loan_amortization_schedule_entries IN SHARE ROW EXCLUSIVE MODE;
 IF EXISTS (SELECT 1 FROM loans WHERE id=6900001 AND
     (loan_number='GH690-LOAN' AND audit_user='GH690' AND status='ACTIVE'
      AND current_principal_balance=12000000 AND total_interest_paid=0 AND total_principal_paid=0) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM loan_events WHERE loan_id=6900001)
    OR EXISTS (SELECT 1 FROM loan_accrual_log WHERE loan_id=6900001)
    OR EXISTS (SELECT 1 FROM loan_disbursals WHERE loan_id=6900001)
    OR EXISTS (SELECT 1 FROM deferred_items WHERE loan_id=6900001)
    OR EXISTS (SELECT 1 FROM recalculation_runs WHERE loan_id=6900001)
    OR EXISTS (SELECT 1 FROM loan_amortization_schedule_entries WHERE loan_id=6900001)
    OR EXISTS (SELECT 1 FROM eir_amortization_schedules WHERE loan_id=6900001 AND
     (id=6900001 AND audit_user='GH690' AND schedule_date=DATE '2090-01-15'
      AND amortization_journal_entry_id IS NULL AND amortization_journal_entry_slip_no IS NULL) IS NOT TRUE)
 THEN RAISE EXCEPTION 'GH690 loan rollback refused: ownership or subsequent activity requires review'; END IF;
 DELETE FROM eir_amortization_schedules WHERE id=6900001 AND loan_id=6900001 AND audit_user='GH690';
 DELETE FROM loans WHERE id=6900001 AND loan_number='GH690-LOAN' AND audit_user='GH690';
END $gh690$;
