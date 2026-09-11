-- Deposit child tables have no account FK. Check every dependent before any delete.
DO $gh690$ BEGIN
 LOCK TABLE deposit_accounts, deposit_transactions, deposit_interest_schedules, deposit_outbox IN SHARE ROW EXCLUSIVE MODE;
 IF EXISTS (SELECT 1 FROM deposit_accounts WHERE (id=6900001 OR account_number='GH690-DEPOSIT') AND
     (id=6900001 AND account_number='GH690-DEPOSIT' AND created_by='GH690' AND customer_code='GH690-CUSTOMER'
      AND balance=1000000 AND status='ACTIVE') IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM deposit_transactions WHERE account_number='GH690-DEPOSIT' AND
     (id=6900001 AND description='GH690 synthetic opening deposit' AND amount=1000000
      AND type='DEPOSIT' AND balance_after=1000000 AND journal_entry_id IS NULL) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM deposit_transactions WHERE account_number='GH690-DEPOSIT'
     AND NOT EXISTS (SELECT 1 FROM deposit_accounts WHERE id=6900001 AND account_number='GH690-DEPOSIT' AND created_by='GH690'))
    OR EXISTS (SELECT 1 FROM deposit_interest_schedules WHERE account_number='GH690-DEPOSIT')
    OR EXISTS (SELECT 1 FROM deposit_outbox WHERE lineage_source_id IN ('6900001','GH690-DEPOSIT'))
 THEN RAISE EXCEPTION 'GH690 deposit rollback refused: ownership or subsequent activity requires review'; END IF;
 DELETE FROM deposit_transactions WHERE id=6900001 AND account_number='GH690-DEPOSIT';
 DELETE FROM deposit_accounts WHERE id=6900001 AND account_number='GH690-DEPOSIT' AND created_by='GH690';
END $gh690$;
