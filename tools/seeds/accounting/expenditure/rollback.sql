-- Manual maintenance only: stop writers first. One DO statement makes refusal atomic.
-- Lock inspected tables so approval/payment or budget changes cannot race the checks.
DO $gh690$ BEGIN
 LOCK TABLE expenditure_resolutions, expenditure_details, ap_payments, budgets IN SHARE ROW EXCLUSIVE MODE;
 IF EXISTS (SELECT 1 FROM expenditure_resolutions WHERE id=6900001 AND
     (resolution_no='GH690-EXP-001' AND created_by='GH690' AND status='DRAFT'
      AND journal_entry_id IS NULL AND lease_contract_id IS NULL AND tax_invoice_id IS NULL
      AND dept_code='GH690-DEPT' AND total_amount=3000) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM ap_payments WHERE expenditure_resolution_id=6900001)
    OR EXISTS (SELECT 1 FROM expenditure_details WHERE expenditure_resolution_id=6900001 AND
     (id IN (6900001,6900002) AND description LIKE 'GH690 synthetic %'
      AND account_code='GH690-EXPENSE' AND business_partner_code='GH690-VENDOR') IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM budgets WHERE id=6900001 AND
     (year_month='209001' AND dept_code='GH690-DEPT' AND account_code='GH690-EXPENSE'
      AND assigned_amount=10000 AND used_amount=3000) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM expenditure_resolutions r JOIN expenditure_details d ON d.expenditure_resolution_id=r.id
     WHERE r.id<>6900001 AND r.dept_code='GH690-DEPT' AND d.account_code='GH690-EXPENSE'
      AND r.resolution_date>=DATE '2090-01-01' AND r.resolution_date<DATE '2090-02-01')
 THEN RAISE EXCEPTION 'GH690 expenditure rollback refused: ownership or subsequent activity requires review'; END IF;
 DELETE FROM expenditure_details WHERE id IN (6900001,6900002) AND expenditure_resolution_id=6900001 AND description LIKE 'GH690 synthetic %';
 DELETE FROM expenditure_resolutions WHERE id=6900001 AND resolution_no='GH690-EXP-001' AND created_by='GH690';
 DELETE FROM budgets WHERE id=6900001 AND year_month='209001' AND dept_code='GH690-DEPT' AND account_code='GH690-EXPENSE';
END $gh690$;
