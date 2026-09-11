-- Bounded context / database name: expenditure-resolution.
INSERT INTO expenditure_resolutions (id,resolution_no,title,resolution_date,payment_date,dept_code,payment_account_code,total_amount,status,created_at,created_by)
VALUES (6900001,'GH690-EXP-001','GH690 synthetic expense resolution','2090-01-15','2090-01-31','GH690-DEPT','GH690-CASH',3000,'DRAFT','2090-01-15 00:00:00','GH690') ON CONFLICT DO NOTHING;
INSERT INTO expenditure_details (id,expenditure_resolution_id,account_code,amount,business_partner_code,description)
VALUES (6900001,6900001,'GH690-EXPENSE',1000,'GH690-VENDOR','GH690 synthetic item A'),(6900002,6900001,'GH690-EXPENSE',2000,'GH690-VENDOR','GH690 synthetic item B') ON CONFLICT DO NOTHING;
INSERT INTO budgets (id,year_month,dept_code,account_code,assigned_amount,used_amount)
VALUES (6900001,'209001','GH690-DEPT','GH690-EXPENSE',10000,3000) ON CONFLICT DO NOTHING;
