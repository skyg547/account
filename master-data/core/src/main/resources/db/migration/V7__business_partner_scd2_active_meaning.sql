-- Forward-only migration for BusinessPartner SCD2 active flag policy (#261)
-- Closed/terminated historical versions (valid_to != '9999-12-31') were incorrectly marked with use_yn = false during legacy termination.
-- Restore use_yn = true for closed/terminated versions so that queries within their validity window [valid_from, valid_to] accurately reflect their active status at that time.

UPDATE business_partners
SET use_yn = TRUE
WHERE valid_to < DATE '9999-12-31'
  AND use_yn = FALSE;
