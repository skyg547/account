# 2026-09-25 — GH-758 immutable posted journal history

## Intake, isolation, and ownership

- Confirmed live [Issue #758](https://github.com/skyg547/account/issues/758), moved it from `status:ready` to `status:in-progress`, and added `agent:codex`. The issue body calls itself a local draft, but the live GitHub Issue and comments are the remote source of truth.
- Preserved the dirty primary checkout. Fetched `origin/main`, created `agent/758-posted-immutability`, and used isolated `/tmp/account-758-posted-immutability` at base `6fdd7a401fe97a85c3a0f637a85bc51e614c1597`.
- Created `agent/758-regression-proof` and `/tmp/account-758-regression-proof` at exact audited source `a97d10ab6efc2570a88f83d630242cd748f8be57`.
- Applied `account-issue-loop`, `account-hexagonal-change`, the single-module/disjoint-owner form of `account-module-parallel`, and `account-review-handoff`, using the requested `gpt-5.6-sol` model at high reasoning.
- Domain, test, and documentation writers owned disjoint files under `journal-ledger/**`; the explorer/planner and `/root/issue_758_review` were read-only. The parent Integrator alone owns module-local records and Git/GitHub actions.
- Other modules, shared contracts, and repository-level shared harness/history files are frozen by the user's explicit allowlist.

## Implementation and acceptance mapping

- `JournalEntry` centralizes final-history detection for POSTED and defensively REVERSED. Every public header setter and collection mutation checks it before changing state.
- `JournalDetail` checks the current, persisted, and prospective parent before changing a line or ownership, so detach/reparent cannot erase the final-state evidence first.
- `@PostLoad`/`@PostPersist`/`@PostUpdate` capture persisted state without traversing lazy details. `@PreUpdate` allows only the legitimate first persisted APPROVED -> POSTED update; already-final dirty update or merge fails. `@PrePersist`/`@PreRemove` protect child insertion and entity/cascade/orphan deletion.
- No generic post-posting audit annotation escape was added: audit identity and timestamps are historical evidence. Correction remains a linked, new reversal/adjustment entry and leaves the original unchanged.
- Independent review found that Spring Data `deleteAllInBatch`, `deleteAllByIdInBatch`, and deprecated `deleteInBatch` use JPQL bulk execution and skip callbacks. This initial P1 was returned to the original owners. Both journal repositories now default-override all four bulk-delete surfaces and fail closed; 8 parameterized persistence cases cover header/detail x four variants.
- Ordinary `delete`, `deleteById`, and `deleteAll` still load entities and use lifecycle callbacks, preserving DRAFT positive controls. The Batch test cleans only its disposable H2 fixture with explicit child-before-parent SQL because production repositories must not expose a callback bypass.
- Existing maker-checker, BigDecimal precision, posting locks, immutable GL/SL snapshot, and reaggregation behavior are unchanged.

## Audited RED proof

- Copied only `JournalPostedImmutabilityAuditedRegressionTest` into the proof worktree; implementation/proof fixture SHA-256 is `4a9d8ab5449d7d5496e11bbeda0c2ff89423baea3b6eecc187d20f26fbcee5ec`.
- `git diff --exit-code -- journal-ledger/core/src/main` in the proof worktree passed, confirming no audited production modification.
- Command: `./gradlew :journal-ledger:core:test --tests 'com.ho.account.journalledger.domain.journal.domain.JournalPostedImmutabilityAuditedRegressionTest' --rerun-tasks --offline --no-daemon --console=plain --max-workers=1`.
- Expected RED: exit1 in23s, 3 tests/3 failures. The audited public APIs changed a POSTED accounting date, changed a line amount, and cleared membership without throwing.
- Current GREEN: the same three tests pass and verify the value/collection remains unchanged after rejection.

## Verification and corrected independent review

- Baseline literal `./gradlew :journal-ledger:test` at the clean base passed core188/API51/batch11 =250 tests.
- Pre-review focused selection passed56/56: aggregate15, audited regression3, persistence11, PostingService15, and PostingConcurrency12.
- The first requested full run exposed four Batch fixture failures because old tests persisted entities already marked POSTED. The test owner corrected only fixture sequencing: persist APPROVED, reload in a transaction, then call the real `post()` transition. Focused Batch passed5/5.
- The requested literal `./gradlew :journal-ledger:test` then passed core207/API51/batch11 =269 tests before the bulk-delete correction.
- Initial independent review raised P1 because inherited repository bulk deletes bypassed `@PreRemove`; an interleaved stale run was explicitly discarded and is not verification evidence.
- Corrected focused commands passed:
  - `./gradlew :journal-ledger:core:test --tests 'com.ho.account.journalledger.infrastructure.persistence.PostedJournalImmutabilityPersistenceTest' --rerun-tasks --offline --no-daemon --console=plain --max-workers=1`:19/19.
  - `./gradlew :journal-ledger:batch:test --tests 'com.ho.account.journalledger.batch.config.BalanceReaggregationBatchConfigTest' --rerun-tasks --offline --no-daemon --console=plain --max-workers=1`:5/5.
- Independent final command: `./gradlew :journal-ledger:test --rerun-tasks --offline --no-daemon --console=plain --max-workers=1`; exit0, BUILD SUCCESSFUL in1m37s, 37/37 tasks executed. XML totals are core29 suites/215 tests, API15/51, Batch4/11 =48 suites/277 tests, failures/errors/skips0.
- `node --test tools/ci/harness-quality-contract.test.cjs` passes32/32. `git diff --check`, tracked/untracked allowlist, unmerged-index, and anchored conflict-marker gates pass.
- Final read-only review reports no remaining P0-P3.

## Q1-Q4 evidence

| 항목 | 판정 (PASS/FAIL/N/A) | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `JournalEntry.java`, `JournalDetail.java`의 응집된 final-history/lifecycle guard; 두 journal repository의 bulk-delete 차단; forced277/277 | 해당 없음: 코드 변경 적용 | 직접 SQL 계층은 별도 DB 권한/trigger 검토 대상 | `/root/issue_758_review`: no P0-P3, PASS |
| Q2 | PASS | `docs/process-flow.md`의 최초 전기 -> 변경/삭제 거절 -> rollback/clear/reload -> 역분개 흐름; persistence19/19 | 해당 없음: 흐름 변경 적용 | 실제 PostgreSQL 동작은 후속 게이트 | `/root/issue_758_review`: PASS |
| Q3 | PASS | `README.md`, `docs/process-flow.md`, `docs/schema.md`; exact 명령, forced277 결과, repository/SQL 경계 | 해당 없음: 기능 문서 변경 적용 | 운영 DB 권한과 배포 문서는 환경 검증 후 확정 | `/root/issue_758_review`: PASS |
| Q4 | PASS | persisted/current 상태 구분, lazy-load 회피, bulk callback 우회, disposable-H2 cleanup 의도 주석 | 해당 없음: 비자명 코드 변경 적용 | JPA provider/DB 변경 시 callback ordering 재검증 | `/root/issue_758_review`: PASS |

## Rollback, limits, and handoff

- Roll back by reverting only this Issue-scoped module change before deployment. There is no migration or data transformation. Reversion restores the known ability to mutate posted history, so posting/correction traffic must not resume under the reverted binary without an approved alternative.
- No live PostgreSQL, production data reconciliation, runtime DB-role test, load, fault injection, or deployment was performed.
- JPA callbacks and the two public repositories do not cover a separately created EntityManager bulk JPQL query, native SQL, direct JDBC, or privileged DB writes. No production journal mutation path using them was found; module docs make the residual boundary explicit.
- Parent publication is authorized only through commit, push, and Draft PR with `Refs #758`. Human review retains Ready, merge, Issue close, deployment, and worktree/branch cleanup authority.

---

The following is retained historical worklog and is not a current GH-758 report.

# 2026-09-25 — GH-757 trusted journal audit actors

## Intake, overlap analysis and ownership

- Confirmed actual GitHub [Issue #757](https://github.com/skyg547/account/issues/757) is OPEN and claimed it from `status:ready` as `agent:codex`. The historical audit body still says no Issue existed; the live Issue and comments are the remote source of truth.
- Preserved the dirty primary checkout. Fetched `origin/main`, created `agent/757-trusted-journal-audit-actors` and isolated `/tmp/account-757-trusted-journal-audit-actors` at `894e95e4b6ab5854a8252e539dcfda03c572b760`.
- Applied `account-issue-loop`, `account-hexagonal-change`, the single-module writer form of `account-module-parallel`, and `account-review-handoff`. `/root/issue757_journal_module` was the sole module writer; `/root/issue757_review` was independently read-only; the parent Integrator owns these module-local records and all Git/GitHub mutations.
- Scope is `journal-ledger/**`, including these three module-local records. Other modules, shared contracts and repository-level shared harness/history files are frozen by the user's explicit narrower instruction.
- Current base analysis found that merged PR #792 already changed the audited `putIfAbsent` event enrichment into an unconditional trusted actor overwrite, rebuilt posting-contract commands from `X-Auth-User`, bound adapter audit identity to the canonical maker, and assigned Kafka/Spring-event service principals. Duplicating those production changes would add no protection, so GH-757 closes the remaining explicit regression-evidence gap.

## Web-boundary regression and audited RED

- Added `JournalAuditActorSanitizationMvcTest`, a standalone MockMvc regression covering both `POST /api/journals/from-event` and `POST /api/v1/journals/posting`.
- Each endpoint has independent forged (`payload-attacker`), blank and conflicting (`payload-maker`/`payload-checker`) cases. The assertions capture the exact map/contract delivered beyond the controller boundary and require both `createdBy` and `auditUser` to equal canonical `trusted-maker`.
- The request includes legacy `X-User-ID` plus current `X-Auth-User`/`X-Auth-Roles`, so the same source compiles and executes against the audited and current web signatures. The payload identity is discarded rather than stored as source identity and never becomes approval identity.
- Audited archive `/tmp/account-757-a97d10a-BzjGLC` uses production files whose SHA-256 values match `git cat-file blob a97d10ab:<path>` for `JournalApiDto`, `JournalPostingRestController`, `JournalRuleEngine` and `JournalPostingAdapter`. The test is byte-identical to the fixed branch with SHA-256 `d65743492885013a2fe83a94c3e3f1a5aec3a9138fc21dcfcdb96b4b63b35f0d`.
- Audited command: `./gradlew :journal-ledger:api:test --tests '*JournalAuditActorSanitizationMvcTest' --console=plain --max-workers=1 --no-daemon`. Expected exit1: 6 tests, 6 failures, errors/skips0—three failures for the event endpoint and three for the posting-contract endpoint. Log: `/tmp/account-757-audited-red.log`; XML remains under the audited archive's API test results.

## GREEN verification and review

- Current focused command with the same test selector passes 6/6, failures/errors/skips0.
- Literal requested `./gradlew :journal-ledger:test` passes in 47s. XML totals: core27 suites/188 tests, API15/51, Batch4/11 =46 suites/250 tests; failures/errors/skips0. The aggregate task is intentionally `NO-SOURCE` but depends on all three real suites.
- Independent `/root/issue757_review` additionally ran the related HTTP/listener/core selection: 35/35 PASS, failures/errors/skips0. It found no P0-P3 and approved the change.
- `git diff --check`, untracked-file whitespace validation, changed-file conflict-marker scan, unmerged-index check and allowlist check pass. Before these parent records, the only working-tree artifact was the permitted new API test.
- Existing `README.md` and `docs/process-flow.md` already explain that body actors are ignored, HTTP uses Gateway-rebuilt trusted headers, non-HTTP adapters use fixed service principals, and approval identity remains separate. No functional behavior changed on this branch, so duplicative documentation edits were intentionally omitted.

| Item | Result | File/test evidence | N/A reason | Risk / next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Single-purpose parameterized MVC regression; focused6 and module250 PASS | Not applicable: executable test changed | Human/Draft PR review | `/root/issue757_review`, no P0-P3 |
| Q2 | PASS | Test traces body/header input through both MVC routes to captured downstream identity; audited6 RED/current6 GREEN | Not applicable: security flow is exercised | Live Gateway remains untested | Reviewer confirmed historical/current comparison |
| Q3 | N/A | `../../README.md` and `../process-flow.md` already describe trusted HTTP/service actors and maker-checker separation | No production/API behavior changed; existing functional documentation is current | Recheck if actor retention policy changes | Reviewer confirmed no documentation drift |
| Q4 | PASS | Test comments explain dual-generation headers and why payload identity cannot outrank trusted context | Not applicable: non-obvious compatibility/security intent exists | Keep headers aligned with boundary evolution | Reviewer confirmed comments match implementation |

## Rollback, limits and publication gate

- Rollback is removal/revert of the single regression and these Issue-local records. There is no production code, migration, schema, financial amount or data correction to roll back.
- No live Gateway/JWT, deployed HTTP service, Kafka broker, PostgreSQL, production data, distributed fault injection or load test was used. MockMvc proves request deserialization, headers, routing and downstream port arguments; the module suite protects existing financial and maker-checker controls.
- Parent is authorized to commit, push and open a Draft PR with `Refs #757`, then move the Issue to `status:needs-review`. Ready, merge, Issue close, deployment and branch/worktree/archive cleanup remain later human gates.

## Draft publication

- Parent committed the independently reviewed four-file module change as `3c1c2069` and pushed only `agent/757-trusted-journal-audit-actors`.
- Opened [Draft PR #793](https://github.com/skyg547/account/pull/793) against `main` with `Refs #757`. Its body records both endpoint matrices, audited/current results, module250, Q1–Q4, rollback, limits and writer/reviewer/Integrator authority separation.
- The Issue handoff moves #757 to `status:needs-review` while retaining `agent:codex`. Ready, merge, Issue close, deployment and branch/worktree/archive cleanup were not performed.
- Remote checks are inspected after this publication-record commit; no remote CI pass is claimed here.

## Remote CI execution prerequisite

- On publication head `b8f02fd0`, Agent Merge Guard run36038521472/job107764548186, Harness Validation run36038521207/job107764546633 and Module Validation run36038521467/job107764548545 failed before executing any steps. Matrix/downstream jobs were skipped or failed as a consequence.
- All three entry-job annotations say the jobs were not started because recent account payments failed or the spending limit must be increased. This is an external GitHub account prerequisite, not an executed code, test or harness failure; no remote CI PASS is claimed.
- Next owner: the repository/account owner resolves GitHub Billing & plans and reruns checks. PR #793 remains Draft and Issue #757 remains OPEN/needs-review. No billing settings, workflows, shared harness files, Ready state, merge, Issue close or cleanup were changed.

---

The following entries are retained history and are not current GH-757 evidence.

# 2026-09-25 — GH-770 concurrent unsettled settlement serialization

## Intake, isolation and ownership

- Confirmed actual GitHub [Issue #770](https://github.com/skyg547/account/issues/770) is OPEN; its body retains the audit draft's historical “No GitHub issue has been created” wording. [Execution contract](https://github.com/skyg547/account/issues/770#issuecomment-5816569426) records branch, isolated worktree, base, module allowlist, verification and Draft PR gate.
- Preserved the dirty primary checkout. Fetched `origin/main` and created `agent/770-unsettled-settlement-lock` in `/tmp/account-770-unsettled-settlement-lock` at `c0b4f204354045adb0db7d9b1ae031879dc60e78`; final fetch showed the same remote base. Detached proof `/tmp/account-770-regression-proof` remains at audited `a97d10ab6efc2570a88f83d630242cd748f8be57`.
- Applied `account-issue-loop`, `account-hexagonal-change`, the single-module ownership form of `account-module-parallel`, and `account-review-handoff`. Planner/reviewer were read-only; Service, SQL, Test and Documentation writers had disjoint file allowlists. Parent alone owns module-local records and Git/GitHub.
- Scope is `journal-ledger/**`, including these three module-local records. Other modules, shared contracts, repository-level shared harness files and `docs/history` are frozen by the user's narrower instruction. This is an explicit exception to the repository-wide shared-record contract; no shared-record synchronization is claimed.
- No production/development credentials, operational database, external service or business data was accessed. PostgreSQL evidence used a task-owned disposable PostgreSQL16 container, loopback-only port, trust authentication, an empty synthetic database and a per-run unique schema.

## Audited defect and behavioral RED

- Current base and audited source have identical unsettled service/domain/port/adapter/repository production files. `UnsettledService` used an ordinary `findById`, so independent transactions could both read remaining100 and persist absolute totals without a version or exclusive lock. The reference-table primary key only deduplicated one reference; it did not serialize distinct effects.
- The new `UnsettledSettlementConcurrencyIntegrationTest` is byte-identical in proof and fixed worktrees, SHA-256 `48d4dbb74fbaef9a5dcab2aea7518d061271fc56824e51375401c70ba73ed0b9`. The proof worktree has no production diff and only this untracked test fixture.
- Audited command: `./gradlew :journal-ledger:core:test --tests com.ho.account.journalledger.infrastructure.persistence.UnsettledSettlementConcurrencyIntegrationTest --offline --no-daemon --console=plain --max-workers=1 --rerun-tasks` exited1 after 5 tests with exactly 3 behavioral failures. Distinct40+50 expected90 but stored50; concurrent same-ref raised reference primary-key violation rather than no-op; after committed60, waiting50 saw stale100 and was not rejected. Full-clear known-ref replay and forced-flush rollback controls already passed.

## Implementation and financial controls

- `UnsettledItemPersistencePort.findByIdForSettlement` expresses an exclusive, transaction-scoped settlement claim while preserving generic reads. `UnsettledService.settleItem` alone switches to that intent; public inbound/API signatures and the domain calculation remain unchanged.
- `UnsettledItemPersistenceAdapter` requires the caller transaction with `Propagation.MANDATORY`, flushes pending work, locks only `unsettled_items.id` via native `FOR UPDATE`, then loads and explicitly refreshes the aggregate and eager reference collection. The ID-only lock avoids PostgreSQL outer-join locking constraints, and refresh prevents a pre-wait first-level-cache snapshot from overwriting the winner.
- The lock remains through service commit/rollback. Existing `AccountingPrecision`, durable reference no-op policy, actor/reference validation, `OPEN -> PARTIAL -> CLEARED`, resolved flag and audit fields remain domain-owned. No migration, `@Version`, schema rewrite or historical repair was introduced.
- Same ID becomes intentionally serialized; different IDs do not share this parent-row lock. Deadlock, serialization and timeout errors propagate, and callers must retry the whole use case in a fresh transaction after rollback. Direct SQL or alternate writers bypassing the port remain outside the protection.

## Regression, verification and independent review

- The fixture uses real JPA repositories and executor threads with independent transactions/connections. It preloads stale contender state and deterministically holds the winner claim. It covers distinct refs40+50 =>90/10 and two refs; concurrent same ref exactly once; full-clear known-ref replay; committed60 then waiting50 rejection against latest40; and failure after actual `EntityManager.flush()` rolling back amounts/status/reference/audit before a fresh retry.
- Parent fixed H2: focused fixture5/5 PASS. Parent disposable PostgreSQL16: identical fixture5/5 PASS, separate backend PIDs and `pg_stat_activity.wait_event_type='Lock'` assertion PASS.
- Requested exact `./gradlew :journal-ledger:test`: BUILD SUCCESSFUL. Forced `./gradlew :journal-ledger:test --rerun-tasks --offline --no-daemon --console=plain --max-workers=1`: core25 suites/178 tests, API13/40, Batch4/11 =42 suites/229 tests; failures/errors/skips0; all37 Gradle tasks executed.
- Independent `/root/issue770_review` reran H2 5/5, PostgreSQL 5/5 and forced module229/229. `node --test tools/ci/harness-quality-contract.test.cjs` passed32/32. Reviewer reports no P0-P3 and Q1-Q4 PASS.
- Parent/reviewer `git diff --check`, untracked trailing-whitespace, explicit changed-file conflict-marker, unmerged-index and module allowlist checks PASS. Nine substantive files are all under `journal-ledger/**` before these parent-owned records.

| Item | Result | File/test evidence | N/A reason | Risk / next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Port/service/adapter responsibilities; H2/PG5 each and forced module229 PASS | Not applicable: production and tests changed | Human/Draft PR and remote CI | `/root/issue770_review`, no P0-P3 |
| Q2 | PASS | `../process-flow.md`, `../schema.md`; success, replay, stale contention, rollback/retry fixture | Not applicable: nontrivial transaction flow changed | Operational retry and load remain later | Reviewer confirmed implementation/test alignment |
| Q3 | PASS | Functional docs include beginner100→40+50 example, commands, expected results and limits | Not applicable: observable concurrency contract changed | Deployment-specific test URL/roles remain external | Reviewer independently executed commands |
| Q4 | PASS | ID-only lock, explicit refresh, stale-cache/flush-rollback test intent comments | Not applicable: locking/cache logic is nontrivial | Recheck if fetch/locking strategy changes | Reviewer confirmed comments match behavior |

## Rollback, limits and publication gate

- Rollback is a reviewed Issue-scoped revert of production, tests and functional/module-local docs; there is no migration or data rollback. Reverting reopens the race, so concurrent settlement traffic must not continue on the reverted version. Existing inconsistent records require separately approved reconciliation.
- This work does not prove production lock-wait distribution, load, distributed retry, explicit deadlock/serialization/timeout injection, operational-role permissions or historical data correctness. The disposable PostgreSQL fixture validates database row-wait/commit/rollback behavior, not production infrastructure.
- Parent is authorized to commit, push and open a Draft PR with `Refs #770`, then move the Issue to `status:needs-review`. Ready, merge, Issue close, deployment, branch/worktree deletion and proof/container deletion remain later human gates.

## Draft publication

- Parent committed the independently reviewed 12-file module change as `86d2a39e` and pushed only `agent/770-unsettled-settlement-lock`.
- Opened [Draft PR #786](https://github.com/skyg547/account/pull/786) against `main` with `Refs #770`. The body records acceptance/RED/GREEN evidence, Q1-Q4, scope, rollback, limits and the writer/reviewer/Integrator authority separation.
- Moved Issue #770 to `status:needs-review` and added the [verification handoff](https://github.com/skyg547/account/issues/770#issuecomment-5817023301). Ready, merge, Issue close, deployment and resource deletion were not performed.
- Stopped task-owned `account-770-postgres` after fixed and independent PostgreSQL verification; the exited container is retained. The branch, implementation worktree and detached proof worktree also remain retained for human review.
- This follow-up changes only the three module-local records to add actual publication state. Remote CI is pending; no remote PASS or readiness claim is made.

## Remote CI execution prerequisite

- On published Draft head `a1bbf0c4`, Module Validation run `36019983902`, Harness Validation run `36019983966` and Agent Merge Guard run `36019984123` failed before executing job steps. Downstream module/discipline jobs were skipped.
- Check-run annotations for the three entry jobs state that recent account payments failed or the spending limit must be increased. Each failed job reports an empty steps array. This is an external GitHub Actions prerequisite, not an executed code/test failure; no remote CI PASS is claimed.
- Next owner: repository/account owner resolves GitHub Billing & plans and reruns checks. PR remains Draft and Issue remains OPEN/needs-review. No billing settings, workflows, shared harness files, Ready state, merge or Issue close were changed.

---

The following entries are retained history and are not current GH-770 evidence.

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

# 2026-09-25 — GH-756 maker-checker approval writer checkpoint

- Issue/claim: #756; branch `agent/756-maker-checker-approval`; isolated worktree
  `/tmp/account-756-maker-checker-approval`; base `origin/main@054cdf132dc7912346bdb8a06f2f7860e332351a`.
  This is an unpublished writer checkpoint. Parent Integrator owns independent review, commit, push,
  Draft PR, remote checks and final records.
- Scope: all changes are under `journal-ledger/**`. Shared contracts, other services and repository-level
  harness/history files are unchanged.
- Architecture: HTTP adapters parse Gateway-rebuilt `X-Auth-User`/`X-Auth-Roles` and enforce maker,
  approver and poster roles. Core canonicalizes every actor (trim + `Locale.ROOT` lowercase, max50),
  requires a maker, enforces `DRAFT -> REQUESTED -> APPROVED -> POSTED`, rejects canonical self-approval,
  and preserves `approvedBy` when `auditUser` changes to the poster.
- Event/machine policy: HTTP payload actor fields are overwritten by the trusted principal; Kafka uses
  `service:journal-kafka-maker`; the Spring contract event listener reconstructs commands with
  `service:journal-spring-event-maker`. Contract drafts persist canonical `createdBy` and ignore payload
  `auditUser` as a separate identity source. `approveAndPost` submits a DRAFT as its stored maker and
  proceeds only with a distinct checker service principal. Shared `SYSTEM` maker/checker is rejected.
- Schema: V16 adds `journal_entries.approved_by`. It backfills only still-`APPROVED` rows whose
  `created_by` and `audit_user` are both nonblank and canonically distinct. Self-approved and makerless
  rows remain NULL, as do already-`POSTED` rows whose historical checker cannot be reconstructed.

## Verification

- Focused command selecting domain/service/adapter/migration plus HTTP/Kafka regressions: exit0.
  Selected classes contain28 tests, failures/errors/skips0. The self-approval and direct-DRAFT tests are
  remediation regressions for audited `a97d10ab`.
- `JournalMakerCheckerAuditedRegressionTest` is a standalone byte-copyable RED/GREEN fixture using only
  APIs present at `a97d10ab`: it requires maker approval of its own balanced DRAFT to throw without state
  mutation. Parent copied it byte-identically to `/tmp/account-756-regression-proof` at exact audited
  commit `a97d10ab6efc2570a88f83d630242cd748f8be57`; both files have SHA256
  `f5611fe1c23d3278b5edeab9b4f70db6c4ff33ae368461dcec69f92251303ba8`. The focused audited run compiled
  and executed1 test with1 expected failure, errors/skips0: the old code raised no throwable and changed
  the DRAFT instead of enforcing maker-checker separation.
- Corrective focused command selecting the Spring-event trust-boundary test, migration evidence test and
  audited regression fixture: exit0, 29s, 3 tests with failures/errors/skips0.
- Literal requested `./gradlew :journal-ledger:test`: exit0, 1m11s. XML totals are core188/API45/batch11
  =244 tests across45 suites, failures/errors/skips0. The parent aggregate task is NO-SOURCE and is not
  counted. API/Batch local contexts apply V16 and Hibernate validate as part of the existing suites.
- `./gradlew :journal-ledger:api:bootJar :journal-ledger:batch:bootJar --offline --no-daemon
  --console=plain --max-workers=1`: exit0. This verifies packaging only, not deployed startup; both boot
  archives were created after V16 was added.
- Parent reran the literal requested command after all corrections: exit0, BUILD SUCCESSFUL in1m02s.
  Independent `/root/issue756_review` then forced a clean `--rerun-tasks` module run: exit0 in1m36s,
  core188/API45/batch11 =244 tests across45 suites, failures/errors/skips0. The reviewer also confirmed
  API/Batch bootJAR packaging and all static gates.
- `git diff --check`, changed-scope allowlist and conflict-marker scan: PASS. No file outside
  `journal-ledger/**` changed.

## Rollback and residual risks

- Rollback is a reviewed Issue-scoped code/test/doc revert. Applied Flyway migrations are immutable:
  retain V16/`approved_by` even if binaries roll back, or use a separately reviewed forward migration.
  V16 does not change amounts or ledger balances.
- Repository search found direct Journal HTTP adapters in Closing, Deposit, Expenditure Resolution,
  Loan, Payable, Receivable and Reconciliation. All seven need trusted maker headers/roles for draft
  creation; Deposit currently fails closed for remote approval, while the other approval/post callers
  also use legacy `X-User-ID` and omit request-approval. They will fail closed until an authorized
  cross-module follow-up supplies Gateway-authenticated headers, distinct service principals and the
  controlled transition. Those files are outside this writer's allowlist and were not changed.
- No live Gateway/JWT, Kafka broker, PostgreSQL, cross-service HTTP deployment, production data or load
  test was used. H2 proves migration/wiring behavior, not PostgreSQL deployment timing. Historical POSTED
  rows intentionally retain a NULL checker evidence gap.

| Item | Result | File/test evidence | N/A reason | Risk / next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `JournalCommandAuthorization`, `JournalActor`, `JournalEntry`; focused28 + corrective3/module244 PASS | Applicable: command/domain responsibilities changed | Seven remote consumers remain a deployment gate | `/root/issue756_review`; no P0–P3 |
| Q2 | PASS | `../process-flow.md`; HTTP → request → approve → post and machine policy; failure tests | Applicable: lifecycle and errors changed | Live cross-service flow untested | `/root/issue756_review` confirmed |
| Q3 | PASS | `../../README.md`, `../beginner-guide.md`, `../process-flow.md`, `../schema.md` | Applicable: public headers/state/schema changed | Consumer code/docs outside allowlist remain a follow-up | `/root/issue756_review` confirmed |
| Q4 | PASS | trusted HTTP/Spring/Kafka identity replacement, adapter auto-submit and V16 non-invention comments | Applicable: non-obvious trust/migration choices changed | Keep comments and service principals aligned | `/root/issue756_review` confirmed |

Independent review initially found that V16 would have legitimized legacy self-approved rows and that the
Spring event listener trusted payload maker identity. The original writer narrowed the migration to
nonblank, canonically distinct evidence, added negative upgrade rows, bound Spring events to a fixed service
maker, expanded the seven-consumer inventory and reran verification. Final review found no P0–P3.

## Draft publication and remote CI prerequisite

- Parent committed the independently reviewed39-file module change as
  `a6ecb1d69be232d9937624e8b26244d0537fd8a9`, pushed only `agent/756-maker-checker-approval`, and opened
  [Draft PR #792](https://github.com/skyg547/account/pull/792) against `main` with `Refs #756`. The PR is
  OPEN/DRAFT/MERGEABLE. Issue #756 remains OPEN and is synchronized to `status:needs-review`.
- On the published implementation head, Module Validation run36036177785/job107756709599, Harness
  Validation run36036177968/job107756711043 and Agent Merge Guard run36036177942/job107756711683 failed
  before executing steps. Each failure annotation states that recent account payments failed or the
  spending limit needs increase. Matrix/downstream checks were skipped or queued.
- This is an external GitHub Actions prerequisite, not an executed code/test failure; no remote CI PASS is
  claimed. The repository/account owner must resolve Billing & plans and rerun checks. Human review and the
  seven remote-consumer compatibility changes remain deployment gates. Ready, merge, Issue close, deployment
  and branch/worktree/proof cleanup were not performed.
