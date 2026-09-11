-- Two balanced POSTED source journals and one DRAFT exclusion control.
INSERT INTO journal_entries (id,accounting_date,currency_code,exchange_rate,slip_date,slip_no,status,audit_user,created_by,entry_type,lineage_source_type,lineage_source_id,description)
VALUES (6900001,'2090-01-15','KRW',1,'2090-01-15','GH690-KRW-001','POSTED','GH690','GH690','MANUAL','GH690','GH690-KRW-001','GH690 synthetic opening balance'),
       (6900002,'2090-01-15','USD',1300,'2090-01-15','GH690-USD-001','POSTED','GH690','GH690','MANUAL','GH690','GH690-USD-001','GH690 synthetic FX asset and liability'),
       (6900003,'2090-01-15','KRW',1,'2090-01-15','GH690-DRAFT-001','DRAFT','GH690','GH690','MANUAL','GH690','GH690-DRAFT-001','GH690 exclusion control') ON CONFLICT DO NOTHING;
INSERT INTO journal_details (id,journal_entry_id,side,account_code,amount,base_amount,dept_code,business_partner_code,detail_description,audit_user)
VALUES (6900001,6900001,'DEBIT','GH690-CASH',1000000,1000000,'GH690-DEPT','GH690-CUSTOMER','GH690 KRW debit','GH690'),
       (6900002,6900001,'CREDIT','GH690-EQUITY',1000000,1000000,'GH690-DEPT','GH690-CUSTOMER','GH690 KRW credit','GH690'),
       (6900003,6900002,'DEBIT','GH690-FX',100,130000,'GH690-DEPT','GH690-CUSTOMER','GH690 USD debit','GH690'),
       (6900004,6900002,'CREDIT','GH690-FXLIAB',100,130000,'GH690-DEPT','GH690-CUSTOMER','GH690 USD credit','GH690'),
       (6900005,6900003,'DEBIT','GH690-CASH',999,999,'GH690-DEPT','GH690-CUSTOMER','GH690 excluded draft debit','GH690'),
       (6900006,6900003,'CREDIT','GH690-EQUITY',999,999,'GH690-DEPT','GH690-CUSTOMER','GH690 excluded draft credit','GH690') ON CONFLICT DO NOTHING;
-- Keep posted entry lineage available to ledger/mart readers; balances are batch outputs, never seeded.
INSERT INTO gl_entries (id,journal_detail_id,account_code,currency_code,dr_amount,cr_amount,base_dr_amount,base_cr_amount,posting_date,fiscal_year,fiscal_period,summary,lineage_source_type,lineage_source_id)
SELECT d.id,d.id,d.account_code,e.currency_code,CASE WHEN d.side='DEBIT' THEN d.amount ELSE 0 END,CASE WHEN d.side='CREDIT' THEN d.amount ELSE 0 END,CASE WHEN d.side='DEBIT' THEN d.base_amount ELSE 0 END,CASE WHEN d.side='CREDIT' THEN d.base_amount ELSE 0 END,e.accounting_date,'2090','01','GH690 synthetic posting','GH690',e.lineage_source_id
FROM journal_details d JOIN journal_entries e ON e.id=d.journal_entry_id
WHERE d.id IN (6900001,6900002,6900003,6900004) AND d.audit_user='GH690' AND e.lineage_source_type='GH690' ON CONFLICT DO NOTHING;
INSERT INTO sl_entries (id,journal_detail_id,account_code,business_partner_code,dept_code,currency_code,dr_amount,cr_amount,base_dr_amount,base_cr_amount,posting_date,fiscal_year,fiscal_period,summary,lineage_source_type,lineage_source_id)
SELECT id,journal_detail_id,account_code,'GH690-CUSTOMER','GH690-DEPT',currency_code,dr_amount,cr_amount,base_dr_amount,base_cr_amount,posting_date,fiscal_year,fiscal_period,summary,lineage_source_type,lineage_source_id
FROM gl_entries WHERE id IN (6900001,6900002,6900003,6900004) AND lineage_source_type='GH690' ON CONFLICT DO NOTHING;
