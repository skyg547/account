INSERT INTO sales_invoices (id,invoice_no,customer_code,issue_date,due_date,total_amount,tax_amount,net_amount,status,description,created_by,created_at)
VALUES (6900001,'GH690-AR-001','GH690-CUSTOMER','2090-01-15','2090-01-31',2200,200,2000,'ISSUED','GH690 synthetic sales invoice','GH690','2090-01-15 00:00:00') ON CONFLICT DO NOTHING;
INSERT INTO receivables (id,sales_invoice_id,customer_code,original_amount,outstanding_amount,due_date,status,created_at)
VALUES (6900001,6900001,'GH690-CUSTOMER',2200,2200,'2090-01-31','OPEN','2090-01-15 00:00:00') ON CONFLICT DO NOTHING;
