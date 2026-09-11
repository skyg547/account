SELECT EXISTS (SELECT 1 FROM ods_acc_mst WHERE subj_cd='GH690-LOAN' AND is_asset=TRUE)
AND EXISTS (SELECT 1 FROM ods_product_mst WHERE prod_cd='GH690-LOAN' AND subj_cd='GH690-LOAN' AND prod_category='LOAN' AND is_excluded=FALSE)
AND EXISTS (SELECT 1 FROM ods_customer_mst WHERE customer_code='GH690-MART-CUSTOMER' AND cust_type='CORPORATE' AND internal_rating='A')
AND EXISTS (SELECT 1 FROM ods_acc_ledger WHERE acc_no='GH690-MART-ACCOUNT' AND outstd_amt=1000000 AND limit_amt=1000000 AND is_active=TRUE)
AND EXISTS (SELECT 1 FROM ods_balance_hist WHERE base_dt=DATE '2090-01-15' AND account_no='GH690-MART-ACCOUNT' AND balance=1000000)
AND EXISTS (SELECT 1 FROM ods_general_ledger WHERE base_dt=DATE '2090-01-15' AND gl_code='GH690-LOAN' AND currency='KRW' AND balance=1000000) AS verified;
