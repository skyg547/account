-- One permanent tuple per business key gives absent-key CREATE and existing history the same lock.
-- Application transactions insert/upsert this row and SELECT it FOR UPDATE before reading versions.
CREATE TABLE master_data_business_key_locks (
    target_type VARCHAR(40) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    CONSTRAINT pk_master_data_business_key_locks PRIMARY KEY (target_type, target_key)
);

-- Validate existing history as well as subsequent writes. Invalid legacy dates stop migration;
-- correcting financial history requires a separately reviewed remediation, never automatic deletion.
ALTER TABLE account_subjects
    ADD CONSTRAINT ck_account_subjects_validity CHECK (valid_from <= valid_to);
ALTER TABLE business_partners
    ADD CONSTRAINT ck_business_partners_validity CHECK (valid_from <= valid_to);
ALTER TABLE departments
    ADD CONSTRAINT ck_departments_validity CHECK (valid_from <= valid_to);
ALTER TABLE products
    ADD CONSTRAINT ck_products_validity CHECK (valid_from <= valid_to);
