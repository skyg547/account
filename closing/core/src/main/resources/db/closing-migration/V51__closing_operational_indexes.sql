-- Forward-only index convergence for databases that were baselined at version 49.
-- Clean databases already receive the same index names in V49, so these statements are no-ops.

CREATE INDEX IF NOT EXISTS idx_closing_task_calendar_order
    ON closing_tasks (calendar_id, task_order);
CREATE INDEX IF NOT EXISTS idx_closing_gate_calendar
    ON closing_gates (calendar_id);
CREATE INDEX IF NOT EXISTS idx_closing_audit_calendar_time
    ON closing_audit_logs (calendar_id, action_at);
CREATE INDEX IF NOT EXISTS idx_period_lock_fiscal_period
    ON period_locks (fiscal_period_id);
CREATE INDEX IF NOT EXISTS idx_reopen_approval_period_requested
    ON reopen_approvals (fiscal_period_id, requested_at);
CREATE INDEX IF NOT EXISTS idx_valuation_batch_fiscal_period
    ON valuation_batches (fiscal_period_id);
CREATE INDEX IF NOT EXISTS idx_provision_batch_fiscal_period
    ON provision_batches (fiscal_period_id);
CREATE INDEX IF NOT EXISTS idx_closing_adjustment_fiscal_period
    ON closing_adjustments (fiscal_period_id);
CREATE INDEX IF NOT EXISTS idx_closing_adjustment_journal_entry
    ON closing_adjustments (journal_entry_id);
