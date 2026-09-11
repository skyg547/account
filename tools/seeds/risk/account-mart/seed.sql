-- Minimal, valid source path from IntegratedPositionEtlJobTest, with real FK parents.
INSERT INTO ods_acc_mst (subj_cd,subj_nm,acc_type,bs_class,is_asset,biz_unit_cd,created_at)
VALUES ('GH690-LOAN','GH690 synthetic loan','ASSET','ASSET',TRUE,'GH690',CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO ods_product_mst (prod_cd,prod_nm,prod_category,asset_liability_type,is_active,subj_cd,default_rate_type,rate_type,default_payment_freq,payment_freq,is_excluded,is_off_balance,default_ccf,created_at)
VALUES ('GH690-LOAN','GH690 synthetic loan','LOAN','ASSET',TRUE,'GH690-LOAN','FIXED','FIXED',1,1,FALSE,FALSE,1,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING;
INSERT INTO ods_customer_mst (customer_code,biz_no,cust_nm,cust_type,country_cd,internal_rating,rating_cd,external_rating,industry_cd,industry_nm,is_sme,branch_cd,credit_status_cd)
VALUES ('GH690-MART-CUSTOMER','GH690-SYNTHETIC','GH690 synthetic borrower','CORPORATE','KR','A','A','A','GH690','GH690 synthetic industry',FALSE,'GH690','NORMAL') ON CONFLICT DO NOTHING;
INSERT INTO ods_acc_ledger (acc_no,customer_code,prod_cd,currency,outstd_amt,limit_amt,int_rate,base_rate_cd,spread,next_reset_dt,open_dt,maturity_dt,delinquent_days,repayment_method,grace_period,repayment_freq,branch_cd,biz_unit_cd,is_active)
VALUES ('GH690-MART-ACCOUNT','GH690-MART-CUSTOMER','GH690-LOAN','KRW',1000000,1000000,5,'GH690',0,DATE '2090-04-15',DATE '2089-01-15',DATE '2091-01-15',0,'BULLET',0,1,'GH690','GH690',TRUE) ON CONFLICT DO NOTHING;
INSERT INTO ods_balance_hist (base_dt,account_no,currency,balance)
VALUES (DATE '2090-01-15','GH690-MART-ACCOUNT','KRW',1000000) ON CONFLICT DO NOTHING;
INSERT INTO ods_general_ledger (base_dt,gl_code,currency,branch_cd,balance)
VALUES (DATE '2090-01-15','GH690-LOAN','KRW','GH690',1000000) ON CONFLICT DO NOTHING;
