-- Preserve the checker identity independently from audit_user, which changes again at posting.
ALTER TABLE journal_entries ADD COLUMN approved_by VARCHAR(50);

-- For unposted legacy approvals audit_user is only recoverable as checker evidence when both actors
-- exist and their canonical identities are distinct. Self-approved or makerless history remains NULL.
-- Posted legacy rows also remain NULL because audit_user may already be the poster.
UPDATE journal_entries
SET approved_by = LOWER(TRIM(audit_user))
WHERE status = 'APPROVED'
  AND created_by IS NOT NULL
  AND TRIM(created_by) <> ''
  AND audit_user IS NOT NULL
  AND TRIM(audit_user) <> ''
  AND LOWER(TRIM(created_by)) <> LOWER(TRIM(audit_user));
