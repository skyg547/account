# Issue #44: GL/Sub-ledger domain modeling

## Why this change exists

The earlier automated PRs for Issue #44 only added a five-line placeholder and a generic completion
note. The placeholder was later removed, while the production flow continued to assemble mutable
JPA ledger entities inside `PostingService`.

The repository already had useful Journal, GL, SL, JPA, and JDBC bulk behavior. This change keeps
that behavior and establishes one domain authority instead of creating another skeleton.

## Domain contract

- `Debit` and `Credit` are immutable value objects. Their distinct types make accounting direction
  explicit instead of relying on method argument order.
- `AccountingPrecision` matches ledger storage at precision 19/scale 2 and exchange-rate storage at
  precision 19/scale 8. It uses `RoundingMode.UNNECESSARY`; a caller must explicitly finalize an
  upstream IFRS 9 calculation before posting it.
- `JournalEntry` owns the `JournalDetail` collection and validates at least one line on each side,
  positive line amounts, transaction-currency balance, and base-currency balance.
- Only an `APPROVED` entry with persisted header/detail IDs can become a `GeneralLedger`.
- `GeneralLedger` freezes every posting field used by both GL and SL: source detail, account,
  partner/department dimensions, period, debit/credit, base amounts, and lineage.

## Hexagonal boundary

`PostingService` creates the aggregate and calls `LedgerEntryPersistencePort.save(GeneralLedger)`.
The JPA adapter maps the snapshot to `GlEntry`/`SlEntry`; the JDBC bulk adapter binds the same
snapshot directly to batch SQL. Application code no longer constructs persistence entities.

The unused `Money`, `GlAccountBalance`, `GlBalanceType`, and `GlAccountBalanceRepository` parallel
authority was removed. `GlBalance`/`SlBalance` remain the active daily balance projections.

## Verification and rollback

- Journal Ledger Core 35, API 2, Batch 3 tests passed.
- Loan Core 30, Expenditure Core 10/API 1, and Closing Batch 12 tests passed; all 93 affected
  tests completed with no failures, errors, or skips.
- Journal Ledger API/Batch and Closing Batch bootJars passed.
- JPA/JDBC mapping parity, source-detail lineage, precision rejection, immutable snapshots, and both
  transaction/base-currency balance checks have focused tests.
- Independent review found and verified fixes for the downstream status-fixture compilation regression
  and the aggregate-total precision overconstraint; re-review reported no P0-P3 findings.

Rollback is a normal revert of the Issue #44 feature commit. There is no schema or production-data
mutation because the code policy matches existing `DECIMAL(19,2)` columns. PostgreSQL bulk load and
lock behavior remain a deployment verification item.

## Integration

PR #238 merged reviewed source commit `d8c0504c` as merge commit `299746a7`. Its `Fixes #44`
keyword closed the Issue, and the remote feature branch was deleted after verification.
