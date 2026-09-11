-- Synthetic GH690 only; no identity sequence advancement or pre-existing row updates.
INSERT INTO account_subjects (id, fixed_asset, unsettled, valid_from, valid_to, created_at, updated_at, balance_type, code, account_type, category, audit_user, name) VALUES
(6900001, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-CASH', 'ASSETS', 'ASSETS', 'GH690', 'GH690 synthetic GH690-CASH'),
(6900002, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-FX', 'ASSETS', 'ASSETS', 'GH690', 'GH690 synthetic GH690-FX'),
(6900003, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', 'GH690-EQUITY', 'EQUITY', 'EQUITY', 'GH690', 'GH690 synthetic GH690-EQUITY'),
(6900004, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-EXPENSE', 'EXPENSES', 'EXPENSES', 'GH690', 'GH690 synthetic GH690-EXPENSE'),
(6900005, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', 'GH690-FXGAIN', 'REVENUE', 'REVENUE', 'GH690', 'GH690 synthetic GH690-FXGAIN'),
(6900006, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-FXLOSS', 'EXPENSES', 'EXPENSES', 'GH690', 'GH690 synthetic GH690-FXLOSS'),
(6900007, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-LOAN', 'ASSETS', 'ASSETS', 'GH690', 'GH690 synthetic GH690-LOAN'),
(6900008, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', 'GH690-ALLOWANCE', 'ASSETS', 'ASSETS', 'GH690', 'GH690 synthetic GH690-ALLOWANCE'),
(6900009, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-BADDEBT', 'EXPENSES', 'EXPENSES', 'GH690', 'GH690 synthetic GH690-BADDEBT'),
(6900010, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', 'GH690-REVERSAL', 'REVENUE', 'REVENUE', 'GH690', 'GH690 synthetic GH690-REVERSAL'),
(6900011, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-DEPR', 'EXPENSES', 'EXPENSES', 'GH690', 'GH690 synthetic GH690-DEPR'),
(6900012, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', 'GH690-ACCDEPR', 'ASSETS', 'ASSETS', 'GH690', 'GH690 synthetic GH690-ACCDEPR'),
(6900013, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', 'GH690-INTINC', 'REVENUE', 'REVENUE', 'GH690', 'GH690 synthetic GH690-INTINC'),
(6900014, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', 'GH690-INTREC', 'ASSETS', 'ASSETS', 'GH690', 'GH690 synthetic GH690-INTREC'),
(6900015, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', 'GH690-FXLIAB', 'LIABILITIES', 'LIABILITIES', 'GH690', 'GH690 synthetic GH690-FXLIAB'),
(6900016, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'DEBIT', '115010', 'ASSETS', 'ASSETS', 'GH690', 'GH690 synthetic 115010'),
(6900017, false, false, DATE '2090-01-01', DATE '2090-01-31', TIMESTAMP '2090-01-15 00:00:00', TIMESTAMP '2090-01-15 00:00:00', 'CREDIT', '410100', 'REVENUE', 'REVENUE', 'GH690', 'GH690 synthetic 410100')
ON CONFLICT DO NOTHING;
INSERT INTO account_subjects (id,fixed_asset,unsettled,valid_from,valid_to,created_at,updated_at,balance_type,code,account_type,category,audit_user,name) VALUES (6900018,false,false,'2090-01-01','2090-01-31','2090-01-15','2090-01-15','DEBIT','GH690-REPAYCASH','ASSETS','ASSETS','GH690','GH690 synthetic repayment cash') ON CONFLICT DO NOTHING;
INSERT INTO account_subjects (id,fixed_asset,unsettled,valid_from,valid_to,created_at,updated_at,balance_type,code,account_type,category,audit_user,name) VALUES (6900019,true,false,'2090-01-01','2090-01-31','2090-01-15','2090-01-15','DEBIT','GH690-ASSET','ASSETS','ASSETS','GH690','GH690 synthetic fixed asset') ON CONFLICT DO NOTHING;
INSERT INTO currencies (id, currency_code, valid_from, valid_to, currency_name, audit_user)
VALUES (6900001,'KRW','2090-01-01','2090-01-31','GH690 synthetic KRW','GH690'),
       (6900002,'USD','2090-01-01','2090-01-31','GH690 synthetic USD','GH690')
ON CONFLICT DO NOTHING;
INSERT INTO exchange_rates (id,effective_date,from_currency_code,to_currency_code,rate,created_at)
VALUES (6900001,'2090-01-15','USD','KRW',1400,'2090-01-15 00:00:00') ON CONFLICT DO NOTHING;
INSERT INTO fiscal_periods (id,start_date,end_date,fiscal_period,fiscal_year,closing_status,audit_user)
VALUES (6900001,'2090-01-01','2090-01-31','01','2090','OPEN','GH690') ON CONFLICT DO NOTHING;
INSERT INTO business_partners (id,use_yn,valid_from,valid_to,business_partner_code,partner_type,risk_rating,kyc_status,business_partner_name,audit_user)
VALUES (6900001,true,'2090-01-01','2090-01-31','GH690-VENDOR','VENDOR','LOW','APPROVED','GH690 synthetic vendor','GH690'),
       (6900002,true,'2090-01-01','2090-01-31','GH690-CUSTOMER','CUSTOMER','LOW','APPROVED','GH690 synthetic customer','GH690') ON CONFLICT DO NOTHING;
INSERT INTO departments (id,valid_from,valid_to,created_at,updated_at,code,type,audit_user,name)
VALUES (6900001,'2090-01-01','2090-01-31','2090-01-15 00:00:00','2090-01-15 00:00:00','GH690-DEPT','COST_CENTER','GH690','GH690 synthetic department') ON CONFLICT DO NOTHING;
