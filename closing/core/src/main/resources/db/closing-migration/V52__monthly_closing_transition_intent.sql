-- A single root row fences every monthly workflow mutation while Master outcome is unresolved.
ALTER TABLE closing_calendars ADD COLUMN transition_id VARCHAR(36);
ALTER TABLE closing_calendars ADD COLUMN transition_target VARCHAR(6);
ALTER TABLE closing_calendars ADD COLUMN transition_stage VARCHAR(20);
ALTER TABLE closing_calendars ADD COLUMN transition_fiscal_period_id BIGINT;
ALTER TABLE closing_calendars ADD COLUMN transition_approval_id BIGINT;
ALTER TABLE closing_calendars ADD COLUMN transition_actor VARCHAR(50);
ALTER TABLE closing_calendars ADD COLUMN transition_prepared_at TIMESTAMP;

ALTER TABLE closing_calendars ADD CONSTRAINT chk_closing_transition_complete CHECK (
    (transition_id IS NULL AND transition_target IS NULL AND transition_stage IS NULL
        AND transition_fiscal_period_id IS NULL AND transition_approval_id IS NULL
        AND transition_actor IS NULL AND transition_prepared_at IS NULL)
    OR
    (transition_id IS NOT NULL AND transition_target IS NOT NULL AND transition_stage IS NOT NULL
        AND transition_fiscal_period_id IS NOT NULL AND transition_actor IS NOT NULL
        AND transition_prepared_at IS NOT NULL
        AND transition_stage IN ('PREPARED', 'DISPATCHED')
        AND ((transition_target = 'OPEN' AND status = 'CLOSED' AND transition_approval_id IS NOT NULL)
            OR (transition_target = 'CLOSED' AND status = 'IN_PROGRESS' AND transition_approval_id IS NULL)))
);
