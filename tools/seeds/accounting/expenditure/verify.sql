SELECT EXISTS (SELECT 1 FROM expenditure_resolutions r WHERE r.id=6900001 AND r.resolution_no='GH690-EXP-001' AND r.created_by='GH690' AND r.resolution_date='2090-01-15'
AND r.payment_date='2090-01-31' AND r.dept_code='GH690-DEPT' AND r.payment_account_code='GH690-CASH' AND r.total_amount=3000 AND r.status='DRAFT'
AND r.total_amount=(SELECT sum(d.amount) FROM expenditure_details d WHERE d.expenditure_resolution_id=r.id))
AND (SELECT count(*)=2 FROM expenditure_details WHERE id IN (6900001,6900002) AND expenditure_resolution_id=6900001 AND account_code='GH690-EXPENSE' AND business_partner_code='GH690-VENDOR')
AND EXISTS (SELECT 1 FROM budgets WHERE id=6900001 AND year_month='209001' AND dept_code='GH690-DEPT' AND account_code='GH690-EXPENSE' AND assigned_amount=10000 AND used_amount=3000);
