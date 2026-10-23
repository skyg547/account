-- One global, non-cycling allocator keeps backdated and concurrent slips distinct.
-- Eight base36 characters fit the existing 20-character JE-YYYYMMDD-XXXXXXXX contract.
-- Sequence gaps on rollback are intentional; committed numbers must never be reused.
-- Fail closed if any existing 20-character JE-shaped slip could occupy this namespace.
-- The LIKE guard is deliberately broader than the generated pattern, avoiding a dialect-specific regex.
SELECT 1 / CASE WHEN EXISTS (
    SELECT 1 FROM journal_entries WHERE slip_no LIKE 'JE-________-________'
) THEN 0 ELSE 1 END;

CREATE SEQUENCE journal_slip_no_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 2821109907455
    NO CYCLE;
