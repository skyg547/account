-- Validate the invoice owner before deleting children; collections retain their lineage.
DO $gh690$ BEGIN
 LOCK TABLE sales_invoices, receivables, collection_allocations IN SHARE ROW EXCLUSIVE MODE;
 IF EXISTS (SELECT 1 FROM sales_invoices WHERE id=6900001 AND
     (invoice_no='GH690-AR-001' AND customer_code='GH690-CUSTOMER' AND created_by='GH690'
      AND status='ISSUED') IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM receivables WHERE id=6900001 AND
     (sales_invoice_id=6900001 AND customer_code='GH690-CUSTOMER' AND status='OPEN'
      AND original_amount=2200 AND outstanding_amount=2200) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM collection_allocations WHERE receivable_id=6900001)
 THEN RAISE EXCEPTION 'GH690 receivable rollback refused: ownership or subsequent activity requires review'; END IF;
 DELETE FROM receivables WHERE id=6900001 AND sales_invoice_id=6900001 AND customer_code='GH690-CUSTOMER';
 DELETE FROM sales_invoices WHERE id=6900001 AND invoice_no='GH690-AR-001' AND customer_code='GH690-CUSTOMER' AND created_by='GH690';
END $gh690$;
