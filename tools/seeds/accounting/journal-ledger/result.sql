-- Run immediately after reaggregation, before any workflow posts further GH690 journals.
WITH expected(account_code,currency_code,debit,credit,ending) AS (VALUES
 ('GH690-CASH','KRW',1000000::numeric,0::numeric,1000000::numeric),
 ('GH690-EQUITY','KRW',0,1000000,-1000000),
 ('GH690-FX','USD',130000,0,130000),
 ('GH690-FXLIAB','USD',0,130000,-130000))
SELECT (SELECT count(*)=4 FROM gl_balances b JOIN expected e USING(account_code,currency_code) WHERE b.balance_date='2090-01-15' AND b.period='2090-01' AND b.beginning_balance=0 AND b.debit_amount=e.debit AND b.credit_amount=e.credit AND b.ending_balance=e.ending)
AND (SELECT count(*)=4 FROM sl_balances b JOIN expected e USING(account_code,currency_code) WHERE b.balance_date='2090-01-15' AND b.period='2090-01' AND b.bp_code='GH690-CUSTOMER' AND b.dept_code='GH690-DEPT' AND b.beginning_balance=0 AND b.debit_amount=e.debit AND b.credit_amount=e.credit AND b.ending_balance=e.ending);
