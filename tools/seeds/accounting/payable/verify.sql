SELECT EXISTS (SELECT 1 FROM purchase_invoices i JOIN payables p ON p.purchase_invoice_invoice_no=i.invoice_no AND p.purchase_invoice_vendor_code=i.vendor_code
WHERE i.id=6900001 AND p.id=6900001 AND i.invoice_no='GH690-AP-001' AND i.vendor_code='GH690-VENDOR' AND p.vendor_code=i.vendor_code AND i.created_by='GH690'
AND i.issue_date='2090-01-15' AND i.due_date='2090-01-31' AND p.due_date=i.due_date AND i.net_amount=1000 AND i.tax_amount=100 AND i.total_amount=1100
AND p.original_amount=i.total_amount AND p.outstanding_amount=1100 AND p.status='OPEN' AND i.status='APPROVED');
