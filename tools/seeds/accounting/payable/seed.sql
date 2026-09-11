INSERT INTO purchase_invoices (id,invoice_no,vendor_code,issue_date,due_date,total_amount,tax_amount,net_amount,status,description,created_by,created_at)
VALUES (6900001,'GH690-AP-001','GH690-VENDOR','2090-01-15','2090-01-31',1100,100,1000,'APPROVED','GH690 synthetic purchase invoice','GH690','2090-01-15 00:00:00') ON CONFLICT DO NOTHING;
INSERT INTO payables (id,purchase_invoice_invoice_no,purchase_invoice_vendor_code,vendor_code,original_amount,outstanding_amount,due_date,status,created_at)
VALUES (6900001,'GH690-AP-001','GH690-VENDOR','GH690-VENDOR',1100,1100,'2090-01-31','OPEN','2090-01-15 00:00:00') ON CONFLICT DO NOTHING;
