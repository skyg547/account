ALTER TABLE loan_accrual_log ADD COLUMN IF NOT EXISTS journal_entry_id BIGINT;
ALTER TABLE eir_amortization_schedules ADD COLUMN IF NOT EXISTS amortization_journal_entry_slip_no VARCHAR(50);
ALTER TABLE loan_events ADD COLUMN IF NOT EXISTS journal_entry_slip_no VARCHAR(50);
