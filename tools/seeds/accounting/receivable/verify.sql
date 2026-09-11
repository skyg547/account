SELECT EXISTS (SELECT 1 FROM sales_invoices i JOIN receivables r ON r.sales_invoice_id=i.id AND r.customer_code=i.customer_code
WHERE i.id=6900001 AND r.id=6900001 AND i.invoice_no='GH690-AR-001' AND i.customer_code='GH690-CUSTOMER' AND i.created_by='GH690'
AND i.issue_date='2090-01-15' AND i.due_date='2090-01-31' AND r.due_date=i.due_date AND i.net_amount=2000 AND i.tax_amount=200 AND i.total_amount=2200
AND r.original_amount=i.total_amount AND r.outstanding_amount=2200 AND r.status='OPEN' AND i.status='ISSUED');
