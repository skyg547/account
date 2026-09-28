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

- Published [Draft PR #808](https://github.com/skyg547/account/pull/808), OPEN/DRAFT and initially MERGEABLE, `Refs #777`, from `agent/777-financial-run-evidence` to `main`. Verified implementation commit `8fa4d8fb`. Issue #777 remains OPEN and is labeled `status:needs-review`; the isolated worktree is retained.
- The PR body contains the 512-test result, packaging/harness checks, Q1–Q4 evidence, rollback/remaining gates and explicit authority separation. Hosted [Module Validation](https://github.com/skyg547/account/actions/runs/36434457290), [Harness Validation](https://github.com/skyg547/account/actions/runs/36434457468) and [Merge Guard](https://github.com/skyg547/account/actions/runs/36434457280) did not start jobs: GitHub annotations report failed recent account payments or a spending-limit restriction. These are external infrastructure failures, not executed code/test results; no billing settings were accessed or changed.
- Ready, merge, Issue close, deployment and branch/worktree deletion were not performed. Next owner is a human current-head/CI reviewer, followed by authorized accounting/operations owners for the documented deployment gates.
