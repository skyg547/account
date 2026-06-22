ALTER TABLE ods_product_mst
    ADD COLUMN IF NOT EXISTS asset_liability_type VARCHAR(20);

ALTER TABLE ods_product_mst
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
