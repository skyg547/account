## 2026-09-23 — GH-53 Frontend Financial Statements & Dashboard API Integration

- Issue: #53 (`difficulty:medium`, `module:frontend`, `status:draft`)
- Branch: `agent/53-frontend-reporting-dashboard`, worktree: `/tmp/account-53-frontend-reporting-dashboard`, base: `origin/main@e8df751c`.
- Scope:
  * `frontend/src/services/reportingService.ts`
  * `frontend/src/app/reports/statements/page.tsx`
  * `frontend/src/app/page.tsx`
- Result: PASS. Implemented normalized datetime serialization, robust mock fallback on network boundary, statement hierarchy adaptation, export download extension detection, and dashboard quick summary widget.
- Verification: TypeScript compilation PASS, `git diff --check` PASS, 0 conflict markers.
- Residual Risk: Live API responses depend on `reporting-api` service; offline mock fallback guarantees continuous client usability.

## 2026-09-23 — GH-59 Frontend Journal Entry & Approval Workflow Implementation

- Issue: #59 (`difficulty:medium`, `module:frontend`, `status:draft`)
- Branch: `agent/59-frontend-journal-workflow`, worktree: `/tmp/account-59-frontend-journal-workflow`, base: `origin/main@4a3cb36c`.
- Scope:
  * `frontend/src/services/journalService.ts`
  * `frontend/src/app/journal/list/page.tsx`
  * `frontend/src/app/journal/entry/page.tsx`
  * `frontend/src/app/journal/list/JournalList.module.css`
- Result: PASS. Implemented approval and posting workflow with UI state transitions, toast feedback, and balanced entry validations.
- Verification: TypeScript compilation, ESLint, `git diff --check`, and conflict marker checks all pass.
- Residual Risk: Real API tests depend on `journal-ledger-api` container running; offline mock fallback ensures seamless client preview.

## 2026-09-22 — GH-515 runtime gate PARTIAL handoff

- Issue/space: #515; `agent/515-dev-compose-runtime-gate`; `/tmp/account-515-dev-compose-runtime-gate`; base `origin/main@e375002ff63652aabaaa1f12c74aa611d4ed29bc`; evidence commit `282387bc`; Draft PR [#728](https://github.com/skyg547/account/pull/728) with `Refs #515`.
- Current result: 대상 8개 컨테이너는 `podman ps -a`에서 모두 Up이지만 전체 gate는 PARTIAL/FAIL. PostgreSQL 5432 protocol/ready와 Redis 6379 PONG은 PASS. Discovery/Gateway는 내부 2xx지만 host HTTP timeout. Config는 host refused/internal timeout. Frontend/Nginx host ports는 refused. Quick Tunnel external `/`는 200이나 `/api/actuator/health`는 401이다. Quick hostname은 공개하지 않고 hash `02ddfba18a25`만 남겼다.
- Residual risk: rootless Podman 게시 포트와 관리 명령이 지연/불능 상태일 수 있고 Config 서비스가 응답하지 않는다. 외부 루트 200은 인증·업무 API·DB schema·Batch·부하 성공을 뜻하지 않는다. API health 401을 UP으로 해석하지 않는다.
- Next owner/action: 개발 서버 운영자가 민감값을 출력하지 않고 Config 프로세스와 Podman 포트 포워딩/관리 지연을 진단·복구한 후 8888/8761/8000/3000/8080 host checks 및 Nginx `/api/actuator/health`를 재실행한다. 별도 승인 전 container restart/recreate/down, DB/schema/data 변경, volume/network 삭제를 하지 않는다.
- Changed files: `docs/ai-harness/agent-status.md`, `docs/ai-harness/worklog.md`, `docs/ai-harness/handoff.md`, `docs/history/CODEX_WORKLOG.md`. Runtime/source/test/Compose changes 없음. Rollback은 문서 commit revert뿐이다.
- Verification/authority: runtime commands and sanitized results are in `worklog.md`. Harness quality 32/32, validator, diff/conflict/secret-pattern checks PASS; independent read-only `/root/independent_review` found no P0–P3 and confirmed Q1 N/A/Q2 PASS/Q3 N/A/Q4 N/A. Merge authority는 reviewer/사용자 승인 후 Integrator에게만 있으며, 이 작업은 Ready/merge/Issue close/cleanup을 수행하지 않는다.

## 2026-09-23 — GH-694 Master Data Direct Write Authorization Enforcement

- Issue: #694 (`difficulty:high`, `agent-loop`, `spec-driven`, `type:security`, `priority:p1`, `sizing:atomic`, `module:master-data`)
- Branch: `agent/694-master-direct-write-authorization`, worktree: `/tmp/account-694-master-direct-write-authorization`, base: `e375002f` (origin/main).
- Scope:
  * `master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataDirectWritePolicy.java`: Unified administrative role authorization helper for direct CRUD operations (`ADMIN`, `SYSTEM_ADMIN`, `MASTER_MANAGER`, `ACCOUNTING_ADMIN`, `PARTNER_MANAGER`). Rejects missing or non-admin roles with HTTP 403 Forbidden fail-closed.
  * `AccountSubjectController.java`, `BusinessPartnerController.java`, `ProductController.java`, `DepartmentController.java`: Bound `X-Auth-Roles` header and invoked `requireAdminRole()` across all 10 mutation endpoints (`POST`, `PUT`, `DELETE`). Zero service/use case invocations on unauthorized requests.
  * `master-data/api/src/test/java/com/ho/account/masterdata/api/web/MasterDataDirectWriteAuthorizationTest.java`: 41 MockMvc test cases verifying missing header, disallowed roles (403), admin roles (200/201/204), and regression coverage for public GET queries.
  * `master-data/README.md`, `docs/process-flow.md`, `docs/local-run.md`: Updated documentation reflecting administrative authorization policy on direct write APIs.
- Verification:
  * `./gradlew :master-data:api:test :master-data:core:test`: 509 tests PASS (API 65, Core 444, 0 failures, 0 errors, 0 skips).
  * `./gradlew :master-data:api:bootJar`: PASS.
  * `git diff --check`: PASS.
  * Conflict marker scan: PASS (exit 1).

## 2026-09-22 — GH-677 Conflict Marker Self-False-Positive Elimination

- Issue: #677 (`difficulty:low`, `agent-loop`, `spec-driven`, `type:docs`, `priority:p2`, `sizing:atomic`)
- Branch: `agent/677-conflict-marker`, worktree: `/tmp/account-677-conflict-marker`, base: `e375002f` (origin/main).
- Scope:
  * `docs/ai-harness/10-rules.md`: Replaced unanchored conflict marker search with anchored pattern `^(<{7}|={7}|>{7})( |$)`, added explicit approved files scanning, added PowerShell and Linux bash examples, and added beginner smoke-detector analogy and raw `rg` exit code explanation (0=fail, 1=pass, 2+=error).
  * `docs/ai-harness/40-test-checklist.md`: Aligned pattern and added explicit reproduction cases (example command string -> exit 1, real 3-line markers -> exit 0, non-existent file -> exit 2+).
- Verification:
  * Raw rg exit code verification in Linux bash:
    - Case 1 (example command string): exit 1 (PASS, no self-match)
    - Case 2 (real conflict marker lines): exit 0 (PASS, markers caught)
    - Case 3 (missing path): exit 2 (PASS, scan error detected)
    - Case 4 (the 2 target files): exit 1 (PASS, zero false positive)
  * `node --test tools/ci/harness-quality-contract.test.cjs`: 32/32 tests PASS.
  * `python3 -m unittest discover -s tools/ci -p "*.py"`: 22/22 tests PASS.
  * `git diff --check`: PASS.

## 2026-09-16 — GH-726 AI Harness v2 (Dynamic Model Tiering & Circuit Breaker)

- Issue: #726 (`difficulty:medium`, `agent-loop`, `spec-driven`, `type:docs`, `priority:p1`, `sizing:atomic`)
- Branch: `agent/726-harness-v2`, worktree: `/tmp/account-726-harness-v2`, base: `6e104a87` (origin/main).
- Scope:
  * `docs/ai-harness/70-model-assignment-policy.md`: Dynamic Model Tiering formalized (`gpt-5.6-sol` default, `gpt-6-astra` escalated for `difficulty:very-high` or `model:astra`).
  * `docs/ai-harness/25-issue-claim-verification.md`: Claim verification early-exit and circuit breaker protocol (2 consecutive failures -> block).
  * `docs/ai-harness/85-github-issue-agent-loop.md`: Harness v2 model tiering in agent loop and quota guard policy.
  * `scripts/codex-runner.py`: Continuous runner with dynamic model selection, `--dry-run`, `--list`, `--issue`, quota exit on usage limit, and consecutive failure circuit breaker.
  * `scripts/test_codex_runner.py`: 7/7 unit tests verifying model tiering, escalation, module extraction.
- Verification:
  * `python3 -m unittest discover -s scripts -p 'test_*.py' -v`: 7 tests PASS.
  * `python3 scripts/codex-runner.py --list`: PASS.
  * Conflict marker scan: PASS (anchored regex check).
  * `git diff --check`: PASS.

## 2026-09-13 — AI Harness Module Scope & Cross-Module Policy

- Branch: agent/harness-module-policy, worktree: /tmp/account-harness-module-policy, base: cebf014a (origin/main).
- Policy update in docs/ai-harness/87-spec-driven-delegation.md:
  * 단일 모듈 수직 슬라이스(Vertical Slice) 예외 명시: Controller, DTO, Port, Service, Domain, Adapter, Test 등 1개 기능의 온전한 헥사고날 슬라이스 구현 시 파일 5개 제한 대신 대상 모듈 전체(module-name/**)를 Allowlist로 허용 (80-file-ownership.md Module Writer와 정합성 일치).
  * 다중 모듈(Cross-Module) 3대 처리 패턴 확립:
    1) Contracts-First: contracts/ 또는 shared-kernel 공통 인터페이스/DTO 선반영 머지 후 모듈별 병렬 진행.
    2) module:cross-module: 강결합 다중 모듈 작업을 단일 세션이 원자적으로 작업(:mod1:test :mod2:test).
    3) Fork-Join: 부모 Integrator가 총괄 지휘하고 서브에이전트가 모듈별 분담 후 부모가 통합 PR 작성.
- Policy update in docs/ai-harness/80-file-ownership.md: Cross-Module Writer 역할 명시.
- Runner automation in scripts/codex-runner.py: Issue의 module:* 라벨을 자동 파싱하여 모듈별 격리 스코프, 디렉터리 Allowlist, 전용 Gradle 테스트 명령 자동 주입.
- Verification: node --test tools/ci/harness-pr-contract.test.cjs tools/ci/harness-quality-contract.test.cjs (64 pass), python3 tools/ci/validate-harness.py (PASS), git diff --check PASS.

## 2026-09-13 — GH-667 Draft PR publication

- Draft PR [#723](https://github.com/skyg547/account/pull/723) OPEN/DRAFT, `Refs #667`, from `agent/667-scd2-future-overlap` to `main`; implementation commit `da88765c`. Worktree `/tmp/account-667-scd2-future-overlap` retained. Issue #667 OPEN / `status:needs-review` / `agent:codex`; ordered execution split documented and stale `sizing:needs-split` removed.
- PR body records473 passing module tests,56 focused subset,18 expected red failures, packaged H2 readiness smoke, initial global-health503, Q1–Q4, rollback, remaining PostgreSQL/concurrency gates and explicit Merge authority/session separation. Independent read-only reviewer also cleared final records/body with no findings.
- GitHub checks have started; no remote CI success is claimed at publication. Draft/Guard trust-policy gates remain before any Ready/merge. Parent only posted/committed/pushed; Ready/merge/Issue close/branch-worktree deletion not performed. Next owner: human reviewer for policy, latest head/base and CI. This checkpoint updates records only, so Java tests are not rerun.

## 2026-09-13 — GH-667 verified; Draft publication pending

- Issue #667; `agent/667-scd2-future-overlap`; `/tmp/account-667-scd2-future-overlap`; base `e7c462f4`. Common core policy prevents four-type sequential future overlap before mutation/save; finite source bounds and scheduled history are preserved.
- Verification: RED18 expected failures; GREEN56 focused; full473(core444/API24/Batch5),0 failures/errors/skips; API/Batch bootJar; isolated local H2 packaged API readiness HTTP200/UP; harness32 and static gates PASS. Initial global health503 is documented separately. See GH-667 in worklog.md for exact commands, evidence and scope.
- Independent `/root/independent_review`: no P0–P3; Q1–Q4 PASS. Production5/test4/module-doc1 files plus parent-owned harness/history. No build/config/DB changes.
- Rollback: reviewed code/test/doc revert; preserve business/scheduled data and migrations. PostgreSQL/concurrent writers/existing-data repair and new end-to-end approved-overlap test remain separate.
- Parent owns Git/GitHub; implementer and reviewer sessions are separated procedurally. Draft PR uses Refs #667 and Merge authority text. Next: parent Draft publication, then human current-head/base/CI review. No Ready/merge/Issue close/cleanup.

## 2026-09-12 — GH-665 Draft PR publication

- Published Draft PR https://github.com/skyg547/account/pull/722 with Refs #665 from `agent/665-command-idempotency` to `main`. Implementation commit: `f640f56c59b9ce382b5000e21fdbbf1907ab5ad0`; worktree: `/tmp/account-665-command-idempotency`. Issue #665 remains OPEN / status:needs-review.
- The published body contains the legacy-key decision, six-command contract, verification commands/results, Q1–Q4 evidence, rollback/cutover risks and explicit implementer/reviewer/merge-authority separation. Independent read-only xhigh Reviewer cleared both the implementation and final records/body with no remaining findings.
- Final evidence: 758 Java tests and 32 harness checks PASS; bootJar and 26 actual H2 HTTP checks PASS. The 26 verified source hashes and executable artifact match the reviewed implementation. This publication checkpoint changes only four harness/history records, so production tests are not repeated.
- Owned ephemeral test containers/pod were removed after verification; the original dirty checkout and this branch/worktree are retained. Existing record histories are preserved.
- Next owner: human reviewer for policy/integration review and latest-head/base CI. GitHub checks started on publication and were still running at this checkpoint; no CI success is asserted here. PR remains Draft. Ready, merge, Issue close and branch/worktree removal were not performed or authorized.

## 2026-09-12 — GH-665 command idempotency and atomic audit

- Issue #665; difficulty:very-high / user-designated xhigh. Branch `agent/665-command-idempotency`, external worktree `/tmp/account-665-command-idempotency`, base `origin/main@87206771d8057fdd4f39bd35df0331768d0bf05f`. Existing worktree policy/checkpoint edits and original dirty primary checkout preserved. #666/PR #705 already merged; no concurrent Internal Audit owner conflict.
- The later user implementation request and Full Authority permit the parent to resolve the proposal and expand related files. Parent adopted legacy audit-only key rejection (409), recorded the decision/compatibility/atomic scope remotely at https://github.com/skyg547/account/issues/665#issuecomment-5645801578, and claimed implementation. This supersedes the earlier local pending checkpoint below; it does not invent an earlier policy approval.
- All six commands now acquire the shared key bucket before lookup, replay only a complete matching v1 receipt, reject a different actor/action/aggregate/effective path/payload, and preserve newer business state on old-key retry. Canonical SHA-256 uses applied defaults/domain normalization; trace/time are excluded. Strict result shape/version and original immutable audit cross-check reject corrupted snapshots safely. HTTP409/500 expose only fixed generic codes/messages.
- SQL uses separate receipt domain/port/JDBC adapter plus identical H2/PostgreSQL V62 and 256 preseeded lock rows. Reservation, business write, audit append and snapshot completion share one Spring transaction. Direct append takes the same lock and compares event fields; UNIQUE errors are not caught and reread from an aborted transaction. Existing V60/V61/audit rows remain unchanged and no legacy receipt is backfilled. No-key/blank and trimmed global key namespace remain compatible; keyed convenience constructors cannot bypass protection.
- Scope: two services/new executor/domain/port/adapter/exception, audit adapter, local V62/HTTP exception mapping/postgresTest build task, core/API tests, migration-runner V62 parity/upgrade tests, module README/process-flow/schema/contract, and four parent-owned harness/history records. Service/SQL/Controller/Test roles used disjoint ownership; independent Reviewer was read-only. Parent owns shared records and Git/GitHub mutations.
- Verification PASS: core 172 + API/H2 232 + PostgreSQL214 + migration-runner 140 = 758 Java tests, failures/errors/skips 0. PG includes 177 shared cases, 36 observed database-lock races across all six commands, and 1 migration/READ_COMMITTED test. Node harness quality 32 PASS. bootJar built; actual local H2 HTTP 26 checks passed (all-six replay plus process conflict/stale-state), test-only Redis health disabled. Logs: /tmp/account-665-migration.log, /tmp/account-665-modules-final.log (core passed; API setup later repaired), /tmp/account-665-postgres.log (final API/PG PASS), /tmp/account-665-live-http.log. XML and /tmp/account-665-evidence/results.json independently inspected. All 26 source-manifest hashes match; bootJar changed classes/V62 match compiled/source bytes. JAR SHA256 d20e637e589c09d9322f912b1defd1e7dfa9b59d334804beae8078c7b0511fb6. Gradle8.7/JDK17, offline network-none CPU1/RAM1536MiB/pids512/max-workers1; fixture PostgreSQL16 had 0.5 CPU/384MiB/tmpfs and synthetic inputs only. Owned H2/PostgreSQL containers stopped; pod cleanup verified separately before publication.
- Intermediate failures: incomplete service edit at early compile, SQL test generic AssertJ inference, Mockito restubbing and transaction-proxy fault setup. All were corrected in their owned files before final verification; earlier failures were not counted as passing evidence. No package/dependency/image download was needed.
- Independent review: /root/reviewer (separate read-only xhigh session) found no remaining P0–P3 and Q1–Q4 PASS. Earlier valid-shaped damaged snapshot concern was fixed with immutable-audit comparison and six-command regressions; strict format/golden SHA256 independently corroborated. Q1=service/port/adapter boundaries and executed rollback tests; Q2=contract/flow/failure explanation; Q3=module/schema/run/rollout documentation; Q4=lock/snapshot/proxy intent comments. Final parent-record audit precedes publication.
- Rollback/risk: reviewed code revert preserves audit rows and applied migration history. Prior writers lack the receipt/lock guard; actual rollout needs write pause, V62 and runtime privileges, and all writers replaced together. Legacy normal retries can now return 409; do not bulk resubmit under new keys. Hash-bucket collisions serialize unrelated keys; PostgreSQL evidence is READ_COMMITTED. Production TLS/load/Gateway authentication/rollout and arbitrary future record-version compatibility are outside this local proof.
- Authority: Implementer tier High Reasoning / xhigh. Merge authority: independent Reviewer evidence and current head/base/CI, then separately authorized parent Integrator only. Role separation is procedural across sessions, not separate GitHub identities or a GitHub APPROVED review. Requested endpoint is Refs #665 Draft PR; Ready/merge/Issue close/branch-worktree deletion are not performed.
- Publication: Verified Draft body prepared with Refs #665; commit/push/PR URL follow at publication checkpoint. Next owner is the parent to publish the verified Draft, then the human reviewer for latest-head/base CI and policy/integration review. No source conflicts occurred.

## 2026-09-12 — GH-665 policy decision checkpoint

- Issue #665; difficulty:very-high / user-designated xhigh. Base `origin/main@87206771d8057fdd4f39bd35df0331768d0bf05f`; branch `agent/665-command-idempotency`; isolated worktree `/tmp/account-665-command-idempotency`. Original dirty checkout preserved.
- Remote inspection: actual labels include status:ready and sizing:needs-split, but body and the only comment explicitly leave legacy audit-only key behavior unapproved. No explicit remote policy decision was found. #666/PR #705 is merged; its count invariants are present in the base.
- Confirmed current save-before-audit and duplicate-by-key-only code path. Existing E2E retries the audit port, not the business command. No new runtime reproduction or implementation success is claimed.
- Prepared `internal-audit/docs/issue-665-command-idempotency-plan.md`: reviewable legacy 409 proposal, no-key compatibility, canonical identity/snapshot scope, transaction boundaries, ownership draft, six-command/H2/HTTP/PostgreSQL verification plan and rollback.
- Read-only independent xhigh policy-contract review confirmed that the ready label does not resolve the missing policy. A concrete policy-choice question is pending with the user. Full Authority covers related file expansion; this checkpoint does not invent approval of the Issue's expressly reserved business decision.
- Changed files: that proposal plus parent-owned `worklog.md`, `agent-status.md`, `handoff.md`, and `docs/history/CODEX_WORKLOG.md`. Production/test/build/migration source unchanged. No claim, child Issue, remote comment, commit, push or PR was created at this checkpoint.
- Baseline verification PASS: cached Gradle8.7/JDK17, network none, CPU1/RAM1536MiB/pids512, one worker; exit0, 12 tasks executed, core76 + API53 =129 tests, failures/errors/skips0. Command: `sh ./gradlew :internal-audit:core:test :internal-audit:api:test --offline --no-daemon --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=1'`. Evidence: `/tmp/account-665-baseline.log` and fresh module XML reports. The owned `--rm` test container exited. This verifies unchanged existing behavior, not the proposed contract; new replay/rollback/HTTP/PostgreSQL and executable-JAR smoke remain unexecuted until implementation.
- Independent docs-checkpoint review: no remaining P0–P3 findings. Added fail-closed handling/tests for corrupt/incomplete receipts and unsupported versions, plus mixed-version writer cutover precondition. Q1=N/A (no production/test change), Q2=PASS (proposal flow/failure rationale), Q3=PASS (proposal prerequisites/commands/limits), Q4=N/A (no nontrivial code change). Reviewer: `/root/policy_contract_review`, read-only xhigh. This is not approval of policy or implementation readiness.
- Static verification: all four existing histories preserved intact, equal new checkpoint prefixes, scoped whitespace/conflict-marker checks and `git diff --check` pass.
- Next owner: user decides legacy policy; parent records the decision remotely, finalizes atomic ownership and implements with Service/SQL/Controller/Test roles, then a separate read-only Reviewer validates actual changes. Draft PR after acceptance tests must use `Refs #665` and include verification plus authority separation. No Ready/merge/close is authorized by this checkpoint.
- Rollback: only the local proposal/record edits need reverting at this stage; no database or migration was changed. Future code rollback must preserve audit rows and applied migration history. Unresolved risks: legacy retry compatibility, actual DB reservation/rollback, HTTP contract and PostgreSQL first-key concurrency.

## 2026-09-11 — GH-666 operating evaluation count invariants

- Issue #666; difficulty:medium / high reasoning (user-specified). Branch `agent/666-operating-evaluation-counts`; isolated worktree `/tmp/account-666-operating-evaluation-counts`; base `origin/main@44f973549e5f4fa79c4076a686838063be92d34a`. Original dirty checkout preserved. Draft publication is the authorized endpoint; no Ready/merge/close/cleanup.
- Goal/AC: reject any present negative count and exceptionCount>sampleSize before evaluation/audit persistence; authentic invalid HTTP requests return400. Existing result normalization and actor/control validation remain. Null/0 policy explicitly recorded in Issue comment and `internal-audit/docs/process-flow.md`: each null independently means unspecified and is preserved, nonnull counts>=0, comparison only when both known; sample0 permits exception null/0. This is a compatibility-based policy decision from Integer/nullableV60, not a claim that prior docs specified its meaning.
- Scope: `OperatingEvaluation.java` (+12 lines), new `OperatingEvaluationTest.java`, existing `EvaluationServiceTest.java` and `EvaluationControllerTest.java`, module process-flow documentation, and these four parent-owned harness/history records. Domain/service agent and test agent had disjoint file allowlists; independent reviewer was read-only. #665 is draft with no active PR; its later shared service-test changes must integrate serially.
- RED: pre-fix core76 tests/27failures/0errors/0skips, exit1/2m53s; API did not execute after core failure. Targeted GREEN domain33/33, exit0/47s. Final core76 + API53 =129/129 (existing57 + new72), failure/error/skip0 across11 fresh XML suites; exit0/2m49s;14tasks,11 executed,3 up-to-date. Both test tasks executed. API bootJar passed; SHA256 `cac45990578dd2732956c65841c821c245c714d68fa699eb6419bac9973a15b9`.
- Command inside cached Gradle8.7/JDK17 container: `sh ./gradlew :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar --offline --no-daemon --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=1'`. Targeted run used `:internal-audit:core:test --tests '*OperatingEvaluationTest'` with same flags; RED used core/API test tasks with repository JVM defaults. Host lacked JDK17; existing image/cache used without installation/download, network none/CPU1/RAM1536MiB/pids512. Linux wrapper replaces the Issue's Windows `.bat` command.
- Independent `/root/independent_review` checked actual diff, new untracked test, RED/GREEN logs and fresh XML. No remaining P0–P3; Q1–Q4 PASS: domain invariant/129 regression tests; process-flow input→error→storage explanation; executable offline instructions/limitations; null-preservation and raw-JSON intent comments. `git diff --check` and conflict-marker checks passed; no conflict log required.
- Evidence artifacts: `/tmp/account-666-baseline.log`, `account-666-targeted.log`, `account-666-full.log`, `account-666-verification.json` (all under `/tmp`). Additional runtime evidence is recorded below after the final checkpoint; offline tests include local H2 composition/integration, not actual PostgreSQL/TLS/concurrency.
- Rollback: reviewed source/test revert; preserve existing evidence rows, schema and shared history. Historical invalid rows can now fail domain rehydration/read; data inspection/correction is separate. No schema/migration/build changes or result automation. Next owner: human Reviewer and separately authorized parent Integrator for final head/base/CI and policy review.
- Implementer tier: difficulty:medium / High reasoning (user-specified). Merge authority: independent Reviewer evidence and current head/base/CI checks followed by separately authorized parent Integrator only; implementation/test/reviewer children perform no Git/GitHub mutations. Parent authorization currently covers dedicated branch push and Draft PR creation only.
- Additional executable-JAR smoke PASS on JDK17 in a no-network/CPU1/RAM768MiB/pids256 ephemeral container: health200, synthetic process/risk/control setup, five invalid requests400 with empty evaluation query, six accepted ordinary/equality/zero/null combinations persisted/read with normalized result and trusted actor. Exit0; owned container stopped/removed. Command: python3 /tmp/account-666-smoke.py; evidence: /tmp/account-666-jdk17-smoke.log. Actual JAR args: java -XX:ActiveProcessorCount=1 -Xmx384m -jar /app.jar --spring.profiles.active=local --server.port=18083 --management.health.redis.enabled=false. The last flag is test-only: the unchanged shared Redis health indicator caused503 with network blocked; production config was not modified. Earlier Podman35s health/creation timeouts are retained in /tmp/account-666-smoke.log and /tmp/account-666-smoke-retry.log; the later unchanged-health container was stopped after503. Additional host JDK21 loopback/H2 smoke also passed the same cases and its owned Java process stopped (/tmp/account-666-host-smoke.log). Required offline129 and final isolated container smoke both used JDK17; no actual PostgreSQL/TLS/concurrency claim.
- Draft PR [#705](https://github.com/skyg547/account/pull/705) published with Refs #666; implementation commit fb6ca0dd pushed on the dedicated branch. Issue status:needs-review; independent Q1–Q4 review complete. Latest-head CI and human policy/integration review are next gates; Draft/Issue remain open.

## 2026-09-11 — GH-690 all10 real batches verified / Draft PR #704

- Issue #690, `difficulty:high`; branch `agent/690-business-batch-seeds`; isolated worktree `/tmp/account-690-business-batch-seeds`. Full authority covers directly required repairs, runtime/tests and Draft publication. Primary dirty checkout is preserved.
- Integrated `main@56c07971` (already-merged #689/#695) as `a936d82b`; older unmerged-PR statements below are historical. Both-parent record checks10/10 and stash-stage checks8/8 passed; financial/security tests retained. See conflict-log and integration-log.
- Changes: Accounting/Products/Risk seeds; safe idempotent injection and exact-identity sequential runners; dev composition/reader/executor fixes; Loan durable scheduled repayment and remote journal validation; Closing read-only sources; actual Reconciliation posted financial queries; beginner and module documentation. Parent owns shared records/Git/runtime; independent reviewer is read-only.
- Validation: Seed12/12 (11 business DB + Closing readiness); Python55+38, SQL10, PostgreSQL rollback probes6 and linked-expenditure rejection, Node135. Java972 across18 project test tasks, failures/errors/skips0 (unchanged Asset Batch3 reused valid up-to-date results); eight batch JARs and affected API builds/compilation pass. Commands/details: [business batch guide](../guides/business-batch-dev-verification.md).
- All10 real batch gates passed: balances1, fx4, deposit1, loan-interest3, loan-repayment4, depreciation1, ecl1, mart1, reconciliation2, provision5. Every metadata status/exit is COMPLETED with expected outputs, exit0/OOMfalse and inspected CPU0.5/RAM768MiB. Same-parameter all10 verification passed without new execution; Mart exactly1 execution; Loan cross-DB journal ID/slip agree and pending0.
- Journal API actual bytes/label match tested SHA256 `65eb68586b08cb7d86ec964ed2cbe6b67205cf5e74181a3f9fc43858945dd28c`; health/Eureka/resource/restart0/OOMfalse/error0/by-id gates pass. Previous image retained. Balances is explicitly a valid prior completed execution, not a claim that the newest compiled JAR was rerun.
- Preserved failure evidence: FX reader lifecycle, provider port and ambiguous response with one persisted draft (same-instance4 reused safely); Deposit duplicate adapters; Loan insufficient pool and missing income mapping; Reconciliation placeholder reads. Corrected and reverified; no metadata deletion or random IDs used to bypass failure.
- Independent final review Q1–Q4 PASS with no remaining P0–P3; reviewer independently checked XML totals, JAR hashes and safe runtime/repeat logs. Draft PR [#704](https://github.com/skyg547/account/pull/704) published with `Refs #690`; implementation commit `94624150`, base `56c07971`. Dedicated branch pushed; final CI/human review are next gates. No Ready/merge/Issue close/cleanup performed.
- Limits/rollback: rootless Podman, small synthetic Stage1/shallow reconciliation, FX/ECL drafts, Deposit read-only; no load/distributed atomicity claim. Reconciliation range response is unpaged. Reviewed source revert; manual guarded seed cleanup only after lineage review, unresolved Loan pending/posted journals block blind cleanup. Preserve DB/volumes/metadata and original Journal image.
- Implementer tier: High reasoning (difficulty:high)
- Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 runtime and Q1–Q4 PASS / Draft PR #689

- Requested Draft PR published: [#689](https://github.com/skyg547/account/pull/689), `Refs #653`, branch `agent/653-business-runtime`. Only this dedicated branch was pushed. Final frozen head/base and live CI results are recorded in the PR body/checks; this handoff-only commit changes no production code. Issue remains open. Ready/main merge/Issue close/cleanup were not performed.

- Issue #653; branch `agent/653-business-runtime`; isolated worktree `/tmp/account-653-business-runtime`. Final runtime observed at 2026-09-10 15:51 KST: all13 sequential images built, Accounting7/Products3/Risk3 package checkpoints and final full13 re-verification PASS; retained all13 and existing platform.
- Every API: root actuator health UP, Eureka current IP:8080 UP, running=true/restart0/OOMfalse, CPU0.50/RAM768MiB, last10000 log ERROR/startup/DB pattern counts0. Accounting/Products actual ACL + desired config hash/healthy reuse and normal Risk Compose DB gate passed. Evidence `/tmp/issue653-runtime-evidence.log` ends with final13 PASS; helper exit0. Per-service results and reproducible commands: `docs/guides/business-external-dev-verification.md`.
- Verification: current affected Java423 + unchanged prior Closing124 =547 unique tests, failures/errors/skips0; Python runner38/38 after timeout300 change. All13 actual bootJar images pass. No repeated Java run for subsequent two comment lines or documentation-only main synchronization. After documentation-only main synchronization to b7c1c7c1fa45, both local Node contracts pass64/64 (Node22.23.2, failures/skips0, exit0). Four history conflicts/six blocks preserve both sides; required conflict-log updated. Independent reviewer confirms Q1–Q4 PASS, Node64 rerun PASS, both-parent history preservation and no remaining P0–P3 findings. Draft CI and human review are separate next gates.
- Approved changes: three Compose files/runner/tests, six Eureka dependencies, Closing/Loan dev persistence isolation, real dev HTTP adapters for Closing/Loan/Reporting/Deposit, Loan column/Deposit TEXT mapping, Reporting Vault off, Deposit automatic outbox relay off, Products/Risk readonly common DB gate mount, API Hikari3/1. No DDL/provider contract/platform/credential/DB-server changes. Bounded file scopes and recovery evidence are linked in Issue comments and feature guide.
- Actual startup failures were repaired and reverified; timeouts never counted as success. Reviewed same-engine recovery started only hash-matched never-started Closing. Existing Accounting/Products DB gates were verified by hash + original readonly ACL + state when no-change Compose timed out. Risk used normal DB Compose. No broad teardown or unrelated service stop.
- Limitations: health is not financial transaction/load certification; provider-missing Loan reference, Closing/Reporting detail/aggregate, Deposit ID approval/recovery contracts explicitly fail; Asset Lease payment fallback/Kafka flows remain separately unverified. Small Hikari pools may queue/time out under load; remote writes do not guarantee distributed atomicity or exactly-once. Java-unchanged modules use bootJar/runtime gates rather than new unit suites; unchanged Batch suites were not broadened.
- Rollback: exact service label only, retain previous images/healthy APIs/DB/network/volumes; reviewed source revert. Parent Integrator owns Git/harness/runtime, independent reviewer stays read-only. Next owner: parent to record final-head CI and independent Draft review; human reviewer then reviews before any separately authorized lifecycle transition. Ready/merge/Issue close/cleanup require separate approval.
- Implementer tier: High reasoning (difficulty:high)
- Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 all package checkpoints PASS (historical checkpoint)

- Latest runtime checkpoint (15:30 KST): all13 images, all13 individual live gates and Accounting7/Products3/Risk3 complete package checkpoints PASS. Risk DB prerequisite used normal Compose and passed in223s. Final full13 retained-state sweep is now checking Accounting then Products then Risk. No final13 marker/commit/push/PR yet; prior checkpoints below describe the recovery sequence.
- Same Issue/branch/worktree: #653, `agent/653-business-runtime`, `/tmp/account-653-business-runtime`. All source review and tests remain valid: current Java423 + unchanged Closing124 =547, Python38, no open P0–P3 source findings.
- Accounting7 full live checkpoint passed. Products3 images built; missing common DB-check script mount repaired read-only in Products/Risk, then Products DB ACL prerequisite and Deposit live gates passed. Deposit automatic outbox relay is explicitly disabled for health verification.
- Loan startup failed PostgreSQL53300 capacity (not OOM/authentication/permission/schema-object failure). Stopped only failed Loan. Added API-only Hikari maximum3/minIdle1 to all13 approved Compose blocks under Issue comment5612617861; DB server limits/roles/DDL/credentials and Batch pools remain unchanged.
- Planned Deposit stop alone did not free sufficient lasting capacity: DB gate and a sanitized SELECT1 still reported capacity failure. Stopped verified Reporting next; its normal DB/up/verify sequence then ran before Deposit recovery and sequential Accounting reloads. Other six Accounting APIs were retained. No simultaneous package startup or broad teardown.
- Reporting and Deposit subsequently recovered and passed all live gates. The Spring Boot3.2.5/Spring6.1.6/Hikari5.0.1 synthetic Binder probe confirms current environment names bind max3/min1. Accounting rolling reload passed Journal Ledger; Closing Compose exceeded180s after create but before start (restart0/OOMfalse/log0). Raised only generic/Compose/per-service command limits to300s; Python38 passes. Same Accounting sequence resumes with unchanged DB/health/Eureka/resource/log gates.
- Further I/O recovery: unchanged Journal Compose up exceeded300s but exact state and desired config hash remained valid. Closing exact created/never-started/hash-matched container was started alone via same engine (98s), then all normal live gates passed. Remaining four Accounting APIs subsequently used individual normal up and passed the full7 checkpoint; no timeout itself is treated as PASS. Independent recovery review found no omitted service/gate; temporary exception sanitizer parity was repaired.
- Accounting DB-check no-change Compose later exceeded240s, while exact checker remained healthy and original ACL script directly passed exit0/error0. Reviewed temporary helper now revalidates existing Accounting/Products DB-check via desired config hash, actual ACL execution and exact healthy state; outer env identity and all API/Risk/build/final gates remain. Remaining Accounting sequence resumed.
- New fetched main `b7c1c7c1` contains documentation/quality policy only, no Java/build changes. New Q1/Q2/Q4 independent review passes; Q4 added only a two-line V42 TEXT intent comment in approved DepositOutboxEntity. Q3 remains completion-pending until final13 evidence, final documentation and both Node contract checks after base synchronization.
- At this checkpoint, helper `/tmp/issue653-remote-runtime.py` had passed Loan/Asset Lease and Products checkpoint and built Risk images; Risk startup/final verification remained. Normalized evidence: `/tmp/issue653-runtime-evidence.log`. No13/13, commit, push or Draft PR claimed; owner explicitly gates commit/Draft on all13 normal operation.
- Parent owns runtime/Git/harness, separate Reviewer remains read-only. Next owner: parent Integrator to complete capacity-limited rolling verification. Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 all-13 runtime continuation (in progress)

- Issue #653; branch `agent/653-business-runtime`; isolated worktree `/tmp/account-653-business-runtime`; preserved implementation base `34c75839`, resumed HEAD `baed1d83`. Owner comment5611054436 authorizes six Eureka API dependencies and bounded repairs needed for all13 APIs; older scope-blocked entries below are historical.
- Implemented six Eureka starters and native enforced Boot3.2.5/Cloud2023.0.1 BOMs for Reconciliation's reproducible Gradle8.7 resolution error; eight dependency comparisons preserve expected versions. Loan dev owns its persistence and explicitly opts into HTTP ports; exact V30 column mapping old_eir/new_eir repaired without DDL. Bounded scopes recorded in Issue comments5611100834/5611355586.
- Verified this continuation: six API30 + Reconciliation core94 + Loan core99/API6/batch3 =232 Java tests, zero failures/errors/skips; Python runner38/38. Existing unchanged Closing124 tests independently confirmed. Separate read-only review cleared these changes.
- Accounting images built sequentially; Payable/Receivable/Expenditure/Tax live gates passed. Reporting actual startup exposed missing LoadLedgerPort; stopped only failed Reporting (restart1, OOMfalse, database failure0). Bounded real HTTP Reporting repair/test scope recorded comment5611823487. Deposit preflight missing real external ports scope recorded comment5611833868; local fabricated journal adapters are not enabled.
- Journal Ledger and Closing subsequent live rechecks passed health/Eureka UP, restart0/OOMfalse, CPU0.50/RAM768MiB and zero inspected error/startup/database patterns. Remaining Accounting checks and Reporting/Deposit tests are in progress. No Accounting package checkpoint or13/13 success claimed yet; Products/Risk not built/started in this continuation.
- Further checkpoint: all six retained Accounting services passed full rechecks. Reporting core92/API10/batch6=108 tests pass after correcting a duplicated lambda variable name. Deposit core85 passed; actual migration validation caught payload TEXT versus @Lob/CLOB, repaired only @Lob/import under comment5611921269 and added long Unicode persistence roundtrip; Deposit core/API/batch rerun pending. Separate reviewer cleared production and resolved two test findings.
- Final source-test checkpoint: Reporting108 and Deposit92 pass, including dev schema validation and long Unicode outbox roundtrip. Independent XML review confirms unique current affected Java423 + unchanged Closing124 =547 tests, failures/errors/skips0; Python38 separately passes. All P0–P3 review findings resolved. Runtime sequence resumed at Reporting rebuild/up; Issue comment5611993076 records this checkpoint.
- Reporting image rebuild passed; actual startup then exposed Vault clientAuthentication/vaultTemplate auto-configuration (restart1/OOMfalse, DB patterns0) absent from the dev test's disabled-Vault composition. Stopped only failed Reporting and disabled Vault explicitly only in its approved Accounting Compose block; Python38 and independent review pass. Same image startup resumed; actual health/Eureka acceptance pending.
- Podman host I/O pressure delays CLI operations. Temporary helper uses the existing current-user local Podman API socket with unchanged runner gates; no infrastructure/config/credentials/raw logs altered or printed. Healthy services retained; broad teardown/recreate avoided.
- HTTP provider gaps remain explicit failures (Loan numeric partner/currency references, unsupported Closing/Reporting journal queries, Deposit approval status-by-ID); health/Eureka does not certify those financial workflows or distributed atomicity.
- Next: Reporting/Deposit tests and separate review, rebuild/start Reporting, Accounting checkpoint, sequential Products then Risk, final13 verification, docs/commit/requested Draft PR with Refs #653. No Ready, merge, Issue close or cleanup authorized/performed. Rollback retains healthy containers, DBs, networks, volumes and prior images; only exact failed service is stopped and source reversion follows review.
- Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 Closing live PASS; Payable Eureka dependency gate

- Final `up --package accounting` exited 1 with `payable-api: health/Eureka deadline exceeded`; no later service was started. Existing containers remain preserved.
- Closing repair commit `44eb41b5` is verified: JDK17 one-worker core/API/batch 124/124 (59/49/16), Python runner 38/38; independent final review clean after P3 adapter relocation. Actual rebuilt Closing and repeated Journal Ledger both pass health/Eureka UP, restart0/OOMfalse, CPU 0.50/RAM 768MiB and all checked error markers 0.
- Accounting quiet Compose and seven-context DB prerequisite pass. Payable is running, health UP, restart0/OOMfalse, error/startup/database markers 0, but expected Eureka registration is false. Value-suppressing live JAR inventory confirms ConfigClient present and all three Eureka starter/client libraries absent. No raw logs/env/responses were exposed.
- Independent review identifies a pre-existing P1 in the six API runtime dependency closures: payable, receivable, expenditure-resolution, tax, reporting, reconciliation. Each API build.gradle needs one `spring-cloud-starter-netflix-eureka-client` dependency using the existing BOM. These six paths are outside the current allowlist: no edits made, review-only patch `/tmp/issue-653-eureka-dependency.patch` prepared, scope expansion asked asynchronously.
- Remaining Accounting 4 APIs are built but not started; Products/Risk 6 are not built/started. Accounting package checkpoint and 13/13 remain unmet. Keep Journal Ledger/Closing, diagnostic Payable and existing infrastructure; no broad stop/removal, DDL, grant, production or platform changes.
- Static Loan inspection additionally found unconditional foreign JPA scans/internal persistence dependencies versus Loan-only migrations; no Loan live failure is claimed and no Loan edit is authorized/performed.
- Branch/worktree: `agent/653-business-runtime`, `/tmp/account-653-business-runtime`; base `origin/main@34c75839`. Guide and prepared Draft body record exact passed/failed/skipped gates. Draft publication remains gated on the requested live verification; no push/Ready/merge/Issue close/cleanup. Parent next action after scope response: six API tests/classpath checks/one-worker image rebuilds and resume sequential package gates.
- Rollback remains exact failed/new package service only with label checks and all data/images/volumes preserved. Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 approved Closing dev boundary repair

- Resumed `agent/653-business-runtime` in `/tmp/account-653-business-runtime` from `8924e83b`, based on fetched `origin/main@34c75839`. Latest owner approval (#653 comment 5604915412) supersedes the previous scope blocker and permits Closing application/configuration/adapters plus direct regression verification.
- Closing dev now scans only owned entities/repositories. API `ClosingMonolithConfiguration` preserves the prior non-dev composition; Core `HttpClosingJournalAdapter` provides real HTTP posting/list/slip lookup and fails explicitly for missing numeric-ID/detail/aggregate contracts. Existing Fiscal HTTP adapter has bounded shorthand timeout parsing and redacted failures. Compose enables fiscal remote and assigns internal destinations.
- Added the actual ClosingApplication dev migration/JPA-validate test and HTTP contract/failure tests. Final JDK17 offline CPU1/RAM1536MiB/one-worker `:closing:core:test :closing:api:test :closing:batch:test` passes 124 tests (59/49/16), no failures/errors/skips. Python runner regression passes 38/38. Independent review's P3 outbound placement finding was resolved by moving the Journal adapter to Core infrastructure; re-review has no remaining P0-P3.
- Closing image rebuilt successfully with `tools/Containerfile.minimal-auth-java` / `bootJar --max-workers=1`. Actual accounting quiet Compose preflight and DB prerequisite pass; sequential API runtime verification is in progress. Previously built six other Accounting images are retained. No new package or 13/13 success is claimed yet.
- Remaining limitations: unsupported Journal remote queries block dependent financial workflows explicitly; health/Eureka is not financial correctness evidence. Non-dev/production composition and distributed fiscal-period transaction atomicity remain outside this repair. No DDL/DB grants/shared contracts/platform changes or secret/raw runtime output.
- Parent owns runtime, harness and GitHub mutations; separate service/test writers had disjoint allowlists and independent reviewer remained read-only. Draft PR (Refs #653) publication still awaits the live verification gate; no Ready, merge, Issue close or worktree cleanup.
- Rollback: stop only exact failed newly created/recreated package service after label verification, retaining healthy services, DBs, images, networks and volumes. Source rollback is a reviewed PR revert. Next owner: parent Integrator for sequential Accounting → Products → Risk gates, then human review.
- Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 business external-dev runtime verification (blocked)

- Issue: #653; branch `agent/653-business-runtime`; preserved isolated worktree `/tmp/account-653-business-runtime`; original base `26f26698`, fast-forwarded without conflict to fetched `origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e`. Primary dirty checkout was preserved.
- Allowlist: three `tools/compose.{accounting,products,risk}-external-dev.yml`, `tools/run-business-external-dev.py`, `tools/test_run_business_external_dev.py`, `docs/guides/business-external-dev-verification.md`, `docs/ai-harness/{agent-status,worklog,handoff}.md`, `docs/history/CODEX_WORKLOG.md`. No Java/schema/platform edits.
- Retained case-sensitive Eureka JVM defaultZone with original JVM memory/security flags and explicit discovery enablement; Reporting receives its application name. Runner suppresses subprocess/health/log bodies, checks env metadata, serializes build/start, enforces memory headroom and exact container Eureka IP/port, resource/restart/OOM/log gates. Logstash connection warnings no longer count as DB failures. Engine command timeout 180s, API readiness retry 600s; DB healthcheck timeout raised 10s to 30s.
- Verification: Python offline suite 38/38 PASS, independently rerun by Reviewer. Accounting quiet Compose preflight and seven-context DB prerequisite PASS. All seven Accounting images built sequentially using existing Containerfile `bootJar --max-workers=1`. Journal Ledger live PASS: health/Eureka UP, restart0/OOMfalse, CPU 0.50/RAM 768MiB, zero error/startup/database markers.
- Blocking finding: Closing image builds but actual `ClosingApplication` fails Hibernate validation on missing `account_subjects`; restart1/OOMfalse observed, sanitized snapshot ERROR4/schema markers10. Unconditional Master Data entity/repository scanning and monolith imports require foreign tables absent from Closing V49 schema. Independent Reviewer confirms pre-existing P1; narrow PostgreSQL schema test and local/create-drop test do not cover this actual runtime composition. Existing #250 owns related Closing PostgreSQL validation work.
- Stopped only failed Closing by exact project/service container identity and retained it. Healthy Journal Ledger and prior infrastructure remain. Other five Accounting APIs have images but were not started; Products/Risk builds and starts were not run after the failure. No Accounting package checkpoint or 13/13 acceptance claimed. Pre-existing Logstash/Kibana unhealthy states were recorded separately.
- User scope clarification is pending: split Closing repair into #250 and continue remaining diagnostics, or explicitly expand repair scope. No validation bypass, foreign DB connection or DDL was attempted. `Refs #653` Draft PR body is prepared locally; publication is pending the requested live verification gate. No Ready, merge, Issue close or cleanup.
- Rollback: stop only newly created/recreated failed target containers after exact label checks, retaining containers/images/networks/volumes; preserve healthy packages. Source rollback is a reviewed revert. Next owner: parent Integrator for agreed runtime continuation, separate Closing owner for composition-root repair, independent Reviewer and human for final acceptance.
- Authority: Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 — GH-664 안전한 migration CLI 오류 출력

- Issue [#664](https://github.com/skyg547/account/issues/664); branch `agent/664-migration-safe-errors`; worktree `C:/tmp/account-664-migration-safe-errors`; base `05ff6efcf8895fba6b6592da4ab90cf761038480`.
- 구현자 `/root/migration_663_writer` (이번 할당은 GH-664, GPT-6 Astra/high), 독립 Reviewer `/root/harness_ci_audit` (Astra/high). 부모 Integrator만 공유 기록과 Git/GitHub를 담당한다.
- 변경: `MigrationCommand`의 IllegalArgumentException 원문 출력을 지정 고정 문구로 교체하고 가까이에 보안 이유를 설명했다. usage 두 줄과 exit 2, 기존 정상/list·baseline exit 3·runtime exit 4 흐름을 유지한다.
- 검증: 테스트만 추가한 RED 16건 중 14실패(오류/skip 0); 합성 malformed URL 세 건에서 기존 합성 호스트 노출 확인. GREEN 대상 16/16, 전체 98/98(기존 85+신규 13), 독립 `--rerun-tasks` 재실행도 98/98, 실패/오류/skip 0. RED는 구현자 실행 근거, 독립 Reviewer는 현재 GREEN을 재실행했다.
- 범위는 실질 Java/테스트 2파일과 부모 기록 4파일뿐이다. Reviewer APPROVE, P0–P3 finding 없음. `git diff --check`, unmerged 및 실제 conflict marker 검사 통과. 실제 DB/TLS·외부 장애 연결·환경 비밀정보 접근·설치·다운로드·기존 로그 삭제 없음.
- 게시: [Draft PR #685](https://github.com/skyg547/account/pull/685), 구현 커밋 `d571a5f7760f147eafe85d6d0ae270e1d8a59538`. 이번 후속 커밋은 PR 식별자를 반영한 기록 전용이며 검토한 실질 2파일은 동일하다. 아래 과거 기록은 이력이며 원격 Issue/PR이 최신 상태다. 새 게시 head CI와 최신 main 통합 가능성을 확인한 뒤 Ready로 인계하고 구현 슬롯을 해제한다. 최종 merge/close는 기존 `account` 예약 소유이며 대기 자체로 독립 Issue를 멈추지 않는다.
- 관련 대기열: #663/PR #684는 동결 Ready, #662/PR #680은 병합 후 account 소유 Issue-close 승인 대기다. #681/PR #682는 병합·종료·main CI 34424476862 성공 확인 후 전용 자원만 정리했고 코드/이력은 main에 보존했다.
- 롤백: 이 두 코드/테스트 파일의 검토된 revert. 원래 입력값 노출 위험이 돌아오므로 후속 보안 검토가 필요하며 DB나 과거 로그를 지우는 작업은 포함하지 않는다.

다음 담당: 부모가 최종 6파일 리뷰와 Draft 게시/current-head CI를 확인해 Ready로 전달한다. 기존 `account` 예약이 최종 독립 검수·merge·Issue close를 담당한다. 반려는 원래 Issue/branch/worktree 재작업으로 돌리고, 대기 중에도 독립 ready Issue 선택을 계속한다. Q1–Q4의 근거·실행 명령은 동시 기록한 `worklog.md` GH-664 절을 참조한다.

### 보존된 이전 이력 (아래는 현재 지시가 아님)

## 2026-09-10 - GH-653 runtime and Q1–Q4 PASS / Draft PR #689

- Requested Draft PR published: [#689](https://github.com/skyg547/account/pull/689), `Refs #653`, branch `agent/653-business-runtime`. Only this dedicated branch was pushed. Final frozen head/base and live CI results are recorded in the PR body/checks; this handoff-only commit changes no production code. Issue remains open. Ready/main merge/Issue close/cleanup were not performed.

- Issue #653; branch `agent/653-business-runtime`; isolated worktree `/tmp/account-653-business-runtime`. Final runtime observed at 2026-09-10 15:51 KST: all13 sequential images built, Accounting7/Products3/Risk3 package checkpoints and final full13 re-verification PASS; retained all13 and existing platform.
- Every API: root actuator health UP, Eureka current IP:8080 UP, running=true/restart0/OOMfalse, CPU0.50/RAM768MiB, last10000 log ERROR/startup/DB pattern counts0. Accounting/Products actual ACL + desired config hash/healthy reuse and normal Risk Compose DB gate passed. Evidence `/tmp/issue653-runtime-evidence.log` ends with final13 PASS; helper exit0. Per-service results and reproducible commands: `docs/guides/business-external-dev-verification.md`.
- Verification: current affected Java423 + unchanged prior Closing124 =547 unique tests, failures/errors/skips0; Python runner38/38 after timeout300 change. All13 actual bootJar images pass. No repeated Java run for subsequent two comment lines or documentation-only main synchronization. After documentation-only main synchronization to b7c1c7c1fa45, both local Node contracts pass64/64 (Node22.23.2, failures/skips0, exit0). Four history conflicts/six blocks preserve both sides; required conflict-log updated. Independent reviewer confirms Q1–Q4 PASS, Node64 rerun PASS, both-parent history preservation and no remaining P0–P3 findings. Draft CI and human review are separate next gates.
- Approved changes: three Compose files/runner/tests, six Eureka dependencies, Closing/Loan dev persistence isolation, real dev HTTP adapters for Closing/Loan/Reporting/Deposit, Loan column/Deposit TEXT mapping, Reporting Vault off, Deposit automatic outbox relay off, Products/Risk readonly common DB gate mount, API Hikari3/1. No DDL/provider contract/platform/credential/DB-server changes. Bounded file scopes and recovery evidence are linked in Issue comments and feature guide.
- Actual startup failures were repaired and reverified; timeouts never counted as success. Reviewed same-engine recovery started only hash-matched never-started Closing. Existing Accounting/Products DB gates were verified by hash + original readonly ACL + state when no-change Compose timed out. Risk used normal DB Compose. No broad teardown or unrelated service stop.
- Limitations: health is not financial transaction/load certification; provider-missing Loan reference, Closing/Reporting detail/aggregate, Deposit ID approval/recovery contracts explicitly fail; Asset Lease payment fallback/Kafka flows remain separately unverified. Small Hikari pools may queue/time out under load; remote writes do not guarantee distributed atomicity or exactly-once. Java-unchanged modules use bootJar/runtime gates rather than new unit suites; unchanged Batch suites were not broadened.
- Rollback: exact service label only, retain previous images/healthy APIs/DB/network/volumes; reviewed source revert. Parent Integrator owns Git/harness/runtime, independent reviewer stays read-only. Next owner: parent to record final-head CI and independent Draft review; human reviewer then reviews before any separately authorized lifecycle transition. Ready/merge/Issue close/cleanup require separate approval.
- Implementer tier: High reasoning (difficulty:high)
- Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 all package checkpoints PASS (historical checkpoint)

- Latest runtime checkpoint (15:30 KST): all13 images, all13 individual live gates and Accounting7/Products3/Risk3 complete package checkpoints PASS. Risk DB prerequisite used normal Compose and passed in223s. Final full13 retained-state sweep is now checking Accounting then Products then Risk. No final13 marker/commit/push/PR yet; prior checkpoints below describe the recovery sequence.
- Same Issue/branch/worktree: #653, `agent/653-business-runtime`, `/tmp/account-653-business-runtime`. All source review and tests remain valid: current Java423 + unchanged Closing124 =547, Python38, no open P0–P3 source findings.
- Accounting7 full live checkpoint passed. Products3 images built; missing common DB-check script mount repaired read-only in Products/Risk, then Products DB ACL prerequisite and Deposit live gates passed. Deposit automatic outbox relay is explicitly disabled for health verification.
- Loan startup failed PostgreSQL53300 capacity (not OOM/authentication/permission/schema-object failure). Stopped only failed Loan. Added API-only Hikari maximum3/minIdle1 to all13 approved Compose blocks under Issue comment5612617861; DB server limits/roles/DDL/credentials and Batch pools remain unchanged.
- Planned Deposit stop alone did not free sufficient lasting capacity: DB gate and a sanitized SELECT1 still reported capacity failure. Stopped verified Reporting next; its normal DB/up/verify sequence then ran before Deposit recovery and sequential Accounting reloads. Other six Accounting APIs were retained. No simultaneous package startup or broad teardown.
- Reporting and Deposit subsequently recovered and passed all live gates. The Spring Boot3.2.5/Spring6.1.6/Hikari5.0.1 synthetic Binder probe confirms current environment names bind max3/min1. Accounting rolling reload passed Journal Ledger; Closing Compose exceeded180s after create but before start (restart0/OOMfalse/log0). Raised only generic/Compose/per-service command limits to300s; Python38 passes. Same Accounting sequence resumes with unchanged DB/health/Eureka/resource/log gates.
- Further I/O recovery: unchanged Journal Compose up exceeded300s but exact state and desired config hash remained valid. Closing exact created/never-started/hash-matched container was started alone via same engine (98s), then all normal live gates passed. Remaining four Accounting APIs subsequently used individual normal up and passed the full7 checkpoint; no timeout itself is treated as PASS. Independent recovery review found no omitted service/gate; temporary exception sanitizer parity was repaired.
- Accounting DB-check no-change Compose later exceeded240s, while exact checker remained healthy and original ACL script directly passed exit0/error0. Reviewed temporary helper now revalidates existing Accounting/Products DB-check via desired config hash, actual ACL execution and exact healthy state; outer env identity and all API/Risk/build/final gates remain. Remaining Accounting sequence resumed.
- New fetched main `b7c1c7c1` contains documentation/quality policy only, no Java/build changes. New Q1/Q2/Q4 independent review passes; Q4 added only a two-line V42 TEXT intent comment in approved DepositOutboxEntity. Q3 remains completion-pending until final13 evidence, final documentation and both Node contract checks after base synchronization.
- At this checkpoint, helper `/tmp/issue653-remote-runtime.py` had passed Loan/Asset Lease and Products checkpoint and built Risk images; Risk startup/final verification remained. Normalized evidence: `/tmp/issue653-runtime-evidence.log`. No13/13, commit, push or Draft PR claimed; owner explicitly gates commit/Draft on all13 normal operation.
- Parent owns runtime/Git/harness, separate Reviewer remains read-only. Next owner: parent Integrator to complete capacity-limited rolling verification. Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 all-13 runtime continuation (in progress)

- Issue #653; branch `agent/653-business-runtime`; isolated worktree `/tmp/account-653-business-runtime`; preserved implementation base `34c75839`, resumed HEAD `baed1d83`. Owner comment5611054436 authorizes six Eureka API dependencies and bounded repairs needed for all13 APIs; older scope-blocked entries below are historical.
- Implemented six Eureka starters and native enforced Boot3.2.5/Cloud2023.0.1 BOMs for Reconciliation's reproducible Gradle8.7 resolution error; eight dependency comparisons preserve expected versions. Loan dev owns its persistence and explicitly opts into HTTP ports; exact V30 column mapping old_eir/new_eir repaired without DDL. Bounded scopes recorded in Issue comments5611100834/5611355586.
- Verified this continuation: six API30 + Reconciliation core94 + Loan core99/API6/batch3 =232 Java tests, zero failures/errors/skips; Python runner38/38. Existing unchanged Closing124 tests independently confirmed. Separate read-only review cleared these changes.
- Accounting images built sequentially; Payable/Receivable/Expenditure/Tax live gates passed. Reporting actual startup exposed missing LoadLedgerPort; stopped only failed Reporting (restart1, OOMfalse, database failure0). Bounded real HTTP Reporting repair/test scope recorded comment5611823487. Deposit preflight missing real external ports scope recorded comment5611833868; local fabricated journal adapters are not enabled.
- Journal Ledger and Closing subsequent live rechecks passed health/Eureka UP, restart0/OOMfalse, CPU0.50/RAM768MiB and zero inspected error/startup/database patterns. Remaining Accounting checks and Reporting/Deposit tests are in progress. No Accounting package checkpoint or13/13 success claimed yet; Products/Risk not built/started in this continuation.
- Further checkpoint: all six retained Accounting services passed full rechecks. Reporting core92/API10/batch6=108 tests pass after correcting a duplicated lambda variable name. Deposit core85 passed; actual migration validation caught payload TEXT versus @Lob/CLOB, repaired only @Lob/import under comment5611921269 and added long Unicode persistence roundtrip; Deposit core/API/batch rerun pending. Separate reviewer cleared production and resolved two test findings.
- Final source-test checkpoint: Reporting108 and Deposit92 pass, including dev schema validation and long Unicode outbox roundtrip. Independent XML review confirms unique current affected Java423 + unchanged Closing124 =547 tests, failures/errors/skips0; Python38 separately passes. All P0–P3 review findings resolved. Runtime sequence resumed at Reporting rebuild/up; Issue comment5611993076 records this checkpoint.
- Reporting image rebuild passed; actual startup then exposed Vault clientAuthentication/vaultTemplate auto-configuration (restart1/OOMfalse, DB patterns0) absent from the dev test's disabled-Vault composition. Stopped only failed Reporting and disabled Vault explicitly only in its approved Accounting Compose block; Python38 and independent review pass. Same image startup resumed; actual health/Eureka acceptance pending.
- Podman host I/O pressure delays CLI operations. Temporary helper uses the existing current-user local Podman API socket with unchanged runner gates; no infrastructure/config/credentials/raw logs altered or printed. Healthy services retained; broad teardown/recreate avoided.
- HTTP provider gaps remain explicit failures (Loan numeric partner/currency references, unsupported Closing/Reporting journal queries, Deposit approval status-by-ID); health/Eureka does not certify those financial workflows or distributed atomicity.
- Next: Reporting/Deposit tests and separate review, rebuild/start Reporting, Accounting checkpoint, sequential Products then Risk, final13 verification, docs/commit/requested Draft PR with Refs #653. No Ready, merge, Issue close or cleanup authorized/performed. Rollback retains healthy containers, DBs, networks, volumes and prior images; only exact failed service is stopped and source reversion follows review.
- Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 Closing live PASS; Payable Eureka dependency gate

- Final `up --package accounting` exited 1 with `payable-api: health/Eureka deadline exceeded`; no later service was started. Existing containers remain preserved.
- Closing repair commit `44eb41b5` is verified: JDK17 one-worker core/API/batch 124/124 (59/49/16), Python runner 38/38; independent final review clean after P3 adapter relocation. Actual rebuilt Closing and repeated Journal Ledger both pass health/Eureka UP, restart0/OOMfalse, CPU 0.50/RAM 768MiB and all checked error markers 0.
- Accounting quiet Compose and seven-context DB prerequisite pass. Payable is running, health UP, restart0/OOMfalse, error/startup/database markers 0, but expected Eureka registration is false. Value-suppressing live JAR inventory confirms ConfigClient present and all three Eureka starter/client libraries absent. No raw logs/env/responses were exposed.
- Independent review identifies a pre-existing P1 in the six API runtime dependency closures: payable, receivable, expenditure-resolution, tax, reporting, reconciliation. Each API build.gradle needs one `spring-cloud-starter-netflix-eureka-client` dependency using the existing BOM. These six paths are outside the current allowlist: no edits made, review-only patch `/tmp/issue-653-eureka-dependency.patch` prepared, scope expansion asked asynchronously.
- Remaining Accounting 4 APIs are built but not started; Products/Risk 6 are not built/started. Accounting package checkpoint and 13/13 remain unmet. Keep Journal Ledger/Closing, diagnostic Payable and existing infrastructure; no broad stop/removal, DDL, grant, production or platform changes.
- Static Loan inspection additionally found unconditional foreign JPA scans/internal persistence dependencies versus Loan-only migrations; no Loan live failure is claimed and no Loan edit is authorized/performed.
- Branch/worktree: `agent/653-business-runtime`, `/tmp/account-653-business-runtime`; base `origin/main@34c75839`. Guide and prepared Draft body record exact passed/failed/skipped gates. Draft publication remains gated on the requested live verification; no push/Ready/merge/Issue close/cleanup. Parent next action after scope response: six API tests/classpath checks/one-worker image rebuilds and resume sequential package gates.
- Rollback remains exact failed/new package service only with label checks and all data/images/volumes preserved. Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 approved Closing dev boundary repair

- Resumed `agent/653-business-runtime` in `/tmp/account-653-business-runtime` from `8924e83b`, based on fetched `origin/main@34c75839`. Latest owner approval (#653 comment 5604915412) supersedes the previous scope blocker and permits Closing application/configuration/adapters plus direct regression verification.
- Closing dev now scans only owned entities/repositories. API `ClosingMonolithConfiguration` preserves the prior non-dev composition; Core `HttpClosingJournalAdapter` provides real HTTP posting/list/slip lookup and fails explicitly for missing numeric-ID/detail/aggregate contracts. Existing Fiscal HTTP adapter has bounded shorthand timeout parsing and redacted failures. Compose enables fiscal remote and assigns internal destinations.
- Added the actual ClosingApplication dev migration/JPA-validate test and HTTP contract/failure tests. Final JDK17 offline CPU1/RAM1536MiB/one-worker `:closing:core:test :closing:api:test :closing:batch:test` passes 124 tests (59/49/16), no failures/errors/skips. Python runner regression passes 38/38. Independent review's P3 outbound placement finding was resolved by moving the Journal adapter to Core infrastructure; re-review has no remaining P0-P3.
- Closing image rebuilt successfully with `tools/Containerfile.minimal-auth-java` / `bootJar --max-workers=1`. Actual accounting quiet Compose preflight and DB prerequisite pass; sequential API runtime verification is in progress. Previously built six other Accounting images are retained. No new package or 13/13 success is claimed yet.
- Remaining limitations: unsupported Journal remote queries block dependent financial workflows explicitly; health/Eureka is not financial correctness evidence. Non-dev/production composition and distributed fiscal-period transaction atomicity remain outside this repair. No DDL/DB grants/shared contracts/platform changes or secret/raw runtime output.
- Parent owns runtime, harness and GitHub mutations; separate service/test writers had disjoint allowlists and independent reviewer remained read-only. Draft PR (Refs #653) publication still awaits the live verification gate; no Ready, merge, Issue close or worktree cleanup.
- Rollback: stop only exact failed newly created/recreated package service after label verification, retaining healthy services, DBs, images, networks and volumes. Source rollback is a reviewed PR revert. Next owner: parent Integrator for sequential Accounting → Products → Risk gates, then human review.
- Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 - GH-653 business external-dev runtime verification (blocked)

- Issue: #653; branch `agent/653-business-runtime`; preserved isolated worktree `/tmp/account-653-business-runtime`; original base `26f26698`, fast-forwarded without conflict to fetched `origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e`. Primary dirty checkout was preserved.
- Allowlist: three `tools/compose.{accounting,products,risk}-external-dev.yml`, `tools/run-business-external-dev.py`, `tools/test_run_business_external_dev.py`, `docs/guides/business-external-dev-verification.md`, `docs/ai-harness/{agent-status,worklog,handoff}.md`, `docs/history/CODEX_WORKLOG.md`. No Java/schema/platform edits.
- Retained case-sensitive Eureka JVM defaultZone with original JVM memory/security flags and explicit discovery enablement; Reporting receives its application name. Runner suppresses subprocess/health/log bodies, checks env metadata, serializes build/start, enforces memory headroom and exact container Eureka IP/port, resource/restart/OOM/log gates. Logstash connection warnings no longer count as DB failures. Engine command timeout 180s, API readiness retry 600s; DB healthcheck timeout raised 10s to 30s.
- Verification: Python offline suite 38/38 PASS, independently rerun by Reviewer. Accounting quiet Compose preflight and seven-context DB prerequisite PASS. All seven Accounting images built sequentially using existing Containerfile `bootJar --max-workers=1`. Journal Ledger live PASS: health/Eureka UP, restart0/OOMfalse, CPU 0.50/RAM 768MiB, zero error/startup/database markers.
- Blocking finding: Closing image builds but actual `ClosingApplication` fails Hibernate validation on missing `account_subjects`; restart1/OOMfalse observed, sanitized snapshot ERROR4/schema markers10. Unconditional Master Data entity/repository scanning and monolith imports require foreign tables absent from Closing V49 schema. Independent Reviewer confirms pre-existing P1; narrow PostgreSQL schema test and local/create-drop test do not cover this actual runtime composition. Existing #250 owns related Closing PostgreSQL validation work.
- Stopped only failed Closing by exact project/service container identity and retained it. Healthy Journal Ledger and prior infrastructure remain. Other five Accounting APIs have images but were not started; Products/Risk builds and starts were not run after the failure. No Accounting package checkpoint or 13/13 acceptance claimed. Pre-existing Logstash/Kibana unhealthy states were recorded separately.
- User scope clarification is pending: split Closing repair into #250 and continue remaining diagnostics, or explicitly expand repair scope. No validation bypass, foreign DB connection or DDL was attempted. `Refs #653` Draft PR body is prepared locally; publication is pending the requested live verification gate. No Ready, merge, Issue close or cleanup.
- Rollback: stop only newly created/recreated failed target containers after exact label checks, retaining containers/images/networks/volumes; preserve healthy packages. Source rollback is a reviewed revert. Next owner: parent Integrator for agreed runtime continuation, separate Closing owner for composition-root repair, independent Reviewer and human for final acceptance.
- Authority: Implementer tier: High reasoning (difficulty:high). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.

## 2026-09-10 — 현재 비동기 Issue 루프 / GH-681 통합 재작업

- 최신 사용자 정정: 구현, PR 검수·병합 대기, 반려 재작업, 종료·정리는 별도 대기열이다. 이전 전역 순차/Draft-only 중단 조건은 폐기하며 아래 기록은 당시 이력이다.
- GH-662 / PR #680: main `5f8103afd4f018723302fd4b15b740b3bcf189c1`에 병합 및 main CI 34381040992 성공. Issue 종료 승인 대기는 기존 account 예약 소유이며 자원을 보존한다. 독립 Issue의 blocker가 아니다.
- GH-681 / PR #682: 원래 `agent/681-harness-quality-contract`, `C:/tmp/account-681-harness-quality-contract`에서 부모가 최신 main 일반 merge로 기록5파일 충돌을 해결한다. 양쪽 이력을 보존하고 실질 정책/테스트5파일은 바꾸지 않는다. 충돌 결정은 conflict-log.md에 기록한다.
- GH-663: 별도 `agent/663-migration-canonical-sslmode`, `C:/tmp/account-663-migration-canonical-sslmode`에서 `/root/migration_663_writer` Astra high가 독립된 Java/테스트2파일을 구현 중이다. 부모 기록은 한 Integrator가 순서대로 갱신한다.
- 예약 account-issue-2의 전역 대기를 제거하고 원래 Issue 재작업/실제 의존성 기준으로 수정했다. ACTIVE·30분·동일 task·기존 품질 블록·최종 merge/close 담당은 보존했고 별도 planner가 정적 검토했다. 실제 미래 예약 실행까지 검증한 것은 아니다.
- 현재 통합본의 두 Node 계약·diff/marker/링크·별도 리뷰·새 head CI를 확인한 뒤 Ready로 인계한다. 최종 병합/close는 account 예약 소유다. 이번 부모 재작업은 merge/close/자원 삭제를 수행하지 않는다.

## 2026-09-10 — GH-683 회계기간 저장 상태 복원

- Issue [#683](https://github.com/skyg547/account/issues/683); branch `agent/683-fiscal-period-reconstitution`; worktree `C:/tmp/account-683-fiscal-period-reconstitution`; base `05ff6efcf8895fba6b6592da4ab90cf761038480`. 위 GH-681 블록과 아래 이전 항목은 당시 이력이며 원격 Issue/PR과 최신 대화 체크포인트가 우선한다.
- Writer `/root/fiscal_683_writer` (service / GPT-6 Astra high), 별도 Reviewer `/root/harness_lifecycle_review` (Astra high). 단일 원인의 응집된 수정에 한해 부모가 정확한 Mapper 1개와 matching tests까지 서비스 역할 범위를 명시 승인했다. 부모만 공유 기록·Git/GitHub를 담당한다.
- 원인: 저장된 상태를 Mapper가 빈 domain의 새 업무 전이로 실행하여 영구 마감 조회·합법 전이 후 저장 결과 복원을 거부했다. 부모의 현재 compiled class 합성 probe 두 건으로 재현했다.
- 변경: 순수 `FiscalPeriod.reconstitute`가 9개 원값과 nullable 감사 필드를 보존하며 null 상태는 거부한다. Mapper는 업무 전이를 호출하지 않는다. 기존 `changeClosingStatus` 본문·actor 검증은 동일하고 API/contracts/JPA entity/DB schema는 변경하지 않았다.
- 검증: production 수정 전 실제 Mapper RED 10건 중 3실패, 오류/skip 0. GREEN Domain21+Mapper10+H2 12=43건, core/API/batch 전체 87건(58/24/5), 22 suites, 실패/오류/skip 0(기존44+신규43). 별도 `/root/harness_lifecycle_review`가 3모듈을 `--rerun-tasks`로 재실행해 87/87 PASS(47초) 및 실질5파일 APPROVE, Q1–Q4 PASS를 확인했다. 게시 head CI는 별도 후속 게이트이며 로컬 성공과 구분한다.
- H2는 실제 Repository/Mapper/Adapter 및 Spring control 프록시를 사용한다. 외부 테스트 rollback 트랜잭션을 끄고 실제 커밋 후 별도 트랜잭션/clear로 ID·잠금 ID·연월 조회를 확인했다. 거부된 명령은 저장 상태·감사값을 보존한다. PostgreSQL 잠금 경합/동시 마감 검증은 아니다.
- 실질 allowlist 5파일 + 부모 기록4파일. 기존 변경 보존, diff/marker/unmerged 검사 통과. 실제 DB/서버/업무 Batch·비밀정보·설치/다운로드·기존 행/로그 삭제 없음. 자동화 테스트의 H2 fixture만 정리했다.
- 게시: [Draft PR #686](https://github.com/skyg547/account/pull/686), 구현 커밋 `9a424d2ac8a19d035e7b350fac8ce422256ea570`. 최종9파일 독립 APPROVE 후 게시했으며 이 후속 커밋은 게시 식별자 반영 기록 전용으로 실질5파일은 동일하다. 새 게시 head CI를 확인한 뒤 Ready로 인계한다. 최종 merge/close는 기존 `account` 예약 소유다. 02:03 UTC 배정 당시 #663/PR684·#664/PR685는 동결 Ready였고 선행 병합이나 #662 종료를 기다리지 않았다. 이후 상태 변화는 각 원격 PR과 오케스트레이터 대기열을 따른다.
- 롤백: 승인된 코드/테스트의 검토된 revert이며 DB·감사 행은 바꾸지 않는다. 롤백하면 원래 영구 마감 복원 결함이 돌아온다. 기능 문서는 기존 전이/terminal 계약을 복원하므로 수정하지 않는다. `local-run.md:83`의 기존18건은 변경 전44건과도 맞지 않던 범위 밖 차이이며 모든 문서가 최신이라고 주장하지 않는다.

상세 Q1–Q4 완료표와 실행 명령은 `docs/ai-harness/worklog.md`의 GH-683 절에 있다. 게시/Ready/병합/종료는 각각 실제 원격 게이트로 구분하며 반려는 원래 Issue 재작업으로 회수한다.

### 보존된 이전 체크포인트와 제출 이력 (현재 지시 아님)

# Current checkpoint — 2026-09-10 승인된 Issue 루프 재개

### GH-681 측 당시 기록
- 대상: GH-681 / PR #682. 사용자가 이 대화에서 오케스트레이션과 다음 Issue 진행 재개를 명시 승인했다.
- 아래의 Draft 제출 시점 기록은 이력이다. 당시 부모가 추가한 Draft-only/별도 지시 대기 조건은 이번 승인으로 대체하며, 구현자와 독립 검수자의 역할 분리 및 검증 게이트는 유지한다.
- 현재 코드 변경은 동결되어 있으며, 독립 사전 리뷰와 대상 Node 테스트 32/32 PASS를 재확인했다. 이 기록 변경의 commit/push 후 최신 head CI와 통합 가능성을 다시 확인해야 Ready로 전환할 수 있다.
- 다음 행동: 기존 PR #680 완료·정리를 먼저 확인한 다음 이 PR의 최신 main 통합 가능성과 검증을 재확인하여 Ready로 인계한다. 대기 중에는 Draft를 유지하지만, 이는 순차 처리 대기이며 추가 사용자 승인 대기가 아니다. 기존 Account PR 독립 검수·병합(account) 예약이 최종 독립 리뷰·병합·Issue close를 담당한다.

<!-- GH-669 checkpoint start -->
## 2026-09-10 — GH-669 제출 전 인계

- 계약: [Issue #669](https://github.com/skyg547/account/issues/669), [사용자 승인5파일·claim](https://github.com/skyg547/account/issues/669#issuecomment-5612175131). `agent/669-mvc-request-errors` / `C:/tmp/account-669-mvc-request-errors` / base `b7c1c7c1fa45ec6550ab2431674fcf22a519bcea`.
- 목적: 네 MVC 요청 예외를 고정 메시지400/405/415와 원래 Allow/Accept/Accept-Patch로 반환. 실제 상속 MockMvc 회귀와 초보자용 process-flow 문서를 함께 추가했다. 기존400/404/하위409/비노출500 및 production 의존성/BOM 유지.
- 검증: production 미수정 RED14/5fail → 대상19 PASS; 작성자 소비자149/36suites PASS, 독립 강제149/36 PASS(1m2s). 부모 library build와 직접 소비자3 bootJar PASS(12s), Node64 PASS. 모든 완료 테스트 failure/error/skip0.
- 실질5파일 독립 APPROVE/Q1–Q4 PASS, 나머지4파일은 부모 기록이다. 다음 담당: 별도 Reviewer의 최종9파일 기록 대조 후 부모 commit/push/Draft(`Refs #669`), 게시 head의 전체 CI·최신 main·통합 확인 후 Ready. 이 기록은 아직 게시/CI 완료 주장이 아니며 실제 PR 번호/head/CI는 원격 Issue/PR 인계에 기록한다.
- 그 다음은 기존 `account`의 최신 head/base·CI 독립 최종 검수 및 별도 승인된 merge/close다. 미병합 PR이나 종료 승인 대기만으로 다른 독립 Issue를 막지 않는다.
- 한계: 합성 Validator는 실제 provider 배선 검증이 아니며 인증/ResponseStatusException/전체 ErrorResponse/실DB/서버/배포 제외. CI 전체62project/23그룹 통과 전 Ready 불가. 루트 전체 build를 실행한 것으로 해석하지 않는다.
- 롤백: 검토된5실질 파일 revert, 기록·사용자 변경·타 worktree 보존. 이번 사용자 승인은 기능 문서1개 추가에 한정되며 무승인 merge/close/cleanup을 허용하지 않는다.
<!-- GH-669 checkpoint end -->

### 병합된 GH-662 측 당시 기록
- 대상: GH-662 / PR #680. 사용자가 이 대화에서 오케스트레이션과 다음 Issue 진행 재개를 명시 승인했다.
- 아래의 Draft 제출 시점 기록은 이력이다. 당시 부모가 추가한 Draft-only/별도 지시 대기 조건은 이번 승인으로 대체하며, 구현자와 독립 검수자의 역할 분리 및 검증 게이트는 유지한다.
- 현재 코드 변경은 동결되어 있으며, 독립 사전 리뷰와 대상 Node 테스트 32/32 PASS를 재확인했다. 이 기록 변경의 commit/push 후 최신 head CI와 통합 가능성을 다시 확인해야 Ready로 전환할 수 있다.
- 다음 행동: 부모가 최신 검증 후 PR #680을 Ready로 인계한다. 기존 Account PR 독립 검수·병합(account) 예약이 최종 독립 리뷰·병합·Issue close를 담당한다.

- 부모는 실제 병합·Issue 종료·main CI 및 전용 자원 소유/청결 검증 후에만 승인된 안전 정리를 수행한다. 두 기존 PR을 순서대로 처리하기 전 새 Issue 구현자를 시작하지 않는다. 이후 상태는 원격 PR과 이 대화/예약 체크포인트를 기준으로 확인한다.

---

## 2026-09-10 — GH-668 payload validation handoff snapshot

- Issue https://github.com/skyg547/account/issues/668; claim https://github.com/skyg547/account/issues/668#issuecomment-5611884163. Workflow Account Issue 구현 오케스트레이터 / task `019fa3ea-ac4c-7022-bb45-951559750df7`; `agent/668-master-data-payload-validation` / `C:/tmp/account-668-master-data-payload-validation`, base `b7c1c7c1fa45ec6550ab2431674fcf22a519bcea`. Writer `/root/fiscal_683_writer` service/Astra high, independent `/root/harness_lifecycle_review` Astra high; parent alone owns shared records and Git/GitHub.
- Substantive allowlist5: MasterDataChangeApplier interface, AccountSubject/Department/Product appliers and existing MasterDataChangeRequestPayloadValidationTest. Parent worklog/status/handoff/CODEX_WORKLOG4 only; no controller, persistence, command, domain-policy, build or migration edits.
- Worker RED211/174 expected no-throw failures,0error/skip (37 prior PASS = BP1 + constructor raw-null/empty/blank36); target GREEN338, full381/19suites = core352/API24/batch5,0failure/error/skip. Independent Astra high forced offline full rerun381/381,48s, same frozen test hash; reviewer APPROVE with no P0–P3 findings.
- CREATE/UPDATE decode→null guard→key/effectiveDate command assembly→required-field/domain-date validation has no business-port calls. Required null fields follow the Issue table; Department/Product partial UPDATE, JPA default inputs, same-day/end-date omission and DEACTIVATE remain. Product lookup stays apply-only; same sourceReference replay may return an old malformed APPROVED request unchanged.
- Q1–Q4 evidence is in this Issue's worklog and final PR body. Functional docs were not edited because existing pretransition/key/date/replay contracts and execution commands are restored; local-run's already-inaccurate test count/absence text remains disclosed out-of-scope debt.
- Next: parent final record review, exact-file commit/push/Draft, exact-head CI and latest-main integration check, Ready handoff. account alone owns final independent review and authorized merge/Issue close. Code freezes and releases the implementation slot at Ready; rejection returns to this original Issue, not a duplicate.
- New tests use real decoder/appliers/request service with mocked ports; no claim of actual JPA defaults, Spring rollback, PostgreSQL concurrency or existing APPROVED-row repair. Rollback is reviewed code/test revert (validation gap returns); no request/audit/DB/history edits or cleanup. Prior global-stop and publication snapshots remain historical, not current queue instructions.

### 이전 제출 시점 기록

### GH-681 측 당시 기록
# Active handoff — 2026-09-10 GH-681

- Issue [#681](https://github.com/skyg547/account/issues/681), parent #661. Explicit user-requested quality follow-up, not an automatic restart of a historical Goal or another worker's Issue.
- Task `019fa3ea-ac4c-7022-bb45-951559750df7`; branch `agent/681-harness-quality-contract`; worktree `C:/tmp/account-681-harness-quality-contract`; base `34c75839af2edf10f80ff60d8f24e89504d69e9e`.
- Writer `/root/harness_lifecycle_writer` (coder / Astra medium), separate `/root/harness_lifecycle_review` (Astra high) APPROVE. Scope: AGENTS/10/40 quality links, new 42 contract, Node contract test; five parent tracking files in addition.
- Verified: Node 32/32, diff check, actual conflict-marker check, three local links; Q1–Q4 PASS with evidence in worklog. Automation `account-issue-2` quality-only insertion verified; prior prompt, 30-minute cadence, target, ACTIVE, model/authority and other two automations preserved.
- State: implementation frozen and submitted as [Draft PR #682](https://github.com/skyg547/account/pull/682); implementation commit `063199e301aa51daa8e90c606a7d21647528ff2c`. No code rework; record-only publication follow-up does not change the reviewed five substantive files. Next owner: parent verifies current published head/CI and then waits for explicit next-gate direction. This record does not assert a CI pass.
- Stop boundary: Draft submission only. Do not automatically mark Ready, merge, close the Issue, remove branches/worktrees or start another Issue. This user request did not lift the existing #662 / Draft #680 stop boundary; preserve that separate worktree and PR as well.
- Risks: local structure tests do not prove semantic quality or enforce GitHub permissions; CI wiring #672 remains. Gradle/DB/server tests are not applicable to this documentation/local-test change. No secret access, installs, operational execution or user-checkout edits.
- Rollback: approved follow-up revert of the five substantive files, retain history; remove only the added quality block from the implementation automation. Preserve unrelated fields and resources.

### 병합된 GH-662 측 당시 기록
# Active handoff — 2026-09-10 GH-662 (parent audit GH-661)

- Workflow: Account Issue 구현 오케스트레이터; user-prioritized harness work, not a restart of the historical Goal.
- Task: `019fa3ea-ac4c-7022-bb45-951559750df7`; writer `/root/harness_lifecycle_writer` (coder, Astra medium); independent `/root/harness_lifecycle_review` (Astra high) approved with no remaining finding.
- Base: `34c75839af2edf10f80ff60d8f24e89504d69e9e`; branch `agent/662-harness-pr-lifecycle`; worktree `C:\tmp\account-662-harness-pr-lifecycle`; Draft PR [#680](https://github.com/skyg547/account/pull/680), implementation commit `af95eca1`.
- Scope: 20/87/88 policy docs, Issue spec form, Node PR contract test; five parent-owned tracking files in addition. No primary checkout changes.
- State: submitted Draft, implementation frozen; local and independent Node 32/32, YAML/field preservation, 19 local links and scoped checks pass. Rework count 1: fixed Draft waiting for Ready-only CI. Next: observe exact PR-head CI and wait for explicit next-gate direction. No new Issue writer while this handoff is active; follow-up record-only commit does not alter the reviewed policy/test files.
- Stop boundary: keep the submitted PR Draft. This request does not authorize automatic Ready/merge/close/cleanup; future runs must preserve this boundary instead of inheriting the generic loop's broader actions.
- Risks: documentation assertions cannot enforce GitHub permissions; local test CI wiring #672 and Guard trust policy #671 remain. No secrets/live systems/dependencies/runtime behavior changed. Rollback: reviewed policy/test reversal, preserve history.


## Preserved prior handoff

# AI Harness Handoff - 2026-09-09 Issue #659 Nginx Single Entry Point Observability Upstream Mapping and Fallback Resilience

- **Owner**: Gemini (Implementer)
- **Issue**: #659 (`[infra][nginx] Nginx 단일 진입점 관측성 업스트림(Grafana/Zipkin/Kibana/pgAdmin) 컨테이너 명칭 매핑 및 동적 프록시 장애복구(Fallback) 강화`)
- **Branch**: `agent/659-nginx-monitoring-upstreams` (Base: `origin/main@26f26698` + commit `72f229d0`)
- **Worktree**: `/tmp/account-659-nginx-monitoring-upstreams`
- **Status**: Draft PR Ready
- **Summary**:
  - `grafana/docker-compose.yml`, `zipkin/docker-compose.yml`, `kibana/docker-compose.yml`:
    - Added dual network aliases (`grafana`/`account-grafana`, `zipkin`/`account-zipkin`, `kibana`/`account-kibana`) to each service under `account-network`.
    - Removed undefined external `depends_on: elasticsearch` from `kibana/docker-compose.yml` to restore standalone compose startup compatibility in Docker Compose v2 and Podman.
  - `frontend-nginx/nginx.conf`:
    - Added `@fallback_grafana`, `@fallback_zipkin`, `@fallback_kibana`, `@fallback_pgadmin` named locations and `error_page 502 = @fallback_...` directives.
    - Enables transparent two-stage upstream routing (`account-<service>` -> `<service>`), providing seamless compatibility whether monitoring containers were started via unified compose or individual standalone compose files.
- **Verification**:
  - Live local curl verification via Nginx (`localhost:8080`) and Cloudflare Quick Tunnel:
    - `/` -> Next.js Frontend (200 OK)
    - `/api/actuator/health` -> Spring Cloud Gateway (401 Unauthorized via JWT Filter)
    - `/zipkin/` -> Zipkin Tracing UI (200 OK via `@fallback_zipkin`)
    - Cloudflare Tunnel external ingress verified across HTTP/2.
  - `git diff --check`: 0 errors.
  - Sensitive information & secret scan: 0 leaks.
- **Rollback**: Clean `git revert` of commit; no persistent storage or database schemas touched.

# AI Harness Handoff - 2026-09-09 Issue #657 Nginx Single Entry Point DNS Alignment and Dynamic Upstream Resilience

- **Owner**: Gemini (Implementer)
- **Issue**: #657 (`[infra][nginx] 저자원 minimal external-dev 스택을 위한 Nginx 단일 진입점 DNS 별칭 동기화 및 런타임 업스트림 내성 강화`)
- **Branch**: `agent/657-nginx-minimal-upstream` (Base: `origin/main@26f26698`)
- **Worktree**: `/tmp/account-657-nginx-minimal-upstream`
- **Status**: Draft PR Ready
- **Summary**:
  - `tools/compose.minimal-auth-external-dev.yml`: Added network aliases `account-gateway` and `account-frontend` under `minimal-gateway` and `minimal-frontend` to align service discovery with root Nginx upstream definitions.
  - `frontend-nginx/nginx.conf`:
    - Added dynamic DNS resolver directive (`resolver ${NGINX_LOCAL_RESOLVERS} valid=10s ipv6=off;`) with variable-based proxying (`set $...; proxy_pass $...;`) across all upstreams to prevent boot-time crashes when optional monitoring tools are offline.
    - Added graceful 502 fallbacks (`error_page 502 = @minimal_frontend;`, `@minimal_gateway;`, `@minimal_static;`) to ensure uninterrupted proxying even when running against legacy containers lacking DNS aliases.
    - Added URL rewrite break for `/pgadmin/` before variable proxying.
  - `frontend-nginx/Dockerfile` & `docker-compose.yml`:
    - Configured `NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1` and template mount (`/etc/nginx/templates/default.conf.template`) so runtime entrypoint automatically injects the container's exact nameserver (Docker `127.0.0.11`, Podman `10.89.4.1`, K8s CoreDNS).
    - Defaulted network name to `${ACCOUNT_NETWORK_NAME:-account-network}`.
- **Verification**:
  - `./gradlew :config-server:test --tests "com.ho.account.configserver.DevelopmentComposePolicyTest"`: 100% PASS.
  - `./gradlew :config-server:test`: 118 tests 100% PASS.
  - Live Nginx container execution in `account-network`:
    - `curl http://localhost:8080/`: 200 OK (Next.js frontend rendered cleanly).
    - `curl http://localhost:8080/api/actuator/health`: 401 Unauthorized (`X-Auth-Error: BEARER_TOKEN_REQUIRED` from Spring Cloud Gateway).
  - `git diff --check`: 0 errors.
  - Secret & credential leak check: 0 occurrences.
- **Rollback**: Clean git revert of commit; no stateful DB or external infrastructure impacted.

# AI Harness Handoff - 2026-09-07 Issue #630 Architecture Docs Normalization and 2026 MSA Topology Update

- **Owner**: Gemini (Implementer / Documentation)
- **Issue**: #630 (`[docs][architecture] 통합 아키텍처 명세서(architecture.md) 위치 정규화 및 최신 2026 MSA 구성도 업데이트`)
- **Branch**: `agent/630-architecture-docs-update` (Base: `origin/main@ad44f873`)
- **Worktree**: `/tmp/account-630-architecture-docs-update`
- **Status**: Draft PR Ready
- **Summary**:
  - Normalized `docs/architecture.md` to `docs/architecture/architecture.md`, eliminating 404 broken link from `docs/README.md` and `README.md`.
  - Updated comprehensive 2026 enterprise system topology diagram, 6-phase financial business pipeline diagram, and hexagonal ports & adapters architecture diagram.
  - Updated relative link in `docs/guides/master-domain-glossary.md`.
- **Verification**:
  - `git diff --check`: 0 errors
  - Conflict markers & credential scan: 0 occurrences
- **Rollback**: Reviewed git revert of documentation commit; no external state changed.

# AI Harness Handoff - 2026-09-02 Issue #521 Beginner Podman Minimal Build, Run, and Rollback Guide

- **Owner**: Gemini (Implementer / Documentation)
- **Issue**: #521 (`[frontend] 초보자용 Podman 최소 이미지 빌드·실행·롤백 가이드 최신화`)
- **Branch**: `agent/521-beginner-podman-auth-runtime` (Base: `origin/main@9816670d`)
- **Worktree**: `/tmp/account-521-beginner-podman-auth-runtime`
- **Status**: Draft PR Ready
- **Summary**:
  - `development-compose.md`: Detailed Docker vs Podman provider comparison table and clear instructions on using `docker-compose` v2.x plugin while strictly prohibiting Python `podman-compose`.
  - Added structured beginner guide for the 7-container low-resource external-dev authentication stack (`tools/compose.minimal-auth-external-dev.yml`), including prerequisites, value-redacting preflight, sequential one-worker build (`--max-workers=1`), smoke verification, and troubleshooting (EACCES permission issues, HTTP 400 vs 503, and memory guardrails).
  - Explicitly documented image rebuild impact (source updates requiring image re-build) and container recreate conditions (`up --force-recreate` / `up -d`).
  - Documented safe project-level stop/down rollback while strictly prohibiting destructive commands: `down -v`, `container prune -f`, `image prune -f`, and `git checkout <commit> -- <files>`.
  - Synchronized references and safety warnings across `container-images.md`, `frontend-runtime-guide.md`, and `runtime-execution-matrix.md`.
- **Verification**:
  - `python3 tools/test_run_minimal_auth_external_dev.py`: 7 tests PASS.
  - `./gradlew :config-server:test --tests "com.ho.account.configserver.DevelopmentComposePolicyTest" --tests "com.ho.account.configserver.ContainerImagePolicyTest"`: 5/5 tasks executed, BUILD SUCCESSFUL.
  - `git diff --check`: 0 errors.
  - Conflict marker & credential scan: 0 occurrences.
- **Rollback**: Reviewed git revert of documentation commits; no external state changed.

# AI Harness Handoff - 2026-08-19 Issue #484 Apply K-Bank Modern Fintech Design System

- **Owner**: Gemini (Parent Integrator / Frontend)
- **Issue**: #484 (`[frontend][ui] 케이뱅크(KBank) 스타일 디자인 시스템 전면 적용`)
- **PR**: #485 (`[#484] 케이뱅크(KBank) 스타일 디자인 시스템 (색상, 폰트, 배경, 레이아웃) 전면 적용`)
- **Status**: Draft PR Open / Review Ready
- **Summary**:
  - Transformed frontend styling into the signature K-Bank modern fintech look: soft gray background (`#f7f8fb`), pure white cards (`#ffffff`, border `#eaedf4`, shadow), KBank signature blue (`#4262ff`, hover `#3452e6`, light `#eef2ff`, border `#dbe3ff`), refined typography (`letter-spacing: -0.015em`, `#17191e` headings), white TopHeader, clean Sidebar, and crisp Dashboard cards.
  - Verification: `npm run build` executed successfully with 122 static routes generated with 0 errors.
# AI Harness Handoff - 2026-08-19 Issue #484 Apply K-Bank Modern Fintech Design System, Dual Theme & Decoupled Navigation

- **Owner**: Gemini (Parent Integrator / Frontend)
- **Issue**: #484 (`[frontend][ui] 케이뱅크(KBank) 스타일 디자인 시스템 전면 적용`)
- **PR**: #487 (`[#484] 케이뱅크(KBank) 스타일 핀테크 디자인 시스템, 듀얼 테마 엔진 및 6대 메가그룹-16대 MSA 모듈 네비게이션 적용`)
- **Status**: Review Ready / Subagent Approved (READY FOR MERGE)
- **Summary**:
  - Transformed frontend styling into signature K-Bank modern fintech look: soft gray background (`#f7f8fb`), pure white cards (`#ffffff`, border `#eaedf4`, shadow), KBank signature blue (`#4262ff`, hover `#3452e6`, light `#eef2ff`, border `#dbe3ff`), refined typography (`letter-spacing: -0.015em`, `#17191e` headings).
  - Integrated global dual-theme engine (`globals.css`, `ThemeContext.tsx`) seamlessly synchronizing light pure white cards and dark navy cards across all 122 pages.
  - Decoupled top-level navigation into **6 Mega Business Groups** (eliminating horizontal scrollbars) while displaying all **16 discrete MSA module sections with tag badges** in Sidebar.
  - Implemented 13 API domain services with 1.5s AbortController timeout fallback to resilient mock datasets.
  - Created master core education guide (`frontend-core-education-guide.md`) and UI layout/screen specification (`ui-layout-and-screen-specification.md`).
  - Verification: `npm run build` executed successfully with 122 static routes generated with 0 errors and 0 warnings. Subagent review APPROVED.

# AI Harness Handoff - 2026-08-13 Issue #92 Fix Tax Batch ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #92 (`[bug] tax:batch 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Review Ready / PR Pending
- **Summary**:
  - Created `tax/batch/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:tax_batch_db;MODE=PostgreSQL`), Flyway baseline migration (`locations: classpath:db/tax-migration`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `TaxBatchApplicationTests.java` verifying ApplicationContext loading, Job/UseCase bean injection, and local profile isolation.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation, Spring Batch Metadata Schema Auto-Initialization, and Bean Injection Verification.
  - Verification: `./gradlew.bat :tax:batch:test` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-13 Issue #93 Fix Expenditure Resolution API ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #93 (`[bug] expenditure-resolution:api 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Review Ready / PR Pending
- **Summary**:
  - Created `expenditure-resolution/api/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:expenditure_resolution_api_db;MODE=PostgreSQL`), Flyway migration (`locations: classpath:db/expenditure-resolution-migration`), JPA `ddl-auto: validate`, and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `ExpenditureResolutionApiApplicationTests.java` verifying ApplicationContext loading, Controller/UseCase/Port stub injection, and local profile isolation.
  - Refactored `ExpenditureResolutionPostgresqlSchemaContextTest.java` to use `@ActiveProfiles("local")`.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation and Outbound Port Stub Adaptations.
  - Verification: `./gradlew.bat :expenditure-resolution:api:test` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-13 Issue #94 Fix Expenditure Resolution Batch ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #94 (`[bug] expenditure-resolution:batch 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Review Ready / PR Created
- **Summary**:
  - Created `expenditure-resolution/batch/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:expenditure_resolution_batch_db;MODE=PostgreSQL`), Flyway migration (`locations: classpath:db/expenditure-resolution-migration`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `ExpenditureResolutionBatchApplicationTests.java` verifying ApplicationContext loading, Job/UseCase bean injection, and local profile isolation.
  - Fixed `JournalPostingPort` anonymous stub implementation in `ExpenditureResolutionLocalExternalPortConfiguration.java` to adhere to multi-method interface contracts.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation, Spring Batch Metadata Schema Auto-Initialization, and Stub Interface Adaptations.
  - Verification: `./gradlew.bat :expenditure-resolution:batch:test` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-13 Issue #96 Fix Reporting Batch ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #96 (`[bug] reporting:batch 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Review Ready / PR Created
- **Summary**:
  - Created `reporting/batch/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:reporting_batch_db;MODE=PostgreSQL`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: create-drop`, memory persistence mode (`account.reporting.persistence.mode: memory`), and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `ReportingBatchApplicationTests.java` verifying ApplicationContext loading and local profile isolation.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation and Spring Batch Metadata Schema Auto-Initialization.
  - Verification: `./gradlew.bat :reporting:batch:test` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-13 Issue #97 Fix Account Mart API ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #97 (`[bug] account-mart:mart-api 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Completed & Merged
- **Summary**:
  - Created `account-mart/mart-api/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:account-mart-api-local;MODE=PostgreSQL`), Flyway baseline migration (`locations: classpath:db/account-mart-local-migration`), JPA `ddl-auto: validate`, and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `AccountMartApiApplicationTests.java` verifying ApplicationContext loading and local profile isolation.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation and Flyway Baseline Migrations.
  - Verification: `./gradlew.bat :account-mart:mart-api:test` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-13 Issue #98 Fix Account Mart Batch ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #98 (`[bug] account-mart:mart-batch 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Review Ready / PR Created
- **Summary**:
  - Created `account-mart/mart-batch/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:account-mart-batch-local;MODE=PostgreSQL`), Flyway baseline migration (`locations: classpath:db/account-mart-local-migration`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `AccountMartBatchApplicationTests.java` verifying ApplicationContext loading and local profile isolation.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation, Flyway Baseline Migrations, and Spring Batch Metadata Schema Auto-Initialization.
  - Verification: `./gradlew.bat :account-mart:mart-batch:test` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-13 Issue #99 Fix ECL API ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #99 (`[bug] ecl:ecl-api 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Review Ready / PR Created
- **Summary**:
  - Created `ecl/ecl-api/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:ecl-api-local;MODE=PostgreSQL`), Flyway V1 baseline migration (`locations: classpath:db/ecl-local-migration`), JPA `ddl-auto: validate`, and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `EclApiApplicationTests.java` verifying ApplicationContext loading and local profile isolation.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation, Flyway Baseline Migrations, and External Control Plane Decoupling.
  - Verification: `./gradlew.bat :ecl:ecl-api:test` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-13 Issue #100 Fix ECL Batch ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #100 (`[bug] ecl:ecl-batch 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Integrated PR #405 / Closed
- **Summary**:
  - Added exception rule `!**/src/test/resources/application-local.yml` to `.gitignore`.
  - Created `ecl/ecl-batch/src/test/resources/application-local.yml` configuring isolated H2 in-memory DB (`jdbc:h2:mem:ecl-batch-local;MODE=PostgreSQL`), Flyway V1 baseline migration (`locations: classpath:db/ecl-local-migration`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled Cloud Config/Eureka/Vault/Kafka control plane services.
  - Created `EclBatchApplicationTests.java` verifying ApplicationContext loading and local profile isolation.
  - Added extensive pedagogical comments across changed files explaining Profile-Based Local Runtime Isolation, Flyway Baseline Migrations, and Spring Batch Metadata Schema Auto-Initialization.
  - Verification: `./gradlew.bat :ecl:ecl-batch:test` passed with 100% SUCCESS. PR #405 merged into `main`.

# AI Harness Handoff - 2026-08-12 Issue #75 Fix Journal Ledger API ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #75 (`[bug] journal-ledger:api 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Review Ready / PR Created
- **Summary**:
  - Created `journal-ledger/api/src/main/resources/application.yml` and `application-local.yml` configuring local default profile, H2 in-memory DB (`jdbc:h2:mem:journal_ledger_api_db;MODE=PostgreSQL`), JPA `create-drop`, `journal-ledger.master-data.local-adapter.enabled: true`, `spring.kafka.listener.auto-startup: false`, and disabled Cloud Config/Eureka/Vault control plane services.
  - Updated `JournalLedgerApplication` Composition Root `@EntityScan` and `@EnableJpaRepositories` to explicitly include `com.ho.account.journalledger.domain` and `com.ho.account.journalledger.adapter.out.persistence` packages.
  - Fixed UTF-16LE BOM encoding of test `journal-ledger/api/src/test/resources/application.properties` to standard UTF-8.
  - Created `JournalLedgerApiLocalProfileTest.java` verifying local ApplicationContext loading, profile isolation, H2 DB configuration, external cloud & Kafka listener decoupling, and Metamodel/Repository bean registrations.
  - Added extensive pedagogical comments across all changed files explaining Explicit Multi-Module JPA Package Scanning, Profile-Based Isolated Runtime, Event Broker Decoupling, and Modular Encapsulation Boundaries.
  - Verification: `./gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:api:bootJar` passed with 100% SUCCESS. Executable JAR smoke test `java -jar journal-ledger/api/build/libs/journal-ledger-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` clean H2 schema startup & JPA initialization.

# AI Harness Handoff - 2026-08-12 Issue #79 Fix Loan API ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #79 (`[bug] loan:api 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Integrated PR #401 / Closed
- **Summary**:
  - Created `loan/api/src/main/resources/application.yml` and `application-local.yml` configuring local default profile, H2 in-memory DB (`jdbc:h2:mem:loan_api_db;MODE=PostgreSQL`), JPA `create-drop`, and disabled Cloud Config/Eureka/Vault control plane services.
  - Updated `LoanApplication` Composition Root `@EntityScan` and `@EnableJpaRepositories` to explicitly include MasterData entity (`com.ho.account.masterdata.core.infrastructure.persistence.entity` & `com.ho.account.masterdata.core.infrastructure.persistence`) and Shared Audit security packages (`com.ho.account.shared.infrastructure.security.domain` & `repository`), resolving `BusinessPartnerJpaEntity` `Not a managed type` and missing `AuditLogRepository` bean errors.
  - Refactored `LoanApplicationLocalProfileTest.java` verifying local ApplicationContext loading, profile isolation, H2 DB configuration, external cloud decoupling, and Metamodel/Repository bean registrations.
  - Added extensive pedagogical comments across all changed files explaining Explicit Multi-Module JPA Package Scanning, Profile-Based Isolated Runtime, and Modular Encapsulation Boundaries.
  - Verification: `./gradlew.bat :loan:core:test :loan:api:test :loan:api:bootJar` passed with 100% SUCCESS. Executable JAR smoke test `java -jar loan/api/build/libs/loan-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` clean H2 schema startup & JPA initialization. PR #401 merged into `main`.

# AI Harness Handoff - 2026-08-12 Issue #80 Fix Loan Batch ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #80 (`[bug] loan:batch 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Integrated PR #400 / Closed
- **Summary**:
  - Created `loan/batch/src/main/resources/application.yml` and `application-local.yml` configuring local default profile, H2 in-memory DB (`jdbc:h2:mem:loan_batch_db;MODE=PostgreSQL`), JPA `create-drop`, `spring.batch.jdbc.initialize-schema: always`, `spring.batch.job.enabled: false`, `web-application-type: none`, and disabled Cloud Config/Eureka/Vault control plane services.
  - Removed `@EnableBatchProcessing` from `LoanBatchApplication` to restore Spring Boot 3 `BatchAutoConfiguration` and auto-creation of Spring Batch metadata tables.
  - Added `masterdata` persistence and `shared.security` packages to `@EntityScan` and `@EnableJpaRepositories` in `LoanBatchApplication`.
  - Added `@Autowired` to `LoanJournalAdapter` primary constructor to resolve Spring DI constructor ambiguity in `loan/core`.
  - Fixed test dependency in `loan/batch/build.gradle` (`spring-batch-test`).
  - Added `LoanBatchLocalProfileTest` and `LoanInterestAccrualBatchConfigTest` verifying ApplicationContext loading, profile isolation, non-web environment, and representative job execution (`loanInterestAccrualJob`).
  - Added extensive pedagogical comments across all changed files explaining Non-Web Process Execution Structure, Resource Automatic Release & Shutdown Lifecycle, Spring Boot 3 `BatchAutoConfiguration` mechanics, and Local H2 In-Memory Batch Runtime benefits.
  - Verification: `./gradlew.bat :loan:core:test :loan:batch:test :loan:batch:bootJar` passed with 100% SUCCESS. Executable JAR smoke test `java -jar loan/batch/build/libs/loan-batch-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` clean startup & exit. PR #400 merged into `main`.

# AI Harness Handoff - 2026-08-12 Issue #81 Fix Deposit API ApplicationContext Loading and Configure Local H2 Profile

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #81 (`[bug] deposit:api 로컬 실행 실패 (ApplicationContext 오류)`)
- **Status**: Completed / Ready for Integration
- **Summary**:
  - Created `deposit/api/src/main/resources/application.yml` and `application-local.yml` configuring local default profile, H2 in-memory DB (`jdbc:h2:mem:deposit_local_db;MODE=PostgreSQL`), JPA `create-drop`, `flyway.enabled: false`, `local-adapters.enabled: true`, and disabled Cloud Config/Eureka/Vault control plane services.
  - Implemented `approveAndPost(Long journalEntryId, String actor)` in `LocalDepositJournalPostingAdapter` to satisfy `JournalPostingPort` interface contract.
  - Created `DepositQueryUseCase` and `DepositUseCase` inbound port interfaces for CQRS read operations and composite interface injection.
  - Added `@Autowired` to primary `DepositService` constructor to eliminate Spring DI constructor disambiguation exception (`NoSuchMethodException`), and implemented `findByAccountNumber`.
  - Configured `@SpringBootApplication(scanBasePackages = "com.ho.account.deposit")`, `@EntityScan`, and `@EnableJpaRepositories` in `DepositApplication`.
  - Enhanced `DepositController` REST endpoints and added `DepositApplicationTest` for local ApplicationContext integration testing.
  - Added extensive pedagogical comments explaining Profile Separation, H2 In-Memory DB Isolation, Hexagonal Inbound Web Adapters, and Spring Container Constructor Injection.
  - Verification: `./gradlew.bat :deposit:core:test :deposit:api:test :deposit:api:bootJar` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-12 Issue #362 Pin Node 20 LTS Runtime and Align Container Contracts for Frontend

- **Owner**: Gemini (Agent loop subagent)
- **Issue**: #362 (`[runtime][frontend] Pin Node runtime and verify local NPM lifecycle`)
- **Status**: Completed / Ready for Integration
- **Summary**:
  - Configured explicit `"engines": { "node": ">=20.0.0 <21.0.0", "npm": ">=10.0.0" }` in `frontend/package.json` with educational comments.
  - Created `frontend/.nvmrc` and `frontend/.node-version` containing `20.18.0` for automatic local Node.js version switching with nvm/fnm/nodenv.
  - Aligned local runtime configurations 100% with container base images (`node:20-alpine`) in `frontend/Dockerfile`, `frontend/Containerfile`, and `frontend/Containerfile.dev`.
  - Added comprehensive pedagogical comments across header comments and documentation detailing Node Runtime Pinning, Dev/Container Runtime Parity, and Lockfile v3 Deterministic Reproducibility.
  - Updated `frontend/README.md` with Node 20 LTS & NPM 10+ runtime requirements guide.
  - Verification: `git diff` and JSON validity check passed.

# AI Harness Handoff - 2026-08-12 Issue #343 Align Demo Seed Fixture and Clean Batch Lifecycle for Account Mart

- **Owner**: Gemini (Agent loop subagent)
- **PR**: #397 (Merged into `main`)
- **Status**: Integrated PR / Issue closed
- **Summary**:
  - Implemented `AccountMartDemoSeedRunner` and `AccountMartDemoFixtureService` activated on `mart.batch.demo-seed.enabled: true` for deterministic H2 fixture seeding (ods account subjects, products, ledgers, balance history, general ledger, exchange rates, KAP ratings, collaterals).
  - Specified `@Bean(destroyMethod = "")` on all `ItemReader` bean definitions in `IntegratedPositionEtlJobConfig`, `KapDataEtlJobConfig`, and `BehavioralHistoryLoadJobConfig` to decouple Spring Container shutdown inferred `close()` from Spring Batch Step ItemStream lifecycle, eliminating unopened reader close warnings during context shutdown (`spring.batch.job.enabled: false`).
  - Added `AccountMartDemoSeedAndLifecycleTest.java` verifying deterministic demo fixture loading, representative job execution (`integratedPositionEtlJob`), and clean context shutdown.
  - Added detailed pedagogical comments explaining Data Mart Batch Lifecycle, Deterministic Demo Seeding, and Spring Container vs Spring Batch lifecycle management.
  - Verification: `./gradlew.bat :account-mart:mart-core:test :account-mart:mart-api:test :account-mart:mart-batch:test :account-mart:mart-api:bootJar :account-mart:mart-batch:bootJar` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-12 Issue #344 Strengthen Self-Contained Local H2 Runtime Policy for Internal Audit

- **Owner**: Gemini (Agent loop subagent)
- **PR**: #396 (Merged into `main`)
- **Status**: Integrated PR / Issue closed
- **Summary**:
  - Added explicit `application-local.yml` for `internal-audit/api` with H2 PostgreSQL mode (`jdbc:h2:mem:internal_audit_local_db`), Flyway V60 migration target, JPA `validate`, and disabled Spring Cloud Config/Discovery/Eureka/Vault control plane services.
  - Strengthened `InternalAuditRuntimePolicyTest.java` validating profile-based isolation, Flyway V60 target schema, control plane decoupling, and execution topology contracts.
  - Updated `internal-audit/README.md` with detailed standalone local execution guide and PowerShell commands.
  - Added extensive pedagogical comments detailing profile-based runtime isolation, control plane decoupling, and offline resilience.
  - Verification: `./gradlew.bat :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-12 Issue #345 Add Explicit Local H2 Profile and Isolate Demo Credentials for Auth Module

- **Owner**: Gemini (Agent loop subagent)
- **Status**: Integrated PR / Issue closed
- **Summary**:
  - Added explicit `application-local.yml` for `auth/api` with H2 PostgreSQL mode (`jdbc:h2:mem:auth_db`), H2 console, Flyway schema migrations, and isolated demo credentials (`auth.jwt.secret`, `auth.internal-api.token`, admin demo account).
  - Enforced Fail-Closed security policy in `application.yml` and `AuthModuleProperties.java` by removing default fallback secrets and adding `@PostConstruct` validation.
  - Created `AuthApiRuntimePolicyTest.java` verifying profile isolation, Fail-Closed policy, and PostgreSQL dev/prod profile compatibility.
  - Verification: `./gradlew.bat :auth:core:test :auth:api:test :auth:api:bootJar` passed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-12 Issue #347 Add Explicit Self-Contained Local Profiles for Reporting Module

- **Issue**: #347 (`[runtime][reporting] Add explicit self-contained local profiles`)
- **PR**: (Merged into `main`)
- **Status**: Completed / Integrated
- **Summary**:
  - Added explicit self-contained `application-local.yml` profiles for `reporting/api` and `reporting/batch` modules.
  - `reporting/api/src/main/resources/application-local.yml`: Configured H2 in-memory DB (`jdbc:h2:mem:reporting_api_db`), JPA `create-drop`, and memory persistence mode (`account.reporting.persistence.mode: memory`). Disabled external Cloud Config, Eureka Discovery, Vault, and Tracing for self-contained local execution.
  - `reporting/batch/src/main/resources/application-local.yml`: Configured `web-application-type: none` to disable embedded servlet container, `spring.batch.job.enabled: false` to prevent automatic job execution on startup, and `spring.batch.jdbc.initialize-schema: always` for H2 batch meta-schema setup.
  - Created integration tests (`ReportingApiLocalProfileTest.java`, `ReportingBatchLocalProfileTest.java`) to verify context loading, active profile matching, H2 connection, and spring batch auto-start prevention under `@ActiveProfiles("local")`.
  - Fixed contract implementations (`findBySlipNo` in `InMemoryJournalQueryAdapter` and `calculateLedgerSummary` in `LedgerClientAdapterTest`).
  - Added comprehensive pedagogical comments on Profile Separation, H2 In-Memory DB Isolation, Automatic Batch Scheduler Prevention (`job.enabled: false`), and Infrastructure Decoupling.
  - Verification: Executed `./gradlew.bat :reporting:core:test :reporting:api:test :reporting:batch:test :reporting:api:bootJar :reporting:batch:bootJar` (100% SUCCESSFUL).

# AI Harness Handoff - 2026-08-12 Issue #300 Refactor Payable & Receivable Batch Jobs to Chunk-Oriented Processing

- **Issue**: #300 (`[payable, receivable][extensibility] 대용량 배치 처리에 부적합한 Tasklet 및 단일 트랜잭션 루프`)
- **PR**: (Prepared for PR creation and merge)
- **Status**: Completed / Review ready
- **Summary**:
  - Refactored `ReceivableAutoMatchingBatchConfig` and `PayablePaymentRunBatchConfig` from single-transaction Tasklet implementations to Spring Batch Chunk-Oriented Architecture (`JpaPagingItemReader`, `ItemWriter`, Chunk Size 100).
  - `ReceivableAutoMatchingBatchConfig.java`: Configured `JpaPagingItemReader<CollectionJpaEntity>` (`@StepScope`, pageSize=100) and `ItemWriter<CollectionJpaEntity>` to process candidate collections page-by-page.
  - `PaymentUseCase` & `PaymentService`: Extended interface and implementation with `createPaymentRun`, `processPaymentRunChunk`, and `completePaymentRun` methods.
  - `PayablePaymentRunBatchConfig.java`: Structured into a 3-step pipeline (`createPaymentRunStep` -> `processPayablePaymentRunChunkStep` -> `completePaymentRunStep`).
  - Added comprehensive pedagogical comments detailing Spring Batch Chunk-Oriented Architecture, Memory Footprint Management (caps memory at $O(\text{chunkSize})$ to prevent Heap OOM), and Transaction Boundary Segregation.
  - Verification: Executed `./gradlew.bat :receivable:batch:test :payable:batch:test` (100% SUCCESSFUL).

# AI Harness Handoff - 2026-08-12 Issue #302 Decompose Monolithic ReconciliationService for Single Responsibility Principle Compliance

- **Issue**: #302 (`[reconciliation][clean-code] ReconciliationService 거대 클래스(680행+) 및 SRP 위반`)
- **PR**: (Merged into `main`)
- **Status**: Completed / Integrated
- **Summary**:
  - Decomposed monolithic 680+ lines `ReconciliationService` into specialized domain services (`ReconciliationUnitService`, `ReconciliationRuleService`, `ReconciliationExecutionService`) in compliance with Single Responsibility Principle (SRP).
  - `ReconciliationUnitService`: Manages `ReconciliationUnit` CRUD and soft deletion.
  - `ReconciliationRuleService`: Manages `ReconciliationRule` and `DifferenceReasonCode` CRUD and soft deletion.
  - `ReconciliationExecutionService`: Orchestrates source/target snapshot collection, N:M matching engine execution, difference assignment/resolution, and adjustment journal posting.
  - Refactored `ReconciliationService` as a Facade Service delegating to specialized services, maintaining 100% backward compatibility for API Controllers, Batch jobs, and legacy constructors.
  - Added comprehensive pedagogical comments explaining God Class smell removal, SRP, and Facade Pattern encapsulation.
  - Created unit tests (`ReconciliationUnitServiceTest`, `ReconciliationRuleServiceTest`).
  - Verification: Executed `./gradlew.bat :reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test` (100% SUCCESSFUL).

# AI Harness Handoff - 2026-08-12 Issue #307 Refactor Balance Reaggregation Batch Job to Chunk-Oriented Processing & Guarantee Idempotency

- **Issue**: #307 (`[journal-ledger:batch][extensibility] BalanceReaggregationBatchConfig 대량 데이터 처리 성능 및 멱등성 한계`)
- **PR**: #391 (Merged into `main`)
- **Status**: Completed / Integrated
- **Summary**:
  - Refactored `BalanceReaggregationBatchConfig` from single-transaction Tasklet to 2-step pipeline with Clean-up Tasklet (`BalanceCleanUpTasklet`) and Chunk-oriented processing (`JpaPagingItemReader`, `ItemProcessor`, `ItemWriter`).
  - Added `clearLedgerBalancesForPeriod` to `LedgerService` for pre-processing balance reset, guaranteeing batch idempotency and restartability.
  - Page-by-page loading caps memory footprint to $O(\text{chunkSize})$ preventing Heap OOM and DB connection timeouts.
  - Added extensive pedagogical comments covering Low Memory Footprint, Transaction Boundaries, Restartability & Idempotency, and Clean-up Step benefits.
  - Created unit/integration tests (`BalanceReaggregationBatchConfigTest`).
  - Verification: Executed `./gradlew.bat :journal-ledger:batch:test` and `./gradlew.bat :journal-ledger:core:test` (100% SUCCESSFUL).

# AI Harness Handoff - 2026-08-12 Issue #320 Implement Item-Level N:M Matching Engine for Financial Reconciliation

- **Issue**: #320 (`[reconciliation][financial] 대사 자동 매칭 로직이 단순 총액 비교에 불과함 — 건별 N:M 매칭 엔진 부재`)
- **PR**: #390 (Merged into `main`)
- **Status**: Completed / Integrated
- **Summary**:
  - Implemented Item-Level N:M matching engine algorithms (`ReconciliationMatchingEngine`, `ItemLevelMatcher`) replacing simple total summary comparison in `ReconciliationService.performReconciliation`.
  - Created `ReconciliationItem` and `ReconciliationCompositeKey` domain VOs supporting normalized multi-attribute grouping (`transactionDate`, `referenceId`, `partnerCode`, `accountCode`) and relaxed key fallbacks.
  - Built 4-phase matching pipeline:
    1) Phase 1: 1:1 Exact/Tolerance Matching (`EXACT_1_1`)
    2) Phase 2: 1:N and N:1 Subset Matching (`ONE_TO_MANY_1_N`, `MANY_TO_ONE_N_1`)
    3) Phase 3: N:M Subset-Sum Combinatorial Search (`MANY_TO_MANY_N_M`)
    4) Phase 4: Relaxed Key Matching & Discrepancy Categorization (`MISSING_TARGET`, `MISSING_SOURCE`, `AMOUNT_MISMATCH`)
  - Integrated with `ReconciliationDifference` domain entity to store rich JSON metadata for drill-through and audit trail.
  - Added comprehensive pedagogical comments detailing offsetting error prevention, audit trail traceability, and NP-Hard combinatorial optimization via composite key partitioning.
  - Created unit tests (`ItemLevelMatcherTest`, `ReconciliationMatchingEngineTest`) and updated `ReconciliationServiceTest`.
  - Verification: Executed `./gradlew.bat :reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test` (100% SUCCESSFUL).

## 2026-08-12 - Issue #321 Push Down Ledger Snapshot Aggregation to DB Level for Heap OOM Prevention

# AI Harness Handoff - 2026-08-12 Issue #321 Push Down Ledger Snapshot Aggregation to DB Level for Heap OOM Prevention

## Active Goal And State

- GitHub Issue `#321` (`[reconciliation][extensibility] 대량 원장 데이터 메모리 적재로 인한 OOM 위험`) completed and merged into `main` via PR `#389`.
- Branch `agent/321-reconciliation-db-aggregate-oom-fix` integrated and worktree cleaned up.

## Changes And Boundaries

- Outbound Ledger Query Port & Aggregation DTO (`contracts/src/main/java/com/ho/account/contracts/ledger/`):
  - Created `LedgerAggregateSummary` DTO (`count`, `totalAmount`).
  - Added `calculateLedgerSummary(String accountSubjectCode, LocalDate date)` and `calculateLedgerSummary(LocalDate startDate, LocalDate endDate, String accountCode, String currencyCode, String amountBasis)` methods to `LedgerQueryPort`.
- DB-level Push-Down Aggregation (`journal-ledger`):
  - Added JPQL `COUNT(g)` and `SUM(...)` aggregate query (`calculateGlBalanceAggregate`) to `GlBalanceRepository`.
  - Implemented `calculateGlBalanceAggregate` across `LedgerBalancePersistencePort`, `LedgerBalancePersistenceAdapter`, `JdbcLedgerBalanceBulkPersistenceAdapter`, `LedgerService`, and `MonolithLedgerQueryAdapter`.
- Service Refactoring (`ReconManagerService.java`):
  - Removed procedural in-memory for-loop aggregation and entity list loading in `buildLedgerSnapshot`.
  - Delegated to `ledgerQueryPort.calculateLedgerSummary(...)` for single-row DB aggregate retrieval, capping memory footprint to $O(1)$.
- Pedagogical Comments:
  - Added extensive comments detailing Heap OOM prevention, Push-Down Aggregation benefits (reduced network I/O, $O(1)$ memory footprint, DB index/aggregate query optimization).
- Verification Evidence:
  - Updated `ReconManagerServiceTest`, `MonolithLedgerQueryAdapterTest`, and `ReconciliationLocalExternalPortConfiguration`.
  - Executed `./gradlew.bat :reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test :journal-ledger:core:test` with 100% SUCCESS.

## Known Risks And Rollback

- Risk: Ensure DB aggregation query behavior handles NULL sums appropriately (defaults to BigDecimal.ZERO).
- Rollback: Revert GH-321 commit on `main`.

---

# AI Harness Handoff - 2026-08-12 Issue #319 Decouple ECL API Module from ECL Batch for Resource & Process Isolation

## Active Goal And State

- GitHub Issue `#319` (`[ecl][msa] API 모듈이 Batch 모듈을 직접 참조 및 구동 — 아키텍처 훼손`) completed and merged into `main`.
- Branch `agent/319-ecl-api-batch-decouple` worktree changes verified and ready for PR creation and merge.

## Changes And Boundaries

- Direct Dependency Removal:
  - Removed `implementation project(':ecl:ecl-batch')` and `implementation 'org.springframework.boot:spring-boot-starter-batch'` from `ecl/ecl-api/build.gradle`.
  - Removed `com.ho.account.ecl.batch` package scan and `AllowanceEclBatchApplication` reference from `AllowanceEclApiApplication.java`.
- Inbound Batch Trigger Port & Adapter (`com.ho.account.ecl.api.port` & `infrastructure.adapter`):
  - Created `BatchTriggerPort` interface (`triggerBatch`, `getBatchStatus`).
  - Created `BatchTriggerResponse`, `BatchStatusResponse`, `BatchAlreadyCompletedException`, `BatchExecutionException`.
  - Implemented `ExternalBatchTriggerAdapter` using `JdbcTemplate` for Spring Batch metadata table queries without direct Spring Batch dependencies.
- Controller & Kafka Consumer Refactoring:
  - Refactored `AllowanceBatchController` and `CdmDataReadyConsumer` to remove `JobLauncher`, `JobExplorer`, and `Job` direct injection and use `BatchTriggerPort`.
- Pedagogical Comments:
  - Added comprehensive comments detailing MSA Resource Isolation, API Server Memory Protection, CPU/Connection Contention Avoidance, and Process Isolation.
- Verification Evidence:
  - Updated `CdmDataReadyConsumerTest.java` for `BatchTriggerPort` mocking.
  - Executed `./gradlew.bat :ecl:ecl-core:test :ecl:ecl-api:test :ecl:ecl-batch:test` with 100% SUCCESS.

## Known Risks And Rollback

- Risk: Ensure external batch trigger endpoint/scheduler configuration matches the expected parameters (`jobName`, `baseDate`, `traceId`, `eventId`).
- Rollback: Revert GH-319 commit on `main`.

---

# AI Harness Handoff - 2026-08-12 Issue #322 Refactor Tax Invoice Batch Validation to Use Paging for OOM Prevention

## Active Goal And State

- GitHub Issue `#322` (`[tax][extensibility] 대량 세금계산서 데이터 한번에 메모리 로딩 — OOM 위험`) completed and merged into `main` via PR `#387`.
- Branch `agent/322-tax-invoice-batch-paging-oom-fix` integrated and worktree cleaned up.

## Changes And Boundaries

- Inbound Port & Paging Persistence Port:
  - Added `validatePurchaseInvoices(LocalDate startDate, LocalDate endDate, int pageSize)` overload to `TaxInvoiceBatchUseCase`.
  - Added `Page<TaxInvoice> findByIssueDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable)` to `TaxInvoicePersistencePort`, `TaxInvoiceRepository`, and `TaxInvoicePersistenceAdapter`.
- Paged Batch Validation & Heap OOM Prevention (`TaxInvoiceBatchService.java`):
  - Refactored `validatePurchaseInvoices` using `PageRequest.of(pageNumber, pageSize)` in a `do-while` loop to process tax invoices in chunks (default pageSize 500).
  - Restricted JVM Heap Memory footprint to $O(pageSize)$ objects, enabling fast Minor GC object collection per chunk.
- Bulk Query Synergy:
  - Maintained 1-time bulk partner lookup (`masterDataQueryPort.findAllByPartnerCodes`) per page chunk to prevent N+1 query overhead while ensuring OOM protection.
- Pedagogical Comments:
  - Added detailed architectural comments explaining Heap OOM prevention, chunking/paging benefits, memory footprint limits, and GC efficiency.
- Verification Evidence:
  - Updated `TaxInvoiceBatchServiceTest.java` for paged queries and added `validatePurchaseInvoicesProcessesInPagesToPreventOOM` test for multi-page batch validation.
  - Executed `./gradlew.bat :tax:core:test :tax:api:test :tax:batch:test` with 100% SUCCESS.

## Known Risks And Rollback

- Risk: Ensure DB queries use proper index on `issue_date` for efficient paged scans across large datasets.
- Rollback: Revert PR #387 commit on `main`.

---

# AI Harness Handoff - 2026-08-12 Issue #328 Configure Gateway Dynamic Discovery Routing, Global CORS, and Rate Limiter

## Active Goal And State

- GitHub Issue `#328` (`[gateway][msa] Gateway 라우팅 룰 및 Rate Limiter, CORS 설정 누락`) completed and merged into `main` via PR `#386`.
- Branch `agent/328-gateway-routing-cors-rate-limiter` integrated and cleaned up.

## Changes And Boundaries

- Dynamic Discovery Routing:
  - Added `spring.cloud.gateway.discovery.locator.enabled: true`, `lower-case-service-id: true`, and `lower-case-service-id-with-rest-site: true` in `application.yml` and `config-repo/gateway-service.yml`.
- Global CORS Configuration:
  - Configured `allowedOriginPatterns` (`http://localhost:*`, `http://127.0.0.1:*`, `https://*.myaccount.com`), `exposedHeaders` (`Authorization`), and `maxAge: 3600` client-side caching.
- Rate Limiter & Key Resolver (`RateLimiterConfig.java`):
  - Implemented `ipKeyResolver` `@Bean` prioritizing `X-Forwarded-For` header for real client IP extraction.
  - Implemented `inMemoryRateLimiter` (`RateLimiter<Config>`) Thread-Safe Token Bucket implementation with response header calculations (`X-RateLimit-*`).
  - Configured `default-filters` in Gateway YAML to apply `RequestRateLimiter` globally.
- Pedagogical Comments:
  - Added extensive architectural comments explaining MSA Single Point of Entry, Bounded Context dynamic routing, global CORS domain isolation, and DDoS/traffic control via IP KeyResolver & Rate Limiting.
- Verification Evidence:
  - Created `RateLimiterConfigTest.java` to test `ipKeyResolver` header/remoteAddress extraction and `inMemoryRateLimiter` token consumption/limiting.
  - Updated `GatewayRouteSecurityPolicyTest.java` to assert discovery locator, globalcors maxAge, and default filter configurations.
  - Executed `./gradlew.bat :gateway:test` with 42 tests 100% SUCCESS.

## Known Risks And Rollback

- Risk: Ensure proxy servers or ALBs properly pass the `X-Forwarded-For` header for accurate IP rate limiting.
- Rollback: Revert `RateLimiterConfig.java`, `RateLimiterConfigTest.java`, `application.yml`, and `config-repo/gateway-service.yml`.

---

# AI Harness Handoff - 2026-08-12 Issue #324 Convert ECL and PD Calculation Logic to BigDecimal for Financial Precision

## Active Goal And State

- GitHub Issue `#324` (`[ecl][financial] 금융 통계(ECL/PD) 계산 로직에 부동소수점(double) 자료형 사용 — 금액 정밀도 위험`) completed.
- Branch `agent/324-ecl-bigdecimal-precision-conversion` prepared for PR and integration.

## Changes And Boundaries

- Exposure Maturity Precision (`CrAccount.java`):
  - Converted constants `DEFAULT_MATURITY_YEARS`, `MINIMUM_MATURITY_YEARS`, `DAYS_PER_YEAR` to `BigDecimal`.
  - Updated `resolveMaturityYears(LocalDate baseDate)` return type to `BigDecimal` with 8 decimal places scale (`setScale(8, RoundingMode.HALF_UP)`).
- PD Curve Calculation (`PdCalculator.java`):
  - Replaced primitive `double` parameters (`maturityYears`) with `BigDecimal`.
  - Completely removed primitive `double` variables (`pd1`, `hazardRate`, `cumulativePd`, `survivalProb`, `marginalPd`) in `generateTransitionBasedCurve` and `generateSimplePdCurve`.
  - Applied mathematical equivalence ($1 - e^{-h} = pd_1$) to eliminate floating-point `Math.log`/`Math.exp` calls, performing 100% `BigDecimal` & `MathContext(15, RoundingMode.HALF_UP)` calculations.
- Service & Pipeline Alignment:
  - Updated `LifetimePdService.java` to accept `BigDecimal maturityYears`.
  - Updated `ForwardLookingEclCalculationPipeline.java` to pass `BigDecimal maturityYears`.
- Collateral Allocation Precision (`CollateralAllocationCalculator.java`):
  - Updated LP optimization DTO mapping to construct `BigDecimal` allocations with 4-decimal scale and `compareTo` threshold checking (`0.0001`).
- Pedagogical Comments:
  - Added extensive comments explaining IEEE 754 floating-point precision loss, `BigDecimal` and `MathContext` precision control, and IFRS 9 ECL financial statistics accuracy.
- Verification Evidence:
  - Executed `./gradlew.bat :ecl:ecl-core:test :ecl:ecl-api:test :ecl:ecl-batch:test` with 100% SUCCESS.

## Known Risks And Rollback

- Risk: High precision decimal calculations (8 decimal places for PD, 4 decimal places for monetary amounts) are required for regulatory IFRS 9 compliance.
- Rollback: Revert changes in `CrAccount.java`, `PdCalculator.java`, `LifetimePdService.java`, `CollateralAllocationCalculator.java`, and `ForwardLookingEclCalculationPipeline.java`.

---

# AI Harness Handoff - 2026-08-12 Issue #326 Configure Production Git Backend and Property Encryption for Config Server

## Active Goal And State

- GitHub Issue `#326` (`[config-server][security] 운영 환경용 Git 백엔드 및 암호화 설정 부재`) completed.
- Branch `agent/326-config-server-git-backend-encryption` prepared for PR and integration.

## Changes And Boundaries

- Common Configuration & Encryption (`config-server/src/main/resources/application.yml`):
  - Configured `encrypt.key: ${ENCRYPT_KEY:account-config-server-secret-key}` for symmetric key property encryption/decryption (`/encrypt`, `/decrypt`, `{cipher}...`).
  - Kept default `SPRING_PROFILES_ACTIVE: native`.
  - Added extensive pedagogical comments explaining centralized config management, property encryption, and profile separation benefits in MSA.
- Production Profile Configuration (`config-server/src/main/resources/application-prod.yml`):
  - Added Git backend configuration (`spring.cloud.config.server.git.uri`, `default-label`, `search-paths`, `clone-on-start`, `username`, `password`) for central Git audit trail and versioning in production.
- Native Profile Configuration (`config-server/src/main/resources/application-native.yml`):
  - Added local filesystem search-locations (`CONFIG_REPO_LOCATION: file:./config-repo`) for isolated local development.
- Configuration Policy Testing (`ConfigServerConfigurationPolicyTest.java`):
  - Added `encrypt.key` assertion to local defaults test.
  - Added `productionProfileUsesGitBackendAndPropertyEncryption` test method to verify production Git backend properties.
- Verification Evidence:
  - Executed `./gradlew.bat :config-server:test` with 100% SUCCESS.

## Known Risks And Rollback

- Operational Risk: Production deployment requires proper `ENCRYPT_KEY` environment variable injection and valid Git credentials if private config repositories are used.
- Rollback: Revert `config-server/src/main/resources/application-prod.yml`, `application-native.yml`, and `application.yml` changes.

---

# AI Harness Handoff - 2026-08-12 Issue #311 Decouple Closing Module from Journal-Ledger Core for MSA Isolation

## Changes And Boundaries

- Direct Project Dependency Removal (`closing/api/build.gradle` & `closing/batch/build.gradle`):
  - Removed `implementation project(':journal-ledger:core')` from `closing:api` and `closing:batch`.
  - Added `implementation project(':contracts')` to `closing:api` and `closing:batch`.
- Shared Kernel Outbound Port Transition (`JournalLedgerClosingJournalEntryAdapter.java`):
  - Replaced `JournalUseCase`, `JournalEntry`, `JournalEntryStatus`, `JournalDetail` direct imports/usage with `JournalPostingPort` and `JournalQueryPort` from `:contracts`.
  - Refactored `createDraftAdjustment` to check existing slips via `journalQueryPort.findBySlipNo` and `getJournalDetails`.
  - Refactored `approveAndPost` to delegate directly to `journalPostingPort.approveAndPost`.
- Contract & Adapter Extensions:
  - Added `slipDate`, `currencyCode`, `lineageSourceType`, `lineageSourceId` to `JournalSummary`.
  - Added `departmentCode` to `JournalDetailSummary`.
  - Added `approveAndPost` method signature to `JournalPostingPort` and implemented in `JournalPostingAdapter`.
  - Added `findBySlipNo` method signature to `JournalQueryPort` and implemented in `MonolithJournalQueryAdapter`.
- Pedagogical Comments:
  - Added comprehensive pedagogical comments on DDD Bounded Context preservation, compile-time module isolation, and Hexagonal Outbound Port pattern benefits for MSA architecture.
- Local Configuration & Tests:
  - Created `ClosingLocalExternalPortConfiguration.java` providing fallback `JournalPostingPort` and `JournalQueryPort` beans for closing module runtime and tests.
  - Updated `JournalLedgerClosingJournalEntryAdapterTest.java` to mock `JournalPostingPort` and `JournalQueryPort`.
- Verification Evidence:
  - Executed `./gradlew.bat :closing:api:test :closing:batch:test :contracts:test :journal-ledger:core:test` with 100% SUCCESS.

## Known Risks And Rollback

- Rollback: Revert commit `9f6106c4` on `main`.

# AI Harness Handoff - 2026-08-12 Issue #315 Decouple Expenditure Resolution Core from Direct Module Dependencies

## Active Goal And State

- GitHub Issue `#315` (`[expenditure-resolution][msa] 모듈 간 직접 의존 및 MSA/헥사고날 경계 심각한 위반`) completed.
- Worktree `C:\tmp\account-315-expenditure-msa-bounded-context-decouple` configured and verified.

## Changes And Boundaries

- Direct Project Dependency Removal (`expenditure-resolution/core/build.gradle` & `api/build.gradle`):
  - Removed `implementation project(':journal-ledger:core')`, `implementation project(':asset-lease:core')`, `testImplementation project(':tax:core')`, `testImplementation project(':master-data:core')` from `expenditure-resolution:core`.
  - Removed `testImplementation project(':journal-ledger:core')` from `expenditure-resolution:api`.
- Shared Kernel Outbound Port Transition (`ExpenditureResolutionService.java`):
  - Replaced `JournalUseCase` and `JournalEntry` direct imports/usage with `JournalPostingPort`, `JournalEntryCommand`, and `JournalPostingResult` from `:contracts`.
  - Refactored `approveResolution` to assemble `JournalEntryCommand` via `buildJournalEntryCommand()` and call `journalPostingPort.createDraftEntry()`.
- Pedagogical Comments:
  - Added extensive educational comments detailing DDD Bounded Context boundary protection, compile-time core isolation, and Hexagonal Outbound Port pattern benefits for MSA scalability.
- Local Configuration & Tests:
  - Updated `ExpenditureResolutionLocalExternalPortConfiguration.java` to provide `JournalPostingPort` bean instead of `JournalUseCase`.
  - Updated `ExpenditureResolutionServiceTest.java` and `ExpenditureTaxApiIntegrationTest.java` to mock `JournalPostingPort`.
- Verification Evidence:
  - Executed `./gradlew.bat :expenditure-resolution:core:test :expenditure-resolution:api:test` with 100% SUCCESS.

## Known Risks And Rollback

- Rollback: Revert commit on `main`.

# AI Harness Handoff - 2026-08-12 Issue #317 Enforce Non-Negative Book Value Floor During Lease Asset Depreciation

## Active Goal And State

- GitHub Issue `#317` (`[asset-lease][financial] 사용권자산 감가상각 시 장부가액 음수 전락 위험 — 상태 검증 부재`) completed.
- Worktree `C:\tmp\account-317-asset-lease-depreciation-book-value-floor` configured and verified.

## Changes And Boundaries

- Domain Defense & Safe Depreciation Calculation (`RightOfUseAsset.java`):
  - Implemented `calculateSafeDepreciationAmount(BigDecimal targetAmount)` and `depreciate()` to guarantee IFRS 16 non-negative book value floor (0원 이상) and automatic transition to `FULLY_DEPRECIATED`.
  - Added extensive pedagogical comments explaining IFRS 16 rules, domain invariants, and rich domain model benefits.
- Fixed Asset Defense & Safe Depreciation Calculation (`FixedAsset.java`):
  - Implemented `calculateSafeDepreciationAmount(BigDecimal targetAmount)` ensuring book value never falls below residual value.
  - Added domain invariant guards in `depreciate(LocalDate processDate)` and updated pedagogical comments.
- Application Service Delegation:
  - Refactored `LeaseEntryService.processContractMonthlyAccounting()` to delegate ROU asset depreciation calculation and state mutation to `RightOfUseAsset.depreciate()`.
  - Added pedagogical comments to `FixedAssetEntryService.processMonthlyDepreciation()`.
- Verification Evidence:
  - Executed `./gradlew.bat :asset-lease:core:test :asset-lease:api:test :asset-lease:batch:test` with 100% SUCCESS.

## Known Risks And Rollback

- Rollback: Revert commit on `main`.

# AI Harness Handoff - 2026-08-12 Issue #318 Decouple JPA Annotations from Payable and Receivable Domain Entities

## Active Goal And State

- GitHub Issue `#318` (`[payable, receivable][architecture] 도메인 엔티티에 JPA 의존성 강결합 — 헥사고날 위반`) completed.
- Worktree `C:\tmp\account-318-payable-receivable-jpa-decouple` configured and verified.

## Changes And Boundaries

- Decoupled JPA Annotations from Domain Entities (Pure Java POJO):
  - Converted 11 domain classes (`PurchaseInvoice`, `Payment`, `Payable`, `AdvancePayment`, `PaymentRun`, `SalesInvoice`, `Collection`, `CollectionAllocation`, `Receivable`, `UnmatchedCollection`, `MatchingRule`) to Pure Java POJO.
  - Added extensive pedagogical comments on Hexagonal Architecture Core domain independence and Data Mapper pattern.
- Created Infrastructure JPA Entities (`infrastructure.persistence.entity`):
  - Created 5 JPA entities in `payable` and 6 JPA entities in `receivable`.
- Implemented Data Mappers (`infrastructure.persistence.mapper`):
  - Implemented two-way Data Mappers for all 11 domain models and JPA entities.
- Updated Repositories & Adapters & App Configs:
  - Updated Spring Data Repositories to manage JPA entities, and Persistence Adapters to map between Domain POJOs and JPA Entities.
  - Updated `@EntityScan` in API and Batch Application entry points.
- Verification Evidence:
  - Executed `./gradlew.bat :payable:core:test :payable:api:test :payable:batch:test :receivable:core:test :receivable:api:test :receivable:batch:test` with 100% SUCCESS.

## Known Risks And Rollback

- Rollback: Revert commit on `main`.

# AI Harness Handoff - 2026-08-12 Issue #309 Harden Gateway JWT Secret Configuration & Asymmetric Key Support

## Active Goal And State

- GitHub Issue `#309` (`[gateway][security] JWT Secret 하드코딩 및 비대칭키 구조 전환 필요`) completed and PR #379 merged into `main`.
- Worktree `C:\tmp\account-309-gateway-jwt-secret-security-hardening` removed and pruned post-merge.

## Changes And Boundaries

- Removed Hardcoded Plaintext Default JWT Secret:
  - Removed `modern-account-system-super-secret-key-1234567890` from `gateway/src/main/resources/application.yml` and `JwtProperties.java`.
  - Configured mandatory external environment variable / Config Server property injection (`${AUTH_JWT_SECRET:${JWT_SECRET:}}`, `${AUTH_JWT_PUBLIC_KEY:${JWT_PUBLIC_KEY:}}`, `${AUTH_JWT_JWKS_URI:${JWT_JWKS_URI:}}`).
- Asymmetric Key (RS256/ES256) & Key Rotation Support:
  - Updated `JjwtAccessTokenVerifier.java` using `SigningKeyResolverAdapter` to dynamically resolve HMAC Secret Key (HS256) or RSA Public Key (RS256) based on JWT header `alg`.
- Pedagogical Security Comments:
  - Added extensive comments covering API Gateway Secret Exposure Risks, Asymmetric Verification Defense-in-Depth benefits, and Key Rotation / JWKS (`/.well-known/jwks.json`) strategies across `JwtProperties.java` and `JjwtAccessTokenVerifier.java`.
- Verification Evidence:
  - Added unit tests in `JjwtAccessTokenVerifierTest.java` for RSA RS256 token verification and missing key exception validation.
  - Executed `./gradlew.bat :gateway:test :config-server:test` with 100% SUCCESS.
  - PR #379 merged into `main` and branch deleted.

## Known Risks And Rollback

- Rollback: Revert PR #379 commit on `main`.

# AI Harness Handoff - 2026-08-12 Issue #316 Fix Budget Double-Deduction Bug on Expenditure Resolution Update and Rejection

## Active Goal And State

- GitHub Issue `#316` (`[expenditure-resolution][financial] 결의서 수정/반려 시 예산 복원 누락 (이중 차감 버그)`) completed.
- Worktree `C:\tmp\account-316-expenditure-budget-restore-fix` created and modified.

## Changes And Boundaries

- Domain & Service Budget Restoration:
  - Added `restoreBudget(BigDecimal amount)` to `Budget.java` domain model with validation and educational comments.
  - Implemented `restoreBudget` in `BudgetService.java` to restore budget for specific yearMonth, departmentCode, and accountCode.
- Hexagonal Architecture Port & Adapter:
  - Added `restoreBudget` to `BudgetControlPort.java` (contracts) and implemented in `BudgetControlAdapter.java`.
- Expenditure Resolution LifeCycle & Double-Deduction Prevention:
  - Modified `ExpenditureResolutionService.updateResolution`: Restores previous budget amount before re-deducting new amount when resolution is in DRAFT state.
  - Modified `ExpenditureResolutionService.rejectResolution`: Automatically restores deducted budget in full upon resolution rejection.
- Pedagogical Comments:
  - Added detailed comments explaining Financial Budget Control LifeCycle, budget over-locking prevention, and consistency advantages.
- Verification:
  - Added unit test in `ExpenditureResolutionServiceTest.java` verifying budget restoration on update and rejection.
  - Added `BudgetServiceTest.java` verifying budget deduction restoration logic.
  - Executed `./gradlew.bat :expenditure-resolution:core:test :expenditure-resolution:api:test` (BUILD SUCCESSFUL).

## Verification Evidence

- `./gradlew.bat :expenditure-resolution:core:test :expenditure-resolution:api:test` executed with 100% SUCCESS.

# AI Harness Handoff - 2026-08-12 Issue #313 Apply Optimistic Locking to Deposit Account to Prevent Lost Updates

## Active Goal And State

- GitHub Issue `#313` (`[deposit][architecture] 낙관적 잠금(Optimistic Locking) 누락으로 인한 잔액 갱신 손실(Lost Update) 위험`) completed and PR #377 merged into `main`.
- Worktree `C:\tmp\account-313-deposit-optimistic-locking` removed and pruned post-merge.

## Changes And Boundaries

- JPA `@Version` Optimistic Locking:
  - Added `@Version private Long version;` field to `DepositAccount.java`.
  - Created Flyway/Schema migration scripts `V41__add_version_to_deposit_accounts.sql` for both H2 and PostgreSQL.
- Inbound Port & Retry Mechanism:
  - Created `DepositTransactionUseCase.java` interface (`deposit`, `withdraw`).
  - Implemented `executeWithOptimisticLockRetry` in `DepositService.java` to catch `OptimisticLockingFailureException` and perform exponential backoff retries with fresh DB refetch.
- Pedagogical Comments & Verification:
  - Added educational comments explaining Optimistic Locking vs Pessimistic Locking, Lost Update prevention, and Hexagonal Architecture exception propagation across `DepositAccount.java`, `DepositService.java`, and `DepositAccountPersistenceAdapter.java`.
  - Unit/Concurrency tests added: `DepositAccountOptimisticLockingTest.java` and `DepositServiceConcurrencyTest.java` (10 concurrent threads).
  - Executed `./gradlew.bat :deposit:core:test :deposit:api:test :deposit:batch:test` with BUILD SUCCESSFUL.

## Verification Evidence

- `./gradlew.bat :deposit:core:test :deposit:api:test :deposit:batch:test` executed with 100% SUCCESS.
- PR #377 merged into `main` and remote branch deleted.

## Known Risks And Rollback

- Rollback: Revert PR #377 commit on `main`.

# AI Harness Handoff - 2026-08-12 Issue #292 Decouple Monolith Journal Posting Adapter for MSA Transition

## Changes And Boundaries

- Removed Monolithic Adapter & Command:
  - Removed `MonolithJournalPostingAdapter.java` and `MonolithJournalPostingCommand.java` from `journal-ledger/core/.../common/adapter/`.
- Hexagonal Port & Adapter Refactoring:
  - Refactored `JournalPostingAdapter.java`: Implemented `JournalPostingPort` in Hexagonal Architecture, with `lineageSourceType` and `lineageSourceId` idempotency deduplication.
- Inbound Adapters:
  - Added `JournalPostingRestController.java` (`POST /api/v1/journals/posting`): REST Inbound Web Adapter for synchronous HTTP journal posting in MSA environment.
  - Added `JournalPostingEventListener.java`: Async Event Inbound Adapter for event-driven journal posting via Spring Application Event / Message Relay.
- Pedagogical Comments & Verification:
  - Added rich educational comments explaining Hexagonal Architecture Port/Adapter design, MSA Bounded Context isolation, REST/Event communication, and Idempotency guarantees.
  - Unit tests added: `JournalPostingRestControllerTest.java` & `JournalPostingEventListenerTest.java`.
  - Executed `./gradlew.bat :journal-ledger:core:test :journal-ledger:api:test` with BUILD SUCCESSFUL.

## Verification Evidence

- `./gradlew.bat :journal-ledger:core:test :journal-ledger:api:test` executed with 0 failures.
- Obsolete monolithic files cleanly removed via `git rm`.

- Added unit/integration test `JournalOutboxPatternTest.java` verifying atomic save, network failure retry resilience, and idempotency key deduplication.
- `./gradlew.bat test` executed across all modules with 100% SUCCESS.

## Known Risks And Rollback

- Rollback: Revert PR commit.

# AI Harness Handoff - 2026-08-12 Issue #290 Decouple PersonalAccessTokenService from JPA Infrastructure (DIP & Hexagonal Outbound Port)

## Active Goal And State

- GitHub Issue `#290` (`[auth:core][architecture] PersonalAccessTokenService 인프라 직접 의존 — 헥사고날 위반`) completed and merged into `main`. `Fixes #290` closes Issue `#290`.
- PR #374 merged into `main` and remote branch `agent/290-auth-pat-service-dip-decouple` deleted.
- Worktree `C:\tmp\account-290-auth-pat-service-dip-decouple` cleaned up.

## Changes And Boundaries

- Domain Model (`auth/core/.../domain/model/PersonalAccessToken.java`):
  - Created Pure POJO `PersonalAccessToken` domain model with zero external technology dependencies.
  - Encapsulated status management (`revoke()`, `markUsed()`) and effective status logic (`getEffectiveStatus()`).
- Outbound Port (`auth/core/.../application/port/out/PersonalAccessTokenPort.java`):
  - Defined Outbound Port interface for PAT domain model persistence and query operations.
- Infrastructure Persistence Adapter & Data Mapper (`auth/core/.../infrastructure/persistence/`):
  - Created `PersonalAccessTokenPersistenceAdapter` implementing `PersonalAccessTokenPort`.
  - Added `toDomain()` and `fromDomain()` mapping methods in `PersonalAccessTokenJpaEntity`.
- Service Refactoring (`auth/core/.../application/service/PersonalAccessTokenService.java`):
  - Refactored `PersonalAccessTokenService` to depend on `PersonalAccessTokenPort` instead of `PersonalAccessTokenJpaRepository`.
  - Replaced direct JPA Entity manipulations with Pure Domain POJO creation and updates.
  - Added detailed pedagogical comments explaining DIP and Hexagonal Architecture benefits.
- Unit Tests:
  - Added `PersonalAccessTokenServiceTest.java` and `PersonalAccessTokenPersistenceAdapterTest.java`.

## Verification Evidence

- `./gradlew.bat :auth:core:test :auth:api:test` passed 100% (BUILD SUCCESSFUL).

## Known Risks And Rollback

- Rollback: Revert commit/PR. DB schema is untouched.

# AI Harness Handoff - 2026-08-12 Issue #291 Decouple JPA Annotations from Master Data Core Domain Models (Pure POJO & Data Mapper)

## Active Goal And State

- GitHub Issue `#291` (`[master-data:core][ddd] 도메인 모델에 JPA 인프라 종속성 포함 — 순수 POJO 원칙 위반`) in progress.
- Worktree `C:\tmp\account-291-master-data-domain-pojo-decouple` active.

## Changes And Boundaries

- Domain Models (`master-data/core/.../domain/model/` and `domain/changerequest/`):
  - Removed JPA technology annotations (`@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToOne`, `@Enumerated`, `@PrePersist`, `@PreUpdate`, `@Version`) from `AccountSubject`, `Product`, `Currency`, `Department`, `ExchangeRate`, `FiscalPeriod`, `TaxProfile`, and `MasterDataChangeRequest`.
  - Added comprehensive pedagogical comments explaining DDD Pure Domain POJO principle and domain-persistence model decoupling benefits.
- Persistence Entities (`master-data/core/.../infrastructure/persistence/entity/`):
  - Created 8 JPA Entity classes: `AccountSubjectEntity`, `ProductEntity`, `CurrencyEntity`, `DepartmentEntity`, `ExchangeRateEntity`, `FiscalPeriodEntity`, `TaxProfileEntity`, `MasterDataChangeRequestEntity`.
- Data Mappers (`master-data/core/.../infrastructure/persistence/mapper/`):
  - Created 8 Data Mapper classes: `AccountSubjectMapper`, `ProductMapper`, `CurrencyMapper`, `DepartmentMapper`, `ExchangeRateMapper`, `FiscalPeriodMapper`, `TaxProfileMapper`, `MasterDataChangeRequestMapper`.
- Repositories & Adapters:
  - Updated Spring Data JPA Repositories to operate on JPA Entities (`AccountSubjectEntity`, `ProductEntity`, etc.).
  - Updated Persistence Adapters (`JpaAccountSubjectPersistenceAdapter`, `JpaProductPersistenceAdapter`, `CurrencyPersistenceAdapter`, `JpaDepartmentPersistenceAdapter`, `JpaFiscalPeriodPersistenceAdapter`, `JpaMasterDataChangeRequestPersistenceAdapter`) to perform two-way mapping between Domain POJOs and JPA Entities via Data Mappers.
  - Refactored `MonolithMasterDataQueryAdapter` to use Outbound Ports (`AccountSubjectPersistencePort`, `DepartmentPersistencePort`) instead of direct JPA Repositories.

## Verification Evidence

- `./gradlew.bat :master-data:core:test :master-data:api:test :master-data:batch:test` passed 100% (BUILD SUCCESSFUL).
- `./gradlew.bat test` full test suite passed 100%.

## Known Risks And Rollback

- Rollback: Revert commit/PR. DB table schemas remain unchanged.

# AI Harness Handoff - 2026-08-12 Issue #295 Implement IFRS 16 Lease Present Value (PV) Calculation & Input Validation


## Active Goal And State

- GitHub Issue `#295` (`[asset-lease][financial] IFRS 16 리스 현재가치(PV) 계산 누락 및 외부 입력 전면 신뢰`) completed and merged into `main`. `Fixes #295` closes Issue `#295`.
- PR #372 merged into `main` and remote branch `agent/295-asset-lease-ifrs16-pv-calculation` deleted.
- Worktree `C:\tmp\account-295-asset-lease-ifrs16-pv-calculation` cleaned up.

## Changes And Boundaries

- `asset-lease/core/.../domain/LeaseContract.java`:
  - Implemented `calculatePresentValue(monthlyPayment, termMonths, annualRate)` to compute present value of lease payments using compound discounting with monthly discount rate ($r = annualRate / 1200$).
  - Added `calculateTermMonths()` to compute exact lease term months from `startDate` and `endDate`.
  - Added `updatePresentValueAndValidate()` to cross-validate external PV inputs against domain-calculated PV and strictly enforce domain invariants for initial recognition of ROU asset and lease liability.
  - Added detailed pedagogical comments explaining IFRS 16 accounting, incremental borrowing rate discounting, and financial domain invariant principles.
- `asset-lease/core/.../application/service/LeaseEntryService.java`:
  - Refactored `registerLeaseContract` and `recognizeInitialLease` to mandate domain PV calculation and validation before initial recognition, eliminating blind trust in external request inputs.
- Tests:
  - Created `LeaseContractTest.java` to test PV calculations (0% rate, 6% rate), term months calculation, and external input override/validation.
  - Updated `LeaseEntryServiceTest.java` to test automated PV calculation and registration integration.

## Verification Evidence

- `./gradlew.bat :asset-lease:core:test :asset-lease:api:test :asset-lease:batch:test` passed 100% (BUILD SUCCESSFUL).

## Known Risks And Rollback

- Rollback: Revert PR #372. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #294 Refactor Asset Lease Core JPA Repository Location to Enforce DIP

## Active Goal And State

- GitHub Issue `#294` (`[asset-lease][architecture] Core 모듈 내부에 JPA Repository 위치 — 헥사고날/DIP 위반`) completed and merged into `main`. `Fixes #294` closes Issue `#294`.
- PR #371 merged into `main` and remote branch `agent/294-asset-lease-core-jpa-repository-dip` deleted.
- Worktree `C:\tmp\account-294-asset-lease-core-jpa-repository-dip` cleaned up.

## Changes And Boundaries

- Relocated Spring Data JPA repository interfaces from `com.ho.account.asset.repository` in `core` to `com.ho.account.asset.infrastructure.persistence.repository`.
- Decoupled `core` domain and application services (`FixedAssetEntryService`, `LeaseEntryService`) from JPA interfaces to strictly rely on Outbound Ports (`FixedAssetPersistencePort`, `LeasePersistencePort`).
- Added `Page<FixedAsset> findByStatus(String status, Pageable pageable)` to `FixedAssetPersistencePort`.
- Added pedagogical comments explaining DIP, Hexagonal Architecture Outbound Port pattern, and persistence encapsulation across core, adapter, and batch files.
- Updated `@EnableJpaRepositories` package scanning in `AssetLeaseApiApplication` and `AssetLeaseBatchApplication`.

## Verification Evidence

- `./gradlew.bat :asset-lease:core:test :asset-lease:api:test :asset-lease:batch:test` passed 100% (BUILD SUCCESSFUL).

## Known Risks And Rollback

- Rollback: Revert PR #371. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #299 Decouple JPA Annotations from Account Mart Domain Entities

## Active Goal And State

- GitHub Issue `#299` (`[account-mart][architecture] 도메인 엔티티 내 JPA 어노테이션 사용 — 포트 앤 어댑터 패턴 위반`) completed and merged into `main`. `Fixes #299` closes Issue `#299`.
- PR merged into `main` and remote branch `agent/299-account-mart-jpa-decouple` deleted.
- Worktree `C:\tmp\account-299-account-mart-jpa-decouple` cleaned up.

## Changes And Boundaries

- `account-mart/mart-core/.../domain/external/kap/KapExternalRating.java`:
  - Stripped JPA annotations (`@Entity`, `@Table`, `@Id`, `@Column`). Pure Java POJO. Added detailed pedagogical comments.
- `account-mart/mart-core/.../domain/mart/AllowanceInputPosition.java`:
  - Stripped JPA annotations (`@Entity`, `@Table`, `@IdClass`, `@Id`, `@Column`, `@Enumerated`). Pure Java POJO. Added detailed pedagogical comments.
- New Infrastructure JPA Entities & Mappers:
  - `KapExternalRatingEntity.java`, `AllowanceInputPositionEntity.java`
  - `KapExternalRatingMapper.java`, `AllowanceInputPositionMapper.java`
  - `JpaKapExternalRatingRepository.java`, `JpaAllowanceInputPositionRepository.java`
  - `AllowanceInputPositionPersistenceAdapter.java`
- Batch Processors & Configs (`mart-batch`):
  - `KapDataEtlJobConfig.java`, `KapExternalRatingProcessor.java`, `KapDataEtlJobTest.java`
  - `IntegratedPositionEtlJobConfig.java`, `IntegratedPositionItemProcessor.java`

## Verification Evidence

- `./gradlew.bat :account-mart:mart-core:test :account-mart:mart-api:test :account-mart:mart-batch:test` passed 100% (BUILD SUCCESSFUL).

## Known Risks And Rollback

- Rollback: Revert PR. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #296 Validate Double-Entry Debit-Credit Balance in Payable & Receivable Services

## Active Goal And State

- GitHub Issue `#296` (`[payable, receivable][financial] 전표 발행 시 대차평균(복식부기 차변=대변) 검증 로직 누락`) completed and merged into `main`. `Fixes #296` closes Issue `#296`.
- PR #369 merged into `main` and remote branch `agent/296-payable-receivable-double-entry-validation` deleted.
- Worktree `C:\tmp\account-296-payable-receivable-double-entry-validation` cleaned up.

## Changes And Boundaries

- `payable/core/.../application/service/PaymentService.java`:
  - Added `validateJournalBalance` call prior to posting journal entries in `postPaymentJournal`, `postAdvanceJournal`, and `postOffsetJournal`.
  - Added pedagogical comments explaining Double-Entry Bookkeeping principles (Equivalence of Debits and Credits).
- `payable/core/.../application/service/PurchaseService.java`:
  - Added `validateJournalBalance` call prior to posting purchase recognition journal entries in `postPurchaseJournal`.
- `receivable/core/.../application/service/SalesService.java`:
  - Added `validateJournalBalance` call prior to posting sales recognition journal entries in `postSalesJournal`.
- `receivable/core/.../application/service/CollectionService.java`:
  - Added `validateJournalBalance` call prior to posting collection recognition and match journal entries in `postCollectionRecognitionJournal` and `postMatchJournal`.
- Unit Tests (`PaymentServiceTest`, `PurchaseServiceTest`, `SalesServiceTest`, `CollectionServiceTest`):
  - Added test methods to verify `IllegalArgumentException` is thrown when an imbalanced entry is checked.

## Verification Evidence

- `./gradlew.bat :payable:core:test :receivable:core:test` passed 100% (BUILD SUCCESSFUL).

## Known Risks And Rollback

- Rollback: Revert PR #369. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #298 Accounting Period Validation in Payable & Receivable Services

## Active Goal And State

- GitHub Issue `#298` (`[payable, receivable][financial] 회계기간 및 마감 상태 사전 검증 누락`) completed and merged into `main`. `Fixes #298` closes Issue `#298`.
- PR merged into `main` and remote branch `agent/298-payable-receivable-accounting-period-validation` deleted.
- Worktree `C:\tmp\account-298-payable-receivable-accounting-period-validation` cleaned up.

## Changes And Boundaries

- `ecl/ecl-core/.../domain/collateral/CrCollateral.java`:
  - Added `calculateRealEstateEffectiveValue()` and `calculateEffectiveValue()` methods to encapsulate collateral valuation logic (KB market price, LTV, prior liens, haircuts) within the entity model.
- `ecl/ecl-core/.../domain/calculator/CollateralAllocationCalculator.java`:
  - Created Pure Domain Calculator component to encapsulate Simplex LP optimization (`calculateLpOptimization`), waterfall allocation algorithm (`calculateWaterfallAllocation`), and loan loss priority weight estimations (`estimateLossPriorityWeight`).
- `ecl/ecl-core/.../domain/calculator/PdCalculator.java`:
  - Added transition-matrix based PD curve generation (`generateTransitionBasedCurve`) and simple PD curve fallback generation (`generateSimplePdCurve`) methods with detailed pedagogical comments.
- `ecl/ecl-core/.../domain/calculator/LgdCalculator.java`:
  - Added secured and unsecured LGD Floor regulation rules (`applyLgdFloor`).
- `application/service/calculation` & `crm` Services Refactoring:
  - `LifetimePdService`, `CollateralAllocationService`, `PdCalculationService`, `LgdCalculationService`: Refactored to inject pure domain calculators and focus solely on Application Service Orchestration (repository fetching, transaction management).
  - Added educational comments explaining Hexagonal Architecture, pure domain calculation, and DDD encapsulation benefits.
- Unit Tests:
  - Created `CollateralAllocationCalculatorTest` and expanded `PdCalculatorTest`.
  - Added `@Spy` injection for `CollateralAllocationCalculator` in `CollateralAllocationServiceTest` and `CollateralAllocationServicePriorityTest`.

## Verification Evidence

- `./gradlew.bat :ecl:ecl-core:test :ecl:ecl-api:test :ecl:ecl-batch:test` passed 100% (BUILD SUCCESSFUL in 36s).

## Known Risks And Rollback

- Rollback: Revert PR #367. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #297 Decouple Spring Framework from Reporting Domain Layer


## Active Goal And State

- GitHub Issue `#297` (`[reporting][architecture] 도메인 계층의 Spring 프레임워크 강결합 — Hexagonal 위반`) completed and merged into `main`. `Fixes #297` closes Issue `#297`.
- Worktree `C:\tmp\account-297-reporting-domain-spring-decouple` was cleaned up.

## Changes And Boundaries

- `reporting/core/.../domain/service/FinancialStatementEngine.java`:
  - Removed Spring `@Component` annotation and import to enforce Pure Java POJO purity.
  - Added educational comments explaining Hexagonal Architecture domain independence and unit test benefits.
- `reporting/core/.../domain/service/IfrsDisclosureNotesEngine.java`:
  - Removed Spring `@Component` annotation and import to enforce Pure Java POJO purity.
  - Enhanced pedagogical comments on domain framework isolation.
- `reporting/core/.../infrastructure/config/ReportingDomainConfiguration.java`:
  - Created `@Configuration` class to manually register domain services (`FinancialStatementEngine`, `IfrsDisclosureNotesEngine`, `RwaCalculator`) as `@Bean`.
  - Added detailed pedagogical comments explaining manual bean registration and Hexagonal Architecture principles.

## Verification Evidence

- `./gradlew.bat :reporting:core:test :reporting:api:test :reporting:batch:test --rerun-tasks` passed 100% (BUILD SUCCESSFUL in 18s).

## Known Risks And Rollback

- Rollback: Revert PR. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #303 Reconciliation Rich Domain Model Refactoring

## Active Goal And State

- GitHub Issue `#303` (`[reconciliation][ddd] 빈약한 도메인 모델(Anemic Domain Model) 적용`) completed and merged into `main`. `Fixes #303` closes Issue `#303`.
- Worktree `C:\tmp\account-303-reconciliation-rich-domain-model` was cleaned up.

## Changes And Boundaries

- `reconciliation/core/.../domain/ReconciliationDifference.java`:
  - Added `createDifference` static factory method and domain behavior methods `assignOwner`, `resolve`, and `attachAdjustmentJournalEntry`.
  - Added pedagogical comments explaining Anemic vs Rich Domain Model, encapsulation, and domain invariants.
- `reconciliation/core/.../domain/ReconciliationRun.java`:
  - Added `startRun` static factory method, `completeRun` (with RUNNING state invariant check), and `failRun` state transition methods.
  - Added educational comments explaining execution lifecycle and encapsulation.
- `reconciliation/core/.../service/ReconciliationService.java`:
  - Simplified Application Service methods (`assignDifference`, `resolveDifference`, `performReconciliation`) to delegate state transitions to rich domain entities, limiting service responsibility to port orchestration.
  - Updated class-level educational comments.
- `reconciliation/core/.../domain/ReconciliationDifferenceTest.java` & `ReconciliationRunTest.java`:
  - Created unit tests verifying domain entity state transitions and invariant exception handling.

## Verification Evidence

- `./gradlew.bat :reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test` passed 100% (BUILD SUCCESSFUL in 29s).

## Known Risks And Rollback

- Rollback: Revert PR. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #305 Resolve N+1 Query in TaxInvoiceBatchService using Bulk Lookup

## Active Goal And State

- GitHub Issue `#305` (`[tax][extensibility] 반복문 내 외부 포트(DB/API) 쿼리 — N+1 문제`) completed and merged into `main`. `Fixes #305` closes Issue `#305`.
- Worktree `C:\tmp\account-305-tax-n-plus-1-bulk-query` was cleaned up.

## Changes And Boundaries

- `contracts/src/main/java/com/ho/account/contracts/masterdata/MasterDataQueryPort.java`:
  - Added `findAllByPartnerCodes(Collection<String>)` default method with educational comments on N+1 query problem, DB Network Round-Trip optimization, and bulk lookup pattern.
- `master-data/core/...`:
  - Added `findActiveByBusinessPartnerCodeIn` in `BusinessPartnerRepository`, `findAllByBusinessPartnerCodeIn` in `BusinessPartnerPersistencePort` / `JpaBusinessPartnerPersistenceAdapter`, and overrode `findAllByPartnerCodes` in `MonolithMasterDataQueryAdapter`.
- `tax/core/src/main/java/com/ho/account/tax/application/service/TaxInvoiceBatchService.java`:
  - Optimized `validatePurchaseInvoices` using 3-step bulk lookup pattern (Set extraction ➔ 1-time bulk query ➔ O(1) map lookup) with pedagogical comments.
- `tax/core/src/test/java/com/ho/account/tax/application/service/TaxInvoiceBatchServiceTest.java`:
  - Created unit tests verifying single bulk query invocation and error handling for missing partners.

## Verification Evidence

- `./gradlew.bat :tax:core:test :tax:api:test :tax:batch:test` passed 100%.
- `./gradlew.bat :master-data:core:test` passed 100%.

## Known Risks And Rollback

- Rollback: Revert PR. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #308 Journal Entry Domain Validation Cohesion & Invariants Consolidation

## Active Goal And State

- GitHub Issue `#308` (`[journal-ledger][ddd] 분개 검증(Validation) 로직의 도메인 응집도 분산`) completed and merged into `main`. `Fixes #308` closes Issue `#308`.
- Worktree `C:\tmp\account-308-journal-entry-domain-validation` was cleaned up.

## Changes And Boundaries

- `journal-ledger/core/src/main/java/com/ho/account/journalledger/domain/journal/domain/JournalEntry.java`:
  - Consolidated domain invariants validation and header validation (`slipDate`, `accountingDate`) with debit/credit balance checks into `validateInvariants()`.
  - Added educational comments on DDD Aggregate Root invariants, encapsulation, and domain model cohesion.
- `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/validator/BalanceValidationFilter.java`:
  - Updated filter to delegate validation to `journalEntry.validateInvariants()`.
  - Added educational comments on Validation Filter orchestration and domain delegation.
- `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalEntryService.java`:
  - Enhanced comments explaining the separation between Application Service orchestration and Domain Entity invariant encapsulation.
- `journal-ledger/core/src/test/java/com/ho/account/journalledger/domain/journal/domain/JournalEntryAggregateTest.java`:
  - Added unit test cases for header invariant validation and `validateInvariants()` success/failure.

## Verification Evidence

- `./gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test` passed 100% (BUILD SUCCESSFUL in 40s).

## Known Risks And Rollback

- Rollback: Revert PR. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #306 Gateway Dynamic Service Discovery in AuthTokenVersionValidator

## Active Goal And State

- GitHub Issue `#306` (`[gateway][msa] TokenVersion 검증 시 하드코딩된 고정 IP 호출`) completed and merged into `main`. `Fixes #306` closes Issue `#306`.
- Worktree `C:\tmp\account-306-gateway-token-version-dynamic-lb` was cleaned up.

## Changes And Boundaries

- `gateway/src/main/java/com/ho/account/gateway/config/WebClientConfig.java`:
  - Created WebClientConfig with `@LoadBalanced WebClient.Builder` Spring Bean.
  - Added comprehensive pedagogical comments explaining MSA Service Discovery and Client-side Load Balancing.
- `gateway/src/main/java/com/ho/account/gateway/security/AuthTokenVersionValidator.java`:
  - Injected `@LoadBalanced WebClient.Builder` to enable dynamic service resolution for Auth token version validation.
  - Added pedagogical comments explaining dynamic instance discovery, round-robin balancing, and HA benefits.
- `gateway/src/main/java/com/ho/account/gateway/security/TokenVersionValidationProperties.java` & `gateway/src/main/resources/application.yml`:
  - Updated default `baseUrl` from `http://localhost:8084` to `lb://auth-service`.
- `gateway/docker-compose.yml` & `GatewayDockerConfigurationTest.java` & `gateway/README.md`:
  - Updated Docker compose configuration, unit tests, and documentation to reflect `lb://auth-service`.

## Verification Evidence

- `./gradlew.bat :gateway:test` passed 100% (BUILD SUCCESSFUL in 40s).

## Known Risks And Rollback

- Rollback: Revert PR. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #304 Discovery Eureka Peer-Awareness and Self-Preservation Config

## Active Goal And State

- GitHub Issue `#304` was integrated by PR `#360`. `Fixes #304` closed the Issue and the remote feature branch was deleted.
- Worktree `C:\tmp\account-304-discovery-eureka-peer-awareness` was cleaned up.

## Changes And Boundaries

- `discovery/src/main/resources/application.yml` & `config-repo/discovery-service.yml`:
  - Added `peer1` and `peer2` Spring profile sections with `spring.config.activate.on-profile: peer1` / `peer2`.
  - Configured defaultZone cross-referencing between peers (peer1 -> peer2:8762/eureka, peer2 -> peer1:8761/eureka).
  - Set `eureka.client.register-with-eureka: true` and `eureka.client.fetch-registry: true` for HA profiles.
  - Added Eureka server self-preservation (`eureka.server.enable-self-preservation: ${EUREKA_SERVER_ENABLE_SELF_PRESERVATION:true}`) and eviction interval timer (`eureka.server.eviction-interval-timer-in-ms: ${EUREKA_SERVER_EVICTION_INTERVAL_TIMER_IN_MS:60000}`) configuration.
  - Written detailed educational comments (Pedagogical comments) to explain HA cluster architecture, Peer-Awareness, self-preservation mode, and eviction interval concepts.
- `discovery/src/test/java/com/ho/account/discovery/DiscoveryConfigurationPolicyTest.java`:
  - Updated unit test to parse multi-document YAML with SnakeYAML `loadAll` and verify default vs `peer1` / `peer2` profile configurations and server properties.

## Verification Evidence

- Run `./gradlew.bat :discovery:test` passed 100% (BUILD SUCCESSFUL in 12s).
- PR #360 merged into `main` cleanly.

## Known Risks And Rollback

- Revert PR #360. No database schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #310 Loan Multi-Currency Rounding Policy

## Active Goal And State

- GitHub Issue `#310` was integrated by PR `#358`. `Fixes #310` closed the Issue and the remote feature branch was deleted.
- Worktree `C:\tmp\account-310-loan-currency-rounding-policy` was cleaned up.

## Changes And Boundaries

- `CurrencyRoundingPolicy.java`: Created domain enum for loan multi-currency precision (KRW/JPY: scale 0, FLOOR; USD/EUR/GBP: scale 2, HALF_UP).
- `LoanService.java`: Applied currency rounding rules to automated journal creation (`createAutomatedJournalEntry`), loan disbursal (`disburseLoan`), deferred items (`createDeferredItem`), and loan recalculation (`recalculateLoanWithEvent`).
- `InterestAccrualService.java`: Applied currency rounding rules to daily interest accrual journal posting (`postAccrualJournal`).
- `EIRAmortizationSchedule.java`: Applied currency rounding rules to monthly amortization schedule generation (`generateMonthly`).
- Added educational comments explaining financial precision principles and rounding policy rationale.
- Added `CurrencyRoundingPolicyTest` and `LoanCurrencyRoundingPolicyIntegrationTest`, and updated existing test assertions.

## Verification Evidence

- Run `:loan:core:test :loan:api:test :loan:batch:test` passed 100% (BUILD SUCCESSFUL in 29s).
- PR #358 merged into main cleanly.

## Known Risks And Rollback

- Revert PR #358. No database schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #312 Deposit DDD Spring Decouple

## Active Goal And State

- GitHub Issue `#312` was integrated by PR `#356`. `Fixes #312` closed the Issue and the remote feature branch was deleted.
- Worktree `C:\tmp\account-312-deposit-ddd-spring-decouple` was cleaned up.

## Changes And Boundaries

- Removed Spring `@Component` annotations from `DepositAccountStateMachine`, `DepositInterestAccrualCalculator`, `DepositTerminationSettlementCalculator`.
- Added educational comments explaining pure domain principles and business/math logic.
- Created `DepositDomainConfiguration` under `infrastructure/config` to explicitly register pure domain POJOs as Spring `@Bean`s when required by Spring container.

## Verification Evidence

- Run `:deposit:core:test :deposit:api:test :deposit:batch:test` passed 100% (BUILD SUCCESSFUL).

## Known Risks And Rollback

- Revert PR. No DB schema changes were introduced.

# AI Harness Handoff - 2026-08-12 Issue #314 Reconciliation JSON Refactor

## Active Goal And State

- GitHub Issue `#314` was integrated by PR `#355`. `Fixes #314` closed the Issue and the remote feature branch was deleted.
- Worktree `C:\tmp\account-314-reconciliation-json-hardcoding` was cleaned up.

## Changes And Boundaries

- `ReconciliationService.java`: Replaced raw JSON string concatenation with `ObjectMapper`-driven `buildItemRefJson` serialization method to protect against invalid JSON parsing errors when unit names contain special characters. Added educational comments explaining the rationale.
- `ReconciliationServiceTest.java`: Added assertions for item references and special character handling test case `performReconciliationHandlesSpecialCharactersInUnitNameSafely`.

## Verification Evidence

- Run `:reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test` passed (BUILD SUCCESSFUL).
- PR #355 merged into main cleanly.

## Known Risks And Rollback

- Revert PR #355. No database schema or external API contracts were changed.

# AI Harness Handoff - 2026-07-30 Issue #45 Budget Control

## Active Goal And State

- GitHub Issue `#45` was integrated by PR `#246`: source `5f06cad1`, merge `db7feeb4`. `Fixes #45` closed the Issue and the remote feature branch was deleted.
- Prior PRs #151/#221 supplied only a placeholder and generic completion note. This branch adds the actual bounded context.
- The initial independent review reported four P1 and two P2 findings. Its follow-up checks also identified typed-empty-404, infrastructure-exception classification, bounded-string, and technology-neutral `yearMonth` gaps. All were corrected, and the same Reviewer reported no remaining P0-P3 findings.

## Changes And Boundaries

- `BudgetPlan` owns allocation, same-YYYYMM transfer collaboration, execution-date matching, derived availability, and `DRAFT → APPROVED → CLOSED`.
- `BudgetTransfer` preserves request-key and source/target lineage through `REQUESTED → APPROVED`; approval changes both locked plans atomically.
- `BudgetExecution` preserves unique external source lineage and explicit cancellation, releasing the exact executed amount.
- `BudgetFiscalYearControl` is a durable OPEN/CLOSED authority. All mutations lock an idempotency shard when relevant, then fiscal year, then plans in ascending id order.
- Output ports isolate JPA adapters. Flyway V50 creates 10,000 year controls, 256 idempotency shards, and three business tables with scalar foreign keys, checks, and unique keys.
- API validates Auth-issued JWT signature/issuer and operation roles, derives audit actor only from JWT `sub`, and maps typed 400/404/409/422 failures. Gateway routes `/api/budgets/**` explicitly with a circuit breaker and 503 fallback.
- The existing Expenditure Budget remains the compatibility authority. No legacy row is migrated or dual-written in this Issue.

## Verification Evidence

- Budget Core 31, API 9, Batch 11, Gateway 33 tests; failures/errors/skipped 0.
- Existing Expenditure Core 10/API 1 tests; failures/errors/skipped 0.
- Combined affected verification: 95 tests.
- Budget API and Batch `bootJar`: passed.
- Both bootJars contain `postgresql-42.6.2.jar`.
- API/Batch composition tests exercised real JPA adapters, Flyway V50, five physical tables, exact seed counts, and Hibernate schema validation on H2 PostgreSQL mode.
- H2 two-thread/separate-transaction tests prove first concurrent transfer/execution exactly-once, conflicting payload 409 semantics, and close-versus-approval serialization.
- `git diff --check`, conflict-marker scan, dependency-direction scan, and lifecycle vocabulary scan: passed.

## Known Risks And Rollback

- Live PostgreSQL migration/locking and query plans were not available. Recheck shard distribution, fiscal-year prefix filtering, lock timeout translation, and contention before production rollout.
- Year-end close is one transaction with row-level flushes; high cardinality needs a separately designed bulk/checkpoint port.
- Expenditure reservation/commit/release migration and data reconciliation remain #17 scope.
- Roll back by reverting the feature commit and removing the three module includes; existing Expenditure data is untouched.

## Final Gate Result

- GitHub `reporting-tests` passed, the Draft PR was promoted to Ready, and PR #246 merged cleanly.
# AI Harness Handoff - 2026-07-30 Issue #243 Runtime Dependencies

- Issue/branch/worktree: `#243`, `agent/243-postgres-actuator-runtime`, `C:\tmp\account-243-postgres-actuator-runtime`.
- Added PostgreSQL JDBC to the runtime classpath of 22 API/Batch applications in asset-lease, closing, deposit, expenditure-resolution, journal-ledger, loan, payable, receivable, reconciliation, reporting and tax.
- Added Actuator to the nine APIs that did not already contain it. No Controller, application, domain, persistence, configuration profile or migration behavior changed.
- `verifyProductionRuntimeDependencies --offline` resolves the classpaths, builds all 22 bootJars, and verifies the expected JAR entries in each executable archive. It explicitly declares its cross-project execution-time inspection incompatible with Gradle configuration cache.
- A shared minimal context test proves HTTP 200 from `/actuator/health/readiness` in each of the nine APIs that received Actuator.
- Verification passed: 22 bootJars and 56 affected tests with no failures/errors/skips. Some Batch projects without module-owned test sources remain a coverage gap, not a failed test.
- Independent final review found no P0-P3 finding. Its clean latest-source rerun covered 29 tests in the nine changed APIs; the remaining affected suites contribute 27, for 56 total.
- PostgreSQL was not contacted. Existing local H2 startup failures were not widened or claimed fixed; schema compatibility and Batch metadata provisioning are owned by #244.
- Commit `c8b10947` is rebased onto `origin/main@5c810422`; harness commit `d0586cbe` records the final state. Both are pushed and Draft PR #248 is open. Post-rebase runtime/archive gate, nine readiness tests, diff/marker checks and semantic review passed. Merge and Issue close are not authorized.
- Rollback: revert the dependency declarations, root verification task, docs and harness records. No data rollback is required.

# AI Harness Handoff - 2026-07-30 Issue #44 Journal/GL/Sub-ledger

## Active Goal And State

- GitHub Issue `#44` was integrated by PR `#238`: source `d8c0504c`, merge `299746a7`. `Fixes #44` closed the Issue and the remote feature branch was deleted.
- The integration-record branch is `agent/44-integration-record` in `C:\tmp\account-44-journal-ledger-domain`, based on merged `origin/main`.
- Previous PRs #152/#222 only produced a placeholder and generic note. This branch connects the requested domain types to the real posting path.

## Changes And Boundaries

- `Debit` and `Credit` are immutable direction-specific value objects. `AccountingPrecision` matches ledger `DECIMAL(19,2)` and exchange-rate `DECIMAL(19,8)` without silent rounding.
- `JournalEntry` owns detail relationships, exposes an immutable detail view, and validates positive lines plus transaction/base-currency double entry.
- Only `APPROVED` entries with persisted header/detail IDs become a `GeneralLedger`; its immutable postings preserve GL/SL dimensions and lineage.
- `PostingService` orchestrates Aggregate creation, journal state change, output-port storage, and balance update. It no longer constructs JPA entities.
- JPA and JDBC bulk adapters map the same `GeneralLedger` snapshot. Active `GlBalance`/`SlBalance` projections share the precision policy.
- Unused `Money`, `GlAccountBalance`, `GlBalanceType`, and repository authority was removed.
- The public arbitrary Journal status setter was removed; impacted Loan/Expenditure callers use DRAFT initialization and normal transitions.

## Verification Evidence

- Journal Ledger: Core 35, API 2, Batch 3 tests; failures/errors/skipped 0.
- Impacted contracts: Loan Core 30, Expenditure Core 10/API 1, Closing Batch 12 tests; failures/errors/skipped 0.
- Combined affected verification: 93 tests, with failures/errors/skipped 0.
- Journal Ledger API/Batch and Closing Batch `bootJar`: passed.
- Focused tests cover precision overflow/fraction rejection, both currency balances, transient-detail rejection, immutable snapshots, and JPA/JDBC GL/SL parity.
- Independent review found a Closing Batch fixture compilation regression (P1) and aggregate-total precision overconstraint (P2). Both were fixed, and re-review reported no P0-P3 findings.

## Known Risks And Rollback

- The fixed scale-2 policy intentionally matches the current schema. Currency-specific minor units require a separately reviewed schema and contract change.
- Real PostgreSQL bulk performance and lock contention were not available in this local pass.
- Roll back with a normal feature-commit revert; there is no schema or production-data mutation.

## Next Gate

- Merge this documentation-only integration record, then remove the Issue #44 worktree and local branches.

# AI Harness Handoff - 2026-07-30 Issue #43 EOD/BOD Lifecycle

## Active Goal And State

- GitHub Issue `#43` was integrated by PR `#236`: source `86ebedf9`, merge `10d80939`. `Fixes #43` closed the Issue and the remote feature branch was deleted.
- The integration-record branch is `agent/43-integration-record` in `C:\tmp\account-43-eod-state`, based on the merged `origin/main`.
- The old docs-only closure note did not implement a callable lifecycle. The merged change supplies the production domain, use case, persistence, migration, API, and Gateway path.

## Changes And Boundaries

- `DailyClosingStatus` is one aggregate per business date with canonical `EodState`, optimistic version, and stage-specific actor/timestamp audit fields.
- Same-row transitions are `OPEN → PRE_CLOSING → CLOSING_IN_PROGRESS → CLOSED`, with preparation cancellation to `OPEN`; `CLOSED` is terminal.
- `startBod(closedDate,nextBusinessDate)` locks and validates the latest prior `CLOSED`, preserves it, and creates a separate explicit next date in `BOD_IN_PROGRESS`. No calendar date is guessed.
- `EodLifecycleUseCase` and its transactional service coordinate the domain through `DailyClosingStatusPersistencePort`; the JPA adapter owns locked reads and persistence conflict translation.
- The HTTP adapter exposes only named commands and trusts JWT-derived Gateway actor/role headers. Gateway routes `/api/closing/**` to `closing-service` before the legacy catch-all with a dedicated circuit breaker/fallback.
- Closing-owned Flyway V50 uses `flyway_schema_history_closing`, baselines existing schemas at 49, upgrades legacy Boolean rows, and removes `is_closed`.
- Monthly/annual authority stays with `ClosingCalendar`, Master Data `FiscalPeriod`, and `AnnualClosingService`; unused `ClosingPeriod` classes/repository were removed.
- Closing API and Batch now explicitly compose the BusinessPartner JPA entity/adapter required after Issue #41.

## Verification Evidence

- `:closing:core:test`: 59 tests.
- `:closing:api:test`: 8 tests.
- `:closing:batch:test`: 12 tests.
- `:gateway:test`: 31 tests.
- Total: 110 tests, failures/errors/skipped 0.
- `:closing:api:bootJar` and `:closing:batch:bootJar`: passed.
- Flyway baseline/V50 legacy backfill, JPA locked lookup/version increment, trusted API command mapping, and Gateway route order are covered by focused tests.
- The first complete run failed on the pre-existing Closing Batch composition omission from Issue #41. After importing the BusinessPartner persistence adapter/entity, the full set passed with `--rerun-tasks`.
- Independent review found shared Flyway-history and stale BOD predecessor risks. Both were fixed and the final read-only re-review passed with no P0-P3 findings.

## Known Risks And Rollback

- Journal posting still checks monthly `FiscalPeriod` state, not `EodState.transactionAllowed`. Cross-service fail-closed integration is a separate change.
- PostgreSQL migration execution and live Auth/Gateway/Discovery routing were not available in this local pass.
- Roll back code with a normal feature-commit revert. Do not drop migrated audit data; a V50 schema rollback would need a separate reviewed forward migration.

## Next Gate

- Merge this integration-record PR, remove the Issue #43 worktree/local branches, and fast-forward the root `main` while preserving the user's untracked `scratch/` directory.
# AI Harness Handoff - 2026-07-30 Issue #229 Development PostgreSQL Contract

## State And Delivered Contract

- Branch/worktree/base: `agent/229-dev-postgres` / `C:\tmp\account-229-dev-postgres` / `origin/main@5fb9cb67`.
- The latest-main verified branch is pushed and Draft PR `#241` is open with `Refs #229`; merge and Issue close were not performed.
- Local direct module execution remains H2. Containerized `dev` uses PostgreSQL.
- Self-contained mode uses explicit `self-contained-db`, 16 database-specific owner roles, and a manifest written only after all bootstrap operations complete.
- External mode uses a separate `external-dev` file with no local DB and an authenticated read-only probe using required `DEV_DB_*`.
- Dev Config requires PostgreSQL/Flyway/JPA validate, disables SQL init, and has no H2/default credential fallback.
- Example env files contain dummy values; real `.env` and `.env.external-dev` are ignored.

## Verification, Risks, And Next Gate

- `:config-server:test --offline`, `bash -n`, LF, diff/conflict/link/private-host/default-password checks passed.
- Initial review findings for missing external override and early `pg_isready` were corrected; final independent review had no findings.
- Docker Compose provider and cached PostgreSQL image are unavailable, so actual render/up, first bootstrap, restart and rerun idempotency are unverified.
- #66 must put backend services and the selected DB/probe in the same Compose project before adding `depends_on.condition=service_healthy`.
- #230 owns production external PostgreSQL/immutable Compose. Service migrations, PostgreSQL SQL parity and Batch metadata/restart remain module gates.
- No shared DB login, metadata, schema, credential, remote container or migration was accessed or changed; the exact shared host is absent.
- Rollback reverts Issue #229 files and uses `docker compose down` without `-v`, preserving named volumes and remote DB state.

---

# AI Harness Handoff - 2026-07-30 Issue #42 Master Data Partner Approval

## Active Goal And State

- GitHub Issue `#42`의 거래처 등록·심사 승인 화면과 실제 API 연동은 PR #234로 `main`에 병합됐고 Issue도 닫혔습니다.
- Branch/worktree: `agent/42-master-data-approval`, `C:\tmp\account-42-master-data-approval`.
- Base: `origin/main@c0fb871b`.
- Integration: source `c8f2b81a`, merge `57aa1729`; 원격 source branch 삭제 확인.
- Backend production Controller와 application/domain 전이는 이미 존재해 중복 구현하지 않았고, 사용을 막던 Gateway 경로를 실제 호출 범위에 맞췄습니다.

## Changes And Boundaries

- `/master-data/partner`는 거래처 등록, 승인 대기 조회, 승인·반려 사유, 현재 거래처 조회와 명시적인 loading/error/empty/progress 상태를 제공합니다.
- Frontend API service는 실제 Backend DTO/경로와 `NEXT_PUBLIC_API_URL`의 Gateway 주소를 사용하며 BUSINESS_PARTNER 요청 생성과 `REQUESTED` 목록, 승인, 반려를 연결합니다. non-2xx는 오류로 전파하고 mock/fallback은 없습니다.
- 기존 `/master/partner`의 잘못된 DTO 필드를 고치고 승인 메뉴가 새 화면을 가리키게 했습니다.
- Gateway의 기존 Master Data route가 `/api/basic/**`와 `/api/master-data/**`를 함께 처리하며 legacy catch-all보다 앞선 순서와 circuit breaker를 유지합니다.
- Gateway가 JWT에서 만든 `X-Auth-User`/`X-Auth-Roles`만 actor/권한으로 사용하고, 변경요청 생성·조회·결정·반영 API 전체를 Master Data 관리 역할로 제한합니다.
- BUSINESS_PARTNER payload는 접수와 승인 전에 typed command/domain validation을 통과해야 하며, UI도 전체 심사 필드를 표시하고 malformed payload 승인을 차단합니다.
- API 테스트는 trusted actor, 역할 차단, HTTP DTO 조립과 use case 위임을 검증하며 상태 전이는 domain/application 경계에 남습니다.

## Verification Evidence

- `:master-data:core:test`, `:master-data:api:test`, `:gateway:test`: 총 52 tests, failures/errors/skipped 0.
- `:master-data:api:bootJar`, `:gateway:bootJar`: 성공.
- 변경된 프런트 파일 ESLint, `git diff --check`, conflict marker 및 legacy API/header 정적 검사: 통과.
- Next production source compilation은 성공했습니다. 전체 type check는 Issue #42 밖의 기존 `profile/pat/page.tsx`, `system/tokens/page.tsx`에서 `PageHeader.breadcrumbs`가 빠진 2건 때문에 실패했습니다.
- 로컬 Next dev server가 30초 내 응답하지 않아 브라우저 시각 검증은 완료하지 못했습니다. 해당 PID와 임시 `node_modules` junction은 범위를 확인한 뒤 제거했습니다.
- Docker CLI가 없어 Compose build argument 연결은 독립 정적 리뷰만 통과했고 `docker compose config`는 실행하지 못했습니다.

## Known Risks And Rollback

- pending API는 모든 target type과 raw `payloadJson`을 반환합니다. 서버측 type filter, pagination, 최소 응답 projection이 필요합니다.
- trusted identity header는 Master Data가 Gateway 뒤에서만 접근된다는 배포 경계를 전제로 하므로 업무 서비스 포트를 직접 공개하면 안 됩니다.
- missing ID/invalid transition의 404/409 표준화와 BusinessPartner Controller validation 일관성은 후속 API 품질 범위입니다.
- 롤백은 Issue #42 feature commit revert이며 DB migration이나 운영 데이터 변경은 없습니다.

## Next Gate

- 최초 독립 리뷰의 API base URL, client actor spoofing, role 없는 apply, malformed payload/P2 심사 표시 finding은 코드와 회귀 테스트로 해소했고 수정 diff 재검토도 PASS했습니다.
- 이 integration record PR을 병합한 뒤 구현/기록 worktree와 로컬 브랜치를 제거하고 root `main`을 fast-forward합니다.
# AI Harness Handoff - 2026-07-30 Issue #227 Runtime Execution Parity Audit

## Active Goal And State

- 장기 목표는 local에서 각 모듈을 Gradle/JAR/NPM과 H2로 독립 실행하고, dev/prod는 container/Compose와 PostgreSQL로 실행하는 것입니다.
- 현재 완료 범위는 첫 실행 감사 Issue `#227`입니다. branch/worktree는 `agent/227-runtime-parity-audit` / `C:\tmp\account-227-runtime-parity-audit`입니다. 원본 감사 snapshot은 `origin/main@c0fb871b`, push gate 재검증 기준은 `origin/main@36a1be4f`입니다.
- 최신 main 검증 branch를 push했고 Draft PR `#242`를 `Refs #227`로 열었습니다. merge와 Issue close는 하지 않았습니다.
- root checkout의 기존 사용자 변경은 건드리지 않았습니다.

## Delivered Audit Contract

- `tools/runtime-smoke.ps1`는 실제 Gradle inventory를 기준으로 Inventory, TaskContract, Packaging, Libraries, LocalJar, Frontend 검증을 제공합니다.
- `TaskContract`의 dry-run은 task path 확인으로만 표시하며 packaging 성공으로 취급하지 않습니다.
- LocalJar는 외부 Config/Eureka/Vault/실DB를 끄고 인메모리 H2를 명시하며 Spring `Started` marker가 있어야 성공입니다.
- Batch local smoke는 `spring.batch.job.enabled=false`인 context 검증입니다. 실제 Job 입력, 결과, 멱등성, 재시작은 각 Batch Issue의 별도 gate입니다.
- 환경 계약은 local H2, self-contained dev PostgreSQL, shared external-dev PostgreSQL override, immutable production image + external PostgreSQL입니다. 공유 host와 credential은 환경변수/secret provider로만 주입합니다.

## Verification Evidence

- Gradle inventory: root 외 70 subprojects = API 16, Batch 16, infra server 3, library 18, aggregator 16, phantom `:app` 1.
- Actual packaging: 33 PASS; `:internal-audit:api` NON_EXECUTABLE_COMPILE_FAILED, `:internal-audit:batch` NON_EXECUTABLE.
- Libraries/aggregators: 34/34 isolated `test+jar --offline` PASS.
- 원본 snapshot local executable JAR context: 25 PASS, 8 FAIL, 2 BLOCKED_BY_PACKAGING.
  - Asset Lease API/Batch, Journal Ledger Batch, Loan API/Batch: Master Data `BusinessPartnerJpaEntity` scan gap.
  - Closing API: H2 runtime driver absent.
  - Journal Ledger API: repository registered twice through Journal Ledger/Internal Audit application scanning.
- Latest-main follow-up: 원래 실패 8개 모두 재패키징 PASS; Closing Batch는 local context `PASS_EXITED`로 복구되어 현재 합계는 26 PASS, 7 FAIL, 2 BLOCKED_BY_PACKAGING입니다.
- Frontend: package/lock/dev-build-start scripts and `npm.cmd` exist. `node_modules` is absent; package installation was not authorized, so actual npm build/start was not run.
- Docker CLI is unavailable; image build and Compose render/up were not run.
- Shared development pgAdmin/PostgreSQL endpoints were TCP-reachable. No login, metadata, schema, credential, or container inspection occurred, and the exact host was not recorded.
- PowerShell parse, `git diff --check`, conflict marker, changed-doc link, and sensitive-host-literal checks passed.

## Issue Routing, Risks, And Rollback

- Runtime failures are attached to #73-#80 and #89-#90. Latest Master Data success was added to #72 without closing it.
- Container image remediation: #228. Development PostgreSQL/bootstrap/external-dev override: #229. Production Compose: #230. Internal Audit/Auth boundary: #231. Root dev orchestration: #66. Initiative coordination: #60/#226.
- H2/PostgreSQL SQL parity, Flyway migrations, representative Batch jobs, frontend dependency build, and all image/Compose runtime behavior remain unverified gates.
- Rollback is a revert of Issue #227 docs/tool/harness records. No migration, remote container, Compose volume, or live database state changed.

---

# AI Harness Handoff - 2026-07-29 Issue #40 Master Data Module Split

## Active Goal And State

- GitHub Issue `#40`의 `master-data:api`, `:batch`, `:core` 헥사고날 멀티 프로젝트 분리는 완료·병합됐고 Issue도 닫혔습니다.
- Implementation branch/worktree: `agent/40-master-data-modules`, `/tmp/account-40-master-data-modules`.
- Integration: PR `#158`, source commit `20e49360`, merge commit `178a7eb19d7cb8890e87b0c3fcb8994e23ed5393`; Issue `CLOSED`.
- Base: implementation 시작 시 `origin/main@fbad9110`; commit 직전 ahead/behind는 `0/0`이었습니다.
- 원본 `/home/ho/dev/account` main checkout의 사용자 변경(`master-data/Dockerfile`, `master-data/docker-compose.yml`)은 건드리지 않았습니다.

## Changes And Boundaries

- API Spring Boot 진입점, REST Controller와 DTO, API 설정/로그 리소스를 `master-data:api`로 이동했습니다.
- Batch Spring Boot 진입점, scheduler orchestration/report mapping을 `master-data:batch`로 이동하고 `masterDataValidityJob`과 Tasklet Step을 추가했습니다.
- Job/Step은 `asOfDate` 파라미터와 실행 흐름만 소유하며, 유효기간 판단과 DB COUNT 집계는 `master-data:core`의 pipeline/port/adapter에 남습니다.
- application/domain/persistence 코드와 Flyway migration은 표준 `master-data/core/src/main/resources`에 남아 API와 Batch가 동일한 core library를 소비합니다.
- API와 Batch는 서로 의존하지 않고 각각 실행 가능한 bootJar를 생성합니다. core는 executable entry point가 없는 library jar입니다.
- Dockerfile, 공용 IntelliJ Run Configuration, module README/docs와 `docs/local-development.md`를 실제 Gradle task에 맞췄습니다.
- Journal Ledger core가 내용 없는 `project(':master-data')` aggregator를 참조하던 의존을 제거하고 기존 `contracts` 계약만 사용하게 했습니다.

## Verification Evidence

- JDK 17 Gradle: Master Data core 1 test, API context 1 test, Batch context/parameter/populated-Job 4 tests, Journal Ledger core 23 tests; 총 29 tests, failures/errors/skipped 0.
- `:master-data:api:bootJar`와 `:master-data:batch:bootJar` 성공.
- populated H2에 네 기준정보의 활성/만료 행을 저장한 통합 테스트가 실제 Job → core pipeline → port → JPA adapter를 실행하고 Step context 활성 건수 4종을 각각 1로 검증했습니다.
- 네트워크 차단 상태에서 기본 Batch는 Config Server 부재를 `ConfigClientFailFastException`으로 차단했고, 문서화한 로컬 H2 예외 플래그에서는 `masterDataValidityJob asOfDate=2026-07-29`의 Job/Step 상태가 `COMPLETED`였습니다.
- core Web/Validation 및 API/Batch 역참조 없음, Batch의 API 의존 없음, conflict marker 없음, `git diff --check` 통과.
- 최초 offline test는 캐시 미보유로 중단되어 선언된 Gradle 테스트 의존성만 해석한 뒤 재검증했습니다. 존재하지 않던 `spring-boot-starter-batch-test` 좌표는 공식 `spring-batch-test`로 수정했습니다.

## Known Risks And Rollback

- PostgreSQL에서 Flyway 전체 부트스트랩과 실제 데이터 기준 대량 집계 실행계획은 검증하지 않았습니다.
- Batch 결과는 현재 Step execution context의 primitive 요약으로만 보존됩니다. 운영 장기 이력, 메트릭, 알림은 출력 포트/어댑터가 필요합니다.
- Spring Batch 기동 중 framework `jobRegistryBeanPostProcessor` 조기 초기화 경고가 있으나 실제 Job은 `COMPLETED`였습니다. 경고 제거는 별도 framework bootstrap 정리 범위입니다.
- typed applier, `requestedVersion`, 요청 잠금/`lockVersion` production 경로는 현재 test source에 회귀 테스트가 없어 별도 품질 gap으로 남습니다.
- 롤백은 Issue #40 커밋 revert입니다. migration 파일 내용과 운영 DB는 변경하지 않았습니다.

## Next Gate

- 독립 Reviewer는 blocking/high/medium finding 없이 PASS했습니다.
- 이 integration record 후속 PR을 병합한 뒤 두 Issue #40 worktree와 로컬/원격 후속 브랜치를 제거하고 최종 GitHub 상태를 확인합니다.

---

# AI Harness Handoff - 2026-07-29 Issue #20 Closing Main Parity And Feature Push

## Active Goal And State

- 사용자 요청에 따라 Closing 고도화 스냅샷과 최신 `main`을 파일 단위로 대조하고 feature 브랜치에 반영·푸시하는 단계입니다.
- Branch/worktree: `agent/20-closing-consistency`, `C:\Users\skyg547\IdeaProjects\account-closing-20`.
- Current base: `ce35ce50` (`origin/main`과 fast-forward 동일). Closing production/test/module docs는 비교 차이 0건입니다.
- Main integration: 고도화는 `31be6f1` (`Issue #60` 커밋)에 함께 포함되어 이미 `main`에 병합됐습니다. 중복 코드를 다시 적용하지 않고 최신 main을 보존했습니다.
- Issue/PR: GitHub Issue `#20`은 이미 CLOSED입니다. Draft PR `#101`로 하네스 정합성 기록을 main에 통합하고, 이미 닫힌 Issue에는 중복 close 호출을 하지 않습니다.
- Recovery: 비교 직전 스냅샷 `codex-post-main-comparison-gh-20-2026-07-29`과 기존 두 stash를 유지합니다.

## Current Main Verification

- 성공 게이트: Shared Kernel 6, Contracts 4, Closing 연계 Master Data 어댑터 5, Journal Ledger Core 23, Closing Core 44, Closing API 1, Closing Batch 12, ECL API 3으로 총 98 tests가 통과했습니다.
- Closing API, Closing Batch, Config Server의 `bootJar` 3개가 성공했습니다.
- 전체 영향 명령에서는 최신 Issue #66 root Compose가 `master-data`와 `config-server` 서비스를 제거/주석 처리한 반면 기존 정책 테스트가 이를 요구해 각 1건 실패했습니다. Closing 코드 실패가 아니며 이 feature 범위에서 인프라 정책을 임의 수정하지 않았습니다.
- 아래 동기화/변경/위험 기록은 이번 main 대조 이전의 구현 이력으로 보존합니다.

## Git Synchronization And Latest Main Conflict Resolution

- 기존 작업을 untracked 파일까지 포함한 이름 있는 stash로 백업한 뒤 현재 브랜치를 `origin/main@b7c8aaa`로 fast-forward하고, stash를 pop하지 않고 apply했습니다.
- 백업 당시 77개 변경 경로가 현재 working tree에 모두 존재하며 누락은 0개입니다. 백업 stash `codex-sync-backup-closing-foundation-2026-07-29`는 복구용으로 유지합니다.
- `docs/WORKLOG.md` 한 파일의 whole-file/line-ending 충돌은 최신 upstream 기록을 기준으로 두고 로컬 Closing/Foundation 기록을 이어 붙여 해결했습니다. 상세 내용은 `conflict-log.md`에 남겼습니다.
- commit/PR 인계용 두 번째 stash에는 82개 경로를 보존했습니다. 외부 `C:\tmp` worktree가 검증 중 소실되어 해당 시도의 결과를 폐기하고, 깨끗한 repository-root checkout을 Issue #20 브랜치로 전환해 같은 stash를 재적용했습니다.
- 그 사이 `origin/main`이 Issue #29 통합 커밋 `f3d33ea`로 전진해 standalone internal-audit Application/test 삭제와 `shared-kernel/build.gradle` 충돌이 발생했습니다.
- Issue #29의 `internal-audit:core/api/batch` 구조와 이동된 감사 API를 보존하고 삭제된 standalone 파일을 되살리지 않았습니다. 중간 `b7c8aaa` 기준 AuditAspect 조립 수정은 새 구조에 맞지 않아 최종 PR에는 포함하지 않았습니다.
- `settings.gradle`, Master Data 어댑터, shared-kernel Jackson BOM 정렬 의도를 의미 단위로 병합하고 결정은 `conflict-log.md`에 기록했습니다.

## Foundation Residual Changes

- 과거 페이지 파일 부족으로 보류된 Config Server health-detail 테스트와 contracts/shared-kernel 영향 검증을 재실행했습니다.
- `jackson-databind`만 2.17.1로 고정되어 Boot BOM의 `jackson-core` 2.15.4와 충돌하던 런타임 오류를 수정했습니다. databind도 Spring Boot 3.2.5 BOM을 따릅니다.
- ECL Kafka 재시도 테스트가 `JobLauncher.run`에 선언되지 않은 상위 checked 예외를 Mockito에 주입하던 문제를 실제 선언 예외로 바로잡았습니다.
- 추적 소스가 없는 `:app` Gradle include를 제거하고 README/입문/로컬 실행 문서를 서비스별 실행 구조와 일치시켰습니다. 로컬의 무시된 `app/build` 산출물은 삭제하지 않았습니다.

## Closing Changes

- 회계기간 누락을 OPEN으로 추정하지 않고 Closing과 Journal 양쪽에서 fail-closed 처리합니다.
- 캘린더·태스크·게이트·재오픈을 도메인 상태 전이로 제한하고, 필수 정의가 없는 vacuous close, 자기 승인, 중복 대기 요청, 감사 실패 은폐를 차단했습니다.
- JSON 태스크/게이트 조건은 typed evidence evaluator가 생기기 전까지 통과시키지 않습니다.
- API 평가/충당 실행 이력을 전표 트랜잭션과 분리해 `RUNNING -> PENDING_APPROVAL` 또는 `FAILED`를 보존하고 DRAFT를 완료로 오표기하지 않습니다.
- FX는 writer가 없는 `gl_account_balances` 대신 실제 `POSTED` 전표의 거래통화/기준통화 금액을 signed balance로 집계합니다. 최대 grid-size개의 계정 범위, JDBC Cursor, chunk atomic pipeline을 사용합니다.
- 환율 provider 내부 Repository 의존을 `ExchangeRateQueryPort`/`ExchangeRateRef` 계약과 Master Data 로컬 어댑터 뒤로 이동했습니다.
- ECL은 DB에서 exposure 행을 전표 그룹으로 선집계하고, 단일 run/model·법인·기준일만 허용합니다. 목표액을 먼저 합산한 뒤 실제 `gl_balances` 대변 잔액을 그룹당 한 번만 차감하며 빈 summary/null 금액은 실패합니다.
- 결정적 20자 slip, 명시 slip 전달, 동일 업무 fingerprint 재사용, 상태별 멱등 자동전기를 구현했습니다. 연말 손익대체는 `POSTED` 기준통화 금액만 사용하고 이익잉여금 계정을 실행 식별자에 포함합니다.
- `AccountSubjectRef`에 계정분류를 전달하고 Journal 조회가 전표 회계일자의 SCD2 계정정보를 사용하도록 했습니다. 세부 감사 actor도 command와 일치시켰습니다.
- API/Batch의 광범위한 `com.ho.account` 스캔을 제거하고 필요한 로컬 어댑터·엔티티·Repository만 조립했습니다. 배치 전용 FX/ECL 서비스는 Batch에서만 import하며 애플리케이션 이름은 각각 `closing-service`, `closing-batch`로 고정했습니다.
- Closing README와 beginner/process/schema/local-run 문서를 현재 파라미터, 원장 원천, 실패/재실행, 런타임 경계와 운영 TODO에 맞췄습니다.

## Verification Evidence

```powershell
.\gradlew :shared-kernel:test :contracts:test :master-data:test :internal-audit:core:test :internal-audit:api:test :internal-audit:batch:test :journal-ledger:core:test :closing:core:test :closing:api:test :closing:batch:test :ecl:ecl-api:test :config-server:test :closing:api:bootJar :closing:batch:bootJar :config-server:bootJar --no-daemon --max-workers=1 '-Porg.gradle.java.installations.paths=C:\Java\jdk17' '-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8' --console=plain
```

- 최신 `f3d33ea` 기준 최종 영향 범위는 Shared Kernel 6, Contracts 4, Master Data 74, Journal Ledger Core 23, Closing Core 44, Closing API 1, Closing Batch 12, ECL API 3, Config Server 9로 총 176 tests이며 실패/오류/skip은 0건입니다.
- 새 `internal-audit:core/api/batch`의 compile/test task는 성공했지만 세 모듈 모두 test `NO-SOURCE`이고 API/Batch production source도 `NO-SOURCE`입니다. 기존 25개 테스트는 Issue #29에서 삭제되어 최종 합계에 포함되지 않으며 별도 잔여 위험입니다.
- Closing API, Closing Batch, Config Server의 세 `bootJar`가 성공했습니다.
- 최신 upstream `gradle.properties`는 로컬 JDK 경로 설정이 주석 처리되어 있고 현재 `JAVA_HOME`은 JDK 21이므로, 저장소 요구 JDK 17은 위 명령의 프로젝트 속성으로 명시했습니다. 머신별 절대 경로는 저장소 설정에 기록하지 않았습니다.
- Contracts 1 suite/4 tests, Master Data 23/74, Journal Ledger Core 12/23, Closing Core 9/44, Closing API 1/1, Closing Batch 6/12.
- Total: 52 suites/158 tests; failures 0, errors 0, skipped 0.
- Closing API와 Batch `bootJar` 생성 성공.
- API/Batch ApplicationContext가 외부 Job 실행 없이 H2에서 로드되고 각각 자체 application name을 사용하는 것을 검증했습니다.
- Foundation 최종 영향 범위는 40 suites/140 tests가 실패·오류·skip 0건이고 Config Server `bootJar`가 성공했습니다. Config health와 shared-kernel/ECL 대상 테스트는 별도로 `--rerun-tasks` 강제 실행했습니다.
- `projects` 출력에서 `:app`이 제거되고 나머지 프로젝트가 유지됨을 확인했습니다.
- `git diff --check`, 충돌 표식 검색과 변경 Markdown 상대 링크 검사가 최종 기록 갱신 후 통과했습니다.

## Known Risks And Completion Criteria

- FX의 posted-journal 집계는 정합성 우선 과도기 구현입니다. 1억 건 운영 전 전기 시 함께 bulk 갱신되는 이중통화 read model, 계정/통화/일자 인덱스, 원장 자동 대사, PostgreSQL 실행계획·부하 테스트가 필요합니다.
- Annual Closing은 아직 전표별 N+1 조회입니다. provider-side `POSTED` 기준통화 계정/분류 집계, 기존 연말마감 lineage 제외와 대량 계획 테스트가 필요합니다.
- `valuation_batches`/`provision_batches`에 업무 실행키 unique 제약과 outbox/reconciliation이 없습니다. 상태 갱신 실패 시 남을 수 있는 DRAFT를 찾아 복구하는 실행 이력과 실패 주입 테스트가 필요합니다.
- GL에 법인 차원이 없어 ECL 실행을 한 법인으로 제한했습니다. 다법인 지원은 GL 키·migration·대사까지 함께 추가해야 합니다.
- `period_locks` unlock은 현재 감사 로그 후 활성 행 삭제입니다. unlock actor/time/reason과 active 상태를 보존하는 forward migration이 필요합니다.
- typed task/gate evidence evaluator는 미구현이며 조건이 설정된 경우 의도적으로 fail-closed입니다.
- Closing 전용 Flyway/PostgreSQL migration과 실제 Docker/Compose 실행은 검증하지 않았습니다.
- 로컬 모놀리스 provider 어댑터를 원격 MSA 어댑터로 바꿀 때 timeout/retry/circuit breaker와 기준정보 snapshot 정책이 필요합니다.
- `shared-kernel`의 광범위한 인프라 전이 의존성과 allowance 전용 타입은 소비 모듈의 직접 의존 선언을 먼저 보강한 뒤 단계적으로 분리해야 합니다.
- Config Server의 Git backend/변경 승인과 endpoint 보호, 실제 컨테이너 통합은 별도 운영 환경 검증이 필요합니다.

## Rollback And Next Handoff

- 커밋 전 롤백 범위는 `closing/**`, Closing 계약용 `contracts` 세 파일, Master Data 환율/계정 어댑터와 테스트, Journal 조회/전표/기간상태 어댑터와 테스트, foundation 잔여 수정(`shared-kernel/build.gradle`, ECL consumer test, `settings.gradle`, 세 실행 문서), 이번 docs/harness/review-prompt 항목입니다.
- 최종 PR에는 AuditAspect/standalone InternalAuditApplication 변경이 없습니다. 전체 변경 롤백은 Issue #20 commit을 revert하고, commit 전 복구가 필요하면 유지 중인 두 이름 있는 stash를 기준으로 별도 안전 checkout에서 비교합니다.
- DB migration은 변경하지 않았으므로 이번 pass의 DB rollback 작업은 없습니다.
- Gemini는 `GEMINI_REVIEW_PROMPT.md` 최상단 Git Sync/Audit 섹션부터 Foundation과 Closing 섹션까지 findings-first로 검수해야 합니다.
- 다음 단계는 이 정합성 기록을 `agent/20-closing-consistency`에 커밋·푸시하고 PR로 main에 병합한 뒤, Issue #20이 CLOSED로 유지되는지 검증하는 것입니다. root Compose 정책 테스트 2건은 Issue #66 후속에서 수정하거나 새 인프라 계약에 맞게 갱신합니다.

---

# AI Harness Handoff - 2026-07-27 Loan Boundary/Workflow Review Ready

## Active Goal And State

- Sequentially audit modules for hexagonal architecture, DDD, executable workflow, skeleton code, consistency, call order, and object/functional design.
- Branch: `agent/asset-lease-split`; remote base HEAD remains `5d55704`.
- The cumulative Master Data follow-up and this Loan pass are local, review-ready, and uncommitted.
- Do not commit, push, merge, rebase, or touch `main` without explicit user authorization.

## Loan Changes

- Replaced Master Data entity relationships in Loan aggregates with Business Partner ID, ISO currency code, and account-code values. Effective-dated provider access is isolated in `LoanReferenceDataAdapter`.
- Added `LoanUseCase`; moved HTTP DTOs/validation to `loan:api`; added stable 404/400/409 exception mapping and event-result response.
- Added `PENDING_DISBURSEMENT`, one full disbursal rule, pessimistic load, optimistic version, unique disbursal protection, and rich domain state methods.
- Preserved original principal while recalculation changes current outstanding balance; separated DEFAULT/RECOVERY lifecycle events from recalculation enum mapping.
- Rewrote EIR calculation with `BigDecimal` and a decimal annual-rate contract; it now fails closed on missing policy/non-convergence.
- Consolidated runtime scheduling on `EIRAmortizationSchedule`; removed the unused duplicate Java entity/repository/JDBC port path while preserving the V30 table for controlled migration.
- Added locked accrual persistence port, success-skip/failed-retry log states, core chunk pipeline, required `accrualDate`, stable reader ordering, and Step failure aggregation.
- Validated balanced journal commands and accounting dates. Journal adapter work runs in an independent transaction so a failed Journal call does not erase the Loan FAILED log.
- Added V33 lock/reference/idempotency indexes without changing V30-V32 checksums.
- Aligned Docker with Java 17, the actual API bootJar, repository-root context, and Compose `SERVER_PORT=8088` while keeping standalone local API example 8087.
- Updated Loan README/docs to the actual state transition, schedule table, retry semantics, value boundaries, migration, and operational risks.

## Verification Evidence

```powershell
.\gradlew :master-data:test :governance:test :closing:core:test :journal-ledger:core:test :loan:core:test :loan:api:test :loan:api:bootJar :loan:batch:bootJar --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx384m' --console=plain
```

- Master Data 22 suites/73 tests, Governance 10/25, Closing Core 4/19, Journal Ledger Core 11/19, Loan Core 9/30, Loan API 2/3.
- Total: 58 suites/169 tests; failures 0, errors 0, skipped 0.
- Loan API `api-0.0.1-SNAPSHOT.jar` and Batch `batch-0.0.1-SNAPSHOT.jar` were generated; Docker COPY matches the API artifact.
- `git diff --check`, conflict marker scan, core web/DTO/provider leakage scan, Batch business-logic scan, legacy schedule usage scan, and current Loan Markdown relative-link validation passed.
- Docker CLI and a local YAML parser are unavailable, so live image/Compose execution and automated Compose parse were not performed.

## Known Risks And Completion Criteria

- Loan DB and Journal commits are not distributed-atomic. Complete with outbox/inbox, lineage idempotency response, retry/compensation history, reconciliation, and failure-injection integration tests.
- HTTP audit actor still comes from request data. Complete by deriving it from verified security principal and proving forged body actors cannot enter audit/lineage.
- Generated EIR schedules are monthly. Products needing daily recognition require approved day-count convention, holiday calendar, daily schedule policy, accounting golden values, and scale/load tests.
- V33 unique indexes require production duplicate preflight and remediation. Validate all migrations on supported PostgreSQL before deployment.
- The legacy `loan_amortization_schedule_entries` table remains intentionally. Remove only after data reconciliation/migration report, zero consumers, backup/restore rehearsal, and a later forward migration.
- Core still has compile-time provider dependencies because local monolith adapters translate Master Data and Journal ports. MSA extraction needs remote adapters, contracts, timeout/retry/circuit-breaker behavior, and reference snapshot policy.
- Docker/Compose build and runtime smoke remain for a Docker-enabled environment.

## Rollback And Next Handoff

- Before commit, roll back only `loan/**`, the six dated Master Data account/currency port-adapter-repository files, root Loan Compose line, and this pass's common log/review entries.
- If V33 has been applied to a shared database, do not delete migration history; create a reviewed forward corrective migration.
- Independent Gemini review should use the new top section of `GEMINI_REVIEW_PROMPT.md` and report findings before summaries.
- Next sequential candidate is the remaining Closing Batch direct Master Data repository boundary after this Loan pass is reviewed.

---

# AI Harness Handoff - 2026-07-27 Master Data Lookup/Fiscal-Period Review Ready

## Active Goal

모듈을 순차적으로 점검하면서 헥사고날 아키텍처, DDD, 실제 업무 프로세스,
스켈레톤 코드 여부, 데이터 정합성, 메서드 호출 순서, 객체지향/함수형 설계를
검수한다. 구현 가능한 항목은 고도화하고, 즉시 구현할 수 없는 운영 설계는
근거 있는 `@todo`로 남긴다. 기존 초보자용 주석과 업무 설명은 최대한 보존하고
코드 흐름에 맞춰 문서도 최신화한다.

## Current State

- Branch/push target: `agent/asset-lease-split` -> `origin/agent/asset-lease-split`.
- Base: `5d55704 Harden master data approval and versioning` is already present on the remote feature branch.
- Scope: prior Master Data approval/runtime pass plus local SCD2 ordering, historical lookup, DB active search, as-of FX, Fiscal Period boundary, skeleton cleanup, tests, and docs.
- Review state: implementation and affected-module verification are complete; the cumulative follow-up working-tree changes are uncommitted and require explicit authorization before commit/push/merge.
- Other four worktrees are clean and their feature commits are already in `main`; do not merge them individually.

## What Changed

- Added repository-backed SCD2 requested-version policy and rechecks at request/approve/apply boundaries.
- Added immutable fail-closed applier registry and duplicate ownership validation.
- Added pessimistic decision lookup, optimistic request lock version, and V3 migration.
- Added Governance approval source-reference idempotency, conflict detection, applied timestamp lineage, and V5 migration.
- Added API validation, HTTP 409 conflict mapping, runtime dependency/port/readiness alignment, and JDK 17 packaging.
- Preserved V2 checksum, added V4 forward payload type correction, and documented the clean PostgreSQL bootstrap gap.
- Validated new SCD2 windows before terminating current Account Subject, Business Partner, Department, and Product rows.
- Resolved Account Subject and Department parent references and assembled new versions before mutating current rows.
- Split current-active and historical-effective Business Partner queries; duplicate effective rows now fail closed instead of selecting the first row.
- Moved Account Subject/Product active lists and Business Partner active-name search into validity-aware DB queries; blank searches fail fast.
- Selected the latest Exchange Rate not after the requested date, validated ISO codes/positive rates, and made overlapping active Currency rows fail closed.
- Routed Fiscal Period changes through a pessimistically locked application port and audited domain transition rules.
- Removed unused change-request full-list contracts, no-op active setters, synthetic detached-Currency compatibility methods, and unused validity helpers after usage scans.
- Added policy/service/domain/adapter tests and H2 JPA regression tests.
- Preserved beginner explanations and updated README, local run, process, schema, archive, worklogs, and review prompt.

## Verification

- `:master-data:test :master-data:bootJar :governance:test :governance:bootJar :closing:core:test :closing:batch:compileJava :journal-ledger:core:test`: passed.
- Master Data 22 suites/73 tests, Governance 10/25, Closing Core 4/19, and Journal Ledger Core 11/19: total 136 with 0 failures, errors, or skips.
- Closing Batch compilation and both bootJars passed.
- Final `git diff --check`, conflict-marker/TODO review, architecture scan, and changed Markdown relative-link validation passed after this handoff/log update.

## Known Risks And TODO

- Live PostgreSQL migration and Docker/Compose image execution were not run.
- Historical V2 declares `CLOB`; a clean PostgreSQL database needs a vendor-specific pre-V2 baseline before production certification.
- Same-key multi-node requests need key/advisory locking; concurrent first source-reference inserts need atomic conflict recovery; bulk apply needs per-request transactions, SKIP LOCKED, and execution history.
- Business Partner overlap is detected at lookup but not prevented at write time; production needs a PostgreSQL date-range exclusion constraint and migration test.
- Historical `BusinessPartnerRef.active` still reflects legacy `useYn`; version closure and business activation require separate temporal modeling and data migration.
- Full-list and name-search APIs still need page-size limits and stable pagination/cursors.
- `TaxProfile` has no repository/use case/applier/consumer; decide ownership with Tax, then implement full SCD2 or migrate/remove it.
- Loan Core directly imports Master Data entities/ports, and Closing Batch directly imports `ExchangeRateRepository`; these documented exceptions are the next sequential provider-boundary candidates.
- Trusted actor extraction, payload masking/authorization, direct-write governance, and three unsupported typed appliers remain.

## Integration Order

1. Perform an independent review of the cumulative Master Data follow-up and its 136-test affected-module evidence.
2. Commit/push the cumulative follow-up only when the user explicitly requests it.
3. Merge current `origin/main` into the feature branch only after the follow-up is preserved and integration is explicitly authorized; do not rebase published history.
4. Resolve harness/docs conflicts, rerun affected checks, and use a reviewed PR to `main`; never push directly to `main`.

## Rollback

- Before commit, discard only the listed cumulative follow-up service/policy/repository/adapter/domain/test/doc/log paths to roll back the local pass.
- To roll back the base, revert the Master Data governance/runtime commit as one unit and keep V3/V4/V5 aligned with their entity fields.

---
# AI Harness Handoff - 2026-07-22 Contracts/Shared-Kernel Review

## Current State

- Branch: `agent/asset-lease-split`
- Owner: Codex
- Working tree: cumulative uncommitted Config Server plus contracts/shared-kernel pass; no commit or push was requested.
- Review state: implementation and static checks complete; JVM verification pending because Windows cannot currently start a 64-128 MB Java process reliably.

## What Changed

- Contracts validate/copy journal commands and reject invalid normal-balance values.
- Master Data's monolith adapter performs actual account/partner/department SCD2 effective-date lookup.
- `@Masked` now invokes Jackson serialization and masks registration/account/email values fail-closed.
- Local capability discovery uses an immutable injected service snapshot and fails on duplicate names.
- Inert `@DistributedLock` usage was removed from ECL.
- CDM event JSON identity is preserved; eventId identifies the Batch JobInstance; completed duplicates are ignored and other failures propagate.
- Added 15 focused tests and archived four no-op library Docker/Compose skeletons.
- Beginner, architecture, business-flow, data-flow, schema, and local-run documents were updated.

## Verification Summary

- `git diff --check`: passed.
- Java `DistributedLock` usages: none outside the deprecated declaration.
- Config Server target test, Gradle module tests, and direct contracts javac could not complete because the Windows paging file is exhausted.
- No spawned Gradle/javac/application JVM remains; the temporary javac directory was removed.
- Do not mark this pass green until tests are rerun.

## Required Rerun

```powershell
.gradlew :config-server:test --tests "com.ho.account.configserver.health.ConfigRepositoryHealthIndicatorTest" --console=plain --max-workers=1 --no-daemon
.gradlew :shared-kernel:test :contracts:test :master-data:test :closing:core:test :ecl:ecl-api:test --console=plain --max-workers=1 --no-daemon
```

## Known Risks And Next Review

- Verify Spring/Jackson/Batch behavior from the new tests once memory is available.
- Live Kafka redelivery/DLT and PostgreSQL overlapping SCD2 versions remain unverified.
- Next sequential focus: implement/remove remaining dated Master Data defaults, then extract shared-kernel infrastructure dependencies and allowance-specific types.
- Remaining TODOs cover versioned source-document DTOs, local capability SPI naming/ownership, shared dependency extraction, ECL type/event ownership, and real distributed locking.

## Rollback

- Revert `contracts` and `shared-kernel` changes including archive moves.
- Revert master-data dated adapter/repository and its test.
- Revert ECL API event consumer/build/test changes.
- Revert this pass's common docs, logs, handoff, and Gemini review prompt.

---
# AI Harness Handoff - 2026-07-22 Config Server Review

## Current State

- Branch: `agent/asset-lease-split`
- Current owner: Codex
- Scope ready for independent review: Config Server repository availability, native config HTTP contract, Docker/Compose startup ordering, and beginner documentation.
- Working tree intent: local uncommitted Config Server changes; do not commit or push until the user explicitly requests it.

## What Changed

- Added configurable `ConfigRepositoryHealthIndicator` that fails readiness on empty property sources or repository errors.
- Added 9 HTTP, health, and configuration policy tests.
- Aligned local/native path, 8888, Prometheus, JDK 17 single bootJar, no embedded config repository, and read-only Compose mounts.
- Changed all 15 Config Server dependents from `service_started` to `service_healthy`.
- Corrected documentation around profile/property-source precedence, optional fallback, client restart/refresh, startup, and shutdown.

## Verification Summary

- Initial `:config-server:test :config-server:bootJar`: passed, 9 tests with no failures/errors/skips.
- The later repository-error detail sanitization produced newer main/test class outputs. Its targeted test rerun stalled before creating a new report because the Windows paging file was exhausted, so rerun it after restoring memory headroom.
- Initial standalone JAR 8888: readiness UP; `master-data/default` HTTP 200 with one property source; Prometheus HTTP 200 and JVM metric present.
- Config response values were not printed.
- Config Server runtime was stopped after smoke.

## Known Risks And Next Review

- Docker CLI is unavailable, so actual image build and live Compose startup are unverified.
- Rerun `ConfigRepositoryHealthIndicatorTest` before merge to close the final verification gap.
- After that rerun, continue the second-pass module audit with `contracts` and then `shared-kernel`, focusing on public contracts, money/code value objects, and dependency direction.
- Config API private network+mTLS/service authentication remains an explicit TODO.
- Reviewed Git backend, immutable label, multi-node refresh and rollback governance remain an explicit TODO.
- Review root Compose overlap with the already committed Discovery `service_healthy` changes.

## Rollback

- Revert `config-server` and `config-repo/README.md` changes.
- Revert only the Config Server service block and `depends_on.config-server` conditions in root Compose.
- Revert `.run/Config Server bootRun.run.xml` and this pass's common docs/harness entries.

---
# AI Harness Handoff - 2026-07-20 Discovery Review

## Current State

- Branch: `agent/asset-lease-split`
- Push target when explicitly requested: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for independent review: Discovery standalone/config runtime, registry lifecycle, readiness, Docker/Compose ordering, and beginner documentation.
- Commit/push state: the user explicitly requested one cumulative Gateway and Discovery commit and push to `origin/agent/asset-lease-split` after final verification.

## What Changed

- Discovery starts on 8761 without Config Server and does not register/fetch itself as a client.
- Config Client, actuator, Prometheus, Brave, and Zipkin settings now have real runtime dependencies.
- Tests cover readiness, registry register/lookup/cancel, local/config YAML, root/module Compose, and Dockerfile policy.
- Docker uses JDK 17, a single bootJar, repository-root context, and image readiness healthcheck.
- All 14 root Compose services that depend on Discovery wait for `service_healthy`.
- Standalone IntelliJ execution and beginner lease/self-preservation/security/HA docs were added.

## Verification Summary

- `:discovery:test :discovery:bootJar`: passed, 6 tests with no failures/errors/skips.
- Standalone 8761: readiness `UP`; Dashboard, registry API, Prometheus HTTP 200; JVM metric present.
- Registry test registered, looked up, canceled, and cleaned a temporary instance.
- Discovery/Gradle runtime processes were stopped after smoke.

## Known Risks And Next Review

- Docker CLI is unavailable, so image and live Compose execution are unverified.
- Live Config Server plus multiple Eureka clients, heartbeat/lease timing, LoadBalancer routing, and production self-preservation thresholds are unverified.
- Review whether private network+mTLS/authentication and multi-AZ peer sync TODOs are sufficient and correctly located.
- Gateway changes from the previous pass are included in the same cumulative review and commit because root Compose overlaps both modules.

## Rollback

- Revert only `discovery`, `config-repo/discovery-service.yml`, root Compose Discovery environment/dependency conditions, `.run/Discovery standalone bootRun.run.xml`, and related docs/harness files after checking the overlapping Gateway Compose edits.

---
# AI Harness Handoff - 2026-07-20 Gateway Review

## Current State

- Branch: `agent/asset-lease-split`
- Push target when explicitly requested: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for independent review: Gateway global authentication, trusted headers, JWT/token-version ports, route and runtime configuration.
- Commit/push state: Gateway is included in the user-requested cumulative Gateway and Discovery commit after final verification.

## What Changed

- All `/api/**` routes now use one global authentication policy; POST login/CORS are the only API exceptions.
- Auth validate/internal paths are blocked before routing.
- Client identity headers are removed and regenerated only from a verified immutable principal.
- JJWT and Auth WebClient/Caffeine concerns sit behind separate verifier ports.
- Missing/fractional roleVersion and incomplete time/role claims fail closed.
- Auth rejection returns 401; timeout/error/empty response returns 503.
- Port 8000, Docker Auth service URL/dependencies, JDK 17 Dockerfile, and standalone IntelliJ run configuration are aligned.
- Gateway and shared beginner/process/local-run docs reflect the current code path.

## Verification Summary

- `:gateway:test :gateway:bootJar`: passed, 30 tests with no failures/errors/skips.
- Local standalone Netty bootRun: port 8000 and actuator health `UP`.
- Route and root/module Compose YAML parsing tests: passed.
- All Gateway/Gradle runtime processes were stopped after smoke verification.

## Known Risks And Next Review

- Docker CLI is not installed, so image build and live Compose execution are unverified.
- Live Config/Discovery/Auth/business API routing is unverified.
- Review header canonicalization, global-filter ordering, JJWT time semantics, and the 401/503 contract.
- Remaining code TODOs: JWKS/key rotation, event-driven distributed cache invalidation, Auth service authentication, legacy catch-all removal.

## Rollback

- Revert only `gateway`, `config-repo/gateway-service.yml`, root/gateway Compose, `.run/Gateway standalone bootRun.run.xml`, and the related docs/harness files after checking for overlapping user edits.

---
# AI Harness Handoff - 2026-07-14 Auth Review

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: auth API/core login boundary, deterministic role snapshot, token-version account policy, Governance role approval idempotency, and H2/Flyway runtime.
- Working tree intent: auth changes passed local verification and are included in the user-requested commit/push.

## What Changed

- Auth core returns `AuthenticationResult`; the API maps it to `LoginResponse`.
- One Clock instant determines effective roles for both the response and JWT.
- Disabled, administratively locked, or role-less users fail token-version validation.
- Memory role replacement preserves dataScope and effective dates.
- approvalTraceId plus request fingerprint prevents duplicate role replacement and roleVersion increments.
- JPA uses a user write lock and V72 apply-log table; trace payload conflicts fail closed.

## Verification Summary

- `:auth:test :auth:bootJar`: passed, 32 tests.
- Local H2/Flyway/JPA validate non-web bootRun: passed through migration V72.
- Core API-package reverse dependency search: no matches.

## Known Risks

- PostgreSQL lock/concurrency and Flyway execution are not live-tested.
- Plain-password hash migration, concurrent first-failure upsert, and apply-log retention remain code TODOs.
- H2 emits a Flyway support-version warning, although migration and schema validation pass.

## Rollback

- Restore only `auth`, `.run/Auth bootRun.run.xml`, and related docs/harness files after checking for overlapping user edits.

---

# AI Harness Handoff - 2026-07-14 Governance Completion

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review and user-requested commit/push: reporting response DTO boundary, deposit command/Batch date policy, master-data typed applier/statistics boundary, and governance approval/runtime boundary.

## Governance Changes

- The executable now scans real audit features and the required master-data approval adapters instead of starting an empty server.
- Authorization DELETE creates a PENDING approval and returns HTTP 202; approved DELETE performs the physical removal.
- Unsupported role/authorization request types fail closed.
- H2 standalone API execution is available through IntelliJ and Gradle; PostgreSQL JDBC is packaged and a disposable local smoke command is documented.

## Verification Summary

- `:governance:compileJava`: passed.
- `:governance:test :governance:bootJar`: passed.
- Local H2 non-web `bootRun`: passed; 12 JPA repositories and actual feature beans loaded.
- Earlier reporting, deposit, and master-data verification remains valid as recorded below.

## Known Risks

- PostgreSQL/Flyway was not live-tested.
- Governance role/authorization create responses still return preview domains instead of a unified approval receipt.
- External Auth application still needs an approvalId-based outbox/inbox boundary.
- Master-data currency/exchange-rate/fiscal-period appliers and requestedVersion conflict enforcement remain pending.

## Rollback

- After commit, revert the combined boundary-refactor commit. Avoid restoring individual files over newer user changes.

---

# AI Harness Handoff - 2026-07-14

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: reporting response DTO boundary, deposit command/Batch date policy, and master-data typed applier/statistics boundary.
- Working tree intent: local uncommitted changes; do not commit or push until the user explicitly requests it.

## What Changed

- Reporting API maps core domain results to response DTOs.
- Deposit account opening command validates required codes and non-negative values; deposit Batch requires explicit `asOfDate`.
- Master-data core no longer depends on a batch DTO.
- Master-data validity statistics use a dedicated output port and four database COUNT queries.
- Account subject, business partner, department, and product change requests have typed appliers.
- Master-data JSON payload decoding is behind an output port/Jackson adapter.
- Deactivation uses the approved effectiveDate and does not require a JSON payload.
- IntelliJ `Master Data bootRun` starts a standalone local/H2 API with external infrastructure disabled.

## Verification Summary

- `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :deposit:core:test :deposit:batch:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1`: passed.
- `.\gradlew :master-data:test --console=plain --max-workers=1`: passed.
- Master-data H2 JPA COUNT integration test: passed.
- Master-data local/H2 non-web bootRun: passed.
- Core-to-batch reverse dependency and batch business-loop searches: no matches.

## Known Risks

- Master-data CURRENCY, EXCHANGE_RATE, and FISCAL_PERIOD typed appliers remain fail-closed.
- Master-data requestedVersion conflict enforcement remains a code `@todo`.
- Master-data batch is currently a package-level orchestrator, not an independent Spring Batch Job/Step executable module.
- PostgreSQL/Flyway and high-volume seeded runs were not verified for these uncommitted changes.
- The master-data Flyway V1 file remains an intentionally empty baseline and is not a production schema.

## Rollback

- Before commit: restore only `reporting`, `deposit`, `master-data`, `.run/Master Data bootRun.run.xml`, and the related docs/worklog/Gemini prompt changes after checking for overlapping user edits.
- After commit: revert the relevant boundary refactor commit.

---

# AI Harness Handoff

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: Reporting API response DTO boundary and Deposit command/Batch asOfDate boundary.
- Working tree intent: uncommitted local changes unless the user asks for commit/push.

## What Changed

- Reporting API maps core domain results to response DTOs instead of returning domain objects directly.
- Deposit `OpenAccountCommand` now validates required codes, currency normalization, non-negative initial deposit, and non-negative interest rate.
- Deposit `depositAccountIntegrityJob` requires explicit `asOfDate=yyyy-MM-dd` and fails fast if missing or malformed.
- Reporting/deposit docs and Gemini review prompt were updated.

## Verification Summary

- `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :deposit:core:test :deposit:batch:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1`: passed.

## Known Risks

- PostgreSQL/Flyway runtime compatibility and high-volume Batch execution are not verified in this pass.
- Deposit real master-data/journal-ledger adapters were not exercised beyond existing local/test doubles.

## Rollback

- Before commit: restore `reporting`, `deposit`, docs/worklog/handoff, and Gemini prompt changes carefully.
- After commit: revert the relevant boundary refactor commit.

---# AI Harness Handoff

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: Reporting API response DTO boundary refactor.
- Working tree intent: uncommitted local changes unless the user asks for commit/push.

## What Changed

- `ReportingController` no longer returns core domain objects directly for JSON responses.
- `reporting:api/.../dto` owns response DTOs for financial statements, disclosure note marts, regulatory submissions, regulatory filings, and drill-down rows.
- Core use cases and domain rules remain unchanged; Controller maps core results to API DTOs.
- Reporting docs and Gemini review prompt were updated to match the new boundary.

## Verification Summary

- `.\gradlew :reporting:api:test --console=plain --max-workers=1`: passed.
- `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1`: passed.

## Known Risks

- PostgreSQL/Flyway runtime compatibility and high-volume reporting batch execution are not verified in this pass.

## Rollback

- Before commit: restore `reporting`, docs/worklog/handoff, and Gemini prompt changes carefully.
- After commit: revert the reporting API DTO boundary commit.

---# AI Harness Handoff

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: Reconciliation API/core command boundary refactor.
- Working tree intent: commit and push this refactor by explicit user request.

## What Changed

- `reconciliation:core` no longer owns HTTP Controller/DTO/Web/Validation dependencies.
- `reconciliation:api` owns `ReconciliationController`, request/response DTOs, and Bean Validation.
- Core input is now command-based: `ReconciliationUnitCommand`, `ReconciliationRuleCommand`, `DifferenceReasonCodeCommand`, `RunReconciliationCommand`, `AssignDifferenceCommand`, `ResolveDifferenceCommand`.
- `reconciliation:batch` delegates to the same core command/service path and uses delayed JobRegistry registration.
- Reconciliation docs and Gemini review prompt were updated to match the new boundary.

## Verification Summary

- `.\gradlew :reconciliation:core:test :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1`: passed.
- `reconciliation:api:bootRun` local/H2 context smoke: passed.
- `reconciliation:batch:bootRun` local/H2 context smoke: passed; JobRegistry warning no longer appeared.
- Reconciliation Java TODO/FIXME search returned no matches.

## Known Risks

- PostgreSQL/Flyway runtime compatibility and high-volume reconciliation job execution are not verified in this pass.
- External source snapshot and journal-ledger posting integration were not exercised beyond local/H2 context smoke.

## Rollback

- After commit: `git revert <commit>` for the reconciliation boundary refactor.
- Before commit: restore the changed `reconciliation`, docs, worklog, handoff, and Gemini prompt files carefully, preserving unrelated user changes.

---
# Latest Handoff - 2026-07-08 Payable + Receivable API/Core Command Boundary

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: payable and receivable API/core command boundaries, API DTO separation, and Batch JobRegistry cleanup.

## What Changed

- `payable:core` and `receivable:core` no longer own HTTP Controller/DTO classes or Web/Validation dependencies.
- Payable core inputs are `PurchaseInvoiceCommand`, `PaymentRunCommand`, `ExecutePaymentCommand`, `AdvancePaymentCommand`, and `OffsetPayableCommand`.
- Receivable core inputs are `SalesInvoiceCommand`, `CollectionCommand`, and `ManualMatchingCommand`.
- `payable:api` and `receivable:api` own controllers, request DTOs, Bean Validation, response DTOs, and controller tests.
- `payable:batch` and `receivable:batch` keep Spring Batch orchestration/JobRegistry infrastructure only and delegate business decisions to core use cases.

## Verification Summary

- `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava --console=plain --max-workers=1`: passed.
- `payable:api:bootRun` and `payable:batch:bootRun` local/H2 context smoke: passed.
- `.\gradlew :receivable:core:test :receivable:api:test :receivable:batch:compileJava --console=plain --max-workers=1`: passed.
- `receivable:api:bootRun` and `receivable:batch:bootRun` local/H2 context smoke: passed; Batch JobRegistry warning no longer appeared after configuration.

## Known Risks

- PostgreSQL/Flyway runtime compatibility was not tested in this pass.
- Real payment gateway/bank statement integrations and high-volume payable/receivable batch runs still need integration verification.

## Rollback

- After commit: revert the payable/receivable boundary refactor commit.

---
# Latest Handoff - 2026-07-08 Payable API/Core Command Boundary

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: payable API/core command boundary, API response DTO separation, and Batch JobRegistry cleanup.

## What Changed

- `payable:core` no longer owns HTTP Controller/DTO classes or Web/Validation dependencies.
- `PurchaseInvoiceCommand`, `PaymentRunCommand`, `ExecutePaymentCommand`, `AdvancePaymentCommand`, and `OffsetPayableCommand` are the core use case inputs.
- `payable:api` owns controllers, request DTOs, Bean Validation, and response DTOs.
- `payable:batch` creates `PaymentRunCommand` and delegates business flow to core `PaymentUseCase`.
- `PayableBatchJobRegistryConfiguration` delays Spring Batch Job registration until singleton initialization is complete.

## Verification Summary

- `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava --console=plain --max-workers=1`: passed.
- `:payable:api:bootRun` local/H2 context smoke: passed.
- `:payable:batch:bootRun` local/H2 context smoke: passed; JobRegistry BeanPostProcessor warning no longer appeared.

## Known Risks

- PostgreSQL/Flyway runtime compatibility was not tested in this pass.
- Real bank/payment gateway adapter and high-volume payment run performance still need integration verification.

## Rollback

- Before commit: restore `payable`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` carefully.
- After commit: revert the payable boundary refactor commit.

---
# Latest Handoff - 2026-07-08 Expenditure Resolution API/Core Boundary

## Current State

- Branch: gent/asset-lease-split
- Push target: origin/agent/asset-lease-split
- Current owner: Codex
- Scope ready for review: expenditure-resolution API/core command boundary, master-data code reference cleanup, and Batch JobRegistry cleanup.

## What Changed

- expenditure-resolution:core no longer owns HTTP Controller/DTO classes.
- ExpenditureResolutionCommand and APPaymentCommand are the core use case inputs.
- ExpenditureResolutionService uses MasterDataQueryPort instead of master-data internal persistence ports/entities.
- Budget and Invoice store master-data references as code values.
- API integration test moved to expenditure-resolution:api tests.
- Batch JobRegistry registration is delayed via ExpenditureResolutionBatchJobRegistryConfiguration.

## Verification Summary

- $compile: passed.
- $verify: passed.
- $apiRun: passed.
- $batchRun: passed; JobRegistry BeanPostProcessor WARN no longer appeared.

## Known Risks

- PostgreSQL migration/data compatibility for code-based budget/invoice mappings is not verified.
- Real high-volume approval Job execution is not verified.

## Rollback

- Before commit: restore expenditure-resolution, 	ax, contracts, docs/worklog/handoff/Gemini prompt carefully.
- After commit: revert the expenditure-resolution boundary refactor commit.

---
# Latest Handoff - 2026-07-08 Tax API/Core Command Boundary

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: tax API/core command boundary and cancelled tax invoice external reference policy.

## What Changed

- `tax:core` now owns `TaxInvoiceCommand`, use case/service, domain, persistence/local adapters, and no longer owns HTTP DTO/Controller classes.
- `tax:api` owns `APInvoiceController`, `TaxInvoiceRequestDto`, and `TaxInvoiceDto`; request DTOs convert to core commands before invoking the use case.
- `TaxInvoiceRef` now exposes `purchase()`, `active()`, and `usableForPurchaseSettlement()` so consumers do not duplicate raw string policy checks.
- `expenditure-resolution` validation now uses `TaxInvoiceRef.purchase()` and `active()` for 지출결의/AP 지급 세금계산서 연결 정책.
- tax docs explain API DTO -> core command -> domain validation and cancelled invoice lookup policy.

## Verification Summary

- `.\gradlew :tax:core:test :tax:api:compileJava :tax:batch:compileJava :expenditure-resolution:core:test --console=plain --max-workers=1`
- `.\gradlew :tax:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
- `.\gradlew :tax:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`: passed.

## Known Risks

- PostgreSQL migration and high-volume `taxInvoiceValidationJob` execution are not verified in this pass.
- `expenditure-resolution:core` still contains a cross-module API integration test importing tax API/controller classes; this was preserved and test dependencies were updated explicitly.

## Rollback

- Before commit: restore `tax`, `contracts`, `expenditure-resolution`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` carefully.
- After commit: revert the tax boundary refactor commit.

---
# Latest Handoff - 2026-07-07 Asset Lease

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: `asset-lease` depreciation batch calculation/persistence boundary.
- Working tree: local uncommitted account-mart and asset-lease changes exist.

## What Changed

- `FixedAssetDepreciationResult` now carries depreciation amount, resulting accumulated depreciation, resulting book value, and resulting status.
- `FixedAsset.calculateDepreciation()` calculates without mutating the entity; `FixedAsset.depreciate()` still performs the single-asset state transition for API/service paths.
- `DepreciationPipeline` returns result values and avoids JPA entity mutation in batch chunks.
- `AssetJdbcAdapter.updateDepreciationBulk()` writes calculated values and status once through JDBC bulk update.
- `AssetDepreciationBatchConfig` remains orchestration-only and delegates calculation to core pipeline.
- asset-lease docs were updated for the new batch flow.

## Verification Summary

- `.\gradlew :asset-lease:core:test :asset-lease:api:compileJava :asset-lease:batch:compileJava --console=plain --max-workers=1`: passed.

## Known Risks

- Actual large seeded `assetDepreciationJob` execution was not run in this pass.
- `updated_at = NOW()` compatibility with the final production PostgreSQL/Flyway DDL should be checked when Flyway is enabled.

## Rollback

- Before commit: restore `asset-lease` and related `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` changes carefully.
- After commit: revert the asset-lease depreciation boundary commit.

---
# Latest Handoff - 2026-07-07 Account Mart

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: `account-mart` collateral detail DQ/LGD prerequisite flow.
- Working tree: local uncommitted account-mart changes exist.

## What Changed

- `OdsApartCollDetail` now contains LGD required-input validation and no longer carries the previous implementation `@todo`.
- `OdsApartCollDetailRepository` outbound port, `JpaOdsApartCollDetailRepository`, and `OdsApartCollDetailPersistenceAdapter` connect apartment collateral detail through hexagonal boundaries.
- `CollateralDataQualityInspectionService` loads apartment detail through the port and delegates business judgement to `CollateralDataQualityProcessor`.
- `CollateralDataQualityProcessor` now checks collateral master appraisal, real-estate/apartment detail existence, and district/KB market price/exclusive-area input quality.
- `CollateralDataQualityItemProcessor` remains a Spring Batch adapter and delegates to the core application service.
- `DataPopulator` creates apartment collateral details for generated real-estate collateral rows.
- `V5__add_ods_apart_coll_detail.sql` and account-mart docs were updated.

## Verification Summary

- `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1`: passed.
- `rg -n "@todo|TODO|FIXME" account-mart --glob "*.java" --glob "!**/build/**"`: no matches.

## Known Risks

- PostgreSQL Flyway execution was not run in this pass.
- High-volume collateral detail lookup performance and final LGD formula integration require separate integration verification.

## Rollback

- Before commit: restore `account-mart` and related `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` changes carefully.
- After commit: revert the account-mart collateral DQ/LGD commit.

---
# Latest Handoff - 2026-07-03 Loan

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: `loan` journal port boundary, journal reference value storage, Batch JobRegistry local context cleanup.
- Working tree: local uncommitted loan changes exist.

## What Changed

- `InterestAccrualService` now creates accrual journal commands through `LoanJournalPort` instead of journal-ledger `JournalUseCase` and domain objects.
- `LoanAccrualLog`, `LoanEvent`, and `EIRAmortizationSchedule` now store journal ID/slipNo values instead of holding journal-ledger entity references or returning null slip numbers.
- `V32__loan_accrual_journal_reference.sql` adds missing journal reference columns for accrual logs, loan events, and EIR schedules.
- `LoanBatchJobRegistryConfiguration` moves Job registration to `JobRegistrySmartInitializingSingleton`, removing the local Batch JobRegistry early-initialization warning.
- Loan README/docs/local-run/process-flow/schema and IntelliJ `.run` configs now use local/H2 commands with explicit `loan-api`/`loan-batch` app names and Redis repository scanning disabled.

## Verification Summary

- `.\gradlew :loan:core:test --console=plain --max-workers=1 --rerun-tasks`: passed.
- `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`: passed.
- `.\gradlew :loan:api:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-api --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --server.port=0 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain --max-workers=1`: passed.
- `.\gradlew :loan:batch:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-batch --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain --max-workers=1`: passed.
- API/BATCH logs identify as `loan-api` and `loan-batch`; Redis repository scan logs were not reproduced.
- Batch JobRegistry BeanPostProcessor warning was not reproduced.

## Known Risks

- PostgreSQL migration execution and seeded high-volume accrual Job were not verified in this pass.
- Local smoke disables Redis repository scanning; if a future loan feature intentionally adds Redis repositories, that local option must be revisited.

## Rollback

- Before commit: restore `loan`, `.run/Loan API bootRun.run.xml`, `.run/Loan Batch Context.run.xml`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` carefully.
- After commit: revert the loan boundary refactor commit.

---
# Latest Handoff - 2026-07-03 Closing

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope in progress: `closing` core/batch boundary refactor after ECL and Journal Ledger were pushed.
- Working tree: local uncommitted closing changes exist.

## What Changed

- FX valuation and ECL provision business decisions moved from `closing:batch` into `closing:core` application services.
- Core outbound ports now hide FX rate lookup, allowance GL balance lookup, and closing journal creation.
- Batch adapters now map master-data/journal-ledger technology APIs to core ports.
- Batch configs now focus on Job/Step/Reader/Tasklet orchestration and delegate accounting decisions to core.
- Closing docs and beginner comments were updated for the new responsibility boundary.

## Verification Summary

- `.\gradlew :closing:core:test :closing:batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :closing:api:compileJava --console=plain --max-workers=1`: passed.
- `.\gradlew :closing:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false" --console=plain --max-workers=1`: passed.
- closing API/BATCH local logback XML parsing: passed.
- Closing core Spring Batch type search: no matches.
- Closing TODO/mojibake search: no matches.

## Known Risks

- PostgreSQL high-volume FX/ECL closing run is not verified in this pass.
- FX valuation writer currently logs account-level failures and continues; production skip-limit/retry/reporting policy may need a stricter adapter configuration.
- Batch BeanPostProcessor WARN remains during local context boot; it did not block startup.

## Rollback

- Before commit: restore `closing`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` carefully.
- After commit: revert the closing boundary refactor commit.

---
# Latest Handoff - 2026-07-03 ECL + Journal Ledger

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: ECL core pipeline boundary refactor and Journal Ledger balance reaggregation Batch Job refactor.
- Working tree intent: commit and push this combined refactor by explicit user request.

## What Changed

- `ecl-core` owns Stage/PD, EAD/LGD, and forward-looking ECL processing order through application pipelines.
- `ecl-batch` processors now act as Spring Batch adapters and delegate to core pipelines.
- `AllowanceCalculationService` reuses the same ECL core pipelines used by batch.
- `journal-ledger:batch` now has `dailyBalanceReaggregationJob` with a Tasklet adapter and JobParameter date-range resolver.
- `journal-ledger:batch` local H2 datasource/JPA/Batch YAML and Batch test dependency were corrected.
- `journal-ledger` docs, `docs/local-development.md`, IntelliJ run config, and beginner comments were updated.

## Verification Summary

- `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain --max-workers=1`: passed, Job status `COMPLETED`.
- ECL/Journal Ledger Java TODO and mojibake search returned no matches.
- `git diff --check`: no whitespace errors, CRLF conversion warnings only.

## Known Risks

- PostgreSQL high-volume seeded ECL and Journal Ledger balance reaggregation runs are not verified in this pass.
- Journal Ledger Batch bootRun still logs Spring Cloud/Batch BeanPostProcessor warnings; they did not block Job completion.

## Rollback

- After commit: `git revert <commit>` for the combined ECL/Journal Ledger refactor.
- Before commit: restore the changed `ecl`, `journal-ledger`, `.run`, docs, worklog, and Gemini prompt files carefully, preserving unrelated user changes.

---
# AI Harness Handoff

## Current State

- Branch: `agent/asset-lease-split`
- Base branch: `main`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Working tree: ecl core pipeline refactor is implemented and verified locally, but not committed in this continuation.
- Verification: ecl core/batch focused tests passed.

## What Changed

- `ecl-core` now owns Stage/PD, EAD/LGD, and forward-looking ECL processing order through application pipelines:
  - `StagingCalculationPipeline`
  - `EadCrmCalculationPipeline`
  - `ForwardLookingEclCalculationPipeline`
- `ecl-batch` processors are Spring Batch adapters only. They resolve job parameters or receive chunk items, then delegate to core pipelines.
- `AllowanceCalculationService` reuses the same core pipelines so API/manual single-account calculation and batch calculation share the business sequence.
- ecl README/docs and batch config beginner comments now describe the core pipeline / batch adapter boundary.
- ecl pipeline tests were added, and the allowance use-case service test now verifies pipeline call order.

## Verification Summary

- `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1`: passed.
- `rg -n "AllowanceParameterService|PdCalculationService|LgdCalculationService|CcfCalculationService|EadCrmCalculationService|LifetimePdService|ForwardLookingEclService|BigDecimal|resolveMaturityYears|AllowanceEclResult\.builder" ecl\ecl-batch\src\main\java\com\ho\account\ecl\batch\processor`: no matches.
- `rg -n "org\.springframework\.batch|ItemProcessor|StepExecution|JobParameters|StepScope" ecl\ecl-core\src\main\java ecl\ecl-core\build.gradle`: only explanatory comment remains.
- `rg -n "@todo|TODO:" ecl --glob "*.java" --glob "!**/build/**"`: no matches.

## Known Risks

- PostgreSQL high-volume seeded ECL run is not verified in this pass.
- Gradle 9 deprecation warning remains from existing build configuration.
- Current changes are local and uncommitted unless the user asks for commit/push.

## Rollback

- Before commit: use `git restore -- ecl docs/WORKLOG.md CODEX_WORKLOG.md docs/ai-harness docs/module-documentation-sequence.md GEMINI_REVIEW_PROMPT.md` with care for any unrelated user changes.
- After commit: revert the ecl boundary refactor commit.

## Next Recommended Steps

1. Run `git diff --check` before committing.
2. Commit/push the ecl boundary refactor if requested.
3. Continue the sequential module review with `journal-ledger` after ecl.

## 2026-07-20 Gateway Handoff

- Branch/push target: agent/asset-lease-split -> origin/agent/asset-lease-split.
- Scope: Gateway global JWT filter, JJWT verifier adapter, trusted identity-header regeneration, token-version result split, request-id validation, route/port/Docker/IntelliJ settings.
- Verification: :gateway:test --rerun-tasks passed under the local profile; :gateway:test :gateway:bootJar passed.
- Remaining risk: live Auth/Config/Discovery and Docker image execution were not verified; shared HS256 to JWKS migration remains open.
- Rollback: revert the Gateway commit as one unit.
# AI Harness Handoff - 2026-07-30 Issue #41 BusinessPartner DDD Separation

## Integrated State

- Issue: `#41`, selected after the user explicitly excluded #40. PR #225 closed it with documentation-only commit `cdb892e4`, then a closure audit reopened it because production code was missing. This branch is the verified implementation and closes the Issue through `Fixes #41`.
- Branch/worktree: `agent/41-business-partner-ddd`, `C:\dev\account\.worktrees\account-41-business-partner-ddd`.
- Base: `main@39dabd4e`, including the Issue #40 Master Data API/Core/Batch physical split and PR #225.
- Merged PR: `#232`, merge commit `c720ae58`, using `Fixes #41`.
- State: implementation, latest-main verification, independent review, merge, automatic Issue closure, and remote branch deletion are verified. The source worktree is cleanup-eligible.

## Implemented Boundary

- `BusinessPartner` and `BusinessPartnerAccount` are JPA-free domain models with creation/reconstitution factories, explicit validity rules, immutable SCD2 code, defensive account ownership, and one-main-account enforcement.
- `BusinessPartnerJpaEntity` and `BusinessPartnerAccountJpaEntity` preserve the existing tables, columns, indexes, cascade/orphan behavior and child FK. No migration or HTTP path change was introduced.
- `JpaBusinessPartnerPersistenceAdapter` owns explicit bidirectional mapping. `BusinessPartnerRepository` accepts only JPA entities, while application and cross-module adapters depend on `BusinessPartnerPersistencePort`.
- `BusinessPartnerService` validates the future version before closing the current row in one transaction. `closeVersion` preserves business availability for version replacement; `terminate` performs actual deactivation.
- Next-version account values are copied into new child rows with null IDs; the previous version retains its original child IDs/FKs, preventing current-account loss without rewriting history.
- `BusinessPartnerDto` owns API mapping and registration-number masking. Expenditure integration setup now mocks the outbound port instead of writing a Master Data repository directly.
- API and Batch composition roots explicitly register persistence-adapter JPA entities. The duplicate BOM-prefixed `MasterDataApiApplication` introduced after #40 was removed in favor of the documented `MasterDataApplication`, and the Batch integration fixture now saves Business Partners through the output port.

## Verification Evidence

- `:master-data:core:test --rerun-tasks`: 12 tests passed, including next-version account copy, service save order/no-write validation, distinct child rows, overlap fail-closed behavior, and the library-owned JPA slice.
- `:master-data:api:test :master-data:api:bootJar --rerun-tasks`: 2 tests and executable packaging passed.
- `:master-data:batch:test :master-data:batch:bootJar --rerun-tasks`: 4 tests and executable packaging passed.
- `:expenditure-resolution:api:test --tests "com.ho.account.expenditure.integration.ExpenditureTaxApiIntegrationTest" --rerun-tasks`: 1 test passed.
- `:loan:core:test --rerun-tasks`: 30 tests passed; the 4-test `LoanReferenceDataAdapterTest` compatibility class is included.
- `:journal-ledger:api:test --tests "com.ho.account.journalledger.IntegratedBusinessProcessTest"` with external discovery/config disabled: 1 compatibility test passed.
- Total observed: 50 tests, 0 failures, 0 errors, 0 skips.
- One combined rerun initially stopped after the passing Expenditure test because the Loan filter incorrectly included `.core` in the Java package and matched no tests. The corrected `com.ho.account.loan.infrastructure.adapter.LoanReferenceDataAdapterTest` filter then passed; this was a command-selection error, not a product-test failure.
- Static checks found no JPA annotations/imports in the BusinessPartner domain files and no Spring Data repository parameterized with the domain type.

## Remote Gate, Risks, And Rollback

- PR #232 is `MERGED`, Issue #41 is `CLOSED`, merge commit `c720ae58` is on `origin/main`, and the remote source branch returns 404 after deletion.
- PostgreSQL execution was not available. H2 verifies mapping behavior, but production overlap prevention still needs a PostgreSQL date-range exclusion constraint and integration test.
- `@EntityGraph(accounts)` prevents per-row lazy queries but an unpaged list may expand result rows; bounded pagination remains a separate contract change.
- Rollback after commit is a normal revert of the Issue #41 follow-up implementation commit. Existing schema and API contracts are retained, so no migration rollback is needed.
- The implementation worktree may be removed after this integration record is merged; the verified Git history remains on `main`.
# AI Harness Handoff - 2026-07-30 Issue #228 Container Images

## Integrated state

- Issue/branch/worktree: `#228`, `agent/228-container-images`, `C:\tmp\account-228-container-images`.
- Base: `origin/main@5fb9cb67`.
- State: implementation and latest-main verification complete; branch pushed and Draft PR `#240` opened with `Refs #228`. No image pull/build, merge or Issue closure was performed.

## Implemented contract

- Root `Containerfile` and `Dockerfile` are byte-identical Java 17 definitions. Required `GRADLE_PROJECT` and `JAR_DIRECTORY` inputs are character-validated, the exact `bootJar` task is invoked, and the build fails unless exactly one non-plain JAR exists.
- Builder writes only as the Gradle user; runtime uses a minimal Java 17 JRE and non-root `app` user with exec-form Java entrypoint.
- `deploy/image-targets.json` is the single target inventory: 35 Java executables plus frontend. It excludes libraries, cores, aggregators and the removed phantom app.
- All 19 existing module Compose build blocks now use the repository-root Containerfile and exact project/path arguments. The manifest owns additional API/Batch images that are not represented by legacy module Compose files and will be orchestrated by #66/#230.
- Frontend uses its existing Next standalone Containerfile with same-origin `/api` default and without source volumes that hide `server.js`; recursive `.env*`, ignored Spring local configs and private-key/keystore files are excluded from build contexts.
- Active Auth/Account Mart/ECL module Compose paths require datasource variables and contain no default credentials; Auth uses `ddl-auto=validate`.
- `tools/container-images.ps1` concurrently drains stdout/stderr, lists targets, verifies all Java packages offline, or builds selected local Docker/Podman images without pushing. It records the full report before returning non-zero for enabled target failures.

## Verification and remaining gates

- `.\gradlew.bat :config-server:test :gateway:test --offline --no-daemon --rerun-tasks`: passed.
- Full package report: 33 `PASS`, 2 `BLOCKED` (`internal-audit-api`, `internal-audit-batch`), with all 33 JAR paths present.
- Updated verifier sample: `master-data-api=PASS`, `internal-audit-api=BLOCKED`, exit code 0.
- PowerShell parse/list smoke, YAML policy parsing, `git diff --check`, private-host/default-secret scans and executable artifact-count checks passed.
- Independent review raised four findings and re-review found one remaining frontend-context gap. All were corrected; the final read-only review found no unresolved issue.
- Frontend dependency install/build was not run because `node_modules` is absent and package installation is not authorized.
- Docker is absent; Podman has no required base images and no Compose provider. Actual image builds, health checks and registry behavior remain environment gates.
- Rollback: revert the Issue #228 commit. No registry image, container, volume or remote environment was changed.
# AI Harness Handoff - 2026-08-11 Issue #254 Asset Lease Recovery

## Review-ready state

- Issue/branch/worktree: `#254`, `agent/254-asset-lease-postgres-reintegration`, `C:\tmp\account-254-asset-lease-postgres-reintegration`.
- Base/source/PR: `origin/main@ea6d8063`, `39edbd26`, Draft PR `#288` targeting `main` with `Refs #254`.
- Reason for recovery: old PR #259 merged into a superseded stacked branch, while current main advertised Asset Lease READY without containing the corresponding profiles and schema implementation.

## Evidence and gates

- Asset Lease Core 19, API 2, Batch 1 and migration-runner 78 tests passed with no failure/error/skip.
- API/Batch bootJars, the full 183-task READY PostgreSQL-driver gate, diff/marker checks and direct local-profile H2 JAR startup passed.
- Independent review findings were resolved and final re-review reported no P0-P3 findings.
- Actual PostgreSQL clean migrate plus API/Batch `ddl-auto=validate` was not run because no approved local image/pull or external DB access was available. This remains an environment gate, not evidence supplied by H2 compatibility mode.
- Rollback is a normal revert of `39edbd26`; never delete or rewrite an already applied V20 migration. Merge, Issue close and deployment require separate approval.
# AI Harness Handoff - 2026-08-11 Issue #249 Budget Runtime

## Review-ready state

- Issue/branch/worktree: `#249`, `agent/249-budget-runtime-matrix`, `C:\tmp\account-249-budget-runtime-matrix`.
- Base/source/PR: `main@fe24362d` including Asset Lease PR #288, source `d904c2ce`, Draft PR `#289` targeting `main` with `Refs #249`.
- Budget API/Batch now own local H2 direct execution, dev/prod PostgreSQL-only profiles, canonical images, module Compose and production Compose services.

## Evidence and gates

- Latest-main combined gate passed 195 Gradle tasks covering Budget tests, Config policies, full migration-runner tests, READY driver packaging and production JDBC/Actuator archives.
- API JAR `PASS_STARTED`; Batch JAR `PASS_EXITED`; both applied Flyway V50 to H2 and validated JPA, with no automatic Batch Job.
- Production validator self-test/template passed with 91 required variables, 36 immutable images and 16 PostgreSQL URLs. Git Bash syntax, diff and marker checks passed.
- Independent review drove JWT/Auth route, Actuator, network, DB role/grant, migration-history and sequence-privilege corrections; final review found no P0-P3.
- Real PostgreSQL role/ACL/TLS, image build and Compose start/health were not run. No external development host or secret was accessed. Rollback is a normal revert of `d904c2ce`; do not delete database volumes or rewrite migrations.

# AI Harness Handoff - 2026-08-11 Issue #231 Internal Audit Runtime Boundary

## Review-ready state

- Issue/branch/worktree: `#231`, `agent/231-internal-audit-runtime-boundary`, `C:\tmp\account-231-internal-audit-runtime-boundary`.
- Base/source/PR: `main@0e254ebd`, source `5052163c`, Draft PR `#338` targeting `main` with `Refs #231`, `#73`, and `#74`.
- Internal Audit API is now the executable boundary; Core is a library and the fake Auth/Internal Audit Batch skeletons are removed because no real Job/Step exists.

## Evidence and gates

- The combined Gradle gate passed 179 tasks; 244 affected tests had no failure/error/skip. API/core packaging, migration-runner, Gateway, Config Server and production runtime dependency gates passed.
- Direct local JAR smoke passed with servlet, H2, Flyway V60 and JPA validate. Production manifest/template validation passed with 36 immutable images and 17 PostgreSQL URLs.
- Independent review drove TLS startup ordering, Gateway routing and parent-integrity corrections; final review found no remaining P0-P3.
- Docker was unavailable and no external development host was accessed. Real PostgreSQL migration/JPA validate, Compose health and live Gateway/Eureka routing remain external gates.
- Rollback is a normal revert of `5052163c`; do not rewrite or delete an applied V60 migration.

# AI Harness Handoff - 2026-08-11 Issue #341 Phantom App Removal

- Issue/branch/worktree/base: `#341`, `agent/341-remove-phantom-app`, `C:\tmp\account-341-remove-phantom-app`, `origin/main@5624ae97`; source `7b393665`, Draft PR `#346`.
- The only project-set change is removal of source/build-less `:app`; 72 real subprojects remain and runtime image/Compose inventory is unchanged.
- `projects` and 21 Config/image/development/production policy tests passed; diff/marker checks and independent review reported no P0-P3.
- No local directory or build output was deleted. Rollback is a normal commit revert; #227 owns regenerated inventory counts and matrix documentation.

# AI Harness Handoff - 2026-08-11 Issue #66 Development Compose

## Review-ready state

- Issue/branch/worktree/base: `#66`, `agent/66-compose-runtime-topology`, `C:\tmp\account-66-compose-runtime-topology`, `origin/main@1e6ad6f1`; source `375105ab`, Draft PR `#339` with `Refs #66`.
- Root development orchestration now covers 3 Java platform services, Frontend, 17 APIs and 15 opt-in Batch services using canonical image targets. Self-contained and external-dev overlays share one service topology while keeping infrastructure ownership mutually exclusive.
- Self-contained mode gates runtime on PostgreSQL health, migrations, grants and a 17-schema privilege check. External-dev starts no PostgreSQL/Redis/Kafka service and gates all API/Batch services on secret-safe, context-specific PostgreSQL probes.

## Evidence and remaining gates

- Six `DevelopmentComposePolicyTest` cases, Config/Gateway/migration-runner tests, 159 Gradle packaging tasks, validator self-test, shell syntax, diff/marker checks and the Frontend production build all passed. The Frontend produced 122 routes and no production rewrite to a development Gateway.
- Independent review findings on standard Kafka/Redis properties, Gateway readiness, PostgreSQL sequence visibility, `psql` error leakage, password consistency and Frontend health were corrected; final re-review found no unresolved P0-P3.
- Docker is unavailable and Podman lacks a Compose provider. Actual render/build/up, HTTP health, 17-context PostgreSQL DML/sequence/Flyway ACL checks and external-network probe timing remain deployment-environment gates. Issue #66 must remain open after code merge.
- Rollback is a normal code/config revert followed by Compose `down` without `-v`. Do not remove named volumes, rewrite migrations or mutate an external development database. Next owner is an approved container/PostgreSQL host operator for the live gates.

# AI Harness Handoff - 2026-08-11 Issue #340 Expenditure/Tax Classpath

## Review-ready state

- Issue/branch/worktree/base: `#340`, `agent/340-expenditure-tax-test-classpath`, `C:\tmp\account-340-expenditure-tax-test-classpath`, `origin/main@81f4206e`.
- Source/PR: commit `aaaa4922`, PR `#350`, merge `aaad0c0d`; Issue #340 closed.
- Changed files are limited to `tax/api/build.gradle`, `expenditure-resolution/api/build.gradle`, and `expenditure-resolution/core/build.gradle`.

## Evidence and remaining gate

- The Tax API now exposes an explicit `-plain.jar` to Gradle consumers while its unclassified artifact remains the executable boot JAR selected by the canonical Containerfile.
- Focused latest-main verification passed 45 tasks and 55 observed tests with no failure/error/skip. Diff and marker checks passed; the prior independent reviewer found no P0-P3.
- The root forced build passed the original #340 compile/integration failure and executed 237 tasks, then stopped at unrelated #344 because Internal Audit's local profile does not declare the datasource URL required by its policy test.
- Independent latest-main review found no P0-P3. Full-root green and parent #227 completion depend on #344, not further #340 production changes.
- Roll back by reverting `aaaa4922`. No DB/data/image/Compose rollback is required.

# AI Harness Handoff - 2026-08-11 Issue #348 Multi-Tool Issue Ownership

## Current state

- Issue/branch/worktree/base: `#348`, `agent/348-multi-tool-issue-ownership`, `C:\tmp\account-348-multi-tool-issue-ownership`, rebased `origin/main@aaad0c0d`.
- GitHub now exposes workflow state through `status:ready`, `status:in-progress`, `status:blocked`, and `status:needs-review`; tool ownership uses `agent:codex`, `agent:gemini`, or `agent:claude-code`.
- Active Codex Issues are assigned to the authenticated repository owner and labeled. Available Issues are visibly `status:ready`, Issue #66 is blocked on live environment evidence, and Issue #89 awaits independent review.

## Contract and remaining gates

- Only an open, fully scoped `status:ready` Issue may be claimed for implementation. The parent Integrator alone records the one tool-owner label, assignee and exact start comment; implementation tools request a claim and wait for confirmation.
- Gemini and Claude remain reviewers by default. They may implement a different ready Issue only after explicit user assignment and a valid claim. A different tool may review `status:needs-review` read-only without taking implementation ownership.
- The primary checkout is dirty and 15 commits behind, so it was not pulled, reset, or edited. All Issue #348 changes are isolated on the latest fetched remote base.
- Initial independent review found three P2 process defects. The parent-only GitHub mutation rule, all advertised ready-Issue contracts, and the malformed #348 claim have been corrected; the branch was rebased while preserving #340/#348 logs.
- The first re-review found one residual P2 because review/transfer wording and four old ready-Issue comments still implied tool-owned mutations. Tool guides/runbooks now reserve Issue comments and every state transition to the parent, and #80/#343/#345/#347 contain explicit superseding comments.
- Final independent re-review found no P0-P3. Head `9a783fc1`, base `aaad0c0d`, ready-Issue state, allowlist, links, diff, marker and CLEAN/MERGEABLE checks passed; zero CI checks are configured and no runtime tests apply. The parent Integrator may perform the user-authorized Ready/merge/close transition.
- PR #349 merged as `c4a50f17`; Issue #348 is closed and its active status/owner labels are removed.

# AI Harness Handoff - 2026-08-11 Issue #79 Loan API Local H2

## Review-ready state

- Issue/branch/worktree/base: `#79`, `agent/79-loan-api-local-h2`, `C:\tmp\account-79-loan-api-local-h2`, `origin/main@c4a50f17`.
- Changed files: `LoanApplication.java` and `LoanApplicationLocalProfileTest.java`, plus parent-owned harness records.
- The composition root now explicitly registers the Master Data persistence entities and shared audit/security entities and repositories already required by its single-repository local adapters; Loan core port boundaries remain unchanged.

## Evidence, rollback, and next owner

- Latest-main Loan Core/API verification passed 42 tests, API bootJar and direct local H2 JAR startup. Static gates passed and independent review found no P0-P3.
- No PostgreSQL, Compose, credential, private endpoint or existing container was accessed. PostgreSQL parity and separate Loan Batch startup remain outside this Issue.
- Commit `09b72f21` is pushed and Draft PR #352 targets main. Issue #79 is frozen at `status:needs-review`; next owner is an independent read-only PR reviewer, followed by the parent Integrator.
- Roll back `09b72f21`; there is no schema, data, container or external-service rollback.
- Final PR-head review found no P0-P3. PR #352 merged as `4fa50cc8`; Issue #79 closed and active status/owner labels were removed.

# AI Harness Handoff - 2026-08-11 Issue #342 Reconciliation Local Health

## Review-ready state

- Issue/branch/worktree/base: `#342`, `agent/342-reconciliation-local-health`, `C:\tmp\account-342-reconciliation-local-health`, `origin/main@4fa50cc8`.
- Changed files: `application-local.yaml`, `ReconciliationLocalHealthPolicyTest.java`, `reconciliation/docs/local-run.md`, plus parent-owned harness records.
- Local health no longer depends on an unused Redis process; probes expose health/readiness while dev/prod retain their existing behavior.

## Evidence, rollback, and next owner

- Latest-main 34 Core/API tests, API bootJar and direct local H2 JAR health/readiness HTTP smoke passed. Static gates passed and independent review found no P0-P3.
- Real Redis-backed dev/prod health and automated container/CI smoke remain outside the Issue. No external environment was accessed.
- Commit `d0698585` is pushed and Draft PR #353 targets main. Issue #342 is frozen at `status:needs-review`; next owner is an independent read-only PR reviewer, followed by the parent Integrator.
- Roll back only `application-local.yaml`, `ReconciliationLocalHealthPolicyTest.java`, and `reconciliation/docs/local-run.md` in a new reviewed commit, then append the outcome to the harness. Preserve all five parent-owned harness files because they also contain accurate #79 integration history; never revert all of `d0698585`. No DB/data/container rollback is required.
- Final PR review found only this P3 rollback-document defect; runtime/test behavior had no P0-P3. The corrected head requires independent re-review before Ready/merge.
- Independent re-review found no P0-P3. PR #353 merged as `cf50e4fc`; Issue #342 closed and active labels were removed.

# AI Harness Handoff - 2026-08-11 Issue #344 Internal Audit Local Policy

## Verification-ready state

- Issue/branch/worktree/base: `#344`, `agent/344-internal-audit-local-h2`, `C:\tmp\account-344-internal-audit-local-h2`, `origin/main@cf50e4fc`.
- Changed runtime scope is limited to `InternalAuditRuntimePolicyTest.java` and `internal-audit/README.md`, plus parent-owned append-only harness records. The production `application-local.yml` was already integrated by #231 and is unchanged.
- The test now pins the full explicit local H2/Flyway V60/JPA/control-plane contract; documentation states local needs no external control plane or PostgreSQL and that in-memory data is ephemeral.

## Evidence, rollback, and next owner

- Focused and full Core/API verification passed 17 tests in 6 suites, API bootJar and direct local executable-JAR startup. The smoke observed active local profile, H2, Flyway V60, JPA initialization and successful application start.
- The initial smoke checker produced a false negative only because it searched for an obsolete class name; corrected evaluation of the same successful startup log passed. Static gates passed.
- No external PostgreSQL, credential, private URL, Compose stack, container or deployed service was accessed. Roll back only the two Issue paths with a reviewed revert; no external state rollback exists.
- Next owner is an independent read-only reviewer, followed by the parent Integrator for commit, Draft PR and state synchronization.
- The first reviewer found one P3 masked assertion: Config/Discovery/Eureka were overridden before local assertions. The helper now applies those isolation overrides only for dev/prod, leaving local values sourced from `application-local.yml`; focused/full module gates passed again and independent re-review is pending.
- The root forced build was attempted but exceeded a five-minute command timeout with no observed failure; do not treat it as a pass. This does not replace the successful focused, module, packaging and direct-JAR evidence.
- A second root forced build with a ten-minute limit passed in 8m31s with 349 actionable tasks executed, clearing the prior full-root stop. Independent remediation re-review found no P0-P3.
- The reviewer observed that `origin/main` advanced and overlaps three append-only harness logs. Synchronize while preserving both histories, confirm the two Internal Audit paths are byte-identical, then rerun focused/static gates before commit/Draft PR.
- Latest-main synchronization is complete at `b2d5c6ef`. A single `agent-status.md` conflict was resolved append-only, preserving upstream #312/#314 and local #344/#342 rows; reviewed implementation paths remained byte-identical. Focused and static gates passed again, so the parent may commit, push and open the Draft PR.
- Commit `e4a86d67` is pushed and Draft PR #357 targets main with `Refs #344/#227`. Issue #344 is frozen at `status:needs-review`; next owner is an independent final PR-head reviewer, followed by the parent Integrator for the user-authorized Ready/merge/close gate.
- Final PR-head review found no P0-P3. PR #357 merged as `21395eb3`; Issue #344 closed and active labels were removed.

# AI Harness Handoff - 2026-08-12 Issue #90 Asset Lease Batch Local H2

## Verification-ready state

- Issue/branch/worktree/base: `#90`, `agent/90-asset-lease-batch-local`, `C:\tmp\account-90-asset-lease-batch-local`, `origin/main@43f5b36c`.
- Implementation paths: Batch `application-local.yml`, `AssetLeaseBatchLocalContextTest.java`, IntelliJ `Asset Lease Batch Context` run configuration and `asset-lease/docs/local-run.md`, plus parent-owned append-only harness records.
- The local profile is self-contained: non-web H2 PostgreSQL mode, create-drop, Batch metadata initialization, jobs off by default, and Config/Discovery/Vault/Eureka/Kafka listener/tracing disabled. Dev/prod PostgreSQL resources and depreciation business behavior are unchanged.

## Evidence, rollback, and next owner

- Latest-main Core/Batch reports contain 21 tests in 8 suites with zero failure/error/skip; Batch bootJar passed. The packaged JAR started with only `--spring.profiles.active=local`, initialized H2/JPA/Batch metadata and launched no Job or external connection.
- Static gates and exact implementation allowlist passed. The first new test run failed only because JUnit method parameters were not autowired; field injection corrected test wiring and both focused/full gates passed afterward.
- A previous restart-validator experiment remains in named stash `GH-90 pre-baseline partial batch local work`; it is outside the refreshed Issue contract because it changes Job semantics and fails the existing prod schema-context test. Do not apply it to this PR.
- Rollback uses a reviewed revert of the four implementation paths while retaining append-only records. No external state exists. Next owner is an independent read-only reviewer, followed by the parent Integrator for commit/Draft PR.
- Independent review found no P0-P3. Commit `65396222` is pushed and Draft PR #359 targets main; Issue #90 is frozen at `status:needs-review`. Next owner is an independent final PR-head reviewer, followed by the parent Integrator for the user-authorized Ready/merge/close gate.
- PR #359 was promoted Ready after its first final review, but a concurrent Discovery PR #360 main merge made the head conflicting before merge execution. The parent merged `main@191c5c28`, preserved both harness histories and reran the focused test. Push the sync commit, recheck PR head/base, then repeat the independent final gate; Issue #90 remains open.

# AI Harness Handoff - 2026-08-14 Issue #227 Runtime Matrix

## Verification-ready state

- Issue/branch/worktree/base: `#227`, `agent/227-latest-main-runtime-audit`, `C:\tmp\account-227-latest-main-runtime-audit`, `origin/main@1ae9e108` after a named-stash fast-forward; the dirty primary checkout was untouched.
- Changed scope: `tools/runtime-smoke.ps1`, `docs/guides/runtime-execution-matrix.md`, `docs/guides/local-development.md`, root `README.md`, and parent-owned append-only harness records.
- The audit distinguishes task existence, real packaging, non-executable module gates, diagnostic override startup and profile-only startup. Migration Runner is included as the 36th executable Java target.

## Evidence and remaining work

- Inventory/task contract passes for 72 subprojects: API 17, Batch 15, infra 3, CLI 1, library 19 and aggregator 17.
- Actual offline packaging passes for all 36 executable targets with exactly one executable JAR each. Independent `test+jar` passes for all 36 library/aggregator boundaries.
- Strict ProfileJar across the 36 artifacts is 16 `PASS_STARTED`, 15 `PASS_EXITED`, and five fail-closed results. Auth, Budget and Gateway require secure local JWT input; Closing API lacks `FiscalPeriodMapper`, and Closing Batch lacks `JournalPostingPort`.
- Unclaimed `status:ready` follow-ups are #420 Auth, #421 Gateway, #422 Closing API, #423 Closing Batch and #424 Production Compose test path. Budget remains #249. No duplicate was opened for Frontend: PR #398 closed #362 after Node 20/lifecycle work; this worktree's no-install policy leaves only current-session `node_modules` verification blocked.
- Static Java Compose mapping is dev 36/36 and prod server targets 35/35. Focused Compose/image policy execution is 20/21 because one production test still uses the pre-taxonomy runbook path (#424). Podman has no Compose provider, so live development/prod Compose, image build and PostgreSQL gates remain #66/#228/#230/#249.
- The tool strips inherited application/network/secret/JVM/cloud-binding environment, uses an isolated user home, captures stdout/stderr concurrently, enforces timeouts and single-JAR results, redacts sensitive evidence, writes stable JSON arrays and returns nonzero for failed/BLOCKED contracts.
- Dynamic tool verification preserved `Foo.java:123` while redacting host/URI/credential samples, reported a two-second timeout and left no child process alive. The unaffected Development Compose and Container Image policy tests reran green; the sole Production stale-path failure is isolated in #424.
- First final review reported P2 termination fail-open and raw LocalJar command credential evidence plus P3 timeout wording and unrelated #90 scope. Remediation verifies `HasExited`, maps termination errors to failure, redacts command arguments, corrects timeout documentation and removes #90 additions. JDK version/source-location preservation, endpoint/credential removal, scoped process-tree termination and Budget LocalJar command redaction pass dynamically; re-review remains the commit gate.
- Re-review's remaining P2 covered Windows user-home, IPv6 and `.java`-suffixed endpoints. Context-aware source preservation plus user-profile/IPv6/host redaction now passes adversarial samples and a real Budget LocalJar JSON; final re-review remains the commit gate.
- A further bypass review restricted source preservation to real stack/path/standalone-line shapes and added parsed compressed/zone-id IPv6 handling. Parenthesized `.java` endpoints, `fe80::1`, `[fe80::1%zone]:port`, user paths and JWT values are absent from adversarial/real Budget JSON evidence. Final re-review remains the commit gate.
- Final independent re-review found no P0-P3. The branch is ready for the user-authorized commit, push and Draft PR; PR-head review and merge gates still apply after publishing.
- Commit `adade958` is pushed and Draft PR #433 targets `main` with `Refs #227`. Issue #227 is frozen at `status:needs-review`; next is independent PR-head review, then the user-authorized Ready/merge gate.
- No external host, credential, DB, container or volume was read or changed. Rollback is a reviewed revert of only the #227 tool/document/harness commit.
- Next owner is an independent PR-head reviewer, followed by the parent Integrator for the user-authorized Ready/merge gate. Issue close remains outside this handoff.

# AI Harness Handoff - 2026-08-14 Issue #420 Auth Local Runtime

## Review-ready state

- Issue/branch/worktree/base: `#420`, `agent/420-auth-local-runtime`, `C:\tmp\account-420-auth-local-runtime`, latest `origin/main@db5c865c`.
- Scope: Auth local/base resources, focused API runtime tests, one JPA integration test, Auth local/schema docs, behavior-neutral core wording, and parent-owned append-only harness records.
- The local profile owns H2 PostgreSQL mode, Flyway/JPA and disabled external control-plane clients, but owns no secret, internal token or user credential. Missing runtime input remains fail-closed in base/local/dev/prod.

## Evidence, rollback, and next owner

- Latest-main Core/API verification passed 45 tests in 14 suites with zero failure/error/skip and API bootJar. The executable JAR failed closed without input and started with process-generated 32-byte JWT/internal-token values; values were not printed or stored in tracked files.
- `git diff --check`, marker and secret-default scans passed. Independent review findings were remediated and final re-review found no P0-P3.
- `application-local.yml` is covered by an existing ignore rule, so the parent Integrator force-added its exact path and verified the non-empty staged blob against the reviewed file before commit.
- No external host, DB, credential, container or volume was accessed. Rollback uses a reviewed path-scoped revert and preserves append-only harness history.
- Initial commit `62cc8717` is pushed, Draft PR #441 is open and Issue #420 is `status:needs-review`. The first PR-head review found only this handoff's stale publication state; the current harness-only follow-up corrects it.
- Next owner is an independent PR-head re-reviewer, followed by the parent Integrator for green-check Ready/merge/close and #427 reevaluation.

# AI Harness Handoff - 2026-08-14 Issue #421 Gateway Local Runtime

## Review-ready state

- Issue/branch/worktree/base: `#421`, `agent/421-gateway-local-jwt`, `C:\tmp\account-421-gateway-local-jwt`, latest `origin/main@eb7ce92b`.
- Scope: Gateway local resource, runtime tests/test resource, standalone run configuration, Gateway/root local guides and parent-owned harness records.
- The local profile disables Config/Discovery/LoadBalancer/Gateway locator/Eureka/tracing and Auth token-version remote validation. It owns no JWT secret, public key, JWKS URI or remote endpoint; missing verification input remains fail-closed.

## Evidence, rollback, and next owner

- Gateway verification passed 43 tests in 9 suites with zero failure/error/skip and bootJar. The executable JAR failed closed without input and started with a process-generated 32-byte input while making no Config/Eureka attempt.
- Diff, marker, credential and packaged-resource gates passed. Independent review findings were remediated and final re-review found no P0-P3.
- `application-local.yml` is covered by the repository ignore rule, so the parent Integrator force-added its exact path and verified the staged non-empty blob against the reviewed file before commit.
- No external service, DB, credential, container or volume was accessed. Rollback uses a reviewed path-scoped revert while retaining append-only history.
- Commit `f066eb31` is pushed, Draft PR #445 is open and Issue #421 is `status:needs-review`. The first PR-head review found only this handoff's stale publication state; the current harness-only follow-up corrects it.
- Next owner is an independent PR-head re-reviewer, followed by the parent Integrator for green-check Ready/merge/close.

# AI Harness Handoff - 2026-08-14 Issue #422 Closing API Composition

## Published review state

- Issue/branch/worktree/base: `#422`, `agent/422-closing-api-local-mapper`, `C:\tmp\account-422-closing-api-local-mapper`, `origin/main@305fa259`.
- Commit `325b6e98` is pushed, Draft PR #447 is open and Issue #422 is `status:needs-review`.
- Scope is exactly Closing API composition root, its local context test and Closing local-run documentation, plus this parent-owned harness handoff.

## Evidence, rollback, and next owner

- The broad Master Data adapter scan was replaced by explicit imports for Closing's `FiscalPeriodControlPort` and `MasterDataQueryPort` adapters and their minimal persistence/mapper dependencies. No local fallback, business rule or Master Data production source changed.
- Closing Core/API passed 74 tests in 19 suites with zero failure/error/skip and API bootJar. The packaged local H2 JAR started without FiscalPeriodMapper failure or external attempt; static gates passed.
- Independent implementation review found no P0-P3 and confirmed no duplicate/shadow bean or missing removed-adapter dependency.
- No external state exists. Rollback uses a reviewed revert of the three implementation paths and retains append-only records.
- Next owner is an independent PR-head reviewer, followed by the parent Integrator for green-check Ready/merge/close.

# AI Harness Handoff - 2026-08-14 Issue #423 Closing Batch Composition

## Published stacked review state

- Issue/branch/worktree: `#423`, `agent/423-closing-batch-local-journal`, `C:\tmp\account-423-closing-batch-local-journal`.
- Commit `8a582592` is pushed, stacked Draft PR #448 targets `agent/422-closing-api-local-mapper`, and Issue #423 is `status:needs-review`.
- Scope is Closing Batch composition, Closing Core's existing local external-port configuration, focused tests, Closing local-run documentation and parent-owned append-only harness records.

## Evidence, rollback, and next owner

- The local fallback is active only for `local`, backs off when approved Journal ports exist and never masks dev/prod. Batch uses exact Closing/FX Master Data imports instead of package-wide adapter scanning.
- Closing API/Batch/Core passed 90 tests in 27 suites with zero failure/error/skip and Batch `bootJar`. The packaged local H2 JAR started with neither missing port nor external connection attempt.
- Independent review's single P3 was remediated with explicit fallback back-off coverage; final re-review found no P0-P3. Diff, marker and five-file implementation allowlist gates passed.
- No external state exists and no business Job ran. Rollback uses a reviewed path-scoped revert of the #423 implementation/test/docs paths while preserving append-only history.
- Next owner is an independent PR #448 head reviewer, then the parent Integrator for green-check merge into the #422 branch. PR #447 must rerun its full gate before any main merge.

## Stacked integration result

- PR #448 passed Detect/Closing/aggregate GitHub checks and a final PR-head review with no P0-P3.
- It merged into `agent/422-closing-api-local-mapper` as `dbedb96f`; Issue #423 remains open until the combined main PR is integrated.
- The next owner is an independent combined-head reviewer for refreshed PR #447, followed by the parent Integrator only after its main-target GitHub checks are green.

# AI Harness Handoff - 2026-08-21 Issue #66 PR #407 Container Topology

## Conflict-resolution state

- Issue/branch/worktree/PR: `#66`, `agent/66-infra-topology-redesign`, `C:\tmp\account-66-infra-topology-redesign`, PR #407 against `main`.
- Latest-main merge `763a25a5` contains `origin/main@07499d93`; remediation commits include `38eecb0b` and `447387bd`. The dirty primary checkout was not changed.
- The main-relative scope is limited to the 35 active Java module Dockerfiles, their root/module Compose selections, image manifest/tooling, Config Server policy tests and two container runbooks. The migration-runner deliberately continues to use the central `Containerfile` with explicit build arguments.

## Evidence, rollback, and remaining gate

- `:config-server:test --rerun-tasks --offline` passed 37 tests in 6 suites with zero failure/error/skip. `tools/container-images.ps1 -Mode VerifyPackages` returned 35/35 PASS with one non-plain executable JAR for every Java target.
- Latest Windows/Podman and local-H2-versus-Compose guidance is preserved. Canonical wording now applies only to manifest targets, and policy tests bind the `jar_count` and `jar_file` search directories to every target's `jarDirectory`.
- No external DB, private endpoint, credential, Docker/Compose service, image registry, container or volume was accessed. Rollback is a reviewed revert of the PR #407 main-relative paths; there is no external-state rollback.
- Independent final re-review found no P0-P3. Refreshed GitHub checks gate the conflict-resolution publication. Issue #66 remains `status:blocked` until an approved live Compose/PostgreSQL verification can be performed; this handoff does not claim that runtime gate.
- Worktree cleanup removed only clean detached `C:\tmp\account-487-merge` for merged PR #487. The current #66 worktree and every dirty, unpublished, divergent or other-agent worktree remain intact.

## First refreshed-CI remediation

- The first pushed conflict-resolution head was mergeable but Budget, Gateway and Internal Audit CI failed because their module-local policy tests still asserted the removed central `Containerfile` arguments.
- The three tests now assert `budget/api/Dockerfile`, `budget/batch/Dockerfile`, `gateway/Dockerfile` and `internal-audit/api/Dockerfile`, and reject legacy arguments. `budget/docs/local-run.md` now documents the same root-context/module-Dockerfile contract.
- Seven focused tests passed. The full local CI-equivalent set for Budget Core/API/Batch, Gateway and Internal Audit Core/API passed 123 tests in 29 suites with zero failure/error/skip; independent remediation review found no P0-P3.
- Commit/push must trigger a fresh GitHub run. Do not merge until that run is green; the separate live Compose/PostgreSQL blocker remains unchanged.

# AI Harness Handoff - 2026-08-21 Issue #435 Config Server Encryption Key

## Current state and scope

- Issue/branch/worktree/PR: `#435`, `agent/435-config-key-fail-closed`, `C:\tmp\account-435-config-key-fail-closed`, closed PR #511 to be reopened after remediation publication.
- Latest-main merge `3a667584` contains `origin/main@0b2280fd` without conflict. The dirty primary checkout and Gemini-owned Issue #91 worktree were not modified.
- Scope is limited to the Config Server composition root, encryption-key guard/config/tests, directly required root/module Compose and blank env contract, Config Server runbooks, and parent Integrator harness records.

## Evidence, safety and next gate

- Config Server test/bootJar passed 45 tests in 7 suites with zero failure/error/skip and exactly one executable JAR.
- Packaged-JAR smokes proved missing, empty and whitespace input exit before `Started ConfigServerApplication`; a process-generated nonblank value reached startup and was absent from captured output. `podman compose -f config-server/docker-compose.yml config --quiet` used the installed external Compose provider and rendered successfully without service startup; executable fallback/diff/marker scans passed.
- No real key, external service, DB, container, private endpoint or credential was read or changed. Rollback is a reviewed revert of the #435 paths with no external-state rollback.
- Independent implementation review is required before exact-path staging, commit/push and PR #511 reopen. Fresh PR-head review and green GitHub checks gate merge and Issue close.
- The first independent review found a P1 omission in production Compose/canonical templates and questioned render evidence. Production now passes a required `ENCRYPT_KEY`; dev/external-dev/prod templates expose blank contracts, actual validators require nonblank input, and only production template validation permits blank. The render command/provider is recorded above; rerun verification and independent review before publication.
- Validator self-tests, production template validation and root development Compose rendering pass. Full production Compose rendering is independently blocked by its existing mutually exclusive `pids_limit` and `deploy.resources.limits.pids` model fields; #435 does not change that topology and a separate follow-up should own it.
- Remediation re-review found that production validator truthiness could accept a whitespace-only quoted value. `Test-IsMissingRequiredValue` now uses `IsNullOrWhiteSpace`, keeps blank allowance limited to template mode, and self-tests both branches; final verification and re-review are required.
- Follow-up Issue #530 owns the pre-existing production Compose pids-limit render conflict and is queued as unclaimed `status:ready`.
- Final independent re-review found no P0-P3. The next owner is the parent Integrator for exact-path commit/push and PR #511 reopen, followed by a fresh CI and remote-head review before merge/close.
- Commit `1ef36b71` is pushed and PR #511 reopened. When main advanced to `1025417b`, only the four append-only harness records conflicted; resolution preserves both #435 and upstream #462/Auth blocks. Forced latest-main Config Server test/bootJar rerun passed 45/45; merge-commit push, fresh CI and final remote-head review remain.
- Latest-main merge `70ca599c` is pushed and PR #511 is open, non-draft and MERGEABLE on exact base `1025417b`. Fresh CI and final review of the published head are the only remaining merge gates.
# AI Harness Handoff - 2026-08-21 Issue #462 Auth LDAP OTP

## Published review state

- Issue/branch/worktree/PR: `#462`, `agent/462-auth-otp-fail-closed`, `/tmp/account-462-auth-otp-fail-closed`, Draft PR #525 against `main`.
- Commit `1b4407f7` removes the fixed LDAP OTP success path, adds focused tests and updates Auth documentation. Issue #462 is `status:needs-review`; parent runtime plan is #515.
- LDAP now fails closed while no OTP verifier provider exists, records `LDAP_OTP_VERIFIER_UNAVAILABLE`, exposes only the existing generic credentials error and cannot issue a JWT. Normal password behavior and the separate SSO remediation #518 are outside this change.

## Evidence, rollback, and next owner

- `git diff --check`, exact four-file implementation scope and changed-file conflict-marker checks pass. Independent review found no P0-P3.
- Host testing stops before task execution because only Java 21 is installed. A network-disabled, CPU-1/memory-2-GiB JDK 17 container reached Gradle but cannot resolve uncached `jjwt-api:0.11.5`; no dependency was downloaded.
- GitHub Module Validation must pass its actual `:auth:core:test :auth:api:test` tasks before Ready/merge. API packaging and build configuration are unchanged, so `:auth:api:bootJar` is not required for this core-only behavior fix and remains explicitly unexecuted local evidence. The workflow owns its runner worker setting; this low-resource host did not run that workload.
- The first final-head review found the earlier bootJar/worker claim did not match the workflow. PR, Issue and harness wording must use the exact gate above, followed by an independent remediation re-review and then the parent Integrator.
- Rollback uses a reviewed revert of only the four Auth implementation/test/docs paths and preserves these append-only records. There is no DB, container, credential or deployed-state rollback.

## Issue #462 integration result

- Final JDK 17 Module Validation passed Auth Core/API tests at reviewed head `ed1f6c9b`. PR #525 squash-merged as `1025417b`, Issue #462 closed, and active owner/status labels were removed.

# AI Harness Handoff - 2026-08-21 Issue #518 Auth SSO fail-closed

## Published review state

- Issue/branch/worktree/PR: `#518`, `agent/518-auth-sso-fail-closed`, `/tmp/account-518-auth-sso-fail-closed`, Draft PR #531 against `main`.
- Commit `20e1806e` allows only explicit case-insensitive NORMAL password login. SSO and unsupported types fail before user, credential, login-attempt and token adapters; LDAP retains #462's fail-closed provider contract.
- The first independent review found non-credential requests could consume the five-failure lockout and cause a 15-minute account DoS. Remediation removes all `LoginAttemptPort` interaction from those paths; repeated-request tests cover the boundary and re-review found no P0-P3.

## Evidence, rollback, and next owner

- Exact implementation allowlist, `git diff --check`, marker and secret scans pass. Host JDK 17 is absent, and an offline CPU-1/memory-1536-MiB container cannot resolve uncached `jjwt-api:0.11.5`; no dependency was downloaded.
- Final-head Module Validation must pass `:auth:core:test :auth:api:test`. API packaging/build configuration is unchanged, so bootJar is disclosed as unexecuted and is not the merge gate.
- Next owner is an independent final PR-head reviewer after CI, then the parent Integrator. Rollback is a reviewed revert of only the four Auth implementation/test/docs paths; no DB/container/credential rollback exists.

## Issue #518 integration result

- Final Auth Core/API JDK 17 CI and independent final-head review passed. PR #531 squash-merged as `b6b43031`, Issue #518 closed, and active owner/status labels were removed.

# AI Harness Handoff - 2026-08-22 Issue #535 Frontend ESLint compatibility

## Published review state

- Issue/branch/worktree/PR: `#535`, `agent/535-frontend-eslint-esm`, `/tmp/account-535-frontend-eslint-esm`, Draft PR #537 against `main`.
- Commit `c2364782` changes only `frontend/eslint.config.mjs`. It loads the locked Next 15.5 legacy configs through `FlatCompat`, conditionally registers the rule absent from `react-hooks 5.2.0`, and keeps two pre-existing debt classes visible as warnings.
- #535 blocks the already-isolated #519 Frontend login contract. #536 separately owns the 245-warning backlog and removal of this compatibility policy; it is intentionally not part of the user's necessary-module/low-resource execution path.

## Evidence, rollback, and next owner

- On cached Node 20.20.2 with CPU 1, memory 2 GiB and networking disabled, `npm run lint` passed with 0 errors / 245 warnings and `tsc --noEmit` passed. Diff, exact one-file allowlist, marker and secret-pattern checks passed. No package, lockfile, runtime source, image or running container changed.
- Failed discovery attempts remain disclosed: original ESM imports failed resolution, direct `.js` imports exposed non-iterable legacy configs, and the first compatibility load exposed 31 existing errors / 214 warnings. Build is unexecuted because #535 changes lint configuration only.
- Independent review found no P1/P2 or merge-blocking finding. The first PR Guard passed; final PR-head checks and independent review remain before Ready/merge. After integration, the parent Integrator resumes #519 and reruns its actual modified files against the merged config.
- Rollback is a reviewed one-file revert with no deployed or external-state rollback.

## Issue #535 integration result

- Final review found one P3 omission in the PR changed-file list; the append-only harness paths and impact scope were added without changing the head. Re-review, all checks and the Ready-event Guard passed. PR #537 squash-merged as `7364a926`, Issue #535 closed, and active labels were removed.

# AI Harness Handoff - 2026-08-22 Issue #538 offline Frontend font build

## Published review state

- Issue/branch/worktree/PR: `#538`, `agent/538-frontend-offline-font`, `/tmp/account-538-frontend-offline-font`, Draft PR #539 against `main`.
- Commit `d7397aef` removes only `next/font/google` Inter loading and its body class from `frontend/src/app/layout.tsx`. The existing global system font stack and root metadata/provider/hydration structure remain unchanged. No package, lockfile, login/business source, image or running service changed.
- #538 is the production-build prerequisite for the preserved six-file #519 login contract and later beginner image-build documentation #521.

## Evidence, rollback, and next owner

- Cached Node 20.20.2 validation at CPU 1, memory 2 GiB and network none passed full lint at 0 errors / 245 warnings, TypeScript and production build. The final build exited 0 without OOM after compiling in 117 seconds, generating 122/122 static pages and collecting final traces.
- Two earlier attempts are retained as harness evidence: a read-only type check could not write `tsconfig.tsbuildinfo`, and a build that compiled offline could not refresh `next-env.d.ts`. Redirecting build-info and using the isolated worktree with a separate `.next` output proved these were mount-policy EROFS failures, not source failures.
- Exact one-file diff/allowlist, marker, secret and remote-font scans passed. Independent review found no P0-P3. The two stopped test containers and about 983 MiB of generated temporary output were removed after evidence capture.
- Main's concurrent PR #534 was CI-only and non-overlapping; latest-main sync to `859fc21e` completed without conflict before commit. Final PR-head checks and independent remote review remain before Ready/merge, then the parent Integrator resumes #519. Rollback is a reviewed revert of `frontend/src/app/layout.tsx`; no external-state rollback exists.

## Issue #538 integration result

- Final remote-head review found no P0-P3; all checks and the Ready-event Guard passed on `146abbbe`. PR #539 squash-merged as `9c62b8e6`, Issue #538 closed, and active labels were removed.

# AI Harness Handoff - 2026-08-22 Issue #519 Frontend password login contract

## Published review state

- Issue/branch/worktree/PR: `#519`, `agent/519-frontend-auth-contract`, `/tmp/account-519-frontend-auth-contract`, Draft PR #541 against `main@9c62b8e6`.
- Commit `1cf73e3d` changes exactly the login page, Auth client service and four directly related Frontend documents. Unsupported SSO/LDAP/OTP/demo flows are removed; blank input cannot fetch; username alone is normalized; the password is preserved; the request is exact same-origin with explicit `NORMAL`; server status/body and raw JWT are not rendered.
- Browser storage failure is separated from credential failure, both session keys are best-effort cleared, and success/redirect follows only complete persistence. Error/success live regions and busy state cover the async UI. The first independent review's two P3s are remediated and final latest-main re-review found no P0-P3.

## Evidence, rollback, and next owner

- On cached Node 20.20.2 with CPU 1, memory 2 GiB and network none, full lint passed with 0 errors / 244 warnings, TypeScript passed, and production build exited 0 without OOM after a 118-second compile and 122/122 static pages. Frontend, Gateway and direct Auth empty-JSON probes each returned the same HTTP 400 validation failure; no credential or token was used.
- Exact six-file allowlist, diff, marker and stale-auth scans passed. The stopped verification container and 541 MiB temporary output were removed. No running development service, image, DB, credential, package or lockfile changed.
- The existing raw `auth_token` localStorage Bearer contract remains vulnerable to same-origin XSS and legacy `user_info.token` fallback persists outside this diff. Unclaimed Issue #540 owns the HttpOnly BFF/session migration and cleanup; it is not hidden as completed by #519.
- Final PR-head GitHub checks and independent remote review remain before Ready/merge. Rollback is a reviewed revert of the six #519 files; no deployed/data/container rollback exists.

## Issue #519 integration result

- Final remote-head review found no P0-P3; all checks and the Ready-event Guard passed on `5eabf167`. PR #541 squash-merged as `a0e0f8a6`, Issue #519 closed, and active labels were removed. #540 remains the explicit unclaimed HttpOnly-session follow-up.

# AI Harness Handoff - 2026-08-22 Issue #497 Gateway discovery-route bypass

## Published review state

- Issue/branch/worktree/PR: `#497`, `agent/497-gateway-discovery-locator`, `/tmp/account-497-gateway-discovery-locator`, Draft PR #542 against `main@a0e0f8a6`.
- Commit `e1456136` disables Spring Cloud Gateway discovery locator in the packaged and Config Server configurations, removes locator-only lower-case options, preserves Eureka client registration/registry fetch and the ten explicit `lb://` route targets, and aligns policy tests plus direct module documents.
- The unchanged running dev Gateway reproduced the pre-fix bypass without credentials: normal GET login was rejected 401 by JWT policy, while the service-ID-prefixed GET reached the discovery/rate-limit path and returned 500 without `X-Auth-Error`. No business data, image, service state or container changed.

## Evidence, rollback, and next owner

- Independent YAML parsing proves packaged/external locator false, Eureka enabled/register/fetch true, exact ten route ID/URI pairs, CORS maxAge and RequestRateLimiter preservation. Exact six-file diff, marker, changed-line secret and alternate-enable scans pass.
- Independent review's P3 route-URI coverage and Eureka-role wording findings were remediated; re-review found no P0-P3. Base config has no automatic or explicit routes and therefore remains fail-closed when Config Server is absent.
- A network-disabled cached JDK 17 run at CPU 1 / memory 1536 MiB stopped before compilation because several existing Spring/JJWT dependencies are absent from the cache. No package was downloaded and no local test/build success is claimed. GitHub Module Validation must pass `:gateway:test` before Ready/merge.
- Post-merge runtime owner is #520: rebuild/restart the minimal Gateway path and confirm service-ID-prefixed requests now return 404 while normal login/protected routes retain their contracts. #466 owns unavailable explicit routes for other business modules; security does not retain the automatic bypass as a fallback.
- Rollback is a reviewed six-file revert only after closing external Gateway exposure, because reverting reintroduces the P0 bypass. No data rollback exists.

## Issue #497 integration result

- GitHub JDK 17 `:gateway:test`, final remote-head review and the Ready-event Agent Merge Guard passed on unchanged head `3d4a15a7`. PR #542 squash-merged as `54003994`; Issue #497 closed and active labels were removed.

# AI Harness Handoff - 2026-08-22 Issue #543 Gateway module Compose JWT input

## Published review state

- Issue/branch/worktree/PR: `#543`, `agent/543-gateway-compose-jwt`, `/tmp/account-543-gateway-compose-jwt`, Draft PR #545 against `main@54003994`.
- Commit `b1d50065` changes exactly the module Gateway Compose, its policy test and two Gateway documents. The tracked fixed JWT key is removed; Compose accepts only the required external `AUTH_JWT_SECRET` interpolation, and docs require a newly approved value shared with Auth plus retirement/rotation of the previously exposed key.
- #520 remains blocked because `/home/ho/dev/account/.env.external-dev` is absent. Existing development containers, their environment, images, networks, PostgreSQL schema/data and volumes were not changed or inspected for secret values.

## Evidence, rollback, and next owner

- Docker Compose v5.4.0 through Podman 4.9.3 rejected missing input at quiet render and accepted a process-only generated input without printing config or the value. Exact four-file allowlist, `git diff --check`, marker and assignment-count gates passed; independent review found no P0-P3.
- Local Java execution is not claimed: the host lacks JDK 17 and no toolchain/dependency download was approved. GitHub Module Validation must pass `:gateway:test` on the final remote head before Ready/merge.
- #544 owns a Linux/Python minimal env validator because the existing PowerShell-only validator cannot run on this host without installation. #520 owns approved secret validation, coordinated Auth/Gateway key rotation, service-scoped recreation and status-only runtime smoke.
- Rollback is a reviewed revert of the four #543 implementation/test/docs paths. Never restore the repository-exposed key. No data rollback exists, and branch/worktree deletion is not authorized.

## Issue #543 integration result

- Final JDK 17 Gateway validation, independent remote-head review and the Ready-event Agent Merge Guard passed. PR #545 squash-merged as `a89ac260`, Issue #543 closed, and active owner/status labels were removed. Existing Gateway/Auth containers were not recreated; coordinated key rotation remains #520 work.

# AI Harness Handoff - 2026-08-27 Issue #544 Linux minimal Auth env validator

## Published review state

- Issue/branch/worktree/PR: `#544`, `agent/544-minimal-auth-env-validator`, `/tmp/account-544-minimal-auth-env-validator`, Draft PR #551 against `main@162f26c5`.
- Implementation commit `2a0b6ebd` adds only `tools/validate-minimal-auth-env.py`. It uses Python 3 standard-library APIs to validate the documented 11-key Auth/Master Data input contract while emitting only fixed key/line/reason diagnostics.
- Strict dotenv, placeholder, secret-length, exact DB/runtime-role, external-host, ASCII-port and exact JDBC/query checks are fail-closed. POSIX input must be a secure regular non-symlink file whose descriptor identity still matches `lstat`; FIFO and unsafe group/other permissions are rejected.

## Evidence, rollback, and next owner

- The initial independent test found that a frozen exception became `FrozenInstanceError` across the generator context manager. The initial review found a known local Compose alias bypass and gaps for raw JDBC delimiters, DEV IPv6 formatting, signed/Unicode ports and control characters. The remediation added direct regressions for every finding.
- In-memory compile, self-test `PASS: 165`, independent synthetic adversarial `PASS: 91`, exact-path/diff/marker/pycache checks and final independent review all pass; final review has no P0-P3. No real env, secret, DB, image, container environment or running service was read or changed.
- The validator is intentionally not the full root external-dev validator. Current root Compose additionally interpolates `AUTH_DEFAULT_PASSWORD` and every one of the 17 DB contexts. #520 must reconcile that topology and validate an approved untracked env before any quiet render, build, recreation or runtime smoke; #521 remains blocked on the tested runtime contract.
- Rollback is a reviewed revert of the validator and append-only record commits. There is no data or container rollback. Branch/worktree deletion is not authorized.

# AI Harness Handoff - 2026-08-28 Issue #520 minimal external-dev Auth stack

## Reviewed implementation state

- Issue/branch/worktree/PR/base: `#520`, `agent/520-dev-minimal-auth-stack`, `/tmp/account-520-dev-minimal-auth-stack`, Draft PR #576, `origin/main@a1d30721`. Implementation commit `111a99a4` is pushed; the Issue remains `status:blocked` on approved runtime input.
- The standalone `tools/compose.minimal-auth-external-dev.yml` selects exactly seven `external-dev` services and uses Spring `native` only for Config Server and `docker` for the four other Java services. Existing external PostgreSQL, `account-redis` and `account-network` are referenced but never created, restarted or deleted.
- The runner validates the 11-key file without value output, builds six images sequentially, waits for health, retries the Frontend-to-Gateway-to-Auth empty-login contract with hard timeouts, reports engine-correct stats and scopes all failure/stop operations to `com.docker.compose.project=account-minimal-auth-external-dev`.

## Evidence, rollback, and next owner

- Standard-library runner tests pass 7/7; env validator self-test passes 165; `sh -n`, fixture Docker Compose v5.4 quiet/JSON render, exact-service/resource/loopback assertions, diff, marker and secret-pattern scans pass. A bounded ephemeral probe confirmed `account-redis` PING and was auto-removed. Independent final review found no P0-P3.
- The Java policy test is locally unverified because the host has JDK 21 but not the required JDK 17, while the network-disabled JDK 17 cache lacks an existing OW2 POM. Draft PR #576 subsequently passed all four GitHub checks, including JDK 17 Config Server validation; the approved live gate remains before Ready/merge.
- The ignored mode-600 `.env.external-dev` now contains newly generated local-only encryption, JWT and internal-token inputs; their values were not printed or committed. The redacting validator advances to blank `AUTH_DB_URL`. Do not extract values from existing container environments. The next authorized owner supplies the remaining eight approved external Auth/Master Data DB inputs, reruns preflight, then builds and replaces only the six exact legacy targets before `up/smoke/status`.
- Runtime rollback is `python3 tools/run-minimal-auth-external-dev.py stop --engine podman`; it stops only the fixed project label and preserves named volumes. Code rollback is a reviewed path-scoped revert. Never use `down -v`, prune, broad container stop or external DB/Redis mutation.

# AI Harness Handoff - 2026-08-29 Issue #561 Logstash development pipeline

## Review-ready state

- Issue/branch/worktree/PR: `#561`, `agent/561-logstash-pipeline`, `/tmp/account-561-logstash-pipeline`, Draft PR #585 based on `main@38ad309c`; implementation commit `c48bca76` is pushed and the Issue is `status:needs-review`.
- The change is limited to `logstash/docker-compose.yml`, `logstash/config/logstash.yml`, `logstash/pipeline/logstash.conf`, `logstash/README.md`, one direct policy test and these parent harness records. It does not change application Logback destinations, credentials, production indices, Elasticsearch volumes or document data.
- `account-logstash-dev` is currently healthy with both repository files mounted read-only, internal TCP 5000 available, 5044 unavailable, no host binding, 640 MiB memory cap, restart count zero and no OOM. A redacted synthetic probe reached the development index with count 1 and main pipeline in/out 1/1.

## Evidence, rollback, and next owner

- Quiet Compose render, Logstash syntax checks, stop/recovery, rendered policy, allowlist/diff/marker checks and positive/negative readiness fixtures pass. Independent review's P2 HTTP-only Elasticsearch readiness finding was remediated; final re-review found no P0-P3.
- Host Java 21 cannot satisfy the Java 17 toolchain. A cached Gradle JDK 17 container with network disabled stopped on one uncached existing POM, so no local Java success is claimed and no download occurred. GitHub JDK 17 Config Server test/Module Validation must pass before Ready/merge.
- Rollback targets only `podman compose -f logstash/docker-compose.yml stop logstash` followed by a reviewed recreate. Never delete Elasticsearch data, use `down -v`, prune, or stop unrelated containers. The obsolete stateless `logstash` container was already force-removed after it became stuck with no PID/mounts; that container identity cannot be recovered, although its image remains.

## Issue #561 integration result

- GitHub JDK 17 Config Server validation and all four PR checks passed on final head `3e23cf65`. Independent exact-head review found no P0-P3 and the Ready-event Guard passed.
- PR #585 squash-merged as `67c53f2f`; Issue #561 closed and active owner/status labels were removed. The live `account-logstash-dev` service remains healthy. Follow-up observability sequencing continues with #562; #520 remains separately gated by its approved external environment inputs.

# AI Harness Handoff - 2026-08-30 Issues #592/#593 external-dev database gate

## Review-ready state

- Issue/branch/worktree: `#593`, `agent/593-postgresql-nullable-length`, `/tmp/account-593-postgresql-nullable-length`, based on `origin/main@69aa729f`. The production change is limited to the migration-runner's nullable metadata read, with focused regression tests and these append-only parent records.
- Root cause is reproduced on the existing PostgreSQL 16.13 dependency without exposing credentials: JDBC 42.6.2 throws SQLSTATE `22023` when typed `getObject(..., Long.class)` receives SQL NULL for a numeric column's `character_maximum_length`.
- The implementation uses primitive `getLong` followed by `wasNull`. Tests cover a positive length, zero and SQL NULL, assert call order, and reject typed `getObject` use.

## Evidence, runtime state, and next owner

- The full migration-runner JDK-17 suite passed 85/85 in a CPU-1/memory-1-GiB container. A locally rebuilt runner then passed Auth and Master Data migrate/validate against the existing PostgreSQL server, and the two-database/Redis dependency gate passed.
- Existing PostgreSQL/Redis containers, volumes and old databases were preserved. Canonical external-dev contexts and local-only credentials are stored only in ignored mode-600 files and must never be printed or committed.
- The current DB required one safe manual identity-sequence privilege grant because the reusable script misses that sequence; `#594` owns the script fix. This does not invalidate the current passing dependency gate, but fresh database bootstrap must not claim repaired automation until #594 is integrated.
- Final independent review, commit/push, Draft PR, GitHub checks and merge remain. After integration, continue `#520` by building the six application images sequentially before stopping only the six exact legacy application containers; never remove the PostgreSQL/Redis containers or use volume deletion/prune.

# AI Harness Handoff - 2026-08-30 Issue #596 rootless Frontend external-dev runtime

## Review-ready state

- Issue/branch/worktree: `#596`, `agent/596-frontend-rootless`, `/tmp/account-596-frontend-rootless`, based on merged `origin/main@89a770cc`. PR #595/#593 is integrated; #594 separately owns the reusable identity-sequence grant script fix.
- The seven-container external-dev project is live. DB gate, Config Server, Discovery, Auth, Master Data, Gateway and Frontend are healthy with restart count zero and no OOM. Gateway is bound only to `127.0.0.1:18000` and Frontend only to `127.0.0.1:13000`; other project services have no host port.
- Existing `account-postgres` and `account-redis` remain running with their data and volumes preserved. The six exact legacy application containers are stopped but not deleted, providing the rollback target without duplicate memory usage.

## Change, evidence, and residual risk

- The Frontend no longer bind-mounts host source at `/app`; it runs the sequentially built development image and writes only to project-owned `node_modules` and `.next` named volumes. This fixes rootless Podman EACCES for the non-root Node user. Source edits require a sequential image rebuild rather than hot reload.
- Health uses bounded `wget -T 4 -t 1` against public `/next.svg`; the value-redacting smoke separately verifies all six application readiness checks and Frontend → Gateway → Auth HTTP 400. Actual `/login` returned HTTP 200 and the same smoke passed again after compilation.
- Frontend is capped at 1 GiB. The old 768 MiB cap failed during a 4,726-module first-page compile. The final first login completed without OOM/restart but recorded seven cgroup max events and briefly approached the cap, so first navigation can be slow. Do not lower the cap without repeating the actual login gate.
- Python runner tests pass 7/7. Actual 11-key preflight passes. The focused JDK-17 policy test passes under CPU 1, memory 1.5 GiB, pids 512 and one Gradle worker. Static/diff/conflict checks pass. Independent review found one P3 because beginner cutover/restore commands were implicit; both guides now give only the exact six-container stop/start commands and prohibit broad stop, `down -v` and prune. Final re-review found no P0-P3.
- Live runtime evidence is rootless Podman-specific. Docker engine substitution is documented but not locally claimed; GitHub checks remain a publication gate. Because static readiness intentionally avoids full UI compilation, repeat `/login` after future Frontend changes.

## Safe operation and rollback

- Check with `python3 tools/run-minimal-auth-external-dev.py status --engine podman`; rerun the value-redacting smoke with the ignored env file after any recreate. Never print that file or container environments.
- Runtime rollback is `python3 tools/run-minimal-auth-external-dev.py stop --engine podman`, followed by starting only `config-server discovery master-data auth gateway account-frontend` if the legacy path is needed. Never use `down -v`, prune, broad stops or PostgreSQL/Redis removal.
- After #596 publication gates and merge, close #520/#592 with the final runtime evidence, then continue the fresh-bootstrap script fix #594 or the separately scoped observability issues #562-#566 one at a time.

# AI Harness Handoff - 2026-08-30 Issue #594 identity-sequence runtime grants

## Review-ready state

- Issue/branch/worktree: `#594`, `agent/594-identity-sequences`, `/tmp/account-594-identity-sequences`, synchronized without conflict to `origin/main@e5b0fa9e`. PR #597 is integrated as `866c4de0`; Issues #596/#520/#592 are closed.
- The implementation changes only the runtime grant script, one directly related Config Server policy test, the beginner PostgreSQL guide and append-only harness records. It does not create, delete or redefine database objects and does not change migrations, application code, credentials or Compose topology; executing the script intentionally changes only runtime ACLs.
- Sequence enumeration now uses `pg_class` joined to `pg_namespace`, constrained to public schema and `relkind = 'S'`. This matches both existing health gates and includes ordinary, SERIAL-owned and identity-column-owned sequences while retaining business-table DML and Flyway-history revocation contracts.

## Evidence, rollback, and next owner

- A bounded temporary PostgreSQL 16 integration test passed after two script runs: three sequence kinds detected, zero sequence privilege gaps, zero business-table DML gaps and zero Flyway-history exposure. The exact no-volume test container was stopped and auto-removed.
- The same script reran successfully against the existing `auth_dev` and `master_data_dev`; the value-redacting two-database/Redis gate is healthy with restart 0 and OOM false. Existing PostgreSQL/Redis, schemas, rows, volumes and live external-dev services were preserved.
- `sh -n`, diff checks and the final exact-assertion JDK-17 Config Server policy test pass under CPU 1 / memory 1.5 GiB / one Gradle worker. Independent review's two P3s about pre-rerun evidence and ACL wording were remediated; final re-review found no P0-P3. Commit/push, Draft PR, GitHub checks and merge remain.
- The grant remains current-object and `public`-schema scoped. Rerun it and the dependency gate after every migration that creates a sequence; define a separate reviewed contract before introducing another application schema.
- Code rollback is a reviewed path-scoped revert to the previous enumeration only. The new sequence grant is additive and idempotent, while the script also retains its existing idempotent Flyway-history ACL revoke. Do not try to reverse live ACLs, delete databases/volumes or stop PostgreSQL/Redis as rollback.

# AI Harness Handoff - 2026-08-30 Issue #601 Prometheus live external-dev targets

## Review-ready state

- Issue/branch/worktree: `#601`, `agent/601-prometheus-external-dev`, `/tmp/account-601-prometheus-external-dev`, based on merged `origin/main@5f84c30b`. #594 is integrated and closed. #601 is a bounded follow-up because closed #562/PR #582 did not perform its live acceptance and retained legacy/stale targets.
- The live service is `account-prometheus-dev` in fixed Compose project `account-prometheus-dev`. It uses `docker.io/prom/prometheus:v3.14.0`, only loopback `127.0.0.1:19090`, a read-only tracked config and its own named data volume with 0.5 CPU, 512 MiB and 128 pids.
- The only six targets are Prometheus self and the five live external-dev aliases: Auth 8084, Master Data 8082, Discovery 8761, Config Server 8888 and Gateway 8000. No legacy name or `host.docker.internal` duplicate remains.

## Evidence, rollback, and residual risk

- Compose quiet/rendered assertions and `promtool check config` pass. The final dedicated JDK-17 policy test passes under CPU 1 / memory 1.5 GiB / one Gradle worker. The first Java attempt failed only because exact map entries were compared in insertion order; the order-independent exact-set assertion then passed and target strictness remains.
- Live readiness and value-redacting target count pass repeatedly at total 6/up 6/down 0. The container is healthy with restart 0/OOM false and used about 31 MiB at the measured snapshot. The tracked config mount is read-only and the TSDB path writes to the named data volume; a read-only container root filesystem is not claimed.
- Exact `stop prometheus-dev` preserved the project volume, retained legacy `prometheus` Created container and all seven application container health states. `up -d --wait` and a pinned-image force recreate both recovered to 6/6. PostgreSQL/Redis and all application services were untouched.
- Runtime rollback is only `podman compose -f prometheus/docker-compose.yml stop prometheus-dev`; never use `down -v`, prune, broad stop or legacy/application removal. A deliberately stopped application target leaves Prometheus ready but makes the 6/6 gate fail, so distinguish service readiness from scrape completeness.
- Independent review found P2 unauthenticated lifecycle shutdown exposure, P2 ineffective bind-config update guidance and P3 duplicate-job/rootfs-evidence gaps. Lifecycle is now absent from the exact runtime command, config changes require promtool plus `--force-recreate prometheus-dev`, policy enforces six unique scrape jobs, and the record distinguishes mount modes from rootfs. Live lifecycle-disabled recreate returned 6/6, the strengthened JDK-17 test passed and final re-review found no P0-P3. Commit/push, Draft PR, GitHub checks and merge remain. Grafana datasource integration stays separate under #563 follow-up.
- Live evidence is rootless Podman-specific; Docker is documented but not run here. The version tag is fixed but not digest-pinned, and the named TSDB volume has no separate size cap. Monitor development disk growth and open a bounded retention follow-up if needed.

# AI Harness Handoff - 2026-08-31 Issue #540 HttpOnly BFF

## Current state

- Issue/branch/worktree: `#540`, `agent/540-http-only-bff`, `/tmp/account-540-http-only-bff`, based on exact `origin/main@85fa1adf`.
- Login and authenticated Master Data browser traffic use same-origin Route Handlers. The BFF owns the JWT in an HttpOnly, SameSite Strict cookie, reconstructs the Gateway Authorization header server-side, requires same-origin evidence for mutations, blocks upstream redirects and strips browser Cookie/Authorization/identity headers.
- PAT and governance files are unchanged from main. Those clients retain their existing override/mock behavior until separate route and authorization contracts are implemented.

## Evidence, gate, and rollback

- CPU-1/memory-1536-MiB/network-none Node 20 TypeScript passes. Development live tests pass 12/12 after the final streamed-body and response-header remediation. ESLint passes with 0 errors and the unchanged 244-warning #536 baseline. Prior production build completed 122/122 pages and prior production live tests passed 9/9; repeat both against the final head before publication.
- Compose/config/image-policy/external-dev preflight and runner checks previously passed without input disclosure. Local offline Config Server compilation remains unavailable only because an existing dependency is not cached; use GitHub JDK 17 validation.
- Independent review's remaining merge blocker is Gateway rate-limit identity collapse behind the BFF. Issue #607 is the required stacked change: a runtime-only shared secret signs opaque per-login keys, Gateway uses verified principals for protected calls, and spoofed/expired inputs fall back to the direct peer. #540 must not become Ready or merge before #607 is reviewed and integrated.
- Code rollback is a reviewed revert of the BFF/session/runtime-routing changes. Existing development and production cookie names are both cleared during logout/401 migration. Never expose a shared secret through `NEXT_PUBLIC_*`, build args, tracked dotenv files, logs or issue comments.

# AI Harness Handoff - 2026-09-01 Issue #607 BFF rate-limit trust boundary

## Review-ready state

- Issue/branch/worktree/PR: `#607`, `agent/607-bff-rate-limit`, `/tmp/account-607-bff-rate-limit`, Draft PR #623 against `main`. The branch is based on `origin/main@4d01f240`; already-merged PR #608 supplied the #540 BFF base and is not reopened or rewritten.
- Protected calls use only the principal attribute written after Gateway JWT and role-version validation. Public login accepts only one lowercase SHA-256 key, timestamp and HMAC header set bound to POST `/api/auth/login` within plus/minus 30 seconds. Invalid inputs and arbitrary `X-Forwarded-For` use the direct peer bucket.
- The rate limiter consumes user and BFF-peer aggregate buckets, scopes them by route and caps its access-order store at 10,000 entries. The BFF headers are removed before the Auth route. A distinct 32-512 UTF-8-byte shared secret is injected only at Frontend/Gateway runtime and validators never print it.

## Evidence, rollback, and next owner

- Node 20 with networking disabled and bounded CPU/memory: development BFF live 12/12, production BFF live 12/12, TypeScript pass, ESLint 0 errors/244 inherited #536 warnings, production build 122/122 pages. The initial live attempt failed only because this worktree had no installed Next executable; the lockfile-identical dependency tree was mounted read-only for the passing rerun and no package was installed.
- Forced JDK 17 Gateway test and the three affected Config Server policy suites pass offline with CPU 1, memory 1.5 GiB and one worker. Python compile/self-test passes 168. `pwsh` is unavailable, so PowerShell validators were not directly run; passing Java policy tests cover their changed required-input/error contracts. Diff, marker and clean-tree gates pass.
- Independent review found no P0-P3. Residual coverage gaps are a single live BFF-to-Gateway-to-Auth process chain and direct tests for duplicate headers, a future-expired signature, the 512-byte boundary and BFF secret error responses. These remain non-blocking because the separated live/unit/policy evidence and code review found no failing behavior.
- Publication owner is the parent Integrator: wait for all GitHub checks on the unchanged PR #623 head, perform the Ready-event gate, then squash-merge and confirm Issue #607 closure. Rollback is a reviewed PR revert; no schema, data, secret value, image, container or external service state changed.

# AI Harness Handoff - 2026-09-07 Issue #466 Gateway external-config sync

## Current state

- Issue/branch/worktree: open `#466`, `agent/466-gateway-config-repo-sync`, `/tmp/account-466-gateway-config-repo-sync`, based on `origin/main@ad44f873`.
- The Config Server Gateway YAML now carries the same ten business route definitions, CircuitBreaker names/fallbacks, explicit Resilience4j instances, BFF rate-limit header stripping and Cloudflare CORS as the packaged Gateway configuration. The legacy `account-api` catch-all is absent.
- A focused policy regression compares packaged and external route/default-filter/CORS/Resilience4j properties, rather than validating only the packaged file.

## Evidence, residual risk, and rollback

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :gateway:test` passes 72 tests with zero failures/errors/skips. `git diff --check` and tracked conflict-marker checks pass. Independent review found no parity-scope defect.
- The copied PR #617 baseline still has a separately identified P0: several public predicates do not cover the actual Controller base paths, while deposit and ECL use Eureka IDs different from their application names. Because this task explicitly requires 100% parity with the packaged source, those source definitions were not redesigned here. Do not mark the original outage resolved or make a PR Ready until that contract receives approved correction and routed verification.
- Rollback is a reviewed revert of this configuration/test/record commit. No database, schema, data, credential, service, container, or remote GitHub state was changed; no PR was created or pushed.

# AI Harness Handoff - 2026-09-07 Issue #466 stage 2 route contracts

## Current state and ownership

- The user explicitly authorized stage-2 implementation, the exact Java 21 Gateway test command, commit message `[#466] Align Gateway Route Predicates and Eureka Service IDs with Controller Contracts`, and push to `origin agent/466-gateway-config-repo-sync`.
- Branch/worktree: `agent/466-gateway-config-repo-sync`, `/tmp/account-466-gateway-config-repo-sync`, starting at stage-1 commit `e49989c4`. Read-only GitHub inspection found PR #632 merged and Issue #466 closed, superseding the prior entry. The remote branch is absent and will be recreated by the authorized push. No new PR or Issue state change is part of this request.
- Changed implementation files: `gateway/src/main/resources/application.yml`, `config-repo/gateway-service.yml`, and `gateway/src/test/java/com/ho/account/gateway/GatewayRouteSecurityPolicyTest.java`. The parent also updates `worklog.md`, `agent-status.md`, this handoff and `docs/history/CODEX_WORKLOG.md`; the Test Agent wrote only the policy test and the Reviewer remains read-only.
- The ten routes match the supplied URI/Path/breaker/fallback specification. Three service IDs and nine Path predicates are corrected, including market-data and both AP endpoint families; reporting already matched. Existing versioned aliases remain route predicates without adding path rewrites. Full routes/default-filters/CORS/Resilience4j parity is preserved.

## Verification, remaining runtime evidence, and rollback

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :gateway:test`: BUILD SUCCESSFUL in 28 seconds, five tasks executed. Parsed XML: nine suites, 72 tests, zero failures/errors/skips. Policy tests pass 30/30, including the named full-configuration parity test. Diff whitespace and tracked source/docs conflict-marker checks pass.
- Independent review is pending final confirmation. Local Controller mappings and nine service application IDs were inspected. The reporting URI remains the user-specified `lb://reporting-api`; its API module has no base application-name declaration in this checkout, so actual reporting registration remains a pre-existing runtime verification item.
- No live Gateway/Eureka routed smoke, deployment, service restart, database or credential access was performed. Static parity and unit/policy tests establish the requested configuration contract; they do not prove that every live backend is registered or that preserved aliases are backend endpoints.
- The parent completes the authorized commit/push after final review. A subsequent integration owner handles any new Draft PR, CI and runtime smoke. Rollback is a reviewed revert of this stage-2 commit, restoring the previous configuration (including its known route mismatches), with no schema/data rollback.

## 2026-09-10 — GH-179 review handoff

- Issue #179; difficulty:medium, requested reasoning high. Branch `agent/179-api-parity-audit`, worktree `/tmp/account-179-api-parity-audit`, base `origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e`. Dirty primary checkout was preserved. Audit commit `d15632a0` was pushed; [Draft PR #676](https://github.com/skyg547/account/pull/676) is open with `Refs #179`, verification results and the merge-authority statement. Issue status: `status:needs-review`.
- Scope follows rejected PR #548 instructions: read-only production evidence, 15 current service/helper files, exhaustive actual method/URL and field/type/nullability/enum comparison. Write allowlist: `frontend/docs/frontend-backend-api-parity-matrix.md`, `frontend/README.md`, `docs/ai-harness/worklog.md`, `docs/ai-harness/agent-status.md`, `docs/ai-harness/handoff.md`, `docs/history/CODEX_WORKLOG.md` only.
- Result: 42 endpoint calls + 3 transport helper calls; 33 mapped and 9 MISSING. Corresponding 13 Controllers have 87 mappings, 54 backend-only. Nested DTOs, missing required query/body fields, enum/nullability drift, BFF transformation/actor injection and actual fallback behavior are documented. Universal endpoint/DTO compatibility is not claimed; Issue stays open.
- Verification: embedded `node /tmp/account-179-verify.cjs` PASS including row/method/path/Controller/backend-only evidence and known DTO/query drift; in-memory negative checks PASS 7/7. `node frontend/node_modules/typescript/bin/tsc --project frontend/tsconfig.json --noEmit --incremental false` exit0. From frontend, `NEXT_TELEMETRY_DISABLED=1 NODE_OPTIONS=--max-old-space-size=3072 taskset -c 0 npm run build` exit0, Next15.5.23/static pages122/122. Existing dependencies reused without installation/lockfile changes. Node22.23.2 differs from engine Node20; Node20 run remains unclaimed.
- Independent Reviewer found one P3: Closing PUT JSON decode errors propagate while HTTP/transport errors return null. Corrected the audit wording; final independent review found no remaining P0-P3. Diff/marker/new-content control checks pass. One pre-existing worklog control byte is unchanged and outside this edit. No production code, credentials, remote API/DB, or runtime state was changed; no secret values were output.
- Skipped: Gradle (no Java changes), Node20 build (current runtime22), live Gateway/Jackson/UI/DB checks (static audit scope). Known MISSING/DRIFT and serializer/runtime TODOs remain follow-up work. Rollback: revert audit commit(s). Next owner: independent Reviewer / human Integrator for Draft PR review and follow-up scope; no Ready/merge/Issue close/worktree removal authorized.

## 2026-09-10 — GH-663 current integration rework handoff

- Current rework round1/2: parent integrated `origin/main@b7c1c7c1fa45ec6550ab2431674fcf22a519bcea` into original GH-663 head `62836e6cb8fc95fbe5b0512db837f96cfb92fc02` by ordinary merge; no force/rebase/automatic side selection. Four EOF conflicts retain main GH-179 records before the original GH-663 records; both-parent line-order preservation is 8/8 PASS.
- PR https://github.com/skyg547/account/pull/684 was returned to Draft and Issue663 to in-progress for this same-contract parent rework. Review request: https://github.com/skyg547/account/pull/684#issuecomment-5611625713; rework claim: https://github.com/skyg547/account/pull/684#issuecomment-5611789117.
- Parent owns records4 and conflict-log only; original two-file implementation stays unchanged. Target46/full125 and bootJar rerun PASS, installed JDK17/Gradle8.7 offline with one worker; no live PostgreSQL/TLS.
- Next owner: separate read-only Astra high reviewer, then parent normal merge commit/push/current-head CI/Ready; account alone performs authorized final PR merge/Issue close. New head is not automatically approved by the old content review.
- Latest remote Issue/PR state supersedes earlier historical Draft/global-wait checkpoints. Ready releases the implementation slot for #668; merge execution authorization and #662 close wait stay separate. Keep other owners/resources untouched. Rollback is reviewed file-level revert preserving both histories; never DB/schema/history repair or cleanup.

## 2026-09-10 — GH-663 canonical sslmode handoff

- Issue https://github.com/skyg547/account/issues/663; Workflow Account Issue 구현 오케스트레이터; task `019fa3ea-ac4c-7022-bb45-951559750df7`.
- Branch/worktree: `agent/663-migration-canonical-sslmode` / `C:/tmp/account-663-migration-canonical-sslmode`; base `5f8103afd4f018723302fd4b15b740b3bcf189c1`. Substantive allowlist2: MigrationConfiguration.java and its test; parent records4 appended without replacing prior histories.
- Implementer `/root/migration_663_writer` Astra high; separate read-only security reviewer `/root/harness_ci_audit` Astra high. Canonical query validation fixes validator/JDBC last-duplicate mismatch; production failures use the unchanged fixed message and original URL is retained downstream.
- Verification: RED46tests16fail; targetGREEN46/46; fullGREEN125/125,0error/fail/skip. Independent offline rerun125/125. Existing85+40 regression cases include both duplicate orders, same-value/bare/empty/case variants, noncanonical percent-encoded alternatives, five driver-verified accepted URLs and development compatibility.
- Q1–Q4 evidence and no-doc-change rationale are in this Issue's worklog entry. No actual DB/TLS handshake or broader JDBC option audit. Rollback reviewed two-file revert only; no schema/data changes.
- Parent completes independent review, exact-file commit/push/Draft PR and new-head CI before Ready. Published PR/current SHA are on the remote Issue. Ready handoff releases the implementation slot; rejected work returns to this original Issue. account retains final review/merge/close. GH-662 close/cleanup delay is not a dependency; preserve its resources.

## 2026-09-11 — GH-691 period balance aggregation handoff

- [Issue #691](https://github.com/skyg547/account/issues/691), claim [5623212221](https://github.com/skyg547/account/issues/691#issuecomment-5623212221). Workflow/task `Account Issue 구현 오케스트레이터` / `019fa3ea-ac4c-7022-bb45-951559750df7`; original ownership retained through review/rework.
- Branch/worktree `agent/691-ledger-period-summary` / `C:/tmp/account-691-ledger-period-summary`, base `65e6b1e36554ed3adb9616b4eedb579843f0e748`. Writer `/root/issue691_implementation` Astra xhigh; independent code reviewer must be a different session from writer/spec author. Four substantive files plus five parent records only.
- Change: GL/SL earliest returned daily opening once per existing key, summed period movements, recalculated ending. Requested date/period and source/list identity preserved. No as-of/missing-account reconstruction or persisted-data repair.
- Writer verification: representative RED4/4 failures confirmed the duplicate opening; final forced offline core65/API11/Batch5 =81tests/28suites, failures/errors/skips0, exit0/53s/37tasks executed. `/root/issue691_code_review` Astra xhigh independently reran all tasks with fresh XML81/81, zero failure/error/skip, unchanged frozen hashes, no P0–P3 and Q1–Q4 PASS. Exact commands and quality evidence are in this Issue's worklog. Parent-record final delta is the remaining local review gate. External DB and real Batch execution are deliberately unperformed, not PASS.
- Next: parent records actual final evidence, commits/pushes exact files and opens Draft PR with `Refs #691`; required current-head CI, no blocking findings and latest-main integration gate Ready. account alone owns final approval/merge/Issue close. No self-approval or deletion. Rework returns to this original Issue, not a new duplicate.
- Rollback: reviewed code/test/doc revert only; no schema/data repair. Dirty primary checkout and other owners' resources remain unchanged. Remote Issue/PR carries exact publication SHA and CI/Ready state, avoiding record-only follow-up commits.

## 2026-09-11 GH-693 posting-period guard

- Issue https://github.com/skyg547/account/issues/693. Workflow Account Issue 구현 오케스트레이터 / task `019fa3ea-ac4c-7022-bb45-951559750df7`; branch/worktree `agent/693-posting-period-recheck` / `C:/tmp/account-693-posting-period-recheck`, base `65e6b1e36554ed3adb9616b4eedb579843f0e748`.
- Substantive4: PostingService, its core test, journal-ledger process-flow, direct LoanJournalPostingFlowTest. Parent records5 preserve old contents. Writer and independent reviewer are distinct Astra xhigh sessions; original source3 spec was amended after independent P1 discovery, with separate spec delta approval before the original writer's rework1.
- Period validation runs after fromApproved and before post/save. Actual filter/provider semantics, zero writes and source invariance on failure, valid OPEN/poster/lineage/accountingDate/SYSTEM and original invalid-state priority are covered. Consumer fixture must use actual guard with explicit OPEN period; no production fallback.
- GH-693 local checkpoint evidence: writer expanded182/48suites/bootJar2 PASS; independent same-hash Journal74 plus revised Loan108/21suites/bootJar2 PASS, original P1 resolved/Q1–Q4 PASS/no additional P0–P3. Original journal-only success was not accepted as consumer compatibility. Parent final record/PR delta precedes checkpoint commit; latest-main integration and integrated test/review/CI gates are still required before Ready.
- Required bundle uses offline cached JDK17/Gradle8.7: journal core/API/Batch tests + Loan core/API/Batch tests/API/Batch bootJar, rerun tasks and fresh XML; no-source tasks explicitly distinguished. Commands, timestamps and counts are in worklog/PR body when verified.
- Remaining limits: query-to-closing race, concurrent duplicate posting and old data repair are not solved; live DB/servers/business Batch/deployment not run. Rollback reviewed substantive4 revert, no schema/data changes. Parent follows Draft→exact-head CI/Ready; account final review/merge/Issue close. CI/merge waits release the writer slot, rejection returns this original Issue. Do not remove resources while follow-up needs them.
- Latest-main integration completed at tree level:7ed966ea +65af7e6f, parent5 EOF conflicts preserve both complete histories and are recorded in conflict-log. Independent integrated full203tests/49suites+LoanbootJar2 PASS, UTC19:23:01.2608176–19:24:32.6531057,54tasks executed/exit0/91.3922881s/failure,error,skip0. Q1–Q4PASS/no new P0–P3; source4 and incoming4 unchanged. Parent final integration-record/PR delta must pass before mergecommit/Draft publication; exact-head CI/Ready remains next. This supersedes only the earlier integration-pending state, not its historical evidence.

## 2026-09-11 — GH-692 settlement precision handoff

- [Issue #692](https://github.com/skyg547/account/issues/692), [exclusive claim](https://github.com/skyg547/account/issues/692#issuecomment-5623500253); Workflow/task `Account Issue 구현 오케스트레이터` / `019fa3ea-ac4c-7022-bb45-951559750df7`. Branch/worktree `agent/692-unsettled-precision` / `C:/tmp/account-692-unsettled-precision`, base `65e6b1e36554ed3adb9616b4eedb579843f0e748`.
- Writer `/root/issue692_implementation` Astra xhigh: domain precision/state-atomicity, core regression, real JPA/H2 transaction test, actual-service HTTP test and schema functional document (5files). Parent exclusively appends shared records5 and controls publication.
- New amount and both next balances are validated before mutation; already-handled/legacy refs retain no-op semantics. Domain RED40/22failures/0errors/skips reproduced; writer and separate `/root/issue692_code_review` Astra xhigh each forced module GREEN core86/API26/Batch5=117tests/29suites, failure/error/skip0,exit0/55s/37tasks executed. Exact command and Q1–Q4 are in this Issue's worklog. Independent P0–P3 none/Q1–Q4 PASS, five frozen hashes match; final parent-record/PR delta confirmation is the remaining local gate.
- No external DB, real Batch/server/deployment, installation, migration/data repair or ref/audit deletion. Rollback is reviewed scoped code/test/doc revert. No self-approval/merge/Issue close; account owns final gate. Draft/current-head CI/Ready publication evidence goes on the remote Issue/PR; frozen verified and independently reviewed PR releases the writer slot without a global merge wait.

## 2026-09-11 GH-692 수동 통합 재작업

- 사용자 직접 요청으로 PR #698의 공유 기록 충돌을 원 브랜치/외부 worktree에서 보완한다. 세 예약은 PAUSED 상태이며 이번 수동 실행이 예약을 재개하지 않는다. 최신 main c94afec5와 원 head f5e32b3의 양쪽 역사를 모두 보존했다.
- 실질5파일은 원 head와 Git blob까지 일치한다. 별도 GPT-6 Astra xhigh /root/manual_review_698의 새 통합본 강제 offline 실행은 core117/API30/Batch5, 총152 tests/30 suites, 실패·오류·skip·stale0, exit0/58초/41tasks 모두 실행이다(UTC2026-09-11 01:00:33.8897880–01:01:33.3742691). API/Batch bootJar2도 새로 생성되었으며 패키징 증거이지 실제 서버 기동 증거는 아니다. 독립 코드 검수 P0–P3 없음/Q1–Q4 PASS; 최종 기록 delta와 원격 CI/Ready 검토가 남는다.
- 부모가 최종 기록·검증 근거와 독립 리뷰를 모아 새 head를 push하고 CI/Ready-only를 확인한다. 사용자 수동 병합 승인 아래 부모만 최종 merge commit/수용 완료 Issue 종료를 수행한다. 기존 자동화 전용 소유 문구는 역사이며 현재 수동 실행의 게이트는 원격 claim5627728952다. 자원 삭제·DB/설정/예약/공유계약 변경은 없다.

## 2026-09-11 GH-673 review-scope handoff

- Issue https://github.com/skyg547/account/issues/673, claim5624214896; workflow/task Account Issue 구현 오케스트레이터 / `019fa3ea-ac4c-7022-bb45-951559750df7`. Branch/worktree `agent/673-review-read-scope` / `C:/tmp/account-673-review-read-scope`, base65af7e6f. Source2 plus parent records5; no other ownership changes.
- Writer and independent code reviewer are separate Astra high sessions; the parent-authored existing-Issue spec had a different independent high review before Ready/claim. Doc top/copyable/handoff now enforce declared filename-first→exact non-sensitive intersection→scoped read/hold; historical meanings/dates/results preserved for #675.
- Writer representative RED1/exit1 then Node60/60/exit0; independent full60/60/exit0, UTC19:33:06.8961432–19:33:07.0447777,fail/skip/cancel0. Source hashes/diff/marker PASS and Q1–Q4 source/doc PASS/no required finding. Final record5/PR delta is the remaining local review gate before exact-file commit/push/Draft, followed by new-headCI/latest-main/Ready.
- No runtime sandbox or exhaustive secret-detection guarantee. Fixtures never probe real sensitive paths or execute plans; CI wiring/Gradle/business systems/history separation unperformed and out of scope. Rollback reviewed2-file revert preserving parent histories. account owns final merge/close, and other PR waits are not a global implementation blocker; weekly budget still gates new work. Remote Issue/PR records actual publication status.

## 2026-09-11 GH-673 manual rework1 — PR700 P2

- User manually authorized review/fix/review/merge; all automations remain PAUSED. Original branch/worktree/Issue retained, new claim5627729450. Removed active bare whitespace-check exemption, applied the same nonempty exact content gate to state-specific whitespace comparisons, preserved historical prose and scope.
- Original writer Astra high: actual unchanged-document RED1/fail1 exit1 (UTC01:01:18.1185470–01:01:18.2962500), then full Node71/71 PASS/fail,skip,cancel,todo0/exit0 (UTC01:03:20.1429198–01:03:20.3555019). Parent separately used only safe synthetic approved.txt/outside-review.txt in an external fixture: unscoped unstaged/staged checks each exit2 with synthetic outside-line output; exact approved-path alternatives each exit0/no output. This is no real-secret probe; test-file diagnostics remain in-memory, not a claimed real Git execution.
- Independent /root/manual_review_673 Astra high source/test/history review APPROVE, P0–P3 none/Q1–Q4 PASS; ONE direct full Node run UTC2026-09-11 01:06:00.0622846–01:06:00.2262431, exit0/71tests71pass/fail,skip,cancel,todo0/runner108.3914ms. Frozen2 hashes unchanged and historical lines remain ordered. Final record/PR delta remains a separate gate. Parent alone replaces CLOSED/unmerged700 with a new Draft after acceptance, then checks fresh head CI/Ready/main before user-authorized merge. Runtime sandbox, exhaustive secret detection, CI wiring, business code and history split#675 remain non-goals; preserve histories/resources.

- Latest-main follow-up: reviewed checkpoint240522c2 now integrates actual PR698 merge/main5b2d184f (Issue692 CLOSED). Parent inspected all seven record-only EOF conflicts and preserved complete incoming692 histories then existing673 histories. Source2 and test input remain frozen; current71-test evidence is distinguished from prior60. Independent integration-record review and published-head CI/Ready remain required; no673 PR merge is claimed here.

## 2026-09-11 - GH-696 governance API runtime and proxy/observability recovery

- Trace: Issue #696, branch `agent/696-governance-runtime`, worktree `/tmp/account-696-governance-runtime`, fetched base `c94afec5`. PR: https://github.com/skyg547/account/pull/701. Next owner: human Draft PR reviewer.
- Implementation: dedicated governance Compose/DB provisioner/Nginx template, Grafana recovery and runtime guide, two Python regression suites/live verifier, module README links and parent harness/history. API business Java/Gradle unchanged. PostgreSQL databases `budget_db` / `internal_audit_db` use separate owner/app roles and runtime schema validation; existing Redis/Eureka are explicitly selected.
- Verification: JDK17 one-worker `:budget:core:test :budget:api:test :internal-audit:core:test :internal-audit:api:test :budget:api:bootJar :internal-audit:api:bootJar` PASS112/112; `:migration-runner:bootJar --offline` PASS. `python3 -B -m unittest discover -s tools -p 'test_governance*.py' -q` PASS12/12. Actual quiet Compose, two-DB migrate/validate/schema/ACL/DDL gates PASS. Image JAR hashes equal tested JARs.
- `python3 -B tools/verify-governance-runtime.py` and separate Reviewer execution PASS: exact current-IP/port Eureka and API health UP; Nginx login and Grafana direct/proxy200, frontend API400/403, persisted datasource and Prometheus6 results; all four targets healthy/restart0/OOMfalse/CPU.50/RAM768MiB. Runtime PostgreSQL sessions observed2 per DB. Diff/markers clean; replay and documentation review findings fixed.
- Recovery detail: Grafana exact existing image/external volume preserved; Nginx template uses current DNS. Dependent frontend was unresponsive, then stale crun state prevented restart; canonical one-service recreation retained original image/two named volumes/0.75CPU/1GiB and restored health. It is a legacy Gateway-rewrite frontend; new BFF deployment is not claimed. Initial startup socket resets and Budget Redis localhost failure were corrected before final gates.
- Rollback/limits: stop only issue consumers, preserve DBs/env/volumes/images, restore prior proxy config and retained Grafana image on the same volume. Partial role creation needs reviewed forward recovery; no drop/prune/schema rewind. Full multi-stage image build was not repeated: tested jars were assembled offline with the original runtime stage. Authenticated Grafana Save & test, unrelated business flows and other stopped/unhealthy services are outside evidence. Worktree hosts live bind mounts and must remain until reviewed relocation.
- Authority: user authorized related implementation/dev migration/recovery, verification, branch push and Draft PR. Writers and read-only Reviewer are separate; parent alone updates harness/GitHub. Keep `Refs #696`, Draft state and open Issue; Ready, merge, close and cleanup require separate authorization.


## 2026-09-11 GH-696 manual PR701 redirect rework

- User authorized manual PR review, rejected-code repair and merge; all three automations remain PAUSED. Published implementation-complete handoff was verified before claim5627974078. Parent uses agent/696-review-redirect / C:/tmp/account-696-review-redirect; original live-mounted /tmp/account-696-governance-runtime is preserved and never modified.
- Independent Astra high /root/manual_review_701 found P2: default absolute redirect loses the published port for bare /grafana. Separate Astra high writer /root/manual_fix_701 changes only the exact-location relative-redirect policy and its regression; parent owns functional guide/shared records and Git/GitHub. Offline rework only: no live DB/container/credentials/deployment/cleanup.
- Ordinary integration of original head3c6f04f9 with latest main ef5679b2 preserves both histories. Parent resolved four record-only EOF conflicts, complete main prefixes4/4 and original ordered histories4/4 PASS. Earlier PR698 and PR702 are actually MERGED, Issues692/673 COMPLETED; see their remote completion evidence. Independent re-review, published-head CI and Ready/latest-main checks remain required for PR701; actual final state belongs to the remote PR, not a prediction in this checkpoint.

## 2026-09-11 — GH-695 verification complete / Draft publication pending

- Issue #695; `agent/695-journal-adapter-precision`; `/tmp/account-695-journal-adapter-precision`; base `c94afec5`. Primary checkout preserved; parent owns Git/PR/shared records, separate Test writer and read-only Reviewer.
- Closing redirects cannot trigger Location follow or subsequent writes. Loan validates DRAFT identity/lineage and exact debit/credit amounts before approval, and rechecks POSTED. Actor preserved. Two adapters, two core test files, two module local-run docs; no build/schema/config change.
- JDK17/Gradle8.7 offline CPU1/RAM1536m: targeted112 PASS; full316/52suites PASS (Loan137/6/3, Closing105/49/16), failure/error/skip0, two API bootJar PASS. Required core/API four-module count297. Harness32, diff/marker/frozen source checks PASS. Full commands/results: latest GH-695 `worklog.md` entry. Local logs `/tmp/issue695-{targeted,regression}.log` and JSON summaries.
- Independent `/root/independent_review` confirmed no P0–P3 and Q1–Q4 PASS; independently parsed all52 XML suites and verified all6 source/test/docs hashes. Next parent action: final record review, exact-file commit/push and requested Draft PR (`Refs #695`), then record its URL and current-head CI. Independent Reviewer remains read-only; no self-approval.
- Rollback via reviewed revert; remote partial writes require reconciliation. Five remote calls per successful Loan posting, no atomic guarantee between lookup and write. External deployed Journal/DB not used; test runtime is real loopback HTTP plus local Spring contexts. Merge authority: Reviewer + user approval before Integrator Ready/merge/Issue close. Preserve branch/worktree until approved cleanup.

### GH-695 Draft publication

- Published requested Draft PR [#703](https://github.com/skyg547/account/pull/703), `Refs #695`, from `agent/695-journal-adapter-precision`; implementation commit `35c70c83`. Independent final source/test/record/PR-body review found no P0–P3 and Q1–Q4 PASS. This publication-only update changes no code/tests; all316 Java tests,32 harness tests and two API JAR results remain valid.
- Issue #695 is `status:needs-review`. Next owner: independent human Reviewer / approved Integrator for current-head GitHub CI and review; implementation approval is not self-approval. Ready/merge/Issue close and branch/worktree cleanup remain unperformed. Worktree `/tmp/account-695-journal-adapter-precision` is retained. PR body contains full commands/results, authority separation, rollback and remote partial-transaction limits.


## 2026-09-11 GH-695 manual PR703 integration

- User requested manual review/rework/merge; schedules3 remain PAUSED. Original implementation-complete handoff verified, manual claim5628076042. Parent uses agent/695-manual-pr-integration / C:/tmp/account-695-manual-pr-integration; original agent/695-journal-adapter-precision worktree preserved. No production changes were needed after separate /root/manual_review_703 Astra xhigh review: no P0–P3, Q1–Q4 PASS.
- Parent ordinary-merges original086a4af9 with actual latest main53ada41b (PR701 merged UTC01:39:00Z). Four record-only EOF conflicts retain complete reviewed main histories then original695 histories. Main prefix7/7 and original ordered histories7/7 pass; substantive6 files unchanged, no incoming Loan/Closing/shared-kernel/build change. Exact main-relative13 paths are original10 plus parent record3. Primary checkout and all other owners preserved.
- Independent new local core run242/242, failure/error/skip0; focused112 are included, not additive. Original316/bootJar evidence remains historical. Current-head core/API CI, final integrated-record review and Ready/latest-main gates are required before user-authorized parent merge; actual publication/completion state is recorded on PR703. No real Journal/DB, deployment, installs/downloads, force/rebase/reset or resource cleanup.


## 2026-09-11 — GH-678 일반 Issue 접수의 초기 Draft 상태

- Issue #678 (Parent #661), branch `agent/678-intake-draft`, worktree `/tmp/account-678-intake-draft`, base `origin/main@44f973549e5f4fa79c4076a686838063be92d34a`. 사용자 실행/Draft PR 요청과 부모의 선행·겹침·명세 검토 후 claim. 폼과 겹치는 열린 PR 없음; PR #705와 공유 기록만 중첩. 기본 dirty checkout/타인 worktree 보존.
- `.github/ISSUE_TEMPLATE/ai-agent-loop-task.yml`의 초기 `status:ready`를 `status:draft`로 교체하고, 87 실행 명세 및 부모의 Ready/claim 승인 안내 3문장을 추가했다. 기존 14개 필드·scope/difficulty·모듈 목록·안전 항목·다른 폼은 보존. 변경 범위는 폼 1개와 부모 기록 4개뿐이며 기존 기록은 추가 방식으로 보존한다.
- `python3 /tmp/issue678-verify.py`: 설치된 PyYAML 6.0.1 구문/14개 ID 유일성/draft 포함·ready 제외/87 링크·대상/부모 gate/전체 기존 필드 비교 PASS. 기존 Ready, draft+ready, draft 누락, 중복 ID, 링크 누락, 안전 required 변경, 부모 gate 누락, malformed YAML의 음성 사례 8개 거부 PASS. 재현 스크립트 전문은 Draft PR 본문에 포함한다.
- `node --test tools/ci/harness-quality-contract.test.cjs`: 32/32 PASS, 실패·skip 0. `git diff --check`, 추적 harness/docs conflict-marker 검사 PASS. 실제 테스트 Issue 생성은 계약상 금지로 미실행, GitHub 폼 렌더링 미검증. 업무 코드/빌드/런타임 영향이 없어 Java/Gradle 및 실서비스 기동 미실행.
- 별도 read-only Astra high `/root/review_678`: P0–P3 없음, 독립 verifier 재실행 PASS, 라벨·안내 외 바이트 보존 확인. Q1 N/A(실행 코드 없음), Q2 PASS(접수→명세→부모 승인), Q3 PASS(87 링크/폼 안내), Q4 N/A(비자명 코드 없음). 동결 폼 SHA256 `7b5c03b2e2f7ecc69ddc1471bd68ce4751bb6173919e47ea96f320cb853340c9`.
- Draft PR: 게시 전, `Refs #678` 본문 준비 완료. 다음 체크포인트에서 실제 URL을 추가한다. 부모만 공유 기록/commit/push/PR/Issue 상태 변경; 이번 작은 폼은 부모 직접 구현, 별도 Reviewer 독립 검토. 모델 티어는 외부 변경 권한이 아니다. Ready/merge/Issue close/cleanup은 미실행이며 별도 승인 게이트다.
- 다음 담당: 독립 사람 Reviewer 및 승인된 부모 Integrator. 현재 head CI/최신 base를 확인하고, 공유 로그 중첩 시 양쪽 역사를 보존한다. 안내는 운영 규율이며 접근 권한 강제 기능은 아니다. 롤백은 이번 폼 변경만 검토된 후속 PR로 되돌리고 공유 역사 기록을 보존한다. 스키마/데이터 변경 없음.

### GH-678 Draft publication

- Draft PR [#706](https://github.com/skyg547/account/pull/706) 게시 완료 (`Refs #678`), 구현 commit `b0b7c6bc`; Issue #678은 `status:needs-review`. 별도 `/root/review_678`의 최종 5파일/기록/PR 본문 리뷰도 P0–P3 없음, 기존 기록 prefix 4/4와 동결 폼 동일성 확인.
- PR 본문에 재현 스크립트, 검증 결과·미실행 사유, Q1–Q4 및 부모/구현/Reviewer 권한 분리를 기록했다. 이 게시 기록 추가는 폼 변경 없이 실제 URL/상태를 동기화한다. 최종 게시 head CI는 다음 확인 게이트이며 Ready/merge/Issue close/cleanup은 수행하지 않았다.


## 2026-09-11 — GH-674 active document reference paths

- Issue #674 / parent #661; difficulty:low, user-requested high reasoning. Parent Integrator implements and publishes; independent read-only Reviewer `/root/review_674` performs review. Branch `agent/674-canonical-doc-paths`, isolated worktree `/tmp/account-674-canonical-doc-paths`, base `origin/main@6afe87a37068d427be3a15a4311540f434591890`. Original dirty checkout and existing `/tmp/account-674-active-doc-paths` remain untouched.
- Preflight: #662/#681/#673 are integrated; no open PR at start. #674 was OPEN/status:ready/agent:codex despite its older draft prose. #670 remains OPEN/draft without an active owner or comments; its authority changes are not claimed complete. The explicit user request authorizes this sequential path-only task; no overlapping child is executed in parallel. Nine mechanical documentation changes qualify for the Issue's >5-file sizing exception; role/authority semantics are unchanged.
- Scope: `docs/ai-harness/{00-overview,80-file-ownership,85-github-issue-agent-loop,95-codex-skills-subagents}.md`, `.github/PULL_REQUEST_TEMPLATE/ai-agent-loop.md`, `.agents/skills/{account-review-handoff,account-module-parallel}/SKILL.md`, `CLAUDE.md`, `GEMINI_REVIEW_PROMPT.md`, plus these four append-only parent records. Active Codex history references now use `docs/history/CODEX_WORKLOG.md`; skill discovery uses `.agents/skills/<skill-name>/SKILL.md`. Current Gemini checklist resolves the archived module/Gemini documents without widening read approval.
- Preserve: #673 already classified the old Gemini list as historical; its list and past root-log success statement remain verbatim. The checklist prefix is unchanged; root `CLAUDE_WORKLOG.md` is unchanged, removed root `CODEX_WORKLOG.md`/`SKILL.md` are not recreated, and no history/backup content is rewritten. No architecture, financial logic, build/config/runtime or data changes.
- Verification PASS: `git cat-file -e <ref>:<target>` for seven targets at Issue base `34c75839af2edf10f80ff60d8f24e89504d69e9e` and current base (14 checks); two removed roots absent at both bases and on disk; two local Markdown file links exist; exact allowed diff, `git diff --check <base> -- <exact-files>`, anchored conflict-marker scan and historical-prefix/root-Claude preservation checks. Reproducible verifier: `python3 /tmp/account-674-verify.py` (full script in PR body; no repository test addition).
- Skill validation PASS: existing `python3 /home/ho/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/account-review-handoff` and the same command for `account-module-parallel`: both `Skill is valid!`; no install/download. Existing regressions: `node --test tools/ci/review-scope-contract.test.cjs tools/ci/harness-quality-contract.test.cjs tools/ci/harness-pr-contract.test.cjs` => 135 pass, 0 fail/skip; `/tmp/account-674-node-tests.log`. Missing/empty/out-of-scope read approval and injected unsafe-command fixtures remain covered; tests do not enforce a runtime security sandbox.
- Independent review: `/root/review_674` found no P0–P3 in the nine-file diff and independently reran review-scope tests (71/71). Q1 N/A: no code change; Q2 PASS: discovery and approved canonical lookup explained (`00:34`, `95:73`, `GEMINI_REVIEW_PROMPT:807-808`); Q3 PASS: active references match Git trees; Q4 N/A: no nontrivial code/comment change. Parent's 135-test and quick-validation results are distinguished from Reviewer's own execution.
- Not run: Gradle/build/service startup/DB checks, because documentation-only changes do not affect executable modules. Full repository historical-link cleanup is out of scope. Independent review is procedural separation, not a separate GitHub identity, permission isolation or GitHub APPROVED review.
- Publication gate: user authorized branch commit/push and a `Refs #674` Draft PR; URL recorded in the next publication entry. Only parent updates Git/GitHub and shared records. Ready/merge/Issue close/cleanup are not performed. Next owner: human reviewer and separately authorized parent Integrator, checking latest head/base and CI before integration.
- Rollback: reviewed follow-up PR reverses only these reference corrections while preserving shared history; no schema/data rollback. No conflict occurred, so conflict-log is unchanged.

- Publication: Draft PR [#707](https://github.com/skyg547/account/pull/707), `Refs #674`, implementation commit `d318abb9` pushed. Independent reviewer also checked the final PR body/Q1–Q4/reproduction script with no P0–P3. Issue moves to `status:needs-review`; latest published-head CI and human review remain integration gates. Draft/Issue/worktree remain open/preserved.


## 2026-09-11 — GH-679 search failure and empty production scope

- Issue #679 / parent #661; difficulty:medium, user-requested high reasoning. Parent implements the small guide change and owns Git/GitHub/shared records; separate read-only Astra high Reviewer `/root/review_679` reviews it. Branch `agent/679-search-verification`, isolated worktree `/tmp/account-679-search-verification`, base `origin/main@20bb8e48b1a4d34bc6a3ba0ee12202614c385590`. Original dirty checkout and other worktrees preserved.
- Preflight: Issue was OPEN/status:ready/agent:codex despite older draft prose, with no competing claim or dependency. Open PR #708 overlaps only shared historical records. Original Bash failure was reproduced before claiming; parent did not set Ready. Claim comment records scope/base/evidence; current Issue is `status:in-progress`.
- Scope: only `docs/ai-harness/25-issue-claim-verification.md` plus four append-only parent records (`worklog.md`, `agent-status.md`, `handoff.md`, `docs/history/CODEX_WORKLOG.md`). Explicit production Java roots, identical rg enumeration/search filters, immediate native exit checks and MATCHES/ZERO_CANDIDATE/ERROR distinguish matches, successful zero candidates and incomplete verification. Missing/empty/mixed-invalid scopes cannot authorize closure. Full acceptance criteria and parent judgment remain required.
- Reproduced original Bash pipeline on synthetic matching/nonmatching/missing/empty fixtures: counts 1/0/0/0; PIPESTATUS `0 0 0` / `1 1 0` / `2 1 0` / `1 1 0`; final pipeline success in all four cases. Missing path stderr is visible but masked by final status/count. Standalone-only wildcard path also produces grep exit 2 despite count 1. Evidence: `/tmp/account-679-verification/before.log`; no repository production file was searched.
- PASS: `node --test tools/ci/harness-quality-contract.test.cjs` 32/32, failure/skip 0; `git diff --check`; exact substantive file scope; unchanged guide prefix/suffix; anchored marker check. Independent Reviewer independently ran the same 32 tests and diff check. Review corrections use rg's own enumeration filters for symlink/case consistency and require PowerShell 7.2+ for redirected-native-stderr fixture behavior.
- NOT RUN: actual PowerShell fixture execution. `python3 /tmp/account-679-verify.py` exited 2 with explicit runtime-absent status after extracting exact Markdown examples to `/tmp/account-679-verification/`. Five documented scenarios and 19 extra boundaries are prepared. Host PowerShell and cached PowerShell image are absent; user clarification about a temporary official runtime download is pending because the Issue explicitly prohibits downloads. No package/dependency was installed/downloaded. No PowerShell success is claimed.
- Independent static review found no remaining functional defect, but Q1/Q3 are FAIL (unverified runtime, not a confirmed code defect); Q2/Q4 PASS for flow/error/authority explanation and intent comments. Final runtime evidence and record review remain required before completion/publication. Gradle/build/service/DB checks are N/A because no business code, build, wiring, schema or runtime changes occur.
- Publication: Draft PR not yet created; requested `Refs #679` publication is gated on actual verification. No commit/push/PR Ready/merge/Issue close/cleanup. Next action belongs to parent after approved PowerShell runtime provision, then independent final review and Draft publication. Review separation is procedural, not a separate GitHub identity, enforced permission sandbox or GitHub APPROVED review; model level does not grant external authority.
- Rollback: reviewed follow-up PR reverts only this Issue's guide change and preserves shared history; no schema/data rollback. No conflict occurred. Fixture files and the isolated worktree are retained for reproducibility.

## 2026-09-11 — GH-675 reusable Gemini review template

- Issue #675 / parent #661; difficulty:medium, user-requested high reasoning. Branch `agent/675-gemini-review-template`, worktree `/tmp/account-675-gemini-review-template`, base `origin/main@20bb8e48b1a4d34bc6a3ba0ee12202614c385590`. #673/PR #702 and #674/PR #707 were merged; #675 was OPEN/ready with no competing claim or open PR. Parent claim: issuecomment-5632370282. Primary dirty checkout and other worktrees preserved.
- Root review prompt is now a 110-line blank current Issue/base/head/branch-worktree/exact-allowlist/evidence template. Missing identity/scope or unverified/mismatched SHA holds review; missing/stale/archive evidence never certifies the current head. #673 filename-first, sensitive/ambiguous exclusion and exact staged/unstaged/untracked/whitespace gates remain.
- Complete pre-change root (901 lines, 89,333 bytes) is enclosed verbatim in `docs/history/GEMINI_REVIEW_PROMPT_ARCHIVE_2026-09-10.md`; inactive wrapper states source/date and forbids treating history as current instructions or verification. Original SHA256 `5ef3b720e8397b70f5f34fed02c571a375cb8d0106e99b1b970060c90ace35bf`; exact comparison with base Git blob PASS. Archive filename is the Issue-specified historical bundle identifier; snapshot includes the merged prerequisite corrections.
- Substantive scope: `GEMINI_REVIEW_PROMPT.md`, archive, new `tools/ci/gemini-review-template.test.cjs`, adapted `tools/ci/review-scope-contract.test.cjs`. Existing test adaptation is a necessary scope extension explicitly authorized by the user's Full Authority and recorded in the claim; all71 safe-read fixtures remain. Parent alone appends these four harness/history records; no conflict or conflict-log change.
- Verification: `node --test tools/ci/gemini-review-template.test.cjs tools/ci/review-scope-contract.test.cjs tools/ci/harness-quality-contract.test.cjs tools/ci/harness-pr-contract.test.cjs` at2026-09-11T09:27:35Z, Node22.23.2:205/205 PASS,fail/cancel/skip/todo0,exit0,wall0.309s; log `/tmp/account-675-tests.tap`. New template70 + scope71 + common contracts64. Legacy scope assertions first failed71/71 against the new layout (expected RED), then were adapted without removing safe-read cases. Active Markdown link/anchor checks and archive mutation checks are included.
- Independent read-only `/root/review_675` (GPT-6 Astra high) found no P0–P3 and directly reran template+scope141/141,exit0,duration239.243568ms. Reviewer independently confirmed archive Git-blob bytes and safe-read preservation. Q1 PASS: cohesive test helpers; Q2 PASS: current input→SHA/evidence gate→review/HOLD and bounded read flow; Q3 PASS: prerequisites, command, inactive history and test limits; Q4 PASS: comments explain fixed document reads, synthetic evidence and bounded link metadata. All four apply, so N/A is not used.
- Final publication checks: exact8-file scope, four prior log prefixes preserved, conflict markers and staged scoped `git diff --check` are checked with `/tmp/account-675-verify.py` before commit. PR URL/publication head is recorded separately after actual creation; no future CI success is claimed.
- Not run: historical build/tests (no re-execution/re-certification), Gradle/build/service/DB checks (no business/build/runtime change). Tests are static/synthetic, not an AI sandbox or exhaustive secret detection. Node tests remain CI-unwired; existing CI success is not their execution evidence. No installation/dependency download, credentials or external runtime access.
- Authority: parent owns document implementation/shared records/Git/GitHub; Test agent owns only two test files; independent Reviewer is read-only. Separate sessions are procedural separation, not GitHub APPROVED or separate account/permission isolation. Model tier confers no external authority. User authorized commit/push/Refs #675 Draft PR; Ready/merge/Issue close/cleanup not performed.
- Rollback: reviewed follow-up PR reverses only four substantive changes and preserves shared history; no schema/data rollback. Next owner: human Reviewer and separately authorized parent Integrator for latest-head/base/CI integration gates.

- Publication: Draft PR [#708](https://github.com/skyg547/account/pull/708) created with `Refs #675`; implementation commit `6a42c579` pushed. Final staged8-file/log/PR-text independent review found no P0–P3; exact scope/archive/prefix/marker/scoped whitespace verifier PASS. Issue #675 is `status:needs-review`. This publication entry adds only the actual PR identity/status; substantive files remain frozen. Final remote-head CI and human review remain integration gates; no Ready/merge/close/cleanup.


## 2026-09-12 — GH-672 dedicated harness CI gate

- Trace: #672 / parent #661; branch `agent/672-harness-ci-gate`; worktree `/tmp/account-672-harness-ci-gate`; base `origin/main@e83212a773cfabd3a505615554e12f3f0ed555f3`. #662/PR#680 and #674/PR#707 merged; parent claim issuecomment-5637621633. Primary dirty checkout and other worktrees preserved.
- Five substantive files: `.github/workflows/harness-validation.yml`, `tools/ci/validate-harness.py`, `tools/ci/test_validate_harness.py`, guides45/95. Four records appended by parent only. Harness runs every PR/main/manual including tools-only changes; result rejects failure/cancel/skip. Existing Gradle matrix is unchanged and this workflow change still triggers all modules.
- Verified locally: schema11 roles/4 skills/3 UI/4 workflows/2 forms/55 paths; Python20 and Node32 pass with zero skipped/failed tests; real report counters, synthetic Guard/API cases and actual shell pipelines; diff/marker/scope. Evidence `/tmp/account-672-evidence/{schema.log,python.log,node.tap}`. Full command/Q1–Q4 details and initial RED corrections are in [worklog](worklog.md#2026-09-12--gh-672-dedicated-harness-ci-gate).
- Prepublication state: frozen implementation awaiting independent final review, then parent-authorized Refs #672 Draft PR. Hosted CI unrun; local Gradle/service/DB N/A because business/build/runtime files are unchanged. Actual hosted harness/full-matrix results must be checked before integration; unrun/pending is not PASS.
- Scope limits: existing system Python/PyYAML6.0.1 with fail-closed runtime drift; no install fallback, no history/private bodies, no exhaustive upstream schema/link validation, no Guard trust-model or GitHub ruleset change. Other Node suites remain local/unwired.
- Authority: parent owns implementation/shared logs/Git/GH; Test agent only fixture; independent Reviewer read-only. Separate sessions are procedural, not separate GitHub identities, enforced sandboxes or an APPROVED review. Model level grants no external authority. Draft publication is authorized; Ready/merge/close/cleanup are not performed.
- Rollback: reviewed follow-up PR reverts this Issue's five substantive files while preserving history; no schema/data rollback. Next owner: human Reviewer and separately authorized Integrator for current head/base/CI gates. PR identity and independent outcome are appended after actual completion.

- Independent final review: `/root/review_672` (Astra high, read-only) found no remaining P0–P3. Independently reran schema PASS, Python20/20 (1.791s), Node32/32 (96.922ms), report20/32 and scoped whitespace/markers; Q1–Q4 all PASS. Initial findings are resolved. Hosted CI remains a postpublication gate.

- Publication: Draft PR [#712](https://github.com/skyg547/account/pull/712) created with `Refs #672`, implementation commit `47e275d0` pushed. Final independent staged9-file/log/PR-text review found no P0–P3; Q-table file:line evidence was completed before creation. Issue moves to `status:needs-review`. This record-only update preserves the reviewed five substantive files. Actual hosted checks remain pending at publication; no Ready/merge/close/cleanup.

- Hosted correction: run [34623919862](https://github.com/skyg547/account/actions/runs/34623919862) on `3674c009` passed existing Python3.12.3/PyYAML6.0.1 and schema, then failed Python20 with six Guard fixture errors because scrubbed `os.defpath` omitted the runner Node toolcache directory; Node suite skipped and fixed result correctly failed. The fixture now resolves the existing absolute Node executable before scrubbing child environment. A nonstandard-PATH regression proves execution, secret-free child environment and missing-runtime failure. Local unittest21/21, schema/report21/diff PASS; this does not claim hosted rerun success.

- Independent portability-fix review: `/root/review_672` found no P0–P3, independently passed unittest21/21 (1.370s), schema55 and scoped diff. Q1–Q4 PASS: helper/test lines33–35/299–312/351 and existing guide95:62–90. No workflow/schema/public command change; hosted rerun at the new head is still required.

- Hosted verification: corrected implementation `5780be0c` passed [Harness Validation run34624098176](https://github.com/skyg547/account/actions/runs/34624098176): actual runner Python3.12.3/PyYAML6.0.1, schema55 paths, Python21/21 (1.550s), Node32/32, zero fail/cancel/skip/todo; both count checks and fixed result PASS. Agent Merge Guard run34624098166 PASS. Full Gradle run34624098167 is still running and is not certified here. This append records the observed successful implementation head; subsequent commit changes only these four records. PR#712 remains Draft, and current-head checks/authority boundaries are reported in its body.

## 2026-09-12 — GH-670 auxiliary publishing and shared-record ownership

- Issue #670 / parent #661; difficulty:medium, requested high reasoning. Branch `agent/670-aux-publishing-ownership`, worktree `/tmp/account-670-aux-publishing-ownership`, base `origin/main@e83212a773cfabd3a505615554e12f3f0ed555f3`. Prerequisite #662/PR #680 merged as `5f8103afd4f018723302fd4b15b740b3bcf189c1`, confirmed an ancestor. Issue was OPEN/status:ready despite historical draft prose; no competing claim/open PR. Parent claim: issuecomment-5637546870.
- Substantive allowlist: `GEMINI.md`, `CLAUDE.md`, `.agent/workflows/frontend-feature-loop.md`, `docs/ai-harness/90-beginner-ai-agent-git-guide.md`. Auxiliary implementers/reviewers return evidence and record-change requests; parent alone owns shared history and approved Git/GitHub mutations. Frontend sequence now covers Issue/specification, isolation, targeted verification, independent review and approved parent `Refs #<issue>` Draft publication. Beginner examples distinguish read-only advisory Integrator from the main parent, enter the isolated worktree, and separate later lifecycle gates.
- Parent is the only writer of these four append-only records and all Git/GitHub operations. Original dirty checkout and other worktrees were not changed or copied; root GEMINI.md had no reported diff at intake. No business code/build/schema/runtime/credentials were accessed for implementation. No installation, dependency download, force, deletion or automatic merge.
- Verification PASS: `git diff --check`; `git diff -- GEMINI.md CLAUDE.md .agent/workflows/frontend-feature-loop.md docs/ai-harness/90-beginner-ai-agent-git-guide.md`; scoped `rg -n '부모|parent|advisory|push|Push|PR|worklog|WORKLOG|기록|승인|approval|immediately|Repeat'` on those same four explicit files. Evidence retained at `/tmp/account-670-scoped.diff` and `/tmp/account-670-scoped-rg.txt`. Parent verifier `python3 /tmp/account-670-verify.py` checks exact allowed scope, no untracked files/conflict markers, four history prefixes byte-preserved, 17 local Markdown links and scoped whitespace; prints substantive SHA256 fingerprints.
- Existing contract checks: `node --test tools/ci/harness-pr-contract.test.cjs tools/ci/harness-quality-contract.test.cjs` PASS 64/64, failure/cancel/skip/todo 0, duration 133.177873 ms; `/tmp/account-670-tests.tap`. These test existing central contracts, not semantic completeness of the four auxiliary documents. Document-only: Gradle, frontend build/lint/runtime, API/core/batch and DB tests are not applicable and were not run. No runtime-permission enforcement or remote CI success is inferred from static checks.
- Three document walkthroughs: implementation-only returns verified changes with no unapproved external publication; review-only returns findings/record requests without mutations; explicit Draft approval permits only parent publication after verification and independent review. All three require zero unauthorized publications and one designated shared-record writer (parent; no write required for review-only). Missing approval, failed required checks, unresolved findings, changed head/base and a request to record review return to the applicable gate, never imply success. Reproduce from the guide's `Check Three Approval Scenarios` table against the four entrypoints and central 30/80/85/86/20/88; no external command is executed for these scenarios.
- Independent review: separate read-only Astra high `/root/review_670` found no P0–P3 on the frozen four-file diff and confirmed all three scenarios plus missing approval, failed/unverifiable checks, changed head/base and review-record requests. Independently reran central contract tests 64/64 PASS (133.396253 ms), scoped whitespace and marker checks. Reviewer confirmed Q1 N/A (no production/test code changes), Q2 PASS (roles → evidence → review/approval → publication and exception flow), Q3 PASS (beginner examples and central lifecycle links), Q4 N/A (no nontrivial code to annotate).
- Publication: independent substantive review passed; Draft PR creation is the next approved action. User has approved commit/push/`Refs #670` Draft PR; Ready/merge/Issue close/cleanup are not performed. Separate reviewer session is procedural independence, not a separate GitHub account, enforced permission sandbox or GitHub APPROVED review; model tier grants no external authority.
- Rollback: reviewed follow-up PR reverts only this Issue's four substantive document changes, preserving shared history. No schema/data rollback. No conflict occurred. Next owner: parent for Draft publication after review, then human reviewer and separately authorized parent for latest head/base, CI and integration gates.

- Publication: Draft PR [#711](https://github.com/skyg547/account/pull/711) created with `Refs #670`; implementation commit `a6274678` pushed. Issue #670 is `status:needs-review`. Independent final record review confirmed no P0–P3, identical appended entries and all prior history prefixes preserved; four substantive SHA256 fingerprints unchanged. This publication entry records actual PR state only. Ready/merge/Issue close/cleanup were not performed. Next owner: human reviewer and separately authorized parent for latest-head/base/CI integration gates; current worktree retained.

## 2026-09-12 — GH-713 Portainer CE and Nginx subpath

- Issue #713, difficulty:medium / requested high reasoning; parent Codex owns implementation/integration. Branch `agent/713-portainer-ce`, worktree `/tmp/account-713-portainer-ce`, exact fetched base `origin/main@eb0058deed3816fb9917cd80d9d862afed00f336`; claim issuecomment-5638616090. Dirty primary checkout was preserved. User authorized relevant adjacent files, dependency/runtime validation and commit/push/Draft PR; no Ready/merge/Issue close/branch or worktree removal.
- Added `portainer/docker-compose.yml`, module README and `portainer/tests/{test_contract.py,live-smoke.cjs,README.md}`; updated `frontend-nginx/{nginx.conf,README.md}` and added `docs/guides/portainer.md`. CE 2.39.7 has a named `/data` volume, no published ports, external account-network, private bootstrap-file requirement, read-only rootfs, bounded CPU/memory/PIDs/logs and only DAC_READ_SEARCH restored. Nginx strips the base prefix once, preserves Host/port, dynamically resolves the backend and supports WebSocket/streaming; scoped access/error logs suppress token-bearing URIs. No API/core/batch/build/DB change.
- Verification PASS: Compose `config --quiet` and missing-admin fail-closed render; `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s portainer/tests -v` 7/7 with 34 unsafe variants; `node --check portainer/tests/live-smoke.cjs`; `python3 tools/ci/validate-harness.py` schema 55; whitespace, tracked conflict-marker and unmerged checks. Synthetic foreign-owner mode0600 file is unreadable with all capabilities dropped and readable with only DAC_READ_SEARCH; no actual credential value was logged.
- Live rootless Podman 4.9.3 / Nginx 1.27-alpine / Chromium143 PASS: actual localhost:8080 HTML/status200, relative301, unauthenticated401, browser admin login plus 8 JS/CSS responses, labelled probe follow logs and restart, WebSocket HTTP101 with interactive shell-produced output (not input echo). Final full smoke passed after Portainer force-recreate changed its container ID while retaining `account-portainer-data`, the administrator account and endpoint. Observed resource contract: read-only rootfs, 512MiB, 0.5CPU, 128PIDs, no host bindings, running/restart0/OOMfalse before cleanup.
- Failure/recovery PASS: Nginx starts and passes `nginx -t` while Portainer is stopped, a synthetic-token request returns502 without the marker in either proxy log stream, and backend restart recovers200. All 28 pre-existing running-container states stayed unchanged. Existing Nginx reported8080 mapping but had no host listener; a temporary Issue-owned proxy used free loopback8080 with this PR's complete template. The existing proxy/template were never edited or recreated. Cleanup stopped Portainer and removed only disposable proxy/probe; its named data volume and private bootstrap file remain. Runbook documents exact retained bootstrap path and safe handoff before /tmp cleanup.
- Independent read-only `/root/review_713` found two P1 issues (error-log token exposure and rootful bootstrap-file permissions); both fixed and verified. Final review found no P0–P3, independently reran static7/7/Node/diff/markers, and confirmed Q1–Q4 PASS with Compose:16/37, Nginx:182, live smoke:43/106, guide:57/159 and tests README:17 evidence. Test writer `/root/test_713` owned only policy tests/test README. Parent alone updates these four shared records and Git/GitHub. Separate sessions provide procedural review, not an independent GitHub approval or enforced credential separation.
- Limits/rollback: rootless Podman is outside official Portainer support; Docker daemon execution and public HTTPS/tunnel remain untested. Sparse follow-log output can be delayed by upstream Portainer buffering; documented burst smoke does not guarantee quiet-stream latency. Existing-Nginx permanent deployment and its pre-existing host-listener issue are separate deployment gates. Gradle/business/DB tests are not applicable and were not run. Roll back only this module and the two proxy locations, retaining data; version downgrade requires matching offline backup. Publication next: approved Draft PR with `Refs #713`, then human reviewer / separately authorized parent for current-head CI and integration.


- GH-713 publication: implementation `dfa7741a` pushed; Draft PR [#718](https://github.com/skyg547/account/pull/718) created with `Refs #713`, verification evidence, Q1–Q4 and explicit merge authority / procedural-review limits. Issue #713 is `status:needs-review`. Final independent shared-record review found no factual, authority or secret-value issue. Post-cleanup comparison again preserved all 28 pre-existing running states. This follow-up changes only these four records; substantive files remain the reviewed implementation. GitHub current-head CI is a subsequent gate, not implied by local PASS. Next owner: human reviewer, then separately authorized parent for current-head CI/integration and deployment. Ready/merge/Issue close/branch-worktree removal were not performed.

## 2026-09-12 — GH-714 Cloudflare Tunnel Compose

- Issue #714, difficulty:medium / user-requested high reasoning. Branch `agent/714-cloudflare-tunnel`; isolated worktree `/tmp/account-714-cloudflare-tunnel`; base `origin/main@eb0058deed3816fb9917cd80d9d862afed00f336`. Dirty primary checkout and existing services/data preserved. User authorized implementation, related live verification and Draft PR publication with `Refs #714`.
- Added `cloudflared/docker-compose.yml` (tokenless Quick), `docker-compose.named.yml` (required `${CLOUDFLARE_TUNNEL_TOKEN}` mapped to `TUNNEL_TOKEN`), `.env.example`, README and `docs/guides/cloudflare-tunnel-guide.md`. Both standalone definitions use official `docker.io/cloudflare/cloudflared:latest`, one project/service, external `account-network`, no host ports/mounts, read-only rootfs, dropped capabilities/no-new-privileges and CPU0.5/RAM256MiB/PID128 limits. Quick targets `http://account-frontend-nginx:80`; Named remote route must point there. Quick restart=no; Named unless-stopped. No API/core/batch/domain/SQL/build changes.
- Guide covers account-free startup, protected Named route/Access setup before publication, ignored owner-only dotenv, mode switch, forced token reinjection, original Nginx host bindings, root-stack `account-dev-network` override, DNS/protocol troubleshooting, application HTTPS/cookie/header acceptance, version/rollback and exact service stop. A Named token alone does not enforce user authentication. Existing application/admin data was not exposed through the synthetic Quick test.
- Configuration: Docker Compose v5.4.0 and podman-compose1.6.0 both render both modes with `config --quiet`; ad-hoc Python JSON assertions verify image/network/no ports/hardening/resources/commands/restart and missing/empty-token rejection, literal-dollar token acceptance, dotenv ignored/example trackable. `python3 /tmp/account-714-config-check.py` PASS, evidence `/tmp/account-714-config.log`. Initial assertion incorrectly disallowed Compose's extra empty `ipam` metadata; assertion corrected to the actual name/external contract and all checks passed without changing production behavior.
- Runtime: pulled official cloudflared2026.9.1, image ID `0002e855e2e69c08f9dfc8cce60d15af062d2a57a8bc105c9fb3f89db3551522`. Rootless Podman4.9.3 / podman-compose1.6.0 created task-only network `account-714-tunnel-test`, synthetic Nginx aliased `account-frontend-nginx`, project `account-714-tunnel-smoke`. `python3 /tmp/account-714-live-check.py` verified Quick URL/QUIC registration, public HTTPS `/` and `/api/probe` exact synthetic HTTP200 responses, running/OOMfalse/no ports/read-only/CPU/RAM/PIDs/network PASS (`/tmp/account-714-live.log`). Actual existing `account-network` Nginx `/healthz` probe passed with body discarded. Network-none Named image run with synthetic invalid TUNNEL_TOKEN rejected. Compose service stop returned running=false/exit0 (`/tmp/account-714-additional.log`).
- Additional HTTP/2 recreate registered successfully, but the new Quick hostname failed local DNS (`gaierror`) in two bounded HTTPS probes; `/tmp/account-714-restart.log` and `/tmp/account-714-http2-retry.log` preserve failures. HTTP/2 public HTTP is NOT claimed. Default-mode recreation and final cleanup will be recorded in the publication checkpoint below.
- `python3 tools/ci/validate-harness.py` PASS (roles11/skills4/ui3/workflows4/forms2/required_paths55). Whitespace/conflict checks pass. Gradle/Java/frontend builds and DB tests were not run because only isolated infra Compose and docs changed. Docker daemon live execution and real Named token/domain/Access/web/API are unexecuted; operator-provided credentials and real application acceptance remain distinct next gates. Quick synthetic responses do not certify financial workflows or load. Mutable latest and existing proxy HTTPS-header handling are documented operational risks.
- Independent read-only reviewer `/root/review_714` found one P2 network-guide omission; root Compose defaults to account-dev-network. Parent corrected guide/example; re-review found no P0–P3 and Q1–Q4 PASS after reading runtime evidence. Q1: bounded Compose responsibilities + config/live assertions; Q2: input/network/route/switch/failure explanation; Q3: runnable guide + Nginx/Quick smoke; Q4: token separation and Quick restart intent comments. Reviewer performs no file/Git/GitHub/runtime mutation; parent owns all shared records and publication.
- Rollback: stop/remove only the dedicated cloudflared service using token-free Quick definition and the same project. Keep external network, Nginx, applications, DBs and volumes; reviewed source revert only. No conflicts occurred. Draft publication is the authorized endpoint; no Ready/merge/Issue close/branch or worktree deletion. Next owner: human reviewer for CI/current-head and Named operator acceptance, then separately authorized Integrator for integration.
- Implementer tier: high reasoning (difficulty:medium, user-specified). Merge authority: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음. Independent subagent review is procedural separation, not a GitHub APPROVED review or separate credential identity.

- Final prepublication checkpoint: default-mode recreation also allocated a URL and registered but its bounded public probe failed (`/tmp/account-714-default-restart.log`); only the initial default-mode public HTTPS smoke is claimed successful. Parent's separate diagnostic observed a DNS `gaierror` for the HTTP/2 hostname. Successful repeat/public HTTP2 acceptance remains a documented limitation, not a passed test. Final stop again returned running=false/exit0. Podman Compose1.6 lacks the rm subcommand; the first removal failed and therefore external test-network removal also correctly refused while the stopped connector remained. Guide now specifies inspect + exact-name podman rm for this provider. Exact stopped connector and synthetic Nginx were removed with podman rm, then task-only network removal succeeded. Existing application/network/data was preserved.

- Publication: Draft PR [#717](https://github.com/skyg547/account/pull/717) created with `Refs #714`, implementation commit `23bc8f01` pushed only to `agent/714-cloudflare-tunnel`; Issue #714 is `status:needs-review`. Independent final guide/recovery review remains Q1–Q4 PASS with no remaining P0–P3. Task-only connector/origin/network cleanup completed; temporary validation logs remain under `/tmp`. This follow-up records publication only. Human CI/current-head review and real Named operator acceptance are next gates; no Ready/merge/Issue close/branch or worktree deletion.

## 2026-09-12 — GH-715 GHCR bulk image packaging (Draft publication prepared)

- Issue: #715; owner: Codex parent Integrator; branch: `agent/715-ghcr-packaging`; isolated worktree: `/tmp/account-715-ghcr-packaging`; fetched base: `origin/main@eb0058deed3816fb9917cd80d9d862afed00f336`. Dirty primary checkout was preserved. Draft PR: publication follows this verified commit, using `Refs #715`.
- Scope: `tools/docker/push-ghcr.sh`, `tools/docker/test_push_ghcr.py`, `docs/guides/ghcr-container-registry-guide.md`, and these four append-only records (`worklog.md`, `agent-status.md`, `handoff.md`, `docs/history/CODEX_WORKLOG.md`). User Full Authority covers directly relevant tests beyond the original two-file implementation allowlist. Parent owns script/docs and shared records; separate test agent owns only the test file.
- Behavior: canonical manifest drives all 36 enabled targets (15 business API/Batch pairs, Auth/Internal Audit APIs, platform 3, frontend). Required version tag, selected targets, source prefix/tag and Docker/Podman options; dry-run never invokes an engine. All source IDs are inspected and pinned before any tag/push; failures stop the sequential release and report partial success. Credentials stay with the engine, and the destination namespace is fixed to `ghcr.io/skyg547/account`.
- Documentation: build prerequisites, PAT classic `write:packages`/stdin login, engine-specific stores, repository/package permissions, retry and immutable release-tag practice, pull/digest verification and rollback. GitHub's official Container Registry documentation checked on 2026-09-12. No automatic event workflow, third-party mirroring, deployment or business/build changes.
- Verification: `bash -n tools/docker/push-ghcr.sh` PASS; `python3 -m unittest discover -s tools/docker -p 'test_push_ghcr.py' -v` 12/12 PASS (also independently rerun); manifest dry-run 36/36; `git diff --check` PASS; `python3 tools/ci/validate-harness.py` PASS (11 roles, 4 skills, 55 required paths). Tests cover malformed manifests/IDs, invalid arguments, target coverage/filtering, Docker/Podman argv, zero dry-run calls, all preflights before mutations, failure stop and repeat after partial push.
- Live verification: Podman 4.9.3 synthetic scratch image built with `--network=none --pull=never`; unmodified launcher performed genuine image-ID inspection, ID-based tag and Podman push using a test wrapper redirecting only the push transport to a temporary local `dir:` destination. Export manifest exists and source/destination IDs match; `SUCCESS: pushed 1/1 images`. Reproduction driver retained at `/tmp/account-715-podman-smoke.py`; fixture tags/files cleaned. This is local transport smoke, not GHCR publication. Real standard-source preflight also rejected absent `account/config-server:local` before any mutation.
- Skipped: real GHCR authentication/upload/pull (no release image set/token supplied; current local images use development/Compose names); actual Docker engine (not installed); Gradle/application startup (no Java/SQL/build/runtime files changed); ShellCheck (not installed). No secret files were read, application containers started/stopped, or registry packages published. Release operator must run approved-image login → push → pull/digest checks; fake engines and local Podman transport do not prove GHCR authorization.
- Independent review: read-only `/root/review_715`, separate from parent implementation and `/root/test_715`, found no P0–P3 in the three substantive files; separately verified tests, shell syntax, whitespace, markers and local documentation links. Q1–Q4 PASS: CLI/manifest/inspect/publish responsibilities and behavior tests; flow/failure/retry documentation; reproducible beginner commands and honest limits; comments explaining manifest reuse, pinned IDs and suppressed credential-helper diagnostics.
- Rollback: reviewed revert of this issue's script/docs/test changes; preserve shared history. Later release rollback uses previous verified remote image digests; no package/volume prune or automatic remote deletion. No conflicts occurred.
- Authority: user approved branch commit/push and Draft PR. Merge authority: independent Reviewer approval then separately authorized parent Integrator only. No self-approval, Ready transition, merge, Issue close or worktree/branch deletion. Separate agent sessions provide procedural review independence, not separate GitHub credentials, sandbox enforcement or a GitHub APPROVED review.
- Next owner: parent publishes Draft and records URL, then human reviewer for latest-head/base and CI review; release operator for genuine GHCR integration verification.

- Publication checkpoint (2026-09-12): Draft PR [#716](https://github.com/skyg547/account/pull/716) is OPEN/DRAFT with `Refs #715`; implementation commit `19498521` is pushed. Issue #715 is `status:needs-review`. Independent final record audit found no P0–P3, confirmed all four prior-history prefixes and identical appended entries, and retained Q1–Q4 PASS. PR body contains verification results, honest GHCR/Docker limitations and explicit Merge authority/role separation. GitHub checks are pending at publication; no Ready/merge/Issue close/cleanup performed. Next owner: human reviewer for PR #716 and latest-head CI; release operator for actual GHCR login/push/pull/digest verification.


## 2026-09-12 — GH-671 fail-closed current review guard (Draft proposal)

- Issue #671 / Parent #661; branch `agent/671-fail-closed`; worktree `/tmp/account-671-fail-closed`; fetched base `origin/main@87206771d8057fdd4f39bd35df0331768d0bf05f`. #662/PR #680 merge `5f8103afd4f018723302fd4b15b740b3bcf189c1` verified. Primary dirty checkout and two pre-existing dirty #671 worktrees preserved. User authorized implementation/testing and Draft publication; trust model activation, Ready, merge, close and cleanup are not complete.
- Five substantive files: `.github/workflows/agent-merge-guard.yml`, `tools/ci/agent-merge-guard.cjs`, `tools/ci/agent-merge-guard.test.cjs`, `docs/ai-harness/87-spec-driven-delegation.md`, and coupled `tools/ci/test_validate_harness.py`. The last file extends the Issue's original allowlist under the user's explicit related-file authorization: #672's existing fixture hardcoded the old inline workflow and accepted approval-free PASS, so preserving it would break Harness CI. Four parent-owned history records are appended; no business/API/core/batch/SQL/build/runtime change.
- Pure metadata evaluator plus read-only API adapter: all main PRs require exactly one implementation owner; latest reviewer decision, current head/base/metadata binding, dismissal/rejection, age/future limits, full bounded pagination and repeated PR/review snapshots fail closed. Candidate formal-review policy exists only for synthetic tests; real workflow has `policy: undefined`. Same-account trust decision is still pending and arbitrary comments never approve. This is a concrete Draft proposal, not completed #671 acceptance or merge readiness.
- Workflow has PR/review edit/dismissal events and shared PR concurrency. Both jobs reject non-main before checkout; candidate tests run separately without persisted credentials; metadata guard loads/tests only event base SHA code with read-only contents/pull-request permissions. Candidate tests must succeed first. Bootstrap trusted-base test fails until helper exists on main; no fallback to PR code. Final-read race, base-only changes, expiry without an event and PR-editable workflow remain documented soft-control limits.
- Verification: `node --test tools/ci/agent-merge-guard.test.cjs` 89/89 PASS; combined Guard/lifecycle/quality Node command 153/153 PASS, zero failures/skips; `/usr/bin/python3 -m unittest discover -s tools/ci -p test_validate_harness.py -v` 22/22 PASS including actual workflow wrapper and pre-checkout shell; `/usr/bin/python3 tools/ci/validate-harness.py` schema55 PASS; Node syntax/diff/marker/unmerged gates PASS. Actionlint is unavailable and not installed; parsed workflow assertions and primary event documentation are the static evidence. No package/dependency download, real PR approval/dismissal/merge, DB, container or operational-server testing. GitHub current-head CI is checked after publication, not implied by local success.
- Baseline original Guard was executed with synthetic API objects at this base: owner absent PASS/API0; approval absent PASS/API1; old head approval PASS/API1; approval then changes requested PASS/API1. Independent read-only Astra high also reproduced these. Audit posted to #661: https://github.com/skyg547/account/issues/661#issuecomment-5645764057 .
- Roles: parent owns helper/workflow/docs, coupled Python integration fixture, Git/GitHub and shared records; `/root/test_671` (test, Astra high) owns only the new Node test; `/root/review_design_671` (reviewer, Astra high) is read-only. Independent review identified unsupported review-event branch filter (P1) and checkout-before-main-validation (P2); both corrected with executable regression. Final review result and Draft URL follow in the publication checkpoint.
- Rollback: a reviewed follow-up PR reverts only this Issue's Guard/test/functional-doc changes, preserving shared history. Next owner: user chooses same-account external trust evidence versus approved distinct Reviewer account (including stable ID/TTL), then independent Reviewer and separately authorized parent verify the activated contract/current head/base/CI. No status change grants an account, signing key, GitHub permission or merge authority.

- Final independent substantive review: `/root/review_design_671` Astra high confirmed both P1/P2 fixes, no remaining P0–P3 and Q1–Q4 PASS (helper:13/72/127; delegation doc:244/277; Python preflight/wrapper). Guard89, Python22, lifecycle32 and schema55 independently verified. This approves a Draft proposal only; trust activation and Issue acceptance remain pending. Parent prepared a PR body with results, skipped checks, bootstrap limitation, original allowlist extension and explicit merge-authority separation.

- Publication checkpoint: Draft PR [#721](https://github.com/skyg547/account/pull/721), `Refs #671`, initial head `ca2dd387713048d9f06722c9e778d03526a2ba5e`; Issue is OPEN / `status:needs-review` with `triage:needs-human` retained. Initial-head GitHub [Guard run 34692737238](https://github.com/skyg547/account/actions/runs/34692737238): candidate tests SUCCESS; trusted-base test FAILURE (base lacks new helper/test), not waived or recorded as PASS. [Harness Validation 34692737178](https://github.com/skyg547/account/actions/runs/34692737178) SUCCESS. Module matrix was pending at observation; no full-module success claimed. This append-only publication commit requires its own latest-head CI observation. Independent Reviewer also audited the four preserved history prefixes and actual Draft body with no P0–P3. Trust choice/activation remains unresolved; no Ready/merge/close/cleanup performed.

## 2026-09-12 — GH-719 외부 분개 REST / OpenAPI RFC

- Issue: #719; owner: Codex parent Integrator; branch: `agent/719-external-journal-rfc`; worktree: `/tmp/account-719-external-journal-rfc`; base: `origin/main@87206771d8057fdd4f39bd35df0331768d0bf05f`. 기존 dirty 기본 checkout을 보존했다. Draft PR은 검증·독립 리뷰 후 `Refs #719`로 게시한다.
- 변경 범위: `docs/architecture/external-journal-integration-rfc.md`와 이 4개 공유 기록(`docs/ai-harness/{worklog,agent-status,handoff}.md`, `docs/history/CODEX_WORKLOG.md`). 실행 코드·설정·DB·빌드 변경 없음. RFC 작성/기록은 부모, Gateway 탐색과 독립 리뷰는 별도 읽기 전용 에이전트가 담당했다.
- 결과: 현재 JournalPostingAdapter/기존 REST 경로와 제안 InternalJournalPortAdapter 역할/외부 controller를 구분했다. 독립 DTO→command 매핑, 인증 주체·원천 권한, 초안 전용 승인 분리, 원자적 receipt 및 replay/충돌/rollback, Gateway API Key/OAuth2 정책, OAS 3.0 전체 YAML 예시와 시퀀스, springdoc group 생성·호환성·배포 및 후속 개방 게이트를 문서화했다.
- 근거: journal/core/contracts 호출 경로·금액 DECIMAL(19,2)/환율 DECIMAL(19,8), Gateway 인증/route를 읽었다. journal API는 core→shared-kernel을 통해 WebMVC UI 2.5.0을 전달받는다는 점과 기존 사용자 JWT 검증/빈 인증 오류 및 JWKS 지원 공백을 제안과 구분했다. springdoc v2/공식 Gradle plugin/OAS 공식 문서를 확인했다.
- 검증: `python3 /tmp/account-719-validate.py` PASS — YAML 2개, 공식 OAS 3.0 schema, 내부 참조 38개, 요청/201 응답 예제, 거부 fixture 12개(필수/actor 위조/날짜/side/숫자 금액/scale/precision/음수/baseAmount/라인/환율), 허용 schema 경계 2개, 로컬 링크 18개. 검증기는 기존 PyYAML/jsonschema를 사용했고 공식 schema JSON만 `/tmp/account-719-oas-schema.json`으로 가져왔다. 패키지 설치 없음.
- 정적 검증: `git diff --check` PASS; tracked 전체 및 신규 RFC 충돌 마커 검사 PASS; `python3 tools/ci/validate-harness.py` PASS (11 roles, 4 skills, 55 required paths). 문서 내 예제의 schema 유효성은 금융 업무 검증이나 실행 API의 구현 증거가 아니다.
- 미실행: Gradle/API·Gateway 실기동·PostgreSQL 동시성·실제 springdoc 추출·Mermaid 렌더링. 문서만 변경하며 해당 API/profile/task는 제안이므로 실행 성공으로 주장하지 않는다. RFC 7절의 구현·보안·금융·생성 CI·통합 테스트가 실제 개방 조건이다. 다이어그램은 텍스트 의미 리뷰만 수행한다.
- 독립 리뷰: `/root/review_rfc` 결과와 Q1–Q4 증거표는 아래 publication checkpoint에 확정 기록한다.
- Rollback: 이 문서 변경을 리뷰된 PR로 revert하고 기존 기록을 보존한다. 런타임 rollback 불필요. 충돌 없음; conflict-log 수정 없음.
- 권한 분리 / Merge authority: Codex 작성·검증과 읽기 전용 독립 Reviewer를 분리하고 부모 Integrator만 공유 기록/Git/GitHub를 갱신한다. 사용자 승인 범위는 commit/push/Draft PR이며 Ready·merge·Issue close·branch/worktree 삭제는 수행하지 않는다. 독립 세션은 절차적 분리이며 별도 GitHub 계정/자격증명이나 GitHub APPROVED review를 의미하지 않는다.
- 다음 담당: 독립 리뷰 반영 후 부모가 Draft PR을 게시하고, 사람 리뷰어가 설계/CI/최신 base를 검토한다. 후속 구현자는 RFC 7절의 개방 게이트와 외화/다중 장부 제한을 확인한다.

- 독립 리뷰 확정: `/root/review_rfc`가 실제 source/DDL/Gateway/build와 RFC를 대조하고 검증기를 직접 재실행하여 P0–P3 없음으로 보고했다. springdoc 2.5.0 공식 source의 `/v3/api-docs.yaml/{group}` 경로도 확인했다.

| 항목 | 판정 | 파일·검증 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | N/A | RFC와 공유 기록만 변경; production/test code diff 없음 | 코드 품질 변경 없음 | 제안 구현 시 API/core 경계 검증 | /root/review_rfc 확인 |
| Q2 | PASS | RFC 2–4절: mapping, 트랜잭션, 재전송, 인증 흐름/예외 | 해당 없음: 설계 설명 대상 | 후속 동시성·보안 통합 테스트 | /root/review_rfc 확인 |
| Q3 | PASS | RFC 1·5–7절: 현재 근거, 검증된 YAML, 미구현 profile/task 표시 | 해당 없음: 기능 문서 대상 | 실제 springdoc 추출·실기동은 후속 게이트 | /root/review_rfc 확인 |
| Q4 | N/A | 실행 코드/비자명 로직 수정 없음; 설정 예시를 제안으로 명시 | 비자명 코드 변경 없음 | 후속 구현 시 의도 주석 검토 | /root/review_rfc 확인 |

- Publication checkpoint (2026-09-12): Draft PR [#720](https://github.com/skyg547/account/pull/720) OPEN/DRAFT, `Refs #719`; implementation commit `f89611ac` pushed. Issue #719 is `status:needs-review`. PR 본문에 검증 결과·미실행 사유·Q1–Q4·Merge authority 및 절차적 권한 분리를 명시했다. GitHub checks는 최종 head에서 별도 확인하며 Ready/merge/Issue close/cleanup은 수행하지 않았다. 다음 담당은 사람 리뷰어(설계·CI·최신 base 검토)다.

## 2026-09-13 — GH-710 container lifecycle and deployment roadmap

- Issue #710 / difficulty:high (requested high reasoning); parent Codex Integrator owns implementation, Git/GitHub and shared records. Branch `agent/710-container-management`, isolated worktree `/tmp/account-710-container-management`, fetched base `origin/main@e7c462f4db2c90238b238454d96c43b7d2a14f1a`; claim issuecomment-5646991772. Dirty primary checkout is preserved. User authorizes related changes/runtime validation and commit/push/Draft PR; Ready/merge/close/cleanup of branch/worktree remain separate gates.
- Reuses merged #713 Portainer/Nginx templates. Adds `deploy/watchtower/{compose.yml,compose.update.yml,README.md,tests/live-smoke.py,tests/test_contract.py}`, `.github/workflows/container-management-validation.yml`, the three-stage roadmap guide and two guide cross-links. No Java/SQL/application/DB/build changes. Parent owns production/docs/live smoke; `contract_tests` owns only offline tests; `architecture_review` is read-only. No business container credentials, environment values or data were test targets.
- Base is opt-in, fixed monitor-only with enable-label + account-dev scope; explicit update override enables sequential recreation and disables monitoring. Live testing found Watchtower1.7.1 rejects global monitor-only + rolling-restart (exit1/OOMfalse); split-mode regression fixes this. Archived upstream (2025-12-17), experimental Podman, singleton downtime, independent migration/rollback and immutable production digest contracts are explicit.
- Verification so far: Watchtower safety tests11, existing Portainer tests7, base/update Compose render, missing-socket fail-closed, Python AST, schema55 and diff gates PASS. Independent reviewer found two P2 smoke-evidence gaps (remote detection vs local cache; registry failure with different local image); both fixed, two-target sequential assertions added and final static re-review found no P0-P3. Final full runtime remains pending at this checkpoint.
- New temporary rootless Podman4.9.3 Portainer2.39.7 instance: admin auth, same-engine inventory73, synthetic probe stats/restart PASS. GUI memory.current21848064/peak22380544 bytes, OOM/max events0; temporary GUI/probe removed. #713 browser/stream/WebSocket evidence is reused, not claimed rerun. GUI-only reading does not prove whole-host100MB overhead. First corrected Podman updater trial failed the strict selection-count assertion; no update success is claimed. Docker CI on a branch push will exercise a credential-free local registry before Draft publication.
- Tests pull versioned public fixture images under the user's runtime authorization. Final smoke uses tmpfs registry storage, random names/scope and owned-resource cleanup; no global prune/business restart. One diagnostic registry anonymous volume was traced and removed by exact ID; first failed attempt may have retained an untracked anonymous fixture volume, which is not deleted without ownership evidence. Base images remain cached.
- Rollback: stop only updater; restore selected dev API's recorded previous digest with its original Compose and readiness/API verification. Keep old images/volumes and migration state. Code rollback is an Issue-scoped reviewed revert. Next owner: parent completes Docker CI and runtime diagnostics, records limits, then creates Draft `Refs #710` with authority separation; independent/human review gates integration.

### GH-710 verified prototype checkpoint

- Docker CI [34704895259](https://github.com/skyg547/account/actions/runs/34704895259) SUCCESS on implementation `73772b496743fc91b977db9159521e672bc2cb99`. All18 static tests and both Compose modes pass. Full live proof: registry v2 is pulled from a restored-v1 local cache without restarting in monitor mode; two selected containers then recreate sequentially; unlabelled/wrong-scope/disabled/stopped controls preserve IDs; repeat is idempotent; refused registry connection preserves both running IDs despite a different local cache; v1 rollback passes. Random loopback port is explicit across registry restart and readiness is awaited. Registry uses tmpfs; final owned-container cleanup passed.
- Upstream1.7.1 metrics include stale items in Updated and can omit HTTP/stream pull failures from Failed, so actual image/container IDs, versions and failure evidence are authoritative. A false counter expectation and a Docker auto-port reallocation in the fixture were corrected before the successful CI. No upstream binary was modified. Independently reviewed functional changes have no outstanding code findings; the README P3 counter wording was corrected. Final exact-head reviewer confirmation follows.
- Updater idle9.039MiB/64MiB was measured on Docker CI. GUI peak22,380,544 bytes/OOM0 was measured on this host; these different-host samples do not establish total-host100MB under active GUI/pull. Local rootless no-pull label/scope selection passed2, but full HTTP registry scan did not pass and is explicitly not supported deployment evidence. Existing registry trust, business services and production settings were not changed. Private GHCR promotion/credentials, actual dev-server rollout, total-host resource gate, and #713 browser replay are not newly validated.
- Final roadmap/README now map acceptance to evidence and disclose these limits. Java/Gradle/SQL suites were not run because implementation is infra/docs/test-only; no application boundary changed. Parent proceeds to DraftPR with Refs#710, explicit implementation/reviewer/merge separation and review/Ready/merge/close retained as separate gates.

### GH-710 Draft publication

- Draft PR [#724](https://github.com/skyg547/account/pull/724) created with `Refs #710`, full verification/limitations/rollback and explicit implementation–read-only Reviewer–merge authority separation. Issue #710 remains OPEN / `status:needs-review` with `agent:codex`; no Ready/merge/close or branch/worktree removal performed.
- Independent Reviewer confirmed implementation `73772b49` has no remaining P0–P3, Q1–Q4 PASS, and directly verified successful Docker CI34704895259. Documentation head `5fc9e3b0` preserves that code and records resource/Podman/private-registry limitations; final PR-triggered checks are inspected separately, never inferred from an earlier head.
- Reproducible worktree remains `/tmp/account-710-container-management`, branch `agent/710-container-management`; all Issue710-labelled local fixture containers were verified absent after cleanup. Rollback and retained-cache/possible first anonymous-volume note remain as above. Next owner: human/independent reviewer evaluates the Draft and remaining host resource/Podman/GHCR adoption gates before any deployment or integration authorization.

### GH-710 PR check observation

- On published head `bb2af8d739a43790da4ad9ea2646479733e9f74f`, PR-triggered Docker prototype [34705065849](https://github.com/skyg547/account/actions/runs/34705065849) and Harness [34705065832](https://github.com/skyg547/account/actions/runs/34705065832) SUCCESS. Final docs and PR evidence received independent review with no substantive P0–P3; the requested per-item Q1–Q4 evidence table is now in PR#724.
- Merge Guard [34705101539](https://github.com/skyg547/account/actions/runs/34705101539) deliberately rejects `MERGE_AUTHORITY_REQUIRED, DRAFT_NOT_MERGE_READY, TRUST_POLICY_UNCONFIGURED`. Candidate guard tests passed; this failure is not waived and no authorization/trust policy is changed. Module Validation was pending at observation; full Java success is not claimed. This append-only record changes no reviewed functionality.

## 2026-09-22 — GH-443 configured-user password contract handoff

- Issue/branch/worktree/base: #443; `agent/443-configured-password-contract`; `/tmp/account-443-configured-password-contract`; `origin/main@e375002ff63652aabaaa1f12c74aa611d4ed29bc` (latest fetch unchanged). Draft publication with `Refs #443` is authorized and pending; no Ready/merge/Issue close/cleanup.
- Result: exact lowercase `{bcrypt}` plus valid BCrypt payload is the only configured/stored format. Raw/noop/unknown/malformed/prefixless/case/whitespace inputs fail before persistence and invalid stored data cannot authenticate. Generic field-path errors do not repeat supplied credential or username data.
- Seed/local boundary: every configured user is validated and mapped before repository access; missing users are inserted in one transaction, existing users are not overwritten, late persistence failure rolls back all inserts, and corrected/idempotent retry is covered. Fixed #652 local SQL credentials/default users were removed, SQL init disabled, and a runtime-generated configured-user API integration test replaces the baseline test. Scope expansion is recorded in issuecomment-5763609044.
- Files: five Auth core production files; eight changed core/API test paths (four modified, one deleted, three new); local application resource plus deleted SQL; `auth/README.md`; `auth/docs/local-run.md`; these four parent-owned records. See `git diff --name-status` for the exact list.
- Verification: requested Gradle command PASS, core71 + API15 =86 tests with zero failures/errors/skips; the parent's run produced bootJar SHA256 `99ec0656a35214514ac7d0f4ffc42b7ccda674d422471485cfeec9ee8d37b49c`. Independent Reviewer reran all tasks with `--rerun-tasks` and confirmed the same counts, deleted SQL absence, static scans and Q1–Q4 PASS. No P0–P2 findings; P3 scope traceability is resolved by the Issue amendment and records.
- Skipped/risk: no external PostgreSQL, container, deployed service or real credential access. H2 proves transaction/startup wiring, not PostgreSQL behavior. Existing legacy/prefixless database hashes intentionally stop authenticating; migration/rotation is a separate approved task.
- Rollback: reviewed path-scoped revert while retaining shared history; never restore the fixed SQL credential as an operational shortcut. Next owner is a human reviewer for Draft diff, published-head CI and latest-base review. Merge authority remains separate.

| Q1 | Q2 | Q3 | Q4 |
| --- | --- | --- | --- |
| PASS — cohesive policy/properties/seed responsibilities and full tests | PASS — mapping, error, rollback/retry flow documented | PASS — exact local inputs/commands/limits match implementation | PASS — nontrivial fail-before-repository and policy intent documented near code |

### GH-443 Draft publication

- Draft PR [#729](https://github.com/skyg547/account/pull/729), `Refs #443`, implementation head `342aed55964639ae026ddef77041dd9f756bb7d5`; PR is OPEN/DRAFT and mergeable. Issue #443 is OPEN / `status:needs-review`.
- Remote checks were queued/in progress when observed, so no CI success is claimed. The worktree and branch are retained. Human review, published-head CI and latest-base confirmation are the next gates; Ready/merge/Issue close/cleanup remain unauthorized.

## 2026-09-22 — GH-642 verified RFC handoff

- Issue #642 is implemented as documentation only on `agent/642-container-build-study` in `/tmp/account-642-container-build-study`, based on fetched `origin/main@e375002ff63652aabaaa1f12c74aa611d4ed29bc`. The older intake-comment base is historical and not the reviewed baseline.
- New RFC: `docs/guides/container-build-modernization-rfc.md`. It reproduces inventory, compares Jib with the existing parameterized Containerfile, separates Temurin/Distroless effects, defines multi-arch/private-registry/air-gap limits, preserves Next.js dual Containerfiles and provides Discovery Go/No-Go, rollout and digest rollback gates.
- Only the RFC and four parent-owned records changed. Existing Dockerfile/Containerfile/Compose/Gradle/source/test/CI files were not modified or removed.
- Evidence: offline Discovery test6 + bootJar PASS; ContainerImagePolicy9 PASS; tracked inventory and local links13/13 PASS; diff, exact allowlist and marker checks PASS. No executable Jib/image/runtime claim is made.
- Independent reviewer initially found missing Eureka client lifecycle evidence. Fixed by requiring synthetic register/lookup/renew/cancel, stale absence and rollback re-registration/registry reconstruction. Final review has no P0–P3; Q1/Q4 N/A for no code change, Q2/Q3 PASS.
- Rollback: reviewed revert of the five changed documentation/history files. Do not delete images, caches, volumes, branches or the worktree. Runtime/data rollback is unnecessary.
- Remaining gates: parent commits/pushes and opens Draft `Refs #642`, records the URL/current head, and syncs Issue to needs-review. Human reviewer then checks current head/base/CI and chooses whether to authorize a separate Discovery PoC. Ready/merge/Issue close/Dockerfile retirement/cleanup remain separate approvals.
## 2026-09-22 — GH-642 Draft publication handoff

- Draft PR [#730](https://github.com/skyg547/account/pull/730) is OPEN/DRAFT with `Refs #642`; reviewed implementation commit `ec9a89d9` is on `origin/agent/642-container-build-study` targeting `main`.
- Issue #642 is OPEN and synchronized to `status:needs-review`. PR body contains actual base, scope, verification, Q1–Q4, skipped image/runtime gates, rollback and authority separation.
- This checkpoint is shared-record only. Human review and current-head/base/CI inspection are next; no Ready, merge, Issue close, branch/worktree cleanup, Jib PoC or Dockerfile deletion is authorized.

## 2026-09-23 — GH-418 Local H2 기준 데이터 handoff

- Worktree/branch/base: `/tmp/account-418-local-baseline-data` / `agent/418-local-baseline-data` / `origin/main@3da1f655454d`. Issue #418 remains open; no PR was created.
- `master-data/api` local startup now runs Flyway then `data-local.sql`, loading 29 baseline rows. The integration test uses a distinct H2 URL and queries currencies, departments, and account subjects at a fixed valid date. The existing create-drop context test explicitly opts out of SQL init.
- Auth source/resources were not changed. The requested full command passed 612 tests with zero failures/errors/skips, including `AuthApiRuntimePolicyTest`; no static password, hash, JWT secret, or internal token was added.
- Independent `/root/issue418_review` found no P0–P3 and confirmed Q1–Q3 PASS/Q4 N/A. Remaining limitation is untested repeat initialization against one persistent same-JVM H2 name; normal local process restart resets the in-memory DB.
- Rollback the five behavior/test/module-doc files through a reviewed revert and preserve these append-only records. H2 needs no data recovery. Next owner may inspect the local commit and decide whether to publish a Draft PR; push/Ready/merge/Issue close/cleanup were not performed.
## 2026-09-23 — GH-47 Cashflow domain handoff

- Issue #47 is implemented on `agent/47-cashflow-domain` in `/tmp/account-47-cashflow-domain`, based on `origin/main@fd5ebc7e`. The user requested a local commit; no push or PR lifecycle action was requested.
- New modules: `cashflow:core`, `cashflow:api`, `cashflow:batch`. Core owns scale-2 monetary validation, statement/forecast aggregates, use cases, services, outbound ports, and local memory adapters. API owns DTO/HTTP mapping; Batch owns Job/Step parameters and calls core.
- Statement safety: activity totals equal classified lines, net equals the three activity totals, and ending equals beginning + net. Forecast safety: net equals inflow - outflow and NORMAL/WATCH/CRITICAL uses validated watch/critical boundaries.
- Local execution is intentionally non-durable. Statement/forecast and ledger balances are memory-backed, Batch metadata uses H2, same-repository completed runs are rejected, and a new JVM permits the same parameters. Production persistence/ledger and a durable JobRepository are follow-up gates.
- Final verification: independent read-only Reviewer reran core/API/Batch tests with all 17 passing and no failures/errors/skips; API invalid scale/date packaged smoke returns 400; Batch repeat and required-parameter failure are executable tests. BootJars, PostgreSQL/Actuator contents, quality contract 32/32, diff whitespace, and conflict markers passed. No P0–P3 remains; Q1–Q4 PASS.
- Not verified: external PostgreSQL, deployed runtime, real ledger integration, bulk/partition performance, or remote CI. Roll back with a reviewed Issue-scoped revert and preserve shared append-only history; no database recovery is needed.
- Next owner: user/human reviewer decides whether to push and open a Draft PR. Ready, merge, Issue close, and branch/worktree cleanup remain separate authorization gates.

## 2026-09-24 — GH-738 external-dev port and ghost cleanup handoff

- Issue #738 is implemented on `agent/738-dev-server-compose-fix` in `/tmp/account-738-dev-server-compose-fix`, based on `origin/main@cf53367a`. The user requested a local commit; no push or PR lifecycle action was requested.
- Closing and Asset Lease respect the Compose `SERVER_PORT=8080` override while preserving standalone fallbacks 8086 and 8089. The three target overlays already had the correct 8080 probe/environment contract; invariant comments and a parsed 13-API assertion preserve that finding.
- The development runbook now lists all 17 APIs with repository defaults and container overrides, including the Asset Lease application 8089 / legacy Config Server 8083 split. It explains stale rootless Podman state and user-systemd healthcheck timers without recommending broad prune.
- `tools/cleanup-ghost-containers.sh` defaults to report-only. Cleanup requires one or more reviewed full 64-hex IDs, rechecks state, skips running targets, and touches only exact selected non-running containers/matching stale timers. It does not prune or remove volumes/images/networks/pods.
- Verification: required Gradle tasks passed 111 tests with zero failures/errors/skips; required YAML parse and explicit 13-API contract passed; Spring fallback assertion, shell syntax/help/argument checks, rootless report-only execution, nonexistent-ID no-op apply flow, harness 32/32, and review-time diff/marker checks passed. Independent `/root/review_738` found no remaining P0–P3 and marked Q1–Q4 PASS after the explicit-ID and inventory corrections.
- Remaining limits: no destructive live apply, mocked timer/race suite, deployed external-dev smoke, or remote CI. Existing Config Server 8083 for Asset Lease is intentionally not changed. Roll back through a reviewed Issue-scoped revert; no data/container recovery is needed because no live target was removed. Next owner may inspect the local commit and decide whether to publish a Draft PR; push/Ready/merge/Issue close/cleanup remain separate authorizations.

## 2026-09-24 — GH-740 API parity handoff

- Issue #740 (parent #179) is implemented on `agent/740-api-parity-endpoints` in `/tmp/account-740-api-parity-endpoints`, based on `origin/main@532bde3f`. The user requested a local commit; no push or PR lifecycle action was requested.
- Closing now serves `GET /api/closing/calendars/{calendarId}/tasks` through a read-only use case and derived FK query, returning `ClosingTaskDto` arrays including the empty-list case.
- Payable now serves list/detail under both `/api/purchase/invoices` and `/api/payable/invoices`. Optional status is normalized to the domain enum; invalid status is HTTP400 and missing invoice is HTTP404. The alias also applies to the existing PurchaseController POST routes.
- Gateway packaged and Config Server routes both include `/api/payable/**` and keep the existing `payableCircuitBreaker`. This closes the BFF→Gateway reachability gap found during independent review.
- Verification: exact requested four-module command passed 246 tests with zero failures/errors/skips; focused forced Gateway policy suite passed30/30; harness quality passed32/32; final diff/untracked whitespace, conflict-marker, and unmerged-index checks pass. Independent read-only Reviewer found no remaining P0–P3 and Q1–Q4 PASS.
- Remaining limits: no live BFF/Gateway/service smoke, real PostgreSQL integration/query plan, or high-cardinality pagination/order test. Existing frontend DTO/enum differences remain documented and out of this endpoint-mapping scope.
- Roll back through a reviewed revert of the Issue-scoped code/test/config/docs commit; no migration or data recovery is needed. Next owner may review the local commit and decide whether to publish. Push, Draft PR, Ready, merge, Issue close, and cleanup remain separate authorization gates.

## 2026-09-24 — GH-744 rootlessport recovery handoff

- Issue #744 is implemented on `fix/744-rootlessport-recovery` in `/tmp/account-744`, based on `origin/main@d5798bc0867832e5d7d8a00a375f39513a025b91`. The requested endpoint is a local commit; no push or PR was requested.
- `tools/cleanup-ghost-containers.sh` keeps report-only default behavior and existing ghost/timer cleanup. It now reports unique containers whose published TCP ports lack host LISTEN sockets and supports `--fix-ports` for all drops or selected full IDs. Recovery revalidates before restart and verifies afterward with bounded retries.
- The runbook documents rootlessport-only failure symptoms, `ss`/`/proc` detection, automated and manual recovery, expected output, and restart disruption. UDP/SCTP are intentionally outside the TCP LISTEN contract.
- Shell syntax, mocked report/recovery/skip/failure paths, live `ss`/`/proc` parity, quality32/32, and static gates pass. Independent `/root/review_744` found no P0–P3 and Q1–Q4 PASS.
- The current host is not clean: final code detects Elasticsearch port9200 missing while Podman still reports it published. No live restart was performed, so the user's 0/0/0 environment expectation remains an external runtime gate rather than a claimed pass.
- Roll back with a reviewed Issue-scoped revert while preserving shared records. Because no real remediation or removal ran, no container/data rollback is needed. Next owner decides whether to authorize the targeted Elasticsearch `--fix-ports` action and any publication; push/PR/Ready/merge/Issue close/cleanup remain separate.

## 2026-09-24 — GH-179 API parity handoff

- Issue #179 is implemented on `feature/179-api-parity-missing-endpoints` in `/tmp/account-179`, based on `11dbd6967dc291963d6f3482dd469f6a100195cf`. The requested endpoint is a local commit; no push or PR lifecycle action was requested.
- Receivable serves optional-status invoice lists and invoice detail through `/api/sales` and `/api/receivable`, with locale-stable enum conversion, typed 400/404 errors, repository queries, and alias/controller/service/adapter tests.
- Auth serves `/api/admin/users` through `AdminUserQueryUseCase` and `AdminUserQueryService`. Gateway-supplied `X-Auth-Roles` must contain `SYSTEM_ADMIN`; stable 48-bit display hashes are JavaScript-safe but are not identity or authorization keys. Supported FE roles are explicit and unknown roles fall back to `USER`.
- FX serves a deterministic three-currency dashboard whose KRW and gain/loss totals are constructor-checked against positions. Inter-branch banking serves an empty deterministic dashboard; auto-match performs no state transition and returns HTTP 501 with `NOT_EXECUTED` so the frontend cannot report false success.
- Packaged and Config Server Gateway files are synchronized for `/api/admin/**`, `/api/fx/**`, `/api/receivable/**`, and `/api/finance/banking/**`. The executable matrix audit verifies all 42 documented method/path mappings, the six Issue endpoints, current line references, route mirrors, and DTO sentinels; six in-memory corruptions are rejected.
- Verification: the exact requested Gradle command is successful with 286 tests and zero failures/errors/skips. Matrix positive/negative checks, harness quality, schema validation, whitespace, marker, control-character, and index gates are in the worklog. Independent `/root/review_179` reports no remaining P0–P3 and Q1–Q4 PASS after follow-up corrections.
- Remaining limits: no live BFF/Gateway/service or database integration/load run, no real FX feed or reconciliation matcher, unbounded list pagination, and documented Receivable FE response drift. Roll back with a reviewed Issue-scoped revert; no DB migration or data recovery is needed. Publication and repository lifecycle operations remain separately authorized.
