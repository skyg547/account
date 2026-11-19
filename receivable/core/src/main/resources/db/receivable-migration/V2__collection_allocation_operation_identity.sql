-- Existing allocations predate request identity; their primary key provides a distinct legacy identity.
ALTER TABLE collection_allocations ADD COLUMN operation_id VARCHAR(100);
UPDATE collection_allocations SET operation_id = 'LEGACY:' || id WHERE operation_id IS NULL;
ALTER TABLE collection_allocations ALTER COLUMN operation_id SET NOT NULL;
ALTER TABLE collection_allocations ADD CONSTRAINT uk_collection_allocation_operation
    UNIQUE (collection_id, operation_id);
