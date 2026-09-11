-- Based on AllowanceEclBatchIntegrationTest, moved to the reserved synthetic date.
INSERT INTO cr_customers (id,customer_code,customer_name,customer_type,internal_rating,country_code,is_sme,warning_level,is_active,created_by,updated_by)
VALUES (6900001,'GH690-CUSTOMER','GH690 synthetic borrower','CORPORATE','GH690-A','KR',FALSE,'NORMAL',TRUE,'GH690','GH690') ON CONFLICT DO NOTHING;
-- Unpledged, inactive collateral is a source sample; it does not secure this exposure.
INSERT INTO cr_collaterals (id,customer_id,collateral_code,collateral_type,appraisal_amt,base_haircut,prior_lien_amt,is_active,created_by,updated_by)
VALUES (6900001,6900001,'GH690-COLLATERAL','REAL_ESTATE',500000,0.20,0,FALSE,'GH690','GH690') ON CONFLICT DO NOTHING;
INSERT INTO allowance_exposure_snapshots (base_date,exposure_id,source_system,source_account_no,customer_code,customer_type,is_sme,country_code,industry_code,product_code,product_category,legal_entity_code,branch_code,currency_code,outstanding_amount,undrawn_amount,interest_rate,open_date,maturity_date,delinquent_days,staging,original_rating,current_rating,warning_level,debt_restructured,accounting_account_code,allowance_account_code)
VALUES (DATE '2090-01-15','GH690-EXPOSURE','GH690','GH690-ECL-ACCOUNT','GH690-CUSTOMER','CORPORATE',FALSE,'KR','GH690','GH690-LOAN','LOAN','GH690','GH690','KRW',1000000,200000,0.05,DATE '2089-01-15',DATE '2091-01-15',0,'STAGE1','GH690-A','GH690-A','NORMAL',FALSE,'GH690-LOAN','GH690-ALLOWANCE') ON CONFLICT DO NOTHING;
INSERT INTO cr_product_masters (id,product_code,product_name,ccf_rate,description)
VALUES (6900001,'GH690-LOAN','GH690 synthetic loan',0.5,'GH690') ON CONFLICT DO NOTHING;
INSERT INTO cr_grade_masters (id,rating_code,pd_value,notch_order,description)
VALUES (6900001,'GH690-A',0.01,1,'GH690') ON CONFLICT DO NOTHING;
-- Shared runtime keys must already agree or verify.sql refuses this fixture.
INSERT INTO allowance_model_parameters (param_key,param_value,description) VALUES
('PD_FLOOR',0.0005,'GH690'),('SECURED_LGD_FLOOR',0.20,'GH690'),('UNSECURED_LGD_FLOOR',0.45,'GH690'),('DEFAULT_DISCOUNT_RATE',0.05,'GH690') ON CONFLICT DO NOTHING;
INSERT INTO cr_lgd_segment_masters (id,segment_name,customer_type,collateral_type,lgd_value)
VALUES (6900001,'GH690 unsecured corporate','CORPORATE','UNSECURED',0.45) ON CONFLICT DO NOTHING;
INSERT INTO cr_macro_scenario (id,scenario_type,apply_year,pd_adjustment_factor,probability_weight,description) VALUES
(6900001,'BOOM',2090,0.80,0.20,'GH690'),(6900002,'BASE',2090,1.00,0.60,'GH690'),(6900003,'RECESSION',2090,1.30,0.20,'GH690') ON CONFLICT DO NOTHING;
INSERT INTO allowance_account_mappings (id,product_code,biz_unit_code,currency_code,legal_entity_code,exposure_account_code,allowance_account_code,bad_debt_expense_account_code,reversal_income_account_code,active)
VALUES (6900001,'GH690-LOAN','GH690','KRW','GH690','GH690-LOAN','GH690-ALLOWANCE','GH690-BADDEBT','GH690-REVERSAL',TRUE) ON CONFLICT DO NOTHING;
