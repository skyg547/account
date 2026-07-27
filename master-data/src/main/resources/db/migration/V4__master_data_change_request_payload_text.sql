-- Preserve the checksum of the already versioned V2 migration and correct the large JSON column forward.
-- @todo A clean PostgreSQL bootstrap still needs a vendor-specific pre-V2 baseline for the legacy payload type.
ALTER TABLE master_data_change_requests
    ALTER COLUMN payload_json SET DATA TYPE TEXT;
