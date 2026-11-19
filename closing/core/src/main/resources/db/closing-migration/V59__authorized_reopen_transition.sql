-- A reopened calendar and its audit/checklist commit before Master exposes OPEN.
-- The retained operation ID fences local admission until the matching Master receipt is CONFIRMED.
ALTER TABLE closing_calendars ADD COLUMN transition_protocol VARCHAR(20);
ALTER TABLE closing_calendars DROP CONSTRAINT chk_closing_transition_complete;

ALTER TABLE closing_calendars ADD CONSTRAINT chk_closing_transition_complete CHECK (
    (transition_id IS NULL AND transition_target IS NULL AND transition_stage IS NULL
        AND transition_fiscal_period_id IS NULL AND transition_approval_id IS NULL
        AND transition_actor IS NULL AND transition_prepared_at IS NULL
        AND transition_evidence_set_id IS NULL AND transition_protocol IS NULL)
    OR
    (transition_id IS NOT NULL AND transition_target IS NOT NULL AND transition_stage IS NOT NULL
        AND transition_fiscal_period_id IS NOT NULL AND transition_actor IS NOT NULL
        AND transition_prepared_at IS NOT NULL
        AND (transition_protocol IS NULL OR transition_protocol = 'RECEIPT_V1')
        AND ((transition_target = 'OPEN' AND transition_approval_id IS NOT NULL
                AND transition_evidence_set_id IS NULL
                AND ((transition_stage IN ('PREPARED', 'DISPATCHED') AND status = 'CLOSED')
                    OR (transition_stage IN ('AUTHORIZED', 'READY_TO_CONFIRM') AND status = 'OPEN')))
            OR (transition_target = 'CLOSED' AND status = 'IN_PROGRESS'
                AND transition_stage IN ('PREPARED', 'DISPATCHED')
                AND transition_approval_id IS NULL)))
);
