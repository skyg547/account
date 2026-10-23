# Tier 2 Core Accounting & General Ledger audit

- **Audit date:** 2026-09-24 (Asia/Seoul)
- **Repository/worktree:** `/tmp/account-tier2-audit-20260924`
- **Branch:** `agent/tier2-gl-closing-audit-20260924`
- **Audited commit:** `a97d10ab6efc2570a88f83d630242cd748f8be57`
- **Scope:** Journal Ledger and Closing core/API/batch; selected contracts, shared precision/dependencies, Master Data period/account boundaries, Gateway identity forwarding and ECL summary lineage.
**Disposition:** Audit complete; findings are local remediation drafts. Production behavior has not been changed or certified safe.

## Assessment

The system has substantial controls for exact journal arithmetic, historical account lookup, immutable posting snapshots, and daily EOD/BOD transitions. All **369 existing tests in 69 suites passed** on a forced fresh offline run. These results do not establish end-to-end financial integrity.

This inspection identifies **42 actionable issues: 26 P1 and 16 P2**, split into 21 Journal Ledger and 21 Closing drafts. The most consequential failures concern authorization/separation of duties, posted-history protection, duplicate/lost financial effects, cutoff races, and incorrect FX/ECL/annual-close calculations. Reliance on the affected automated posting and closing paths should be gated on the relevant P1 remediations and integration evidence. The audit does not assert that production data has already been corrupted.

**17 diagnostic probes passed their observation assertions**: one grouped positive-control probe and 16 defect-reproduction scenarios. A probe PASS means the described existing behavior was reproduced; it does not mean the defect is fixed. These are separate from the 369 Gradle tests. Most probes execute real domain/application code with controlled ports; P12 executes the real FX source SQL against synthetic in-memory H2 tables.

Examples with concrete financial consequences:

- Exact debit/credit checks accept USD100 at exchange rate1300 with base100 on both sides. Balanced amounts alone do not establish correct currency conversion (GL08/P05).
- JSON amount 900719925474099.11 loses one cent before BigDecimal validation in the default untyped event path (GL09/P06).
- A historical +10 posting changes day1 but leaves an existing day2 ending balance120 instead of130 (GL12/P09).
- A posted KRW200 FX adjustment is excluded from the next source read, producing another200 gain at the same rate (CL15/P12).
- USD100 target allowance minus an existing KRW104000 base balance produces a103900 release, rather than USD20 additional provision (CL17/P14).
- Annual closing accepts an arbitrary CASH_ASSET destination and reuses a1000 draft after source income rises to1500 (CL05/CL06/P17).

## Verification evidence

### Commands and results

The exact requested command was run first:

```bash
./gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test :closing:core:test :closing:api:test :closing:batch:test --offline --no-daemon --console=plain --max-workers=2
```

It returned exit0 / BUILD SUCCESSFUL, but **all 52 actionable tasks were UP-TO-DATE**. This is recorded as a successful baseline invocation, not fresh execution.

The same command was then run with `--rerun-tasks`. It returned exit0 / BUILD SUCCESSFUL, **52 tasks executed**, with the following XML results:

| Project | Suites | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| journal-ledger:core | 18 | 117 | 0 | 0 | 0 |
| journal-ledger:api | 11 | 34 | 0 | 0 | 0 |
| journal-ledger:batch | 3 | 5 | 0 | 0 | 0 |
| closing:core | 15 | 119 | 0 | 0 | 0 |
| closing:api | 11 | 74 | 0 | 0 | 0 |
| closing:batch | 11 | 20 | 0 | 0 | 0 |
| **Total** | **69** | **369** | **0** | **0** | **0** |

Gradle is 8.7; the repository declares a Java 17 toolchain. The shell/probe runtime is OpenJDK 21.0.12.1. The build reports deprecated Gradle features; no dependency download or package installation was performed. Build output is evidence of tests/compilation, not a live application, real PostgreSQL deployment, or operational batch execution.

Evidence files:

- [test-summary.json](test-summary.json): machine-readable runs, per-suite/per-case results and probe outcomes.
- [Requested baseline log](evidence/gradle-baseline.log) and [fresh run log](evidence/gradle-fresh.log).
- [Fresh JUnit XML snapshots](evidence/test-results/) for all six projects.
- [AuditProbes.java](evidence/AuditProbes.java), [probe results](evidence/probe-results.json), [probe console log](evidence/probes.log), and [reproduction instructions](evidence/README.md).
- [source-inventory.json](source-inventory.json): SHA-256 fingerprints of 276 scoped Java/SQL/build/document files, excluding build output, runtime configuration and archived docs. Inventory membership is not a claim that each line received equal review depth.

Temporary diagnostic setup initially failed twice: the classpath init script used the wrong Gradle receiver, and the probe used an incorrect getter name. Both were corrected only under `/tmp/account-tier2-audit-evidence`; classpath export, final compilation and all probes then succeeded. These setup failures are not product-test failures and are preserved in the evidence/JSON.

### Probe interpretation and boundaries

| Probe | Executed observation | Related control |
| --- | --- | --- |
| P01 | Empty/single/imbalanced/zero/negative/fractional-cent/overflow inputs rejected; balanced and lossless trailing-zero values accepted | Positive arithmetic controls |
| P02 | Maker directly approves own draft | GL01 |
| P03 | Posted date/line/collection mutation succeeds in memory | GL03; no successful DB deletion asserted |
| P04 | Two inverted reversal objects can be created from one original | GL04; service persistence not exercised |
| P05 | Incorrect transaction/base FX relationship passes approval/snapshot | GL08 |
| P06 | Untyped JSON loses a cent before precision validation | GL09; default mapper, not actual MVC |
| P07 | Unknown CLOSING_IN_PROGRESS status returns open | CL02; this is not a current master enum state |
| P08 | Out-of-order bulk dates yield wrong carry-forward | GL15; the batch reader itself orders dates correctly |
| P09 | Backdated posting leaves a later balance stale | GL12 |
| P10 | Null and literal NULL counterparties merge | GL16 |
| P11 | 5000 same-day number allocations produce197 duplicates | GL11; random sample count varies |
| P12 | Real FX SQL excludes prior reporting-currency adjustment; service generates it again | CL15; synthetic H2 |
| P13 | Fixed-asset marker does not prevent valuation | CL16; full eligibility policy is absent |
| P14 | ECL service subtracts incompatible currency units | CL17; controlled balance/summary ports |
| P15 | Stage sum6/target100 accepted | CL19 |
| P16 | Old task/gate evidence permits reclose | CL08/CL09 |
| P17 | Arbitrary retained-earnings destination and stale annual draft accepted | CL05/CL06 |

No probe exercised actual PostgreSQL races, authenticated network requests, remote commit/response-loss scenarios, or production data. Those findings are supported by explicit source interleavings and remain acceptance-test obligations. No statement that a deployed endpoint was exploited or that a live accounting balance was inspected is made.

## Architecture and control coverage

### Runtime boundaries and call paths

The important paths are:

```text
Journal HTTP / event / Kafka
  -> JournalUseCase / JournalEntryService
  -> JournalValidationEngine: balance + period + dated account
  -> JournalPersistencePort -> JPA

approve
  -> JournalEntry.approve
post
  -> PostingService
  -> GeneralLedger.fromApproved (immutable financial snapshot)
  -> period status read
  -> mark/save POSTED
  -> LedgerEntryPersistencePort (JPA or JDBC)
  -> LedgerService -> GL/SL balance persistence

Closing HTTP
  -> ClosingService: calendar/tasks/gates/locks/reopen
  -> local persistence + FiscalPeriodControlPort (local or HTTP)

Annual close
  -> JournalQueryPort -> full-year posted detail/category aggregation
  -> JournalPostingPort -> DRAFT transfer

FX batch
  -> account-range partition/cursor over posted journal rows
  -> FxValuationPipeline -> FxValuationService
  -> ClosingJournalEntryPort -> draft, optional approve/post

ECL batch
  -> tasklet -> EclProvisionService
  -> finalized summary + allowance balance ports
  -> ClosingJournalEntryPort -> draft, optional approve/post
```

Controllers generally use DTOs and inbound use-case contracts. Calculation belongs in core, and the batch configurations orchestrate core services. Posting maps one immutable GeneralLedger snapshot into both GL and SL adapters, reducing divergent line construction. However, **30 of 42 files in the two core domain trees import Spring or JPA**. Domain JPA entities, callbacks, converters and Spring Data repositories violate the requested strict domain boundary (GL19, CL14).

Cross-service ports do not make distributed writes transactional. In remote modes, a local Spring transaction cannot undo an already committed HTTP journal or master-period operation. Local and remote implementations require separate consistency/retry evidence; their behavior must not be inferred from the same annotations.

### Financial invariants and decimal handling

| Control | Observed behavior | Assessment |
| --- | --- | --- |
| Minimum lines / sides | At least two lines; at least one debit and credit | Enforced in aggregate and posting snapshot path |
| Exact debit == credit | BigDecimal compareTo for transaction and base totals independently | Enforced; no tolerance bypass found |
| Empty/single/zero/negative lines | Aggregate checks and positive setters reject them | Positive controls reproduced |
| Decimal precision | Amount NUMERIC(19,2), rate NUMERIC(19,8), UNNECESSARY normalization | Prevents silent ledger-boundary rounding; lossless trailing zero normalization allowed |
| Aggregate totals | Arbitrary-precision sum; no artificial per-line column bound applied to total | Appropriate; accumulated stored balance can still exceed column limit and fail |
| FX/ECL rounding | Explicit HALF_UP to 2 decimals at final calculation/group boundary | Defined calculation rounding; should not be replaced indiscriminately with blanket rounding |
| Event numeric ingestion | Untyped decimal input can become Double before calculation | GL09 |
| Transaction/base relationship | Independently balanced numbers need not reflect stated FX rate | GL08 |
| Normal balances | Ending = beginning + debit − credit; credit balances remain negative | Valid signed-ledger convention, not a normal-sign defect |
| Currency reporting | GL/SL projection stores base amount while retaining transaction-currency key | Consumers must know units; ECL violates this contract (CL17) |

The search found no deliberate `doubleValue()`/`floatValue()` monetary arithmetic in the inspected core calculation paths. That does not clear numeric precision: Jackson's implicit Double at the event boundary is the demonstrated problem. Rule-expression division explicitly rounds to 12 decimals HALF_UP; resulting posted lines still face exact ledger scale validation. Uniform two-decimal storage is the implemented contract, not a demonstrated currency-specific minor-unit policy; three-decimal currencies and rounding-residual policy require a defined product contract.

For FX, missing/nonpositive rates and missing accounts fail. Signed credit positions are handled without taking an absolute balance that would erase liability direction. The calculation nevertheless has two material gaps: earlier revaluation entries are omitted from carrying value, and eligibility does not distinguish monetary from historical-cost/non-P&L items (CL15/CL16).

No integrated realized-FX settlement workflow was found in the audited modules. A standalone FxPosition helper has no discovered production callers; the FX dashboard is explicitly a deterministic scaffold. These are capability/coverage limits, not claims that a functioning realized-FX engine was tested and failed.

### Posting lifecycle, immutability and SoD

The actual enum is **DRAFT, REQUESTED, APPROVED, REJECTED, POSTED, REVERSED**. There is no SUBMITTED enum. Approval accepts DRAFT or REQUESTED directly; no complete submitted/requested command workflow was found. Sequential posting requires APPROVED and rejects an already-posted journal.

An approved journal is revalidated before snapshot creation, including detail IDs and both balance equalities. Posting checks the accounting date, not the slip date, against the period port before changing state. Missing period/lookup failure/already-CLOSED state aborts that path before writes. These are valuable controls.

They are not sufficient for concurrent or authorized posting: maker-checker/role checks are missing, header/line setters remain open, the same journal can be posted by overlapping requests, and account balance updates can overwrite each other (GL01–GL07). A transaction annotation provides rollback for one local transaction; it does not serialize competing read-modify-write operations.

Reversal creates a genuine separate journal with inverted sides and copied amounts/dimensions, requested accounting date, current slip date and original-journal lineage. The service validates the new journal/period and persists it as a draft. It does not atomically claim reversal ownership or prevent multiple effective reversals (GL04). Do not fix this by blindly filtering the original out of POSTED source queries: accounting cancellation normally needs the original and its counter-entry to remain reconstructible.

The public HTTP surface inspected does not expose arbitrary journal update/delete commands. Nevertheless, internal use cases and repositories carry mutable JPA objects. P03 proves object mutation, and mappings show dirty-update exposure. Existing GL/SL foreign keys can block deletion of referenced detail rows; the audit does not claim every cascade/orphan delete succeeds.

Gateway sanitizes and reconstructs trusted identity headers. The findings are missing downstream authorization and body-based attribution overrides, not an invented Gateway header-spoofing bypass. Closing EOD has trusted identity/role enforcement; the general closing endpoints do not provide the same protection.

### Period cutoff, subledger coordination and annual closing

State authority is split:

| Owner | Actual state model / protection | Limit |
| --- | --- | --- |
| Master fiscal period | OPEN / CLOSED / PERMANENTLY_CLOSED; status update obtains a pessimistic row lock | No shared posting admission fence |
| ClosingCalendar | OPEN -> IN_PROGRESS -> CLOSED; approved reopen returns OPEN | No distinct durable REOPENED epoch; monthly aggregate lacks version/locking |
| PeriodLock | Local lock type and audit records | Not consulted by journal cutoff |
| ReopenApproval | PENDING -> APPROVED or REJECTED; different supplied actor strings | Caller identity binding and concurrent decision control missing |
| DailyClosingStatus | Per-business-date history, version and pessimistic lock; explicit next-day BOD | Not integrated into Journal transaction admission; documented scope limitation |

Mandatory task and gate presence is enforced, mandatory tasks cannot simply be skipped, and configured JSON conditions fail closed because typed evaluators do not exist. There is no required policy inventory or evidence integration for AP, AR, lease, loan, outstanding journal adjustments, confirmed ECL posting and annual transfer before close. Condition-free manual flags can complete the workflow (CL08). Reopen retains those prior flags (CL09).

Already-closed period posting fails, but three separate hazards remain: local lock/IN_PROGRESS omission (CL02), the status-read-to-post-commit race (GL07), and remote fiscal changes committing ahead of durable local approval/audit state (CL03). These require different acceptance tests and should not be collapsed into one boolean change.

Annual closing correctly uses posted base amounts, reverses revenue/expense directions, sorts account lines and offsets profit/loss into the requested destination. It creates a draft rather than silently posting. It does not validate an equity destination, refresh stale drafts, fail closed on missing classification, or provide a bounded aggregate query (CL05/CL06/CL12/CL13). Final fiscal close does not establish that the annual transfer has actually been posted.

### IFRS 9 ECL boundary

The real ECL batch consumes summaries, rather than recalculating PD/LGD/EAD or applying a fixed portfolio percentage. It rejects empty input, mixed run/model IDs, mixed legal entities, wrong dates and conflicting account mappings. It groups target values before subtracting existing allowance once, and rounds at the group posting boundary.

The upstream summary writer selects COMPLETED results. This audit therefore does **not** label the whole ECL source as unfinalized. Missing controls are stage-total reconciliation, durable run/model lineage and currency-unit compatibility (CL17/CL19/CL20). General ClosingService's API provision run is a separate fixed-amount path and does not use this validated batch calculation (CL07). A completed draft-generation run also does not prove provision journals have been approved/posted before fiscal closure.

### Batch processing and scale

There is **no current class named ClosingBatchConfig**. The actual Closing jobs are FxValuationBatchConfig and EclProvisionBatchConfig.

| Job / path | Actual mechanism | Existing evidence | Missing gate |
| --- | --- | --- | --- |
| Balance reaggregation | Cleanup step, then JpaPagingItemReader/chunk 100; ordered date/journal/detail IDs | Existing small job test; parameter tests | Atomic publication, stable source/restart interval, overlap/close/post exclusion (GL13/GL14) |
| Direct core reaggregation | Delete then load all posted details in one transaction | Core service tests | Chronological ordering and bounded caller contract (GL15) |
| Default JPA cleanup | Materializes all target balances before delete | Static adapter inspection | Predicate deletion/load behavior (GL17) |
| JDBC balances | Batch writes/upsert; nullable SL key handled in schema | H2 adapter tests | Atomic increments, PostgreSQL conflict/concurrency tests (GL06) |
| FX | Account-range partitions, cursor saveState, step-scoped ItemStream; configurable chunk default 1000/grid 1 | Real scoped-reader lifecycle test with mocked source/pipeline | Correct source model, stable restart snapshot, resource sizing and PostgreSQL plans (CL15/CL21) |
| ECL | One tasklet over grouped summaries | Core grouping/negative tests and job tests | Large-group transaction duration, failure/restart and remote partial-effect recovery |
| Annual closing | Full-year collections and per-journal/per-line queries | Small unit fixtures | Bounded aggregation, remote round trips and load evidence (CL12) |

FX pipeline failures propagate so the local chunk transaction can roll back. Remote journal commits cannot be rolled back by that transaction; HTTP auto-post recovery is specifically incomplete (CL18). Auto-post defaults false, which limits immediate ledger effects of FX/ECL draft generation.

No explicit retry policy or transaction timeout was found on the inspected job steps. Absence of a custom timeout is not itself proof of corruption. ECL grouped source cardinality can be much smaller than raw loans, but it is still unbounded by a checkpointed chunk contract. Real timeout, heap, lock-wait and restart evidence remains required for stated large-volume expectations. The FX source explicitly defers 100M-row readiness and a maintained dual-currency read model; this audit does not invent throughput guarantees.

## Database mapping and verification limits

### Journal-owned mapping

| Tables | Relevant mapping/constraints | Important limits |
| --- | --- | --- |
| journal_entries | Identity PK, unique slip_no, status check, accounting-date/status and lineage indexes; rate NUMERIC(19,8) | No version, unique client operation ID, or historical financial update guard |
| journal_details | Header FK, required account/side, NUMERIC(19,2) amount/base, side check and lookup indexes | No cross-line DB balance constraint or posted-state immutability guard |
| gl_entries / sl_entries | Transaction/base debit-credit columns and journal-detail FKs | Detail references nullable/nonunique; duplicate posting not constrained |
| gl_balances | Account/currency/date/period unique key; beginning/debit/credit/ending NUMERIC(19,2) | Absolute-total writes without version/atomic movement serialization |
| sl_balances | Dimensions include nullable partner/department; V11 uses UNIQUE NULLS NOT DISTINCT | Null-safe uniqueness is present; application string-key collision is separate |
| journal_rules / conditions / details | Rule/child relations, side/operator checks, validity/indexes | Event completeness/idempotency not supplied by these constraints |
| unsettled_items / settlement references | Management-number uniqueness, original detail FK, monetary fields and full reference history | Sequential reference protection; no item-level concurrency guard |

V1 is a placeholder baseline; V10 adds settlement audit fields, V11 supplies forward schema completion, and V12 stores all settlement references/backfills the last reference. The `JournalLedgerPostgresqlSchemaContextTest` name does not mean PostgreSQL was used: its test database is H2 in PostgreSQL compatibility mode. Eleven owned tables are covered by the current mapping test. JDBC adapter tests use hand-built H2 tables and mocked repository reads.

### Closing-owned mapping

V49 creates ten tables: closing_calendars, closing_tasks, closing_gates, closing_audit_logs, period_locks, reopen_approvals, valuation_batches, provision_batches, closing_adjustments and daily_closing_status. V50 upgrades daily EOD/BOD state and versioning; V51 converges operational indexes.

Calendar/task/gate/audit relations are local. Fiscal period and journal IDs represent external ownership and cannot themselves guarantee cross-service transactional integrity. Period-lock and reopen indexes are nonunique; no database guard enforces one active lock or one pending reopen request. Daily status has a date key and version; monthly approval/calendar entities do not inherit that concurrency protection.

The expected master fiscal period and account data are accessed through contracts/local or remote adapters. FX reads journal source tables; ECL reads allowance summaries; these are explicit cross-context read models with unit, snapshot and retention obligations. They are not co-owned tables that Closing can repair under a local transaction.

### Not established by this audit

- Real PostgreSQL clean/upgrade migration, NULLS NOT DISTINCT and ON CONFLICT behavior under the intended server version.
- Runtime database roles, ownership, DDL/DML grants, trigger coverage or operational deployment topology.
- Two-connection isolation/deadlock behavior for posting, balances, settlement, period close and reopen.
- Transactional recovery across HTTP commit/response-loss boundaries or Kafka broker acknowledgement/DLQ configuration.
- Query plans, index selectivity, cursor streaming, memory, timeout and 100M-row performance.
- Existing data reconciliation, business-entity completeness or historical production incident impact.

No operational database, credentials, account secrets, production URLs or live financial records were intentionally read. No real business batch, service deployment, migration, Git commit/push or GitHub issue publication was performed.

## Prioritized issue catalogue

Each draft contains exact source path/line references, problem, evidence/reproduction, impact, proposed remediation and testable acceptance criteria. **P1** means an affected financial/control path needs remediation before reliance. **P2** identifies bounded/conditional correctness, auditability, scale or architecture work. Severity expresses consequence and control importance, not proof of a deployed incident.

The required `status:ready` labels mean ready for issue triage/planning. They do not mean a fix is implemented, tested or ready to merge. All entries are local drafts; there are no assigned GitHub issue numbers.

| Issue | Priority | Ownership | Summary | Executed probe |
| --- | --- | --- | --- | --- |
| [GL01](issues/GL01.md) | P1 | journal-ledger / core | Approval lacks permission and maker-checker enforcement | P02 |
| [GL02](issues/GL02.md) | P1 | journal-ledger / api | Request bodies can forge journal audit actors | Source analysis |
| [GL03](issues/GL03.md) | P1 | journal-ledger / core | Posted journal headers and lines remain mutable | P03 |
| [GL04](issues/GL04.md) | P1 | journal-ledger / core | Reversal requests can repeatedly reverse the same posted journal | P04 |
| [GL05](issues/GL05.md) | P1 | journal-ledger / core | Concurrent requests can post one approved journal twice | Source analysis |
| [GL06](issues/GL06.md) | P1 | journal-ledger / core | Concurrent postings overwrite account balance movements | Source analysis |
| [GL07](issues/GL07.md) | P1 | journal-ledger / core | Period status check and posting commit are not atomic | Source analysis |
| [GL08](issues/GL08.md) | P1 | journal-ledger / core | Foreign journal creation does not enforce transaction-to-base conversion | P05 |
| [GL09](issues/GL09.md) | P1 | journal-ledger / api | Untyped event JSON loses cents before BigDecimal validation | P06 |
| [GL10](issues/GL10.md) | P1 | journal-ledger / core | Journal creation lacks an atomic idempotency contract | Source analysis |
| [GL11](issues/GL11.md) | P2 | journal-ledger / core | Slip numbers have only 65536 random values per day | P11 |
| [GL12](issues/GL12.md) | P1 | journal-ledger / core | Backdated posting leaves later GL and SL balances stale | P09 |
| [GL13](issues/GL13.md) | P1 | journal-ledger / batch | Reaggregation publishes deleted or partial balances and races with posting | Source analysis |
| [GL14](issues/GL14.md) | P1 | journal-ledger / batch | Reaggregation restart dates drift with the wall clock | Source analysis |
| [GL15](issues/GL15.md) | P2 | journal-ledger / core | Core reaggregation trusts an unspecified source order | P08 |
| [GL16](issues/GL16.md) | P2 | journal-ledger / core | String aggregation keys merge distinct subledger dimensions | P10 |
| [GL17](issues/GL17.md) | P2 | journal-ledger / batch | JPA rebuild cleanup materializes the complete balance range | Source analysis |
| [GL18](issues/GL18.md) | P1 | journal-ledger / api | Unmatched Kafka events complete without a journal or durable exception record | Source analysis |
| [GL19](issues/GL19.md) | P2 | journal-ledger / core | Domain packages depend on JPA and Spring Data | Source analysis |
| [GL20](issues/GL20.md) | P1 | journal-ledger / core | Concurrent unsettled settlements can lose monetary updates | Source analysis |
| [GL21](issues/GL21.md) | P2 | journal-ledger / api | Kafka silently substitutes processing date for missing accounting date | Source analysis |
| [CL01](issues/CL01.md) | P1 | closing / api | Caller-controlled actors bypass closing authorization and reopen separation of duties | Source analysis |
| [CL02](issues/CL02.md) | P1 | closing / core | Active period locks and closing-in-progress do not block journal admission | P07 |
| [CL03](issues/CL03.md) | P1 | closing / core | Remote fiscal transitions can commit before local closing approval | Source analysis |
| [CL04](issues/CL04.md) | P1 | closing / core | Concurrent reopen decisions can contradict the final period state | Source analysis |
| [CL05](issues/CL05.md) | P1 | closing / core | Annual close reuses stale drafts after source activity changes | P17 |
| [CL06](issues/CL06.md) | P1 | closing / core | Annual close accepts non-equity retained-earnings accounts | P17 |
| [CL07](issues/CL07.md) | P1 | closing / core | Valuation and provision run APIs use configured constants instead of FX or ECL results | Source analysis |
| [CL08](issues/CL08.md) | P1 | closing / core | Final close does not require subledger and adjustment completeness evidence | P16 |
| [CL09](issues/CL09.md) | P2 | closing / core | Reclose reuses tasks and gates from the previous close cycle | P16 |
| [CL10](issues/CL10.md) | P2 | closing / api | Repeated run API calls create duplicate adjustment drafts | Source analysis |
| [CL11](issues/CL11.md) | P2 | closing / core | Concurrent lock and reopen-request creation bypasses uniqueness checks | Source analysis |
| [CL12](issues/CL12.md) | P2 | closing / core | Annual close loads full-year detail history with N+1 lookups | Source analysis |
| [CL13](issues/CL13.md) | P2 | closing / core | Annual close silently omits unclassified posted accounts | Source analysis |
| [CL14](issues/CL14.md) | P2 | closing / core | Closing domain models contain persistence framework dependencies | Source analysis |
| [CL15](issues/CL15.md) | P1 | closing / batch | FX valuation excludes earlier posted revaluation adjustments | P12 |
| [CL16](issues/CL16.md) | P1 | closing / core | FX valuation includes ineligible revenue and nonmonetary accounts | P13 |
| [CL17](issues/CL17.md) | P1 | closing / core | ECL provision mixes foreign targets with functional-currency balances | P14 |
| [CL18](issues/CL18.md) | P2 | closing / core | HTTP auto-post cannot resume an already approved adjustment | Source analysis |
| [CL19](issues/CL19.md) | P2 | closing / core | ECL stage totals are not reconciled with the posting target | P15 |
| [CL20](issues/CL20.md) | P2 | closing / core | ECL adjustment lineage drops confirmed run and model identity | Source analysis |
| [CL21](issues/CL21.md) | P2 | closing / batch | FX parallel cursors exceed the fixed single-connection source pool | Source analysis |

[issue-manifest.json](issue-manifest.json) is the machine-readable catalogue.

## Remediation sequencing and acceptance gates

1. **Identity, immutability and exactly-once posting:** GL01–GL07/GL10 and CL01–CL04. Establish authenticated actors, enforced permissions, immutable historical facts, durable operation identity, atomic journal posting and period admission. Balance and state races require real database barriers, not additional unit mocks.
2. **Correct financial measurement:** GL08/GL09/GL12 and CL05–CL08/CL15–CL17. Freeze source snapshots, establish unit contracts, reconcile FX/ECL/annual calculations and require completion evidence before period close.
3. **Recoverable recomputation and integration:** GL13/GL14/GL18/GL20 plus CL09–CL11/CL18–CL20. Prove failure/restart/response-loss behavior and old/new evidence separation.
4. **Scale, key safety and boundaries:** Remaining P2s. Use typed dimensions, adequate identifier space, provider aggregation, bounded cleanup/pools and incremental infrastructure/domain separation.

Acceptance should include both precise negative tests and source-to-journal-to-ledger reconciliation. For races, use independent database transactions with controlled barriers. For batches, exceed one chunk, fail at several points and restart the same JobInstance. For remote operations, inject a response loss after the remote commit. For currency tests, use deliberately unequal transaction/base units and two successive posted revaluations.

Audit artifacts can be removed independently if no longer wanted; no financial data or production-code rollback is required. Remediation/data-repair work is a separate scoped task and should retain this commit/evidence baseline.

## Review, quality contract and handoff

The parent conducted Journal/domain checks, ran tests/probes and wrote all artifacts/records. Separate read-only reviewers inspected Closing controls, FX/ECL, persistence/consistency and API/batch/architecture. Their findings were reconciled to avoid duplicate issues and false claims about normal signed balances, null-safe SL uniqueness, Gateway headers, PostgreSQL execution and successful posted-line deletion.

The audit package itself is subject to a final independent read-only consistency review. Its result is recorded in [review.md](review.md); it assesses this evidence package, not remediation of the 42 findings.

| Q item | Assessment | Evidence / applicability | Residual gate |
| --- | --- | --- | --- |
| Q1: clear responsibilities | PASS for audit artifacts | Named local probes, structured issue ownership, explicit source/probe distinction; no production/test-code change | Remediation designs still need code review |
| Q2: flow and rationale | PASS | Call paths, reproduction inputs, numerical consequences, failure/restart scope above and in each issue | Live concurrency/distributed execution unverified |
| Q3: documentation and beginner guidance | PASS | Evidence README gives prerequisites, exact commands, expected outputs and limits; report explains signed balances and units | Existing module docs not rewritten because behavior was not changed; drift is recorded below |
| Q4: local intent comments | PASS for diagnostic source | AuditProbes explains observation semantics and stateful-port limitations near code; production comments preserved | Production comment changes N/A: no production edit |

**Documentation drift observed:** Journal docs/process-flow.md still describes a tasklet-only reaggregation flow while the actual job now uses cleanup plus chunks; batch comments overstate unconditional idempotency/timeout safety. Enum/docs describe posted immutability and a complete REQUESTED/REVERSED lifecycle more strongly than implementation supports. Closing's local-transaction descriptions must not imply rollback of already-committed remote writes. These inaccuracies are recorded here rather than corrected during this read-only product audit.

The next owner is the user's engineering/financial-control triage team. Prioritize the local issue drafts, refine policy-dependent acceptance criteria and assign isolated remediation work. No production fix, commit, push, PR, Ready transition, merge, issue closure or resource deletion is included in this handoff.

