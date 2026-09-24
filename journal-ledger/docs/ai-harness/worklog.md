# 2026-09-24 — GH-767 fail-closed balance reaggregation

## Intake, isolation and ownership

- Confirmed actual GitHub Issue [#767](https://github.com/skyg547/account/issues/767) is OPEN. The body retains the audit draft's historical “No GitHub issue has been created” wording.
- Preserved the dirty primary checkout. Fetched `origin/main`, created `agent/767-reaggregation-consistency` and `/tmp/account-767-reaggregation-consistency` at `795c494a950980864c0e9bc63464dcb505a8d46b`; final fetch showed the same remote base.
- [Execution contract comment](https://github.com/skyg547/account/issues/767#issuecomment-5815677299) records branch, isolated worktree, module allowlist, requested model and verification. Remote status is `status:in-progress`, owner `agent:codex` before PR handoff.
- Applied `account-issue-loop`, `account-hexagonal-change`, the single-module ownership form of `account-module-parallel`, and `account-review-handoff`. `/root/implement_767` owned module implementation/tests/docs; `/root/explore_767` and `/root/review_767` were read-only. Parent alone owns these records and Git/GitHub.
- Scope is `journal-ledger/**`, including these module-local records. Other modules, shared contracts and repository-level shared harness files are frozen. No production/development credentials or database were accessed.

## Audited defect and RED evidence

- The audited schedule was confirmed: cleanup commits deletion before independent 100-detail chunks; failures expose empty/partial balances, a live POSTED reader can move under posting, and overlapping instances can delete or double-apply output. Existing V14 stripes cover each transaction, not the whole Job.
- Migration RED: `./gradlew :journal-ledger:core:test --tests '*BalanceReaggregationControlMigrationTest' --rerun-tasks --console=plain --max-workers=1` exited1; 1 test/1 expected failure because audited production had no V15 migration.
- Financial behavioral RED: `./gradlew :journal-ledger:batch:test --tests '*BalanceReaggregationBatchConfigTest.rejectsAcceptedEmptyBalancesAfterCommittedCleanup' --rerun-tasks --console=plain --max-workers=1` exited1; 1 test/1 expected failure. Cleanup deleted an accepted100.00 balance and audited `getGlBalances` returned the empty state instead of rejecting it.
- Review-found runtime RED: actual `JournalLedgerApplication` local boot initially returned HTTP500 for a GL balance GET because create-drop did not create non-JPA `ledger_reaggregation_control`. The focused `JournalLedgerApiLocalProfileTest.localMigratedSchemaSupportsFencedBalanceReads` failed1/1 before correction.

## Implementation and financial controls

- Append-only `V15__ledger_reaggregation_control.sql` creates one strongly constrained row: status, JobInstance owner, frozen start/end and monotonic epoch. Earlier migrations are unchanged.
- Core owns `BalanceReaggregationControlPort`, its JDBC adapter and `BalanceReaggregationService`; Batch owns only start/cleanup/chunk/final Step orchestration. The selected JPA or JDBC-bulk balance adapter remains behind `LedgerBalancePersistencePort`.
- Start acquires all256 V14 stripes before closing the barrier. Ordinary posting acquires affected stripes before asserting `OPEN`; a winner commits into the stable POSTED source, while a later request rolls back journal state, entries and balances. Owner-only cleanup/chunks may write while `REBUILDING`.
- JobParameters are normalized once into JobExecutionContext. Same JobInstance restart skips the completed cleanup and resumes its saved reader checkpoint; a different instance is rejected while an owner is active.
- Reads compare `OPEN+epoch` before and after materialization/aggregation and discard any result crossing a rebuild. Finalize locks all stripes and compares stable POSTED source with actual GL/SL by day, complete keys, nullable BP/department, debit/credit and carried beginning/ending balances. Mismatch rolls back without release.
- API local now uses the module Flyway chain plus Hibernate `validate`; dev/prod keep runtime Flyway disabled for the external migration-runner. Batch local already uses owned H2/Flyway/validate and now has an actual composition-root lifecycle regression.

## Verification and independent review

- Chunk restart coverage uses101 balanced journals/202 details, producing 100/100/2 chunks. Failure on writer invocation2 or3 leaves respectively50.00 or100.00 committed but unpublishable, then the same JobInstance resumes to exactly101.00. Cleanup is verified once.
- Coverage also includes cleanup failure, overlap rejection, both posting/barrier orderings, JPA and JDBC-bulk rollback/retry, epoch read race, range freeze, V15 constraints/idempotent reacquire, reconciliation failure, exact GL and nullable SL keys, and existing posting/precision controls.
- Writer final: `./gradlew :journal-ledger:test --console=plain --max-workers=1` exited0 in54s; core173/API40/batch11 =224 tests across41 suites, failures/errors/skips0.
- Independent reviewer final: `./gradlew :journal-ledger:test --rerun-tasks --console=plain --max-workers=1` exited0; all37 Gradle tasks executed, the same224 tests passed with failures/errors/skips0. Focused local/schema verification passed6/6.
- Actual API local boot applied seven journal migrations through V15; `GET /api/v1/ledger/gl/balances?...` returned HTTP200 body `[]`. It was intentionally stopped after the check. Batch local boot with jobs disabled applied V15, validated the schema and exited normally. No bootRun process remains.
- `node --test tools/ci/harness-quality-contract.test.cjs`:32/32 PASS. `git diff --check`, untracked whitespace, changed-file conflict markers and module allowlist checks PASS.
- Independent review initially reported the local API schema P2 above. The original writer switched local schema ownership to Flyway/validate and added actual-context coverage. Final `/root/review_767`: no P0-P3; Q1-Q4 PASS.

| Item | Result | File/test evidence | N/A reason | Risk / next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | core control port/service/JDBC adapter, four Batch Steps; module224 PASS | Not applicable: production behavior changed | Production cardinality/load remains later | `/root/review_767`, no P0-P3 |
| Q2 | PASS | `../posting-concurrency.md`; 202-detail restart, overlap, both posting orders and reconciliation failure tests | Not applicable: concurrency flow changed | Distributed process-kill/PostgreSQL gate remains | `/root/review_767` confirmed |
| Q3 | PASS | module README/docs cover V15, local/dev/prod schema owner, same-instance recovery, rollback and direct-SQL limit | Not applicable: runtime/schema contract changed | Deployment owner verifies grants and coordinated cutover | `/root/review_767` confirmed |
| Q4 | PASS | stripe-before-barrier, epoch double-read, checkpoint/generation and fail-closed release intent comments | Not applicable: nontrivial ordering changed | Keep old binaries stopped during cutover | `/root/review_767` confirmed |

## Rollback, limits and publication gate

- Rollback requires a reviewed application revert while all journal writers/readers are stopped. Preserve V14/V15 and the singleton; do not delete/repair migrations or manually force `OPEN`. Restore protected binaries before traffic resumes.
- No live PostgreSQL, production data, distributed kill, runtime-role grant or production load test was used. Privileged direct DB readers can see in-place partial rows while `REBUILDING`; direct SQL and older writers bypass the application barrier. Final all-stripe lock and compact daily-key projections require production lock-wait/cardinality validation.
- Parent is authorized to commit, push and open a Draft PR with `Refs #767`, then move the Issue to `status:needs-review`. Ready, merge, Issue close, deployment and branch/worktree deletion remain later human gates.

## Draft publication

- Parent committed the independently reviewed module change as `06e5ccc3` and pushed only `agent/767-reaggregation-consistency`.
- Opened [Draft PR #785](https://github.com/skyg547/account/pull/785) against `main` with `Refs #767`. The body records all RED/GREEN evidence, Q1-Q4, rollback/limits, and the writer/reviewer/Integrator authority separation.
- This follow-up adds only the actual PR link to the three module-local records. No production or test behavior changed after independent approval. Ready, merge, Issue close, deployment and resource deletion were not performed.

## Remote CI execution prerequisite

- On the published Draft PR head, Module Validation, Harness Validation and Agent Merge Guard failed before executing job steps; downstream module/discipline jobs were skipped.
- Every failure annotation states that recent account payments failed or the spending limit must be increased. This is an external GitHub Actions prerequisite, not an executed code/test failure; no remote CI PASS is claimed.
- Next owner: repository/account owner resolves GitHub Billing & plans and reruns checks. PR remains Draft and Issue remains open/needs-review. No billing settings, workflows or shared harness files were changed.

---

The following entries are retained history and are not current GH-767 evidence.

# GH-760 worklog

## 2026-09-24 — Intake and isolation

- Confirmed actual GitHub Issue #760 is OPEN, with no earlier comments or linked open PR. The body still contains the audit draft's historical “No GitHub issue has been created” wording.
- Preserved dirty primary checkout. Fetched `origin/main`, created `agent/760-single-posting` and `/tmp/account-760-single-posting` from audited SHA `a97d10ab6efc2570a88f83d630242cd748f8be57`.
- [Claim](https://github.com/skyg547/account/issues/760#issuecomment-5814474458) records scope, acceptance, verification, rollback, and ownership. Remote status is `status:in-progress`, owner `agent:codex`.
- Applied `account-issue-loop`, `account-hexagonal-change`, and the single-module ownership variant of `account-module-parallel`. Shared harness editing is forbidden by the user's narrower instruction; records stay under `journal-ledger/docs/ai-harness/`.
- Scope: serialize requests for one approved journal, enforce durable unique GL/SL identity, preserve financial controls, verify both persistence paths and rollback/retry. Other-journal balance contention, concurrent period closure, historical data repair, and deployment are outside this issue.
- Found `./gradlew :journal-ledger:test --offline --console=plain` returned success with `NO-SOURCE` and no tests. Added module-local aggregation of core/API/batch test tasks so the requested command performs verification.
- Started a task-owned disposable PostgreSQL 16 container from the existing local image, bound only to loopback. No existing database/container configuration or credentials were accessed.

## Verification plan

1. Target new concurrency, rollback, retry, uniqueness and migration tests; JPA/JDBC each use independent transaction connections and existing balances.
2. Demonstrate regression failure against the unchanged audited baseline in an isolated worktree.
3. Execute `./gradlew :journal-ledger:test` and relevant packaging/schema checks; record exact counts and skipped cases.
4. Independent read-only review, Q1–Q4 evidence, scoped diff/marker check.
5. Parent commits/pushes, opens a Draft PR using `Refs #760`, and records verification and authority separation.

## Audited baseline evidence

- Separate proof branch/worktree: `agent/760-regression-proof`, `/tmp/account-760-regression-proof`, unchanged audited production source at `a97d10ab6efc2570a88f83d630242cd748f8be57`.
- `./gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --offline --console=plain --max-workers=1`: PASS, 1m 10s, 37 tasks executed. XML totals: core117/API34/batch5 = 156 tests in 32 suites; failures/errors/skips 0. This establishes existing financial tests pass before remediation, not that concurrency is protected.
- Available local tools: Gradle 8.7, existing JDK17 toolchain, cached Flyway9.22.3/PostgreSQL JDBC42.6.2 dependencies, cached PostgreSQL16 container image. No installation/download was needed.
- PostgreSQL regression RED: copied only `PostingConcurrencyIntegrationTest.java`, `PostingTestDatabase.java`, and two core test dependencies into the proof worktree; original production source and migrations remained unchanged (`git diff --exit-code -- journal-ledger/core/src/main` passed). Ran `JOURNAL_POSTING_TEST_POSTGRES_URL=<task-owned loopback JDBC URL> ./gradlew :journal-ledger:core:test --tests '*PostingConcurrencyIntegrationTest.overlappingRequestsWithPreviouslyLoadedApprovedStatePostExactlyOnce' --offline --console=plain --max-workers=1`: expected exit1, 2 tests/2 failures, 28s. Both JPA and JDBC report `[gl_entries entry count] expected: 2 but was: 4`. This executed reproduction replaces the audit's static-only concurrency evidence.
- Initial fixed H2 targeted run: `PostingServiceTest`, `PostingConcurrencyIntegrationTest`, `PostingIdentityMigrationTest`, 29 tests (15+9+5), failures/errors/skips0, 29s. PostgreSQL GREEN and full aggregate verification follow.

## Remediated PostgreSQL evidence

- `JOURNAL_POSTING_TEST_POSTGRES_URL=<task-owned loopback JDBC URL> ./gradlew :journal-ledger:core:test --tests '*PostingConcurrencyIntegrationTest' --tests '*PostingIdentityMigrationTest' --offline --console=plain --max-workers=1`: PASS, 40s, 14 tests (9 concurrency/transaction + 5 migration), failures/errors/skips0. XML preserved externally at `/tmp/account-760-evidence-postgres/`; execution log `/tmp/account-760-targeted-postgres.log`.
- Each mode uses distinct `pg_backend_pid()` values and a preloaded APPROVED entity in each persistence context. The held first request owns the header while `pg_stat_activity.wait_event_type = 'Lock'` confirms the second connection waits. Exactly one request succeeds, one receives the existing domain conflict, GL/SL each contain two unique detail rows, and both ledger/balance sides total100.00 with the original winner actor retained.
- Claim/status flush followed by injected failure before entry insertion rolls back to APPROVED/approver with zero entries and unchanged pre-existing balances; retry succeeds in each mode. Same-transaction approval/posting and read-only detail access pass.
- V12→V13 preserves valid financial history and enforces GL and SL identity independently. Four duplicate/NULL legacy cases fail migration while retaining rows and amounts. Production data, distributed crash injection, load, cross-journal balance races, and concurrent closing were not tested.
- Independent reviewer caught a stale one-query Javadoc; writer is correcting the affected explanation. No production defect has been reported at this checkpoint; final Q1–Q4 and full suite evidence remain pending.

## Full module verification

- Exact requested command `./gradlew :journal-ledger:test`: PASS, 1m12s, 37 tasks (26 executed/11 up-to-date). Core131/API34/batch5 = 170 tests across34 suites, failures/errors/skips0. The parent aggregate task remains a no-source Java test task but now depends on all three real suites; its zero-source task is not counted as test evidence. New concurrency/migration tests use H2 in this run; the separate14-test PostgreSQL run is recorded above.
- Writer behavior frozen. Reviewer confirmed the stale Javadoc correction and found no remaining P0–P3 source/test/functional-document findings. Independent direct-consumer test and API/Batch packaging remain the final verification step.
- Scoped static checks: all23 changed files are under `journal-ledger/`; diff whitespace and exact changed-file conflict-marker gates PASS. No shared harness or other-module source changed. Primary checkout's pre-existing dirty files are preserved.

## Independent review and handoff

- `/root/posting_review` directly executed `./gradlew :loan:core:test --tests '*LoanJournalPostingFlowTest' :journal-ledger:api:bootJar :journal-ledger:batch:bootJar --offline --console=plain --max-workers=1`: PASS, 24s, 26 tasks (10 executed/16 up-to-date), Loan1/1 with failures/errors/skips0. No Loan source was changed. Both bootJARs contain `db/journal-migration/V13__unique_journal_posting.sql` in the nested core JAR, byte-identical to source. Packaging is not deployment or a separately launched business server.
- Reviewer independently checked actual170-test aggregate XML, saved14-test PostgreSQL16.13 XML, audited2-test expected failures, source/test/docs diff and scoped static gates. Initial stale Javadoc P3 corrected by original writer; final substantive review has no open P0–P3.
- [Review handoff](https://github.com/skyg547/account/issues/760#issuecomment-5814607342) moves Issue to `status:needs-review`; parent publishes Draft PR with `Refs #760`. Final Ready/merge/Issue close belongs to later human review/approval.
- Task-owned PostgreSQL container is stopped after verification and retained for follow-up. Both issue/proof branches and worktrees are retained. No conflicts occurred; no conflict-log change was needed or allowed outside the module.

| Item | Result | File/test evidence | N/A reason | Risk / next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `JournalPersistenceAdapter.java:107`; `PostingConcurrencyIntegrationTest.java:135`; module170/PG14/Loan1 PASS | Not applicable: posting behavior changed | Production load remains a later gate | `/root/posting_review`, no P0–P3 |
| Q2 | PASS | `../posting-concurrency.md:9`; lock/refresh/commit/failure/retry flow checked against implementation | Not applicable: transaction flow changed | Cross-journal balance and closing races outside scope | `/root/posting_review` confirmed |
| Q3 | PASS | `../posting-concurrency.md:31`, `:57`; README/process-flow/schema; command/XML/bootJAR evidence | Not applicable: behavior/schema changed | Deployment owner must check actual legacy history and DDL window | `/root/posting_review` confirmed |
| Q4 | PASS | adapter lines111/118; repository37; V13 line2; concurrency test coordination/rollback comments | Not applicable: nontrivial concurrency logic changed | None: no remaining comment mismatch | `/root/posting_review` confirmed |

## Draft publication

- Independent reviewer approved the final three module-local records and PR description; no factual mismatch or unresolved finding. Parent committed the reviewed23-file change as `65a799f4` and pushed only `agent/760-single-posting`.
- Opened [Draft PR #782](https://github.com/skyg547/account/pull/782) against `main`, using `Refs #760`. The body includes regression/full-suite/packaging evidence, Q1–Q4, user-directed module-local harness exception, rollback limits and explicit authority separation.
- This publication follow-up changes only these three record files to add the actual PR link. No implementation or test changed after independent approval. Remote CI is a subsequent check and is not represented by local test results. Ready, merge, Issue close and resource deletion were not performed.

## Remote CI execution prerequisite

- On published head `ee1f625f`, [Module Validation](https://github.com/skyg547/account/actions/runs/36003024467), [Harness Validation](https://github.com/skyg547/account/actions/runs/36003024509), and [Agent Merge Guard](https://github.com/skyg547/account/actions/runs/36003024616) failed before executing any job steps. Failed-job logs were unavailable because jobs never started.
- Read-only check annotations for jobs107644055366/107644055404/107644055794 all report that recent account payments failed or the spending limit must be increased. This is an external GitHub Actions prerequisite, not an executed code/test failure; no remote CI PASS is claimed.
- Next owner: repository/account owner resolves GitHub Billing & plans and reruns checks. Parent leaves PR Draft and Issue OPEN/needs-review. This record-only follow-up and PR body disclose the remaining gate; no billing settings, workflow/shared harness files, implementation or tests were changed.


# 2026-09-24 — GH-761 concurrent balance movements

- Issue: [#761](https://github.com/skyg547/account/issues/761); [Codex claim](https://github.com/skyg547/account/issues/761#issuecomment-5814751685). Model assignment: gpt-6-astra, xhigh. Base: `origin/main@0c0fcd3a7338161e7987c644f9b2c1f31ce29470`; branch `agent/761-concurrent-balances`; worktree `/tmp/account-761-concurrent-balances`. Primary checkout was dirty and its existing changes were preserved.
- Scope: only `journal-ledger/**`. User explicitly froze other modules, shared contracts and shared harness documents. These three module-local records are maintained by the parent Integrator; prior GH-760 history is retained. No production/development database or credential files were accessed.
- Ownership: `/root/balance_implementation` owns production and functional docs, `/root/balance_tests` owns the new concurrency fixture, parent owns existing test adaptations, V14 upgrade test and records/Git/GitHub. `/root/balance_review` reviews independently without editing.
- Change: V14 seeds 256 transaction lock rows; both persistence modes acquire the complete account/currency stripe set in sorted order before balance reads. This covers existing/missing GL and all nullable SL dimensions across dates. Cached balances refresh after waiting; JDBC detaches them before bulk writes. Unsupported isolation and missing lock rows fail closed. Core rebuild locks all stripes; existing Batch cleanup/chunk commits still require posting quiescence throughout the Job.
- Controls preserved: approved immutable snapshot, current period validation, duplicate-entry constraints, BigDecimal precision, actor/state rollback and carry-forward. Within-bulk dates are ordered and typed write keys distinguish SQL NULL from literal `NULL`. Constructors remain compatible. Deadlock retry belongs to the caller after rollback of the entire ambient transaction; there is no retry inside an aborted transaction.

## Audited PostgreSQL regression evidence

- Created `/tmp/account-761-regression-proof`, branch `agent/761-regression-proof`, at exact audited commit `a97d10ab6efc2570a88f83d630242cd748f8be57`. Production Java/SQL remained unchanged. Only copied test fixture and core test build support (PostgreSQL runtime dependency plus owned migration resource location) were added.
- Command: `JOURNAL_POSTING_TEST_POSTGRES_URL=<task-owned loopback JDBC URL> ./gradlew :journal-ledger:core:test --tests '*LedgerBalanceConcurrencyIntegrationTest.distinctJournalsPreserveEveryDebitAndCredit' --offline --console=plain --max-workers=1`. PostgreSQL16.13, exit1, 58s; 16 tests, 16 expected failures, errors/skips0.
- JPA new-key cases4 committed only one of the two journals. JPA existing-key cases4 and all JDBC cases8 retained debit/credit100.00 instead of140.00 despite the committed journal/entry totals. This reproduces the financial defect rather than a setup/compile error.
- Evidence: `/tmp/account-761-baseline-postgres.log`, `/tmp/account-761-evidence-baseline-postgres/`. The baseline fixture pauses after the first real balance read, permits the second request to proceed or wait, and then reconciles actual persisted state.

## Initial verification checkpoints

- H2 narrow command: `./gradlew :journal-ledger:core:test --tests '*LedgerServiceTest' --tests '*LedgerBalanceLockMigrationTest' --tests '*PostingConcurrencyIntegrationTest' --tests '*JdbcLedgerBulkPersistenceAdapterTest' --offline --console=plain --max-workers=1`: exit0, 29s, 34 tests, failures/errors/skips0.
- Initial PostgreSQL command selects the same classes except `JdbcLedgerBulkPersistenceAdapterTest`: exit0, 31s, 33 tests (service23, V14 upgrade1, existing posting9), failures/errors/skips0. The extra service test was the newly added NULL/literal-key regression, so these overlapping counts are not additive. XML saved in `/tmp/account-761-evidence-initial-postgres/`.
- New26-case H2 fixture: `./gradlew :journal-ledger:core:test --tests '*LedgerBalanceConcurrencyIntegrationTest' --offline --no-daemon --max-workers=1 --console=plain`: exit0, 53s, failures/errors/skips0. Real H2 deadlock SQLSTATE40001 is executed. Evidence `/tmp/account-761-new-h2.log` and `/tmp/account-761-new-h2.xml`.
- PostgreSQL acceptance, final full-module and independent review evidence follow below when complete.

## Remediated PostgreSQL and full module verification

- New PostgreSQL command: `JOURNAL_POSTING_TEST_POSTGRES_URL=<task-owned loopback JDBC URL> ./gradlew :journal-ledger:core:test --tests '*LedgerBalanceConcurrencyIntegrationTest' --rerun-tasks --offline --no-daemon --max-workers=1 --console=plain`: exit0, 42s, 26 tests, failures/errors/skips0. XML/log: `/tmp/account-761-new-postgres.xml`, `/tmp/account-761-new-postgres.log`.
- All16 existing/new/null-dimension cases use distinct PostgreSQL backend IDs and assert an actual `pg_stat_activity` Lock wait before releasing the first writer. Journals, GL/SL entries and balance debit/credit totals reconcile. The remaining10 cases cover concurrent next-day carry-forward, real SQLSTATE40P01 after all posting writes, complete rollback and fresh-transaction retry, REPEATABLE_READ rejection, missing-stripe rollback, and three postings in one ambient transaction. Every case runs in both persistence modes.
- Exact requested `./gradlew :journal-ledger:test`: exit0, 1m22s, 37 tasks (26 executed/11 up-to-date), core164/API34/Batch5 = 203 tests across36 suites; failures/errors/skips0. Log `/tmp/account-761-full-module.log`; XML `/tmp/account-761-evidence-full-module/{core,api,batch}/`. The parent aggregate task is NO-SOURCE but depends on all three executed suites; its own empty task is not counted.
- Full-suite additions also verify lock-before-read/failure, whole-account rebuild locking, empty input, NULL/literal dimensions, chronological bulk dates and V13→V14 data preservation. Existing posting/precision/closing/rollback controls remain passing.
- Initial independent review found one P3 documentation overclaim about executing V14 constraint rejection tests. Original author narrowed the wording to exact seeded IDs and preserved data. No production or test correction was required. Independent consumer/packaging verification and final review follow.

## Rollback, limits and authority

All posting/reaggregation writers, including other services embedding journal core, must stop for coordinated V14/new-binary cutover. Mixed old/new writers are unsafe. Keep READ_COMMITTED and runtime SELECT/UPDATE privileges on the lock table;256 stripes may serialize unrelated accounts, so production contention/load remains a deployment gate.

A deadlock requires the caller to roll back the whole ambient transaction and retry in a new one. Core single-transaction rebuild is serialized; cleanup/chunk Batch still requires posting stopped for the entire Job and a single rebuild Job. Backdated changes still require approved forward-period reaggregation. No production workload, distributed/process crash injection, actual deployment or historical data repair was performed.

Rollback is a reviewed scoped code revert while all writers remain stopped. Retain V13/V14 and migration history; reverting to pre-lock binaries reopens the race, so do not resume traffic until protection is restored. Parent alone commits/pushes and opens the user-authorized Draft PR with `Refs #761`; Ready, merge, Issue close, deployment and branch/worktree deletion require later approval. No conflicts occurred.

## Independent review and handoff

- Independent `/root/balance_review` inspected the final source/test/functional diff, baseline production identity, actual saved PostgreSQL and full-module XML, and scoped marker/whitespace/index gates. After the original author corrected one P3 wording overclaim, no P0–P3 remained. Q1–Q4 PASS.
- Reviewer directly executed `./gradlew :loan:core:test --tests '*LoanJournalPostingFlowTest' :journal-ledger:api:bootJar :journal-ledger:batch:bootJar --offline --console=plain --max-workers=1`: exit0, 23s, 26 tasks (8 executed/18 up-to-date), Loan1/1 failure/error/skip0. This consumer fixture mocks LedgerService; it verifies compatibility/flow, not Loan DB concurrency. No Loan source changed.
- Both bootJARs contain source-identical V14 at `db/migration` and `db/journal-migration` in the nested core JAR. V14 SHA256: `187bebf05318fe97d8337f52dc24a45854c8e00cd99714ddc3350123ec5c4393`. This is packaging evidence, not deployed-service smoke.
- Parent retained external evidence and both issue/proof branches/worktrees. Task-only disposable PostgreSQL container `account-761-postgres` is stopped after verification and retained. Primary checkout changes remain untouched; all23 changed files are within journal-ledger. Remote CI is still a publication-time gate.

| Item | Result | File/test evidence | N/A reason | Risk / next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `LedgerBalanceWriteLock.java:25`; `LedgerService.java:155`; module203/PG26/Loan1 PASS | Applicable: concurrency behavior changed | Production contention/load remains untested | `/root/balance_review`, no P0–P3 |
| Q2 | PASS | `../posting-concurrency.md:31`; real lock/wait/rollback/new-tx retry assertions | Applicable: transaction flow changed | Caller retries whole ambient transaction | `/root/balance_review` confirmed |
| Q3 | PASS | `../posting-concurrency.md:87`, `:133`; API/Batch V14 bytes and run evidence | Applicable: migration and execution contract | Coordinated deployment; Batch quiescence | `/root/balance_review` confirmed |
| Q4 | PASS | helper45/50/59; service158/200; concurrency fixture intent comments | Applicable: nontrivial locking logic changed | No remaining comment mismatch | `/root/balance_review` confirmed |

## Draft publication and remote CI prerequisite

- Parent committed the independently reviewed23-file change as `b2af5823` and pushed only `agent/761-concurrent-balances`. Opened [Draft PR #783](https://github.com/skyg547/account/pull/783) against main with `Refs #761`; PR is OPEN/Draft and mergeable. Latest fetched main remains base `0c0fcd3a`.
- [Review handoff](https://github.com/skyg547/account/issues/761#issuecomment-5814930608) records203 module tests,26 PostgreSQL regressions,16 expected baseline failures, independent review, and authority separation. Issue is OPEN/status:needs-review. Parent added the actual PR link and CI limitation to these module-local records; no implementation/test change follows review.
- On published implementation head `b2af5823`, [Module Validation](https://github.com/skyg547/account/actions/runs/36005006837), [Harness Validation](https://github.com/skyg547/account/actions/runs/36005006794), and [Agent Merge Guard](https://github.com/skyg547/account/actions/runs/36005006830) failed before execution. Check annotations107650797995/107650798152/107650799199 state that jobs were not started because recent account payments failed or the spending limit needs increase; the module detection job has an empty steps array. This is an external GitHub Actions prerequisite, not an executed code/test failure. No remote CI PASS is claimed.
- Next owner: repository/account owner checks GitHub Billing & plans, resolves the prerequisite and reruns checks. PR stays Draft and Issue stays open; Ready, merge, Issue close, deployment and resource deletion were not performed. This follow-up only publishes traceability/CI records; shared harness, billing settings, workflows and production/test files remain unchanged.

# 2026-09-24 — GH-764 exact event decimal deserialization

- Issue/claim: [#764](https://github.com/skyg547/account/issues/764), [Codex execution contract](https://github.com/skyg547/account/issues/764#issuecomment-5815230687). Base `origin/main@bd2707af704c4a106328c34477f7e3259d827d99`; branch `agent/764-preserve-event-decimals`; worktree `/tmp/account-issue-764`. The dirty primary checkout and its unrelated files were not changed.
- Model/ownership: `gpt-5.6-sol`, high reasoning. `/root/controller_764` owned only inbound production configuration, `/root/test_764` owned the two new regression files, parent Integrator owned functional docs, module-local records and Git/GitHub, and `/root/review_764` reviewed read-only. The single-module form of `account-module-parallel` kept all writes under `journal-ledger/**`; shared contracts, other modules and repository-level shared harness records remained frozen.
- Root cause: `EventRequest.eventData` and `KafkaTransactionListener` receive untyped maps. Default Jackson converted JSON decimals to `Double` before `JournalRuleEngine` interpolation, so a rounded string reached the existing `BigDecimal` parser and `AccountingPrecision`.
- Change: API Jackson now enables `USE_BIG_DECIMAL_FOR_FLOATS`. A unique `StringJsonMessageConverter` reuses that Boot-managed mapper for Kafka's default String payload. Integer JSON tokens remain integral. Core rule interpolation, `JournalDetail.setAmount`, `AccountingPrecision`, persistence, migrations and financial controls are unchanged.
- Functional contract: raw HTTP/Kafka decimals become `BigDecimal` before `${amount}` interpolation. `900719925474099.11` remains exact; `100.000000000000001` reaches `RoundingMode.UNNECESSARY` with scale15 and is rejected before rounding. HTTP maps this to400; Kafka rethrows listener failure. Retry count and DLQ routing remain deployment error-handler responsibilities.

## Regression and verification evidence

- Audited RED proof: `/tmp/account-764-regression-proof`, branch `agent/764-regression-proof`, exact audited source `a97d10ab6efc2570a88f83d630242cd748f8be57`. The MVC regression file is byte-identical to the fixed branch. `./gradlew :journal-ledger:api:test --tests '*JournalEventPrecisionMvcTest' --rerun-tasks --offline --no-daemon --console=plain --max-workers=1` fails the two intended assertions: the large exact decimal expected HTTP200 but got400, while excess precision expected400 but got200. Production source in the proof worktree was unchanged.
- Focused GREEN: MVC raw JSON2, Kafka raw `ConsumerRecord` conversion/rule interpolation2, and actual Spring Boot converter/default listener-factory wiring1 =5/5 PASS. Tests assert `BigDecimal` type, exact value and scale rather than relying on JSONPath floating-point comparison.
- Exact requested `./gradlew :journal-ledger:test`: BUILD SUCCESSFUL. XML totals are core164/API39/batch5 =208 tests across38 suites, failures/errors/skips0. The parent aggregate task is `NO-SOURCE` but depends on the three real suites and is not counted as a test.
- `./gradlew :journal-ledger:api:bootJar --offline --no-daemon --console=plain --max-workers=1`: BUILD SUCCESSFUL. This is packaging evidence, not a deployed server or broker smoke.
- Scope, `git diff --check`, untracked trailing-whitespace, changed-file conflict-marker and unmerged-index gates pass. All task files are under `journal-ledger/**`.
- Independent `/root/review_764` initially identified two P3 evidence/documentation gaps: direct converter construction did not prove Boot factory wiring, and DLQ wording exceeded configured behavior. The original test author added a real `JournalLedgerApplication` context assertion for the unique converter/default factory; the parent narrowed the documentation. Independent rerun found no remaining P0–P3 and marked Q1–Q4 PASS.

## Rollback, limits and authority

- Rollback is a reviewed Issue-scoped revert of the Jackson property, converter bean, regressions and documentation. There is no migration, production-data transformation or financial reconciliation step.
- No live Kafka broker, deployed HTTP server, production data, PostgreSQL, distributed fault injection or production load was used. The application-context test proves production converter/factory wiring and direct `ConsumerRecord` conversion; actual retry counts and DLQ delivery require deployment-specific integration verification. Scientific-notation amount expressions retain the existing fail-closed parser limitation and were not broadened by this issue.
- Parent is authorized to commit, push and open a Draft PR with `Refs #764`. Ready transition, merge, Issue close, deployment, branch/worktree deletion and proof-worktree cleanup remain later human gates.

| Item | Result | File/test evidence | N/A reason | Risk / next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | API Jackson property; single-purpose converter config; focused5 and module208 PASS | Not applicable: inbound production behavior changed | Live broker remains untested | `/root/review_764`, no P0–P3 |
| Q2 | PASS | `../process-flow.md`; raw JSON → BigDecimal → rule interpolation → `AccountingPrecision`; HTTP400/Kafka exception tests | Not applicable: flow and exception behavior changed | Deployment retry/DLQ policy remains external | `/root/review_764` confirmed |
| Q3 | PASS | `../process-flow.md` documents HTTP/Kafka boundaries, exact examples, domain ownership and deployment limit | Not applicable: event contract documentation changed | Broker/deployment smoke is a later gate | `/root/review_764` confirmed |
| Q4 | PASS | converter Javadoc, YAML precision comment and test no-rounding intent comments | Not applicable: non-obvious deserialization policy changed | Keep comment/property aligned if mapper policy changes | `/root/review_764` confirmed |

## Draft publication and remote CI prerequisite

- Parent committed the independently reviewed eight-file change as `6d96ba0d`, pushed only `agent/764-preserve-event-decimals`, and opened [Draft PR #784](https://github.com/skyg547/account/pull/784) against `main` with `Refs #764`. The PR is OPEN/Draft/MERGEABLE. Issue #764 is OPEN with `status:needs-review`; Ready, merge, Issue close, deployment and cleanup were not performed.
- [Review handoff](https://github.com/skyg547/account/issues/764#issuecomment-5815580888) records the exact208 tests, audited2 expected failures, independent review, residual risks and authority separation. The PR body includes verification, Q1–Q4, rollback, application-wide mapper scope, scientific-notation fail-closed behavior and the no-live-broker limit.
- On published implementation head `6d96ba0d`, Module Validation run36009677391/job107666844502, Harness Validation run36009676896/job107666842811, and Agent Merge Guard run36009676918/job107666843813 failed before executing job steps. Each failure annotation states that recent account payments failed or the spending limit must be increased. Matrix/downstream jobs were skipped.
- This is an external GitHub Actions prerequisite, not an executed code/test failure; no remote CI PASS is claimed. The repository/account owner must resolve GitHub Billing & plans and rerun checks before a later readiness gate. No billing settings, workflow files or repository-level harness files were changed.
