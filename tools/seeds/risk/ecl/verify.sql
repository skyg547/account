SELECT EXISTS (SELECT 1 FROM allowance_exposure_snapshots WHERE base_date=DATE '2090-01-15' AND exposure_id='GH690-EXPOSURE' AND outstanding_amount=1000000 AND undrawn_amount=200000 AND staging='STAGE1' AND current_rating='GH690-A' AND interest_rate=0.05 AND maturity_date=DATE '2091-01-15')
AND EXISTS (SELECT 1 FROM cr_customers WHERE id=6900001 AND customer_code='GH690-CUSTOMER' AND customer_type='CORPORATE')
AND EXISTS (SELECT 1 FROM cr_collaterals WHERE id=6900001 AND collateral_code='GH690-COLLATERAL' AND appraisal_amt=500000 AND is_active=FALSE)
AND EXISTS (SELECT 1 FROM cr_product_masters WHERE product_code='GH690-LOAN' AND ccf_rate=0.5)
AND EXISTS (SELECT 1 FROM cr_grade_masters WHERE rating_code='GH690-A' AND pd_value=0.01)
AND (SELECT COUNT(*) FROM allowance_model_parameters WHERE (param_key='PD_FLOOR' AND param_value=0.0005) OR (param_key='SECURED_LGD_FLOOR' AND param_value=0.20) OR (param_key='UNSECURED_LGD_FLOOR' AND param_value=0.45) OR (param_key='DEFAULT_DISCOUNT_RATE' AND param_value=0.05))=4
AND EXISTS (SELECT 1 FROM cr_lgd_segment_masters WHERE customer_type='CORPORATE' AND collateral_type='UNSECURED' AND lgd_value=0.45)
AND (SELECT COUNT(*) FROM cr_macro_scenario WHERE apply_year=2090)=3
AND (SELECT COUNT(*) FROM cr_macro_scenario WHERE apply_year=2090 AND ((scenario_type='BOOM' AND pd_adjustment_factor=0.8 AND probability_weight=0.2) OR (scenario_type='BASE' AND pd_adjustment_factor=1 AND probability_weight=0.6) OR (scenario_type='RECESSION' AND pd_adjustment_factor=1.3 AND probability_weight=0.2)))=3
AND EXISTS (SELECT 1 FROM allowance_account_mappings WHERE id=6900001 AND product_code='GH690-LOAN' AND legal_entity_code='GH690' AND exposure_account_code='GH690-LOAN' AND allowance_account_code='GH690-ALLOWANCE' AND active=TRUE) AS verified;
