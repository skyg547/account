-- Existing calendars and their checklist evidence are the first completed/open cycle.
-- Forward-only migration: historical task and gate rows remain in place for audit lookup.
ALTER TABLE closing_calendars ADD COLUMN cycle_number INTEGER NOT NULL DEFAULT 1;
ALTER TABLE closing_calendars ADD COLUMN last_source_changed_at TIMESTAMP;
ALTER TABLE closing_tasks ADD COLUMN cycle_number INTEGER NOT NULL DEFAULT 1;
ALTER TABLE closing_gates ADD COLUMN cycle_number INTEGER NOT NULL DEFAULT 1;

-- A legacy reopen has no reliable child-row cycle provenance. Leave all existing evidence in
-- cycle 1 and fence the calendar at cycle 2 so it cannot reuse passed/completed rows after deploy.
-- Operators must define fresh tasks and gates for the active cycle before another close.
UPDATE closing_calendars SET cycle_number = 2 WHERE reopened_at IS NOT NULL;

ALTER TABLE closing_calendars ADD CONSTRAINT chk_closing_calendar_cycle_positive CHECK (cycle_number > 0);
ALTER TABLE closing_tasks ADD CONSTRAINT chk_closing_task_cycle_positive CHECK (cycle_number > 0);
ALTER TABLE closing_gates ADD CONSTRAINT chk_closing_gate_cycle_positive CHECK (cycle_number > 0);

CREATE INDEX idx_closing_task_calendar_cycle_order
    ON closing_tasks (calendar_id, cycle_number, task_order);
CREATE INDEX idx_closing_gate_calendar_cycle
    ON closing_gates (calendar_id, cycle_number);
