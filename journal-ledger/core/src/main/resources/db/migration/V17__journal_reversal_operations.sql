-- Durable one-to-one claim between a posted source journal and its reversal operation.
-- The source primary key serializes the effective relationship at the database boundary;
-- the reversal UNIQUE constraint prevents one reversal journal from being reused elsewhere.
-- A composite relationship is required because a foreign key to the source ID alone would
-- accept a DRAFT/REQUESTED/APPROVED journal as the accounting event being reversed.
ALTER TABLE journal_entries
    ADD CONSTRAINT uk_journal_entry_id_status UNIQUE (id, status);

CREATE TABLE journal_reversal_operations (
    original_journal_entry_id BIGINT PRIMARY KEY,
    original_journal_status VARCHAR(20) DEFAULT 'POSTED' NOT NULL,
    reversal_journal_entry_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    posted_at TIMESTAMP(6),
    cancelled_at TIMESTAMP(6),
    cancelled_by VARCHAR(50),
    cancellation_reason VARCHAR(500),
    CONSTRAINT uk_journal_reversal_reversal_entry UNIQUE (reversal_journal_entry_id),
    CONSTRAINT fk_journal_reversal_posted_original
        FOREIGN KEY (original_journal_entry_id, original_journal_status)
        REFERENCES journal_entries (id, status),
    CONSTRAINT fk_journal_reversal_reversal_entry
        FOREIGN KEY (reversal_journal_entry_id) REFERENCES journal_entries (id),
    CONSTRAINT ck_journal_reversal_original_status
        CHECK (original_journal_status = 'POSTED'),
    CONSTRAINT ck_journal_reversal_status
        CHECK (status IN ('PENDING', 'POSTED', 'CANCELLED')),
    CONSTRAINT ck_journal_reversal_lifecycle
        CHECK (
            (status = 'PENDING'
                AND posted_at IS NULL
                AND cancelled_at IS NULL
                AND cancelled_by IS NULL
                AND cancellation_reason IS NULL)
            OR
            (status = 'POSTED'
                AND posted_at IS NOT NULL
                AND cancelled_at IS NULL
                AND cancelled_by IS NULL
                AND cancellation_reason IS NULL)
            OR
            (status = 'CANCELLED'
                AND posted_at IS NULL
                AND cancelled_at IS NOT NULL
                AND cancelled_by IS NOT NULL
                AND cancellation_reason IS NOT NULL)
        ),
    CONSTRAINT ck_journal_reversal_distinct_entries
        CHECK (original_journal_entry_id <> reversal_journal_entry_id)
);

-- Supports lifecycle/audit scans. PK and UNIQUE already cover both point-lookup directions.
CREATE INDEX idx_journal_reversal_operation_status
    ON journal_reversal_operations (status);

-- Preserve recognizable active legacy reversals. Deliberately do not sanitize or choose a winner:
-- invalid numeric lineage, missing/non-POSTED source journals, or duplicate active reversals must
-- abort V17 so accounting history can be reconciled explicitly before retrying the migration.
-- Runtime treats reversal entry type case-insensitively after trimming, so migration recognition
-- applies the same policy. JOURNAL_ENTRY lineage uses that normalization while unrelated source
-- types remain outside this relationship; surrounding whitespace on the numeric source ID is benign.
INSERT INTO journal_reversal_operations (
    original_journal_entry_id,
    reversal_journal_entry_id,
    status,
    created_at,
    updated_at,
    posted_at,
    cancelled_at,
    cancelled_by,
    cancellation_reason
)
SELECT
    CAST(TRIM(reversal.lineage_source_id) AS BIGINT),
    reversal.id,
    CASE
        WHEN reversal.status = 'POSTED' THEN 'POSTED'
        ELSE 'PENDING'
    END,
    COALESCE(reversal.created_at, CURRENT_TIMESTAMP),
    COALESCE(reversal.updated_at, reversal.created_at, CURRENT_TIMESTAMP),
    CASE
        WHEN reversal.status = 'POSTED'
            THEN COALESCE(reversal.updated_at, reversal.created_at, CURRENT_TIMESTAMP)
        ELSE NULL
    END,
    NULL,
    NULL,
    NULL
FROM journal_entries reversal
WHERE UPPER(TRIM(reversal.entry_type)) = 'REVERSAL'
  AND UPPER(TRIM(reversal.lineage_source_type)) = 'JOURNAL_ENTRY'
  AND reversal.status IN ('DRAFT', 'REQUESTED', 'APPROVED', 'POSTED');
