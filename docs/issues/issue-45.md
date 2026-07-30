# Issue #45: executable Budget Control foundation

## Background

PRs #151 and #221 did not create a Budget module; they added a placeholder and a generic completion
note. This implementation adds real `budget:core`, `budget:api`, and `budget:batch` projects.

## Domain and architecture

- `BudgetPlan` owns monthly allocation, transfer-in/out, execution, availability, and
  `DRAFT → APPROVED → CLOSED` transitions.
- `BudgetTransfer` preserves a unique request key and source/target lineage.
- `BudgetExecution` preserves a unique source triple and explicit cancellation.
- `BudgetFiscalYearControl` durably gates all mutations after year-end close.
- Application services own `idempotency shard → fiscal year → plan id` lock order,
  same-month transfer/execution rules, and typed failure meaning.
- Persistence adapters own JPA entity conversion, 10,000 preseeded year controls,
  256 preseeded idempotency shards, pessimistic queries, and unique constraints.
- API validates signed JWTs and operation roles, derives actor from JWT `sub`, owns transport/error
  mapping, and is routed explicitly by Gateway. Batch owns required job-parameter parsing only.

All stored amounts follow precision 19/scale 2 with `RoundingMode.UNNECESSARY`.

## Compatibility boundary

The legacy `expenditure-resolution` monthly Budget remains unchanged because its existing data
contains no reliable reservation/commit/release lineage and has no repository migration. The new
bounded context writes only `budget_*` tables. Cross-service migration and reconciliation are
tracked in #17; this Issue does not claim a production-data cutover.

## Verification and rollback

Budget Core 31, API 9, Batch 11, Gateway 33, and existing Expenditure Core 10/API 1 tests pass:
95 affected tests with no failures/errors/skips. Both executable bootJars pass and contain the
PostgreSQL JDBC driver. API/Batch context tests exercise real adapters, Flyway V50, 10,000 year
controls, 256 lock shards, and Hibernate validation; H2 two-thread tests prove first-call
idempotency and close-versus-approval serialization. `git diff --check` and the conflict-marker
scan pass. All independent-review findings were fixed, and the final staged re-review reported
no remaining P0-P3 findings.

Rollback is a normal feature revert; existing Expenditure schema/data is not mutated. Live
PostgreSQL migration/locking and high-cardinality year-end close performance were not exercised
in this local verification.

## Integration

PR #246 merged source commit `5f06cad1` as merge commit `db7feeb4`. `Fixes #45` closed the
Issue, the required GitHub check passed, and the remote feature branch was deleted.
