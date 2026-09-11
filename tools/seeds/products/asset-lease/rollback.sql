-- Parent ownership and payment state must be checked before removing a lease schedule.
-- Depreciation batch outputs may be removed, but lifecycle/history activity needs review.
DO $gh690$ BEGIN
 LOCK TABLE fixed_assets, asset_histories, lease_contracts, lease_payment_schedules, right_of_use_assets, lease_liabilities IN SHARE ROW EXCLUSIVE MODE;
 IF EXISTS (SELECT 1 FROM fixed_assets WHERE id=6900001 AND
     (asset_code='GH690-ASSET' AND asset_name='GH690 synthetic equipment'
      AND dept_code='GH690-DEPT' AND status='ACTIVE') IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM lease_contracts WHERE id=6900001 AND
     (contract_no='GH690-LEASE' AND contract_name='GH690 synthetic lease'
      AND lessor_code='GH690-VENDOR' AND status='ACTIVE') IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM lease_payment_schedules WHERE lease_contract_id=6900001 AND
     (id=6900001 AND status='SCHEDULED' AND actual_payment_amount IS NULL) IS NOT TRUE)
    OR EXISTS (SELECT 1 FROM asset_histories WHERE asset_id=6900001)
    OR EXISTS (SELECT 1 FROM right_of_use_assets WHERE lease_contract_id=6900001)
    OR EXISTS (SELECT 1 FROM lease_liabilities WHERE lease_contract_id=6900001)
 THEN RAISE EXCEPTION 'GH690 asset-lease rollback refused: ownership or subsequent activity requires review'; END IF;
 DELETE FROM lease_payment_schedules WHERE id=6900001 AND lease_contract_id=6900001;
 DELETE FROM lease_contracts WHERE id=6900001 AND contract_no='GH690-LEASE';
 DELETE FROM fixed_assets WHERE id=6900001 AND asset_code='GH690-ASSET';
END $gh690$;
