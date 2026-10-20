-- Existing lock rows are treated as active because earlier releases deleted rows on unlock.
-- Before deployment, inspect the duplicate-period queries in closing/docs/schema.md.
-- A duplicate active lock or PENDING approval intentionally fails the UNIQUE constraint
-- below; an operator must reconcile its business/audit history before retrying migration.

ALTER TABLE period_locks ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE period_locks ADD COLUMN active_fiscal_period_id BIGINT;
ALTER TABLE period_locks ADD COLUMN unlocked_by VARCHAR(50);
ALTER TABLE period_locks ADD COLUMN unlocked_at TIMESTAMP;

UPDATE period_locks
SET active_fiscal_period_id = fiscal_period_id;

ALTER TABLE period_locks ADD CONSTRAINT ck_period_lock_active_slot
    CHECK (
        (active = TRUE
            AND active_fiscal_period_id IS NOT NULL
            AND active_fiscal_period_id = fiscal_period_id
            AND unlocked_by IS NULL
            AND unlocked_at IS NULL)
        OR (active = FALSE
            AND active_fiscal_period_id IS NULL
            AND unlocked_by IS NOT NULL
            AND unlocked_at IS NOT NULL)
    );

ALTER TABLE period_locks ADD CONSTRAINT uk_period_lock_active_period
    UNIQUE (active_fiscal_period_id);

ALTER TABLE reopen_approvals ADD COLUMN pending_fiscal_period_id BIGINT;

UPDATE reopen_approvals
SET pending_fiscal_period_id = fiscal_period_id
WHERE status = 'PENDING';

ALTER TABLE reopen_approvals ADD CONSTRAINT ck_reopen_approval_pending_slot
    CHECK (
        (status = 'PENDING'
            AND pending_fiscal_period_id IS NOT NULL
            AND pending_fiscal_period_id = fiscal_period_id)
        OR (status <> 'PENDING'
            AND pending_fiscal_period_id IS NULL)
    );

ALTER TABLE reopen_approvals ADD CONSTRAINT uk_reopen_approval_pending_period
    UNIQUE (pending_fiscal_period_id);
