# Closing worklog

## 2026-09-25 — contract and reproduction

The user authorized implementation within `closing/**`, a dedicated branch/worktree, verification and Draft PR publication. The explicit ban on shared harness edits takes precedence over the generic shared-record rule. These three module-local records are the parent-owned handoff.

The fetched base is `8e476f1a7caf2612860652df5069302abd6599ec`; the audited revision is `a97d10ab6efc2570a88f83d630242cd748f8be57`. The two production files used in the RED reproduction are byte-identical at these revisions:

| File | Git blob at both revisions |
| --- | --- |
| `closing/core/src/main/java/com/ho/account/closing/application/service/ClosingService.java` | `9a524daf04370efe3cd0b383764286ce8846e780` |
| `closing/core/src/main/java/com/ho/account/closing/infrastructure/external/ClosingStatusAdapter.java` | `44a051e65e2e270c69625994b08ed23950883329` |

The pre-change focused regression exercised the real Closing status adapter and service. Three active lock types plus `IN_PROGRESS` each incorrectly returned open: **4 tests, 4 assertion failures, 0 errors**. This executes the unchanged audited admission implementation on the fetched base; it does not claim a separate full checkout run of the audited revision. Local reproduction artifacts are `/tmp/account-772-service-red.log`, `/tmp/account-772-service-red-test.java`, and `/tmp/account-772-service-red-results.xml`.

Exploration found that standalone Journal selects its own `@Primary FiscalPeriodAccountingPeriodStatusAdapter` and never asks Closing. The packaged Closing API/Batch have no Journal core dependency. The root `closing` project's Journal dependency is not inherited by these children. GL07/#762 is still OPEN and its commit fence is absent from this base. Therefore the closing-only supplier and seam tests cannot establish the full Issue's production acceptance criteria.

## Implementation

- One query combines validated Master period identity/date/explicit OPEN, matching OPEN calendar and absence of an active lock.
- Closing's old boolean API and its `AccountingPeriodStatusPort` adapter delegate to that query. The query has no Journal command dependency and introduces no constructor cycle.
- A read-only HTTP endpoint returns the same ordinary posting snapshot with `no-store`; invalid input returns 400 and unavailable data returns sanitized 503.
- All lock rows block the date-only contract; no adjustment exemption is inferred from type strings or actors. Existing controlled adjustment registration and close/reopen workflows retain their checks.
- The advertised `:closing:test` now depends on Core/API/Batch test tasks. Journal core is a Core test-only dependency to exercise real validation/posting consumers without changing deployment classpaths.

Verification, independent review and publication results will be appended after execution. No database migration, operational access or deployment is part of this change.

## Verification

| Command / evidence | Result |
| --- | --- |
| Pre-change focused four regression invocations | Expected RED: 4 failures, 0 errors; original service/adapter match audited blobs above |
| `bash gradlew :closing:core:test --tests '*ClosingAdmissionServiceTest' --tests '*ClosingServiceTest' --tests '*ClosingJournalAdmissionIntegrationTest' --offline --console=plain --max-workers=1 --no-daemon` | PASS, 94 tests (44 + 23 + 27), no failures/errors/skips |
| `./gradlew :closing:api:test --tests '*ClosingAdmission*' --offline --console=plain --max-workers=1 --no-daemon` | PASS, 21 tests (controller15 + real HTTP/H2 lifecycle6), 34 seconds |
| **`./gradlew :closing:test`** | **PASS, 314 tests / 41 suites: Core199/API95/Batch20, failures/errors/skips0; 1m4s, exit0** |

Focused results are included in the full 314, not additional to it. All three child test tasks executed; the parent `:closing:test NO-SOURCE` is expected and does not replace the children. Final suite XML and counts are archived at `/tmp/account-772-full-evidence/`; command log is `/tmp/account-772-full-test.log`.

The live test starts a random-port Closing HTTP server with synthetic H2 persistence. It exercises commands through transaction proxies and sends fresh HTTP requests after command commits. It proves all three lock types, unlock, IN_PROGRESS, CLOSED, approved reopen retaining an independent lock, missing calendar/master, no audit writes from queries, and no Journal runtime classes added to Closing API. Core seam tests instantiate the real Journal filter and PostingService with Closing's actual status provider and synthetic outbound ports, checking immutable journal/detail state and absence of all writes on denial.

Not executed: live PostgreSQL, deployed standalone Journal-to-Closing requests, network timeout/restart of a distributed admission protocol, barrier-controlled GL07 close-versus-post commits, production load, deployment or hosted CI. These are remaining acceptance gates, not covered by H2 or successful packaging.

## Independent review and packaging

Read-only `/root/review_772` independently inspected all 19 approved Closing paths, the original audited/base blob identities, RED/GREEN XML and the full execution log. It found **no new scoped P0–P3 findings**, Q1–Q4 PASS. It explicitly placed **full #772 acceptance on HOLD** because standalone Journal consumer selection, governed adjustment authorization and GL07 commit fencing are not implemented in this scope. The reviewer did not edit files, mutate Git/GitHub or duplicate the coordinated Gradle runs.

`./gradlew :closing:api:bootJar :closing:batch:bootJar --offline --console=plain --max-workers=1 --no-daemon` passed, exit0/14s. Parent inspected both `BOOT-INF/lib` inventories: neither includes a Journal runtime JAR. API artifact SHA256: `1b6f4092cd95e4c92a2823b914976cd586ba88c3ee50fb98ea5040b77fcd777b`; Batch: `35d08fd3733dd237af76095f398dfc7dcae467598c65ebd9692b456d67dbf0f2`. These are packaging and dependency-isolation checks; live execution evidence is the separate H2/HTTP test, not a claim that the packaged JAR was deployed.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `ClosingAdmissionService.java:34`; actual Journal consumer27; full314 PASS | 해당 없음: 코드 변경 | standalone Journal consumer and GL07 fence remain | `/root/review_772`, no scoped P0–P3 |
| Q2 | PASS | `closing/docs/process-flow.md:95`; committed H2/HTTP lifecycle6 PASS | 해당 없음: 조회·상태 흐름 변경 | snapshot is not a commit permit | `/root/review_772` |
| Q3 | PASS | `closing/docs/local-run.md:71`; README partial-scope warning; aggregate child tasks verified | 해당 없음: 기능 문서 변경 | deployed remote integration not executed | `/root/review_772` |
| Q4 | PASS | `ClosingAdmissionService.java:61,72`; `ClosingAdmissionController.java:35,53` | 해당 없음: 비자명 로직 변경 | trusted adjustment policy must not use caller strings | `/root/review_772` |

Rollback: reviewed Issue-scoped revert of these Closing files; no migration or data repair. Parent alone publishes the branch and requested Draft PR. Reviewer cannot perform commit/push/Ready/merge/Issue close. Resources and the Issue remain open for the next integration owner.

## Draft publication

Published [Draft PR #787](https://github.com/skyg547/account/pull/787), OPEN/DRAFT, `Refs #772`, from `agent/772-closing-journal-admission` to `main`. Implementation commit `46800ead5d643d81b6beeaea75b8e25b16e6ad23` was pushed after scoped verification and independent review. All 19 changed paths are under `closing/**`. No Ready, merge, Issue close, deployment or resource deletion was performed.

Initial-head hosted [Module Validation](https://github.com/skyg547/account/actions/runs/36022330652) and [Agent Merge Guard](https://github.com/skyg547/account/actions/runs/36022330328) failed **before jobs started**. Their annotations report failed recent account payments or a spending-limit restriction; the exact account condition was not investigated. No hosted code/test result is inferred. The repository account owner must resolve that external gate before hosted CI can execute. Local 314-test and packaging evidence remains separate. This publication-only record update does not change tested source or build behavior.

## GH-774 — 2026-09-25 contract and design

The user requested Issue #774 through the issue-loop, hexagonal-change and module-parallel skills, with one Closing module writer and disjoint integration-test ownership. Existing dirty primary checkout changes were preserved. The isolated branch starts at fetched `origin/main@1d3e6264c6703bace265186f3319f44407dc1458`.

The explicit `closing/**` allowlist and ban on shared harness edits take precedence over generic shared-record instructions. These existing module-local records are updated by the parent only; GH-772 history is retained.

Required acceptance evidence: barrier-controlled approve/reject with one winning decision and loser conflict; calendar/Master agreement after tested failure recovery; close/start/task/gate schedules under the same root lock; baseline-failing regressions and preserved financial controls. Required full command: `./gradlew :closing:test`.

Inspected the existing Closing service/domain/repositories/API, Master control contract/adapter, migration packaging and module documents. Master currently exposes GET and ordinary status PUT with a row lock, but no operation key/CAS/fencing. A local database transaction cannot roll back a committed remote PUT. The design therefore combines a calendar row lock with durable local transition intent, an at-most-once dispatch marker and explicit reconciliation. An ambiguous dispatched operation must remain fenced instead of being blindly replayed.

Planned substantive paths are Closing core service/domain/ports/persistence and a forward migration, recovery API/DTO/advice, focused unit and H2 transaction/failure tests. Parent updates process/schema/local-run documents from verified implementation. No shared contract, other module, production database, deployment or historical-data repair is authorized. Rollback must preserve unresolved transition evidence; no destructive down migration is planned.

### GH-774 pre-fix regression evidence

The executed pre-fix command was `./gradlew :closing:api:test --tests com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest --console=plain --max-workers=1 --no-daemon`. It produced **1 test, 1 expected assertion failure, 0 errors/skips**: the competing rejection returned successfully while the approval also succeeded. The test stopped at that failed conflict assertion, so this execution does not claim a captured final OPEN/REJECTED database snapshot.

This used real separate Closing Spring transactions and a separate autocommit H2 database for the synthetic Master. The service file blob was `e43a91e7fec8078602c67777baa28df18473d3fd`; the eight relevant decision/request/close/start/task/gate method bodies are byte-identical between audited `a97d10ab6efc2570a88f83d630242cd748f8be57` and fetched base. The full service differs because #772 added the independent admission query. Local evidence: `/tmp/account-774-red-concurrency.log`, `/tmp/account-774-red-concurrency.xml`, `/tmp/account-774-red-concurrency-test.java`, `/tmp/account-774-audited-methods.json`.

The final concurrency regression strengthens the schedule with worker-specific Master lookup barriers rather than a timed scheduling opportunity. Final GREEN results are recorded separately below.

### GH-774 implementation and initial verification

The calendar is the monthly mutation root. The JPA adapter locks and refreshes it before reading mutable approval/task/gate state, including previously managed checklist collections. Closing start/close/decision coordinators reject ambient transactions; independent REQUIRES_NEW phases preserve PREPARED and DISPATCHED intent across remote/local failures. Calendar creation and child creation reject existing IDs to prevent merge-based overwrite outside this policy. Period lock/unlock and controlled adjustments also use the root lock.

Recovery GET/POST APIs use Gateway actor/role headers. The approved decision is durable before Master dispatch; losing decisions return 409, whereas an ambiguous dispatch returns 503 `PERIOD_TRANSITION_RECOVERY_REQUIRED`. Recovery never resends DISPATCHED. PREPARED can resume once. Audit distinguishes pre-dispatch resume from original-request-termination reconciliation and preserves operation/original/recovery actors. V52 adds nullable fields and a completeness/fail-closed CHECK; shared migration-runner discovers it without edits.

Initial focused verification: `./gradlew :closing:core:test --tests com.ho.account.closing.application.service.ClosingServiceTest --tests com.ho.account.closing.domain.ClosingDomainTransitionTest --tests com.ho.account.closing.application.service.ClosingAdmissionServiceTest :closing:api:test --tests com.ho.account.closing.ClosingMonthlyTransitionMigrationTest --tests com.ho.account.closing.ClosingPostgresqlSchemaContextTest --tests com.ho.account.closing.web.ClosingTransitionControllerTest --console=plain --max-workers=1 --no-daemon` passed Core70/API5, failures/errors/skips0 (`/tmp/account-774-narrow.log`). Final additional regression coverage and the required full suite are recorded below rather than inferred from these focused results.

Independent source review found one P2: PREPARED resume incorrectly wrote an original-request-termination attestation. The writer corrected the audit reason; the reviewer confirmed the fix. Initial concurrent/fault-injection suites passed 22 tests; final refinements add direct HTTP409/503, PREPARED recovery and stale gate/calendar evidence before the final whole-module run.

### GH-774 final module verification

| Command / artifact | Result |
| --- | --- |
| `./gradlew :closing:api:test --tests com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest --tests com.ho.account.closing.ClosingTransitionRecoveryIntegrationTest --console=plain --max-workers=1 --no-daemon` | PASS28 (aggregate13/recovery15), failures/errors/skips0,33s; `/tmp/account-774-integration-final.log` |
| **`./gradlew :closing:test`** | **PASS348/45 suites: Core201/API127/Batch20; failures/errors/skips0, exit0,1m14s,29 tasks(8executed/21up-to-date)** |
| Scoped diff/new-file whitespace, conflict markers, unmerged index, allowlist | PASS;31 paths all under `closing/**`,24 production/test/migration source hashes unchanged after full test |

All Core/API/Batch test tasks executed; parent `:closing:test NO-SOURCE` does not stand in for child coverage. Focused75 and28 are subsets, not extra unique tests. Final XML and summary are archived in `/tmp/account-774-full-evidence/`, full log `/tmp/account-774-full-test.log`, frozen source manifest `/tmp/account-774-source-freeze.json` and path list `/tmp/account-774-changed-files.txt`.

The tests include actual Spring transaction proxies, an independent committed H2 Master, worker-controlled approve/reject barriers and persisted final state/audit assertions, synthetic post-remote/local-commit failure injection, initial PREPARED recovery and concurrent dispatch claim, stale operation/managed entity defenses and actual MockMvc409/503 responses. Existing random-port HTTP admission lifecycle also ran in the full suite. Clean V49–V52 migration/JPA validation and V51 forward upgrade preserve prior data. H2 PostgreSQL mode is not a live PostgreSQL test. Deployed network faults, process kills, production load/data and deployment were not exercised.

### GH-774 independent review and packaging

Read-only `/root/closing_774_review` independently compared source/test/docs, the preserved RED and method hashes, focused28 XML, full348 archived XML/log, and source freeze. It found no unresolved findings and confirmed Q1–Q4 PASS. The sole earlier P2 audit wording finding was corrected and its two recovery paths are now tested. The reviewer did not edit code or run a concurrent duplicate Gradle build.

`./gradlew :closing:api:bootJar :closing:batch:bootJar --offline --console=plain --max-workers=1 --no-daemon` passed, exit0/14s. Both executable JARs contain the exact V52 resource and transition service from Closing core; neither adds a Journal runtime dependency. This is packaging evidence, not a claim of packaged deployment. Log `/tmp/account-774-packaging.log`, artifact checks `/tmp/account-774-package-evidence.json`.

- API SHA256: `c01b2388f6227eae725151d8d4b90c75635a9ccc6143d429f0e8f3379d6ebe25`.
- Batch SHA256: `cf5cd5e358b602e1ea0ab962a65ff2b7d7db1444dfd2a8fd0403b308e8a1374b`.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `ClosingPeriodTransitionService.java:18`, `ClosingTransitionTransactions.java:26,56,105,119`, `JpaClosingAggregatePersistenceAdapter.java:68,83`; focused28/full348 PASS | 해당 없음: 구현 적용 대상 | 실제 PostgreSQL/배포 네트워크 장애·부하 후속 검증 | `/root/closing_774_review`, 미해결 finding 없음 |
| Q2 | PASS | `closing/docs/process-flow.md:98`; 준비→전송→완료, 실패·복구·호출자 트랜잭션 설명 | 해당 없음: 흐름 설명 적용 대상 | 원 요청 종료는 운영 확인이며 Master 직접 writer 차단 계약 없음 | `/root/closing_774_review` |
| Q3 | PASS | `closing/docs/local-run.md:96`, `closing/docs/schema.md:46`, README; V52/JPA/API·Batch 검증 | 해당 없음: 기능 문서 적용 대상 | V52 선적용·구버전 writer 종료·실배포 별도 | `/root/closing_774_review` |
| Q4 | PASS | `ClosingTransitionTransactions.java:63,111,132`, JPA adapter:86, `ClosingCalendar.java:58`; 독립 DB/rollback 주석 | 해당 없음: 비자명 로직 적용 대상 | 원격 종료 확인과 PREPARED 재개의 감사 의미를 유지 | `/root/closing_774_review` |

Rollback/next owner: preserve unresolved operation and audit evidence, reconcile Master before a reviewed Closing-only revert, and drain old writers. Parent publishes only the authorized dedicated branch and Draft PR; a human reviewer owns current-head CI, deployment prerequisites and any later Ready/merge/Issue-close decision. No live database/deployment/cleanup occurred.

### GH-774 Draft publication

Published [Draft PR #788](https://github.com/skyg547/account/pull/788), OPEN/DRAFT with `Refs #774`, from the dedicated branch to `main`. Implementation commit `9a4348071ca5de39ecee266c08a0009cae16a722` was committed/pushed only after full verification and independent source/record/PR review. Issue #774 is OPEN / `status:needs-review`; the [handoff comment](https://github.com/skyg547/account/issues/774#issuecomment-5817690314) preserves evidence and remaining gates.

Initial implementation-head hosted CI did not execute its jobs: GitHub annotations report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36024856421), [Harness Validation](https://github.com/skyg547/account/actions/runs/36024856552) and [Merge Guard](https://github.com/skyg547/account/actions/runs/36024856595) are failed before execution; no hosted code/test pass is claimed. The repository account owner must resolve that external gate before current-head CI can run. No billing settings were inspected or changed.

The publication follow-up updates only the three module harness records. All24 verified production/test/migration hashes remain unchanged, so Java tests are not redundantly rerun for publication text. No conflict occurred; no shared harness, other-module files, Ready/merge/Issue close/deployment or resource cleanup changed. Parent retains the branch and isolated worktree for human review.

## GH-779 — FX posted carrying value (2026-09-25)

- Issue: [#779](https://github.com/skyg547/account/issues/779), CL15. Branch `agent/779-fx-carrying-value`; worktree `/tmp/account-779-fx-carrying-value`; fetched base `origin/main@363cdff48069eb5930ae4a45b0f5e56045d66122`.
- User-authorized scope: `closing/**` only. Shared harness and other modules are read-only; these existing module-local records satisfy the requested harness update within that scope. Primary-checkout user changes are preserved.
- Claim: [5817780319](https://github.com/skyg547/account/issues/779#issuecomment-5817780319), Codex / `status:in-progress`. Requested delegated model/reasoning: GPT-6 Astra / xhigh.
- Goal: include posted reporting-currency FX adjustments and effective reversal history in source-currency carrying value without changing foreign principal. Keep current financial validation, posting policy, deterministic identity, bounded partition metadata and cursor lifecycle.
- Diagnosis: the source excludes reporting-currency journals; the service writes them with an existing `FX_VALUATION` / `batch|account|currency` lineage. These two files are byte-identical between the audited `a97d10ab` and this fetched base (`git diff` empty).
- Ownership: read-only `/root/fx_source_analysis`; SQL writer `/root/fx_sql_fix` owns only `JournalFxValuationBalanceSource.java`; test writer `/root/fx_regression_tests` owns its source test and new posted-history integration test. Parent owns module documentation/records and Git/GitHub. Independent review follows implementation.
- Verification plan: demonstrate pre-fix regression failure; focused actual SQL/service synthetic-H2 suite; exact `./gradlew :closing:test`; affected boot packaging; allowlist/whitespace/conflict checks; independent Q1–Q4 review. Production data, live PostgreSQL and load testing are outside this local verification.
- Rollback: reviewed revert of the Issue-scoped Closing changes. No migration or live data mutation is planned. Deliver an OPEN/DRAFT PR with `Refs #779`; Ready, merge, Issue close and cleanup remain later gates.

### Regression proof and SQL compatibility correction

- Pre-fix command: `bash ./gradlew :closing:batch:test --tests '*FxValuationPostedHistoryTest.unchangedRateAfterPostedValuationCreatesNoAdditionalJournal' --offline --no-daemon --console=plain --max-workers=1`. Exit1 after25s; 1test/1failure/0errors/0skips; XML timestamp `2026-09-24T16:13:04`, archived at `/tmp/account-779-fx-history-red.xml`.
- The initial RED fixture used USD100/base100000/rate1100: May and June each generated10000, so expected1command was2. Production source/service had no diff at that point and match the audited revision. The final regression uses the Issue's scaled example USD100/base1000/rate12/adjustment200; this is an intentional fixture change, not the exact original RED test artifact.
- Initial recursive query integration failed H2 SQL parsing before financial assertions. After the same alias failure repeated, full Gradle retries stopped and the SQL writer isolated H2 recursive-CTE/UNION and parameter-binding behavior in synthetic direct JDBC probes. These failed runs are diagnostic evidence, not verification passes.

### Final implementation and whole-module verification

- The reader now shares one contribution relation between account discovery and cursor aggregation. Ordinary foreign journals contribute signed principal/base; existing FX lineage and recursive reversal ancestry contribute signed base only on the attributed account leg. Each contribution uses its own POSTED status/accounting-date cutoff. Unchanged-rate evaluation generates no new journal, while changed rates use the already-adjusted carrying value.
- Known FX metadata/header/account-leg errors fail closed before partition processing and in direct cursor reads. Adjustment-only residual accounts remain visible to the existing core inconsistency guard. Core formulas, posting policy, lineage format, public/shared contracts and schema are unchanged.
- H2 compatibility: contribution selection uses CASE rather than a UNION after recursion, and bind parameters appear only in the final SELECT. A direct parameterized synthetic JDBC probe returned foreign100/base120 after an adjustment/reversal cancellation, followed by focused32/32 PASS. Final header-policy fixture refinement passed8/8; these focused cases are subsets of the whole suite below, not additional tests.
- Exact required command `./gradlew :closing:test`: BUILD SUCCESSFUL, exit0,1m23s. All three child test tasks executed; the source-free parent task is correctly NO-SOURCE. Core201/API127/Batch50 = **378 tests,46 suites, failures/errors/skips0**. Includes existing Spring API/Batch context, migration-schema, financial policy and cursor lifecycle tests. Log `/tmp/account-779-closing-full.log`; XML and summary preserved in `/tmp/account-779-full-evidence/` before independent focused replay.
- `./gradlew :closing:api:bootJar :closing:batch:bootJar --offline --console=plain`: BUILD SUCCESSFUL, exit0,14s. Log `/tmp/account-779-packaging.log`. Packaging verifies executable artifacts; no deployed service or real Journal posting is claimed.
- Static scope gate:11 approved changed paths, all under Closing. Changed-line/new-file whitespace, conflict markers and unmerged index checks pass. An initial whole-file whitespace probe encountered pre-existing README spacing; the correct diff-scoped gate passes without out-of-scope cleanup. Primary checkout status remains the same as intake.
- Changed files: `closing/batch/.../JournalFxValuationBalanceSource.java`; existing `JournalFxValuationBalanceSourceTest.java`; new `FxValuationPostedHistoryTest.java`; `closing/README.md`; `closing/docs/{process-flow,schema,beginner-guide,local-run}.md`; module-local `closing/docs/ai-harness/{agent-status,worklog,handoff}.md`.
- Residual gates: no real PostgreSQL SQL/EXPLAIN/load execution (local PostgreSQL binaries unavailable), deployed Journal approval/posting, distributed faults, concurrent posting snapshot, production repair or100M load run. Existing source-lineage/index usage and repeated recursive scan costs require plan/load validation. Orphan reporting reversals cannot be classified without their source and require reconciliation. Tests use real Reader/service and synthetic H2 persistence at the Journal port.
- Rollback: reviewed revert of this Issue-scoped Closing change; preserve posted journals and lineage. No schema migration or live data rollback is required by this implementation. The old reader reintroduces the duplicate-evaluation defect; suspend affected FX jobs while resolving a rollback. Reviewer stays read-only; parent owns commit/push/Draft, later human review owns Ready/merge/Issue close.

### Independent review and delivery gate

- Independent read-only `/root/fx_independent_review` (GPT-6 Astra/xhigh): no unresolved P0–P3, Q1–Q4 PASS. Own command `./gradlew :closing:batch:test --tests '*FxValuationPostedHistoryTest' --tests '*JournalFxValuationBalanceSourceTest' --offline --no-daemon --max-workers=1 --console=plain`:32/32 PASS, fail/error/skip0, exit0,17s; test task executed. Observed UTC start16:24:36/end16:25:01; XML suite timestamps16:24:51/16:24:54.
- Reviewer independently verified full archived378/46suites, all three child test tasks executed, final source/test hashes unchanged. Production source SHA256 `ebe393090534fcdb0c2457bcc92464b5821dc2356afe6e02e45595d375ac3d15`; history test `cf4591ce4b6ba885b683ed71b482df8492547fc781b89b2842428e9c13872693`.
- Boot JAR SHA256: API `098225ed162e7b921ac32ae2600c70eedffbfb093d7c1b441ba9adddeb939789`; Batch `2f399d414f8bdb4c81baa000def4233bc490bba4a930c395b78832e64ddb6f1a`.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `JournalFxValuationBalanceSource.java:69,91,135,163,227`; `FxValuationPostedHistoryTest.java:95,115,138`; required378 and independent32 PASS | 해당 없음: SQL/테스트 책임과 금융 계산 입력 변경 | PostgreSQL 실행계획·부하 미검증 | `/root/fx_independent_review`, P0–P3 없음 |
| Q2 | PASS | `closing/docs/process-flow.md:210,224,230,235`; reversal/retry/cursor tests188,205,223,271,288 | 해당 없음: 귀속·역분개·재시작 흐름 변경 | 동시 전기 snapshot·분산 장애 미검증 | 동일 리뷰어 확인 |
| Q3 | PASS | `closing/README.md`; beginner-guide39, schema84, local-run186; exact378 tests and bootJar2 PASS | 해당 없음: 기능/입문/실행 문서 변경 | 실제 Journal 승인·전기와 운영 PostgreSQL 미검증 | 동일 리뷰어 문서·명령 대조 |
| Q4 | PASS | Reader intent comments28,43,112; test fixture/reversal/retry comments36,226,277,373 | 해당 없음: 비자명 SQL·테스트 로직 변경 | 고아 원전표 연결은 별도 대사 필요 | 동일 리뷰어 확인 |

### GH-779 Draft publication

- Published [Draft PR #789](https://github.com/skyg547/account/pull/789), OPEN/DRAFT, `Refs #779`, from `agent/779-fx-carrying-value` to `main`. Verified implementation commit: `b6d2a9ff8df0ebe8b79c9e51b946c03d1fc2cf8b`. Issue #779 remains OPEN / `status:needs-review`; worktree retained.
- Initial implementation-head GitHub Actions did not start because GitHub annotations report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36027412303), [Harness Validation](https://github.com/skyg547/account/actions/runs/36027412183) and [Merge Guard](https://github.com/skyg547/account/actions/runs/36027412321) fail before job execution. These are not hosted code/test results. No billing configuration was accessed or changed.
- Next owner: repository account owner to resolve the hosted Actions gate, then human reviewer to check current-head CI and the documented PostgreSQL/operational limits. This publication-only append preserves verified production/test contents. Ready, merge, Issue close, deployment and branch/worktree deletion were not performed.


## 2026-09-25 — GH-780 intake

- Goal: explicit dated monetary-account eligibility on source and service inputs; exclude historical revenue/expense and nonmonetary historical assets; fail closed for insufficient policy. Preserve signed normal/abnormal balances, prior valuation/reversal carrying amounts, lineage and existing financial controls.
- Base `4e20273c661edde3ab901c5e77f59e1ee5d6d8f2`, branch `agent/780-fx-valuation-eligibility`, external worktree `/tmp/account-780-fx-valuation-eligibility`. Primary checkout's unrelated changes are preserved.
- Planned implementation: Closing-owned effective-dated policy; core validation and adapter filtering; complete two-sided journal regression; source/direct-service bypass tests, missing/overlapping/date-boundary policy tests, existing FX history suite. No shared contract/schema changes or inferred nonmonetary exceptions.
- Audited `FxValuationService.java` at `a97d10ab6efc2570a88f83d630242cd748f8be57` equals intake source byte-for-byte (SHA256 `2ab7ff861bd458fe06859173f1ddd446e8ef1cd6d5385427c74964cc41a7ae2d`); pre-change regression against this file is evidence for the audited behavior.
- Verification plan: focused RED/GREEN, required whole-module `./gradlew :closing:test`, affected Spring wiring/packaging, independent read-only review and scoped diff/conflict checks. Production data, live PostgreSQL, distributed faults and production load remain outside this execution.
- Rollback: reviewed Closing-only revert; suspend affected FX runs while policy is unresolved and separately reconcile any previously posted ineligible adjustments. Parent owns publication; Ready, merge, Issue close, deployment and cleanup are separate gates.

### GH-780 audited-behavior regression RED

- Before production edits, `FxValuationEligibilityRegressionTest.completeCashRevenueJournalMustProduceOnlyCashGain` passed both signed legs of a synthetic EUR sale (cash debit100 / revenue credit100, reporting amounts110, rate1.2) to the actual service. It failed: expected one journal, received cash gain10 **and revenue loss10**, reproducing net-gain cancellation.
- Focused core run:1 test,1 assertion failure,0 errors/skips; Gradle exit1,27s. Test XML timestamp UTC2026-09-24T16:34:47. Preserved test, audited source, log and XML in `/tmp/issue-780-red/`; no infrastructure/compile failure is being represented as financial RED.
- The tested service's SHA256 matches the audited commit as recorded above. This proves the relevant audited service behavior; it is not a claim that the entire old repository test suite was rerun.

### GH-780 implementation and full verification

- Implemented `FxValuationPolicy` with explicit account/date/treatment rules and overlap validation; `FxValuationEligibilityResolver` checks dated Master identity/classification. `MONETARY` accepts canonical/legacy asset and liability classifications without fixed-asset contradiction; `HISTORICAL_COST` excludes classified historical items. Missing/unknown/conflicting policy fails closed. No generic exceptions or cross-module contract/schema changes.
- The source uses the same core gate for partition discovery and direct Cursor reads. It counts skipped physical rows for restart and reuses only the immediately preceding account decision. Global malformed FX lineage remains fatal before filtering. The service independently validates before rate lookup/zero-difference shortcuts, while signed amounts, rounding, DRAFT default and #779 carrying-value logic remain intact.
- Batch monolith/local imports and dev construction supply the resolver. Explicit policy configuration and deployment/retry limits are documented in README, process-flow, schema, local-run and beginner-guide.
- Writer focused commands: `./gradlew :closing:core:test --tests '*FxValuation*' --offline --console=plain --max-workers=1 --no-daemon` passed41; `./gradlew :closing:batch:test --tests '*FxValuation*' --tests '*JournalFxValuationBalanceSourceTest' --offline --console=plain --max-workers=1 --no-daemon` passed46. These87 are subsets of the full suite. Logs `/tmp/issue-780-red/{core-green,batch-green}.log`.
- Required exact `./gradlew :closing:test`: **BUILD SUCCESSFUL**, exit0,1m21s. Core236/API127/Batch63 = **426 tests /50 suites; failures/errors/skips0**. All three child test tasks executed; source-free parent correctly reports NO-SOURCE. Includes actual Spring API/Batch context, schema/profile and Cursor lifecycle tests. JDK17 from `/home/ho/.jdks/jdk-17.0.20.1+1`; `GRADLE_OPTS=-Dorg.gradle.workers.max=2` bounds workers without editing build files.
- `./gradlew :closing:api:bootJar :closing:batch:bootJar --offline --console=plain --max-workers=1`: PASS, exit0,14s. Both boot JARs produced. This is packaging evidence, not deployed Journal or PostgreSQL execution.
- Full log `/tmp/account-780-closing-full.log`; packaging log `/tmp/account-780-packaging.log`; archived XML/summary and14 frozen Java hashes in `/tmp/account-780-full-evidence/`. New regression coverage adds48 tests to the base378; #779 financial assertions remain intact.
- Static gates:22 approved Java/Markdown paths, all within `closing/**`; tracked diff and new-file whitespace, exact changed-file conflict scan and unmerged index checks pass. No shared harness or other module was edited.
- Limits: no live PostgreSQL SQL/EXPLAIN/load, production data, actual deployed Journal posting or distributed-fault test. Dated Master queries remain per account and service call; no new bulk port/read model or global policy/source snapshot is claimed. Policy list defaults empty and must be supplied before FX execution. Previously posted ineligible adjustments need separate reconciliation/correction.
- Rollback: reviewed Closing-only revert with affected FX execution suspended; preserve posted journal lineage and module histories. No migration or automatic data repair. Independent read-only replay and final Q1–Q4 are the next gate before authorized Draft publication.

### GH-780 independent review and publication gate

- Independent read-only `/root/closing_independent_review` (requested GPT-6 Astra/xhigh) reviewed actual source/test/docs/RED/full evidence and found no P0–P3. Q1–Q4 PASS.
- Own command: `./gradlew :closing:core:test --tests '*FxValuation*' :closing:batch:test --tests '*FxValuation*' --offline --rerun-tasks --max-workers=1 --no-daemon --console=plain`, JDK17. PASS exit0,35s,24 tasks all executed; Core41/5suites + Batch46/4suites =87, failure/error/skip0. XML UTC timestamps16:41:49–16:42:01 (2026-09-24). These87 are included in full426, not additional cases.
- Reviewer and parent both compared all14 Java hashes with the full-run freeze: unchanged. Independent XML/summary archived `/tmp/account-780-independent-evidence/`; full426 archive remains untouched. Parent main checkout's dirty status matches intake; refreshed `origin/main` still equals the task base.
- Parent proceeds with authorized commit/push and OPEN Draft PR using `Refs #780`. Remote publication/current-head hosted CI are recorded separately; Ready/merge/Issue close/deployment/cleanup are not performed.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `closing/core/src/main/java/com/ho/account/closing/domain/fx/FxValuationPolicy.java:21`, `closing/core/src/main/java/com/ho/account/closing/application/service/FxValuationEligibilityResolver.java:16`; full426 + independent87 PASS | 해당 없음: core 정책과 source 경계 변경 | 실제 PostgreSQL·대량 조회 비용 미검증 | `/root/closing_independent_review`, P0–P3 없음 |
| Q2 | PASS | `closing/docs/process-flow.md:212`; policy/date/sign/restart flow and complete-journal tests | 해당 없음: 평가 입력·실패·재실행 흐름 변경 | 영속 policy snapshot·동시 원장 변경 잠금은 미제공 | 동일 리뷰어 구현·설명 대조 |
| Q3 | PASS | `closing/docs/local-run.md:184`, `closing/docs/schema.md:112`, `closing/docs/beginner-guide.md:48`; Binder + full426 + bootJar2 PASS | 해당 없음: 실행 전제·설정·입문 문서 변경 | 승인된 계정 정책 제공, 기존 오평가 별도 대사 필요 | 동일 리뷰어 검증·문서 대조 |
| Q4 | PASS | `closing/core/src/main/java/com/ho/account/closing/domain/fx/FxValuationPolicy.java:33`, `closing/core/src/main/java/com/ho/account/closing/application/service/FxValuationService.java:57`; `closing/batch/src/main/java/com/ho/account/closing/batch/adapter/out/JournalFxValuationBalanceSource.java:237` | 해당 없음: 유효기간·분류·물리행 checkpoint 의도 주석 대상 | 배포 서비스 간 장애·운영 부하 후속 검증 | 동일 리뷰어 확인 |

### GH-780 Draft publication

- Published [Draft PR #790](https://github.com/skyg547/account/pull/790), OPEN/DRAFT, `Refs #780`, branch `agent/780-fx-valuation-eligibility` to `main`. Verified implementation commit `3fd6568d2a6b73f982c3c70d428288cdac796364`. Issue #780 remains OPEN / `status:needs-review`; worktree retained at `/tmp/account-780-fx-valuation-eligibility`.
- Initial implementation-head hosted checks did not execute: GitHub annotations explicitly report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36029393022), [Harness Validation](https://github.com/skyg547/account/actions/runs/36029393098), [Merge Guard](https://github.com/skyg547/account/actions/runs/36029392991). These failures are infrastructure gating, not executed code/test results. No billing settings were accessed or changed.
- This publication append changes only the three module records; all14 verified Java files remain frozen. The PR body contains426-test verification, independent87, Q1–Q4 and authority separation. Ready, merge, Issue close, deployment and resource deletion were not performed.
- Next owner: repository account owner to resolve hosted Actions availability, then human reviewer to verify current-head CI and approved effective-dated policies/operational prerequisites. Rollback and residual PostgreSQL/load/data-reconciliation gates remain as documented above.


## 2026-09-25 — GH-781 ECL monetary units

- Issue [#781](https://github.com/skyg547/account/issues/781), CL17; claim [5818387525](https://github.com/skyg547/account/issues/781#issuecomment-5818387525). Branch `agent/781-ecl-currency-units`, worktree `/tmp/account-781-ecl-currency-units`, base `origin/main@0709b1154a2333587e85d19335b548fe68028086`.
- Scope is `closing/**` only. The user's prohibition on common harness edits takes precedence over default record paths; only this module's three harness records are updated. No other module, shared contract, schema or production data was changed.
- Ownership/model: GPT-6 Astra / xhigh. `/root/closing_implementation` owns 12 production Java files; `/root/closing_tests` owns seven test files; `/root/closing_independent_review` is read-only. Parent owns five module guides, these records and Git/GitHub publication.
- Result: ECL reads explicit transaction/functional credit balances from posted journals, including attributed FX adjustments and recursive reversals. Targets remain in their source currency. Dated positive representable rates replace the hardcoded unit rate. Existing foreign carrying value must reconcile at that rate; otherwise eligible FX valuation must be approved/posted first. Same-currency inconsistencies require source reconciliation. Cumulative target base deltas reconcile both units without an extra cent from separately rounding the transaction delta.
- Controls: all groups validate before the first remote write; default DRAFT, opt-in auto-post, one run/model/entity, date/batch requirements, mappings, rounding and deterministic slip/lineage are preserved. Currency/rate in the compared description rejects changed-rate retries even if rounded line amounts are identical. Legacy drafts require reconciliation.
- RED: two real-H2/source/outgoing-adapter cases failed on the original wrong103900 release and base100. Six key production blobs at the tested base are identical to audited `a97d10ab6efc2570a88f83d630242cd748f8be57`. Original Java/XML and source equality evidence: `/tmp/account-781-red-evidence/`.
- Focused writer validation: 79 unique cases individually green across two runs. One Mockito fixture restubbing NPE was corrected; no production failure remained. Independent static review requested three additional real-JDBC mixed run/model/entity cases; all three pass in final full verification.
- Required `./gradlew :closing:test` PASS: Core267/API127/Batch98 =492 tests, 53 suites, failures/errors/skips0. Initial whole-module run executed489 tests (1m19s); after adding the three batch tests, final exact command passed44s, reran Batch98 and reused verified unchanged Core/API. Complete XML, command logs and 19 Java hashes are archived at `/tmp/account-781-full-evidence/`.
- Packaging: `./gradlew :closing:api:bootJar :closing:batch:bootJar --offline --console=plain --max-workers=1 --no-daemon` PASS14s. Actual packaged Batch JAR started and exited0 with explicit local/H2/non-web/no-job settings in18.7s; business Job execution was not observed. See `batch-smoke-summary.json` and `batch-smoke.log` in the evidence directory.
- Independent replay/Q1–Q4 final confirmation and Draft publication are pending at this checkpoint; no hosted CI success is claimed.
- Limits: synthetic H2 with actual Closing adapters and Journal/Master Data contract doubles; no deployed Journal approval/posting, live PostgreSQL plans/load, distributed faults or concurrent source-write verification. Per-group recursive history aggregation requires operational capacity validation and a maintained reconciled dual-currency read model before very high volumes. Local preflight does not provide a distributed transaction or source snapshot lock. A manual persisted-rate alteration with an unchanged description is outside the shared query's detectable fields.
- Rollback: suspend affected ECL jobs and use a reviewed scoped revert, preserving posted journals, business data and FX lineage. No migration/data reset. Next owner is the human Draft/current-head CI reviewer and authorized FX/accounting operator. Ready, merge, Issue close, deployment and branch/worktree deletion remain separate gates.

Changed implementation/test/guide paths (the three parent-owned module records are additional):

- `closing/README.md`
- `closing/batch/src/main/java/com/ho/account/closing/batch/adapter/out/GlAllowanceBalanceLookupAdapter.java`
- `closing/batch/src/main/java/com/ho/account/closing/batch/adapter/out/JournalFxValuationBalanceSource.java`
- `closing/batch/src/main/java/com/ho/account/closing/batch/adapter/out/JournalLedgerClosingJournalEntryAdapter.java`
- `closing/batch/src/main/java/com/ho/account/closing/batch/config/ClosingBatchDevConfiguration.java`
- `closing/batch/src/test/java/com/ho/account/closing/batch/adapter/out/EclJournalFixture.java`
- `closing/batch/src/test/java/com/ho/account/closing/batch/adapter/out/EclProvisionCurrencyUnitsTest.java`
- `closing/batch/src/test/java/com/ho/account/closing/batch/adapter/out/EclProvisionPersistedRetryTest.java`
- `closing/batch/src/test/java/com/ho/account/closing/batch/adapter/out/GlAllowanceBalanceLookupAdapterTest.java`
- `closing/core/src/main/java/com/ho/account/closing/application/port/out/AllowanceBalance.java`
- `closing/core/src/main/java/com/ho/account/closing/application/port/out/AllowanceBalanceLookupPort.java`
- `closing/core/src/main/java/com/ho/account/closing/application/port/out/ClosingJournalEntryCommand.java`
- `closing/core/src/main/java/com/ho/account/closing/application/port/out/EclAllowanceResultPort.java`
- `closing/core/src/main/java/com/ho/account/closing/application/service/ClosingAccountingProperties.java`
- `closing/core/src/main/java/com/ho/account/closing/application/service/EclProvisionService.java`
- `closing/core/src/main/java/com/ho/account/closing/domain/ClosingMonetaryPrecision.java`
- `closing/core/src/main/java/com/ho/account/closing/domain/EclAllowanceSummary.java`
- `closing/core/src/test/java/com/ho/account/closing/application/port/out/AllowanceBalanceTest.java`
- `closing/core/src/test/java/com/ho/account/closing/application/service/EclProvisionServiceTest.java`
- `closing/core/src/test/java/com/ho/account/closing/application/service/EclProvisionValidationTest.java`
- `closing/docs/beginner-guide.md`
- `closing/docs/local-run.md`
- `closing/docs/process-flow.md`
- `closing/docs/schema.md`

### GH-781 independent review complete

- Read-only `/root/closing_independent_review` independently reran82 focused tests (Core45/Batch37), six suites, failures/errors/skips0, exit0/35s; all24 tasks forced with `--rerun-tasks --offline --console=plain --max-workers=1 --no-daemon`. Command filters were Core `*EclProvision*`, `*AllowanceBalanceTest` and Batch `*EclProvision*`, `*GlAllowanceBalanceLookupAdapterTest`. Log: `/tmp/account-781-independent-review-tests.log`.
- Reviewer independently counted the archived full492/53 suites and verified19 Java hashes before/after replay. No remaining P0–P3; Q1–Q4 PASS. Requested identity3 coverage gap is resolved.
- Parent exact-path checks PASS: all27 changed files stay inside Closing; whitespace/conflict/unmerged checks clean. Implementation and tests remain frozen. Parent now performs the authorized commit/push/Draft publication; subsequent remote facts are recorded separately.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `closing/core/src/main/java/com/ho/account/closing/application/service/EclProvisionService.java:83`; `closing/core/src/main/java/com/ho/account/closing/domain/ClosingMonetaryPrecision.java:15`; full492/independent82 PASS | 해당 없음: 계산·계약 변경 | PostgreSQL query plans/load remain | `/root/closing_independent_review`, no P0–P3 |
| Q2 | PASS | `closing/docs/process-flow.md:309`; dated rate, prior posted FX, dual-unit reconciliation and restart behavior match implementation | 해당 없음: 흐름 변경 | No distributed atomicity/source snapshot locking | Independent implementation/docs comparison PASS |
| Q3 | PASS | `closing/docs/local-run.md:243`; `closing/docs/beginner-guide.md:37`; full492, packaging and actual local JAR smoke evidence | 해당 없음: 기능 안내 변경 | Deployed Journal/production recovery not verified | Independent docs/evidence review PASS |
| Q4 | PASS | `closing/batch/src/main/java/com/ho/account/closing/batch/adapter/out/JournalFxValuationBalanceSource.java:149`; service preflight/cumulative rounding/rate fingerprint comments | 해당 없음: 비자명 로직 변경 | No additional code gate; retain operational limits | Independent intent/implementation comparison PASS |

### GH-781 Draft publication

- Published [Draft PR #791](https://github.com/skyg547/account/pull/791), OPEN/DRAFT, `Refs #781`, from `agent/781-ecl-currency-units` to `main`. Verified implementation commit `1add69bb1b083fc9c5074feba4809fbb77975965`; initial GitHub mergeability is MERGEABLE. Issue #781 remains OPEN / `status:needs-review`; isolated worktree is retained.
- Initial implementation-head hosted checks did not execute: GitHub check annotations report failed recent account payments or a spending-limit restriction. [Module Validation](https://github.com/skyg547/account/actions/runs/36031718246), [Harness Validation](https://github.com/skyg547/account/actions/runs/36031718247), [Merge Guard](https://github.com/skyg547/account/actions/runs/36031718278). These failures are infrastructure gating, not executed code/test results. No billing settings were accessed or changed.
- This publication append changes only the three module records; all19 verified Java files remain frozen. The PR body includes full492, independent82, package/startup evidence, Q1–Q4, rollback, remaining risks and authority separation. Ready, merge, Issue close, deployment and resource deletion were not performed.
- Next owner: repository account owner to restore hosted Actions availability, then human reviewer to verify current-head CI and FX/accounting operational prerequisites. Actual PostgreSQL/load/distributed behavior and existing-data reconciliation remain separate gates.

## 2026-09-28 — GH-775 annual close stale-draft remediation

- Contract: [Issue #775](https://github.com/skyg547/account/issues/775), `agent/775-annual-close-stale-draft`, `/tmp/account-775-annual-close-stale-draft`, base/current main `f128a5dd3cf628f1d5226ae3c5db0ad264021e65`, allowlist `closing/**`, Draft delivery with `Refs #775`.
- Architecture: `AnnualClosingService` remains the core application coordinator. It canonicalizes sorted posted source summaries/details into a SHA-256 snapshot, uses an 86-character `year|retainedHash|snapshotHash` lineage and 20-character snapshot-derived slip, validates every annual header/detail/status/lineage, and calculates `required closing - cumulative POSTED annual closing` by signed `BigDecimal` base amount. No schema, shared contract, API shape or Batch flow changed.
- Retry policy: only a single exact `DRAFT` for the current snapshot and residual is reusable. Stale, malformed, legacy-draft, unsupported-status or duplicate pending candidates fail closed. Fully validated legacy year-only `POSTED` entries remain cumulative history. A posted close plus new posted activity creates only the residual delta; posting that delta makes the same input a no-op.
- Classification: Journal-provided supported categories are used when present. Missing values resolve through `MasterDataQueryPort.findAccountSubjectAt(account,date)` with a per-run account/date cache. Dev remote composition adds `HttpClosingMasterDataQueryAdapter`; invalid, missing, mismatched, redirected or failed responses do not become permission to close.
- Regression evidence: untouched audited behavior failed 13 of 68 focused tests on stale reuse/delta/content/category cases while existing posted-only/base-amount controls remained green. Final independent replay passed129/129: Annual25, Journal HTTP54, Master HTTP17, admission27 and slip factory6.
- Whole module: exact `./gradlew :closing:test` PASS in1m26s; Core315/API127/Batch98 =540 tests across54 suites, failures0/errors0/skipped0. `git diff --check`, conflict-marker scan, unmerged-path check and closing-only scope check pass.
- Baseline compatibility: the first whole-module run exposed27 pre-existing admission fixture failures caused by direct DRAFT-to-APPROVED transitions. The test-only fixture now requests approval first and expects the canonical lowercase system actor; its27 assertions pass without production behavior changes.
- Review: the first read-only review found the introduced Journal `accountCategory` contract assumption. The writer corrected it with the dated Master fallback and provider-shape tests. Final reviewer found no scoped P0–P3, mapped all acceptance criteria PASS and set the scoped delivery gate PASS.

Changed implementation/test/guide paths; the three module-local records are additional:

- `closing/README.md`
- `closing/api/src/test/java/com/ho/account/closing/ClosingDevRuntimeContextTest.java`
- `closing/core/src/main/java/com/ho/account/closing/application/service/AnnualClosingService.java`
- `closing/core/src/main/java/com/ho/account/closing/application/service/ClosingSlipNoFactory.java`
- `closing/core/src/main/java/com/ho/account/closing/infrastructure/external/HttpClosingJournalAdapter.java`
- `closing/core/src/main/java/com/ho/account/closing/infrastructure/external/HttpClosingMasterDataQueryAdapter.java`
- `closing/core/src/test/java/com/ho/account/closing/application/service/AnnualClosingServiceTest.java`
- `closing/core/src/test/java/com/ho/account/closing/application/service/ClosingJournalAdmissionIntegrationTest.java`
- `closing/core/src/test/java/com/ho/account/closing/infrastructure/external/HttpClosingJournalAdapterTest.java`
- `closing/core/src/test/java/com/ho/account/closing/infrastructure/external/HttpClosingMasterDataQueryAdapterTest.java`
- `closing/docs/beginner-guide.md`
- `closing/docs/local-run.md`
- `closing/docs/process-flow.md`

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `AnnualClosingService.java:74,157,411`; focused129/full540 PASS | 해당 없음: 금융 계산·재실행 변경 | 실제 인증 원격 Journal/Master 및 동시 변경 검증 | `/root/issue775_review`, snapshot/sign/balance/status/duplicate 검토 PASS |
| Q2 | PASS | `AnnualClosingService` application 정책과 `HttpClosingMasterDataQueryAdapter` outbound 기술 분리; `process-flow.md:362` | 해당 없음: 구조·흐름 변경 | summary/detail 및 unique account/date N+1, 비원자 snapshot | 독립 구현/경계/문서 대조 PASS |
| Q3 | PASS | `README.md:16`, `beginner-guide.md:45`, `local-run.md:96`, `process-flow.md:362`; full540 PASS | 해당 없음: 기능 안내 변경 | 승인된 service-principal 계약 전 remote write HOLD | 독립 문서 사실성/한계 대조 PASS |
| Q4 | PASS | snapshot TODO/lineage 길이/redirect 차단 의도 주석; provider-order·failure tests | 해당 없음: 비자명 로직 변경 | 36-bit slip discriminator 충돌은 full lineage 검증으로 fail-closed | 독립 주석/구현 일치 확인 PASS |

Rollback: stop annual-close execution, preserve every posted/draft journal and lineage for reconciliation, then use a reviewed scoped revert. No migration or data reset is required. Existing old drafts must not be deleted automatically. Next owner after Draft publication is a human reviewer for current-head CI and the separate Journal service-authentication integration; Ready/merge/Issue close/deployment/cleanup are not authorized here.

### GH-775 Draft publication

- [Draft PR #806](https://github.com/skyg547/account/pull/806) is OPEN/DRAFT, `Refs #775`, from `agent/775-annual-close-stale-draft` to `main`; initial mergeability was MERGEABLE. Verified implementation commit: `1200064ad77dbb44925641d359a73924e4c212bc`. Issue #775 is OPEN / `status:needs-review`; worktree retained.
- The implementation-head [Module Validation](https://github.com/skyg547/account/actions/runs/36423379589), [Harness Validation](https://github.com/skyg547/account/actions/runs/36423379826), and [Merge Guard](https://github.com/skyg547/account/actions/runs/36423379805) jobs did not start. Each GitHub annotation identifies failed recent account payments or a spending-limit restriction. No hosted code/test pass is claimed, and no billing setting was accessed or changed.
- Publication changes after the verified implementation commit are limited to `agent-status.md`, `worklog.md`, and `handoff.md`. Next owner: repository account owner for Actions availability, then a human reviewer for current-head CI and the documented Journal authentication/operational gates. Ready, merge, Issue close, deployment, and cleanup remain unperformed.

## 2026-09-28 — GH-776 retained-earnings destination control

- Contract: [Issue #776](https://github.com/skyg547/account/issues/776), `agent/776-retained-earnings-control`, `/tmp/account-776-retained-earnings-control`, allowlist `closing/**`, Draft delivery with `Refs #776`. The branch began at `origin/main@f128a5dd3cf628f1d5226ae3c5db0ad264021e65` and was fast-forwarded to reviewed #775 Draft head `a1b108f7f5e4c18a9e384139c5e93c7b3b151d68`; #776 is therefore a stacked Draft dependency on PR #806.
- API and architecture: the annual endpoint accepts only JSON `year`; account and entity selectors are rejected as unknown fields before use-case delegation. `RetainedEarningsMappingPort` keeps the core independent of Spring configuration, while `ConfiguredRetainedEarningsMappingAdapter` resolves one runtime legal entity and one exact fiscal-year rule.
- Control input: every configured rule requires account code, `postable: true`, nonblank `approvedBy` and `changeReference`. There is no usable default and all rows are checked for invalid/duplicate years and missing evidence. Configuration is snapshotted at bean construction, so changes require a drained annual-close path and full runtime restart.
- Financial gate: before Journal reads, the configured account is looked up on Dec31 and must return the exact code, `EQUITY` category and CREDIT normal balance. Asset, liability, revenue, expense, unknown/missing, mismatched-code, blank-category and debit-normal equity cases fail before any Journal read/write. Profit, loss and zero-net-income paths all cross this gate.
- Audit/retry: the normalized entity/year/account/postable/approver/change-reference identity joins the #775 source content in `ANNUAL_SOURCE_SNAPSHOT_V2`. A metadata change makes a pending draft stale. Fully validated posted balances are still subtracted before a new write, so an already complete close does not create a financial delta merely because audit metadata changed. Existing #775 full-header/line/status/lineage, cumulative delta, ordering, BigDecimal and legacy-posted controls remain.
- RED: on the stacked #775 source, the P17-style regression supplied dated cash account `10100`/`ASSETS` and posted revenue 1,000. The test compiled and failed because `createDraftEntry` was reached; 1 test, 1 intended failure. This proves the defect rather than a fixture/compiler failure.
- Focused GREEN: Core60 (Annual29, destination9, configuration16, slip6) and API9 (controller7, dev context2), failures/errors/skips0. Independent reviewer forced the same69 relevant tests with offline/rerun/single-worker settings and obtained failures/errors/skips0.
- Required full verification: exact `./gradlew :closing:test` PASS in1m25s. XML totals Core344/API132/Batch98 =574 tests across25/17/14 =56 suites, failures0/errors0/skipped0. `./gradlew :closing:api:bootJar :closing:batch:bootJar --console=plain --max-workers=1 --no-daemon` PASS in16s. Scope, whitespace, conflict-marker and unmerged-index checks pass.
- Review: independent read-only `/root/review_776` found no P0–P3 and marked Q1–Q4 PASS. Draft publication is safe only against `agent/775-annual-close-stale-draft`; #806 must merge before retargeting/rebasing #776 to current `main` and repeating current-head verification.

Changed implementation/test/guide paths; the three parent-owned module records are additional:

- `closing/README.md`
- `closing/api/src/main/java/com/ho/account/closing/dto/AnnualClosingRequestDto.java`
- `closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java`
- `closing/api/src/test/java/com/ho/account/closing/ClosingDevRuntimeContextTest.java`
- `closing/api/src/test/java/com/ho/account/closing/web/ClosingControllerTest.java`
- `closing/core/src/main/java/com/ho/account/closing/application/port/in/AnnualClosingUseCase.java`
- `closing/core/src/main/java/com/ho/account/closing/application/port/out/RetainedEarningsMappingPort.java`
- `closing/core/src/main/java/com/ho/account/closing/application/service/AnnualClosingService.java`
- `closing/core/src/main/java/com/ho/account/closing/domain/ApprovedRetainedEarningsMapping.java`
- `closing/core/src/main/java/com/ho/account/closing/infrastructure/config/AnnualClosingConfigurationProperties.java`
- `closing/core/src/main/java/com/ho/account/closing/infrastructure/config/ConfiguredRetainedEarningsMappingAdapter.java`
- `closing/core/src/test/java/com/ho/account/closing/application/service/AnnualClosingDestinationControlRegressionTest.java`
- `closing/core/src/test/java/com/ho/account/closing/application/service/AnnualClosingServiceTest.java`
- `closing/core/src/test/java/com/ho/account/closing/infrastructure/config/ConfiguredRetainedEarningsMappingAdapterTest.java`
- `closing/docs/beginner-guide.md`
- `closing/docs/local-run.md`
- `closing/docs/process-flow.md`
- `closing/docs/schema.md`

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `AnnualClosingService.java:80`, `ApprovedRetainedEarningsMapping.java:10`, `ConfiguredRetainedEarningsMappingAdapter.java:20`; full574/independent69 PASS | 해당 없음: 금융 목적지·API·설정 경계 변경 | 실제 provider 동시성·원격 쓰기 미검증 | `/root/review_776`, P0–P3 없음 |
| Q2 | PASS | `process-flow.md:362`; year→mapping→Dec31 Master→snapshot→draft/delta와 실패·재시작 흐름 | 해당 없음: 입력·처리·출력·재실행 변경 | provider-side 원자 snapshot 없음 | 독립 구현/문서/테스트 대조 PASS |
| Q3 | PASS | `README.md:16`, `beginner-guide.md:45`, `local-run.md:96`, `schema.md:46`; 합성 YAML과 운영 제약 | 해당 없음: API·설정·운영 계약 변경 | 배포 설정 review와 실제 원격 write는 운영 게이트 | 독립 문서 사실성/제약 대조 PASS |
| Q4 | PASS | `ApprovedRetainedEarningsMapping.java:3-7`, `ConfiguredRetainedEarningsMappingAdapter.java:70-76`, `AnnualClosingService.java:211-218`, `AnnualClosingService.java:579-580`, `AnnualClosingRequestDto.java:21-27`; postability attestation, 단일 법인, 설정 identity fingerprint, Master 한계, unknown-field fail-closed 의도 주석 | 해당 없음: 비자명 통제 로직 변경 | upstream 계약이 확장되면 주석/정책 갱신 필요 | 독립 의도/구현 일치 확인 |

잔여 테스트 공백: 허용되는 최대 50자 계정 코드의 lineage가 정적으로 86자로 `VARCHAR(100)` 이내임은 독립 리뷰에서 확인했지만, 최대 길이를 직접 assertion하는 회귀 테스트는 없다.

Rollback: 먼저 annual-close endpoint를 중지하고 기존 DRAFT/POSTED 전표와 lineage를 보존·대사한다. 검토된 #776 delta만 revert하고 설정은 코드 rollback 뒤 제거한다. 이전 request-selected account 계약은 endpoint가 비활성화된 동안만 복원할 수 있으며, Journal 데이터 삭제·재작성이나 migration rollback은 하지 않는다. 다음 owner는 stacked Draft의 human reviewer와 #806 이후 current-main 통합 검증자다.

## 2026-09-28 — GH-777 evidence-backed financial run APIs

- Issue [#777](https://github.com/skyg547/account/issues/777), CL07/P1. Branch `agent/777-financial-run-evidence`, isolated worktree `/tmp/account-777-financial-run-evidence`, base `f128a5dd3cf628f1d5226ae3c5db0ad264021e65`. Scope is `closing/**`; no shared harness, other module, migration or production data changed.
- GPT-5.6 Sol/high agents used disjoint API/core/SQL/Batch/test/document ownership. Parent Integrator owns these three records and Git/GitHub publication. Independent `/root/closing_777_independent_review` was read-only.
- RED before production edits: `ClosingServiceTest` added missing-FX-evidence and missing-finalized-ECL-summary cases. Both failed on the old fixed-rule path because it returned normally and created the configured draft: **2 tests, 2 assertion failures, 0 errors/skips**. This is executed behavioral evidence on the task base, not a claim of a full audited-revision checkout.
- The initial whole-module baseline also exposed an unrelated stale maker/checker test fixture: 27 `ClosingJournalAdmissionIntegrationTest` cases called `approve` directly from DRAFT after the Journal contract began requiring `submitForApproval`. The fixture was corrected without changing production behavior.
- API valuation/provision now accepts only `FX_RATE`/`ECL`, creates RUNNING history, delegates the fiscal-period end date and persisted run ID to `FinancialClosingCalculation`, and records `COMPLETED`, `PENDING_APPROVAL` or `FAILED`. Zero or multiple journals keep the legacy scalar journal ID null; a single result records its ID.
- FX API streams posted-journal evidence once, applies the same core preparation policy as Batch, retains only a capped immutable command plan, and posts exactly that plan. FX evidence is bounded by cap+1 JDBC rows and a 60-second query timeout. ECL source aggregation is bounded conservatively by the command cap plus one and the same timeout before per-group ledger/rate reads. Missing rate, eligibility, lineage, mapping or finalized summary fails before the first Journal call.
- Batch keeps its partition ranges, stateful cursor, chunk/checkpoint and concurrency controls. A write-free whole-source FX validation step precedes posting and uses `allowStartIfComplete(true)` so a same-JobInstance restart validates again. API and Batch share core `FxValuationService`/`EclProvisionService` and `PostedJournalContributionQuery`; core has no Spring Batch dependency and API has no Batch dependency.
- The legacy fixed-rule amount contract, valuation rule map and configured-amount tests were removed. ECL retains account fallback mapping only. No separate manual fixed-amount workflow remains.
- Independent review found and returned two integration defects during the gate: remote Journal draft calls omitted required maker headers, and the new bounded ECL adapter's overloaded constructors broke the `!dev` Batch context when class-imported. The final diff sends `X-Auth-User`/`ROLE_JOURNAL_MAKER`, asserts them through a loopback server, and constructs the monolith ECL adapter with an explicit `JdbcTemplate` bean. Review also requested and received bounded ECL source materialization.
- Required JDK17 `./gradlew :closing:test`: **BUILD SUCCESSFUL**, API127 + Batch100 + Core285 = **512 tests**, failures/errors/skips0. Focused auth/cap tests and the local Batch context passed. `./gradlew :closing:api:bootJar :closing:batch:bootJar --console=plain --max-workers=1 --no-daemon`: PASS; both executable JARs produced.
- Static gates: all changed/new paths under `closing/**`; `git diff --check`, conflict-marker scan, unmerged index, no migration diff, no API→Batch and no core→Spring Batch dependency all pass. Legacy fixed-rule type/config/amount keys have no production, test or functional-guide match outside build output.
- Rollback: suspend affected financial runs and revert the reviewed Closing-only change. No schema rollback is needed. Remote drafts/posts already created must be reconciled by deterministic slip/lineage and approved correction/reversal; never delete financial history as code rollback.
- Residual gates: no live PostgreSQL plan/load, production data, deployed read-only source connection, distributed Journal fault injection or concurrent source mutation test. Batch validation/posting requires an operationally frozen ledger/rate/policy set and does not claim an immutable distributed snapshot. API request idempotency key, multi-journal history lookup, ECL zero-portfolio marker and high-volume per-group allowance reads remain separate work.

### GH-777 independent review and quality contract

- Read-only reviewer inspected the latest implementation/tests/docs, confirmed the two returned findings were fixed, reconciled the final 512-test XML and returned **APPROVE** with no open P0–P3.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `FinancialClosingCalculationService`, core FX/ECL services, source/external adapters and Batch config have separate responsibilities; full512 PASS | 해당 없음: 금융 실행 책임 변경 | live PostgreSQL/deployed source 미검증 | `/root/closing_777_independent_review`, open P0–P3 없음 |
| Q2 | PASS | `process-flow.md`; full preflight→immutable commands→Journal, history failure and restart flow tests | 해당 없음: 입력·실패·재시작 흐름 변경 | 분산 snapshot/atomicity 없음 | 동일 리뷰어 구현·문서 대조 |
| Q3 | PASS | Closing README, beginner guide, local-run, process-flow, schema; full512 + bootJar2 PASS | 해당 없음: 사용자 실행 의미 변경 | 운영 source/profile/권한 설정 별도 검증 | 동일 리뷰어 확인 |
| Q4 | PASS | cap+1 overflow, remote effects, validation rerun, callback/cursor and lineage comments; focused auth/cap/restart tests | 해당 없음: 비자명 금융·재시작 로직 변경 | 대량 성능·분산 장애 후속 게이트 | 동일 리뷰어 확인 |

- Publication gate: authorized Closing-only commit/push/Draft PR with `Refs #777` follows. Ready, merge, Issue close, deployment and branch/worktree deletion are not authorized by this gate.

### GH-777 Draft publication

- Published [Draft PR #808](https://github.com/skyg547/account/pull/808) with `Refs #777` from `agent/777-financial-run-evidence` to `main`; it was initially MERGEABLE and was later CLOSED after the independent P1 Journal review. Initial implementation commit `8fa4d8fb`. Issue #777 remains OPEN; the isolated worktree is retained.
- The PR body contains the 512-test result, packaging/harness checks, Q1–Q4 evidence, rollback/remaining gates and explicit authority separation. Hosted [Module Validation](https://github.com/skyg547/account/actions/runs/36434457290), [Harness Validation](https://github.com/skyg547/account/actions/runs/36434457468) and [Merge Guard](https://github.com/skyg547/account/actions/runs/36434457280) did not start jobs: GitHub annotations report failed recent account payments or a spending-limit restriction. These are external infrastructure failures, not executed code/test results; no billing settings were accessed or changed.
- Ready, merge, Issue close, deployment and branch/worktree deletion were not performed. Next owner is a human current-head/CI reviewer, followed by authorized accounting/operations owners for the documented deployment gates.

## 2026-10-01 — GH-777 remote Journal control remediation and current-main integration

- Issue #777 / CL07/P1, `agent/777-financial-run-evidence`, `/tmp/account-777-financial-run-evidence`; original base `f128a5dd`, integrated `origin/main@b06e7de3`. Old Draft PR #808 was closed after an independent P1 finding: it called remote `/approve` and `/post` directly after DRAFT without maker request, distinct checker, poster role, or reliable uncertain-response recovery. This is a review correction of the same Issue, not an assertion that #808 passed.
- Resolution: the `dev` HTTP Journal adapter binds `account.closing.accounting.auto-post-adjustments` and rejects `true` before `createDraftEntry` sends any HTTP write. `approveAndPost` cannot send a remote request, including for an existing DRAFT/REQUESTED/APPROVED/POSTED slip. With the default `false`, it creates only a validated DRAFT for separate human Journal review. A returned draft must have a positive ID, DRAFT status and the requested deterministic slip. The shared Journal adapter checks the same response contract, including monolith/Batch providers, because Journal may deduplicate by lineage and return a different existing slip.
- Existing #777 calculation path remains: FX/ECL API and Batch use the same core financial policy and posted-journal evidence; missing FX rate/ECL summary fails before Journal; fixed configured amounts and the manual fixed workflow are absent. The API source caps, Batch validation/restart, period/approval controls and deterministic slip/lineage remain. This change does not add an automatic maker/checker/poster service-principal workflow.
- Merge conflicts with #771/#775/#776 were resolved in Closing only. The two financial request DTOs retain positive period IDs, `ClosingDevRuntimeContextTest` retains both read-only source and approved annual configuration, core HTTP detail/accountCategory tests and annual docs are retained, and stale direct-approval stubs were removed. [Conflict decisions](conflict-log.md) record the affected files. No shared harness, other authored module, migration or production data change is in the PR diff.
- Focused verification: core HTTP loopback contract 46/46; API HTTP adapter 20/20; shared Journal adapter 6/6. Final `./gradlew :closing:test :closing:api:bootJar :closing:batch:bootJar --offline --no-daemon --console=plain --max-workers=2` PASS in 1m30s, XML API141/Batch102/Core354 = 597 tests in58 suites, failures/errors/skips0; both executable JARs built. Journal authorization MVC `--rerun-tasks` PASS5/5; Node harness quality PASS32/32. Scope `git diff --name-only origin/main` is exclusively `closing/**`; `git diff --check`, staged diff check, marker scan and unmerged-index scan PASS.
- Independent read-only `/root/closing_777_review` inspected the final integrated diff, reran XML reconciliation and returned Draft-ready, no open scoped P0/P1 findings, Q1–Q4 PASS. It found and returned a mismatch-response slip P2, which the writer corrected. Same-slip concurrent lineage dedupe without provider-side atomic content comparison is an existing P2 Ready/deployment risk; a readback per journal would raise Batch I/O and still not make provider creation atomic. Journal-side idempotency/content control needs a separate authorized change.
- Rollback: stop affected FX/ECL runs, revert the reviewed Closing-only delta, preserve any existing DRAFT/POSTED Journal and local run history, and reconcile by deterministic slip/lineage. Do not delete remote financial entries or blindly replay an uncertain request. Remaining gates: trusted Journal service authentication and role-separated workflow, live PostgreSQL/scale, concurrent provider dedupe, distributed faults/source freeze and human current-head CI review. Ready, merge, Issue close, deployment and branch/worktree deletion are outside this Draft handoff.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `core/application/service/FinancialClosingCalculationService.java:32`; `core/infrastructure/external/JournalLedgerClosingJournalEntryAdapter.java:64`; full Closing597 PASS | 해당 없음: 금융 실행·어댑터 변경 | provider 원자 멱등성은 후속 | `/root/closing_777_review`, core/API/Batch 경계와 diff 확인 |
| Q2 | PASS | `docs/process-flow.md:520`; FX/ECL preflight→DRAFT/FAILED→대사, full597 및 Journal5 PASS | 해당 없음: 실패·재실행 흐름 변경 | 실 원격 장애와 원천 동시 변경 미검증 | 동일 리뷰어 코드·문서 대조 |
| Q3 | PASS | `docs/local-run.md:307,430`; `docs/beginner-guide.md:111`; API JSON·원격 auth 한계, API141 PASS | 해당 없음: 사용자 실행 의미 변경 | 운영 service principal/실 PostgreSQL 별도 | 동일 리뷰어 문서와 DTO 대조 |
| Q4 | PASS | `core/infrastructure/external/HttpClosingJournalAdapter.java:114,146`; `JournalLedgerClosingJournalEntryAdapter.java:65`; HTTP46/Batch adapter6 PASS | 해당 없음: 비자명 원격 경계 변경 | 같은 slip 동시 dedupe는 provider 통제 필요 | 동일 리뷰어 의도 주석과 회귀 대조 |

### GH-777 corrected Draft PR #819 publication

- Published [Draft PR #819](https://github.com/skyg547/account/pull/819), OPEN/DRAFT and initially MERGEABLE, `Refs #777`, from `agent/777-financial-run-evidence` to `main`. Initial publication head `5e4c428e`; Issue #777 remains OPEN and now carries `status:needs-review`. The isolated worktree remains available; PR #808 stays CLOSED.
- The PR body includes the final 597-test XML breakdown, bootJar and Journal/harness checks, Q1–Q4 independent review, rollback, residual gates and explicit implementation/review/Integrator/human authority separation.
- Initial hosted [Module Validation](https://github.com/skyg547/account/actions/runs/36764864113), [Harness Validation](https://github.com/skyg547/account/actions/runs/36764864173), and [Agent Merge Guard](https://github.com/skyg547/account/actions/runs/36764864117) did not start test jobs. Each run annotation reports failed recent account payments or spending-limit restriction. These are external infrastructure failures, not code/test results or hosted PASS; no billing settings were accessed or changed. The final record-only head will be checked again after push.
- Next owner: repository account owner to restore hosted Actions availability, then human current-head reviewer and authorized accounting/operations owners for the documented trusted Journal and production-data gates. Ready, merge, Issue close, deployment and resource deletion were not performed.

## 2026-10-09 — GH-891 ECL Stage 합계와 전기 목표 대사

- Issue #891 / CL19, `agent/891-ecl-stage-reconciliation`, `/tmp/account-891-ecl-stage-reconciliation`, fetched base `origin/main@9348966a26a1bf279d03c5ac982fc9c13c928d4c`. 변경과 기록은 `closing/**`에만 있다. 공용 하네스, 타 모듈, 공용 계약, migration, 운영 데이터는 변경하지 않았다.
- 입력→처리→출력: `EclAllowanceSummary` 직접 포트는 음수/null과 Stage 1+2+3 대 목표를 원래 `BigDecimal` 정밀도로 검사한다. JDBC는 그룹별 `invalid_source_rows`를 같은 bounded query로 집계해 서로 상쇄되는 행별 오류까지 거부한 뒤 정확한 목표·exposure·Stage 합계를 반환한다. Core는 전체 그룹을 검증·준비한 다음 전기 경계에서 그룹 목표를 한 번 소수 2자리로 반올림한다. 나중 그룹의 실패도 첫 Journal 쓰기 전에 전파된다.
- `allowance_summary`는 날짜별 재생성될 수 있다. 생성되는 전표의 두 Journal 상세 설명에는 정확한 그룹 목표·exposure·Stage 1/2/3 및 전체 run ID/model version을 보존하고, 짧은 헤더에는 통화쌍·환율을 보존한다. 전표 재시도는 기존 헤더·상세와 비교하므로 같은 반올림 금액이라도 원천 합계·run이 달라지면 초안을 재사용하지 않는다. 헤더120자·상세194자 제한은 Journal 역분개 접두사/사유 공간을 남기며 초과는 쓰기 전에 실패한다. 빈 결과는 실패, 명시적인 0 exposure/0 target/0 stages와 잔액0은 무전표다.
- 수정 전 RED: 직접 summary 3개 중2개 실패(불일치·0 exposure), JDBC 그룹 4개 중1개 실패(상쇄 오차), 기존 초안의 재생성 원천/증빙 회귀2개 실패, 큰 정상 전표의 실제 Journal 역분개1개 실패. 수정 후 이 회귀와 기존 FX/ECL 통제가 통과했다.
- 최종 전체 검증: `./gradlew :closing:test --rerun-tasks --offline --no-daemon --console=plain --max-workers=2` exit0, XML Core380/API159/Batch111 =650 tests, 0 failures/errors/skips, 29 executed tasks. 요청한 `./gradlew :closing:test` exit0 (직전 전체 실행의 child tasks up-to-date; aggregator `:closing:test`는 NO-SOURCE). 독립 읽기 전용 `/root/closing_review`의 최종 집중 재실행56/56 PASS; `git diff --check`, 변경 파일 conflict marker 및 범위 확인 PASS. 독립 리뷰 중 발견된 source evidence/P2와 reversal/P2는 원 작성자가 수정했고 최종 P0–P3 없음.
- Rollback: Closing 실행을 멈추고 이미 생성된 DRAFT/POSTED 전표와 lineage를 보존·대사한 뒤 이 Issue의 Closing 변경만 검토하여 되돌린다. 새 설명 형식의 초안은 자동 재사용을 가정하지 않는다. 스키마·데이터 rollback은 없다. 무전표 그룹에는 Journal 증빙이 없고, 기존 형식 초안은 수동 대사가 필요하다. 실 PostgreSQL 실행계획·부하, 분산 원천 재생성/전기 경쟁, 운영 데이터·배포는 미검증이며 Draft 이후 사람/운영 검증 게이트다.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `core/domain/EclAllowanceSummary.java:45`, `core/infrastructure/source/JdbcEclAllowanceResultAdapter.java:32`, `core/application/service/EclProvisionService.java:187`; 전체650·집중56 PASS | 해당 없음: 금융 코드 변경 | 실 PostgreSQL 계획·대량 입력 후속 | `/root/closing_review`, P0–P3 없음 |
| Q2 | PASS | `docs/process-flow.md:474`의 원천→대사→그룹→반올림→전표·재시도·역분개 설명; 실제 역분개 회귀 | 해당 없음: 핵심 흐름 변경 | 분산 snapshot/재시도 실환경 후속 | 동일 리뷰어 코드·문서 대조 |
| Q3 | PASS | `README.md`, `docs/beginner-guide.md`, `docs/schema.md:157`, `docs/local-run.md:408`; 전체650 PASS | 해당 없음: 기능 사용·실패 의미 변경 | 구형 초안·무전표 한계 운영 인계 | 동일 리뷰어 기능 문서 확인 |
| Q4 | PASS | `EclAllowanceSummary.java:49`, `JdbcEclAllowanceResultAdapter.java:103`, `EclProvisionService.java:187,215`의 정밀 대사·상쇄 방지·역분개 여유 의도; 집중56 PASS | 해당 없음: 비자명 로직 변경 | 설명 길이와 Journal 장문 사유 제한 | 동일 리뷰어 변경 주석 확인 |

- 권한 분리: `gpt-6-sol` / high 작성자는 Closing만 수정했고 독립 리뷰어는 읽기 전용이다. 부모 Integrator만 모듈 기록·commit·push·Draft PR을 수행한다. AI 리뷰는 사람/GitHub 승인을 대체하지 않는다. Draft에는 `Refs #891`을 사용하고 Ready·merge·Issue close·배포·branch/worktree 삭제는 수행하지 않는다.

### GH-891 Draft PR 게시

- 부모가 검증된 Closing-only head `2ee7ae3841fd17181ac97bc84175dbe143403da0`을 push하고 [Draft PR #906](https://github.com/skyg547/account/pull/906)을 `main` 대상으로 생성했다. 원격은 OPEN/DRAFT, 초기 MERGEABLE, 본문은 `Refs #891`, 650개 전체/56개 독립 검증, Q1–Q4, rollback, 남은 위험과 구현·리뷰·인간 권한 분리를 포함한다. Issue는 OPEN이다.
- 게시 직후 hosted module detection, merge guard, harness check는 PENDING이었다. 이는 CI 통과 증거가 아니다. 이 기록만 추가한 최종 head에 대해 사람 리뷰와 current-head CI를 다시 확인해야 한다. Ready, merge, Issue close, 배포, 자원 삭제는 수행하지 않았다.
