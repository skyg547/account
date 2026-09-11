# GH690 synthetic accounting fixtures

Each child directory owns SQL for exactly one database. `expenditure/` maps to
`expenditure-resolution`; the other directory names match their bounded contexts.
Use `tools/seed-external-dev.py` to inject approved external-dev credentials without
printing values and execute each seed plus `verify.sql` in one transaction. Each
verification returns exactly one boolean. A false assertion must roll back the
transaction; `ON CONFLICT DO NOTHING` alone is not proof of a successful rerun.

All business dates are in January 2090, with batch date **2090-01-15**. IDs start
at **6900001**, are explicit per table, and do not advance identity sequences.
Actor, descriptive names, business keys and synthetic account codes use `GH690`.
The loan module currently requires numeric chart codes `115010` and `410100`;
their names and audit actor remain synthetic. No personal identity, bank number,
real customer, production endpoint or copied frontend person appears in these fixtures.

## Data and exact assertions

| Context | Inputs | Expected result |
| --- | --- | --- |
| master-data | 19 chart accounts, KRW/USD currencies, USD→KRW rate, January fiscal period, 2 business partners, 1 department | Unique active chart/currency keys on the batch date; rate 1400; fiscal period OPEN |
| journal-ledger | 2 POSTED headers, 4 posted details, matching GL/SL entries; 1 DRAFT header with 2 details | KRW cash debit/equity credit 1,000,000; USD FX asset debit/liability credit 100, each base amount 130,000; DRAFT 999 excluded |
| payable | Purchase invoice + related OPEN payable | Net 1,000 + tax 100 = original and outstanding 1,100 |
| receivable | Sales invoice + related OPEN receivable | Net 2,000 + tax 200 = original and outstanding 2,200 |
| expenditure | DRAFT resolution, two details, budget reservation | Detail 1,000 + 2,000 = resolution 3,000; budget assigned 10,000, used 3,000 |

`journal-ledger/verify.sql` checks source loading. Run
`journal-ledger/result.sql` immediately after `dailyBalanceReaggregationJob`
with `startDate=2090-01-15 endDate=2090-01-15`. It checks four GL and four SL
balance keys, zero beginning balances, exact debit/credit amounts and signed ending
balances. KRW endings are +1,000,000/-1,000,000; USD endings are
+130,000/-130,000 **in reporting/base currency**, consistent with
`LedgerService.updateDailyBalances`. The excluded DRAFT must not add 999.

The same USD posted source is consumed by Closing FX. At rate 1,400, asset
appreciation is 100 × 1,400 − 130,000 = **10,000 gain**; the equal liability
appreciation is **10,000 loss**. Expect two balanced adjustment journals, not a
single 10,000 net gain. Keep automatic posting disabled so those drafts do not
change subsequent original-balance assertions. Configure FX gain/loss accounts
`GH690-FXGAIN`/`GH690-FXLOSS`; core retains the calculation and side decisions.

`journal-ledger/result-fx.sql` checks those two DRAFT journals for batch ID
6900001. `result-ecl.sql` checks one DRAFT ECL provision from the same batch ID,
with debit `GH690-BADDEBT` and credit `GH690-ALLOWANCE`: source target 4808.5714
is persisted as 4808.57 by journal `NUMERIC(19,2)` columns.

Risk reconciliation uses `GH690-CASH`, KRW, expected 1,000,000 on the batch date.
ECL supports `GH690-LOAN`, `GH690-ALLOWANCE`, `GH690-BADDEBT` and
`GH690-REVERSAL`. Cross-context references are codes only; this package never
creates remote tables in Closing, product or risk databases. `closing/seed.sql` is an explicit no-op and its verification checks the V51-era public
migration, required tables, and absence of a lock on the synthetic period/date.
Closing-owned seed rows are unnecessary for FX/ECL: its inputs are source contracts and the master
fiscal period. Actual run outputs must be verified in their owner databases.

## Execution and rollback constraints

- Load master-data first, followed by journal-ledger and the other contexts.
  One transaction per context, one batch at a time, no date-wide simultaneous work.
- Treat the date and IDs as reserved. Abort on any collision or incompatible
  existing data; do not update existing rows to force a passing assertion.
  Existing active numeric chart codes or currencies on the synthetic date can
  cause a cardinality conflict and require an explicit integration decision.
- Journal reaggregation deletes/rebuilds **all** balances in its date range,
  not only GH690 balances. The runner must reject non-GH690 activity and existing
  unrelated balances on the reserved date before launch. Do not widen the range.
  FX scans all foreign POSTED entries up to the valuation date, so the runner must
  also establish that no unrelated foreign history is eligible.
- `rollback.sql` files are manual cleanup plans, never part of normal verification.
  Stop the GH690 workflow and inspect synthetic lineage first. Run each rollback
  in a transaction, remove product/risk and accounting dependants before
  master-data, and retain Batch job execution evidence.
- Rollback uses exact IDs/business markers and child-before-parent deletes;
  foreign keys deliberately refuse removal when later payments/collections exist.
  It does not reset sequences, truncate tables, cascade, delete Batch metadata,
  or reverse generated FX/ECL/loan journals. After generated/posting activity,
  review and reverse that activity first; the seed-only rollback is insufficient.
- Expenditure, payable and receivable cleanup runs as one atomic `DO` statement:
  all ownership/state/reference checks precede all deletes. A journal-linked,
  approved/requested expenditure, AP payment, invoice journal, changed balance,
  collection allocation or parent collision raises an exception and preserves
  every row. Expenditure also refuses linked lease/tax invoices and a budget used
  by another resolution. Never clear these references just to permit cleanup.
  These manual statements take `SHARE ROW EXCLUSIVE` locks on inspected tables,
  blocking their writes until transaction end. Stop API/batch writers first,
  use a bounded lock timeout and finish the transaction promptly. Cross-database
  activity cannot be discovered or reversed by these local checks.
- No migrations, DDL, grants, cross-database joins or runtime permissions are
  changed. Multirow inserts avoid N+1 access. PK and existing business-key indexes
  bound the tiny fixture writes; checks use aggregate reads. Transaction rollback
  prevents a failed child insert or assertion from leaving partial data.

Rollback guard regression (repository root):
`python3 -m unittest discover -s tools/seeds/tests -v`.
The fixtures execute the actual six accounting/product rollback predicates and
deletes against canonical migration/seed shapes in an in-memory SQLite database.
They cover clean/repeated cleanup and refusal before any delete on ownership,
payment, journal and lifecycle changes. They do not execute PostgreSQL PL/pgSQL
or prove its locking behavior; a separate approved PostgreSQL check is required
before using these manual cleanup plans.

## Source references

The fixture shapes are adapted to current PostgreSQL migrations rather than
copied verbatim from UI mock DTOs:

- `frontend/src/mocks/master.ts`: chart hierarchy and balance-side examples;
  V6 `master-data` baseline supplies actual enums and temporal validity columns.
- `frontend/src/mocks/ledger.ts`: balanced debit/credit lines and POSTED versus
  DRAFT examples; V11 `journal-ledger` baseline supplies canonical journal,
  GL/SL entry and balance tables. `BalanceReaggregationBatchConfig` filters POSTED.
- `frontend/src/mocks/expenditure.ts`: invoice/resolution/detail shapes;
  `payable`, `receivable`, `expenditure-resolution` V1 baselines define actual
  FK cardinality and amount/status checks.
- `payable/core/src/test/java/com/ho/account/expenditure/domain/PayableTest.java`
  and `receivable/core/src/test/java/com/ho/account/receivable/domain/ReceivableTest.java`
  define original/outstanding balance invariants.
- `expenditure-resolution/core/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionBudgetIntegrationTest.java`
  defines reservation of budget by resolution lines.
- `closing/core/src/main/java/com/ho/account/closing/application/service/FxValuationService.java`
  and its test establish reporting-currency revaluation and debit/credit behavior.
