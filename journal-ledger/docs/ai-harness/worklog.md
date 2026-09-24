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
