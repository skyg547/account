-- Support the stable owner and administrator PAT list orders without sorting the full table.
CREATE INDEX IF NOT EXISTS idx_pat_owner_page
    ON auth_personal_access_tokens(username, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_pat_admin_page
    ON auth_personal_access_tokens(created_at DESC, id DESC);
