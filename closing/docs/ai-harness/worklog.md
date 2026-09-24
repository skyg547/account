# GH-772 worklog

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
