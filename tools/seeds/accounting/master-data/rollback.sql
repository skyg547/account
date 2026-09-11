-- Stop the GH690 batch/API workflow first; remove dependent context fixtures before master data.
DELETE FROM departments WHERE id=6900001 AND code='GH690-DEPT' AND audit_user='GH690';
DELETE FROM business_partners WHERE id IN (6900001,6900002) AND business_partner_code IN ('GH690-VENDOR','GH690-CUSTOMER') AND audit_user='GH690';
DELETE FROM fiscal_periods WHERE id=6900001 AND fiscal_year='2090' AND fiscal_period='01' AND audit_user='GH690';
DELETE FROM exchange_rates WHERE id=6900001 AND effective_date='2090-01-15' AND from_currency_code='USD' AND to_currency_code='KRW' AND rate=1400;
DELETE FROM currencies WHERE id IN (6900001,6900002) AND audit_user='GH690';
DELETE FROM account_subjects WHERE id BETWEEN 6900001 AND 6900019 AND audit_user='GH690' AND name LIKE 'GH690 synthetic %';
