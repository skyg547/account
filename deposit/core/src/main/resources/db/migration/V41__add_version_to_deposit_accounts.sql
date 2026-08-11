-- Add version column to deposit_accounts table for Optimistic Locking (@Version)
ALTER TABLE deposit_accounts ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
