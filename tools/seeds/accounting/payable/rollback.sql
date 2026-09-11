-- A journal reference is not a local FK: refuse before deleting either invoice or payable.
DO $gh690$ BEGIN
 LOCK TABLE purchase_invoices, payables, payments IN SHARE ROW EXCLUSIVE MODE;
 IF EXISTS (SELECT 1 FROM purchase_invoices WHERE (id=6900001 OR (invoice_no='GH690-AP-001' AND vendor_code='GH690-VENDOR')) AND
     (id=6900001 AND invoice_no='GH690-AP-001' AND vendor_code='GH690-VENDOR' AND created_by='GH690'
      AND status='APPROVED' AND journal_entry_id IS NULL) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM payables WHERE id=6900001 AND
     (purchase_invoice_invoice_no='GH690-AP-001' AND purchase_invoice_vendor_code='GH690-VENDOR'
      AND vendor_code='GH690-VENDOR' AND status='OPEN' AND outstanding_amount=1100
      AND original_amount=1100 AND journal_entry_id IS NULL) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM payments WHERE payable_id=6900001)
 THEN RAISE EXCEPTION 'GH690 payable rollback refused: ownership or subsequent activity requires review'; END IF;
 DELETE FROM payables WHERE id=6900001 AND purchase_invoice_invoice_no='GH690-AP-001' AND vendor_code='GH690-VENDOR';
 DELETE FROM purchase_invoices WHERE id=6900001 AND invoice_no='GH690-AP-001' AND vendor_code='GH690-VENDOR' AND created_by='GH690';
END $gh690$;
