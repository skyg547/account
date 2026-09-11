-- Integrity job validates existing rows; it intentionally has no result table.
SELECT EXISTS (SELECT 1 FROM deposit_accounts WHERE id=6900001 AND account_number='GH690-DEPOSIT' AND balance=1000000 AND interest_rate=0.03 AND status='ACTIVE')
AND (SELECT SUM(amount) FROM deposit_transactions WHERE account_number='GH690-DEPOSIT')=1000000 AS verified;
