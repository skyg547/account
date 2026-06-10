ALTER TABLE loan_disbursals ADD COLUMN IF NOT EXISTS journal_entry_slip_no VARCHAR(30);
ALTER TABLE deferred_items ADD COLUMN IF NOT EXISTS initial_journal_entry_slip_no VARCHAR(30);
ALTER TABLE recalculation_runs ADD COLUMN IF NOT EXISTS adjustment_journal_entry_slip_no VARCHAR(30);
