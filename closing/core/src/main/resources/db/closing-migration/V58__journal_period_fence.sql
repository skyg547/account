-- Historical evidence stays readable but cannot authorize a new close without a real checkpoint.
ALTER TABLE final_close_evidence_sets ADD COLUMN journal_revision BIGINT;
ALTER TABLE final_close_evidence_sets ADD COLUMN journal_start_date DATE;
ALTER TABLE final_close_evidence_sets ADD COLUMN journal_end_date DATE;
ALTER TABLE final_close_evidence_sets ADD CONSTRAINT chk_evidence_journal_checkpoint CHECK (
    (journal_revision IS NULL AND journal_start_date IS NULL AND journal_end_date IS NULL)
    OR (journal_revision IS NOT NULL AND journal_revision >= 0
        AND journal_start_date IS NOT NULL AND journal_end_date IS NOT NULL
        AND journal_start_date <= journal_end_date AND journal_end_date = ledger_cutoff)
);

-- The exact close command outlives the transition and is released only after reopen commits.
ALTER TABLE closing_calendars ADD COLUMN journal_fence_operation_id VARCHAR(36);
ALTER TABLE closing_calendars ADD COLUMN journal_fence_period_id BIGINT;
ALTER TABLE closing_calendars ADD COLUMN journal_fence_start_date DATE;
ALTER TABLE closing_calendars ADD COLUMN journal_fence_end_date DATE;
ALTER TABLE closing_calendars ADD COLUMN journal_fence_revision BIGINT;
ALTER TABLE closing_calendars ADD COLUMN journal_fence_actor VARCHAR(50);
ALTER TABLE closing_calendars ADD CONSTRAINT chk_calendar_journal_fence CHECK (
    (journal_fence_operation_id IS NULL AND journal_fence_period_id IS NULL
        AND journal_fence_start_date IS NULL AND journal_fence_end_date IS NULL
        AND journal_fence_revision IS NULL AND journal_fence_actor IS NULL)
    OR (journal_fence_operation_id IS NOT NULL AND journal_fence_period_id IS NOT NULL
        AND journal_fence_period_id > 0 AND journal_fence_start_date IS NOT NULL
        AND journal_fence_end_date IS NOT NULL AND journal_fence_start_date <= journal_fence_end_date
        AND journal_fence_revision IS NOT NULL AND journal_fence_revision >= 0
        AND journal_fence_actor IS NOT NULL)
);
ALTER TABLE closing_calendars ALTER COLUMN transition_stage TYPE VARCHAR(30);
ALTER TABLE closing_calendars DROP CONSTRAINT chk_closing_transition_complete;
ALTER TABLE closing_calendars ADD CONSTRAINT chk_closing_transition_complete CHECK (
    (transition_id IS NULL AND transition_target IS NULL AND transition_stage IS NULL
        AND transition_fiscal_period_id IS NULL AND transition_approval_id IS NULL
        AND transition_actor IS NULL AND transition_prepared_at IS NULL
        AND transition_evidence_set_id IS NULL)
    OR (transition_id IS NOT NULL AND transition_target IS NOT NULL AND transition_stage IS NOT NULL
        AND transition_fiscal_period_id IS NOT NULL AND transition_actor IS NOT NULL
        AND transition_prepared_at IS NOT NULL
        AND ((transition_target = 'OPEN' AND transition_approval_id IS NOT NULL
                AND transition_evidence_set_id IS NULL
                AND ((status = 'CLOSED' AND transition_stage IN ('PREPARED', 'DISPATCHED'))
                    OR (status = 'OPEN' AND transition_stage = 'RELEASE_PENDING'
                        AND journal_fence_operation_id IS NOT NULL)))
            OR (transition_target = 'CLOSED' AND status = 'IN_PROGRESS'
                AND transition_approval_id IS NULL
                AND (transition_stage IN ('PREPARED', 'DISPATCHED')
                    OR (transition_stage = 'CANCEL_RELEASE_PENDING'
                        AND journal_fence_operation_id IS NOT NULL
                        AND journal_fence_operation_id = transition_id)))))
);
