-- One durable GL identity and one durable SL identity per source detail.
-- Fail on legacy duplicates/null references: financial history must be reconciled,
-- never silently deleted or rewritten by a schema migration.
ALTER TABLE gl_entries ALTER COLUMN journal_detail_id SET NOT NULL;
ALTER TABLE sl_entries ALTER COLUMN journal_detail_id SET NOT NULL;
ALTER TABLE gl_entries ADD CONSTRAINT uk_gl_entries_journal_detail UNIQUE (journal_detail_id);
ALTER TABLE sl_entries ADD CONSTRAINT uk_sl_entries_journal_detail UNIQUE (journal_detail_id);
