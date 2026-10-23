-- Keep the original request reason immutable across rejection; existing rejected rows are not inferred or backfilled.
ALTER TABLE master_data_change_requests
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
