-- GH690 synthetic account and its opening deposit; no identity sequence changes.
INSERT INTO deposit_accounts (id,account_number,customer_code,product_code,balance,currency_code,interest_rate,status,opened_at,valid_from,valid_to,created_by,updated_by)
VALUES (6900001,'GH690-DEPOSIT','GH690-CUSTOMER','GH690-DEPOSIT',1000000,'KRW',0.03,'ACTIVE',DATE '2090-01-01',DATE '2090-01-01',DATE '2090-12-31','GH690','GH690') ON CONFLICT DO NOTHING;
INSERT INTO deposit_transactions (id,account_number,transaction_date,amount,type,balance_after,description)
VALUES (6900001,'GH690-DEPOSIT',TIMESTAMP '2090-01-15 09:00:00',1000000,'DEPOSIT',1000000,'GH690 synthetic opening deposit') ON CONFLICT DO NOTHING;
