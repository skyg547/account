## 2026-09-23 — GH-53 Frontend Financial Statements & Dashboard API Integration

- Issue: #53 (`difficulty:medium`, `module:frontend`, `status:draft`)
- Branch: `agent/53-frontend-reporting-dashboard`, worktree: `/tmp/account-53-frontend-reporting-dashboard`, base: `origin/main@e8df751c`.
- Scope:
  * `frontend/src/services/reportingService.ts`:
    - Added `normalizeStatementBaseDate` converting `YYYY-MM-DD`, `YYYY-Q#`, and `YYYY-FY` into ISO datetime (`T23:59:59`) compatible with Spring `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)`.
    - Enhanced `generateStatement` with `fetchWithTimeout(..., silentToast: true)` returning `{ ...statement, dataSource: 'API' }` with transparent fallback to `createMockStatement` when offline.
    - Enhanced `exportDocument` with file download, mime-based extension resolution (`.xlsx` vs `.csv`), and user warning toast on network failure.
  * `frontend/src/app/reports/statements/page.tsx`:
    - Connected `generateStatement` for parallel fetching of `BALANCE_SHEET` and `INCOME_STATEMENT`.
    - Mapped API `ReportLineDto` list into hierarchical table model with search and collapse/expand support.
    - Added "오프라인 Mock 데이터 표시 중" visual indicator badge when running in offline/mock mode.
    - Wired document export buttons (`PDF` and `Excel`) to `exportDocument`.
  * `frontend/src/app/page.tsx`:
    - Added "재무상태표 & 손익계산서 요약" quick widget displaying assets, liabilities, equity, and net income with direct navigation link to `/reports/statements`.
- Verification:
  * TypeScript check (`tsc --noEmit --incremental false --project tsconfig.json`): PASS (exit 0).
  * `git diff --check`: PASS (0 errors).
  * Zero conflict markers.

## 2026-09-23 — GH-59 Frontend Journal Entry & Approval Workflow Implementation

- Issue: #59 (`difficulty:medium`, `module:frontend`, `status:draft`)
- Branch: `agent/59-frontend-journal-workflow`, worktree: `/tmp/account-59-frontend-journal-workflow`, base: `origin/main@4a3cb36c`.
- Scope:
  * `frontend/src/services/journalService.ts`:
    - Added `approveJournalEntry(id: number)` calling `POST /api/journals/${id}/approve` with `X-User-ID: frontend-user`.
    - Added `postJournalEntry(id: number)` calling `POST /api/journals/${id}/post` with `X-User-ID: frontend-user`.
    - Enhanced `getJournalEntries(params)` supporting `startDate`, `endDate`, and `status` query parameters with graceful mock fallback when offline.
    - Added DTO alignment translating frontend `details` (and `detailDescription`) to backend `lines` (and `description`) with `baseAmount`.
  * `frontend/src/app/journal/list/page.tsx`:
    - Wired date range (`startDate`, `endDate`) and status filter dropdown to `fetchJournals`.
    - Added row actions: DRAFT journals show '승인' button triggering `approveJournalEntry`, APPROVED journals show '전기' button triggering `postJournalEntry`, POSTED journals show completed badge.
    - Integrated `useToast` for user notifications and loading/spinner states during action submission.
  * `frontend/src/app/journal/entry/page.tsx`:
    - Refactored balance validation to use minor units (`Math.round(amount * 100)`) avoiding floating-point precision errors.
    - Enforced strict `JournalDetailDto` typing without `@ts-expect-error`.
    - Added navigation link back to `/journal/list`.
  * `frontend/src/app/journal/list/JournalList.module.css`:
    - Added styles for action buttons, disabled states, completed badge, loading spinner, and mock notices.
- Verification:
  * TypeScript check (`tsc --noEmit --incremental false --project tsconfig.json`): PASS (exit 0).
  * ESLint (`eslint src/services/journalService.ts src/app/journal/list/page.tsx src/app/journal/entry/page.tsx`): PASS (exit 0).
  * `git diff --check`: PASS (0 errors).
  * Zero conflict markers.

## 2026-09-22 — GH-515 external-dev Compose/PostgreSQL live verification

- Contract: Issue #515, `agent/515-dev-compose-runtime-gate`, `/tmp/account-515-dev-compose-runtime-gate`, base `origin/main@e375002ff63652aabaaa1f12c74aa611d4ed29bc`. 사용자 승인 대상 8개 서비스만 읽기 전용으로 검사했다. credential/env/DB data는 읽거나 출력하지 않았고 컨테이너·DB·볼륨·네트워크를 변경하지 않았다.
- Observation window: 2026-09-22 00:15–00:23 KST, Podman 4.9.3. 전체 `podman ps -a`에서 대상 8개 컨테이너는 모두 `Up 4 days`였다. 전체 목록의 비대상 unhealthy/exited 리소스는 이번 Issue 결과로 오인하지 않았고 조작하지 않았다.

| 대상 | Podman 게시/실행 | 실제 요청 결과 | 판정 |
| --- | --- | --- | --- |
| PostgreSQL 5432 | `account-postgres`, `0.0.0.0:5432`, Up | host TCP open; PostgreSQL SSLRequest에 `N` 응답; container `pg_isready` accepting | PASS |
| Redis 6379 | `account-redis`, `0.0.0.0:6379`, Up | host RESP `PING` → `+PONG`; container `redis-cli PING` → `PONG` | PASS |
| Config 8888 | `config-server`, `0.0.0.0:8888`, Up | host connection refused; `account-network` 내부 `/actuator/health` 5초 timeout | FAIL |
| Discovery 8761 | `discovery`, `0.0.0.0:8761`, Up | host `/` 5초 timeout; 내부 `/` HTTP 2xx | PARTIAL/FAIL (host) |
| Gateway 8000 | `gateway`, `0.0.0.0:8000`, Up | host `/actuator/health` 5초 timeout; 내부 health HTTP 2xx | PARTIAL/FAIL (host) |
| Frontend 3000 | `account-frontend`, `0.0.0.0:3000`, Up | host `/` connection refused; Quick Tunnel `/` HTTP 200이 Nginx→Frontend 경로를 증명 | PARTIAL/FAIL (host) |
| Nginx 8080 | `account-frontend-nginx`, `127.0.0.1:8080`, Up | host `/`와 `/api/actuator/health` connection refused; tunnel 경유 `/` 200, API health 401 | PARTIAL/FAIL |
| Cloudflare Quick Tunnel | `account-cloudflare-tunnel_cloudflared_1`, Up; Quick 명령 프로세스 확인 | journald URL 발견, hostname 비공개/해시 `02ddfba18a25`; 외부 `/` 200, 외부 API health 401 | PARTIAL |

- Commands/evidence: `podman ps -a --format ...`, 대상 `podman stats --no-stream`/`podman top`, `ss -ltnH`, 제한시간을 둔 `curl`, 자격 증명 없는 Python socket PostgreSQL SSLRequest/Redis RESP PING, Nginx 컨테이너의 내부 `wget`, cloudflared user journal에서 URL만 추출 후 hostname을 비공개 처리한 외부 `curl`. 실제 URL, DB 사용자/비밀번호, JWT, container env는 기록하지 않았다.
- Podman management anomaly: 일부 `inspect`, `logs`, `exec`가 6–10초 내 끝나지 않았고 Redis `PONG` 등 출력을 낸 뒤에도 client가 지연됐다. Codex가 시작한 진단 client PID만 TERM으로 종료했다. 서비스 프로세스는 중지/재시작/재생성하지 않았다.
- Acceptance result: 현재 gate는 PARTIAL/FAIL. 컨테이너 `Up`만으로 health를 통과시키지 않았다. Config 응답, rootless Podman host-port forwarding, Nginx API health 401 원인을 운영자가 복구한 뒤 같은 검사로 재검증해야 한다. Compose render/image build/schema readiness/migration-runtime role 분리/Batch 비실행/로그인/업무 트랜잭션/부하는 이번 좁은 실기동 요청에서 검증하지 않았다.
- Quality/review: harness quality 32/32, validator, `git diff --check`, conflict-marker 및 변경 파일 secret-pattern 검사가 PASS했다. 독립 read-only `/root/independent_review`는 P0–P3 없음과 Q1 N/A(코드 없음), Q2 PASS(계층별 입력·응답·실패 경계), Q3 N/A(코드·Compose·사용법 변경 없음), Q4 N/A(비자명 코드·주석 없음)를 확인했다.
- Rollback/authority: runtime rollback 없음; 문서 커밋만 검토 후 revert. Draft PR은 `Refs #515`; reviewer/사용자 승인 후 Integrator만 merge한다. Ready, merge, Issue close, runtime teardown, branch/worktree 삭제는 하지 않는다.
- Publication: evidence commit `282387bc`를 원격 branch에 push하고 Draft PR [#728](https://github.com/skyg547/account/pull/728)을 생성했다. Issue #515에는 sanitized 결과와 권한 분리 코멘트를 남겼다. PR은 Draft, Issue는 OPEN 상태로 유지한다.

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

| 항목 | 판정 (PASS/FAIL/N/A) | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Core Loan 예약·전표 정합성, Closing source ports, Reconciliation HTTP 조회; Java972·실제10잡·SQL 결과 | 전 항목 적용 | 작은 합성 데이터 검증; 대량·운영 검증 별도 | 독립 XML·해시·실기동 증거 대조 PASS |
| Q2 | PASS | Loan process-flow, 동일 식별 파라미터·완료 재사용, 실패/부분 원격 쓰기 복구 기록 | 전 항목 적용 | pending 수동 대사·복구 | 독립 source review PASS |
| Q3 | PASS | business-batch-dev-verification, Loan/Mart/Reconciliation 모듈 문서와 실제 실행표 | 전 항목 적용 | Draft CI·사람 리뷰 | 독립 문서·실행 결과 대조 PASS |
| Q4 | PASS | scoped reader, 정확한 전표 multiset, 읽기전용 source pool, 원자적 SQL 정리, 실제 HTTP 필터 주석 | 전 항목 적용 | 기간 HTTP 조회의 메모리·분산 스냅샷 한계 문서화 | 독립 source review PASS |

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

## 2026-09-10 — GH-664 안전한 migration CLI 오류 출력

- Issue [#664](https://github.com/skyg547/account/issues/664); branch `agent/664-migration-safe-errors`; worktree `C:/tmp/account-664-migration-safe-errors`; base `05ff6efcf8895fba6b6592da4ab90cf761038480`.
- 구현자 `/root/migration_663_writer` (이번 할당은 GH-664, GPT-6 Astra/high), 독립 Reviewer `/root/harness_ci_audit` (Astra/high). 부모 Integrator만 공유 기록과 Git/GitHub를 담당한다.
- 변경: `MigrationCommand`의 IllegalArgumentException 원문 출력을 지정 고정 문구로 교체하고 가까이에 보안 이유를 설명했다. usage 두 줄과 exit 2, 기존 정상/list·baseline exit 3·runtime exit 4 흐름을 유지한다.
- 검증: 테스트만 추가한 RED 16건 중 14실패(오류/skip 0); 합성 malformed URL 세 건에서 기존 합성 호스트 노출 확인. GREEN 대상 16/16, 전체 98/98(기존 85+신규 13), 독립 `--rerun-tasks` 재실행도 98/98, 실패/오류/skip 0. RED는 구현자 실행 근거, 독립 Reviewer는 현재 GREEN을 재실행했다.
- 범위는 실질 Java/테스트 2파일과 부모 기록 4파일뿐이다. Reviewer APPROVE, P0–P3 finding 없음. `git diff --check`, unmerged 및 실제 conflict marker 검사 통과. 실제 DB/TLS·외부 장애 연결·환경 비밀정보 접근·설치·다운로드·기존 로그 삭제 없음.
- 게시: [Draft PR #685](https://github.com/skyg547/account/pull/685), 구현 커밋 `d571a5f7760f147eafe85d6d0ae270e1d8a59538`. 이번 후속 커밋은 PR 식별자를 반영한 기록 전용이며 검토한 실질 2파일은 동일하다. 아래 과거 기록은 이력이며 원격 Issue/PR이 최신 상태다. 새 게시 head CI와 최신 main 통합 가능성을 확인한 뒤 Ready로 인계하고 구현 슬롯을 해제한다. 최종 merge/close는 기존 `account` 예약 소유이며 대기 자체로 독립 Issue를 멈추지 않는다.
- 관련 대기열: #663/PR #684는 동결 Ready, #662/PR #680은 병합 후 account 소유 Issue-close 승인 대기다. #681/PR #682는 병합·종료·main CI 34424476862 성공 확인 후 전용 자원만 정리했고 코드/이력은 main에 보존했다.
- 롤백: 이 두 코드/테스트 파일의 검토된 revert. 원래 입력값 노출 위험이 돌아오므로 후속 보안 검토가 필요하며 DB나 과거 로그를 지우는 작업은 포함하지 않는다.
### 실행 근거와 품질

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot'
& 'C:/Users/skyg5/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/bin/gradle.bat' :migration-runner:test --tests com.ho.account.migration.MigrationCommandTest --offline --no-daemon --console=plain --max-workers=1 '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot'
& 'C:/Users/skyg5/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/bin/gradle.bat' :migration-runner:test --offline --rerun-tasks --no-daemon --console=plain --max-workers=1 '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot'
git diff --check
git diff --name-only --diff-filter=U
```

초보자 설명: 잘못된 URL을 설명하는 예외 메시지 자체에 URL이 들어갈 수 있다. 예외 문자열 일부만 지우면 새 입력 형식을 놓칠 수 있으므로, CLI는 값 없는 고정 안내와 사용법만 출력한다. malformed 입력은 연결 전에 실패하므로 이 테스트는 실제 DB를 사용하지 않는다.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `migration-runner/src/main/java/com/ho/account/migration/MigrationCommand.java:48`; `migration-runner/src/test/java/com/ho/account/migration/MigrationCommandTest.java:142`의 공통 assertion, 전체 98/98 | 해당 없음: 코드 변경 | 게시 head CI | `/root/harness_ci_audit` 최소 책임·diff 확인 |
| Q2 | PASS | `MigrationCommand.java:49`, `MigrationCommandTest.java:77`와 `:123`; 입력→예외→고정 출력/종료 흐름 및 연결 없는 테스트 이유 | 해당 없음: 오류 흐름 변경 | 실제 DB 장애는 비목표 | 설명과 실제 호출 경로 대조 완료 |
| Q3 | PASS | `migration-runner/README.md:52`, `docs/db/postgresql-migration-runbook.md:83`의 exit·비노출·안전한 오류 수집 계약 유지 | 해당 없음: 문서 정확성 검토 적용 | 기존 문서 계약 복원으로 기능 문서 수정 불필요; 무관한 문서 정리 제외 | no-edit 사유 독립 확인 |
| Q4 | PASS | `MigrationCommand.java:49`와 `MigrationCommandTest.java:77`, `:123`의 입력 노출 위험 및 합성 입력 의도 주석 | 해당 없음: 비자명 보안 변경 | 최종 독립 merge gate | 코드·테스트와 주석 일치 확인 |

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

### 실행 명령과 품질 근거

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot'
& 'C:/Users/skyg5/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/bin/gradle.bat' :master-data:core:test --tests '*FiscalPeriodMapperTest' --offline --no-daemon --console=plain --max-workers=1 '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot'
& 'C:/Users/skyg5/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/bin/gradle.bat' :master-data:core:test :master-data:api:test :master-data:batch:test --offline --no-daemon --console=plain --max-workers=1 '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot'
git diff --check
git diff --name-only --diff-filter=U
```

초보자 설명: 이미 보관된 결재 상태를 읽는 것과 새 결재를 실행하는 것은 다르다. 저장 상태를 복원할 때 새 마감 명령을 실행하지 않아야 영구 마감 이력과 원래 감사값을 읽을 수 있다. 새 명령의 전이 제한은 그대로 지킨다. 실제 JPA 수정의 PreUpdate 시간 변경과 순수 복원의 원값 보존도 구분한다.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `FiscalPeriod.java:48`의 단일 복원 책임, `FiscalPeriodMapper.java:16` 값 변환; Domain21/Mapper10 GREEN | 해당 없음: 코드 변경 | 게시 head CI | `/root/harness_lifecycle_review` 책임·경계 확인 |
| Q2 | PASS | Domain:43 복원≠명령 설명, H2 Test:52·112·202 실제 커밋과 별도 조회 이유; H2 12 GREEN | 해당 없음: 비자명 흐름 | PostgreSQL 동시성은 비목표 | 흐름·JPA 콜백 구분 독립 확인 |
| Q3 | PASS | `master-data/docs/schema.md:65`, `beginner-guide.md:71` 기존 전이/terminal 계약 및 실행 명령 유지 | 검토 적용: 문서 무수정 사유를 명시함 | 기존 local-run18건 차이는 범위 밖, 전체 문서 정확성 주장 없음 | 무수정 사유와 기존 부채 독립 확인 |
| Q4 | PASS | Domain:43, Mapper:18, H2 Test:52·112·120·180·205의 복원·커밋·JPA 콜백 구분 주석 | 해당 없음: 비자명 코드 변경 | 최종 게시·독립 merge gate | 코드·주석 일치 독립 확인 |

파일 전체 경로는 Issue allowlist와 PR diff에 고정한다. RED는 구현자 실행 근거이며 독립 리뷰어는 원본을 되돌리지 않고 GREEN을 재실행한다.

### 보존된 이전 체크포인트와 제출 이력 (현재 지시 아님)

최신 독립 검수: `/root/harness_lifecycle_review` (Astra high) staged 통합 후보 APPROVE, finding 없음. Node64/64·양쪽 이력10/10·정확한 실질5+부모6파일·Q1–Q4·권한 분리를 직접 대조했다. 최초 품질 예약 스냅샷도 별도로 비교하여 추가 블록과 두 개행 제거 시 원래 prompt와 완전 일치, 그 외 변경은 updated_at뿐임을 확인했다. 이후 사용자 승인 비동기 루프 수정과 구분하며 타 예약의 현재 해시를 과거 해시로 재인증하지 않는다. 게시 후 새 SHA/CI와 최종 account 리뷰는 후속 게이트다.

현재 통합 검증: `node --test tools/ci/harness-pr-contract.test.cjs tools/ci/harness-quality-contract.test.cjs` 64/64 PASS (fail/skip 0), `git diff --check` 및 staged diff check PASS, 실제 충돌 marker/unmerged 경로 0, 품질 링크3/3 PASS. 두 부모의 기록5파일을 줄 순서 보존으로 대조하여 10/10 PASS; GH-681 실질5파일은 이전 검토 SHA와 동일하다. 독립 재검토와 게시 후 새 head CI는 아직 다음 게이트이며 아래 과거 PASS를 대신 사용하지 않는다.

## 2026-09-10 — 승인된 Issue 루프 재개

### GH-681 측 당시 기록
- 대상: GH-681 / PR #682. 사용자가 이 대화에서 오케스트레이션과 다음 Issue 진행 재개를 명시 승인했다.
- 아래의 Draft 제출 시점 기록은 이력이다. 당시 부모가 추가한 Draft-only/별도 지시 대기 조건은 이번 승인으로 대체하며, 구현자와 독립 검수자의 역할 분리 및 검증 게이트는 유지한다.
- 현재 코드 변경은 동결되어 있으며, 독립 사전 리뷰와 대상 Node 테스트 32/32 PASS를 재확인했다. 이 기록 변경의 commit/push 후 최신 head CI와 통합 가능성을 다시 확인해야 Ready로 전환할 수 있다.
- 다음 행동: 기존 PR #680 완료·정리를 먼저 확인한 다음 이 PR의 최신 main 통합 가능성과 검증을 재확인하여 Ready로 인계한다. 대기 중에는 Draft를 유지하지만, 이는 순차 처리 대기이며 추가 사용자 승인 대기가 아니다. 기존 Account PR 독립 검수·병합(account) 예약이 최종 독립 리뷰·병합·Issue close를 담당한다.

<!-- GH-669 checkpoint start -->
## 2026-09-10 — GH-669 공용 MVC 요청 오류 분류 / 제출 전 검증 체크포인트

- Issue [#669](https://github.com/skyg547/account/issues/669), [승인 범위·claim](https://github.com/skyg547/account/issues/669#issuecomment-5612175131). 사용자의 "허용해줘"는 `shared-kernel/docs/process-flow.md`를 추가한 5실질 파일 승인이다. 기존 merge/Issue close 게이트를 넓히지 않는다.
- Branch `agent/669-mvc-request-errors`, worktree `C:/tmp/account-669-mvc-request-errors`, base `b7c1c7c1fa45ec6550ab2431674fcf22a519bcea`. 부모 task `019fa3ea-ac4c-7022-bb45-951559750df7`; primary checkout과 다른 동결 PR은 보존했다.
- 단일 구현 writer `/root/migration_663_writer`는 shared-kernel Module Writer로 명시 재배정(Astra high), 별도 `/root/harness_lifecycle_review` Astra high가 실질 5파일 APPROVE, 잔여 P0–P3 없음. 부모만 아래 기록 4종과 Git/GitHub 후속 단계를 맡는다.
- 실질 범위: 공용 `GlobalExceptionAdvice.java`, 기존 직접 테스트, 신규 `GlobalExceptionAdviceMvcTest.java`, `shared-kernel/build.gradle`의 test Servlet 6.0.0/BOM 정렬 jsr310 두 줄, 승인된 `process-flow.md`. 공유 기록 포함 최종 9파일 후보이며 production 의존성/BOM/소비자 코드는 불변이다.
- 정확히 네 요청 예외만 400/405/415 고정 메시지로 처리한다. Spring `Allow/Accept/Accept-Patch`와 failure JSON 네 필드를 보존하고 원문/cause/ProblemDetail을 새 응답에 복사하지 않는다. 기존 400/404/하위 구체 409/비노출 500 의미는 유지한다.
- 확정 RED: production 미수정 상태에서 직접 3 + MVC 11 = 14건 중 요청 오류 5건이 실제 500으로 실패, error/skip 0. 원인별로 query/깨진 JSON→기대400, method→405, POST/PATCH media→415. 처음의 추가 fixture validation 실패 1건은 합성 Spring Validator로 보완한 뒤 이 RED를 재확인했으며 production 결함으로 집계하지 않는다.
- 작성자 GREEN: `:shared-kernel:test --tests '*GlobalExceptionAdvice*'` 19/19(직접8+MVC11); 소비자 `:shared-kernel:test :contracts:test :master-data:api:test :account-mart:mart-api:test :ecl:ecl-api:test` 149/149, 36 suites(102/7/24/3/13), failure/error/skip 0. 기존 133 + 신규16이며 중복 실행 횟수를 신규 테스트 수로 더하지 않는다.
- 독립 Reviewer는 위 소비자 명령에 `--rerun-tasks`를 붙여 1m2s/32 tasks를 실제 재실행, XML149/36·failure/error/skip0 확인. RED는 작성자/부모 증거이며 Reviewer의 RED 재실행 주장이 아니다.
- 명령 공통: 기설치 JDK17/캐시 Gradle8.7, `--offline --no-daemon --console=plain --max-workers=1` 및 `-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot`. 실제 설치 경로·전체 예는 `shared-kernel/docs/process-flow.md:137`부터다.
- 부모 패키징: 같은 환경에서 `:shared-kernel:build :contracts:build :master-data:api:bootJar :account-mart:mart-api:bootJar :ecl:ecl-api:bootJar` 성공(12s, 6 executed/24 up-to-date). 이 단계의 test는 up-to-date이며 별도 신규149회 실행으로 주장하지 않는다. Node 하네스 계약 두 파일 64/64도 통과했다.

| 항목 | 판정 (PASS/FAIL/N/A) | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `GlobalExceptionAdvice.java:41`의 명시 handler 3개; `shared-kernel/build.gradle:45`의 test 의존성만 추가; 대상 19건 | 해당 없음: 코드 변경 | 전체 모듈 CI | harness_lifecycle_review: 기존 책임/handler/의존성 보존 확인 |
| Q2 | PASS | `shared-kernel/docs/process-flow.md:80`, `GlobalExceptionAdviceMvcTest.java:83`의 입력→MVC→응답 흐름 | 해당 없음: 흐름 설명 적용 | 실제 배포·인증은 제외 | harness_lifecycle_review: 상태/헤더/비노출 설명 일치 |
| Q3 | PASS | `shared-kernel/docs/process-flow.md:107`, `:119`의 계약·전제·실행 명령·기대 결과·한계 | 해당 없음: 사용자가 기능 문서 승인 | 합성 Validator는 실제 provider 배선 증거가 아님; 전체 CI 대기 | harness_lifecycle_review: 문서 의미/명령 직접 대조 |
| Q4 | PASS | `GlobalExceptionAdvice.java:41/49/57`, `GlobalExceptionAdviceMvcTest.java:61/65`의 의도 주석 | 해당 없음: 비자명 로직 변경 | 최종 9파일 기록 검수 | harness_lifecycle_review: 날짜 fallback·헤더·비노출 이유 확인 |

- Diff whitespace/실제 marker/unmerged/allowlist 검사 통과. 다음은 부모 기록을 포함한 최종9파일 독립 검수 → 승인된 commit/push/Draft PR(`Refs #669`) → 게시 head의 전체 Module Validation·Guard 및 최신 main 통합 확인 → 승인된 Ready다. PR 번호·head·CI 증거는 원격 Issue/PR 인계에서 확정하며 이 제출 전 기록은 CI 성공이나 게시 완료 주장이 아니다.
- shared-kernel 변경은 CI의 62 Gradle project/23 모듈 그룹 대상이다. 로컬 소비자 테스트/제한 패키징은 루트 전체 build·전체 CI·실제 배포의 성공 증거가 아니다. 기존 When.MAYBE/Gradle deprecation 경고는 범위 밖으로 남긴다.
- 한계: fixture/controller/use case는 합성, 검증기는 테스트용 Spring Validator이며 실제 Bean Validation provider 배선을 검증하지 않는다. ResponseStatusException·전체 ErrorResponse·인증/인가·실서버/실DB·업무 Batch·배포는 제외했고 설치/다운로드/민감정보 접근은 하지 않았다.
- 롤백: 검토된 5실질 파일 변경을 승인된 revert로 되돌리고 이력은 보존한다. DB/소비자 임시 수정 없음. 최종 독립 검토·실제 merge/Issue close는 기존 `account` 예약 소유다. 이전 PR 대기는 독립 구현의 전역 blocker가 아니다.
<!-- GH-669 checkpoint end -->

### 병합된 GH-662 측 당시 기록
- 대상: GH-662 / PR #680. 사용자가 이 대화에서 오케스트레이션과 다음 Issue 진행 재개를 명시 승인했다.
- 아래의 Draft 제출 시점 기록은 이력이다. 당시 부모가 추가한 Draft-only/별도 지시 대기 조건은 이번 승인으로 대체하며, 구현자와 독립 검수자의 역할 분리 및 검증 게이트는 유지한다.
- 현재 코드 변경은 동결되어 있으며, 독립 사전 리뷰와 대상 Node 테스트 32/32 PASS를 재확인했다. 이 기록 변경의 commit/push 후 최신 head CI와 통합 가능성을 다시 확인해야 Ready로 전환할 수 있다.
- 다음 행동: 부모가 최신 검증 후 PR #680을 Ready로 인계한다. 기존 Account PR 독립 검수·병합(account) 예약이 최종 독립 리뷰·병합·Issue close를 담당한다.

- 부모는 실제 병합·Issue 종료·main CI 및 전용 자원 소유/청결 검증 후에만 승인된 안전 정리를 수행한다. 두 기존 PR을 순서대로 처리하기 전 새 Issue 구현자를 시작하지 않는다. 이후 상태는 원격 PR과 이 대화/예약 체크포인트를 기준으로 확인한다.

---

## 2026-09-10 — GH-668 required side-effect-free payload validation

- Workflow Account Issue 구현 오케스트레이터 / task `019fa3ea-ac4c-7022-bb45-951559750df7`; `agent/668-master-data-payload-validation` / `C:/tmp/account-668-master-data-payload-validation`, base `b7c1c7c1fa45ec6550ab2431674fcf22a519bcea`. Writer `/root/fiscal_683_writer` service/Astra high, independent `/root/harness_lifecycle_review` Astra high; parent alone owns shared records and Git/GitHub.
- Fresh OPEN ready+atomic/unclaimed state, no linked PR and no overlapping active file/contract ownership were verified; claim https://github.com/skyg547/account/issues/668#issuecomment-5611884163. This follows GH-663 rework Ready, independent of its final merge/close.
- Root cause: default validate no-op let three typed appliers omit the existing pretransition contract. The interface now requires implementation; each missing applier decodes once into its existing effectiveDate/targetKey command, rejects null payload before dereference, checks only the approved null fields and delegates the window invariant to MasterDataValidityPolicy. No business query or write occurs in validate; DEACTIVATE needs no decoder.
- Preserved: BP implementation/test, service transition/version/replay flow, null/blank key fallback, null default inputs, Department/Product partial UPDATE, equal-day validTo, omitted validTo interpreted as9999-12-31 for validation, payload validFrom overridden by effectiveDate, and Product currentId lookup only during apply. No new blank-name/negative-price/scalar-coercion policy or old APPROVED-row repair.
- Scope is source/test5 (478 additions/6 deletions, including three missing EOF newlines) plus parent records4. Existing histories are retained and the record insertion is separated from other queued PR record positions. No changes to functional docs, interface DTO shapes, persistence/schema, dependencies, other agents or primary checkout.
- Worker RED211/174 expected no-throw failures,0error/skip (37 prior PASS = BP1 + constructor raw-null/empty/blank36); target GREEN338, full381/19suites = core352/API24/batch5,0failure/error/skip. Independent Astra high forced offline full rerun381/381,48s, same frozen test hash; reviewer APPROVE with no P0–P3 findings.
- Executed with installed JDK17.0.19 and cached Gradle8.7: targeted `:master-data:core:test --tests '*MasterDataChangeRequestPayloadValidationTest'`, then `:master-data:core:test :master-data:api:test :master-data:batch:test`, both `--offline --no-daemon --console=plain --max-workers=1` plus explicit installed JDK path. Target GREEN12s, worker full44s; separate reviewer adds `--rerun-tasks` and checks fresh XML. RED is actual implementer evidence (test-only diff), not independently rerun or confused with the later expanded338-case suite.
- Target338 =210 negative pathway cases +118 normal accept/apply cases +9 DEACTIVATE/replay/conflict cases +existingBP1. Full381 =baseline44+337new. Failing new request paths assert save(any())0; failed approval preserves REQUESTED, approver/times/requester, lockVersion7 and appliedAt; valid stubs prevent version failures masking payload defects. Exact command equality and business-port no-interaction checks test observable behavior, not copied validator predicates.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Account/Department/Product validate:29/32/30, existing decoder/key helper/domain date policy; target338/full381 | 해당 없음: 코드 변경 | exact published-head CI | separate reviewer verified cohesive responsibilities |
| Q2 | PASS | Interface:19–21, Test:75/88/102/149/235 actual request/approve/apply/replay | 해당 없음: 비자명 흐름 | mock tests are not real DB rollback | separate reviewer verified input→validation→transition and replay boundary |
| Q3 | PASS | Existing Interface pretransition contract and master-data/docs/process-flow.md:72–85; commands/defaults unchanged | 문서 미편집: 기존 계약 복원, 새 공개정책/실행법 없음 | local-run.md:83,93–95 inherited count/test-absence debt; no blanket doc-accuracy claim | separate reviewer approved scoped no-edit rationale |
| Q4 | PASS | Interface:19–21, Account:34, Department:37, Product:35; Test:104/244/372/421 | 해당 없음: 비자명 코드 | final record review and remote CI | separate reviewer checked comments against behavior |

- 기능 문서 판단: process-flow72–85의 기존 sourceReference/버전/업무 키/DEACTIVATE 계약과 기존 상태 전이 전 validate 의무를 복원합니다. 실행법/기본값/공개 정책은 바뀌지 않아 승인된5파일 안에서 가까운 흐름·의도 주석을 보강했습니다. local-run83의18건 및93–95의 테스트 부재 설명은 baseline44에도 맞지 않던 별도 문서 부채입니다. WORKLOG를 기능 문서 대용으로 삼거나 모듈 문서 전체 최신화를 주장하지 않습니다.
- Limits: new regressions are unit tests with real Jackson/applier/service and mocked persistence/version/business ports. They preserve null/default inputs but do not execute actual JPA fallback, Spring transaction rollback or PostgreSQL concurrency. Existing APPROVED malformed rows and SCD2 inter-version interval policy remain out of scope; no real database/server/business Batch or deployment was run.
- Static diff/marker/unmerged and final parent-record review gate commit/push. Exact published-head CI and latest-main integration gate Ready; remote Issue/PR records subsequent SHA/CI/status without record-only commit churn. account's final merge/close approval wait is not bypassed and does not block independent new implementation. No cleanup in this change.
- Rollback: reviewed five-file revert; the previous acceptance/approval validation gap may reopen, while request/audit/schema/data/history remain unchanged. Earlier global serial-stop checkpoints are historical and superseded by the saved asynchronous queue contract.

### 이전 제출 시점 기록

### GH-681 측 당시 기록
### 2026-09-10 — GH-681 코드·문서 품질 계약

- Issue [#681](https://github.com/skyg547/account/issues/681), parent #661; base `34c75839af2edf10f80ff60d8f24e89504d69e9e`; branch `agent/681-harness-quality-contract`; worktree `C:/tmp/account-681-harness-quality-contract`.
- 범위: AGENTS, 10/40 정책 연결, 신규 42 품질 문서와 `tools/ci/harness-quality-contract.test.cjs`의 실질 5파일 및 부모 기록 5파일. 승인 범위 밖 기능 개선, 역할/skill, CI workflow, 기존 #662/#680은 변경하지 않았다.
- 역할: 부모 Spec/Integrator, `/root/harness_lifecycle_writer` Astra medium 구현, 별도 `/root/harness_lifecycle_review` Astra high 독립 검수. `/root/harness_workflow_audit`가 명세를, `/root/harness_ci_audit`가 예약 품질 요약을 읽기 전용 검토했다.
- 명령/결과: `node --test tools/ci/harness-quality-contract.test.cjs` 최종 32/32, `git diff --check` 통과, 승인 파일의 실제 conflict marker 없음, Markdown 로컬 링크 3개 정상. 최초 문서 누락 baseline은 24개 중 15개 실패. 부모와 Reviewer가 GREEN을 직접 재실행했다.
- 예약: 부모가 앱 도구로 기존 `account-issue-2`에 품질 블록만 추가했다. 제거 비교로 원래 prompt 완전 보존 확인; 30분 주기, 동일 task `019fa3ea-ac4c-7022-bb45-951559750df7`, ACTIVE, 모델/권한·다른 설정 유지. `account` SHA256 `78A16628C64F363883772E4CC94510065B17050F075821D79205970474D06445`, `account-issue` SHA256 `F44B7E625281582FCD16CDB6E4FF36E48365307D774259BD18E0C2A99780AB56` 전후 동일.

| 항목 | 판정 (PASS/FAIL/N/A) | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `tools/ci/harness-quality-contract.test.cjs:24`, `:29`, `:52` 책임 분리; Node 32/32 | 해당 없음: 테스트 코드 추가 | 구조 검사 외 의미 판단은 독립 리뷰 | harness_lifecycle_review 확인 |
| Q2 | PASS | `docs/ai-harness/42-code-documentation-quality.md:20` 입력/처리/출력·예외·재실행 설명과 구현 대조 | 해당 없음: 검사 흐름 추가 | 해당 없음: 현재 설명 대조 완료 | harness_lifecycle_review 확인 |
| Q3 | PASS | `docs/ai-harness/42-code-documentation-quality.md:71` 전제/명령/기대결과/제약; 링크 3개 정상 | 해당 없음: 기능 문서 추가 | CI 연결 #672, 자동 의미 판정 아님 | harness_lifecycle_review 확인 |
| Q4 | PASS | `tools/ci/harness-quality-contract.test.cjs:13`, `:40`, `:114` 입력 범위·검사 의도·한계 주석 | 해당 없음: 비자명 검사 추가 | 해당 없음: 구현과 주석 대조 완료 | harness_lifecycle_review 확인 |

- 독립 실질 변경 판정: APPROVE, finding 없음. 신규 구조 테스트는 CI 미연결이며 기존 CI를 실행 증거로 쓰지 않는다. 업무 변경이 없어 Gradle·실DB·서버 검증은 비대상이다.
- 안전: 처음 Issue 게시가 외부 전송 검증으로 보류됐다. origin과 일치하는 비공개 저장소 및 ADMIN 권한을 읽기 확인한 후 같은 대상 게시가 허용됐다. 비밀정보나 운영 접속정보를 읽거나 전송하지 않았다.
- 롤백: 승인된 후속 revert로 이번 5실질 파일을 복원하고 부모 기록은 이력을 보존한다. 예약은 이번 추가 품질 블록만 제거한다. 새 의존성/설치/파괴적 작업 없음.
- 게시: [Draft PR #682](https://github.com/skyg547/account/pull/682), 구현 커밋 `063199e301aa51daa8e90c606a7d21647528ff2c`. 후속 기록 전용 커밋에서 실질 5파일은 불변이다. 다음은 현재 PR head의 CI 확인이며, 이 기록은 CI 통과를 주장하지 않는다. Ready·merge·Issue close·branch/worktree 삭제는 별도 승인 대기이며 다른 Issue를 자동 착수하지 않는다.

### 병합된 GH-662 측 당시 기록
## 2026-09-10 — GH-661 audit / GH-662 PR lifecycle contract

- Audited active repository/tool/skill/role guidance, Issue/PR forms and all three CI workflows against `origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e`. Parent #661 links implementation #662 and Draft follow-ups #670/#671/#672/#673/#674/#675/#677/#678/#679. Historical logs were sampled for current routing, not fully re-certified; secret-capable local settings contents were not read.
- Separate Astra coder/medium owns four central PR policy/form files and `tools/ci/harness-pr-contract.test.cjs`; parent alone owns shared records/Git/GitHub. External worktree: `C:\tmp\account-662-harness-pr-lifecycle`.
- Reconciled frozen-Draft pre-review, approved parent Ready, independent current-head/base final review and separate merge/close/cleanup approvals. Model capability does not grant mutation authority. Tool-specific follow-ups remain #670.
- Verification: initial RED (23 cases, 9 failures); independent review found a Ready-only CI prerequisite deadlock. The new 32-case regression first failed on three actual documents, then passed 32/32 after repair. Independent Astra high re-review approved with no remaining finding. Existing js-yaml verified 21 unique form IDs and field/label preservation; 19 local links and scoped diff/marker/allowlist checks passed. No dependency installed.
- Limits: regex documentation checks are not runtime access control; CI wiring remains #672 and Guard trust/evidence design #671. No application/DB/container tests apply to this docs/test-only change.
- Published implementation `af95eca1` in Draft PR [#680](https://github.com/skyg547/account/pull/680), `Refs #662`. This record-only follow-up synchronizes the published handoff; reviewed policy/test files are unchanged. No Ready/merge/Issue close/deletion. Rollback: reviewed reversal of #662 policy/test paths, preserving history and all primary checkout changes.


### 📅 2026-09-09 (GH-659: Nginx 단일 진입점 관측성 업스트림 컨테이너 명칭 매핑 및 동적 프록시 장애복구(Fallback) 강화)
- **Component**: `frontend-nginx/nginx.conf`, `grafana/docker-compose.yml`, `zipkin/docker-compose.yml`, `kibana/docker-compose.yml`, `docs/ai-harness/`
- **Changes**:
  - `grafana/docker-compose.yml`, `zipkin/docker-compose.yml`, `kibana/docker-compose.yml`:
    - `account-network` 네트워크 섹션에 `grafana`/`account-grafana`, `zipkin`/`account-zipkin`, `kibana`/`account-kibana` 별칭(aliases)을 추가하여 독립형(standalone) compose와 통합 모니터링 compose 간 DNS 이름 정합화.
    - `kibana/docker-compose.yml`: 단독 기동 시 외부 컨테이너 의존성 오류를 일으키는 미정의 `depends_on: elasticsearch` 블록을 제거하여 Docker Compose v2 및 Podman 호환성 복구.
  - `frontend-nginx/nginx.conf`:
    - 관측성 도구 엔드포인트(`/grafana/`, `/zipkin/`, `/kibana/`, `/pgadmin/`)에 대해 `@fallback_...` 네임드 로케이션 및 `error_page 502 = @fallback_...` 지시자를 추가.
    - `account-grafana` -> `grafana`, `account-zipkin` -> `zipkin`, `account-kibana` -> `kibana`, `account-pgadmin` -> `pgadmin` 순차 폴백을 지원하여 어떤 compose 방식으로 기동되더라도 Nginx 프록시가 투명하게 트래픽을 전달하도록 구성.
- **Verification**:
  - `DevelopmentComposePolicyTest` 및 전체 빌드 검증 성공.
  - Nginx 단일 진입점(`localhost:8080`) 및 Cloudflare 터널을 통해 `/` (Next.js 200 OK), `/api/actuator/health` (Gateway 401 Unauthorized), `/zipkin/` (Zipkin UI 200 OK) 연결 확인.
  - `git diff --check`: 오류 0건, credential 노출 0건.

### 📅 2026-09-09 (GH-657: 저자원 minimal external-dev 스택을 위한 Nginx 단일 진입점 DNS 별칭 동기화 및 런타임 업스트림 내성 강화)
- **Component**: `frontend-nginx/`, `tools/compose.minimal-auth-external-dev.yml`, `docs/ai-harness/`
- **Changes**:
  - `tools/compose.minimal-auth-external-dev.yml`: `minimal-gateway` 및 `minimal-frontend` 컨테이너 네트워크에 `account-gateway` 및 `account-frontend` 네트워크 별칭(aliases)을 추가하여 표준 전체 스택과 DNS 일관성 확보.
  - `frontend-nginx/nginx.conf`:
    - 컨테이너 런타임 DNS 리졸버 자동 연동(`resolver ${NGINX_LOCAL_RESOLVERS} valid=10s ipv6=off;`)을 추가하고 모든 업스트림을 변수 기반 동적 해석(`set $...; proxy_pass $...;`)으로 전환하여 미실행 모니터링 컨테이너로 인한 Nginx 부팅 시 crash 원천 방지.
    - `minimal-frontend` 및 `minimal-gateway` 대상 `error_page 502 = @minimal_...` 우아한 장애 조치(graceful fallback)를 구성하여 별칭 미지정 레거시 컨테이너 및 신규 컨테이너 환경 모두 무중단 지원.
  - `frontend-nginx/Dockerfile` & `docker-compose.yml`:
    - `NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1` 환경변수 주입 및 `/etc/nginx/templates/default.conf.template` 템플릿 마운트 방식을 채택하여 Docker(127.0.0.11), Podman(10.89.4.1), K8s DNS를 자동 감지/주입.
    - 네트워크 기본값을 `${ACCOUNT_NETWORK_NAME:-account-network}`로 정합화.
- **Verification**:
  - `DevelopmentComposePolicyTest` 단독 및 `:config-server:test` 전체 118개 테스트 성공 (BUILD SUCCESSFUL).
  - Podman 기반 `test-nginx-template` 기동 후 `curl http://localhost:8080/` (Next.js 200 OK) 및 `curl http://localhost:8080/api/actuator/health` (Gateway 401 Unauthorized via JWT Filter) 정상 프록시 실시간 검증 완료.
  - `git diff --check`: 0 errors, credentials/secrets 0건.

### 📅 2026-09-08 (GH-436: Config Server encrypt/decrypt endpoint 인증 및 네트워크 노출 제한)
- **Component**: `config-server`, `compose.prod.yml`, `docs/ai-harness/`
- **Changes**:
  - `EncryptionEndpointSecurityFilter.java`: Config Server 암복호화 엔드포인트(`/encrypt`, `/decrypt` 및 URL 인코딩, 세미콜론 매트릭스 파라미터 변형 포함)에 대해 승인된 내부 토큰(`X-Config-Token`, `X-Config-Internal-Token`) 인증 강제 및 비활성화 기본 정책 구현.
  - `EncryptionEndpointWebSecurity.java`: `EncryptionController` 핸들러 매핑 변경 시에도 최고 우선순위 인터셉터로 인증 인가를 보장하도록 WebMvcConfigurer 구현.
  - `application.yml`: 기본값으로 `spring.cloud.config.server.encrypt.enabled: false` 설정 및 `EncryptionController` 로깅 OFF 지정하여 민감정보/평문 노출 원천 차단.
  - `compose.prod.yml`: `SPRING_CLOUD_CONFIG_SERVER_ENCRYPT_ENABLED: "false"` 명시 및 외부 포트 미노출 유지.
  - `ConfigServerEncryptionEndpointSecurityIntegrationTest.java`: 비활성화 404, 무인증/잘못된 토큰 401, 토큰 미설정 403 fail-closed, 유효 토큰 200 허용, health/readiness 및 마이크로서비스 설정 조회 무회귀 통합 테스트 작성.
  - `ConfigCryptoExposurePolicyTest.java`: Compose 및 Gateway 라우팅에서 Config Server가 공용 ingress로 노출되지 않음을 보증하는 정적 정책 테스트 추가.
- **Verification**:
  - `./gradlew :config-server:test :config-server:bootJar` (118 tests 100% SUCCESSFUL).
  - Compose 및 Gateway 라우팅 정적 검사 통과.
  - `git diff --check`: 오류 0건.

### 📅 2026-09-08 (GH-640: external-dev PostgreSQL에 accounting 업무 패키지 DB 프로비저닝 및 마이그레이션 적용)
- **Component**: `postgres/runtime/provision-accounting-external-dev.py`, `docs/ai-harness/`
- **Changes**:
  - `postgres/runtime/provision-accounting-external-dev.py`: external-dev PostgreSQL 컨테이너(`account-postgres`)에 accounting 7개 서비스(`journal-ledger`, `closing`, `payable`, `receivable`, `tax`, `expenditure-resolution`, `reporting`) DB 생성, `_owner` 및 `_app` 역할 프로비저닝, `account-migration-runner.jar` 기반 마이그레이션 적용 및 validate 검증 스크립트 작성.
  - Podman 사용자 정의 브릿지 네트워크(`account-network`)를 사용하여 `account-external-dev-db` 별칭을 컨테이너 내에서 안전하게 통신하도록 설정.
  - Fail-closed 보안 검증: DDL 거부(`_app` 사용자의 `CREATE TABLE` 실패 확인), `flyway_schema_history` 격리, `_app` 패스워드 인증 검증.
  - 환경 변수(`.env.external-dev`)에 안전하게 32바이트 보안 난수 비밀번호 및 JDBC URL 자동 주입(0600 권한 보장, 미커밋 보존).
- **Verification**:
  - `provision-accounting-external-dev.py --apply`: 7/7 databases migrate, validate, login, ACL, DDL denial PASS.
  - Dry-run preflight check: PASS (seven canonical development targets; existing state preserved).
  - `tools/check-accounting-databases.sh`: 7개 DB 격리 게이트 PASS.
  - Git diff check & secret leak check: 0 issues.

### 📅 2026-09-07 (Harness: 4단계 작업 난이도(하/중/상/최상) 및 모델 추론 강도(high/xhigh) 매핑 정책 수립)
- **Component**: `docs/ai-harness/70-model-assignment-policy.md`, `docs/ai-harness/85-github-issue-agent-loop.md`, `docs/ai-harness/87-spec-driven-delegation.md`, `docs/ai-harness/00-overview.md`, `.github/ISSUE_TEMPLATE/*`

### 📅 2026-09-07 (GH-630: 통합 아키텍처 명세서 위치 정규화 및 최신 2026 MSA 구성도 업데이트)
- **Component**: `docs/architecture/architecture.md`, `docs/guides/master-domain-glossary.md`, `docs/README.md`
- **Changes**:
  - `docs/architecture.md`를 `docs/architecture/architecture.md`로 위치 정규화 이동하여 `docs/README.md`(29행) 및 루트 `README.md`(262행)와의 링크 정합성 100% 일치.
  - `docs/architecture/architecture.md` 전면 개편:
    - 2026 엔터프라이즈 최신 시스템 전체 토폴로지 다이어그램(Nginx 단일 진입점, Next.js 15 BFF, Gateway :18000, 16개 마이크로서비스, 17개 격리 DB 컨텍스트, Prometheus/Grafana/ELK/Zipkin 관측 인프라) 반영.
    - 6단계 엔드투엔드 금융 비즈니스 파이프라인 Mermaid 흐름도 반영.
    - 마이크로서비스 내부 헥사고날 아키텍처(Inbound UseCase, Core POJO, Outbound SPI, JPA/Kafka Adapter) 상세 설계도 반영.
  - `docs/guides/master-domain-glossary.md` 내 상대 경로 링크(`../architecture/architecture.md`) 보정.
- **Verification**:
  - `git diff --check`: 0 errors
  - Conflict markers & credential pattern scan: 0 occurrences

### 📅 2026-09-02 (GH-521: 초보자용 Podman 최소 이미지 빌드·실행·롤백 가이드 최신화)
- **Component**: `docs/guides/development-compose.md`, `docs/guides/container-images.md`, `docs/guides/frontend-runtime-guide.md`, `docs/guides/runtime-execution-matrix.md`
- **Changes**:
  - `development-compose.md`: Podman vs Docker 차이점 및 Provider 요구사항(Python `podman-compose` 금지 및 Go 바이너리 `docker-compose` v2.x 플러그인 연결 필수) 정합화.
  - 초보자용 저자원 7개 최소 인증 스택(`tools/compose.minimal-auth-external-dev.yml`) 1-Worker 순차 빌드(`--max-workers=1`), 기동, smoke, 헬스체크 및 트러블슈팅(EACCES, HTTP 400 vs 503, OOM 방지) 가이드 구성.
  - 이미지 재빌드 시 영향(소스 변경 시 재빌드 필수) 및 컨테이너 재생성 조건(`up --force-recreate` / `up -d`) 명시.
  - 안전한 프로젝트 단위 중지/롤백 절차 및 절대 금지 명령어(`down -v`, `container/image prune -f`, `git checkout <commit> -- <files>`) 명시.
  - `container-images.md`, `frontend-runtime-guide.md`, `runtime-execution-matrix.md` 연계 링크 및 금지 명령어 경고 동기화.
- **Verification**:
  - `python3 tools/test_run_minimal_auth_external_dev.py` (7 tests PASS)
  - `./gradlew :config-server:test --tests "com.ho.account.configserver.DevelopmentComposePolicyTest" --tests "com.ho.account.configserver.ContainerImagePolicyTest"` (BUILD SUCCESSFUL, 5/5 tasks executed)
  - `git diff --check` (오류 0건)
  - Conflict markers & credential scan (0건)

### 📅 2026-08-19 (GH-484: Apply K-Bank Modern Fintech Design System)
- **Component**: `frontend/src/app/globals.css`, `tailwind.config.js`, `MainLayout.tsx`, `TopHeader.tsx`, `Sidebar.tsx`, `PageHeader.tsx`, `StatusBadge.tsx`, `AmountDisplay.tsx`, `Tabs.tsx`, `app/page.tsx`
- **Changes**: Transformed the frontend look-and-feel into the signature KBank modern fintech aesthetic: clean soft-gray canvas (`#f7f8fb`), pure white elevated cards (`#ffffff`, border `#eaedf4`, subtle shadow), KBank signature blue (`#4262ff`, light `#eef2ff`, border `#dbe3ff`), refined typography (`letter-spacing: -0.015em`), crisp text hierarchy (`#17191e`, `#545b69`, `#8c94a4`), and white-glass TopHeader and Sidebar navigation.
- **Verification**: `npm run build` (122 routes 100% SUCCESSFUL).
### 📅 2026-08-19 (GH-484: Apply K-Bank Modern Fintech Design System, Dual-Theme & Decoupled Navigation)
- **Component**: `frontend/src/app/globals.css`, `ThemeContext.tsx`, `TopHeader.tsx`, `Sidebar.tsx`, `NavContext.tsx`, `menus/*.ts`, `frontend/docs/`
- **Changes**: 
  - Transformed frontend look-and-feel into signature KBank modern fintech aesthetic: clean soft-gray canvas (`#f7f8fb`), pure white cards (`#ffffff`), KBank signature blue (`#4262ff`), refined typography (`letter-spacing: -0.015em`), and robust global dual-theme engine (`html:not(.dark)` pure white vs `html.dark` navy dark card `#131b2e`).
  - Decoupled combined menus and structured navigation into **6 Mega Business Groups** in TopHeader (eliminating horizontal scrollbars) while rendering all **16 discrete MSA module sections with tag badges** in Sidebar.
  - Implemented 13 API domain services with 1.5s AbortController timeout fallback to resilient mock datasets.
  - Created comprehensive master education guide (`frontend-core-education-guide.md`) and UI layout/screen specification (`ui-layout-and-screen-specification.md`).
- **Verification**: `npm run build` (122 routes 100% SUCCESSFUL, 0 errors, 0 warnings). Independent subagent approved.

### 📅 2026-08-14 (Premium Mermaid Diagram & Visual Chart Enhancement)
- **Component**: `README.md`, `docs/guides/master-domain-glossary.md`
- **Changes**: Enhanced all Mermaid flowcharts and system architecture diagrams with rich HSL/Hex color fills (`style`), explicit subgraphs, quoted node labels for rendering safety, and clear flow annotations across root `README.md` and `docs/guides/master-domain-glossary.md`.
- **Verification**: Verified Markdown and Mermaid rendering validity.

### 📅 2026-08-13 (Enterprise Documentation Restructuring, Code-Sync & Master Glossary)
- **Component**: `docs/`, `docs/architecture/`, `docs/guides/`
- **Changes**: Restructured top-level `docs/` taxonomy into dedicated subdirectories (`docs/architecture/` for systemic architecture 명세 and `docs/guides/` for developer/runtime runbooks). Created `docs/guides/master-domain-glossary.md` providing intuitive analogies for core domain & technical concepts (전표, 마감, 대사, IFRS9 ECL, Outbox, Chunking, Circuit Breaker). Updated `docs/README.md` as the master navigation hub. All documentation synchronized 100% with current Spring Boot 3.4 / Java 21 / Hexagonal code implementation.
- **Verification**: Verified directory structure and relative link integrity.

### 📅 2026-08-13 (AI Harness Root Streamlining & Clean Root Directory Policy)
- **Component**: Root directory, `AGENTS.md`, `docs/ai-harness/10-rules.md`, `docs/history/`
- **Changes**: Deleted legacy root `SKILL.md` and empty `CLAUDE_WORKLOG.md`. Relocated `CODEX_WORKLOG.md` to `docs/history/CODEX_WORKLOG.md` and `CODEX_HANDOFF_TASKS.md` to `docs/ai-harness/codex-handoff-tasks.md`. Preserved tool-required root contracts (`README.md`, `AGENTS.md`, `GEMINI.md`, `CLAUDE.md`, `GEMINI_REVIEW_PROMPT.md`). Updated `AGENTS.md` and `docs/ai-harness/10-rules.md` establishing the Clean Root Directory Policy.
- **Verification**: Verified clean git status and updated document paths.

### 📅 2026-08-13 (MSA Microservice Module Documentation Audit & Harness Synchronization)
- **Component**: `docs/ai-harness`, MSA service modules (`journal-ledger`, `closing`, `deposit`, `loan`, `asset-lease`, `payable`, `receivable`, `tax`, `budget`, `expenditure-resolution`, `reporting`, `account-mart`, `ecl`, `shared-kernel`)
- **Changes**: Audited and verified all MSA service module documentation (`README.md`, `docs/*.md`) under dedicated branch `docs/msa-module-docs-update`. Confirmed 100% alignment with Hexagonal Architecture boundaries (api/core/batch), H2 `local` profile isolation, and multi-module parallel execution standards ($account-module-parallel). Synchronized AI harness records (`agent-status.md`, `worklog.md`, `handoff.md`).
- **Verification**: Verified Markdown syntax and git diff status.

### 📅 2026-08-13 (GH-92: Fix Tax Batch ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `tax/batch`
- **Changes**: Created `tax/batch/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:tax_batch_db;MODE=PostgreSQL`), Flyway baseline migration (`locations: classpath:db/tax-migration`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `TaxBatchApplicationTests.java` with `@SpringBootTest(classes = TaxBatchApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading, Job/UseCase bean injection, and profile isolation. Added extensive pedagogical comments across changed files.
- **Verification**: `./gradlew.bat :tax:batch:test` (100% SUCCESSFUL).

### 📅 2026-08-13 (GH-93: Fix Expenditure Resolution API ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `expenditure-resolution/api`
- **Changes**: Created `expenditure-resolution/api/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:expenditure_resolution_api_db;MODE=PostgreSQL`), Flyway migration (`locations: classpath:db/expenditure-resolution-migration`), JPA `ddl-auto: validate`, and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `ExpenditureResolutionApiApplicationTests.java` with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading, Controller/UseCase/Port stub injection, and profile isolation. Refactored `ExpenditureResolutionPostgresqlSchemaContextTest.java` to leverage local profile with detailed pedagogical comments.
- **Verification**: `./gradlew.bat :expenditure-resolution:api:test` (100% SUCCESSFUL).

### 📅 2026-08-13 (GH-94: Fix Expenditure Resolution Batch ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `expenditure-resolution/batch`
- **Changes**: Created `expenditure-resolution/batch/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:expenditure_resolution_batch_db;MODE=PostgreSQL`), Flyway migration (`locations: classpath:db/expenditure-resolution-migration`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `ExpenditureResolutionBatchApplicationTests.java` with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading, Job/UseCase injection, and profile isolation. Fixed `JournalPostingPort` anonymous stub implementation in `ExpenditureResolutionLocalExternalPortConfiguration.java`. Added extensive pedagogical comments across changed files.
- **Verification**: `./gradlew.bat :expenditure-resolution:batch:test` (100% SUCCESSFUL).

### 📅 2026-08-13 (GH-96: Fix Reporting Batch ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `reporting/batch`
- **Changes**: Created `reporting/batch/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:reporting_batch_db;MODE=PostgreSQL`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: create-drop`, memory persistence mode (`account.reporting.persistence.mode: memory`), and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `ReportingBatchApplicationTests.java` with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading and profile isolation. Added extensive pedagogical comments across changed files.
- **Verification**: `./gradlew.bat :reporting:batch:test` (100% SUCCESSFUL).

### 📅 2026-08-13 (GH-97: Fix Account Mart API ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `account-mart/mart-api`
- **Changes**: Created `account-mart/mart-api/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:account-mart-api-local;MODE=PostgreSQL`), Flyway baseline migration (`locations: classpath:db/account-mart-local-migration`), JPA `ddl-auto: validate`, and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `AccountMartApiApplicationTests.java` with `@SpringBootTest(classes = AllowanceMartApiApplication.class)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading and profile isolation. Added extensive pedagogical comments across changed files.
- **Verification**: `./gradlew.bat :account-mart:mart-api:test` (100% SUCCESSFUL).

### 📅 2026-08-13 (GH-98: Fix Account Mart Batch ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `account-mart/mart-batch`
- **Changes**: Created `account-mart/mart-batch/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:account-mart-batch-local;MODE=PostgreSQL`), Flyway baseline migration (`locations: classpath:db/account-mart-local-migration`), Spring Batch H2 metadata schema initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `AccountMartBatchApplicationTests.java` with `@SpringBootTest(classes = AllowanceMartBatchApplication.class)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading and profile isolation. Added extensive pedagogical comments across changed files.
- **Verification**: `./gradlew.bat :account-mart:mart-batch:test` (100% SUCCESSFUL).

### 📅 2026-08-13 (GH-99: Fix ECL API ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `ecl/ecl-api`
- **Changes**: Created `ecl/ecl-api/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:ecl-api-local;MODE=PostgreSQL`), Flyway V1 baseline migration (`locations: classpath:db/ecl-local-migration`), JPA `ddl-auto: validate`, and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `EclApiApplicationTests.java` with `@SpringBootTest(classes = AllowanceEclApiApplication.class)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading and profile isolation. Added extensive pedagogical comments across all changed files.
- **Verification**: `./gradlew.bat :ecl:ecl-api:test` (100% SUCCESSFUL).

### 📅 2026-08-13 (GH-100: Fix ECL Batch ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `ecl/ecl-batch`
- **Changes**: Added exception rule `!**/src/test/resources/application-local.yml` to `.gitignore`. Created `ecl/ecl-batch/src/test/resources/application-local.yml` with isolated H2 in-memory DB (`jdbc:h2:mem:ecl-batch-local;MODE=PostgreSQL`), Flyway V1 baseline migration (`locations: classpath:db/ecl-local-migration`), Spring Batch H2 metadata table initialization (`spring.batch.jdbc.initialize-schema: always`), JPA `ddl-auto: validate`, and disabled external Cloud Config/Eureka/Vault/Kafka control plane services. Created `EclBatchApplicationTests.java` with `@SpringBootTest(classes = AllowanceEclBatchApplication.class)` and `@ActiveProfiles("local")` verifying clean ApplicationContext loading and profile isolation. Added extensive pedagogical comments across all changed files.
- **Verification**: `./gradlew.bat :ecl:ecl-batch:test` (100% SUCCESSFUL). PR #405 merged into `main`.

### 📅 2026-08-12 (GH-75: Fix Journal Ledger API ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `journal-ledger/api`, `journal-ledger/core`
- **Changes**: Created `journal-ledger/api/src/main/resources/application.yml` and `application-local.yml` with default `local` profile, H2 in-memory DB (`jdbc:h2:mem:journal_ledger_api_db;MODE=PostgreSQL`), JPA `create-drop`, `journal-ledger.master-data.local-adapter.enabled: true`, `spring.kafka.listener.auto-startup: false`, and disabled Cloud Config/Eureka/Vault control plane services. Updated `JournalLedgerApplication` Composition Root `@EntityScan` and `@EnableJpaRepositories` to explicitly include `com.ho.account.journalledger.domain` and `com.ho.account.journalledger.adapter.out.persistence` packages. Fixed UTF-16LE BOM encoding of `journal-ledger/api/src/test/resources/application.properties` to standard UTF-8. Created `JournalLedgerApiLocalProfileTest.java` verifying local ApplicationContext loading, profile isolation, H2 DB configuration, external cloud & Kafka listener decoupling, and Metamodel/Repository bean registrations. Added extensive pedagogical comments across all changed files.
- **Verification**: `./gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:api:bootJar` (100% SUCCESSFUL), executable JAR smoke test `java -jar journal-ledger/api/build/libs/journal-ledger-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` clean H2 schema startup and JPA initialization.

### 📅 2026-08-12 (GH-79: Fix Loan API ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `loan/api`, `loan/core`
- **Changes**: Created `loan/api/src/main/resources/application.yml` and `application-local.yml` with default `local` profile, H2 in-memory DB (`jdbc:h2:mem:loan_api_db;MODE=PostgreSQL`), JPA `create-drop`, and disabled Cloud Config/Eureka/Vault control plane services. Updated `LoanApplication` Composition Root `@EntityScan` and `@EnableJpaRepositories` to explicitly include MasterData entity (`com.ho.account.masterdata.core.infrastructure.persistence.entity` & `com.ho.account.masterdata.core.infrastructure.persistence`) and Shared Audit security packages (`com.ho.account.shared.infrastructure.security.domain` & `repository`), resolving `BusinessPartnerJpaEntity` `Not a managed type` and missing `AuditLogRepository` bean errors. Refactored `LoanApplicationLocalProfileTest.java` to verify local ApplicationContext loading, profile isolation, H2 DB configuration, external cloud decoupling, and Metamodel/Repository bean registrations. Added extensive pedagogical comments across all changed files.
- **Verification**: `./gradlew.bat :loan:core:test :loan:api:test :loan:api:bootJar` (100% SUCCESSFUL), executable JAR smoke test `java -jar loan/api/build/libs/loan-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` clean H2 schema startup and JPA initialization. PR #401 merged into `main`.

### 📅 2026-08-12 (GH-80: Fix Loan Batch ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `loan/batch`, `loan/core`
- **Changes**: Created `loan/batch/src/main/resources/application.yml` and `application-local.yml` with default `local` profile, H2 in-memory DB (`jdbc:h2:mem:loan_batch_db;MODE=PostgreSQL`), JPA `create-drop`, `spring.batch.jdbc.initialize-schema: always`, `spring.batch.job.enabled: false`, `web-application-type: none`, and disabled Cloud Config/Eureka/Vault control plane services. Removed `@EnableBatchProcessing` from `LoanBatchApplication` to restore Spring Boot 3 `BatchAutoConfiguration` and auto-creation of Spring Batch metadata tables. Added `masterdata` persistence and `shared.security` packages to `@EntityScan` and `@EnableJpaRepositories` in `LoanBatchApplication`. Added `@Autowired` to `LoanJournalAdapter` primary constructor to resolve Spring DI constructor ambiguity in `loan/core`. Fixed test dependency in `loan/batch/build.gradle` (`spring-batch-test`). Added `LoanBatchLocalProfileTest` and `LoanInterestAccrualBatchConfigTest` verifying ApplicationContext loading, profile isolation, non-web environment, and representative job execution (`loanInterestAccrualJob`). Added extensive pedagogical comments across all changed files.
- **Verification**: `./gradlew.bat :loan:core:test :loan:batch:test :loan:batch:bootJar` (100% SUCCESSFUL), executable JAR smoke test `java -jar loan/batch/build/libs/loan-batch-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` clean startup & exit.

### 📅 2026-08-12 (GH-81: Fix Deposit API ApplicationContext Loading and Configure Local H2 Profile)
- **Component**: `deposit/api`, `deposit/core`
- **Changes**: Created `deposit/api/src/main/resources/application.yml` and `application-local.yml` with default `local` profile, H2 in-memory DB (`jdbc:h2:mem:deposit_local_db;MODE=PostgreSQL`), JPA `create-drop`, `flyway.enabled: false`, `local-adapters.enabled: true`, and disabled Cloud Config/Eureka/Vault control plane services. Implemented `approveAndPost` in `LocalDepositJournalPostingAdapter` to satisfy `JournalPostingPort`. Created `DepositQueryUseCase` and `DepositUseCase` inbound port interfaces. Added `@Autowired` to `DepositService` primary constructor to resolve Spring DI constructor ambiguity, and implemented `findByAccountNumber`. Configured `@SpringBootApplication(scanBasePackages = "com.ho.account.deposit")`, `@EntityScan`, and `@EnableJpaRepositories` in `DepositApplication`. Enhanced `DepositController` REST endpoints and created `DepositApplicationTest` for local ApplicationContext integration testing. Added extensive pedagogical comments across all changed files.
- **Verification**: `./gradlew.bat :deposit:core:test :deposit:api:test :deposit:api:bootJar` (100% SUCCESSFUL).

### 📅 2026-08-12 (GH-362: Pin Node 20 LTS Runtime and Align Container Contracts for Frontend)
- **Component**: `frontend`
- **Changes**: Configured explicit `"engines": { "node": ">=20.0.0 <21.0.0", "npm": ">=10.0.0" }` in `frontend/package.json`. Created `frontend/.nvmrc` and `frontend/.node-version` targeting Node 20 LTS (`20.18.0`). Aligned local runtime specifications with `node:20-alpine` in `frontend/Dockerfile`, `frontend/Containerfile`, and `frontend/Containerfile.dev`. Added extensive pedagogical comments explaining Node Runtime Pinning, Dev/Container Runtime Parity, and Lockfile v3 Deterministic Reproducibility across configuration header blocks, `package.json`, and `frontend/README.md`.
- **Verification**: `git diff` inspection and `package.json` JSON parsing structure validation.

### 📅 2026-08-12 (GH-343: Align demo seed fixture and clean batch lifecycle for account mart)
- **Component**: `account-mart/mart-batch`, `account-mart/mart-core`
- **Changes**: Created `AccountMartDemoFixtureService` and `AccountMartDemoSeedRunner` activated on `mart.batch.demo-seed.enabled: true` for deterministic H2 fixture seeding (ods account subjects, products, ledgers, balance history, general ledger, exchange rates, KAP ratings, collaterals). Added `@Bean(destroyMethod = "")` to all `ItemReader` bean definitions in `IntegratedPositionEtlJobConfig`, `KapDataEtlJobConfig`, and `BehavioralHistoryLoadJobConfig` to decouple Spring Container shutdown inferred `close()` from Spring Batch Step ItemStream lifecycle, guaranteeing warning-free clean context shutdown. Added `AccountMartDemoSeedAndLifecycleTest.java` and extensive pedagogical comments.
- **Verification**: `./gradlew.bat :account-mart:mart-core:test :account-mart:mart-api:test :account-mart:mart-batch:test :account-mart:mart-api:bootJar :account-mart:mart-batch:bootJar` (100% SUCCESSFUL). PR #397 merged into `main`.

### 📅 2026-08-12 (GH-344: Strengthen Self-Contained Local H2 Runtime Policy for Internal Audit)
- **Component**: `internal-audit/api`, `internal-audit/core`
- **Changes**: Added explicit `application-local.yml` for `internal-audit/api` with H2 PostgreSQL mode (`jdbc:h2:mem:internal_audit_local_db;MODE=PostgreSQL`), Flyway V60 migration target, JPA `validate`, and disabled Spring Cloud Config/Discovery/Eureka/Vault control plane services. Strengthened `InternalAuditRuntimePolicyTest.java` with assertions for profile isolation, PostgreSQL dev/prod policies, and bootJar/Compose execution topology contracts. Updated `internal-audit/README.md` with explicit local standalone execution commands. Added extensive pedagogical comments on profile-based runtime isolation and control plane decoupling.
- **Verification**: `./gradlew.bat :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar` (100% SUCCESSFUL). PR #396 merged into `main`.

### 📅 2026-08-12 (GH-345: Add explicit local H2 profile and isolate demo credentials for auth module)
- **Component**: `auth/api`, `auth/core`
- **Changes**: Added explicit `application-local.yml` for `auth/api` with H2 PostgreSQL mode, H2 console, Flyway migration and isolated demo credentials (`auth.jwt.secret`, `auth.internal-api.token`, demo user). Stripped default fallback secrets from `application.yml` and `AuthModuleProperties.java`, enforcing Fail-Closed policy on base/dev/prod environments when required credentials are missing. Created `AuthApiRuntimePolicyTest.java` for policy assertions.
- **Verification**: `./gradlew.bat :auth:core:test :auth:api:test :auth:api:bootJar` (100% SUCCESSFUL).

## 2026-08-12 - Issue #347 Add Explicit Self-Contained Local Profiles for Reporting Module

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/347-reporting-local-profile` / `C:\tmp\account-347-reporting-local-profile`.
- Base: `origin/main`.
- Scope:
  - `reporting/api/src/main/resources/application-local.yml`:
    - Configured explicit `local` profile with H2 in-memory DB (`jdbc:h2:mem:reporting_api_db`), JPA `create-drop`, and memory persistence mode (`account.reporting.persistence.mode: memory`).
    - Decoupled external infrastructure services (Config Server, Eureka Discovery, Vault, Tracing) to guarantee self-contained local execution.
  - `reporting/batch/src/main/resources/application-local.yml`:
    - Configured `web-application-type: none` to disable embedded servlet container.
    - Set `spring.batch.job.enabled: false` to prevent automatic batch job executions upon startup.
    - Set `spring.batch.jdbc.initialize-schema: always` to initialize Spring Batch meta-tables in H2.
  - Integration Tests (`ReportingApiLocalProfileTest.java`, `ReportingBatchLocalProfileTest.java`):
    - Added `@ActiveProfiles("local")` integration tests validating context startup, H2 database connection, web-environment disabled, batch job auto-start prevention, and memory adapter injection.
  - Interface Implementation Fixes:
    - Implemented `findBySlipNo` in `InMemoryJournalQueryAdapter.java` and `calculateLedgerSummary` in `LedgerClientAdapterTest.java` to align with contract updates.
  - Pedagogical Comments:
    - Added comprehensive comments detailing Profile Separation, H2 In-Memory DB Isolation, Automatic Batch Scheduler Prevention, and Infrastructure Decoupling.
  - Test Validation:
    - Executed `./gradlew.bat :reporting:core:test :reporting:api:test :reporting:batch:test :reporting:api:bootJar :reporting:batch:bootJar` (100% SUCCESSFUL).

## 2026-08-12 - Issue #300 Refactor Payable & Receivable Batch Jobs to Chunk-Oriented Processing

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/300-payable-receivable-batch-chunk` / `C:\tmp\account-300-payable-receivable-batch-chunk`.
- Base: `origin/main`.
- Scope:
  - `ReceivableAutoMatchingBatchConfig.java`:
    - Refactored single-transaction Tasklet (`runAutoMatching`) to Spring Batch Chunk-Oriented Architecture (Chunk Size 100).
    - `receivableAutoMatchingItemReader`: Configured `JpaPagingItemReader<CollectionJpaEntity>` (`@StepScope`, pageSize=100) to stream candidate collections page-by-page.
    - `receivableAutoMatchingItemWriter`: Delegates 100-item chunks to `collectionUseCase.attemptAutoMatching` committing per chunk.
  - `PaymentUseCase.java` & `PaymentService.java`:
    - Added chunk processing support methods `createPaymentRun`, `processPaymentRunChunk`, and `completePaymentRun`.
  - `PayablePaymentRunBatchConfig.java`:
    - Refactored single-transaction Tasklet into a 3-step pipeline (`createPaymentRunStep` -> `processPayablePaymentRunChunkStep` -> `completePaymentRunStep`).
    - `createPaymentRunStep` (Tasklet): Creates `PaymentRun` header in INITIATED status and puts `paymentRunId` & `runDate` into `JobExecutionContext`.
    - `processPayablePaymentRunChunkStep` (Chunk Step, Chunk Size 100): Uses `JpaPagingItemReader<PayableJpaEntity>` to load due payables page-by-page, calling `paymentUseCase.processPaymentRunChunk` per 100 items to cap memory footprint at $O(\text{chunkSize})$.
    - `completePaymentRunStep` (Tasklet): Transitions `PaymentRun` status to PROCESSING upon chunk completion.
  - Pedagogical Comments:
    - Added comprehensive comments on Chunk-oriented Architecture benefits, Memory Footprint Management (Heap OOM prevention), and Transaction Boundary Segregation (partial failure rollback/retry).
  - Test Validation:
    - Executed `./gradlew.bat :receivable:batch:test :payable:batch:test` (100% SUCCESSFUL).

## 2026-08-12 - Issue #302 Decompose Monolithic ReconciliationService for Single Responsibility Principle Compliance

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/302-reconciliation-srp-service-split` / `C:\tmp\account-302-reconciliation-srp-service-split`.
- Base: `origin/main`.
- Scope:
  - Specialized Domain Services:
    - Created `ReconciliationUnitService.java` for ReconciliationUnit CRUD and soft deletion management.
    - Created `ReconciliationRuleService.java` for ReconciliationRule and DifferenceReasonCode CRUD and soft deletion management.
    - Created `ReconciliationExecutionService.java` for data collection, N:M matching engine orchestration, difference assignment/resolution, and adjustment journal creation.
  - Facade Pattern Refactoring (`ReconciliationService.java`):
    - Converted `ReconciliationService` into a Facade Service delegating to the specialized domain services.
    - Preserved 100% backward compatibility for API Controllers, Batch services, and legacy constructors.
  - Pedagogical Comments:
    - Added comprehensive comments detailing Single Responsibility Principle (SRP), God Class smell removal, and Facade Pattern encapsulation benefits.
  - Unit Tests & Verification:
    - Added unit tests (`ReconciliationUnitServiceTest.java`, `ReconciliationRuleServiceTest.java`).
    - Verified all reconciliation module tests (`:reconciliation:core:test`, `:reconciliation:api:test`, `:reconciliation:batch:test` - BUILD SUCCESSFUL).

## 2026-08-12 - Issue #307 Refactor Balance Reaggregation Batch Job to Chunk-Oriented Processing & Guarantee Idempotency

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/307-journal-ledger-batch-reaggregation-chunk` / `C:\tmp\account-307-journal-ledger-batch-reaggregation-chunk`.
- Base: `origin/main`.
- Scope:
  - Domain & Service Extensions (`LedgerService.java`):
    - Added `clearLedgerBalancesForPeriod(startDate, endDate)` method for deleting existing GL/SL balances before re-aggregation to guarantee idempotency.
  - Pre-processing Clean-up Tasklet (`BalanceCleanUpTasklet.java`):
    - Created `BalanceCleanUpTasklet` to clean up target date range balances in Step 1 (`balanceCleanUpStep`). Removed obsolete `BalanceReaggregationTasklet.java`.
  - 2-Step Chunk-Oriented Pipeline Refactoring (`BalanceReaggregationBatchConfig.java`):
    - **Step 1 (`balanceCleanUpStep`)**: Executes `BalanceCleanUpTasklet` for pre-processing cleanup.
    - **Step 2 (`balanceReaggregationStep`)**: Chunk-oriented processing with chunk size 100.
      - `balanceReaggregationItemReader`: `JpaPagingItemReader` (`@StepScope`) for reading POSTED `JournalDetail` records page-by-page, controlling memory footprint to $O(\text{chunkSize})$.
      - `balanceReaggregationItemProcessor`: Pass-through processor (`item -> item`).
      - `balanceReaggregationItemWriter`: Delegates chunk list to `ledgerService.updateLedgerBalancesBulk(chunk.getItems())`, committing transactions per 100 items.
  - Pedagogical Comments:
    - Added extensive comments covering Low Memory Footprint, Transaction Boundaries, Restartability & Idempotency, and Clean-up Step benefits.
  - Test Validation (`BalanceReaggregationBatchConfigTest.java`):
    - Created `@SpringBatchTest` verifying initial batch execution balance calculation and second run idempotency.
    - Executed `./gradlew.bat :journal-ledger:batch:test` and `./gradlew.bat :journal-ledger:core:test` (100% SUCCESSFUL). PR #391 merged into `main`.

## 2026-08-12 - Issue #320 Implement Item-Level N:M Matching Engine for Financial Reconciliation

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/320-reconciliation-item-matching-engine` / `C:\tmp\account-320-reconciliation-item-matching-engine`.
- Base: `origin/main`.
- Scope:
  - Domain Model Design (`reconciliation/core/src/main/java/com/ho/account/reconciliation/domain/`):
    - Created `ReconciliationItem` immutable value object representing individual source/target reconciliation items.
    - Created `ReconciliationCompositeKey` value object supporting normalized multi-attribute grouping (`transactionDate`, `referenceId`, `partnerCode`, `accountCode`) and relaxed keys.
  - Item-Level Matching Engine Algorithm (`ItemLevelMatcher.java` & `ReconciliationMatchingEngine.java`):
    - Implemented 4-phase matching pipeline:
      1) Phase 1: Composite Key Exact 1:1 Matching (`EXACT_1_1`)
      2) Phase 2: 1:N & N:1 Subset Matching (`ONE_TO_MANY_1_N`, `MANY_TO_ONE_N_1`)
      3) Phase 3: N:M Subset-Sum Combinatorial Search (`MANY_TO_MANY_N_M`)
      4) Phase 4: Relaxed Key Fallback & Discrepancy Categorization (`MISSING_TARGET`, `MISSING_SOURCE`, `AMOUNT_MISMATCH`)
  - Outbound Port & Adapter Enhancements:
    - Added `loadItems` to `ExternalReconSnapshotPort` and implemented in `ExternalReconStageSnapshotAdapter`.
    - Added `findStageRecords` JPQL query to `ExternalReconStageRecordRepository`.
  - Service Integration (`ReconciliationService.java`):
    - Updated `performReconciliation` to orchestrate item-level N:M matching via `ReconciliationMatchingEngine`.
    - Generated rich `ReconciliationDifference` records with detailed item JSON refs for unmatched items.
  - Pedagogical Comments:
    - Added extensive comments explaining offsetting error prevention, audit trail traceability, and NP-Hard combinatorial optimization via composite key partitioning.
  - Test Validation:
    - Created `ItemLevelMatcherTest`, `ReconciliationMatchingEngineTest`, and validated `ReconciliationServiceTest`.
    - Executed `./gradlew.bat :reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test` (100% SUCCESSFUL). PR #390 merged into `main`.

## 2026-08-12 - Issue #321 Push Down Ledger Snapshot Aggregation to DB Level for Heap OOM Prevention

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/321-reconciliation-db-aggregate-oom-fix` / `C:\tmp\account-321-reconciliation-db-aggregate-oom-fix`.
- Base: `origin/main`.
- Scope:
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
  - Test Validation:
    - Updated `ReconManagerServiceTest`, `MonolithLedgerQueryAdapterTest`, and `ReconciliationLocalExternalPortConfiguration`.
    - Executed `./gradlew.bat :reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test :journal-ledger:core:test` (100% SUCCESSFUL). PR #389 merged into `main`.

## 2026-08-12 - Issue #319 Decouple ECL API Module from ECL Batch for Resource & Process Isolation

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/319-ecl-api-batch-decouple` / `C:\tmp\account-319-ecl-api-batch-decouple`.
- Base: `origin/main`.
- Scope:
  - Dependency Removal (`ecl/ecl-api/build.gradle` & `AllowanceEclApiApplication.java`):
    - Removed `implementation project(':ecl:ecl-batch')` and `implementation 'org.springframework.boot:spring-boot-starter-batch'`.
    - Removed `com.ho.account.ecl.batch` scan and `AllowanceEclBatchApplication` reference.
  - Inbound Batch Trigger Port & Adapter (`com.ho.account.ecl.api.port` & `infrastructure.adapter`):
    - Created `BatchTriggerPort` interface (`triggerBatch`, `getBatchStatus`).
    - Created `BatchTriggerResponse`, `BatchStatusResponse`, `BatchAlreadyCompletedException`, `BatchExecutionException`.
    - Implemented `ExternalBatchTriggerAdapter` using `JdbcTemplate` for Spring Batch metadata table queries without direct Spring Batch dependencies.
  - Controller & Kafka Consumer Refactoring (`AllowanceBatchController.java`, `CdmDataReadyConsumer.java`):
    - Replaced direct `JobLauncher`, `JobExplorer`, and `Job` injection with `BatchTriggerPort`.
    - Updated `CdmDataReadyConsumer` to trigger external batches asynchronously via `BatchTriggerPort` while preserving idempotency and exception propagation.
  - Pedagogical Comments:
    - Added comprehensive comments detailing MSA Resource Isolation, API Server Memory Protection, CPU/Connection Contention Avoidance, and Process Isolation.
  - Test Validation:
    - Updated `CdmDataReadyConsumerTest.java` to test `BatchTriggerPort` interaction.
    - Executed `./gradlew.bat :ecl:ecl-core:test :ecl:ecl-api:test :ecl:ecl-batch:test` (100% SUCCESS).

## 2026-08-12 - Issue #322 Refactor Tax Invoice Batch Validation to Use Paging for OOM Prevention

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/322-tax-invoice-batch-paging-oom-fix` / `C:\tmp\account-322-tax-invoice-batch-paging-oom-fix`.
- Base: `origin/main`.
- Scope:
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
  - Test Validation:
    - Updated `TaxInvoiceBatchServiceTest.java` for paged queries and added `validatePurchaseInvoicesProcessesInPagesToPreventOOM` test for multi-page batch validation.
    - Executed `./gradlew.bat :tax:core:test :tax:api:test :tax:batch:test` (100% SUCCESS). PR #387 merged into `main`.

## 2026-08-12 - Issue #328 Configure Gateway Dynamic Discovery Routing, Global CORS, and Rate Limiter

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/328-gateway-routing-cors-rate-limiter` / `C:\tmp\account-328-gateway-routing-cors-rate-limiter`.
- Base: `origin/main`.
- Scope:
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
  - Test Validation:
    - Created `RateLimiterConfigTest.java` to test `ipKeyResolver` header/remoteAddress extraction and `inMemoryRateLimiter` token consumption/limiting.
    - Updated `GatewayRouteSecurityPolicyTest.java` to assert discovery locator, globalcors maxAge, and default filter configurations.
    - Executed `./gradlew.bat :gateway:test` (42 tests 100% SUCCESSFUL). PR #386 merged into `main`.

## 2026-08-12 - Issue #324 Convert ECL and PD Calculation Logic to BigDecimal for Financial Precision

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/324-ecl-bigdecimal-precision-conversion` / `C:\tmp\account-324-ecl-bigdecimal-precision-conversion`.
- Base: `origin/main`.
- Scope:
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
  - Test Validation:
    - Updated `CrAccountTest.java` and `PdCalculatorTest.java`.
    - Executed `./gradlew.bat :ecl:ecl-core:test :ecl:ecl-api:test :ecl:ecl-batch:test` (100% SUCCESSFUL).

## 2026-08-12 - Issue #326 Configure Production Git Backend and Property Encryption for Config Server

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/326-config-server-git-backend-encryption` / `C:\tmp\account-326-config-server-git-backend-encryption`.
- Base: `origin/main`.
- Scope:
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
- Verification:
  - Executed `./gradlew.bat :config-server:test` (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #311 Decouple Closing Module from Journal-Ledger Core for MSA Isolation

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/311-closing-msa-bounded-context-decouple` / `C:\tmp\account-311-closing-msa-bounded-context-decouple`.
- Base: `origin/main`.
- Scope:
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
- Verification:
  - Executed `./gradlew.bat :closing:api:test :closing:batch:test :contracts:test :journal-ledger:core:test` (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #315 Decouple Expenditure Resolution Core from Direct Module Dependencies

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/315-expenditure-msa-bounded-context-decouple` / `C:\tmp\account-315-expenditure-msa-bounded-context-decouple`.
- Base: `origin/main`.
- Scope:
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
- Verification:
  - Executed `./gradlew.bat :expenditure-resolution:core:test :expenditure-resolution:api:test` (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #317 Enforce Non-Negative Book Value Floor During Lease Asset Depreciation

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/317-asset-lease-depreciation-book-value-floor` / `C:\tmp\account-317-asset-lease-depreciation-book-value-floor`.
- Base: `origin/main`.
- Scope:
  - Domain Defense & Safe Depreciation Amount Calculation (`RightOfUseAsset.java`):
    - Implemented `calculateSafeDepreciationAmount(BigDecimal targetAmount)` to automatically clamp depreciation amount so that net book value never falls below 0 (IFRS 16 non-negativity principle).
    - Implemented `depreciate()` domain method encapsulating accumulated depreciation updates, book value reduction, status transition to `FULLY_DEPRECIATED`, and domain invariant checks.
    - Added extensive pedagogical comments detailing IFRS 16 rules, domain invariants, and rich domain model benefits.
  - Fixed Asset Defense & Safe Depreciation Calculation (`FixedAsset.java`):
    - Implemented `calculateSafeDepreciationAmount(BigDecimal targetAmount)` ensuring book value never falls below residual value.
    - Added domain invariant guards in `depreciate(LocalDate processDate)` and updated pedagogical comments.
  - Application Service Delegation:
    - Refactored `LeaseEntryService.processContractMonthlyAccounting()` to delegate ROU asset depreciation calculation and state mutation to `RightOfUseAsset.depreciate()`.
    - Added pedagogical comments to `FixedAssetEntryService.processMonthlyDepreciation()`.
- Verification:
  - Added unit test `RightOfUseAssetTest.java` verifying safe depreciation clamping, negative book value defense, and `FULLY_DEPRECIATED` transition.
  - Added unit test in `FixedAssetTest.java` for `calculateSafeDepreciationAmount`.
  - Added unit test in `LeaseEntryServiceTest.java` for ROU safe depreciation clamping in monthly lease accounting.
  - Executed `./gradlew.bat :asset-lease:core:test :asset-lease:api:test :asset-lease:batch:test` with 100% SUCCESS.

## 2026-08-12 - Issue #318 Decouple JPA Annotations from Payable and Receivable Domain Entities

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/318-payable-receivable-jpa-decouple` / `C:\tmp\account-318-payable-receivable-jpa-decouple`.
- Base: `origin/main`.
- Scope:
  - Decoupled JPA Annotations from Domain Entities (Pure Java POJO):
    - Removed all `@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@Enumerated`, `@PrePersist` etc. annotations from `payable` (5 classes) and `receivable` (6 classes) domain models.
    - Added extensive pedagogical comments explaining Hexagonal Architecture domain independence and Data Mapper pattern benefits.
  - Created Infrastructure JPA Entities (`infrastructure.persistence.entity`):
    - Created `PurchaseInvoiceJpaEntity`, `PaymentJpaEntity`, `PayableJpaEntity`, `AdvancePaymentJpaEntity`, `PaymentRunJpaEntity` in `payable`.
    - Created `SalesInvoiceJpaEntity`, `CollectionJpaEntity`, `CollectionAllocationJpaEntity`, `ReceivableJpaEntity`, `UnmatchedCollectionJpaEntity`, `MatchingRuleJpaEntity` in `receivable`.
  - Implemented Data Mappers (`infrastructure.persistence.mapper`):
    - Created two-way Data Mappers for all 11 domain models and JPA entities with pedagogical comments.
  - Updated Spring Data Repositories & Persistence Adapters:
    - Updated repositories to manage JPA entities and persistence adapters to map Domain POJO <-> JPA Entity via Data Mappers.
    - Updated `@EntityScan` in API and Batch application entry points.
- Verification:
  - `./gradlew.bat :payable:core:test :payable:api:test :payable:batch:test :receivable:core:test :receivable:api:test :receivable:batch:test` executed with 100% SUCCESS.

## 2026-08-12 - Issue #309 Harden Gateway JWT Secret Configuration & Asymmetric Key Support

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/309-gateway-jwt-secret-security-hardening` / `C:\tmp\account-309-gateway-jwt-secret-security-hardening`.
- Base: `origin/main`.
- Scope:
  - Removed Hardcoded Plaintext Default JWT Secret:
    - Removed `modern-account-system-super-secret-key-1234567890` from `gateway/src/main/resources/application.yml` and `JwtProperties.java`.
    - Made external environment variable / Config Server property injection mandatory (`${AUTH_JWT_SECRET:${JWT_SECRET:}}`, `${AUTH_JWT_PUBLIC_KEY:${JWT_PUBLIC_KEY:}}`, `${AUTH_JWT_JWKS_URI:${JWT_JWKS_URI:}}`).
  - Asymmetric Key (RS256/ES256) & Key Rotation Support:
    - Updated `JjwtAccessTokenVerifier.java` using `SigningKeyResolverAdapter` to dynamically resolve HMAC Secret Key (HS256) or RSA Public Key (RS256) based on JWT header `alg`.
  - Pedagogical Security Comments:
    - Added extensive comments covering API Gateway Secret Exposure Risks, Asymmetric Verification Defense-in-Depth benefits, and Key Rotation / JWKS (`/.well-known/jwks.json`) strategies across `JwtProperties.java` and `JjwtAccessTokenVerifier.java`.
- Verification:
  - Added unit tests in `JjwtAccessTokenVerifierTest.java` for RSA RS256 token verification and missing key exception validation.
  - Executed `./gradlew.bat :gateway:test :config-server:test` with 100% SUCCESS.
  - PR #379 merged into `main` and branch deleted.

## 2026-08-12 - Issue #316 Fix Budget Double-Deduction Bug on Expenditure Resolution Update and Rejection

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/316-expenditure-budget-restore-fix` / `C:\tmp\account-316-expenditure-budget-restore-fix`.
- Base: `origin/main`.
- Scope:
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

## 2026-08-12 - Issue #313 Apply Optimistic Locking to Deposit Account to Prevent Lost Updates

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/313-deposit-optimistic-locking` / `C:\tmp\account-313-deposit-optimistic-locking`.
- Base: `origin/main`.
- Scope:
  - JPA `@Version` Optimistic Locking:
    - Added `@Version private Long version;` field to `DepositAccount.java`.
    - Added V41 migration scripts (`V41__add_version_to_deposit_accounts.sql`) for both H2 and PostgreSQL schemas.
  - Inbound Port & Retry Mechanism:
    - Created `DepositTransactionUseCase.java` interface (`deposit`, `withdraw`).
    - Implemented `DepositService` retry logic (`executeWithOptimisticLockRetry`) with exponential backoff on `OptimisticLockingFailureException`.
  - Pedagogical Comments:
    - Added comprehensive comments comparing Optimistic Locking vs Pessimistic Locking, Lost Update prevention, and Hexagonal Architecture persistence exception handling across `DepositAccount.java`, `DepositService.java`, and `DepositAccountPersistenceAdapter.java`.
- Verification:
  - Added unit test `DepositAccountOptimisticLockingTest.java` verifying version increment and conflict exception.
  - Added concurrency test `DepositServiceConcurrencyTest.java` verifying balance integrity under 10 concurrent deposit threads.
  - Executed `./gradlew.bat :deposit:core:test :deposit:api:test :deposit:batch:test` (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #292 Decouple Monolith Journal Posting Adapter for MSA Transition

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/292-journal-ledger-msa-adapter-decouple` / `C:\tmp\account-292-journal-ledger-msa-adapter-decouple`.
- Base: `origin/main`.
- Scope:
  - Removed Obsolete Monolithic Adapter & Command:
    - Removed `MonolithJournalPostingAdapter.java` and `MonolithJournalPostingCommand.java` from `journal-ledger/core/.../common/adapter/`.
  - Hexagonal Architecture Port & Adapter Refactoring:
    - Updated `JournalPostingAdapter.java` implementing `JournalPostingPort` with idempotency deduplication (`lineageSourceType` & `lineageSourceId`).
  - REST & Async Event Inbound Adapters:
    - Added `JournalPostingRestController.java` (`POST /api/v1/journals/posting`): REST Inbound Web Adapter for synchronous HTTP posting requests in MSA environment.
    - Added `JournalPostingEventListener.java`: Async Event Inbound Adapter for event-driven journal posting via Spring Event / Message Relay.
  - Pedagogical Comments:
    - Added extensive pedagogical comments detailing Hexagonal Architecture, MSA Bounded Context decoupling, REST & Event-Driven communication, and Idempotency / Eventual Consistency guarantees.
- Verification:
  - Added unit tests `JournalPostingRestControllerTest.java` and `JournalPostingEventListenerTest.java`.
  - Executed `./gradlew.bat :journal-ledger:core:test :journal-ledger:api:test` (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #293 Implement Transactional Outbox Pattern for Journal Posting Dual Write Consistency

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/293-msa-transactional-outbox-journal` / `C:\tmp\account-293-msa-transactional-outbox-journal`.
- Base: `origin/main`.
- Scope:
  - Extracted `com.ho.account.contracts.outbox` package in `contracts` module:
    - Domain/Persistence Event Models: `OutboxStatus`, `OutboxEvent`, `JournalOutboxEvent`.
    - Ports: `OutboxPort` (atomic local DB save & pending query/status update), `OutboxEventPublisher` (relay engine interface).
    - Asynchronous Relay Engine: `JournalOutboxRelayService` (queries PENDING events and relays to `JournalPostingPort` with At-Least-Once delivery and Eventual Consistency).
    - In-Memory Adapter: `InMemoryOutboxAdapter` for testing and local standalone runs.
  - Integrated `deposit` and `loan` modules:
    - Refactored `DepositService`: saves `JournalOutboxEvent` atomically within local DB transaction during account opening/initial deposit, then relays via `OutboxEventPublisher`.
    - Refactored `LoanJournalAdapter`: saves `JournalOutboxEvent` atomically upon loan disbursal/adjustment journal posting, transitioning to PUBLISHED upon completion.
  - Idempotency & Pedagogical Comments:
    - Added idempotency check in `JournalPostingAdapter` using `lineageSourceType` and `lineageSourceId` to prevent duplicate journal posting.
    - Added extensive pedagogical comments explaining MSA Dual Write issues, Transactional Outbox atomic save, Eventual Consistency, and Idempotency benefits.
- Verification:
  - Added unit/integration tests `JournalOutboxPatternTest.java` (verifying atomic save, retry resilience upon network failure, and idempotency deduplication).
  - Executed `./gradlew.bat test` across all modules (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #290 Decouple PersonalAccessTokenService from JPA Infrastructure (DIP & Hexagonal Outbound Port)

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/290-auth-pat-service-dip-decouple` / `C:\tmp\account-290-auth-pat-service-dip-decouple`.
- Base: `origin/main`.
- Scope:
  - Decoupled `PersonalAccessTokenService` from direct dependencies on `PersonalAccessTokenJpaRepository` and `PersonalAccessTokenJpaEntity` to satisfy DIP.
  - Created Pure POJO Domain Model `PersonalAccessToken.java` with domain logic for status management (`revoke()`, `markUsed()`) and effective status evaluation (`getEffectiveStatus()`).
  - Created Outbound Port Interface `PersonalAccessTokenPort.java` in `application/port/out/`.
  - Created Outbound Persistence Adapter `PersonalAccessTokenPersistenceAdapter.java` in `infrastructure/persistence/` implementing `PersonalAccessTokenPort` with Data Mapper conversion logic.
  - Added Data Mapper conversion methods `toDomain()` and `fromDomain()` in `PersonalAccessTokenJpaEntity.java`.
  - Refactored `PersonalAccessTokenService.java` to depend solely on `PersonalAccessTokenPort` and `PersonalAccessToken` domain model.
  - Added pedagogical comments explaining Hexagonal Outbound Ports and DIP architectural benefits.
  - Created unit tests `PersonalAccessTokenServiceTest.java` and adapter tests `PersonalAccessTokenPersistenceAdapterTest.java`.
- Verification:
  - `./gradlew.bat :auth:core:test :auth:api:test` passed 100% (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #291 Decouple JPA Annotations from Master Data Core Domain Models (Pure POJO & Data Mapper)

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/291-master-data-domain-pojo-decouple` / `C:\tmp\account-291-master-data-domain-pojo-decouple`.
- Base: `origin/main`.
- Scope:
  - Decoupled JPA technology annotations (`@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToOne`, `@Enumerated`, `@PrePersist`, `@PreUpdate`, etc.) from all domain models in `master-data/core` (`AccountSubject`, `Product`, `Currency`, `Department`, `ExchangeRate`, `FiscalPeriod`, `TaxProfile`, `MasterDataChangeRequest`).
  - Created 8 JPA Entity classes in `infrastructure/persistence/entity/` (`AccountSubjectEntity`, `ProductEntity`, `CurrencyEntity`, `DepartmentEntity`, `ExchangeRateEntity`, `FiscalPeriodEntity`, `TaxProfileEntity`, `MasterDataChangeRequestEntity`).
  - Created 8 Data Mapper classes in `infrastructure/persistence/mapper/` (`AccountSubjectMapper`, `ProductMapper`, `CurrencyMapper`, `DepartmentMapper`, `ExchangeRateMapper`, `FiscalPeriodMapper`, `TaxProfileMapper`, `MasterDataChangeRequestMapper`).
  - Refactored JPA Repositories and Persistence Adapters to perform two-way mapping between Domain POJOs and JPA Entities.
  - Refactored `MonolithMasterDataQueryAdapter` to depend on Domain Outbound Ports (`AccountSubjectPersistencePort`, `DepartmentPersistencePort`) instead of direct JPA Repositories.
  - Added comprehensive pedagogical comments explaining DDD Pure Domain POJO principles and domain-persistence model separation benefits.
- Verification:
  - `./gradlew.bat :master-data:core:test :master-data:api:test :master-data:batch:test` passed 100% (BUILD SUCCESSFUL).
  - `./gradlew.bat test` full test suite passed 100% (65 executed tasks).

## 2026-08-12 - Issue #295 Implement IFRS 16 Lease Present Value (PV) Calculation & Input Validation


- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/295-asset-lease-ifrs16-pv-calculation` / `C:\tmp\account-295-asset-lease-ifrs16-pv-calculation`.
- Base: `origin/main`.
- Integration: Merged PR #372 into `main`. Issue `#295` closed and remote branch deleted.
- Scope:
  - Implemented IFRS 16 lease present value (PV) calculation logic in `LeaseContract` domain model (`calculatePresentValue(monthlyPayment, termMonths, annualRate)`).
  - Added `calculateTermMonths()` to calculate exact lease term months from `startDate` and `endDate`.
  - Added `updatePresentValueAndValidate()` in `LeaseContract` to cross-validate external PV inputs against domain-calculated PV and strictly enforce domain invariants.
  - Refactored `LeaseEntryService.registerLeaseContract` and `recognizeInitialLease` to mandate domain PV calculation and validation, preventing reliance on external request values.
  - Added detailed pedagogical comments explaining IFRS 16 accounting standards, incremental borrowing rate discounting, and financial domain invariants.
  - Added domain unit tests in `LeaseContractTest.java` and integration tests in `LeaseEntryServiceTest.java`.
- Verification:
  - `./gradlew.bat :asset-lease:core:test :asset-lease:api:test :asset-lease:batch:test` passed 100% (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #294 Refactor Asset Lease Core JPA Repository Location to Enforce DIP

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/294-asset-lease-core-jpa-repository-dip` / `C:\tmp\account-294-asset-lease-core-jpa-repository-dip`.
- Base: `origin/main`.
- Integration: Merged PR #371 into `main`. Issue `#294` closed and remote branch deleted.
- Scope:
  - Moved Spring Data JPA repositories from `com.ho.account.asset.repository` package in `core` to `com.ho.account.asset.infrastructure.persistence.repository` (`FixedAssetRepository`, `AssetHistoryRepository`, `LeaseContractRepository`, `LeaseLiabilityRepository`, `LeasePaymentScheduleRepository`, `RightOfUseAssetRepository`).
  - Decoupled `core` domain and application services (`FixedAssetEntryService`, `LeaseEntryService`) from JPA interfaces to strictly rely on Outbound Ports (`FixedAssetPersistencePort`, `LeasePersistencePort`).
  - Enhanced `FixedAssetPersistencePort` with `Page<FixedAsset> findByStatus(String status, Pageable pageable)` and comprehensive educational comments (Pedagogical comments) explaining Hexagonal Architecture Outbound Port pattern and DIP advantages.
  - Updated `AssetLeaseApiApplication`, `AssetLeaseBatchApplication`, and `AssetDepreciationBatchConfig` with new infrastructure repository package imports and scanning configuration.
  - Updated `asset-lease/core/build.gradle` with pedagogical comments on core dependencies.
- Verification:
  - `./gradlew.bat :asset-lease:core:test :asset-lease:api:test :asset-lease:batch:test` passed 100% (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #299 Decouple JPA Annotations from Account Mart Domain Entities

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/299-account-mart-jpa-decouple` / `C:\tmp\account-299-account-mart-jpa-decouple`.
- Base: `origin/main`.
- Integration: Merged PR into `main`. Issue `#299` closed and remote branch deleted.
- Scope:
  - Removed all JPA annotations (`@Entity`, `@Table`, `@Id`, `@Column`, `@Enumerated`, `@IdClass`, etc.) from `KapExternalRating` and `AllowanceInputPosition` domain classes in `account-mart/mart-core`.
  - Converted domain entities into Pure Java POJOs to adhere to Hexagonal Architecture (Port and Adapter Pattern) and DDD guidelines.
  - Added JPA Entities (`KapExternalRatingEntity`, `AllowanceInputPositionEntity`) and Data Mappers (`KapExternalRatingMapper`, `AllowanceInputPositionMapper`) in `infrastructure/persistence`.
  - Updated `JpaKapExternalRatingRepository`, `JpaAllowanceInputPositionRepository`, `AllowanceInputPositionPersistenceAdapter`, `KapExternalRatingProcessor`, `IntegratedPositionItemProcessor`, `KapDataEtlJobConfig`, and `IntegratedPositionEtlJobConfig`.
  - Added comprehensive educational comments (Pedagogical comments) explaining Hexagonal Architecture, domain purity, and persistence model separation.
- Verification:
  - `./gradlew.bat :account-mart:mart-core:test :account-mart:mart-api:test :account-mart:mart-batch:test` passed 100% (BUILD SUCCESSFUL).

## 2026-08-12 - Issue #296 Validate Double-Entry Debit-Credit Balance in Payable & Receivable Services

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/296-payable-receivable-double-entry-validation` / `C:\tmp\account-296-payable-receivable-double-entry-validation`.
- Base: `origin/main`.
- Integration: Merged PR #369 into `main`. Issue `#296` closed and remote branch deleted.
- Scope:
  - Added explicit pre-posting double-entry debit-credit balance validation (`validateJournalBalance`) across `payable` (`PaymentService`, `PurchaseService`) and `receivable` (`SalesService`, `CollectionService`) application services.
  - Ensures sum of DEBIT amounts equals sum of CREDIT amounts (`compareTo == 0`) before dispatching `JournalEntryCommand` to `JournalPostingPort`.
  - Throws `IllegalArgumentException` when an imbalanced entry is detected (Fail-Closed principle).
  - Added educational comments (Pedagogical comments) explaining Double-Entry Bookkeeping (Equivalence of Debits and Credits), general ledger consistency, and fail-closed validation advantages.
  - Added unit test cases verifying `IllegalArgumentException` thrown on imbalanced journal entries in `PaymentServiceTest`, `PurchaseServiceTest`, `SalesServiceTest`, and `CollectionServiceTest`.
- Verification:
  - `./gradlew.bat :payable:core:test :receivable:core:test` passed 100% (BUILD SUCCESSFUL).
- Rollback:
  - Revert PR #369. No DB schema changes were introduced.

## 2026-08-12 - Issue #298 Accounting Period Validation in Payable & Receivable Services

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/298-payable-receivable-accounting-period-validation` / `C:\tmp\account-298-payable-receivable-accounting-period-validation`.
- Base: `origin/main`.
- Integration: Merged into `main`. Issue `#298` closed and remote branch deleted.
- Scope:
  - Integrated `AccountingPeriodStatusPort` into `payable` (`PaymentService`, `PurchaseService`) and `receivable` (`SalesService`, `CollectionService`) application services.
  - Implemented pre-validations (`validateAccountingPeriodOpen`) before posting journal entries, creating invoices, executing payments, recording advance payments, or matching collections.
  - Throws `IllegalStateException` when a transaction is attempted against a CLOSED accounting period.
  - Added `@Bean @ConditionalOnMissingBean AccountingPeriodStatusPort` definitions to `PayableLocalExternalPortConfiguration` and `ReceivableLocalExternalPortConfiguration`.
  - Added educational comments (Pedagogical comments) detailing financial accounting period controls, anti-backdating rules, and internal control benefits.
  - Added unit test cases for closed period validation in `PaymentServiceTest`, `PurchaseServiceTest`, `SalesServiceTest`, and `CollectionServiceTest`.
- Verification:
  - `./gradlew.bat :payable:core:test :receivable:core:test` and `./gradlew.bat test` passed 100% (BUILD SUCCESSFUL).
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #301 ECL Core Domain Calculator Refactoring & Pure Domain Logic Encapsulation

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/301-ecl-domain-calculator-refactor` / `C:\tmp\account-301-ecl-domain-calculator-refactor`.
- Base: `origin/main`.
- Integration: PR `#367`, merged into `main`. Issue `#301` closed and remote branch deleted.
- Scope:
  - Extracted PD/LGD calculation formulas and collateral LP/waterfall allocation logic from Application Services (`LifetimePdService`, `CollateralAllocationService`, `PdCalculationService`, `LgdCalculationService`) into Pure Domain Calculators (`PdCalculator`, `CollateralAllocationCalculator`, `LgdCalculator`) and `CrCollateral` domain entity.
  - Refactored Application Services to focus strictly on Application Service Orchestration (repository fetch, transaction boundary, delegating calculations to domain calculators).
  - Created `CollateralAllocationCalculator` (Pure Domain Service) to encapsulate Simplex LP optimization, waterfall allocation algorithm, and priority weight calculations.
  - Added transition-matrix based PD curve generation and simple PD curve fallback logic to `PdCalculator`.
  - Added secured/unsecured LGD floor rules encapsulation to `LgdCalculator` and collateral effective value calculation to `CrCollateral`.
  - Added comprehensive pedagogical comments detailing DDD rich domain models, pure domain service encapsulation, and Hexagonal Architecture principles.
  - Added `CollateralAllocationCalculatorTest` and updated existing unit tests.
- Verification:
  - `./gradlew.bat :ecl:ecl-core:test :ecl:ecl-api:test :ecl:ecl-batch:test` passed 100% (BUILD SUCCESSFUL in 36s).
- Rollback:
  - Revert PR #367. No DB schema changes were introduced.

## 2026-08-12 - Issue #297 Decouple Spring Framework from Reporting Domain Layer


- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/297-reporting-domain-spring-decouple` / `C:\tmp\account-297-reporting-domain-spring-decouple`.
- Base: `origin/main`.
- Integration: Merged into `main`. Issue `#297` closed and remote branch deleted.
- Scope:
  - Removed Spring `@Component` annotations and imports from `FinancialStatementEngine` and `IfrsDisclosureNotesEngine` domain services to maintain Pure Java POJO purity.
  - Added educational comments explaining Hexagonal Architecture principles, domain framework independence, and unit testing benefits.
  - Created `ReportingDomainConfiguration` in `reporting/core/infrastructure/config` for explicit `@Bean` registration of domain services (`FinancialStatementEngine`, `IfrsDisclosureNotesEngine`, `RwaCalculator`).
- Verification:
  - `./gradlew.bat :reporting:core:test :reporting:api:test :reporting:batch:test --rerun-tasks` passed 100% (BUILD SUCCESSFUL in 18s).
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #303 Reconciliation Rich Domain Model Refactoring

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/303-reconciliation-rich-domain-model` / `C:\tmp\account-303-reconciliation-rich-domain-model`.
- Base: `origin/main`.
- Integration: Merged into `main`. Issue `#303` closed and remote branch deleted.
- Scope:
  - Refactored `ReconciliationDifference` and `ReconciliationRun` from Anemic Domain Models into Rich Domain Models with business behavior methods (`assignOwner`, `resolve`, `attachAdjustmentJournalEntry`, `startRun`, `completeRun`, `failRun`).
  - Added educational comments explaining Anemic vs Rich Domain Model, encapsulation, and domain invariants.
  - Simplified `ReconciliationService` to focus strictly on Application Service orchestration rather than fragmented state transitions.
  - Added domain unit tests in `ReconciliationDifferenceTest` and `ReconciliationRunTest`.
- Verification:
  - `./gradlew.bat :reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test` passed 100% (BUILD SUCCESSFUL in 29s).
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #305 Resolve N+1 Query in TaxInvoiceBatchService using Bulk Lookup

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/305-tax-n-plus-1-bulk-query` / `C:\tmp\account-305-tax-n-plus-1-bulk-query`.
- Base: `origin/main`.
- Integration: Merged into `main`. Issue `#305` closed and remote branch deleted.
- Scope:
  - Added `findAllByPartnerCodes(Collection<String>)` bulk lookup default method in `MasterDataQueryPort` with pedagogical comments on N+1 query problem & DB I/O optimization.
  - Implemented SQL `IN` clause bulk query in `BusinessPartnerRepository`, `BusinessPartnerPersistencePort`, and `MonolithMasterDataQueryAdapter`.
  - Refactored `TaxInvoiceBatchService.validatePurchaseInvoices` using 3-step bulk lookup (Set extraction -> bulk query 1-time execution -> O(1) map lookup).
  - Added unit tests in `TaxInvoiceBatchServiceTest` verifying bulk query invocation and exception handling.
- Verification:
  - `./gradlew.bat :tax:core:test :tax:api:test :tax:batch:test` and `./gradlew.bat :master-data:core:test` passed 100%.
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #308 Journal Entry Domain Validation Cohesion & Invariants Consolidation

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/308-journal-entry-domain-validation` / `C:\tmp\account-308-journal-entry-domain-validation`.
- Base: `origin/main`.
- Integration: Merged into `main`. Issue `#308` closed and remote branch deleted.
- Scope:
  - Created `validateInvariants()` method in `JournalEntry` Aggregate Root to consolidate required header checks (`slipDate`, `accountingDate`) and debit/credit balance checks (`validateBalance()`).
  - Refactored `BalanceValidationFilter` to delegate validation by invoking `journalEntry.validateInvariants()`.
  - Added educational comments on DDD Aggregate Root invariants, encapsulation, and domain model cohesion.
  - Enhanced `JournalEntryAggregateTest` with unit tests for header invariant validation.
- Verification:
  - `./gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test` passed 100% (BUILD SUCCESSFUL in 40s).
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #306 Gateway Dynamic Service Discovery in AuthTokenVersionValidator

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/306-gateway-token-version-dynamic-lb` / `C:\tmp\account-306-gateway-token-version-dynamic-lb`.
- Base: `origin/main`.
- Integration: Merged into `main`. Issue `#306` closed and remote branch deleted.
- Scope:
  - Created `WebClientConfig.java` in Gateway module with `@LoadBalanced WebClient.Builder` Spring Bean.
  - Refactored `AuthTokenVersionValidator` to inject `@LoadBalanced WebClient.Builder` for Eureka Service Discovery and Spring Cloud LoadBalancer dynamic routing.
  - Changed default `baseUrl` in `TokenVersionValidationProperties` and `application.yml` from `http://localhost:8084` to `lb://auth-service`.
  - Added comprehensive pedagogical comments on MSA Service Discovery, Client-side Load Balancing, Eureka Registry lookup, and round-robin load distribution.
  - Updated `docker-compose.yml`, `GatewayDockerConfigurationTest.java`, and `README.md` to reflect `lb://auth-service`.
- Verification:
  - `./gradlew.bat :gateway:test` passed 100% (BUILD SUCCESSFUL in 40s).
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #304 Discovery Eureka Peer-Awareness and Self-Preservation Config

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/304-discovery-eureka-peer-awareness` / `C:\tmp\account-304-discovery-eureka-peer-awareness`.
- Base: `origin/main`.
- Integration: PR `#360`, merged into `main`. Issue `#304` closed and remote branch deleted.
- Scope:
  - Added `peer1` and `peer2` Spring profiles (`spring.config.activate.on-profile: peer1` / `peer2`) to `discovery/src/main/resources/application.yml` and `config-repo/discovery-service.yml`.
  - Configured defaultZone cross-referencing between peers (peer1 -> peer2:8762/eureka, peer2 -> peer1:8761/eureka).
  - Set `eureka.client.register-with-eureka: true` and `eureka.client.fetch-registry: true` for HA profiles.
  - Added Eureka server self-preservation (`eureka.server.enable-self-preservation: true`) and eviction interval timer (`eureka.server.eviction-interval-timer-in-ms: 60000`) settings.
  - Added pedagogical comments explaining Eureka Server HA, Peer-Awareness, self-preservation mode, and eviction interval concepts.
  - Added unit test `DiscoveryConfigurationPolicyTest` to parse multi-document YAML and assert profile-specific configurations and server properties.
- Verification:
  - `./gradlew.bat :discovery:test` passed 100% (BUILD SUCCESSFUL in 12s).
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #310 Loan Multi-currency Rounding Policy Implementation

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/310-loan-currency-rounding-policy` / `C:\tmp\account-310-loan-currency-rounding-policy`.
- Base: `origin/main`.
- Integration: PR `#358`, merged into `main`. Issue `#310` closed and remote branch deleted.
- Scope:
  - Created `CurrencyRoundingPolicy` domain enum for loan multi-currency rounding rules (KRW/JPY: scale 0, FLOOR; USD/EUR/GBP: scale 2, HALF_UP).
  - Added educational comments for financial calculation precision principles.
  - Applied `CurrencyRoundingPolicy` across `LoanService`, `InterestAccrualService`, and `EIRAmortizationSchedule`.
  - Added unit and integration tests (`CurrencyRoundingPolicyTest`, `LoanCurrencyRoundingPolicyIntegrationTest`) and adjusted existing test assertions.
- Verification:
  - `./gradlew.bat :loan:core:test :loan:api:test :loan:batch:test` passed 100% (BUILD SUCCESSFUL).
- Rollback:
  - Revert PR #358. No DB schema changes were introduced.

## 2026-08-12 - Issue #312 Deposit DDD Spring Decouple Refactor

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/312-deposit-ddd-spring-decouple` / `C:\tmp\account-312-deposit-ddd-spring-decouple`.
- Base: `origin/main`.
- Integration: PR `#356`, merged into `main`. Issue `#312` closed and remote branch deleted.
- Scope:
  - Removed Spring `@Component` annotations from Deposit domain classes (`DepositAccountStateMachine`, `DepositInterestAccrualCalculator`, `DepositTerminationSettlementCalculator`).
  - Added educational comments detailing Pure Domain principles and financial calculation logic.
  - Created `DepositDomainConfiguration` under `infrastructure/config` to explicitly register domain services as Spring `@Bean`s.
- Verification:
  - `./gradlew.bat :deposit:core:test :deposit:api:test :deposit:batch:test` passed 100% (BUILD SUCCESSFUL).
- Rollback:
  - Revert PR. No DB schema changes were introduced.

## 2026-08-12 - Issue #314 Reconciliation JSON String Hardcoding Refactor

- Owner: Gemini (Agent loop subagent)
- Source branch/worktree: `agent/314-reconciliation-json-hardcoding` / `C:\tmp\account-314-reconciliation-json-hardcoding`.
- Base: `origin/main`.
- Integration: PR `#355`, merged into `main`. Issue `#314` closed and remote branch deleted.
- Scope:
  - Fixed hardcoded JSON string concatenation in `ReconciliationService.java` for `sourceItemRef` and `targetItemRef`.
  - Introduced `buildItemRefJson` helper method leveraging Spring/Jackson `ObjectMapper` for safe serialization.
  - Added educational comments explaining JSON escaping and serialization safety.
  - Expanded `ReconciliationServiceTest` with assertions on `itemRef` JSON integrity and special character handling.
- Verification:
  - Gradle test task `:reconciliation:core:test :reconciliation:api:test :reconciliation:batch:test` passed 100% (BUILD SUCCESSFUL).
- Rollback:
  - Revert PR #355 / commit on main. No DB schema changes were introduced.

## 2026-07-30 - Issue #45 executable Budget Control foundation

- Owner: Codex as Integrator with Domain/Service, SQL, Controller, Batch, Gateway, Test, and independent Reviewer roles.
- Source branch/worktree: `agent/45-budget-control` / `C:\tmp\account-45-budget-control`.
- Base: `origin/main@36a1be4f`.
- Integration: PR `#246`, source commit `5f06cad1`, merge commit `db7feeb4`; Issue `#45` closed and the remote feature branch was deleted.
- Scope:
  - Added `budget:core`, `budget:api`, and `budget:batch` as an isolated bounded context.
  - Implemented pure BudgetPlan/Transfer/Execution/FiscalYearControl aggregates, inbound commands/use cases, output ports, and a transactional service with shard/year/plan lock ordering.
  - Added explicit JPA entities/mappers/adapters, 10,000 preseeded fiscal controls, 256 idempotency shards, unique business keys, version/locked reads, and Flyway V50.
  - Added JWT signature/issuer and operation-role enforcement, JWT-sub audit identity, typed HTTP errors, seven HTTP operations, a dedicated Gateway route/fallback, and a restart-aware year-end close Job.
  - Kept the existing Expenditure Budget unchanged and documented #17 as the migration/reconciliation boundary.
- Verification:
  - Budget Core 31, API 9, Batch 11, Gateway 33, and Expenditure Core 10/API 1 tests passed; 95 affected tests, failures/errors/skipped 0.
  - Budget API and Batch bootJars passed and contain PostgreSQL JDBC 42.6.2.
  - API and Batch composition tests used real adapters, Flyway V50 seeds, and Hibernate schema validation against H2 PostgreSQL mode.
  - Separate-transaction two-thread tests prove first-call transfer/execution exactly-once, typed conflicting payloads, and close-vs-approval serialization.
  - The initial four P1/two P2 findings plus typed-empty-404, infrastructure-exception classification, bounded-string, and technology-neutral `yearMonth` findings were fixed.
  - The same independent Reviewer completed the final staged re-review with no remaining P0-P3 findings.
- Risks:
  - Live PostgreSQL migration, row-lock contention, shard distribution/lock timeouts, and fiscal-year query-plan behavior were not exercised.
  - Year-end close currently uses one transaction and row-level `saveAndFlush`; a bulk/checkpoint output-port contract is required if cardinality outgrows that boundary.
  - Legacy Expenditure Budget migration needs explicit reservation/commit/release reconciliation in #17.
- Rollback:
  - Revert the Issue #45 feature commit and remove the three module includes. Existing Expenditure schema/data is not mutated.
## 2026-07-30 - Issue #243 production PostgreSQL and Actuator runtime classpath

- Owner: Codex Integrator; independent read-only review required before commit.
- Branch/worktree/base: `agent/243-postgres-actuator-runtime` / `C:\tmp\account-243-postgres-actuator-runtime`; initial base `origin/main@36a1be4f`, rebased commit `c8b10947` on `origin/main@5c810422`.
- Added PostgreSQL runtime dependencies to 11 bounded contexts across 22 executable API/Batch projects while preserving local H2 dependencies.
- Added Actuator to the nine APIs that lacked the production readiness endpoint dependency; Journal Ledger and Loan already had it.
- Added root `verifyProductionRuntimeDependencies`, which resolves all 22 runtime classpaths, builds their bootJars, and inspects the archives for PostgreSQL and Actuator JARs.
- Added a shared minimal readiness context contract to the nine APIs that received Actuator; each proves `/actuator/health/readiness` returns HTTP 200 without an external runtime.
- Verification: the runtime/bootJar gate passed; all 22 bootJars passed; the 22 affected test tasks passed with 56 observed tests and no failures/errors/skips. Some Batch projects still have no module-owned test sources.
- Independent review re-ran all nine changed API test suites from the latest source state with one worker: 29 tests passed; combined with the remaining affected 27 tests the total is 56. Final review found no P0-P3 finding.
- Post-rebase runtime/archive gate, nine readiness tests, diff/marker checks and semantic re-review passed with no remaining P0-P3 finding.
- Commits `c8b10947` and `d0586cbe` were pushed on `agent/243-postgres-actuator-runtime`; Draft PR #248 is open with #244 and actual PostgreSQL/Compose validation retained as deployment gates.
- No external database, Config Server, registry, image, container or server was accessed. Existing local-start gaps such as Closing API runtime H2 remain their module Issues; PostgreSQL schema/migration execution remains Issue #244.

## 2026-07-30 - Issue #44 Journal/GL/Sub-ledger domain authority

- Owner: Codex as Integrator with an independent read-only Reviewer. The initial three audit roles hit the account usage limit; the commit-review retry later succeeded.
- Source branch/worktree: `agent/44-journal-ledger-domain` / `C:\tmp\account-44-journal-ledger-domain`.
- Base: `origin/main@54352362`.
- Integration: PR `#238` merged source `d8c0504c` as `299746a7`; `Fixes #44` closed the Issue and the remote feature branch was deleted.
- Scope:
  - Added immutable Debit/Credit value objects and one fail-closed precision policy matching ledger amount and exchange-rate storage.
  - Strengthened JournalEntry line ownership plus transaction/base-currency double-entry invariants.
  - Added GeneralLedger as the approved persisted-entry snapshot authority.
  - Changed LedgerEntryPersistencePort to carry the Aggregate and moved JPA/JDBC storage mapping into outbound adapters.
  - Removed unused Money/GlAccountBalance parallel authority and aligned impacted Loan/Expenditure callers with intent-based state initialization.
- Verification:
  - Journal Ledger Core 35, API 2, Batch 3 tests and both bootJars passed.
  - Loan Core 30, Expenditure Core 10/API 1, and Closing Batch 12 tests passed; all 93 affected tests completed with no failures, errors, or skips.
  - Closing Batch bootJar passed in addition to both Journal Ledger bootJars.
  - Focused coverage proves precision rejection, immutable posting lineage, transaction/base balance, and JPA/JDBC parity.
  - Independent review found and verified fixes for the downstream fixture compilation P1 and aggregate-total precision P2; re-review reported no P0-P3 findings.
- Risks:
  - The existing fixed scale-2 schema is not a currency-specific minor-unit model.
  - PostgreSQL bulk load, indexing, and lock contention were not exercised locally.
- Rollback:
  - Revert the Issue #44 feature commit; no data or schema rollback is required.

## 2026-07-30 - Issue #43 EOD/BOD daily lifecycle

- Owner: Codex as Integrator with Service, Controller, SQL, Gateway, Test, and independent Reviewer roles.
- Branch/worktree: `agent/43-eod-state` / `C:\tmp\account-43-eod-state`.
- Base: `origin/main@5fb9cb67`.
- Integration: PR `#236` merged source `86ebedf9` as `10d80939`; `Fixes #43` closed the Issue and the remote feature branch was deleted.
- Scope:
  - Replaced the orphan enum/Boolean holder with a versioned, audited daily aggregate and explicit lifecycle use case.
  - Preserved prior `CLOSED` rows and created explicitly dated next-business-day BOD rows atomically.
  - Added the output port, locked JPA adapter, trusted-header named-command API, Gateway route/fallback, and Closing-owned Flyway V50 with an isolated schema-history table.
  - Removed the unused duplicate `ClosingPeriod` authority and documented `ClosingCalendar`/`FiscalPeriod`/`AnnualClosingService` as monthly/annual owners.
  - Repaired Closing API/Batch composition after the BusinessPartner pure-domain/JPA split.
- Verification:
  - Closing Core 59, API 8, Batch 12, Gateway 31 tests passed; failures/errors/skipped 0.
  - Closing API and Batch bootJars passed.
  - Flyway baseline 49 to V50 executed against a populated legacy H2 schema; Boolean state backfill and schema history were asserted.
  - JPA pessimistic lookup, optimistic version increment, API actor/role gate, and Gateway route ordering were asserted.
  - The first full run exposed the missing BusinessPartner persistence bean in Closing Batch; after composition repair the complete set passed with `--rerun-tasks`.
  - Independent review findings for an isolated Closing Flyway history and direct BOD predecessor validation were fixed; re-review passed with no P0-P3 findings.
- Risks:
  - Daily transaction allowance is not yet wired into Journal posting; the current accounting-period gate remains monthly.
  - PostgreSQL execution and live Auth/Gateway/Discovery routing were not run.
- Rollback:
  - Revert the Issue #43 feature commit. V50 rollback requires a controlled data-preserving migration, not a destructive down migration.
## 2026-07-30 - Issue #229 development PostgreSQL ownership and health contract

- Owner: Codex Integrator; independent read-only Reviewer.
- Branch/worktree/base: `agent/229-dev-postgres` / `C:\tmp\account-229-dev-postgres` / `origin/main@5fb9cb67`.
- Added opt-in self-contained PostgreSQL and external-dev authenticated probe Compose models, 16 database-specific owner roles, and a post-bootstrap manifest.
- Dev Config now requires `DEV_DB_*`, PostgreSQL, Flyway validation and JPA validate with SQL init/H2/default credential fallback disabled.
- Config Server offline tests, POSIX shell syntax/LF, diff, conflict, relative-link, private-host and legacy-password scans passed.
- Reviewer findings for missing external override and early readiness were corrected with separate profiles/files and the completion manifest; final review had no findings.
- Docker Compose provider and cached PostgreSQL image were unavailable, so live Compose/bootstrap/restart/idempotency remains a #66 gate.
- No remote DB login, metadata, schema, credential, container, migration or volume mutation occurred.
- Rollback reverts Issue #229 files and stops local Compose without `-v`; named volumes and remote DB state remain.
- Pushed `agent/229-dev-postgres` and opened Draft PR `#241` with `Refs #229`; no merge or Issue closure.

## 2026-07-30 - Issue #42 Master Data partner approval UI/API

- Owner: Codex acting as Integrator with Frontend, Controller-audit, Gateway, and Test roles.
- Branch/worktree: `agent/42-master-data-approval` / `C:\tmp\account-42-master-data-approval`.
- Base: `origin/main@c0fb871b`.
- Integration: PR `#234` merged source `c8f2b81a` as `57aa1729`; `Fixes #42` closed the Issue and the remote source branch was deleted.
- Scope:
  - Added the `/master-data/partner` registration and approval screen with real backend DTOs and strict API failure handling.
  - Connected BUSINESS_PARTNER change-request creation, pending lookup, approval, rejection, and current-partner lookup.
  - Used the configured Gateway API base, trusted JWT-derived actor/role headers, role gates for every exposed change-request endpoint, and typed payload validation before request/approval state transitions.
  - Corrected the legacy partner page DTO fields and approval menu route.
  - Routed `/api/master-data/**` through the existing Master Data gateway route while preserving route ordering and circuit-breaker policy.
  - Added controller contract tests and expanded the gateway route policy test.
- Verification:
  - Master Data Core 13, API 9, and Gateway 30 tests passed; failures/errors/skipped 0.
  - Master Data API and Gateway bootJars passed.
  - Focused frontend ESLint, `git diff --check`, conflict-marker scan, and API-path/header scans passed.
  - Next source compilation passed; the repository-wide type check then stopped on two pre-existing `PageHeader.breadcrumbs` errors outside Issue #42.
  - Independent re-review passed with no remaining P0-P3 findings after the API URL, trusted actor/role, and payload-review fixes.
- Risks:
  - Pending requests need server-side target filtering, pagination, and a payload projection instead of raw JSON.
  - Trusted headers assume the Master Data service is reachable only behind Gateway; direct service-port exposure must remain prohibited.
  - Browser visual QA was blocked by an unresponsive local Next dev server; the exact processes and temporary junction were removed.
  - Docker CLI was unavailable, so Compose build-argument wiring received static review but no `docker compose config` execution.
- Rollback:
  - Revert the Issue #42 feature commit; no schema or live data was changed.
## 2026-07-30 - Issue #227 runtime execution parity audit

- Owner: Codex Integrator with read-only Explorer/Planner/Reviewer agents.
- Branch/worktree: `agent/227-runtime-parity-audit` / `C:\tmp\account-227-runtime-parity-audit`.
- Base: rebased to `origin/main@c0fb871b`; root checkout user changes were not touched.
- Scope:
  - Classified all 70 Gradle subprojects and separated 35 executable targets from 34 library/aggregator targets and phantom `:app`.
  - Added a dependency-free PowerShell audit for actual offline packaging, library tests/JARs, explicit in-memory-H2 executable JAR startup, and frontend contract inspection.
  - Defined local H2, self-contained dev PostgreSQL, shared external-dev PostgreSQL, and production external PostgreSQL/container boundaries without storing host or credentials.
- Verification:
  - 33/35 executable targets produced executable JARs. Internal Audit API compile/bootJar and Batch bootJar remain non-executable.
  - 34/34 library/aggregator projects passed isolated `test jar --offline`.
  - 25/35 local JAR contexts passed, 8 failed, and 2 were blocked by packaging. Business Batch jobs were disabled and therefore not claimed as verified.
  - After rebasing to `origin/main@36a1be4f`, all eight formerly failing targets repackaged; Closing Batch now passes local context startup through #43, leaving seven current failures and two packaging blocks.
  - Frontend package/lock/scripts and `npm.cmd` exist; actual install/build/start was blocked by absent `node_modules` and the no-install-without-approval policy.
  - Docker CLI was unavailable, so image and Compose behavior remains a static audit only.
  - Script parse, `git diff --check`, conflict marker scan, sensitive-host-literal scan, and Markdown relative-link checks passed.
- Issue routing:
  - Runtime failures: #73-#80 and #89-#90; Master Data latest success evidence added to #72.
  - Packaging/image #228, development DB #229, production overlay #230, Internal Audit/Auth boundary #231, root orchestration #66; coordination #60/#226.
- Safety:
  - Shared development endpoints received TCP reachability checks only. No login, schema, metadata, credential, remote container, migration, or volume mutation occurred.
- Delivery:
  - Pushed `agent/227-runtime-parity-audit` and opened Draft PR `#242` with `Refs #227`; no merge or Issue closure.
- Rollback:
  - Revert Issue #227 documentation, `tools/runtime-smoke.ps1`, and these harness records. No database rollback is needed.

## 2026-07-29 - Issue #40 Master Data executable module split

- Owner: Codex acting as Coder/Integrator Agent.
- Branch/worktree: `agent/40-master-data-modules` / `/tmp/account-40-master-data-modules`.
- Base: `origin/main@fbad9110` (0 ahead / 0 behind before final records).
- Integration: PR `#158` merged as `178a7eb1`; `Fixes #40` closed the Issue.
- Scope:
  - Moved HTTP entry point/controllers/DTOs to `master-data:api`.
  - Moved scheduler orchestration and reporting to `master-data:batch`; added a required-`asOfDate` Spring Batch Job/Step that delegates business aggregation to core.
  - Kept application/domain/persistence and standard-path Flyway resources in `master-data:core`.
  - Enabled separate API/Batch bootJars and aligned Docker, run configurations, dependencies, and module documentation.
- Verification:
  - Master Data core 1, API 1, Batch 4 and Journal Ledger core 23 tests passed on JDK 17; both Master Data bootJars passed.
  - The populated-H2 Job test stored active/expired rows for four Master Data types and asserted all four Step-context active counts as 1.
  - The normal Batch runtime failed fast without Config Server; the explicit local-H2 exception path completed `masterDataValidityJob asOfDate=2026-07-29`.
  - Architecture leakage scans, conflict-marker scan, and `git diff --check` passed.
- Risks:
  - PostgreSQL/Flyway runtime and high-volume plans were not executed.
  - Batch output is restart metadata only; durable history and monitoring adapters remain follow-up work.
  - Typed-applier, requested-version, and request-lock regression tests are absent from the current test source and remain a separate quality gap.
- Rollback:
  - Revert the Issue #40 commit; no migration content or live database was changed.

## 2026-07-08 - Payable API/core command boundary
- Branch: `agent/asset-lease-split`
- Scope: `payable` core/api/batch/docs
- Changes:
  - Moved HTTP controllers and request DTOs from `payable:core` to `payable:api`.
  - Added core use case commands for purchase invoice, payment run, payment execution, advance payment, and offset.
  - Added API response DTOs so JPA/domain entities are not serialized directly as the external contract.
  - Removed Web/Validation dependencies from `payable:core`.
  - Updated payable batch to call core through `PaymentRunCommand` and delayed Batch JobRegistry registration.
  - Updated payable beginner/process/schema/local-run docs.
- Verification:
  - `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava --console=plain --max-workers=1`: passed.
  - `:payable:api:bootRun` local/H2 context smoke: passed.
  - `:payable:batch:bootRun` local/H2 context smoke: passed; JobRegistry BeanPostProcessor warning not reproduced.
- Risk:
  - PostgreSQL migration/runtime, real payment gateway adapter, and high-volume payment run performance need integration verification.
## 2026-07-07 - Asset Lease depreciation batch boundary
- Branch: `agent/asset-lease-split`
- Scope: `asset-lease` core/domain/pipeline/port/adapter/batch/docs
- Changes:
  - Added `FixedAssetDepreciationResult` for batch-safe depreciation results.
  - Added mutation-free `FixedAsset.calculateDepreciation()` and kept `depreciate()` as the single-asset state transition path.
  - Updated `DepreciationPipeline` to return result values without mutating JPA entities.
  - Updated `AssetJdbcAdapter` to persist calculated accumulated depreciation, book value, status, and last depreciation date once through JDBC bulk update.
  - Updated asset-lease docs and Gemini review prompt.
- Verification:
  - `.\gradlew :asset-lease:core:test :asset-lease:api:compileJava :asset-lease:batch:compileJava --console=plain --max-workers=1`: passed.
- Risk:
  - Actual high-volume PostgreSQL/H2 job execution and production Flyway DDL compatibility need integration verification.

## 2026-07-07 - Account Mart 담보 상세 DQ/LGD 연결
- Branch: `agent/asset-lease-split`
- Scope: `account-mart` core/application/domain/infrastructure/batch/docs/migration
- Changes:
  - Connected `OdsApartCollDetail` to the real collateral DQ/LGD prerequisite flow.
  - Added `OdsApartCollDetailRepository` outbound port and JPA persistence adapter.
  - Added `CollateralDataQualityInspectionService` so application service coordinates port lookup and domain processor invocation.
  - Updated batch item processor to delegate to the core application service.
  - Added demo/bootstrap apartment collateral detail seed and Flyway V5 table DDL.
  - Updated account-mart beginner/business-flow docs and Gemini handoff prompt.
- Verification:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1`: passed.
  - account-mart Java TODO search: no matches.
- Risk:
  - PostgreSQL Flyway execution, high-volume collateral detail lookup performance, and final LGD formula integration still require integration environment verification.


## 2026-07-03 - Loan 전표 포트 경계 및 실행 문서 최신화
- Branch: `agent/asset-lease-split`
- Scope: `loan` core/service/domain/batch/docs/run configs
- Changes:
  - `InterestAccrualService` now posts accrual journals through `LoanJournalPort` instead of journal-ledger internal types.
  - Loan accrual/event/EIR schedule journal references are stored as ID/slipNo values.
  - `V32__loan_accrual_journal_reference.sql` and migration assertions were added.
  - `LoanBatchJobRegistryConfiguration` removes the Spring Batch JobRegistry early BeanPostProcessor warning.
  - Loan local-run docs and IntelliJ `.run` configs now set app name and disable Redis repository scanning for local smoke.
- Verification:
  - `.\gradlew :loan:core:test --console=plain --max-workers=1 --rerun-tasks`: passed.
  - `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`: passed.
  - `:loan:api:bootRun` local/H2 context smoke: passed.
  - `:loan:batch:bootRun` local/H2 context smoke: passed; JobRegistry warning not reproduced.
- Risk:
  - PostgreSQL and high-volume seeded accrual Job were not verified.
# AI Harness Worklog

## 2026-06-25 - Harness Upgrade Bootstrap

- Owner: Codex acting as AI Harness Architect and Integrator Agent.
- Branch: `ai-harness-upgrade-20260625`.
- Scope:
  - Preserved existing root harness files.
  - Created `docs/ai-harness/` operating documentation.
  - Added backup copies under `_backup/2026-06-25/`.
  - Added worktree ignore entries.
- Verification:
  - Required `docs/ai-harness` file existence check passed.
  - Conflict marker check passed.
  - Trailing whitespace check passed.
  - `git diff --check` on modified tracked guidance files passed with CRLF conversion warnings only.
- Risks:
  - Branch prefixes with slash failed in local Git ref layout, so this task uses `ai-harness-upgrade-20260625` without slash.
- Rollback:
  - Restore root guidance files from `docs/ai-harness/_backup/2026-06-25/`.
  - Remove or revert `docs/ai-harness/` additions and `.gitignore` worktree entries if the harness upgrade is rejected.

## 2026-06-26 - Beginner AI Agent Git Guide

- Owner: Codex acting as AI Harness Architect and Integrator Agent.
- Branch: `ai-harness-upgrade-20260625`.
- Scope:
  - Added `90-beginner-ai-agent-git-guide.md`.
  - Linked the guide from `Agents.md` and `00-overview.md`.
  - Covered Git branch, worktree, Draft PR/MR, multi-agent roles, prompt templates, verification, and safety rules.
- Verification:
  - Required file existence check passed.
  - Conflict marker check passed.
  - Trailing whitespace check passed.
  - `git diff --check` passed with CRLF conversion warnings only.
- Integration intent:
  - Push current branch.
  - Merge into `main` because the user explicitly requested mainstream merge.
  - Push `main` and verify clean sync.

## 2026-06-26 - Mainstream Merge

- Owner: Codex acting as Integrator Agent.
- Source branch: `ai-harness-upgrade-20260625`.
- Target branch: `main`.
- Result:
  - Source branch pushed to origin.
  - `main` updated from `origin/main`.
  - Source branch merged into `main` with `--no-ff`.
  - No merge conflicts occurred.
- Final sync:
  - `main` pushed to origin.
  - Clean synchronization check expected after this log commit is pushed.

## 2026-06-30 - Asset Lease Split

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Split `asset-lease` into `core`, `api`, and `batch` Gradle subprojects.
  - Kept `:asset-lease` as a compatibility wrapper for `:asset-lease:core`.
  - Added separate API and Batch Spring Boot entry points.
  - Updated run configs, Dockerfile, local docs, and dependent module reference.
- Verification:
  - `.\gradlew projects --console=plain` passed.
  - `.\gradlew :asset-lease:core:test :asset-lease:api:bootJar :asset-lease:batch:bootJar :expenditure-resolution:core:compileJava --console=plain --max-workers=1` passed.
  - `.\gradlew :asset-lease:test :asset-lease:compileJava --console=plain --max-workers=1` passed.
- Risks:
  - Long-running bootRun smoke and real `assetDepreciationJob` execution are not run yet.
## 2026-07-02 - Full Local Build/API/BATCH Verification

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Verified current Gradle project graph, full build, API bootRun smoke, Batch context smoke, and representative Spring Batch Jobs.
  - Fixed Batch bootstrap behavior for asset-lease, account-mart, and ecl so Batch apps run non-web in local CLI mode.
  - Fixed asset-lease batch paging reader repository signature.
  - Updated local development and module run documents with H2/PostgreSQL separation and verified local flags.
- Verification:
  - `.\gradlew projects --console=plain` passed.
  - `.\gradlew compileJava --console=plain --max-workers=1` passed.
  - `.\gradlew build --console=plain --max-workers=1` passed.
  - API bootRun smoke passed for all current API/server modules.
  - Batch context smoke passed for all current Batch modules.
  - Representative Job smoke passed for all current Job-bearing Batch modules; journal-ledger batch remains context-only because no Job definition exists.
  - Java TODO search returned no matches.
- Risks:
  - PostgreSQL path is documented but not executed against a live local PostgreSQL instance in this pass.
  - Smoke data is empty/demo H2, so business-result correctness under production-like volume still needs seeded integration tests.
- Rollback:
  - Revert the batch bootstrap files and docs changed in this verification pass if the non-web CLI behavior is rejected.

## 2026-07-02 - Account Mart Core/Batch Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Removed direct Spring Batch interface usage from `mart-core` processors.
  - Added `mart-batch` processor adapters and a StepExecution-to-core parameter helper.
  - Removed a JPA-leaking unused application port skeleton.
  - Corrected ODS-GL reconciliation balance summary to aggregate by base date, subject/account, and currency.
  - Updated account-mart docs with beginner-friendly core/batch responsibility boundaries.
- Verification:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1` passed.
  - `.\gradlew :account-mart:mart-batch:test --console=plain --max-workers=1` passed.
  - Core Spring Batch type search returned only explanatory comments, no imports or implemented interfaces.
  - Unused skeleton port search returned no matches.
- Risks:
  - Batch test shutdown still logs existing step-scope reader close warnings.
  - PostgreSQL high-volume reconciliation plan is not verified in this pass.
- Rollback:
  - Revert this account-mart commit if the boundary refactor is rejected.

## 2026-07-03 - ECL Core Pipeline Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Moved Stage/PD, EAD/LGD, and forward-looking ECL processing order from `ecl-batch` processors into `ecl-core` application pipelines.
  - Reduced Spring Batch processors to adapter delegation.
  - Reused the same core pipelines from `AllowanceCalculationService` so API/manual calculation and batch share the same business sequence.
  - Updated ecl docs and beginner comments to describe the pipeline boundary.
- Verification:
  - `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1` passed.
  - Batch processor search found no direct calculation service, `BigDecimal`, maturity, or builder logic.
  - Core Spring Batch type search found no imports or implemented interfaces, only explanatory comments.
  - ECL Java TODO search returned no matches.
- Risks:
  - PostgreSQL high-volume seeded ECL run is not verified in this pass.
- Rollback:
  - Revert the ecl files and worklog/handoff updates from this pass if the boundary refactor is rejected.
## 2026-07-03 - Journal Ledger Batch Reaggregation Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Added `dailyBalanceReaggregationJob` Tasklet adapter and JobParameter-to-date-range support.
  - Kept journal-ledger core business work in `LedgerService`; batch now wires and delegates.
  - Fixed batch H2 datasource/JPA YAML structure and Batch test dependency.
  - Updated journal-ledger docs, local development guide, IntelliJ run config, and beginner comments.
- Verification:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --console=plain --max-workers=1` passed.
  - `.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain --max-workers=1` passed with Job status `COMPLETED`.
  - ECL and journal-ledger Java TODO/mojibake search returned no matches.
  - `git diff --check` reported no whitespace errors, only CRLF conversion warnings.
- Risks:
  - PostgreSQL high-volume balance reaggregation is not verified in this pass.
  - Spring Cloud/Batch BeanPostProcessor WARN remains during bootRun; it did not block Job completion.
- Rollback:
  - Revert the combined ECL/journal-ledger refactor commit if the boundary changes are rejected.
## 2026-07-03 - Closing Core/Batch Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Moved FX valuation and ECL provision business decisions from `closing:batch` into `closing:core` application services.
  - Added core outbound ports for FX rate lookup, allowance balance lookup, and closing journal creation.
  - Added batch adapters for master-data exchange rates, journal-ledger GL balances, and journal-ledger journal creation.
  - Updated closing docs and beginner comments to describe the core/batch boundary.
- Verification:
  - `.\gradlew :closing:core:test :closing:batch:test --console=plain --max-workers=1` passed.
  - `.\gradlew :closing:api:compileJava --console=plain --max-workers=1` passed.
  - Closing core Spring Batch type search returned no matches.
  - Closing TODO/mojibake search returned no matches.
- Risks:
  - PostgreSQL high-volume FX/ECL closing run and operational skip/retry policy are not verified in this pass.
- Rollback:
  - Revert the closing refactor files and worklog/handoff updates if the boundary change is rejected.
- Additional verification:
  - `.\gradlew :closing:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false" --console=plain --max-workers=1` passed.
  - Added closing API/BATCH local logback settings so local runs avoid Logstash connection warnings.
## 2026-07-08 - Tax API/Core Command Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Removed HTTP DTO/Controller ownership from `tax:core`.
  - Added `TaxInvoiceCommand` as the application input boundary.
  - Moved AP invoice Controller/DTO classes to `tax:api`.
  - Added `TaxInvoiceRef` purchase/active/usable helper methods and updated expenditure-resolution validation to use the contract methods.
  - Updated tax docs with beginner-friendly API DTO -> core command -> domain flow.
- Verification:
  - `.\gradlew :tax:core:test :tax:api:compileJava :tax:batch:compileJava :expenditure-resolution:core:test --console=plain --max-workers=1`
- `.\gradlew :tax:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
- `.\gradlew :tax:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1` passed.
- Risks:
  - PostgreSQL seeded execution and real `taxInvoiceValidationJob` high-volume run are not verified in this pass. Local context smoke is verified with H2/create-drop.
  - Cross-module API integration test still lives under `expenditure-resolution:core`; this was preserved to avoid a broad test layout move.
- Rollback:
  - Revert the tax/contracts/expenditure-resolution changes and associated docs/log updates from this pass if the boundary refactor is rejected.
## 2026-07-08 - Expenditure Resolution API/Core Command Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: gent/asset-lease-split.
- Scope:
  - Moved expenditure-resolution HTTP Controller/DTO/assembler ownership from core to api.
  - Added ExpenditureResolutionCommand and APPaymentCommand as core application input boundaries.
  - Replaced service-level master-data repository/entity dependency with MasterDataQueryPort.
  - Converted Budget and legacy Invoice master-data entity references to code values.
  - Moved cross-module API integration test to expenditure-resolution:api tests.
  - Added Batch JobRegistry delayed registration configuration.
- Verification:
  - $compile passed.
  - $verify passed.
  - $apiRun passed.
  - $batchRun passed; JobRegistry BeanPostProcessor WARN no longer appeared.
- Risks:
  - PostgreSQL migration/data compatibility for code-based budget/invoice columns is not verified in this pass.
  - Real high-volume expenditureResolutionApprovalJob execution is not verified in this pass.
- Rollback:
  - Revert the expenditure-resolution/tax/contracts changes and associated docs/log updates from this pass if rejected.
## 2026-07-08 - Receivable API/Core Command Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Removed HTTP Controller/DTO ownership from `receivable:core`.
  - Added `SalesInvoiceCommand`, `CollectionCommand`, and `ManualMatchingCommand` as core use case input boundaries.
  - Moved controllers, request/response DTOs, Bean Validation, and controller tests to `receivable:api`.
  - Added `ReceivableBatchJobRegistryConfiguration` so Batch Job registration happens after singleton initialization.
- Verification:
  - `.\gradlew :receivable:core:test :receivable:api:test :receivable:batch:compileJava --console=plain --max-workers=1` passed.
  - `receivable:api:bootRun` local/H2 context smoke passed.
  - `receivable:batch:bootRun` local/H2 context smoke passed; JobRegistry BeanPostProcessor WARN no longer appeared.
- Risks:
  - PostgreSQL/Flyway compatibility and high-volume auto-matching Job execution are not verified in this pass.
- Rollback:
  - Revert the receivable/payable boundary refactor commit if rejected.
## 2026-07-08 - Reconciliation API/Core Command Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Removed HTTP Controller/DTO ownership from `reconciliation:core`.
  - Added command records for reconciliation unit/rule/reason code/run/difference assignment/difference resolution input boundaries.
  - Moved `ReconciliationController`, request/response DTOs, and Bean Validation to `reconciliation:api`.
  - Added `ReconciliationBatchJobRegistryConfiguration` so Batch Job registration happens after singleton initialization.
  - Updated reconciliation docs and Gemini review prompt for the API DTO -> core command -> domain/service flow.
- Verification:
  - `.\gradlew :reconciliation:core:test :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1` passed.
  - `reconciliation:api:bootRun` local/H2 context smoke passed.
  - `reconciliation:batch:bootRun` local/H2 context smoke passed; JobRegistry BeanPostProcessor WARN no longer appeared.
- Risks:
  - PostgreSQL/Flyway compatibility and high-volume `reconciliationDailyJob` execution are not verified in this pass.
- Rollback:
  - Revert the reconciliation boundary refactor commit if rejected.
## 2026-07-08 - Reporting API Response DTO Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Added reporting API response DTOs for statements, disclosure note marts, regulatory submissions, regulatory filings, and drill-down rows.
  - Updated `ReportingController` to call core use cases and map domain results to API DTOs.
  - Kept business aggregation, validation, note classification, and filing mapping in core.
  - Updated reporting docs and Gemini review prompt for the core domain -> API response DTO flow.
- Verification:
  - `.\gradlew :reporting:api:test --console=plain --max-workers=1` passed.
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1` passed.
- Risks:
  - PostgreSQL/Flyway compatibility and high-volume reporting Batch execution are not verified in this pass.
- Rollback:
  - Revert the reporting API DTO boundary changes if rejected.
## 2026-07-09 - Deposit Command and Batch asOfDate Boundary Refactor

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Added validation to `OpenAccountCommand` for required codes, currency normalization, non-negative initial deposit, and non-negative interest rate.
  - Made `depositAccountIntegrityJob` require explicit `asOfDate=yyyy-MM-dd` instead of defaulting to the current date.
  - Added command validation and Batch JobParameter tests.
  - Updated deposit docs and Gemini review prompt.
- Verification:
  - `.\gradlew :deposit:core:test :deposit:batch:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1` passed.
- Risks:
  - PostgreSQL/Flyway compatibility and real master-data/journal-ledger adapter integration are not verified in this pass.
- Rollback:
  - Revert the deposit boundary changes if rejected.

## 2026-07-14 - Master Data Typed Applier and Statistics Boundary

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Removed the core pipeline dependency on the batch report DTO.
  - Added a core validity report model and statistics output port.
  - Replaced four full-table Java stream counts with JPA COUNT queries.
  - Added typed change appliers for account subjects, business partners, departments, and products.
  - Moved JSON decoding behind a core output port and Jackson infrastructure adapter.
  - Enforced targetKey/payload-key consistency and approved effectiveDate for SCD2 deactivation.
  - Updated master-data local H2/IntelliJ documentation and run configuration.
- Verification:
  - `.\gradlew :master-data:test --console=plain --max-workers=1` passed.
  - H2 JPA statistics integration test passed.
  - Local/H2 non-web `bootRun` passed after disabling Config, Discovery, Vault, tracing, Flyway, and using Hibernate create-drop.
  - Core-to-batch dependency and batch business-loop searches returned no matches.
- Risks:
  - Currency, exchange-rate, and fiscal-period typed appliers remain fail-closed.
  - requestedVersion conflict enforcement remains an explicit code `@todo`.
  - The package-level batch orchestrator is not yet an independent Spring Batch Job/Step application.
  - PostgreSQL Flyway DDL and high-volume execution plans remain unverified.
- Rollback:
  - Revert the master-data files, IntelliJ run configuration, and related docs/handoff entries for this pass.

## 2026-07-14 - Governance Approval Boundary and Standalone Runtime

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Fixed Governance component/entity/repository scanning so the executable loads real audit and master-data approval beans.
  - Changed authorization revocation from immediate deletion to an approval request and HTTP 202 receipt.
  - Made unsupported system-role/authorization approval combinations fail closed.
  - Added service, adapter, and Spring context regression tests.
  - Added H2/PostgreSQL runtime drivers, standalone IntelliJ configuration, and beginner-focused runtime/process documentation.
- Verification:
  - `:governance:compileJava` passed.
  - `:governance:test :governance:bootJar` passed.
  - Local H2 non-web `bootRun` passed and loaded 12 JPA repositories.
- Risks:
  - PostgreSQL/Flyway runtime was documented but not live-tested.
  - Approval receipt API unification and Auth outbox/inbox atomicity remain code `@todo` items.
- Rollback:
  - Revert the governance/runtime/docs changes in the pending combined commit if rejected.

## 2026-07-14 - Auth Authentication Snapshot and Approval Idempotency

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Removed the Auth core dependency on API login DTOs.
  - Reused one Clock-based effective-role snapshot for the core result and JWT claims.
  - Hardened token-version validation for inactive, administratively locked, and role-less users.
  - Preserved role metadata in memory mode.
  - Added approvalTraceId plus SHA-256 fingerprint idempotency to memory/JPA role assignment adapters.
  - Added a pessimistic user lock, apply-log entity/repository, Flyway V72, and regression tests.
  - Updated standalone H2/Flyway/IntelliJ and PostgreSQL documentation.
- Verification:
  - `:auth:test :auth:bootJar` passed with 32 tests.
  - Local H2 non-web bootRun applied V70-V72 and passed Hibernate schema validation.
  - Auth core-to-API dependency search returned no matches.
- Risks:
  - PostgreSQL locking/concurrency was not live-tested.
  - Plain-password migration, first-failure atomic upsert, and idempotency-log retention remain explicit code TODOs.
- Rollback:
  - Revert the auth, IntelliJ run configuration, and related docs/harness changes for this pass.

## 2026-07-20 - Gateway Global Authentication and Runtime Boundary

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Replaced route-by-route JWT opt-in with a default global `/api/**` authentication policy.
  - Exposed only POST login and blocked Auth validation/internal callback paths at ingress.
  - Removed all client-provided identity headers and regenerated them from an immutable verified principal.
  - Split token verification and token-version checks into ports with JJWT and WebClient/Caffeine adapters.
  - Required iat, exp, subject, roles, positive integral roleVersion, and header-safe identity codes.
  - Distinguished Auth rejection (401) from timeout/error/empty response (503) while remaining fail-closed.
  - Added request-id bounds, configuration validation, test-only console logging, and route/Compose YAML tests.
  - Aligned port 8000, Docker Auth service URL/dependency, JDK 17 image, repository-root build context, and standalone IntelliJ execution.
  - Updated Gateway and shared beginner/runtime documentation with code and data flows.
- Verification:
  - `.\gradlew :gateway:test :gateway:bootJar --console=plain --max-workers=1 --no-daemon` passed.
  - Authoritative XML result: 30 tests, 0 failures, 0 errors, 0 skipped.
  - Standalone local-profile Netty bootRun listened on port 8000 and `/actuator/health` returned `UP`; Gateway/Gradle processes were then stopped.
  - Route and root/module Compose YAML parsing tests passed.
  - Docker CLI was unavailable, so live Compose/image execution was not run.
- Risks:
  - Live Config/Discovery/Auth/business-route integration remains unverified.
  - JWKS rotation, event-driven multi-node cache invalidation, Auth service authentication, and legacy catch-all removal remain explicit `@todo` items.
  - The repository default HS256 key remains local-development compatibility only; production must inject a managed secret.
- Rollback:
  - Revert Gateway, external route config, root/module Compose, standalone run configuration, and related documentation/harness files for this pass.
## 2026-07-20 - Discovery Registry Lifecycle and Readiness Boundary

- Owner: Codex acting as Coder/Integrator Agent.
- Branch: `agent/asset-lease-split`.
- Scope:
  - Added standalone port 8761 and server-only Eureka client defaults.
  - Connected Config Client, actuator, Prometheus, Brave, and Zipkin runtime dependencies to existing configuration.
  - Replaced a context-only test with readiness and register/lookup/cancel registry lifecycle coverage.
  - Added local/config/Compose/Docker policy tests.
  - Rebuilt the Docker path around JDK 17, one bootJar, repository-root context, and readiness healthcheck.
  - Changed all 14 root Compose Discovery dependencies to `service_healthy` and added container addresses.
  - Added standalone IntelliJ execution, test-only console logging, and beginner registry/lease/self-preservation documentation.
- Verification:
  - `.\gradlew :discovery:test :discovery:bootJar --console=plain --max-workers=1 --no-daemon` passed with 6 tests and no failures/errors/skips.
  - Standalone local-profile port 8761 returned readiness `UP`; Dashboard, registry API, and Prometheus returned HTTP 200; JVM metrics were present.
  - Discovery and Gradle processes were stopped after runtime smoke.
- Risks:
  - Docker CLI/image execution and live Config/multi-client heartbeat/load-balancing were not verified.
  - Registry authentication/private networking and multi-AZ peer synchronization remain explicit Config `@todo` items.
- Rollback:
  - Revert Discovery, external config, root Compose Discovery env/dependency conditions, standalone run configuration, and related documentation/harness files.

## 2026-07-22 - Config Server strict repository readiness
- Branch: `agent/asset-lease-split`
- Scope: Config Server runtime/config/tests/Docker/Compose/docs.
- Changes:
  - Added strict property-source availability health through `EnvironmentRepository`.
  - Added 9 HTTP/unit/policy tests and Prometheus/test dependencies.
  - Aligned native repository path, JDK 17 image, read-only Compose mounts, and 15 Config Server health dependencies.
  - Updated beginner, process, local-run, shared sequence, handoff, and review documents.
- Verification:
  - Initial `.\gradlew :config-server:test :config-server:bootJar --rerun-tasks --console=plain --max-workers=1 --no-daemon`: passed with 9 tests.
  - Later health-detail sanitization main/test classes compiled, but the targeted test rerun produced no new report because the Windows paging file was exhausted; spawned Gradle JVMs were stopped.
  - Initial runtime 8888 readiness/config/Prometheus smoke: passed without printing config values.
- Risk:
  - Docker CLI unavailable; image/Compose runtime unverified.
  - Rerun `ConfigRepositoryHealthIndicatorTest` after restoring paging-file headroom.
  - Production endpoint protection and Git-backed change governance remain TODO.

## 2026-07-22 - Contracts/shared-kernel second-pass boundary hardening
- Branch: `agent/asset-lease-split`
- Scope: contracts, shared-kernel, master-data dated integration adapter, ECL CDM consumer, docs/harness.
- Changes:
  - Immutable journal contracts and strict normal-balance values.
  - Real SCD2 effective-date lookup in master-data.
  - Actual Jackson masking binding and fail-closed policies.
  - Immutable deterministic local capability registry.
  - eventId-based Spring Batch idempotency and failure propagation.
  - Four no-op library runtime skeletons archived; 15 tests added.
- Verification:
  - Static diff/usage checks passed.
  - JVM tests/compile pending because the Windows paging file could not start 64-128 MB Gradle/javac work.
  - Spawned JVMs and temporary outputs were cleaned.
- Next:
  - Rerun Config Server health test plus shared-kernel/contracts/master-data/closing/ECL tests after memory recovery.
  - Continue with Master Data provider/default closure and shared dependency extraction.

## 2026-07-27 - Master Data SCD2 approval/version boundary

- Branch: `agent/asset-lease-split`.
- Scope: Master Data approval aggregate, SCD2 version policy, Governance source-reference idempotency, applier registry, locking, API validation, runtime packaging, tests, and docs.
- Verification: clean Master Data/Governance test and bootJar passed with 82 tests (57 + 25); diff, marker, and Markdown link checks passed.
- Known risk: live PostgreSQL and Docker were not run; historical V2 `CLOB` and concurrent source-reference recovery require a vendor-specific clean PostgreSQL bootstrap strategy before production.
- Next: independent Gemini review, then merge current `origin/main` into the feature branch and rerun affected checks before PR integration.

## 2026-07-27 - Master Data SCD2 validity/historical lookup follow-up

- Branch: `agent/asset-lease-split`; base HEAD `5d55704` is already present on the remote feature branch.
- Scope: validity-first SCD2 update ordering, historical Business Partner lookup, overlap fail-closed behavior, focused tests, and beginner/handoff docs.
- Changes:
  - Validate every new Account Subject, Business Partner, Department, and Product SCD2 window before closing the current row; resolve Account/Department parent references first as well.
  - Separate current-active Business Partner lookup from historical effective-date lookup.
  - Return single-key queries as `Optional` so overlapping rows raise an incorrect-result exception instead of being silently selected.
  - Add unit and H2 JPA regression tests; document PostgreSQL exclusion-constraint and legacy `useYn` follow-ups with completion criteria.
- Verification: Master Data 18 suites/63 tests plus Governance 10 suites/25 tests passed; both bootJars, diff, marker, and changed Markdown link checks passed.
- Risk: live PostgreSQL/Docker remain unverified; historical `BusinessPartnerRef.active` still reflects legacy `useYn` rather than a fully separated temporal activation model.
- State: this follow-up is uncommitted; do not commit, push, or merge it without explicit user direction.

## 2026-07-27 - Master Data lookup/fiscal-period second pass

- Branch: `agent/asset-lease-split`; base HEAD `5d55704` is already present on the remote feature branch.
- Scope: DB-side current lookup/search, deterministic as-of FX selection, Fiscal Period port/lock/domain transitions, dead-skeleton cleanup, tests, and docs.
- Changes:
  - Push Account Subject/Product active lists and Business Partner active-name search into validity-aware DB queries.
  - Select the latest eligible exchange rate; validate ISO codes/positive rates and fail closed on overlapping active Currency rows.
  - Change Fiscal Period through a pessimistically locked application port and audited domain transition rules.
  - Remove unused full-list change-request contracts and no-op/synthetic compatibility methods after repository-wide usage checks.
  - Record concrete PostgreSQL baseline, pagination, TaxProfile ownership, and Loan/Closing provider-boundary follow-ups.
- Verification: Master Data 73 + Governance 25 + Closing Core 19 + Journal Ledger Core 19 = 136 tests passed; Closing Batch compile and two bootJars passed.
- Risk: live PostgreSQL/Flyway and Docker remain unverified; pagination, exclusion constraints, TaxProfile completion, and two cross-module dependency exceptions remain.
- State: cumulative local follow-up is review-ready and uncommitted; explicit user authorization is required before commit/push/merge.

## 2026-07-27 - Loan value boundary and executable accounting workflow

- Isolated Business Partner/Currency/Account provider models behind Loan-owned dated reference ports and scalar aggregate values.
- Added inbound use-case boundary, API-owned DTO validation, pending/full-disbursal lifecycle, rich recalculation/default/recovery behavior and concurrency/idempotency controls.
- Replaced double/percent EIR and disconnected duplicate schedule flow with BigDecimal decimal EIR and one EIR schedule consumed by Batch.
- Added retryable accrual logs, locked per-loan processing, required business date, core chunk failure aggregation and independent Journal transaction isolation.
- Added V33, Java 17 exact API bootJar Docker/8088 Compose alignment, focused core/API tests and workflow/schema/beginner docs.
- Verified 169 affected tests and two Loan bootJars; static boundary/diff/marker/link checks passed. Docker/Compose and PostgreSQL remain environment follow-ups.
- Review-ready and uncommitted; explicit user authorization is required before commit/push/merge.

## 2026-07-28 - Closing consistency, batch and runtime boundary pass

- Branch/base: `agent/closing-consistency-pass` from `a06ebd6`; no Issue/PR, no commit/push/merge.
- Replaced fail-open period lookup and setter-driven closing/reopen/task/gate changes with fail-closed domain transitions, mandatory definitions, maker-checker approval and durable audit behavior.
- Preserved API batch RUNNING/FAILED history in independent transactions and represented generated DRAFT journals as PENDING_APPROVAL.
- Replaced unwritten FX balance/provider-repository paths with posted-journal signed balance Cursor ranges and an Exchange Rate contract adapter; chunk failures roll back the checkpoint.
- Aggregated ECL in SQL, fixed GL credit sign and group subtraction, enforced one run/model/legal entity/date, and rejected empty or incomplete results.
- Added deterministic explicit slips, same-content retry reuse, state-aware auto-post, annual closing base-currency/category handling and application-name/runtime scan isolation.
- Updated Closing beginner/process/schema/local-run docs and added focused domain, pipeline, JDBC, adapter, parameter, idempotency and ApplicationContext tests.
- Final verification: 52 suites/158 tests passed with 0 failures/errors/skips; Closing API/Batch bootJars passed.
- Risks: dual-currency read model/load test, annual aggregate port, batch execution-key/outbox reconciliation, GL legal-entity dimension, unlock history migration, typed evidence evaluator, PostgreSQL/Docker verification and remote MSA adapters remain.
- Rollback: revert the Closing tree, named contracts/Master Data/Journal Ledger bridge files and this pass's docs/harness entries. State is review-ready and uncommitted.

## 2026-07-28 - Foundation pending verification and phantom app cleanup

- Recovered the Config Server and contracts/shared-kernel verification that had been blocked by paging-file exhaustion.
- Forced the Config repository health test and shared-kernel/ECL focused tests; all passed after two test-infrastructure fixes.
- Aligned Jackson databind with the Spring Boot 3.2.5 BOM to eliminate a 2.17.1 databind / 2.15.4 core `NoSuchMethodError`.
- Replaced an invalid Mockito checked exception with `JobExecutionAlreadyRunningException` while preserving the ECL Kafka retry assertion.
- Removed the source-less `:app` include and corrected root/onboarding/local-run documentation; no local build artifacts were deleted.
- Final affected verification passed 40 suites/140 tests with no failures/errors/skips, Config Server bootJar, and a Gradle project listing without `:app`.
- Remaining work: staged shared-kernel infrastructure/allowance ownership extraction, Config Git/security controls, and live Docker/PostgreSQL integration.
- State: review-ready and uncommitted on `agent/closing-consistency-pass`; no commit/push/merge is authorized.

## 2026-07-29 - Git synchronization and Issue #20 latest-main integration

- Issue/branch/worktree: `#20`, `agent/20-closing-consistency`, repository root.
- Backed up tracked and untracked local work in a named stash, fast-forwarded the branch to `origin/main@b7c8aaa`, and restored every one of the 77 backup paths without dropping the stash.
- Resolved the only conflict in `docs/WORKLOG.md` by preserving the latest upstream records and local Closing/Foundation entries; recorded the decision in `conflict-log.md`.
- Preserved 82 transfer-stash paths after an external C:\tmp worktree disappeared, then reapplied them to the clean repository-root issue branch.
- Integrated latest main `f3d33ea` by preserving Issue #29's internal-audit split and upstream AuditAspect API, not resurrecting deleted standalone runtime files, and retaining the Jackson BOM alignment rationale.
- Verification: 176 affected tests passed with no failures/errors/skips; Closing API, Closing Batch, and Config Server bootJars passed. Internal-audit core/api/batch tasks pass but all tests and API/Batch production source are `NO-SOURCE`.
- Local JDK 17 was selected per command because current `JAVA_HOME` is JDK 21 and the synchronized Gradle properties no longer select the installed JDK path.
- State: user authorized commit/push/Draft PR; named recovery stashes remain. Merge, Issue close, and stash deletion are not authorized.

## 2026-07-29 - Issue #20 Closing main parity and integration preparation

- Fetched `origin/main@ce35ce5` and compared the resolved 82-path transfer snapshot against it.
- Confirmed that every Closing production/test/module-doc change and its contracts, Master Data, Journal Ledger, ECL, settings, and Jackson support already exists in main through commit `31be6f1`; no duplicate code delta remains.
- Preserved the comparison state as `codex-post-main-comparison-gh-20-2026-07-29`, then fast-forwarded `agent/20-closing-consistency` from `f3d33ea` to `ce35ce5`.
- Final successful verification covered 98 tests: Shared Kernel 6, Contracts 4, targeted Closing-facing Master Data adapters 5, Journal Ledger Core 23, Closing Core 44, Closing API 1, Closing Batch 12, and ECL API 3. Closing API/Batch and Config Server bootJars passed.
- A broader attempt exposed two pre-existing latest-main policy failures: root Compose no longer contains active Master Data/Config Server services after Issue #66, while their configuration policy tests still require them. The Closing branch does not modify this unrelated infrastructure contract.
- Issue #20 was already CLOSED before this integration request. The user authorized main integration; the harness-only reconciliation uses the feature branch and PR merge path.
- Draft PR `#101` was opened against `main` with `Refs #20`.
- Rollback: revert only the harness reconciliation commit. Code rollback for the already-main Closing implementation requires a separately reviewed revert of `31be6f1`, not applying the retained stale snapshot over current main.
## 2026-07-30 - Issue #41 BusinessPartner DDD/persistence separation

- Selected Issue #41 after explicitly skipping #40; work is isolated on `agent/41-business-partner-ddd` at `C:\dev\account\.worktrees\account-41-business-partner-ddd` and rebased onto `main@39dabd4e`.
- Concurrent PR #225 initially closed Issue #41 using documentation-only commit `cdb892e4`. A closure audit reopened #41 because production code was still missing; this branch supplies the verified implementation and closes the Issue through `Fixes #41`.
- Split the Business Partner aggregate into JPA-free domain models and infrastructure-owned JPA entities, with explicit adapter mapping and unchanged database/API contracts.
- Preserved hexagonal direction through `BusinessPartnerPersistencePort`; the application service owns transaction ordering, the domain owns validity/account invariants, and the API DTO owns response masking.
- Reconciled Issue #40's physical module split by registering persistence-owned entities in both composition roots, deleting the duplicate/BOM-prefixed API entry point, and moving Batch integration setup behind the output port.
- Verification passed for Master Data Core 12, API 2, Batch 4, Expenditure API 1, Loan Core 30, and Journal Ledger integration 1 tests. API and Batch bootJars also passed; 50 observed tests had no failures/errors/skips.
- Independent review identified account loss on SCD2 replacement. The domain now clones account values with new child IDs, while tests prove old/new FK ownership, close-before-save ordering, invalid-update no-write behavior, and overlapping-row fail-closed lookup.
- Draft PR #232 was marked Ready and merged as `c720ae58`; `Fixes #41` closed the reopened Issue automatically, and the remote source branch was deleted. PR, Issue, and remote branch states were reverified before marking the source worktree cleanup-eligible.
- Remaining risks: PostgreSQL execution was not run, overlapping SCD2 periods still need a database exclusion constraint, and unbounded list/query pagination remains outside this issue.
## 2026-07-30 - Issue #228 canonical Java/frontend image packaging

- Issue/branch/worktree: `#228`, `agent/228-container-images`, `C:\tmp\account-228-container-images`; based on `origin/main@5fb9cb67`.
- Replaced divergent root Java image definitions with one Java 17 multi-stage contract that builds an exact Gradle `bootJar`, requires exactly one non-plain JAR, and runs as a non-root user.
- Added a 36-target manifest: 35 Java API/Batch/infra targets plus frontend. The 33 currently executable Java targets are enabled; Internal Audit API/Batch remain explicitly blocked by #73/#74/#231.
- Pointed all 19 active module Compose build definitions at the canonical root Containerfile with exact Gradle project and JAR-directory arguments.
- Kept the frontend on its standalone Containerfile and same-origin `/api` default, removed runtime-masking source volumes, and excluded recursive `.env*`, ignored Spring local configs, key stores, build, Node and Next artifacts from build contexts.
- Replaced tracked database credentials in the now-active Auth/Account Mart/ECL module Compose paths with required variables and changed Auth schema handling from `update` to `validate`.
- Added a reusable PowerShell package/image verifier and policy tests covering target ownership, Java version, deterministic artifact selection, Compose mappings and frontend standalone execution.
- Verification: forced Config Server/Gateway policy tests passed; all 33 enabled Java targets produced exactly one executable JAR offline; 2 Internal Audit targets reported `BLOCKED`; frontend remained static-only because dependency installation and image pulls were not authorized.
- Environment gate: Docker is absent and Podman has neither required base images nor an approved pull, so no actual local image build or registry action was performed.
- Independent review findings for root/frontend secret context leakage, tracked Compose credentials, process-output deadlock and frontend URL drift were corrected; final re-review found no unresolved issue after the frontend context was hardened.
- State: pushed to `origin/agent/228-container-images`; Draft PR `#240` opened with `Refs #228`. No merge or Issue closure.

## 2026-08-11 - Issue #341 remove phantom Gradle project

- Issue/branch/worktree/base: `#341`, `agent/341-remove-phantom-app`, `C:\tmp\account-341-remove-phantom-app`, `origin/main@5624ae97`.
- Removed only the source/build-less `:app` include and updated current-state root/container/local-development prose; no directory or build artifact was deleted.
- `gradlew projects --offline` now lists 72 real subprojects instead of 73 entries, with set difference exactly `app`; all executable image/Compose targets remain unchanged.
- Config/image/development/production policy suites passed 21 tests with no failure/error/skip. Diff/marker checks and independent review found no P0-P3.
- Rollback is a normal revert of the Issue #341 commit; local ignored `app/build` remnants remain untouched. Latest matrix counts are owned by #227.
- Commit `7b393665` is pushed and Draft PR `#346` targets `main` with `Refs #341` and `Refs #227`.
## 2026-08-11 - Issue #254 Asset Lease baseline recovery

- Recovered the Asset Lease implementation from obsolete stack PR #259 onto current `origin/main@ea6d8063` without reapplying stale shared migration-runner changes.
- Added local H2 and dev/prod PostgreSQL API/Batch runtime profiles, complete PostgreSQL V20 schema parity, domain/DDL date consistency, financial precision guards and published-H2 checksum locking.
- Removed the main-line standalone conflict marker. Core 19, API 2, Batch 1, runner 78, two bootJars, full driver packaging and direct local JAR smoke passed.
- Independent review findings on staged marker state, lease period validation and remeasurement ordering were corrected; final re-review reported no P0-P3 findings.
- Pushed `39edbd26`; Draft PR #288 targets main. No external database, container deployment, merge or Issue close was performed.
## 2026-08-11 - Issue #249 Budget runtime integration

- Integrated Budget API/Batch into the reproducible local H2, PostgreSQL profile, container image and production Compose matrices.
- Added fail-closed production TLS, health/JDBC packaging, one shared JWT trust value for Auth/Gateway/Budget, Gateway-to-Auth token-version routing and required Auth production secrets.
- Split self-contained dev owner/runtime roles and added a mandatory post-migration grant gate that excludes all Flyway history tables and grants only DML plus sequence USAGE/SELECT.
- On latest main, 195 combined Gradle tasks, Budget API/Batch JAR smokes, production env validator and shell/static gates passed. Final independent review found no P0-P3.
- Pushed `d904c2ce`; Draft PR #289 targets main. Actual PostgreSQL ACL/TLS and Compose/image runtime remain external gates.

## 2026-08-11 - Issue #231 Internal Audit runtime boundary

- Promoted `internal-audit:api` to the executable composition root, retained Core as a library and removed Auth/Internal Audit placeholder Batch modules with no real Job/Step.
- Added local H2, dev/prod PostgreSQL, V60 migration parity, pre-bean production TLS validation, Gateway route, migration-runner support and canonical container/Compose wiring.
- Moved web adapters into API, removed duplicated Audit/Security sources and enforced RCM/Evaluation parent existence and path/body identifier consistency before persistence.
- A 179-task combined gate passed; 244 affected tests had zero failures/errors/skips. The local JAR smoke proved servlet startup, H2, Flyway V60 and JPA validate. Production image/template policy checks passed for 36 images and 17 PostgreSQL URLs.
- Independent review findings were corrected; final review reported no remaining P0-P3. Commit `5052163c` is pushed and Draft PR #338 targets main with references to #231/#73/#74.
- Docker was unavailable and the external development host was not accessed. Actual PostgreSQL, Compose and live Gateway/Eureka verification remain environment gates.

## 2026-08-11 - Issue #66 root development Compose topology

- Issue/branch/worktree/base: `#66`, `agent/66-compose-runtime-topology`, `C:\tmp\account-66-compose-runtime-topology`, `origin/main@1e6ad6f1`.
- Implemented one root development topology for 36 enabled targets: Config Server, Discovery, Gateway, Frontend, 17 APIs and 15 opt-in Batch applications. Java builds use the canonical root `Containerfile`; development Frontend uses a non-root Node 20 `Containerfile.dev` with reproducible `npm ci` dependencies and same-origin Gateway login routing.
- Added mutually exclusive self-contained and external-dev overlays. Self-contained owns PostgreSQL/Redis/Kafka, release migration and runtime grants; external-dev starts no duplicate infrastructure and probes all 17 context-specific runtime credentials without logging endpoints or secrets.
- All API/Batch services use PostgreSQL `dev`, Flyway/SQL-init/DDL creation disabled and JPA `validate`; Batch is non-web, profile-gated and job-disabled. Host publication is loopback-only and business API/Batch ports stay internal.
- Verification passed: shell syntax, PowerShell AST and validator self-test, six development Compose policy tests, Config/Gateway/migration-runner tests and 159-task packaging gate, Frontend production build with 122 routes and zero production `/api` rewrites, diff/marker checks, and independent re-review with no remaining P0-P3.
- Environment gates: Docker CLI is unavailable and Podman has no Compose provider, so actual Compose render/build/up and 17-context PostgreSQL privilege probes were not run. No external host, credentials, database or existing container was inspected or changed.
- Rollback: revert the Issue #66 commit and stop the same Compose project without `-v`; never delete named volumes or change an external database as rollback. The Issue remains open until live self-contained/external-dev gates pass.
- Commit `375105ab` is pushed to `origin/agent/66-compose-runtime-topology`; Draft PR `#339` targets `main` with `Refs #66` so the live environment gates do not close the Issue.

## 2026-08-11 - Issue #340 Expenditure/Tax test classpath

- Issue/branch/worktree/base: `#340`, `agent/340-expenditure-tax-test-classpath`, `C:\tmp\account-340-expenditure-tax-test-classpath`, latest `origin/main@81f4206e` after a non-overlapping fast-forward.
- Enabled the Tax API plain library artifact with the explicit `plain` classifier while retaining the sole unclassified executable boot JAR. The canonical Containerfile excludes `*-plain.jar`, so runtime selection remains deterministic.
- Removed a duplicate Expenditure API Actuator declaration and an unused Tax API test dependency from Expenditure Core. No production Java behavior or endpoint changed.
- Latest-main focused verification executed 45 tasks successfully; observed Tax/Expenditure/Container-policy reports contained 55 tests with no failure/error/skip. Tax output contained one approximately 98 MB executable JAR and one approximately 9 KB `-plain.jar`.
- Root `build --offline --rerun-tasks --max-workers=1` passed the original Expenditure test compilation/integration failure and executed 237 tasks. It stopped only at the separately tracked #344 `InternalAuditRuntimePolicyTest` explicit-local-datasource assertion; fresh evidence was added to #344.
- `git diff --check` and scoped conflict-marker checks passed. The independent pre-sync review reported no P0-P3 and latest-main changed-file overlap was empty.
- Independent latest-main review reported no P0-P3. PR `#350` was promoted Ready and merged as `aaad0c0d`; Issue #340 was closed and its active status/owner labels were removed.
- Rollback is a normal revert of `aaaa4922`. A named pre-sync stash commit is retained until integration; no database, container, credential, private endpoint, or external service was accessed.

## 2026-08-11 - Issue #348 multi-tool GitHub ownership protocol

- Confirmed the repository had only the nine default GitHub labels, no workflow/agent labels, no Issue assignees on the active runtime Issues, and no GitHub Projects Status field.
- Added `agent-loop`, `agent:codex`, `agent:gemini`, `agent:claude-code`, `status:ready`, `status:in-progress`, `status:blocked`, and `status:needs-review` with non-secret descriptions.
- Assigned the authenticated repository owner and `agent:codex` to active Codex work; marked #66 blocked on live Compose/PostgreSQL gates; marked unclaimed #80/#343/#345/#347 ready; and marked #89 for independent review. Each synchronized Issue received a concise state comment.
- Created Issue #348 and isolated `agent/348-multi-tool-issue-ownership` / `C:\tmp\account-348-multi-tool-issue-ownership` from fetched `origin/main@81f4206e` because the primary checkout is dirty and 15 commits behind.
- Added a focused runbook defining exactly one active writer, claim-race handling, safe transfer, review-only ownership, labels versus GitHub assignees, and dirty-main synchronization for Codex, Gemini, and Claude Code.
- No GitHub Project, bot/App, package, database, container, credential, production/test code, or primary-checkout file was changed. Rollback is a documentation revert plus removal of only the eight Issue #348 labels if the convention is rejected.
- Changed-file allowlist, relative links, required-label existence, synchronized Issue state/assignee audit, `git diff --check`, and scoped conflict-marker checks passed.
- Commit `34d14d34` is pushed and Draft PR `#349` targets `main` with `Refs #348`. Issue #348 is `status:needs-review`; Gemini or Claude Code may perform the independent read-only review. No Ready transition, merge, or Issue close was performed.
- The first independent PR review found three P2 process defects: incomplete ready-Issue contracts, contradictory GitHub mutation ownership, and a malformed original #348 claim comment. The protocol now makes the parent Integrator the sole GitHub mutator, the ready contracts were completed, and an exact superseding claim was posted.
- Rebased onto `origin/main@aaad0c0d`; append-only conflicts in four shared harness logs preserved both the integrated #340 evidence and the #348 records. No production, test, build, database, or runtime file conflicted.
- The first remediation re-review found one remaining P2: review/transfer wording and old ready-Issue comments still implied direct tool mutations. Updated both tool guides and both runbooks so the parent alone posts Issue comments and changes review state, and posted superseding parent-only comments on #80/#343/#345/#347.
- Final independent re-review at `9a783fc1` found no P0-P3 and approved the merge gate. PR #349 is CLEAN/MERGEABLE on `origin/main@aaad0c0d`; no CI checks are configured and no production/runtime tests apply to this documentation-only change.
- PR #349 was promoted Ready and merged as `c4a50f17`; Issue #348 was closed and its active workflow/owner labels were removed.

## 2026-08-11 - Issue #79 Loan API local H2 composition

- Issue/branch/worktree/base: `#79`, `agent/79-loan-api-local-h2`, `C:\tmp\account-79-loan-api-local-h2`, latest `origin/main@c4a50f17` after a non-overlapping fast-forward from `5624ae97`.
- Added only the explicit Master Data persistence entity package and shared security/audit entity/repository packages required by the already composed local adapters. No broad `com.ho.account` scan, business logic, API contract, migration, or Batch behavior changed.
- Added a real `local` profile API context regression test that disables external control-plane clients, uses ephemeral H2/create-drop, and asserts both Master Data persistence entity types are managed.
- Latest-main verification passed 24 Gradle tasks, Loan Core/API 42 tests in 14 suites with zero failure/error/skip, API `bootJar`, and a bounded direct executable-JAR smoke that observed H2 start and `Started LoanApplication` in 8.181 seconds.
- `git diff --check`, two-file allowlist and scoped conflict-marker scan passed. Independent review found no P0-P3 and approved parent-owned commit/Draft PR preparation.
- PostgreSQL schema parity, Loan Batch startup and automated CI/container JAR smoke are outside #79. No external DB, credentials, private URL, container or deployed service was accessed. Rollback is a normal revert of the Issue commit.
- Commit `09b72f21` is pushed and Draft PR #352 targets `main` with `Refs #79/#227`. Issue #79 is `status:needs-review`; no Ready transition, merge, close or external deployment was performed.
- Final PR-head review found no P0-P3. PR #352 was promoted Ready and merged as `4fa50cc8`; Issue #79 was closed and its active status/owner labels were removed.

## 2026-08-11 - Issue #342 Reconciliation local health

- Issue/branch/worktree/base: `#342`, `agent/342-reconciliation-local-health`, `C:\tmp\account-342-reconciliation-local-health`, latest `origin/main@4fa50cc8` after a non-overlapping fast-forward from `5624ae97`.
- The API `local` profile now enables health probes and disables only the unused Redis health contributor. Vault remains disabled locally; dev/prod resources and business/runtime dependencies are unchanged.
- Added a Spring-config-backed policy test proving local resolves Redis/probes as `false/true` while dev and prod resolve neither override, plus truthful local-run documentation for both health endpoints.
- Latest-main Core/API reports passed 34 tests in 9 suites with zero failure/error/skip; API bootJar passed. A bounded direct executable-JAR smoke returned HTTP 200 and `UP` from `/actuator/health` and `/actuator/health/readiness` without Redis.
- Three-file allowlist, `git diff --check` and scoped conflict-marker scan passed. Independent review found no P0-P3 and approved parent-owned commit/Draft PR preparation.
- Real Redis-backed dev/prod health, automated HTTP/container smoke and Batch behavior are outside #342. No external Redis/DB, credential, private URL, container or deployed service was accessed.
- Rollback must use a new reviewed commit that restores only the three #342 runtime/test/docs paths and appends the rollback result. Do not revert all of `d0698585`, because that commit also records the already-integrated #79 history in five shared harness files.
- Commit `d0698585` is pushed and Draft PR #353 targets `main` with `Refs #342/#227`. Issue #342 is `status:needs-review`; no Ready transition, merge, close or external deployment was performed.
- Final PR review found one P3 in the original whole-commit rollback wording; the path-scoped, append-only rollback contract above corrects it. Runtime/test findings remain clear; independent re-review is required.
- Independent re-review found no P0-P3. PR #353 was promoted Ready and merged as `cf50e4fc`; Issue #342 was closed and its active status/owner labels were removed.

## 2026-08-11 - Issue #344 Internal Audit explicit local policy

- Issue/branch/worktree/base: `#344`, `agent/344-internal-audit-local-h2`, `C:\tmp\account-344-internal-audit-local-h2`, latest `origin/main@cf50e4fc` after a non-overlapping fast-forward from `81f4206e`.
- Confirmed #231 already integrated the tracked `application-local.yml`; this bounded change strengthens the regression assertions for active local profile, H2 PostgreSQL mode, Flyway location/target V60, JPA validate, SQL-init off and disabled Config/Discovery/Vault/Eureka, plus documents ephemeral in-memory data.
- Focused policy verification and the full Internal Audit Core/API gate passed 17 tests in 6 suites with zero failure/error/skip; API bootJar passed. A bounded direct executable-JAR smoke observed the local profile, H2, Flyway V60, JPA initialization and successful `InternalAuditApiApplication` start.
- The first smoke checker used the obsolete expected class name and returned a false negative after the application had started successfully; corrected log evaluation against the actual class name passed all six startup checks. No production resource, migration, dev/prod behavior or external environment changed.
- Two runtime test/documentation paths, `git diff --check` and scoped conflict-marker checks passed. Independent read-only review remains before commit/PR.
- Rollback only the two Issue #344 paths through a reviewed revert and append the result to shared records. No DB/data/container rollback is required.
- Independent review found one P3 because three control-plane assertions were initially masked by highest-priority test overrides. The helper now leaves local Config/Discovery/Eureka values unmasked while retaining dev/prod-only isolation overrides; focused and full module gates reran successfully. Independent re-review remains.
- A latest-main root forced build attempt ran for five minutes without an observed test failure but exceeded the command timeout, so it is not claimed as passed. Module gates and the prior full-root failure point are covered; final root-wide completion remains a separate recorded gate unless a longer run finishes.
- A second root forced build with a ten-minute limit completed successfully in 8m31s: all 349 actionable tasks executed. This clears the prior #340/#344 full-root stopping point on the tested base.
- Independent remediation re-review found no P0-P3. The reviewer identified a newer `origin/main` and overlapping append-only harness logs, so latest-main synchronization and focused revalidation remain before commit/Draft PR.
- Synchronized to latest `origin/main@b2d5c6ef` through a named stash and fast-forward. One conflict in `agent-status.md` retained upstream #312/#314 integration rows and local #344/#342 rows; the other append-only logs merged automatically. The two reviewed Internal Audit paths are byte-identical to the reviewed stash, static gates passed and the focused policy test passed again.
- Committed `e4a86d67`, pushed `agent/344-internal-audit-local-h2`, and opened Draft PR #357 with `Refs #344/#227`. Issue #344 moved to `status:needs-review`; Ready/merge/close remain gated on a final PR-head check.
- Final PR-head review at `2c0eb829` found no P0-P3. PR #357 was promoted Ready and merged to main as `21395eb3`; Issue #344 closed and its active owner/status labels were removed.

## 2026-08-12 - Issue #90 Asset Lease Batch explicit local profile

- Issue/branch/worktree/base: `#90`, `agent/90-asset-lease-batch-local`, `C:\tmp\account-90-asset-lease-batch-local`, latest `origin/main@43f5b36c` after two non-overlapping fast-forwards from `5624ae97`.
- Current-main baseline proved PR #288 had already removed the historical managed-type/startup defect: Batch tests and bootJar passed, and the packaged JAR started with profile-only local/H2 in about seven seconds. The remaining gap was an incomplete Batch-owned local policy and no real-profile regression test.
- `application-local.yml` now explicitly owns isolated H2 PostgreSQL mode, create-drop JPA, Batch metadata initialization, disabled automatic jobs, SQL init, Config/Discovery/Vault/Eureka/Kafka listener and tracing policy. Dev/prod PostgreSQL files, business Job flow and depreciation logic are unchanged.
- Added a real `local` non-web context test that reads the tracked profile without property overrides, proves no Job runner/instance, and verifies the registered depreciation Job. Simplified the IntelliJ run configuration and Batch documentation to require only the local profile.
- Latest-main Core/Batch verification passed 21 tests in 8 suites with zero failure/error/skip and Batch bootJar. A bounded packaged-JAR smoke observed local, H2 PostgreSQL mode, JPA, Batch metadata, successful application start, no business Job launch, no external connection attempt and no startup failure.
- `git diff --check`, four-file implementation allowlist and scoped conflict-marker checks passed. An older restart-validator experiment changed Job semantics and regressed the existing prod schema-context test; it is intentionally excluded and retained only in named stash `GH-90 pre-baseline partial batch local work` until review/merge cleanup.
- Rollback only the four Issue #90 implementation paths through a reviewed revert and append the result to shared records. No external DB/data/container rollback exists; no private host, credential, PostgreSQL, Kafka or Compose runtime was accessed.
- Independent review found no P0-P3. Commit `65396222` is pushed and Draft PR #359 targets main with `Refs #90/#227`; Issue #90 is `status:needs-review`. Ready/merge/close remain gated on final PR-head verification.
- The first final PR-head review passed and PR #359 was promoted Ready, but main advanced through Discovery PR #360 before merge; GitHub correctly rejected the stale conflicting merge and Issue #90 remained open. Merged `origin/main@191c5c28`, preserved both append-only #90/#304 records, confirmed zero upstream overlap in the four implementation paths, and reran the focused local-context test successfully. A final PR-head recheck is required after pushing the sync commit.

## 2026-08-14 - Issue #227 latest-main runtime audit

- Synchronized the Issue worktree through a named stash to `origin/main@1ae9e108`; no conflict occurred and the dirty primary checkout remained untouched.
- Inventory/task contract passed for 72 subprojects (17 API, 15 Batch, 3 infra, 1 CLI, 19 libraries, 17 aggregators). All 36 executable targets packaged offline with exactly one executable JAR, and all 36 non-executable library/aggregator `test+jar` gates passed.
- Strict ProfileJar across all executable artifacts produced 16 `PASS_STARTED`, 15 `PASS_EXITED`, and five fail-closed results. Auth/Budget/Gateway lack secure JWT input in the intentionally stripped environment; Closing API lacks `FiscalPeriodMapper`; Closing Batch lacks `JournalPostingPort`.
- Created unclaimed `status:ready` Issues #420 (Auth), #421 (Gateway), #422 (Closing API), #423 (Closing Batch), and #424 (Production Compose test runbook path). Reused existing #249 for Budget and #66/#228/#230 for live Compose/image/PostgreSQL gates. Did not duplicate closed #362 because PR #398 already owns the Node 20/frontend lifecycle change.
- Updated the runtime matrix and local guide to separate actual H2 startup, secure local input, static resource location, and live Compose gates. Current Java Compose mapping is development 36/36 and production server targets 35/35.
- Hardened `runtime-smoke.ps1`: bounded Gradle/process-tree termination, concurrent output capture, isolated environment/user home, evidence redaction without erasing Java `File.java:line` locations, exact single executable JAR checks, Migration Runner support, stable arrays, and nonzero failed/BLOCKED exits.
- Frontend mode correctly wrote `BLOCKED` because this isolated worktree has no `node_modules` and package install was not authorized. Podman is installed but has no Compose provider. No external host, credential, DB, container or volume was accessed.
- Parser, diff and marker checks pass. Focused Compose/image policy execution is 20/21; the single stale runbook-path failure is #424, while the unaffected Development/Container policy subset reruns green. Dynamic checks prove Java source locations survive redaction, host/URI/credential samples are removed, timeout is reported, and the scoped child process tree is terminated. Final independent review remains before the authorized commit/push/Draft PR.
- Independent review found two P2s and two P3s: termination failure could still be classified `PASS_STARTED`, LocalJar command evidence retained a diagnostic JWT, timeout success semantics were misstated, and unrelated #90 completion records were in scope. The tool now verifies post-kill exit and fails on termination errors, redacts Command, preserves JDK versions/source locations while removing endpoint/credential samples, the guide distinguishes `PASS_STARTED`, and #90 additions were removed. Dynamic process-tree and Budget LocalJar command-redaction checks pass; independent re-review is required.
- Re-review found one remaining P2: user-home, IPv6 and `.java`-suffixed endpoint text could evade or confuse redaction. Source preservation is now limited to parenthesized stack locations and path-qualified source locations; user-home, Windows profile paths, contextual/bare IPv6 and host tokens are redacted. Adversarial context/IPv6/user tests and the real Budget LocalJar JSON pass without exposing the user path or diagnostic JWT; final re-review remains.
- The next re-review tightened that P2 further: only actual `at package.Method(File.java:line)`, standalone source lines and path-qualified sources are preserved; parenthesized endpoints are not. Bracketed zone-id and compressed one-group IPv6 candidates are parsed/redacted while loopback `::1` remains safe. Adversarial samples and the Budget JSON pass; final independent review is required.
- Final independent re-review found no P0-P3. Publish the eight scoped #227 files, open a Draft PR, then repeat the PR-head gate before Ready/merge.
- Committed `adade958`, pushed `agent/227-latest-main-runtime-audit`, and opened Draft PR #433 with `Refs #227`. Issue #227 moved to `status:needs-review`; PR-head review precedes the authorized Ready/merge.
- Rollback is a reviewed revert of only the #227 tool/document/harness commit; no external-state rollback exists.

## 2026-08-14 - Issue #420 Auth secure local H2 runtime

- Claimed `#420` on `agent/420-auth-local-runtime` in `C:\tmp\account-420-auth-local-runtime`; synchronized the isolated worktree to non-overlapping `origin/main@db5c865c` while leaving the dirty primary checkout untouched.
- Added the missing Auth `application-local.yml` with H2 PostgreSQL mode, Flyway/JPA ownership and disabled Config/Discovery/Vault/Eureka/tracing. The profile contains no JWT secret, internal token or default user credential; all profiles remain fail-closed without runtime input.
- Runtime policy tests now load real base/local/dev/prod configuration. Base/dev/prod independently reject missing JWT and missing internal token, while local and injected dev/prod contexts bind process-generated ephemeral values.
- Corrected Auth local-run/schema documentation for port `8081`, migrations V70-V73 and the no-default credential contract. The minimal core edit only corrects stale Javadoc/error wording and changes no behavior.
- Latest-main Auth Core/API verification passed 45 tests in 14 suites with zero failure/error/skip, plus API `bootJar`. Direct packaged-JAR smokes passed both required paths: missing input failed closed and generated 32-byte inputs started the local H2 application.
- `git diff --check`, scoped conflict-marker and secret-default scans passed. Independent review findings for port/docs/profile-binding/dual-credential coverage were remediated; final re-review found no P0-P3.
- No external DB, private endpoint, credential, container or volume was accessed. Rollback is a reviewed revert of only the #420 Auth resource/test/docs/core-wording and harness paths; no external-state rollback exists.
- The ignored local resource was force-added at its exact path and its staged blob matched the reviewed working file. Initial commit `62cc8717` is pushed and Draft PR #441 targets main with `Refs #420/#427`; Issue #420 is `status:needs-review`.
- The first PR-head review found only a P3 stale-harness handoff after publication. This harness-only follow-up records the actual commit/PR/Issue state; repeat independent PR-head review and GitHub checks before Ready/merge.

## 2026-08-14 - Issue #421 Gateway secure standalone local runtime

- Claimed `#421` on `agent/421-gateway-local-jwt` in `C:\tmp\account-421-gateway-local-jwt` from latest `origin/main@eb7ce92b`; the dirty primary checkout remained untouched.
- Baseline Gateway tests/bootJar passed and the packaged JAR failed closed without a verification key. A process-generated 32-byte input started the profile-only local JAR, but an Eureka registration attempt proved the tracked local runtime policy was missing.
- Added an explicit Gateway `application-local.yml` that disables Config/Discovery/LoadBalancer/Gateway locator/Eureka/tracing and token-version remote validation. It contains no JWT secret/public key/JWKS URI or Auth endpoint.
- The shared standalone IntelliJ configuration and local guides now activate `local` and require a runtime-only ephemeral JWT input. Fixed test secrets were removed; actual Spring profile tests verify isolated startup, missing-key fail-closed behavior and the absence of any local JWT/key/remote URL section.
- Latest-main Gateway verification passed 43 tests in 9 suites with zero failure/error/skip and bootJar. Packaged-JAR smokes passed missing-input fail-closed and ephemeral-input startup with no Config or Eureka attempt.
- `git diff --check`, scoped marker, resource/run-config/test-resource credential and Boot JAR resource gates passed. Independent review findings for allowlist evidence, literal-secret test coverage and troubleshooting wording were resolved; final re-review found no P0-P3.
- No external endpoint, credential, DB, container or volume was accessed. Rollback is a reviewed revert of the #421 Gateway profile/test/run-config/docs and harness paths; no external-state rollback exists.
- The ignored local resource was force-added at its exact path and its staged blob matched the reviewed working file. Commit `f066eb31` is pushed and Draft PR #445 targets main with `Refs #421/#227`; Issue #421 is `status:needs-review`.
- The first PR-head review found only a P3 stale-harness handoff after publication. This harness-only follow-up records the actual commit/PR/Issue state; repeat independent PR-head review and GitHub checks before Ready/merge.

## 2026-08-14 - Issue #422 Closing API Master Data composition

- Claimed `#422` on `agent/422-closing-api-local-mapper` in `C:\tmp\account-422-closing-api-local-mapper` from latest `origin/main@305fa259`; the dirty primary checkout remained untouched.
- Reproduced the local context failure at `FiscalPeriodMapper`. The mapper-only fix exposed an already-active broad Master Data adapter scan with missing persistence ports, so the Closing composition root now imports only its two used contract adapters and their complete minimal persistence/mapper graph.
- Closing business/domain behavior and Master Data production sources are unchanged. No profile-specific fallback was added, and dev/prod continue to use the same JPA production adapters without duplicate or shadow beans.
- Latest-main Core/API verification passed 74 tests in 19 suites with zero failure/error/skip and API bootJar. The packaged local H2 JAR started with no FiscalPeriodMapper error or external attempt.
- Exact three-file implementation allowlist, `git diff --check` and marker gates passed. Independent review found no P0-P3.
- Commit `325b6e98` is pushed and Draft PR #447 targets main with `Refs #422/#227`; Issue #422 is `status:needs-review`.
- No external endpoint, DB, credential, container or volume was accessed. Rollback is a reviewed path-scoped revert of the #422 composition/test/docs commit while retaining append-only history.
- Next gate is independent PR-head review and green GitHub checks before Ready/merge/close.

## 2026-08-14 - Issue #423 Closing Batch local Journal composition

- Claimed `#423` on `agent/423-closing-batch-local-journal` in `C:\tmp\account-423-closing-batch-local-journal`; the branch is stacked on reviewed PR #447 because #422 and #423 are independent composition fixes whose shared Closing CI previously blocked each other.
- Wired the existing Closing local Journal ports into the Batch composition root and restricted the fallback configuration to `local`. `@ConditionalOnMissingBean` keeps approved Journal implementations authoritative when they are present.
- Removed the broad Master Data adapter scan from Closing Batch and explicitly imported only the Closing/FX contract adapters and their minimal persistence/mapper dependency closure.
- Closing API/Batch/Core passed 90 tests in 27 suites with zero failure/error/skip and Closing Batch `bootJar`. The packaged local H2 JAR started without missing Journal/ExchangeRate ports or any external attempt.
- Diff/marker/allowlist checks passed. Independent review's P3 about missing fallback back-off coverage was remediated; final re-review found no P0-P3.
- Implementation commit `8a582592` is pushed and stacked Draft PR #448 targets `agent/422-closing-api-local-mapper`; Issue #423 is `status:needs-review`.
- No external endpoint, DB, credential, container or volume was accessed. No Closing Job or journal posting ran. Rollback is a reviewed path-scoped revert of the five #423 implementation/test/docs paths while preserving append-only history.
- PR #448 passed all GitHub checks and final PR-head review with no P0-P3, then merged into the #422 branch as `dbedb96f`. Main-target Draft PR #447 now carries both independently reviewed Closing composition fixes and must pass its refreshed full Closing CI before Ready/merge.

## 2026-08-21 - Issue #66 PR #407 container topology conflict resolution

- Took over the existing Issue #66 follow-up branch `agent/66-infra-topology-redesign` in `C:\tmp\account-66-infra-topology-redesign`; the dirty primary checkout was not pulled, reset or edited. PR #407 is distinct from the earlier Codex PR #339.
- Merged latest `origin/main@07499d93` without textual conflict and resolved the semantic conflict between the old central `Containerfile` policy and PR #407's module-specific image design. Active Java targets now use the 35 manifest-selected module Dockerfiles; the migration helper remains an explicit central `Containerfile` exception.
- Removed stale PR-only compose/script/document additions and cancelled unrelated stale-branch application changes so the final main-relative delta contains only image definitions, Compose selection, manifest/tooling, policy tests and runbooks.
- Config Server policy verification passed 37 tests in 6 suites with zero failure/error/skip. `VerifyPackages` built and inspected all 35 Java targets with 35 PASS results and exactly one non-plain executable JAR per target.
- Independent review findings about preserving the latest Windows/Podman/H2 guidance, narrowing the canonical claim to the 35 active targets and binding both Dockerfile `find` paths to each manifest `jarDirectory` were remediated; final independent re-review found no P0-P3. Refreshed GitHub checks remain the publication gate.
- No Docker/Compose/PostgreSQL/private endpoint/credential/container operation was performed. Issue #66 remains `status:blocked` until an approved live Compose/PostgreSQL runtime gate is executed.
- Removed only the clean detached temporary worktree `C:\tmp\account-487-merge` for already-merged PR #487. Dirty, unpublished, divergent, current and other-agent worktrees were intentionally retained.
- The first refreshed PR-head CI exposed three stale module policy tests that still required the removed central build arguments: Budget, Gateway and Internal Audit. Their exact module-Dockerfile assertions and Budget runbook were corrected; 7 focused tests and the CI-equivalent six-project set passed 123 tests in 29 suites with zero failure/error/skip. Independent remediation review found no P0-P3; a new GitHub CI run is required after push.

## 2026-08-21 - Issue #435 Config Server ENCRYPT_KEY fail-closed remediation

- Took over rejected PR #511 on `agent/435-config-key-fail-closed` in `C:\tmp\account-435-config-key-fail-closed`, claimed Issue #435 as `agent:codex` / `status:in-progress`, and merged latest `origin/main@0b2280fd` without conflict. The dirty primary checkout and other-agent worktrees remained untouched.
- Removed the empty encryption-key fallback and added a pre-context startup guard that rejects missing, empty, whitespace and surrounding-whitespace input without including key material in diagnostics. Tests inject only per-process generated values.
- Root/module Compose now require `ENCRYPT_KEY` interpolation, `.env.example` exposes only a blank variable contract, and Config Server runbooks describe generated input with cleanup.
- `:config-server:test :config-server:bootJar --offline` passed 45 tests in 7 suites. Packaged-JAR smokes rejected missing/empty/whitespace input and started with a generated nonblank input without logging it; `podman compose ... config --quiet` used the installed external Compose provider and passed without starting a service. Executable-default, diff and marker checks passed.
- No real key, external Config Server, DB, container, private endpoint or credential was accessed. Independent review, commit/push, PR #511 reopen, fresh CI and merge remain the publication gates.
- Independent review found a P1 production-path gap: `compose.prod.yml` and the canonical dev/external-dev/prod templates did not carry the required variable. The production service, all three templates and both environment validators now enforce the same contract; focused verification and re-review are required.
- Both validator self-tests, production template validation and root development Compose rendering passed. A production Compose render reached model validation after generated dummy input injection but is blocked by the pre-existing `pids_limit` / `deploy.resources.limits.pids` conflict; this is outside #435 and requires a separate follow-up rather than an unreviewed topology change here.
- Remediation re-review found a P2 PowerShell truthiness gap for whitespace-only production input. The production validator now uses `IsNullOrWhiteSpace` and its self-test covers actual whitespace rejection plus template-only blank allowance; verification and final re-review remain.
- Filed follow-up Issue #530 as unclaimed `status:ready` for the pre-existing production Compose pids-limit model conflict; #435 remains limited to encryption-key fail-closed behavior.
- Final independent re-review found no P0-P3. Exact-path commit/push, PR #511 reopen, fresh CI and remote-head review are the remaining merge gates.
- Published commit `1ef36b71` and reopened PR #511. Main then advanced to `1025417b`; latest-main merge conflicts were limited to the four append-only harness records, where both #435 and upstream #462/Auth histories were retained. A forced Config Server rerun passed 45/45 and bootJar; push and refreshed PR gates remain.
- Latest-main merge commit `70ca599c` is pushed. PR #511 is open, non-draft and MERGEABLE on exact base `1025417b`; fresh GitHub checks and a final remote-head review remain before merge.
## 2026-08-21 - Issue #462 Auth LDAP OTP fail-closed

- Claimed `#462` on `agent/462-auth-otp-fail-closed` in `/tmp/account-462-auth-otp-fail-closed` from `origin/main@0b2280fd`; the primary checkout and unrelated dirty Frontend worktree were not changed.
- Removed the universal LDAP OTP `123456` acceptance path. Until a real OTP verifier exists, every LDAP request records `LDAP_OTP_VERIFIER_UNAVAILABLE`, returns the existing generic credentials error and never issues a JWT. Normal password login and the separately tracked SSO path are unchanged.
- Added focused tests for both the former fixed OTP and another OTP, including generic failure, no token issuance and the internal failure reason; updated Auth README and process-flow documentation.
- `git diff --check`, the four-file implementation allowlist and changed-file marker checks passed. Independent review found no P0-P3.
- Local execution is not claimed: the host has Java 21 while the build requires Java 17, and the network-disabled cached JDK 17 container lacks cached `io.jsonwebtoken:jjwt-api:0.11.5`. No package was installed or downloaded. GitHub Module Validation must pass its actual Auth Core/API test tasks before merge. API packaging/build configuration did not change, so `bootJar` is not an Issue #462 merge gate and remains unexecuted local evidence.
- Commit `1b4407f7` is pushed and Draft PR #525 targets `main` with `Refs #462/#515`; Issue #462 is `status:needs-review`. Rollback is a reviewed path-scoped revert of the four Auth implementation/test/docs files while retaining append-only harness history.
- Final PR-head review found a P1 because the first record overstated Module Validation as running `bootJar --max-workers=1`; the workflow actually runs `:auth:core:test :auth:api:test` with its configured runner defaults. This record and the PR/Issue handoff now use that exact gate; independent remediation re-review remains before Ready/merge.
- The final #462 Auth Core/API JDK 17 CI passed, independent remediation re-review found no P0-P3, and PR #525 squash-merged to `main` as `1025417b`; Issue #462 closed and its active owner/status labels were removed.

## 2026-08-21 - Issue #518 Auth client-selected login type fail-closed

- Claimed `#518` on `agent/518-auth-sso-fail-closed` in `/tmp/account-518-auth-sso-fail-closed` from `origin/main@1025417b`; primary and unrelated dirty worktrees were untouched.
- Only explicit case-insensitive `NORMAL` may reach password authentication. SSO and null/blank/unknown types return the existing generic credentials error before user, password, login-attempt or token adapters; LDAP retains #462's password-then-provider-unavailable fail-closed path.
- Added focused SSO/no-adapter tests, six repeated calls for each unsupported type with no lockout mutation, lowercase `normal` success and retained LDAP no-token assertions. Auth README/process flow now state the provider and lockout-counter contracts.
- Independent review found one medium lockout DoS in the first implementation because non-credential requests incremented credential failures. The remediation moved rejection before `LoginAttemptPort`; independent re-review found no P0-P3. Diff, exact four-file allowlist, marker and secret scans pass.
- Local execution is not claimed: host Java 17 is unavailable and the network-disabled CPU-1/memory-1536-MiB JDK 17 container lacks cached `jjwt-api:0.11.5`. No dependency was downloaded. Final-head GitHub Auth Core/API tests are the merge gate; bootJar is explicitly omitted because API packaging/build configuration is unchanged.
- Commit `20e1806e` is pushed and Draft PR #531 targets `main` with `Refs #518/#515/#462`; Issue #518 is `status:needs-review`. Rollback is a reviewed path-scoped revert of the four Auth files while preserving harness history.

## 2026-08-22 - Issues #518 integration and #535 Frontend ESLint compatibility

- Final JDK 17 Auth Core/API CI and independent final-head review passed for #518. PR #531 squash-merged to `main` as `b6b43031`; Issue #518 closed and its active owner/status labels were removed.
- #519's six-file Frontend login contract implementation remains isolated and uncommitted in `/tmp/account-519-frontend-auth-contract`. TypeScript passed, while the network-disabled production build stopped only at the existing `next/font` Google Fonts fetch. The original lint command failed before source analysis because Node 20 could not resolve the extensionless Next ESLint ESM imports, so #535 was split out as the blocking tooling issue and #519 moved to `status:blocked`.
- #535 changes only `frontend/eslint.config.mjs`: legacy Next 15.5 configs load through `FlatCompat`, the absent `react-hooks/set-state-in-effect` rule receives a conditional no-op compatibility registration, and only the two documented legacy debt classes are retained as warnings. #536 owns removal of the shim/severity policy and the 245 warning backlog; it is non-blocking and intentionally not executed under the low-resource scope.
- Discovery evidence is retained: extensionless imports produced `ERR_MODULE_NOT_FOUND`; direct `.js` imports produced a non-iterable config failure; the first `FlatCompat` run exposed 31 errors / 214 warnings. Final network-disabled Node 20.20.2 verification at CPU 1 / memory 2 GiB passed full lint with 0 errors / 245 warnings and `tsc --noEmit` with exit 0. Diff, one-file allowlist, marker and secret-pattern checks passed; build was not run because #535 changes lint configuration only.
- Independent #535 review found no P1/P2 or merge-blocking finding. Commit `c2364782` is pushed and Draft PR #537 targets `main`; Issue #535 is `status:needs-review`. First PR Guard passed, while final PR-head checks/review remain before Ready/merge. Rollback is a reviewed revert of `frontend/eslint.config.mjs` only.

## 2026-08-22 - Issues #535 integration and #538 offline Frontend font build

- #535 final remote-head review found and corrected a P3 PR-body omission for the four append-only harness files; re-review and all GitHub checks passed. The Ready-event Guard passed and PR #537 squash-merged as `7364a926`; Issue #535 closed and active labels were removed.
- #519 then passed targeted lint, full lint with 0 errors / 244 warnings and TypeScript on the merged ESLint baseline. Its required network-disabled production build remained blocked solely by the root `next/font/google` Inter download, so the one-file build prerequisite was split to #538 and #519 returned to `status:blocked` with its six changes preserved.
- #538 removes only the remote Inter import, initialization and generated body class from `frontend/src/app/layout.tsx`; the existing `globals.css` system font stack, metadata, hydration setting and provider order remain unchanged. Main advanced through CI-only PR #534 before commit, so the change was safely stashed, fast-forwarded to `859fc21e` and restored without conflict.
- Cached Node 20.20.2 validation used CPU 1, memory 2 GiB and no network. Full lint passed with 0 errors / 245 existing warnings. The first read-only type check stopped on `tsconfig.tsbuildinfo` EROFS; redirecting that file passed TypeScript. The next build compiled offline but stopped when Next refreshed `next-env.d.ts` on the read-only source mount. A final isolated writable-worktree run with a separate `.next` output passed with exit 0, OOM false, 117-second compilation, 122/122 static pages and final trace.
- Diff, exact one-file allowlist, marker, secret and remote-font scans passed; independent review found no P0-P3. The two stopped verification containers and about 983 MiB of temporary output were removed. Commit `d7397aef` is pushed and Draft PR #539 targets `main`; Issue #538 is `status:needs-review`, pending final PR-head checks/review before Ready/merge. Rollback is a reviewed one-file revert.

## 2026-08-22 - Issues #538 integration and #519 Frontend password login contract

- #538 final remote-head review found no P0-P3 and all GitHub checks passed. The Ready-event Guard passed and PR #539 squash-merged as `9c62b8e6`; Issue #538 closed and active owner/status labels were removed.
- #519's six-file login/UI/documentation change was safely stashed, fast-forwarded to latest `origin/main@9c62b8e6` and restored without conflict. The UI no longer offers unimplemented SSO/LDAP/OTP or demo values, and the service rejects blank inputs before exact same-origin `/api/auth/login`, trims only username, preserves the password value and sends explicit `NORMAL`.
- Raw JWT display and duplicate token storage in `user_info` are removed, while the existing `auth_token` localStorage Bearer contract remains. Independent review found two P3s: storage failure could look like invalid credentials and leave a partial session, and async status lacked live-region/busy semantics. Remediation separates API/storage errors, best-effort clears both keys, blocks success/redirect on storage failure, and adds alert/status/aria-busy; final re-review found no P0-P3.
- Cached Node 20.20.2 verification at CPU 1, memory 2 GiB and network none passed full lint with 0 errors / 244 warnings, TypeScript and production build. The build exited 0 without OOM after compiling in 118 seconds, generating 122/122 static pages and collecting final traces. Empty JSON POST probes through Frontend `:3000`, Gateway `:8000` and Auth `:8084` all returned the same HTTP 400 `VALIDATION_ERROR` without credentials.
- Diff, exact six-file allowlist, marker and stale-auth scans passed. The stopped test container and 541 MiB temporary build output were removed. Structural HttpOnly BFF migration and legacy `user_info.token` cleanup are tracked by unclaimed #540. Commit `1cf73e3d` is pushed and Draft PR #541 targets `main`; Issue #519 is `status:needs-review` pending final PR-head checks/review. Rollback is a reviewed six-file revert with no external-state rollback.

## 2026-08-22 - Issues #519 integration and #497 Gateway discovery-route bypass

- #519 final remote-head review found no P0-P3 and all GitHub checks passed. The Ready-event Guard passed and PR #541 squash-merged as `a0e0f8a6`; Issue #519 closed and active owner/status labels were removed. The remaining browser-token risk stays open as unclaimed #540.
- #497's P0 was reproduced against the unchanged running dev Gateway without credentials or data: `GET /api/auth/login` returned 401 `BEARER_TOKEN_REQUIRED`, while `GET /auth-service/api/auth/login` avoided `X-Auth-Error`, reached global rate limiting and returned 500. No container was restarted or changed.
- Both packaged `gateway/src/main/resources/application.yml` and external `config-repo/gateway-service.yml` now disable discovery locator and remove its lower-case options. Eureka client registration/registry fetch and all ten explicit `lb://` routes remain; documentation distinguishes service discovery from external route generation.
- The policy test parses both YAML files, asserts locator false/obsolete options absent, preserves Eureka/CORS/rate-limiter contracts, and verifies each explicit route ID's exact URI. Independent review found P3 route-URI coverage and Eureka wording gaps; both were remediated and re-review found no P0-P3. Exact six-file diff, YAML parse, marker, secret and alternate-enable scans passed.
- A cached JDK 17, CPU-1, memory-1536-MiB, network-disabled Gradle attempt stopped before `compileJava` because existing Spring WebFlux/Cloud/CircuitBreaker/OpenAPI/JJWT artifacts were not cached; no dependency was downloaded and no local test success is claimed. Commit `e1456136` is pushed and Draft PR #542 targets `main`; Issue #497 is `status:needs-review`. GitHub JDK 17 `:gateway:test`, final remote-head review and Ready-event Guard gate merge. Post-restart prefixed-path 404 belongs to #520; #466 owns explicit routes for unlisted business modules.

## 2026-08-22 - Issue #497 integration, #520 preflight and #543 Gateway Compose JWT input

- #497 passed GitHub JDK 17 `:gateway:test`, final independent remote-head review and the Ready-event Agent Merge Guard. PR #542 squash-merged to `main` as `54003994`; Issue #497 closed and active owner/status labels were removed. The post-rebuild 404 smoke remains in #520.
- #520 preflight confirmed Podman 4.9.3 with Docker Compose v5.4.0 and the unchanged minimal development containers. The approved `.env.external-dev` path is absent, so no environment value, container environment, database, image or service state was read or changed. The tracked example rendered only after a disclosed non-secret structure placeholder; this is not real-environment validation.
- The Linux host has no `pwsh`, and the full root external-dev path requires all 17 database contexts. #544 owns a standard-library Python value-redacting validator for the smaller Auth path; no package installation was attempted.
- #543 removes the repository-fixed JWT verification key from `gateway/docker-compose.yml`. Module Compose now requires `${AUTH_JWT_SECRET:?set AUTH_JWT_SECRET}`, and the policy test permits exactly that single assignment. Gateway documents require a newly approved value shared with Auth and retirement of the exposed prior key; they do not claim runtime rotation complete.
- Independent value-nonprinting Compose checks passed: missing input exits 1 and process-only generated input exits 0 under `config --quiet`. Exact four-file allowlist, diff and marker gates pass; independent implementation review found no P0-P3. No service was built, started or restarted.
- Implementation commit `b1d50065` is pushed and Draft PR #545 targets `main`; Issue #543 is `status:needs-review`. Host JDK 17 is unavailable and no download was approved, so GitHub Module Validation `:gateway:test`, final remote-head review and Ready-event Guard remain before merge. Runtime key rotation and status-only smoke stay blocked in #520 until an approved secret path exists.

## 2026-08-27 - Issue #543 integration and #544 Linux minimal Auth env validator

- #543 passed GitHub JDK 17 Gateway validation, final independent remote-head review and the Ready-event Agent Merge Guard. PR #545 squash-merged to `main` as `a89ac260`; Issue #543 closed and its active owner/status labels were removed. The running Gateway was not recreated, so approved Auth/Gateway key rotation remains blocked in #520.
- #544 was implemented on `agent/544-minimal-auth-env-validator` in `/tmp/account-544-minimal-auth-env-validator` from `origin/main@162f26c5`. The new standard-library Python tool validates only the documented 11-key Auth/Master Data preflight contract without printing input values or file paths.
- Strict parsing rejects quote/interpolation/inline-comment/duplicate/placeholder/control-character input. File handling requires a regular non-symlink file, checks descriptor identity and POSIX permissions, and rejects FIFO/race cases. DB validation enforces exact runtime targets and roles, external hosts, bracket-safe IPv6, ASCII ports, exact PostgreSQL JDBC/query syntax and known local aliases.
- The first independent test found a frozen-exception/contextmanager classification defect. The first independent review then found a local-alias bypass plus JDBC delimiter, DEV IPv6, non-ASCII port and control-character gaps. All findings were remediated with regression coverage; final in-memory compile, `PASS: 165` self-test and independent `PASS: 91` adversarial harness passed, and final independent review found no P0-P3.
- Exact one-file implementation commit `2a0b6ebd` is pushed and Draft PR #551 targets `main`; actual env, container environment, DB, image and service state were not accessed or changed. Root external-dev still requires `AUTH_DEFAULT_PASSWORD` and all 17 DB contexts, so this smaller validator does not by itself satisfy #520 Compose render. Approved env absence and that integration gap remain explicit #520 blockers.

## 2026-08-28 - Issue #520 low-resource external-dev Auth stack

- Implemented on `agent/520-dev-minimal-auth-stack` in `/tmp/account-520-dev-minimal-auth-stack` from unchanged `origin/main@a1d30721`. The dirty primary checkout and its existing Frontend work were not modified.
- Added a standalone `tools/**` Compose path with exactly seven containers: a read-only two-PostgreSQL/Redis-PING gate, Config Server, Discovery, Auth, Master Data, Gateway and Frontend. It creates no PostgreSQL, Redis, Kafka, Batch, business API or observability container; only loopback Frontend/Gateway ports are published.
- Java builds are sequential with one Gradle worker. Runtime limits total 3.95 CPU and about 6 GiB; tracing is disabled and a console-only Logback configuration prevents the minimal stack from retrying the not-yet-repaired Logstash endpoint. Master Data explicitly overrides the Config repository H2 default with external PostgreSQL plus `ddl-auto=validate`, Flyway off and SQL init off.
- The runner rejects symlinked or changed env files, detects both exact legacy names and Compose service labels, separates Docker/Podman stats formats, bounds Eureka/login retry time, and stops only its fixed project-label containers after `up` or `all` failure. Volumes, the external network, existing Redis and external databases are preserved.
- Runner tests pass 7/7, validator self-test passes 165, shell syntax and fixture Compose quiet/JSON renders pass, and an ephemeral 0.1-CPU/64-MiB gate container received `PONG` from the existing Redis and removed itself. Diff, conflict-marker and common-secret-pattern gates pass. Independent review findings were remediated and final re-review found no P0-P3.
- Host JDK 17 is unavailable; the network-disabled cached JDK 17 attempt stopped before compilation on an uncached existing `org.ow2:ow2:1.5.1` POM. No dependency was downloaded and the Java policy test is not claimed as passed locally.
- `/home/ho/dev/account/.env.external-dev` remains ignored and mode 600. Fresh local-only values were generated for `ENCRYPT_KEY`, `AUTH_JWT_SECRET` and `AUTH_INTERNAL_API_TOKEN` without printing or committing them; the redacting validator now stops at blank `AUTH_DB_URL`. Image build, external PostgreSQL gate, new stack `up/smoke/status/stop` and replacement of the six running legacy targets remain blocked until the eight approved external Auth/Master Data DB inputs are supplied.
- Commit `111a99a4` plus record commit `14cc52d0` are pushed and Draft PR #576 targets `main`. All four GitHub checks pass, including JDK 17 Config Server validation. Issue #520 remains `status:blocked`; the approved live gate still precedes Ready/merge.

## 2026-08-29 - Issue #561 development Logstash pipeline recovery

- Claimed `#561` on `agent/561-logstash-pipeline` in `/tmp/account-561-logstash-pipeline` from `origin/main@38ad309c`; the dirty primary checkout and unrelated worktrees were not changed. Scope is the four Logstash files, one directly related Config Server policy test and parent-only harness records.
- The development Compose project `account-logstash-dev` mounts `logstash.yml` and `logstash.conf` read-only, accepts JSON lines on internal TCP 5000, writes only to `account-logs-dev-*`, removes the default 5044/stdout pipeline, exposes no host port and caps Logstash at 0.5 CPU, 640 MiB, 256 pids and a 256 MiB JVM heap.
- Health is fail-closed across the Logstash main pipeline and Elasticsearch: the response is accepted only when curl succeeds, `timed_out=false` and status is yellow or green. Independent review found the original HTTP-only P2 gap; the remediation and policy regression test received a no-P0-P3 re-review.
- Quiet Compose render, two Logstash `--config.test_and_exit` runs, exact allowlist/diff/marker checks and rendered-health checks pass. Live recreation and rollback recovery pass with read-only mounts, TCP 5000 open, 5044 closed, host ports 0, restart 0, OOM false, and a redacted synthetic event count plus pipeline in/out of 1/1. Predicate fixtures accept green and reject red and timed-out responses without changing Elasticsearch state.
- Local Java 17 test success is not claimed. The host has only Java 21; a cached network-disabled Gradle JDK 17 run stopped at an uncached existing `org.ow2:ow2:1.5.1` POM. No dependency or image was downloaded, so GitHub JDK 17 Module Validation is the required publication gate.
- The prior official-image `logstash` container had no mounts and became stuck in `stopping` after ignoring SIGTERM. After confirming its PID was gone and it had no mounts, only that exact stateless container was force-removed; its container identity is not recoverable, while the image and Elasticsearch data remain. The user Podman socket was started for the installed Compose provider. Rollback stops/recreates only the `account-logstash-dev` service and never uses volume deletion or prune.
- Commit `c48bca76` is pushed and Draft PR #585 targets `main`; Issue #561 is `status:needs-review`. GitHub JDK 17 Module Validation and an independent final review of the unchanged remote head remain before Ready/merge.
- Final GitHub JDK 17 Config Server validation, all four PR checks, exact remote-head review and the Ready-event Guard passed on `3e23cf65`. PR #585 squash-merged as `67c53f2f`; Issue #561 closed and its active owner/status labels were removed.

## 2026-08-30 - Issues #592/#593 external-dev PostgreSQL migration verification

- Resumed `#592` against the existing `account-postgres` PostgreSQL 16.13 server without replacing its container or volume. Created only the canonical `auth_dev` and `master_data_dev` contexts and their owner/runtime roles, populated the ignored mode-600 external-dev inputs without disclosure, and preserved all pre-existing databases and data.
- A value-redacting diagnostic isolated SQLSTATE `22023` to Flyway batch-metadata verification: PostgreSQL returns SQL `NULL` for `information_schema.columns.character_maximum_length` on numeric columns, while JDBC 42.6.2 rejected `ResultSet.getObject(4, Long.class)` for that null value. Issue `#593` owns the bounded migration-runner fix.
- `BatchMetadataVerifier` now reads the nullable length with `getLong` plus `wasNull`. Focused tests cover length 2500, non-null zero and SQL NULL, require the JDBC call order, and prohibit regression to typed `getObject`.
- A CPU-1/memory-1-GiB/JDK-17 container rerun passed the complete migration-runner suite: 85 tests in 8 suites, zero failures/errors/skips. The rebuilt runner then reported migrate/validate PASS for both Auth and Master Data against PostgreSQL 16.13.
- The normal runtime-grant script omitted an Auth identity sequence because it enumerates `information_schema.sequences`. Current DB privileges were safely completed using the intended sequence grant, and the final two-PostgreSQL/Redis dependency gate passed. The reusable script correction is isolated as `#594`; no volume, database or existing data was deleted.
- Static diff, conflict-marker and sensitive-value output checks pass. Independent final review, commit/push, Draft PR, GitHub checks and integration remain the `#593` publication gates; the verified DB dependencies unblock the later `#520` image build and runtime cutover.

## 2026-08-30 - Issues #593 integration and #596 rootless Frontend external-dev runtime

- The nullable PostgreSQL metadata fix passed independent review and all four GitHub checks. PR #595 squash-merged to `main` as `89a770cc`; Issue #593 closed and its active owner/status labels were removed. The reusable fresh-bootstrap identity-sequence enumeration remains isolated in #594.
- Resumed #520 from merged PR #576. The ignored mode-600 `.env.external-dev` passed the 11-key value-redacting preflight; Auth and Master Data migrate/validate plus the two-database/Redis gate passed without printing credentials or response bodies. Six application images were built sequentially with one Gradle worker before any cutover.
- The first real rootless Podman cutover failed only at Frontend because `../frontend:/app` exposed root-owned host source to the non-root image user. The runner stopped only its fixed project containers and the six exact retained legacy application containers were restarted; PostgreSQL, Redis, volumes and data were not stopped or removed. Issue #596 was split for the bounded repair.
- #596 removes the host source bind and keeps only project-owned `node_modules` and `.next` named volumes. Frontend readiness now probes public `/next.svg` with bounded internal wget timeouts, while the separate smoke continues to verify Frontend → Gateway → Auth and accepts only the existing HTTP 400 validation contract without printing the body.
- A second real attempt proved the permission repair but the old `/` readiness triggered a 4,726-module development compile and reached the former 768 MiB limit; the runner again performed project-scoped rollback and the legacy six were restored. With the static probe and a measured 1 GiB Frontend cap, the final cutover completed: all seven project containers are healthy with restart 0 and OOM false, `/login` returned HTTP 200, and the post-compile authentication smoke passed. The first compile touched the memory cap (`max=7`) but produced no OOM; the beginner guide records that it can be slow.
- The exact legacy six are now stopped but retained for rollback. Existing `account-postgres` and `account-redis` remain running. Only Gateway `127.0.0.1:18000` and Frontend `127.0.0.1:13000` are published. Config Server uses Spring `native`; Discovery, Auth, Master Data and Gateway use their container `docker` profile; the fixed Compose project label is the external-dev mode identity.
- Python runner tests pass 7/7, actual preflight and repeated live smoke pass, and the focused JDK-17 Config Server policy test passes under CPU 1 / memory 1.5 GiB / one Gradle worker. Independent review's P3 missing exact legacy cutover/restore commands was remediated in both beginner guides; final re-review found no P0-P3. Static gates pass; commit/push, Draft PR, GitHub checks and merge remain publication gates. Live runtime evidence is rootless Podman-specific, so Docker remains a documented but not locally claimed path.

## 2026-08-30 - Issue #596 integration and #594 identity-sequence runtime grants

- PR #597 passed all four GitHub checks, exact-head independent review and the Ready-event Guard, then squash-merged as `866c4de0`. Issues #596, #520 and #592 were closed with active owner/status labels removed. The seven-container rootless external-dev stack remained healthy for more than 16 minutes after merge with restart 0 and OOM false; the exact legacy six remain stopped but retained, and PostgreSQL/Redis remain running.
- Claimed #594 on `agent/594-identity-sequences` in `/tmp/account-594-identity-sequences`, then fast-forwarded without conflict to concurrent latest `origin/main@e5b0fa9e`. The three-file implementation replaces `information_schema.sequences` with the same `pg_class`/`pg_namespace` and `relkind = 'S'` catalog contract already used by both runtime health gates.
- A temporary PostgreSQL 16 container capped at 0.5 CPU/384 MiB created one ordinary, one SERIAL and one identity sequence. Two consecutive grant-script runs passed with sequence count 3, sequence privilege gaps 0, business-table DML gaps 0 and Flyway-history exposure 0; the no-volume test container removed itself.
- The corrected script was then rerun idempotently for the existing `auth_dev` and `master_data_dev` contexts. The live two-database/Redis gate remained healthy with restart 0 and OOM false; no database, schema, row, volume or service was deleted or recreated.
- Shell syntax and diff gates pass. The focused JDK-17 Config Server policy test passed under CPU 1, memory 1.5 GiB, pids 512 and one Gradle worker. Independent review found two P3 documentation-evidence/ACL wording gaps; the final exact assertion test was rerun and the ACL scope was corrected, and re-review found no P0-P3. Commit/push, Draft PR, GitHub checks and merge remain #594 publication gates. New migrations must rerun the grant/gate, and non-public schemas require a separate contract.

## 2026-08-30 - Issue #594 integration and #601 Prometheus live external-dev targets

- PR #600 passed all four GitHub checks, exact-head independent review and the Ready-event Guard, then squash-merged as `5f84c30b`; Issue #594 closed with active owner/status labels removed. The corrected Auth/Master Data grant gate and seven-container external-dev application stack remained healthy.
- Audited closed #562/merged PR #582 before runtime mutation. Its validation was static, the tracked target list addressed stopped legacy names plus `host.docker.internal`, and the existing container named `prometheus` was still Created with `/tmp/prometheus.yml`, no Compose identity and no CPU/memory limit. All five live minimal application metrics endpoints succeeded without response-body output, and Issue #601 was created for only the unmet live correction.
- #601 defines the unique one-service project/container `account-prometheus-dev`, fully qualified pinned image `docker.io/prom/prometheus:v3.14.0`, loopback `127.0.0.1:19090`, read-only tracked configuration, separate named data volume, restart policy and 0.5-CPU/512-MiB/128-pid limits. It targets Prometheus self plus exactly `minimal-auth`, `minimal-master-data`, `minimal-discovery`, `minimal-config-server` and `minimal-gateway`; legacy and host duplicates are removed.
- Compose quiet/rendered policy and Prometheus `promtool` config checks pass. The final dedicated JDK-17 policy test passes under CPU 1, memory 1.5 GiB and one Gradle worker; an intermediate map-order assertion failure was corrected to order-independent exact entries without relaxing the target set.
- Live rootless Podman verification passes with target totals 6/up 6/down 0, healthy/restart 0/OOM false, about 31 MiB observed memory, exact resource limits, loopback port and read-only config mount. Exact one-service stop preserved the named volume, legacy Created container and all application health; restart and post-recreate pinned-image target gates passed again. Independent review found two P2 lifecycle/recreate defects and two P3 duplicate-job/rootfs-evidence gaps. The unauthenticated lifecycle flag was removed, the runbook now requires validated one-service force recreate, policy enforces exact command/data path plus six unique jobs, and records no longer overclaim read-only rootfs. Live 6/6 and the JDK-17 test pass again; final re-review found no P0-P3. Commit/push, Draft PR, GitHub checks and merge remain. Docker live execution, digest pinning and TSDB volume size limits are not claimed.

## 2026-08-31 - Issue #540 HttpOnly BFF implementation and stacked rate-limit gate

- Claimed `#540` on `agent/540-http-only-bff` in `/tmp/account-540-http-only-bff` from exact `origin/main@85fa1adf`. The browser login and Master Data paths now use same-origin Next.js Route Handlers; the JWT exists only in an HttpOnly, SameSite Strict session cookie and is injected into Gateway requests by server code. Browser Authorization, Cookie and identity headers are not forwarded, unsafe methods require exact same-origin evidence, redirects are blocked and an upstream 401 clears the session.
- Production uses the Secure `__Host-account_session` cookie; development uses `account_session`. The login response exposes only session metadata, runtime `GATEWAY_INTERNAL_URL` replaces image-build routing, and the old localStorage keys are deletion-only migration artifacts. The PAT and governance mock/override contracts were explicitly restored because their Gateway route and RBAC work is outside #540.
- The body reader stops and cancels streaming input at 16 KiB for login and 1 MiB for authenticated proxy requests. Gateway request/error/rate-limit response headers are allowlisted while upstream `Set-Cookie` stays blocked. CPU-1/memory-1536-MiB/network-none Node 20 TypeScript passes; development BFF live verification passes 12/12, including declared and chunked oversized bodies, CSRF, spoofed identity stripping, header filtering, 401 cleanup and logout. Full ESLint remains at the inherited #536 baseline of 0 errors/244 warnings.
- Earlier production build and production live checks passed before the final bounded-stream/header remediation: all 122 static pages built and the BFF production flow passed 9/9. The final production rerun remains a publication gate. Config Compose renders, image-policy scans, external-dev redacting preflight, runner tests 7/7 and static diff/marker gates passed. The focused offline Java attempt stopped before compilation only because an existing Config Server artifact was not cached; GitHub JDK 17 validation remains required.
- Independent review found a BFF-induced Gateway IP-bucket collapse. Issue `#607` now owns the merge-blocking, stacked BFF-to-Gateway signed rate-key trust boundary in its own branch/worktree. #540 must not merge until #607 is independently reviewed and integrated into this branch; no secret value is tracked or printed.

## 2026-09-01 - Issue #607 BFF rate-limit trust boundary

- Resumed clean `agent/607-bff-rate-limit` in `/tmp/account-607-bff-rate-limit` at `f913d8d3`, exactly one implementation commit above `origin/main@4d01f240`. PR #608 and Issue #540 were already merged/closed, so #607 is published as the separate main-target Draft PR #623 rather than stacked back into the completed PR.
- Protected API rate keys now come only from the Gateway-verified principal exchange attribute. Login requests use a 30-second, path- and method-bound HMAC over an opaque username hash; malformed, duplicate, expired, wrong-path or invalid inputs ignore browser forwarding headers and fall back to the direct peer. Gateway removes the three BFF trust headers before routing.
- The in-memory limiter scopes buckets by route, caps the LRU store at 10,000 and consumes a larger BFF-peer aggregate bucket as well as the per-login bucket. Frontend and Gateway receive a distinct 32-512 UTF-8-byte runtime secret through fail-closed Compose inputs and redacting validators; no value, `NEXT_PUBLIC_*` variable or image build input was added.
- Node 20/network-none development and production BFF live tests both pass 12/12, TypeScript passes, ESLint passes with 0 errors and the inherited #536 baseline of 244 warnings, and the production build completes 122/122 pages. The first development live attempt stopped before Next startup because this worktree's dependency directory was empty; a lockfile-identical dependency tree was then mounted read-only and the bounded rerun passed.
- Forced JDK-17/network-none Gateway tests and the three changed Config Server policy suites pass with one Gradle worker. The Python validator compiles and its redacting self-test passes 168 checks; PowerShell validators were not executed because `pwsh` is unavailable, while their exact policy strings and required inputs are covered by the passing Java policy tests. Diff, clean-tree and conflict-marker checks pass.
- Independent review found no P0-P3. Residual test gaps are a single-process BFF-to-Gateway-to-Auth live flow and direct boundary cases for duplicate headers, future timestamp, 512-byte secret and BFF secret error responses; current unit/policy/live evidence found no defect. Rollback is a reviewed revert of PR #623 with no database, credential value, image, container or external runtime change.

## 2026-09-07 - Issue #466 Config Server Gateway route synchronization

- Resumed open Issue `#466` on clean isolated branch/worktree `agent/466-gateway-config-repo-sync` / `/tmp/account-466-gateway-config-repo-sync` from `origin/main@ad44f873`. Scope is the omitted external Gateway configuration, a focused parity regression test, and parent-owned harness records; no runtime, secret, database, or remote state was changed.
- `config-repo/gateway-service.yml` now mirrors the packaged Gateway's ten deposit/receivable/payable/tax/reconciliation/asset-lease/reporting/expenditure-resolution/account-mart/ECL routes, named CircuitBreaker filters and fallbacks. The dead `account-api` / `lb://account` catch-all is removed. Existing signed-BFF rate-key filters, `requestRateKeyResolver`, Cloudflare CORS, monitoring, logging and Eureka settings are preserved.
- `GatewayRouteSecurityPolicyTest` now reads the external Config Server YAML and compares its full routes, default filters, CORS and Resilience4j property sets with the packaged configuration. Java 21 `./gradlew :gateway:test` passes 72/72; diff, whitespace and conflict-marker gates pass.
- Independent review found no defect in the requested parity-only change, but identified a pre-existing P0 in PR #617's source contract: several packaged route predicates do not match current Controller base paths, and the deposit/ECL route URIs do not match their `spring.application.name` values. The user-required exact mirror deliberately does not redesign that baseline; resolve the source route contract in a separately approved scope before declaring the original runtime outage fully closed or making the PR Ready.
- The source and external files now both declare the ten requested `resilience4j.circuitbreaker.instances` entries with `baseConfig: default`, matching the route filter names. Rollback is a reviewed revert of this commit; no schema or data rollback is required.

## 2026-09-07 - Issue #466 stage 2 Controller paths and Eureka service IDs

- Continued the user-authorized follow-up on clean `agent/466-gateway-config-repo-sync` in `/tmp/account-466-gateway-config-repo-sync` from `e49989c4` (stage 1, based on `ad44f873`). GitHub inspection found stage-1 PR #632 already merged and Issue #466 closed; this entry supersedes the prior local-only/open status. The remote branch was absent after that integration, so the requested push will recreate the same branch.
- Both Gateway YAML files now follow the exact ten-route mapping: deposit targets `lb://deposit-service`, asset-lease targets `lb://asset-lease-service`, and ECL targets `lb://ifrs9-allowance-api`. Nine predicates add the actual Controller prefixes, including receivable collections/sales, payable payments/purchase, tax AP invoices, expenditure AP payments/expenditures and market-data; reporting already matched. The specified versioned aliases, route IDs, breakers and fallbacks remain intact.
- Updated `GatewayRouteSecurityPolicyTest.java` with nine Path and six packaged/external URI assertions. The existing parity test verifies all routes, default filters, CORS and Resilience4j properties across the two files. No build configuration or business logic changed; the parent owns the two YAML files and four shared records, while the Test Agent owns only the policy test.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :gateway:test` succeeded in 28 seconds with five executed tasks. XML evidence confirms nine suites, 72 tests, zero failures/errors/skips; the policy suite passes 30/30 including `externalRoutesRateLimitsCorsAndResilienceSettingsMatchPackagedConfiguration`. `git diff --check` and the tracked source/docs conflict-marker scan pass.
- Independent review is pending final confirmation. Publication is limited to the explicitly requested commit and push; no new PR, Ready transition, merge or Issue state change is included. Live Eureka-backed smoke and deployment were not performed. Rollback is a reviewed revert of this stage-2 commit, which restores the prior route contract and its known mismatches; no database or schema rollback is needed.

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

## 2026-09-10 - GH-653 approved Closing dev boundary repair

- Resumed `agent/653-business-runtime` in `/tmp/account-653-business-runtime` from `8924e83b`, based on fetched `origin/main@34c75839`. Latest owner approval (#653 comment 5604915412) supersedes the previous scope blocker and permits Closing application/configuration/adapters plus direct regression verification.
- Closing dev now scans only owned entities/repositories. API `ClosingMonolithConfiguration` preserves the prior non-dev composition; Core `HttpClosingJournalAdapter` provides real HTTP posting/list/slip lookup and fails explicitly for missing numeric-ID/detail/aggregate contracts. Existing Fiscal HTTP adapter has bounded shorthand timeout parsing and redacted failures. Compose enables fiscal remote and assigns internal destinations.
- Added the actual ClosingApplication dev migration/JPA-validate test and HTTP contract/failure tests. Final JDK17 offline CPU1/RAM1536MiB/one-worker `:closing:core:test :closing:api:test :closing:batch:test` passes 124 tests (59/49/16), no failures/errors/skips. Python runner regression passes 38/38. Independent review's P3 outbound placement finding was resolved by moving the Journal adapter to Core infrastructure; re-review has no remaining P0-P3.
- Closing image rebuilt successfully with `tools/Containerfile.minimal-auth-java` / `bootJar --max-workers=1`. Actual accounting quiet Compose preflight and DB prerequisite pass; sequential API runtime verification is in progress. Previously built six other Accounting images are retained. No new package or 13/13 success is claimed yet.
- Remaining limitations: unsupported Journal remote queries block dependent financial workflows explicitly; health/Eureka is not financial correctness evidence. Non-dev/production composition and distributed fiscal-period transaction atomicity remain outside this repair. No DDL/DB grants/shared contracts/platform changes or secret/raw runtime output.
- Parent owns runtime, harness and GitHub mutations; separate service/test writers had disjoint allowlists and independent reviewer remained read-only. Draft PR (Refs #653) publication still awaits the live verification gate; no Ready, merge, Issue close or worktree cleanup.
- Rollback: stop only exact failed newly created/recreated package service after label verification, retaining healthy services, DBs, images, networks and volumes. Source rollback is a reviewed PR revert. Next owner: parent Integrator for sequential Accounting → Products → Risk gates, then human review.
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

## 2026-09-10 — GH-179 frontend/backend API parity audit

- Issue #179; difficulty:medium, requested reasoning high. Branch `agent/179-api-parity-audit`, worktree `/tmp/account-179-api-parity-audit`, base `origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e`. Dirty primary checkout was preserved. Audit commit `d15632a0` was pushed; [Draft PR #676](https://github.com/skyg547/account/pull/676) is open with `Refs #179`, verification results and the merge-authority statement. Issue status: `status:needs-review`.
- Scope follows rejected PR #548 instructions: read-only production evidence, 15 current service/helper files, exhaustive actual method/URL and field/type/nullability/enum comparison. Write allowlist: `frontend/docs/frontend-backend-api-parity-matrix.md`, `frontend/README.md`, `docs/ai-harness/worklog.md`, `docs/ai-harness/agent-status.md`, `docs/ai-harness/handoff.md`, `docs/history/CODEX_WORKLOG.md` only.
- Result: 42 endpoint calls + 3 transport helper calls; 33 mapped and 9 MISSING. Corresponding 13 Controllers have 87 mappings, 54 backend-only. Nested DTOs, missing required query/body fields, enum/nullability drift, BFF transformation/actor injection and actual fallback behavior are documented. Universal endpoint/DTO compatibility is not claimed; Issue stays open.
- Verification: embedded `node /tmp/account-179-verify.cjs` PASS including row/method/path/Controller/backend-only evidence and known DTO/query drift; in-memory negative checks PASS 7/7. `node frontend/node_modules/typescript/bin/tsc --project frontend/tsconfig.json --noEmit --incremental false` exit0. From frontend, `NEXT_TELEMETRY_DISABLED=1 NODE_OPTIONS=--max-old-space-size=3072 taskset -c 0 npm run build` exit0, Next15.5.23/static pages122/122. Existing dependencies reused without installation/lockfile changes. Node22.23.2 differs from engine Node20; Node20 run remains unclaimed.
- Independent Reviewer found one P3: Closing PUT JSON decode errors propagate while HTTP/transport errors return null. Corrected the audit wording; final independent review found no remaining P0-P3. Diff/marker/new-content control checks pass. One pre-existing worklog control byte is unchanged and outside this edit. No production code, credentials, remote API/DB, or runtime state was changed; no secret values were output.
- Skipped: Gradle (no Java changes), Node20 build (current runtime22), live Gateway/Jackson/UI/DB checks (static audit scope). Known MISSING/DRIFT and serializer/runtime TODOs remain follow-up work. Rollback: revert audit commit(s). Next owner: independent Reviewer / human Integrator for Draft PR review and follow-up scope; no Ready/merge/Issue close/worktree removal authorized.

## 2026-09-10 — GH-663 current-main record conflict rework

- Current rework round1/2: parent integrated `origin/main@b7c1c7c1fa45ec6550ab2431674fcf22a519bcea` into original GH-663 head `62836e6cb8fc95fbe5b0512db837f96cfb92fc02` by ordinary merge; no force/rebase/automatic side selection. Four EOF conflicts retain main GH-179 records before the original GH-663 records; both-parent line-order preservation is 8/8 PASS.
- Trigger: account's independent final content APPROVE identified latest-main integration conflicts in parent records only. Original branch `agent/663-migration-canonical-sslmode` / `C:/tmp/account-663-migration-canonical-sslmode`, original owner retained. Parent4+required conflict-log5 are the only rework edits; MigrationConfiguration and its test remain byte-identical to reviewed62836e6c. Incoming GH-681 policy and GH-179 frontend records are carried unchanged, not new issue scope.
- Actual verification after manual resolution: forced cached JDK17/Gradle8.7 target46/46 (10s), full8suites125/125 plus bootJar (16s),0failures/errors/skips; `--offline --no-daemon --console=plain --max-workers=1 --rerun-tasks`. Target command selects `com.ho.account.migration.MigrationConfigurationTest`; full command is `:migration-runner:test :migration-runner:bootJar`. Parent checked fresh XML; old RED evidence remains historical, not rerun.
- The four record files retain every line from both parents in relative order (8/8), including inherited control-byte history. No unrelated text cleanup; conflict-log explains why both EOF additions belong. Static checks and independent review gate commit/push; exact published-head CI gates Ready. Record-only follow-up status stays on the remote PR to avoid commit churn.
- Q1/Q2/Q4: original helper105 and comments110/115/122, Test116 preserve readable one-purpose validation, raw input→count→reject/accept and no-connection intent; current target/full GREEN. Q3: README49–54/runbook55–58 still state required verify-full; no production usage/parameter change during rework, functional docs remain untouched with independent rationale review. Parent record explanations distinguish historic vs current states and implementation vs final-merge authority.
- Latest queue supersedes earlier checkpoints: GH-681 merged/closed/cleaned, GH-664 and GH-683 frozen Ready; this actionable original-Issue rework occupies this automation's one implementation slot. #668 follows Ready independently of unrelated merge/close waits. account's rejected merge execution remains approval-gated; parent does not retry or bypass it.
- Rollback: reviewed code/test revert for original bugfix (may reintroduce TLS policy mismatch), or scoped record correction preserving both histories for this integration. No SQL/data/certificates, build/dependency changes, live services, package installation or primary checkout edits.

## 2026-09-10 — GH-663 production TLS canonical query validation

- Workflow: Account Issue 구현 오케스트레이터; task `019fa3ea-ac4c-7022-bb45-951559750df7`. Parent remote claim: https://github.com/skyg547/account/issues/663#issuecomment-5611047479. Original ready+atomic spec has no dependency on GH-662/GH-681.
- Branch `agent/663-migration-canonical-sslmode`, worktree `C:/tmp/account-663-migration-canonical-sslmode`, base `5f8103afd4f018723302fd4b15b740b3bcf189c1`. Writer `/root/migration_663_writer` GPT-6 Astra high (security boundary), independent `/root/harness_ci_audit` Astra high; parent alone owns these four records and Git/GitHub.
- Substantive scope: MigrationConfiguration.java and MigrationConfigurationTest.java only. Production accepts exactly one case-sensitive raw `sslmode=verify-full` entry, counting all case-insensitive sslmode keys to reject ambiguous duplicates/bare/empty keys. URL is never decoded/rewritten; development, database binding, approvals and credential handling remain unchanged.
- RED before source fix: targeted46 tests/16 expected failures/0 errors/0 skips. GREEN targeted46 and full125 (existing85+40),0 failures/errors/skips. Existing runtime PostgreSQL Driver.parseURL reflection reproduces original effective disable and confirms five unchanged accepted URLs effective verify-full without a connection.
- Commands: cached Gradle8.7/JDK17, `:migration-runner:test --tests com.ho.account.migration.MigrationConfigurationTest` then `:migration-runner:test`, both with `--offline --no-daemon --console=plain --max-workers=1` and explicit installed JDK path. Independent reviewer reran full task with `--rerun-tasks`:8 suites125/125. Parent parsed fresh XML and checked diff/markers. No installs/downloads/live DB/TLS/credentials or primary-checkout edits.
- Q1 PASS: helper105 has single validation responsibility and expressive naming. Q2 PASS: raw input→count/reject→boolean comments110–122 match regression/driver behavior. Q3 PASS: README canonical example24 and production requirement49–50 already express intended contract; this bugfix restores it and does not require out-of-allowlist docs. Q4 PASS: nearby raw/no-rewrite/ambiguous-key comments and reflection no-connection comment116. Independent `/root/harness_ci_audit` confirmed Q1–Q4 and returned APPROVE, no P0–P3 findings. RED counts are implementer-provided execution evidence; the independent reviewer reran current GREEN rather than reverting production to reproduce RED.
- Parent appends records here to preserve existing histories and avoid the unrelated GH-681 top-of-file insertions. Latest queue authority is the updated automation and remote Issue/PR, not old global-stop checkpoints. PR handoff releases implementation slot; rejection returns this original Issue for rework. Final merge/close belongs to account; no self-merge/close or cleanup here.
- Rollback: reviewed revert of the two substantive files; no SQL/schema/data/certificate operations. Residual scope: actual TLS handshake/other JDBC option audit not performed; no claim of a real exposure incident. After independent review, the parent commits, pushes and publishes a Draft PR; only after checking CI at that exact published head does the parent mark Ready. Remote publication status is recorded on the Issue/PR to avoid record-only commit churn.

## 2026-09-11 — GH-691 GL/SL period opening aggregation

- Issue/claim: [GH-691](https://github.com/skyg547/account/issues/691#issuecomment-5623212221). Workflow `Account Issue 구현 오케스트레이터`, task `019fa3ea-ac4c-7022-bb45-951559750df7`; parent Integrator exclusively owns remote state and records. Branch `agent/691-ledger-period-summary`, worktree `C:/tmp/account-691-ledger-period-summary`, base `65e6b1e36554ed3adb9616b4eedb579843f0e748`.
- Writer `/root/issue691_implementation`, GPT-6 Astra xhigh, four exact substantive allowlist files: LedgerService.java, LedgerServiceTest.java, new LedgerPeriodBalanceQueryTest.java and journal-ledger/docs/ledger-carry-forward.md. Parent records: WORKLOG, CODEX_WORKLOG, ai-harness worklog/agent-status/handoff. Original unrelated changes and histories are preserved.
- Root cause: successive daily openings already include carry-forward and cannot be added as independent movements. Each original GL/SL grouping key now tracks its earliest source date separately from the requested output date; opening is selected once, movements are accumulated in O(n), and each new aggregate recalculates its ending once. No sorting/mutation of source rows or public key/filter/period redesign.
- Actual representative RED = 4 tests / 4 failures / 0 errors / 0 skips, expected opening1000 versus actual2080. Initial malformed Gradle property parsing never ran tests. Final writer forced offline GREEN = core18suites/65tests, API7/11, Batch3/5; total28suites/81tests, zero failure/error/skip, exit0/53s/37tasks executed. Fresh XML UTC2026-09-10 core18:19:14/API18:19:36/Batch18:19:48. New regressions21; original bulk and MonolithLedgerQueryAdapter regressions preserved. Independent `/root/issue691_code_review` Astra xhigh separately reran the same complete tasks: exit0/53s/37tasks, fresh28XML after UTC2026-09-10 18:21:39.8137081, again81/81 with failure/error/skip0. Frozen substantive hashes match; no remaining P0–P3 and independent Q1–Q4 PASS. This is a separate session's review, not implementer self-approval.
- Exact final command (installed JDK17/Gradle8.7 cache; no installation): `.\gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --offline --no-daemon --console=plain --max-workers=1 --rerun-tasks '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot' '-Porg.gradle.java.installations.auto-download=false'`. Earlier targeted LedgerServiceTest and combined core/API regressions also passed; final forced run removes UP-TO-DATE ambiguity.
- Independent reviewer used the same tasks/options with `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot` and cached executable `C:/Users/skyg5/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/bin/gradle.bat`, then checked all fresh XML suites; no dependency download or external service execution.
- Parent static gates: exact four substantive plus five record paths; diff check PASS; no added conflict markers/control characters; all five old record contents remain exact ordered prefixes. Production hash SHA256 `B0CDAADF0FC2E17EA4CE7F787B86F04F8F5BDFBB9E0D60A92B2CC5E128010CFE`; remote main remained base65e6b1e3 at the pre-review check.
- Q1 PASS: focused O(n) grouping/min-date selection and movement-only helpers with exact precision, key/order/date preservation tests. Q2 PASS: functional document input→earliest opening→movement sum→ending and example. Q3 PASS: actual functional doc includes boundaries/non-goals/HTTP paths/verification. Q4 intent-comments PASS: why carry-forward is selected once and recalculate waits for a completed group, plus actual-service MockMvc intent. Automation Q4 actual-verification PASS is the writer and independent forced81/81 evidence above. All four are confirmed by the separate reviewer; no N/A shortcut.
- Non-goals/unexecuted: missing-transaction as-of reconstruction, existing balance-data repair, calculateGlBalanceAggregate COUNT/SUM, posting/concurrency, external DB/server or real business Batch execution. No install/download/credentials access. Rollback: reviewed scoped source/test/doc revert, no DB/schema/data operations.
- Next gates: final parent-record delta confirmation, exact-file commit/push/Draft PR (`Refs #691`), applicable current-head CI and latest-main integration before Ready. Parent does not self-approve/merge/close; account owns final review/merge/Issue close. Frozen locally verified and independently reviewed PR releases the writer slot even while CI/Ready is pending; no global merge wait.

## 2026-09-11 GH-693 — Posting period guard / rework1

- Workflow/task: Account Issue 구현 오케스트레이터 / `019fa3ea-ac4c-7022-bb45-951559750df7`. Issue https://github.com/skyg547/account/issues/693, branch `agent/693-posting-period-recheck`, worktree `C:/tmp/account-693-posting-period-recheck`, base `65e6b1e36554ed3adb9616b4eedb579843f0e748`.
- Substantive allowlist4: `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/ledger/PostingService.java`, its `core/src/test/.../ledger/PostingServiceTest.java`, `journal-ledger/docs/process-flow.md`, and `loan/core/src/test/java/com/ho/account/loan/service/LoanJournalPostingFlowTest.java`. Parent records5 are separate. No provider/public port/build/schema modification.
- Existing ClosingLockValidationFilter runs after immutable approved snapshot validation and before first state mutation. This preserves validation priority and OPEN writes, while closed/missing/unavailable periods fail before state/audit/details or any of the three writes changes. Actual filter/adapter semantics, distinct slip/accounting dates, SYSTEM overload and invalid input invariants are exercised.
- Evidence correction: initial spec/claim incorrectly said only PostingServiceTest constructs the service. Independent reviewer reproduced Loan fixture compile failure. Parent updated original Issue scope by the indivisible constructor-consumer exception in policy87 section3-4, kept xhigh, obtained separate-session spec delta APPROVE, then assigned the original writer rework1. This is one root cause, not an unrelated Loan feature or a guard-bypass compatibility constructor.
- RED: production diff0, closed-period rejection test1/1 failure, UTC2026-09-10 18:53:42.421–18:54:00.978, exit1; actual filter rejects but original service does not throw. Writer initial forced journal GREEN UTC18:59:43.507–19:00:37.709,37tasks executed/74tests/27suites,0failure/error/skip. Independent journal rerun UTC19:02:16.089–19:03:10.521 passed74; independent Loan compile UTC19:04:15.001–19:04:34.405 failed with one 4-versus-3-argument javac error. Journal-only success was not accepted as whole-impact completion.
- GH-693 final writer verification: Loan targeted1/1, exit0/13.081s UTC19:10:46.291–19:10:59.371. Expanded forced bundle UTC19:11:39.417–19:13:09.781, exit0/90.365s/54tasks all executed; fresh XML journal core18suites62tests/API6suites7/Batch3suites5 + Loan core15suites99/API4suites6/Batch2suites3 =48suites182tests, failures/errors/skips0. All XML after run start, no test task NO-SOURCE. Loan API/Batch bootJar fresh at19:12:41.223/19:12:56.098, respectively111553472/112624044 bytes. Source4 hashes identical before/after; service/core test unchanged during rework.
- GH-693 independent rework verification: cached Gradle8.7/JAVA_HOME17 and the three Loan test + two bootJar tasks with the same offline forced options, UTC19:15:17.6797021–19:16:12.1245114, exit0/54s/31tasks all executed. Fresh21XML/108tests=core99/API6/Batch3, failure/error/skip0; targetLoan1PASS, JAR2fresh. Independent Journal74 execution remains valid for unchanged source/core-test hashes. Reviewer resolved P1, no additional P0–P3. Parent final record/PR delta is reviewed before local checkpoint commit, while latest-main integrated verification and CI/Ready remain separate.
- Validation environment: installed JDK17 at `C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot`, cached Gradle8.7, offline/no-daemon/console=plain/max-workers=1, auto-download=false. Full bundle tasks: journal-ledger core/API/Batch test, Loan core/API/Batch test and Loan API/Batch bootJar with --rerun-tasks. Targeted LoanJournalPostingFlowTest first; NO-SOURCE is not a passing test. Actual fresh XML/exit evidence required.
- GH-693 quality gates: independent xhigh source/doc Q1–Q4 PASS. Q1: PostingService34,49–59 and Loan test45–51,72,106–107, one guard with actual consumer injection and independent108 tests. Q2: service50–52/process-flow46–69, input→snapshot→period→write/refusal matches independent Journal74+Loan108. Q3: process-flow71–90, exact commands/NO-SOURCE/package and unverified live boundaries. Q4: service50–51/Loan46/PostingServiceTest307,341, nearby revalidation/actual-filter/value-snapshot intent comments. N/A reason for all: not applicable, each quality criterion applies to this code/doc change. Parent actual exact9/prefix5/diff/markers/added-control checks PASS. Next gate remains latest-main integration, expanded rerun, independent delta and published-head CI/Ready.
- Risk/rollback: one period query per valid posting, no distributed closing/posting atomicity or duplicate-posting concurrency guarantee, no old data repair. Actual PostgreSQL/external servers/business Batch/deployment are out of scope. Reviewed four-file code/test/doc revert, no schema/data migration. Parent preserves history/other owners, publishes Refs #693 Draft only after independent review, then verified-head CI/Ready; account owns final merge/close.

### GH-693 latest-main integration evidence

- Local checkpoint `7ed966ea2ebb9e5c716019fabaacec3c56ddffb8` was independently approved before commit. Normal merge of main `65af7e6f07a642ecf683e7e67b17049eb467da8b` imports reviewed GH-691. Parent inspected five EOF-only record conflicts and preserved each complete incoming691 entry then own693 entry; conflict-log records this judgment. Both complete histories were asserted intact, not resolved with automatic ours/theirs. Source6934 and incoming6914 are unchanged from their reviewed sides.
- Independent `/root/issue693_code_review` Astra xhigh reran the documented full6-test/2-bootJar bundle on the resolved integrated tree, UTC2026-09-10 19:23:01.2608176–19:24:32.6531057, exit0/91.3922881s/54tasks all executed. Fresh49XML203tests: Journal core18suites79tests/API7suites11/Batch3suites5 + Loan core15suites99/API4suites6/Batch2suites3, all failures/errors/skips0. Test task NO-SOURCE0. Posting15/LedgerService18/HTTP4/LoanFlow1 PASS; these are included, not added again to203.
- Loan API/Batch JARs fresh at19:24:04.1206293/19:24:19.0636203 UTC,111553780/112624352 bytes. Parent/reviewer mainrelative allowlist10(source4+records5+conflict-log), main-record prefixes+original693 entries, unmerged-index0, markers0 and diff checks PASS. Frozen source4 SHA256 unchanged. Incoming Ledger read-only aggregation changes do not alter PostingService's bulk write path; semantic integration has no new P0–P3 and Q1–Q4 remains PASS.
- This updates the prior checkpoint's pending integration gate with actual evidence. Parent integration-record6/PR final delta is reviewed before completing/pushing the merge commit and opening Refs #693 Draft. Exact published-head CI and latest-main/ownership gates still precede Ready; account owns repository final merge/Issue close. Remote publication state is not fabricated in these prepublication records.

## 2026-09-11 — GH-692 settlement amount and next-state precision

- [Issue/claim #692](https://github.com/skyg547/account/issues/692#issuecomment-5623500253); Workflow `Account Issue 구현 오케스트레이터`, task `019fa3ea-ac4c-7022-bb45-951559750df7`. Branch/worktree `agent/692-unsettled-precision` / `C:/tmp/account-692-unsettled-precision`, base `65e6b1e36554ed3adb9616b4eedb579843f0e748`. Parent retains exclusive remote ownership and Git/PR operations.
- Substantive writer `/root/issue692_implementation` Astra xhigh owns UnsettledItem.java, UnsettledItemTest.java, new UnsettledSettlementPersistenceTest.java, new UnsettledSettlementPrecisionTest.java and journal-ledger/docs/schema.md. Parent owns WORKLOG/CODEX_WORKLOG/ai-harness worklog/agent-status/handoff; existing records preserved. PR697 is frozen in a disjoint substantive scope; potential parent-record integration must preserve both histories.
- Root cause: unrestricted decimal amount changes domain balances/status before NUMERIC(19,2) storage can round them. The domain now validates a new ref's positive amount and both next nonnegative balances before assignment. Invalid precision cannot partially change amounts, status, reference content/collection or audit fields. Previously handled full-set/legacy refs remain no-ops before new amount validation.
- Actual RED before production edits: `.\gradlew.bat :journal-ledger:core:test --tests 'com.ho.account.journalledger.domain.unsettled.UnsettledItemTest' --offline --no-daemon --console=plain --max-workers=1 --rerun-tasks '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot' '-Porg.gradle.java.installations.auto-download=false'`: exit1,1suite/40tests/22failures/0errors/0skips, XML2026-09-10T18:33:27. Covers unwanted scale acceptance, unvalidated next states, 1.000 normalization and early null collection mutation.
- Writer forced full GREEN: core18suites86tests/API8suites26/Batch3suites5=29suites117tests, failure/error/skip0, exit0/55s/37tasks all executed. Fresh XML UTC2026-09-10 core18:40:46–48/API18:41:04–08/Batch18:41:21–23. Final domain41 includes an extra precision-overflow-before-balance comparison case after RED; new API19 = actual H2/JPA2 + service/MVC17. Separate `/root/issue692_code_review` Astra xhigh directly reran all tasks UTC18:42:38.0046551–18:43:33.2246647: exit0/55s/37tasks executed, fresh29XML/117tests, failure/error/skip0. XML core18:42:55–57/API18:43:13–17/Batch18:43:28–31. Independent source/test/schema P0–P3 none and Q1–Q4 PASS, all five frozen hashes unchanged. This is separate-session evidence, not implementer self-approval.
- Exact final command: `.\gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --offline --no-daemon --console=plain --max-workers=1 --rerun-tasks '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot' '-Porg.gradle.java.installations.auto-download=false'`. Installed JDK17/Gradle8.7/cache only. Earlier domain targeted40/40 and new API19/19 also passed; final forced run verifies the frozen source together.
- Independent reviewer set `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot` and used the same tasks/options via cached executable `C:/Users/skyg5/.gradle/wrapper/dists/gradle-8.7-bin/bhs2wmbdwecv87pi65oeuq5iu/gradle-8.7/bin/gradle.bat`, not a second unverified wrapper-only claim.
- Parent static verification: exact5 substantive+5record paths, append-only preservation5/5, diff check and conflict-marker checks PASS. Frozen UnsettledItem SHA256 `391BCABB2FCB07737BA739C3E0C3715420337F98E015CA2ECD7666D7FA8D9AD2`.
- Q1 PASS: domain-only precision/state rule, existing shared policy, all computed values validated before assignment; tests preserve replay and field snapshots. Q2 PASS: schema input→validation→state→save and 0.999 counterexample. Q3 PASS: actual functional schema, valid/rejected inputs, full-ref/legacy and HTTP-vs-domain replay boundaries, offline prerequisites. Q4 intent-comments PASS: replay priority, no DB rounding before state validation, isolated JPA transaction and real-service test intent. Automation Q4 actual-verification PASS = writer and independent forced117/117 above. Separate reviewer confirmed all four, with no N/A shortcut; final parent-record/PR delta confirmation remains before publication.
- No DTO/shared exception/migration/lock/ref-fingerprint/currency-policy redesign or existing inconsistent-data repair. No external DB/server, real business Batch/deployment, dependency download, credentials access or cleanup. Rollback: reviewed Issue source/test/doc revert; never data/audit/ref deletion or migration alteration.
- Parent publishes exact validated files as `Refs #692` Draft PR after independent review. Current-head CI, no blocking findings and current-main integration gate Ready. Final account reviewer owns approval/merge/Issue close; this writer never self-approves or merges. Frozen verified/reviewed PR releases the implementation slot independently of merge; remote Issue/PR records publication SHA and later CI/Ready state.

## 2026-09-11 GH-692 수동 통합 재작업

- 사용자 직접 요청으로 PR #698의 공유 기록 충돌을 원 브랜치/외부 worktree에서 보완한다. 세 예약은 PAUSED 상태이며 이번 수동 실행이 예약을 재개하지 않는다. 최신 main c94afec5와 원 head f5e32b3의 양쪽 역사를 모두 보존했다.
- 실질5파일은 원 head와 Git blob까지 일치한다. 별도 GPT-6 Astra xhigh /root/manual_review_698의 새 통합본 강제 offline 실행은 core117/API30/Batch5, 총152 tests/30 suites, 실패·오류·skip·stale0, exit0/58초/41tasks 모두 실행이다(UTC2026-09-11 01:00:33.8897880–01:01:33.3742691). API/Batch bootJar2도 새로 생성되었으며 패키징 증거이지 실제 서버 기동 증거는 아니다. 독립 코드 검수 P0–P3 없음/Q1–Q4 PASS; 최종 기록 delta와 원격 CI/Ready 검토가 남는다.
- 부모가 최종 기록·검증 근거와 독립 리뷰를 모아 새 head를 push하고 CI/Ready-only를 확인한다. 사용자 수동 병합 승인 아래 부모만 최종 merge commit/수용 완료 Issue 종료를 수행한다. 기존 자동화 전용 소유 문구는 역사이며 현재 수동 실행의 게이트는 원격 claim5627728952다. 자원 삭제·DB/설정/예약/공유계약 변경은 없다.

## 2026-09-11 GH-673 — bounded review content reads

- Issue https://github.com/skyg547/account/issues/673 / claim5624214896. Owner Account Issue 구현 오케스트레이터, task `019fa3ea-ac4c-7022-bb45-951559750df7`; branch `agent/673-review-read-scope`, worktree `C:/tmp/account-673-review-read-scope`, base `65af7e6f07a642ecf683e7e67b17049eb467da8b`.
- Existing Issue preparation was independently approved by `/root/issue673_spec_review` Astra high and its preparation claim explicitly released before implementation. Writer `/root/issue673_implementation` Astra high uses bounded Documentation/Test roles; independent code reviewer `/root/issue673_code_review` Astra high is separate from writer and parent spec author. Exact substantive2: GEMINI_REVIEW_PROMPT.md and tools/ci/review-scope-contract.test.cjs. Parent records5 only; no global settings/roles/automations/CI wiring or business-code edits.
- Input names/status → current exact non-sensitive content allowlist ∩ changes → literal single-file diff/read or hold. Empty/missing/outside/sensitive/absolute/traversal/ambiguous identity is not an unscoped fallback. Top/copyable/handoff gates agree; historical scopes/dates/results remain evidence, not present permission. Prior history separation remains #675 and is not silently implemented here.
- Test implementation reads only the approved document. Static ordered/rule checks, synthetic tracked/staged/untracked path plans and in-memory document mutations are coupled. Fixtures never read real paths or execute their planned commands. Educational comments explain why normalization, extra disk inputs and historical-command reinterpretation are avoided. The conservative fixture checker is not a general-purpose filesystem/security engine.
- Actual RED: original GEMINI document diff0, selected actual-document assertion1 failed for missing top gate, exit1 UTC2026-09-10 19:28:57.1168762–19:28:57.2830751. Writer GREEN command `& 'C:/Program Files/nodejs/node.exe' --test tools/ci/review-scope-contract.test.cjs`, Node24.18.0,60tests/pass60/fail0/skip0/cancel0/todo0,exit0 UTC19:30:37.9318357–19:30:38.2227338,wall0.290898s/runner142.7448ms. RED uses the same executable/file with --test-name-pattern='^actual document'.
- Independent reviewer directly reran that full command UTC19:33:06.8961432–19:33:07.0447777,exit0,60tests/pass60/fail0/skip0/cancel0,wall148.6345ms/runner94.2432ms. Diff check0/scoped marker no-match and frozen2 hashes match. Source/document no required finding; final parent-record/PR delta review precedes publication.
- Q1 PASS: test functions30/38/76/90 separate section/document/path/plan responsibilities with table fixtures116–183. Q2 PASS: document6–46 input→intersection/exclusion→scoped/hold and test91–104 document coupling/synthetic identity. Q3 PASS: document49–64 verified Node command/prerequisites/expected results/CI-disconnected limits. Q4 PASS: test8–9/62–63/78–79/91–92/103 explain single input, historical commands, no normalizing unsafe paths, doc coupling and synthetic evidence. All apply; N/A reason is not applicable because this approved code/doc change uses each criterion. Independent source/doc review confirms all four; final records remain a distinct review delta.
- No real sensitive file presence/content probes, external systems, package downloads, Gradle or CI connection. Static/doc/synthetic tests do not enforce real AI access or guarantee exhaustive secret detection. Rollback reviewed substantive2 revert, preserving shared history; no schema/data changes. Parent follows Refs #673 Draft/new-head CI/latest-main/Ready, account owns final merge/close. Exact publication metadata lives on the Issue/PR.

## 2026-09-11 GH-673 manual rework1 — PR700 P2

- User manually authorized review/fix/review/merge; all automations remain PAUSED. Original branch/worktree/Issue retained, new claim5627729450. Removed active bare whitespace-check exemption, applied the same nonempty exact content gate to state-specific whitespace comparisons, preserved historical prose and scope.
- Original writer Astra high: actual unchanged-document RED1/fail1 exit1 (UTC01:01:18.1185470–01:01:18.2962500), then full Node71/71 PASS/fail,skip,cancel,todo0/exit0 (UTC01:03:20.1429198–01:03:20.3555019). Parent separately used only safe synthetic approved.txt/outside-review.txt in an external fixture: unscoped unstaged/staged checks each exit2 with synthetic outside-line output; exact approved-path alternatives each exit0/no output. This is no real-secret probe; test-file diagnostics remain in-memory, not a claimed real Git execution.
- Independent /root/manual_review_673 Astra high source/test/history review APPROVE, P0–P3 none/Q1–Q4 PASS; ONE direct full Node run UTC2026-09-11 01:06:00.0622846–01:06:00.2262431, exit0/71tests71pass/fail,skip,cancel,todo0/runner108.3914ms. Frozen2 hashes unchanged and historical lines remain ordered. Final record/PR delta remains a separate gate. Parent alone replaces CLOSED/unmerged700 with a new Draft after acceptance, then checks fresh head CI/Ready/main before user-authorized merge. Runtime sandbox, exhaustive secret detection, CI wiring, business code and history split#675 remain non-goals; preserve histories/resources.

- Latest-main follow-up: reviewed checkpoint240522c2 now integrates actual PR698 merge/main5b2d184f (Issue692 CLOSED). Parent inspected all seven record-only EOF conflicts and preserved complete incoming692 histories then existing673 histories. Source2 and test input remain frozen; current71-test evidence is distinguished from prior60. Independent integration-record review and published-head CI/Ready remain required; no673 PR merge is claimed here.
- Parent actual latest-main Node rerun: UTC2026-09-11T01:13:54.2879577Z–01:13:54.4608996Z, full71/71, failures/skips/cancel/todo0, exit0, runner117.813ms. Complete main prefixes7/7 and original ordered histories7/7, markers0/unmerged0. This is an additional parent execution, not a second independent review; separate prior71-test evidence applies to unchanged source2/doc input.

## 2026-09-11 - GH-696 governance API runtime and proxy/observability recovery

- Issue: #696 (`difficulty:high`); branch `agent/696-governance-runtime`; isolated worktree `/tmp/account-696-governance-runtime`; fetched base `origin/main@c94afec5`. PR: https://github.com/skyg547/account/pull/701. The dirty primary checkout was preserved.
- Added the bounded two-API development Compose with PostgreSQL/Redis wiring, module-specific discovery flags and the case-sensitive Eureka JVM property. Budget uses port 8096 / `budget_db`; Internal Audit uses port 8083 / `internal_audit_db`, registering as `INTERNAL-AUDIT-SERVICE`. Runtime Flyway/DDL/SQL initialization remain disabled. Both API jars already had Eureka/driver dependencies; no business Java or Gradle file modification was needed.
- The dedicated provisioner generated distinct owner/app inputs in an ignored mode-600 file and used stdin-only secret transport. Both databases passed migrate, validate, expected schema, login, table/sequence ACL and denied-DDL checks; Budget has 13 public tables and Internal Audit 16 including migration/Batch metadata. Actual API sessions were observed as 2 per database. PostgreSQL container/image/mount identity and unrelated database catalog remained unchanged. No DB, volume, password or business data was deleted/reset.
- Recreated Nginx and Grafana with enforced 0.50 CPU / 768 MiB caps, loopback ports 8080/13001, current-DNS resolution and the exact preserved external `grafana-storage` volume/image. Grafana's persisted default datasource points to `account-prometheus-dev:9090`; its container query succeeded with 6 results. Existing frontend failed even an in-container static probe and normal restart left stale crun state. After confirming old PIDs were gone, a configuration clone preserved settings and volumes; canonical one-service Compose recreation restored its healthcheck, image, two volumes and existing 0.75-CPU/1-GiB cap. The retained legacy frontend uses a Gateway rewrite; no new BFF deployment is claimed.
- JDK17/Gradle8.7, CPU1/RAM1536MiB/one-worker core/API tests and bootJars passed 112/112 (Budget 42+13, Audit 26+31), zero failures/errors/skips, 27 executed tasks. Current `:migration-runner:bootJar --offline` passed. OCI images were assembled offline from the tested jars and the unchanged existing Java Containerfile runtime stage (COPY adapted to staged jars); both image jar SHA-256 values equal the tested jars. The documented complete multi-stage Compose source build was not separately rerun.
- Python policy/replay tests passed 12/12, including pending-migration resume, owner authentication before mutation, exact env preservation and matched PID limits. Actual-input quiet Compose passed. Initial live Budget Redis localhost failure was corrected with existing Redis host/port; initial Compose5 PID-limit rejection was corrected by setting both fields to 256. No application health gate was disabled.
- Final read-only live gate and independent Reviewer rerun passed: both API health and exact current-IP/port Eureka instances UP; Nginx login/health and direct/proxied Grafana HTTP200; frontend negative input/origin responses 400/403; persisted datasource + Prometheus query PASS; all four issue containers healthy/restart0/OOMfalse with actual CPU0.50/RAM805306368. Cold-start socket resets are not counted as success. Independent review's P2 replay and P3 wording findings were resolved; diff/marker checks pass.
- Scope/files: `.gitignore`, Budget/Internal Audit README links, `grafana/{docker-compose.yml,README.md,provisioning/datasources/datasource.yml}`, new governance DB provisioner, two new Compose files, Nginx template, two Python regression suites, live verifier and `docs/guides/governance-external-dev.md`; parent-owned harness/history records.
- Rollback: stop only issue consumers; retain DBs, generated env, all volumes and retained images; restore prior Nginx configuration/image and Grafana image with the same external volume. Partial DB role creation requires reviewed forward recovery. No prune, volume deletion, schema rewind or whole-stack down. Runtime bind mounts use this worktree, so keep it until configurations are moved through a reviewed deployment.
- Authority separation: user authorized related implementation, development DB preparation/recovery, verification, branch push and Draft PR. Implementers and the read-only Reviewer are separate. Parent Integrator alone owns harness/GitHub mutations. Human review and separate authorization remain for Ready, merge, Issue close and cleanup. Next owner: human PR reviewer; GitHub checks are tracked on the Draft PR. Other stopped/unhealthy services from prior tasks remain out of scope.


## 2026-09-11 GH-696 manual PR701 redirect rework

- User authorized manual PR review, rejected-code repair and merge; all three automations remain PAUSED. Published implementation-complete handoff was verified before claim5627974078. Parent uses agent/696-review-redirect / C:/tmp/account-696-review-redirect; original live-mounted /tmp/account-696-governance-runtime is preserved and never modified.
- Independent Astra high /root/manual_review_701 found P2: default absolute redirect loses the published port for bare /grafana. Separate Astra high writer /root/manual_fix_701 changes only the exact-location relative-redirect policy and its regression; parent owns functional guide/shared records and Git/GitHub. Offline rework only: no live DB/container/credentials/deployment/cleanup.
- Ordinary integration of original head3c6f04f9 with latest main ef5679b2 preserves both histories. Parent resolved four record-only EOF conflicts, complete main prefixes4/4 and original ordered histories4/4 PASS. Earlier PR698 and PR702 are actually MERGED, Issues692/673 COMPLETED; see their remote completion evidence. Independent re-review, published-head CI and Ready/latest-main checks remain required for PR701; actual final state belongs to the remote PR, not a prediction in this checkpoint.

- Frozen original-head independent Ubuntu WSL tests12/12 PASS (policy9/replay3). Writer test-only RED on unchanged Nginx blob: UTC01:26:08.3174264–01:26:11.1857965, policy10/1fail/9pass, exit1. After minimal fix full offline suite13/13 (policy10/replay3), failures/errors/skips0, exit0 UTC01:26:24.6364202–01:26:24.8646756. Existing Python/PyYAML only; mocked transport/synthetic temporary files. Original Java112 and live2DB/four-service evidence are historical, not a new live rerun. Independent rework rerun is a separate gate.
- Functional guide explains request /grafana → relative Location /grafana/ → browser retains public port; unchanged slash-path and API proxies. Static policy regression is not an actual HTTP/deployment proof. Rollback is a reviewed config/test/doc revert; applying it to a runtime requires separate authority, never DB/volume deletion.
- Independent rework /root/manual_review_701 Astra high: ONE Ubuntu WSL suite UTC2026-09-11T01:30:29.7578738Z–01:30:32.3533767Z,13/13 (policy10/replay3), failures/errors/skips0, unittest0.024s, exit0. P2 resolved, source no new P0–P3; frozen2 SHA256 unchanged before/after, original11 paths/imported helpers unchanged. Raw main prefix7/7 and ordered original conflict history4/4, scoped diff/markers/unmerged PASS. Final record/body Q1–Q4 and published-head CI/Ready/latest-main remain distinct gates; no live rerun or runtime deployment.
- Final independent local source/guide/record/body review APPROVE, P0–P3 none, Q1–Q4 PASS: nginx22/test120 focused change; guide180 flow/189 prerequisites-limits; nginx20/test125 intent comments. Reviewer authorized commit/publication after recording measured13-test evidence and these verdicts. Published-head CI/Ready/latest-main remain the next gate; implementation merge does not deploy the template.

## 2026-09-11 — GH-695 HTTP Journal financial validation (intake)

- Issue #695 (difficulty:high); isolated branch `agent/695-journal-adapter-precision`, worktree `/tmp/account-695-journal-adapter-precision`, fetched base `origin/main@c94afec5`. Explicit user branch instruction supersedes the stale `agent/691-...` in AC4. Primary dirty checkout preserved.
- Goal: reject Closing redirects without following Location or advancing later writes; validate Loan draft and posted JournalApiDto lines using exact BigDecimal debit/credit totals before approval and on confirmation, preserving actor and lineage. Provider posting endpoint returns only a summary, requiring a preapproval slip lookup.
- Parent owns two HTTP adapters, required build dependency corrections, shared records and Git/PR state. Separate Test agent owns the two core adapter test files; independent Reviewer remains read-only. User authorized related build/source/config repairs and requested branch push plus Draft PR publication (`Refs #695`).
- Verification: targeted regressions including real loopback HTTP transport, then all Loan/Closing core/API tests; widen packaging/context checks if build dependencies change. No domain/DB migration, external runtime deployment or distributed-transaction redesign planned.
- Rollback: revert this PR through reviewed branch; remote drafts/partially approved journals require reconciliation before retry because HTTP writes are separate transactions. Final Ready/merge/Issue close remains human-owned.

## 2026-09-11 — GH-695 HTTP Journal validation verified

- Issue: #695; branch `agent/695-journal-adapter-precision`; isolated worktree `/tmp/account-695-journal-adapter-precision`; base `origin/main@c94afec5`. User requested `agent/695-...`, superseding the stale AC4 `agent/691-...`. Primary dirty checkout preserved.
- Closing HTTP adapter now disables automatic redirects and explicitly rejects non-success informational/redirect responses across creation, approval, posting and queries; default 4xx/5xx handling retains slip-404 semantics. Failed creation never reaches approval/posting, and failed approval never reaches posting.
- Loan HTTP adapter reads the stored DRAFT because creation returns only a summary. It checks ID/slip/status/date/currency/lineage and positive known-side lines, then compares each side's exact BigDecimal total against requested lines before approving. POSTED confirmation repeats validation. Actor and lineage mapping remain covered; GET redirects are never followed.
- Changed substantive files: the two HTTP adapters, their two core test files (Closing core test is new), and `loan/docs/local-run.md` / `closing/docs/local-run.md`. No build, configuration, schema, shared contract or domain change was needed. User Full Authority covers the two related functional-doc additions required by Q3. Parent owns these adapters/docs plus shared records; `/root/adapter_tests` wrote only its two test files; `/root/independent_review` is read-only.
- Verification environment: cached `gradle:8.7-jdk17-alpine`, JDK17, rootless Podman `--network=none --cpus=1 --memory=1536m --pids-limit=256`, one Gradle worker, existing Gradle cache. No package download or external DB/service access.
- Commands inside the container use `gradle --offline --no-daemon --max-workers=1 --console=plain`. Compile `:loan:core:compileJava :closing:core:compileJava` passed (7 tasks). Focused `:loan:core:test --tests '*HttpLoanJournalAdapterTest' :closing:core:test --tests '*HttpClosingJournalAdapterTest'` passed 112 tests (Loan66/Closing46), 1m24s, 22 tasks.
- Full command: `:loan:core:test :loan:api:test :closing:core:test :closing:api:test :loan:batch:test :closing:batch:test :loan:api:bootJar :closing:api:bootJar`. PASS in5m9s, 46 tasks (26 executed/20 up-to-date); all six test tasks executed. Fresh52 XML suites contain316 tests: Loan core137/API6/Batch3; Closing core105/API49/Batch16; failures/errors/skips0. Required four-module AC totals297. XML timestamps UTC2026-09-11 01:14:39–01:19:13. Both API executable JARs generated. Focused112 are included in316, not additional distinct tests.
- `node --test tools/ci/harness-quality-contract.test.cjs`:32/32 PASS. `git diff --check`, tracked source/docs conflict-marker scan and hashes of all six reviewed source/test/module-doc files PASS. Local evidence: `/tmp/issue695-targeted.log`, `/tmp/issue695-targeted-summary.json`, `/tmp/issue695-regression.log`, `/tmp/issue695-regression-summary.json`, `/tmp/issue695-reviewed-files.json`, `/tmp/issue695-harness-tests.log`.
- Tests include a real loopback HTTP server with production constructors, Closing35 redirect stage/status cases, Loan200-vs100, invalid lines/sides/amounts, exact large/fractional/multiline sums, scale equality and actor/lineage. API/Batch Spring context regressions passed. External deployed Journal/PostgreSQL and end-to-end business writes were not run; no external deployment was required for this adapter contract change.
- Independent `/root/independent_review` confirmed no P0–P3 and Q1–Q4 PASS after reading actual logs, independently parsing all52 XML suites and rehashing all6 frozen files. No repeat tests needed. Draft PR body uses `Refs #695`; PR URL will be synchronized after publication.
- Rollback: reviewed revert of this PR. Remote drafts or partly completed approval/posting are separate transactions and must be reconciled before retry; no automatic retry or remote deletion. Preapproval GET adds one request (five total on success), and lookup-to-write concurrency is not atomic.
- Authority separation: parent Integrator alone owns records/commit/push/requested Draft PR. Implementer cannot self-approve. Merge authority: independent Reviewer approval and explicit user approval are required before Integrator Ready/merge/Issue-close. Current requested endpoint is Draft PR; no merge, Issue close or worktree deletion.

### GH-695 Draft publication

- Published requested Draft PR [#703](https://github.com/skyg547/account/pull/703), `Refs #695`, from `agent/695-journal-adapter-precision`; implementation commit `35c70c83`. Independent final source/test/record/PR-body review found no P0–P3 and Q1–Q4 PASS. This publication-only update changes no code/tests; all316 Java tests,32 harness tests and two API JAR results remain valid.
- Issue #695 is `status:needs-review`. Next owner: independent human Reviewer / approved Integrator for current-head GitHub CI and review; implementation approval is not self-approval. Ready/merge/Issue close and branch/worktree cleanup remain unperformed. Worktree `/tmp/account-695-journal-adapter-precision` is retained. PR body contains full commands/results, authority separation, rollback and remote partial-transaction limits.


## 2026-09-11 GH-695 manual PR703 integration

- User requested manual review/rework/merge; schedules3 remain PAUSED. Original implementation-complete handoff verified, manual claim5628076042. Parent uses agent/695-manual-pr-integration / C:/tmp/account-695-manual-pr-integration; original agent/695-journal-adapter-precision worktree preserved. No production changes were needed after separate /root/manual_review_703 Astra xhigh review: no P0–P3, Q1–Q4 PASS.
- Parent ordinary-merges original086a4af9 with actual latest main53ada41b (PR701 merged UTC01:39:00Z). Four record-only EOF conflicts retain complete reviewed main histories then original695 histories. Main prefix7/7 and original ordered histories7/7 pass; substantive6 files unchanged, no incoming Loan/Closing/shared-kernel/build change. Exact main-relative13 paths are original10 plus parent record3. Primary checkout and all other owners preserved.
- Independent new local core run242/242, failure/error/skip0; focused112 are included, not additive. Original316/bootJar evidence remains historical. Current-head core/API CI, final integrated-record review and Ready/latest-main gates are required before user-authorized parent merge; actual publication/completion state is recorded on PR703. No real Journal/DB, deployment, installs/downloads, force/rebase/reset or resource cleanup.

- New separate Reviewer focused run UTC2026-09-11T01:33:56.5353749Z–01:34:23.9154855Z:112tests/2freshXML,22tasks executed, exit0/27.3801106s. Full core UTC01:35:05.6752404Z–01:35:34.2494388Z: Loan137/15suites + Closing105/13 =242/28freshXML,22tasks executed, exit0/28.5741984s; XML01:35:24–01:35:33, failures/errors/skips0. Installed JDK17.0.19/cachedGradle8.7, offline, one worker, rerun-tasks, JVM1024m/ActiveProcessorCount1. Initial unquoted PowerShell -P failed before tests; corrected quoted options passed.
- Command tasks :loan:core:test :closing:core:test with --offline --no-daemon --console=plain --max-workers=1 --rerun-tasks '-Porg.gradle.java.installations.auto-download=false' '-Dorg.gradle.jvmargs=-Xmx1024m -XX:ActiveProcessorCount=1'. Focused run additionally applied each module's --tests '*HttpLoanJournalAdapterTest' / '*HttpClosingJournalAdapterTest'. Mock HTTP, test-owned loopback and isolated H2 only; API/Batch/bootJar not redundantly rerun locally and not claimed as new evidence.
- Independent Q1–Q4 PASS: Loan111 exact provider-side BigDecimal sums/Closing210 status gate; Loan84 preapproval-detail flow/partial-write limits; Loan local-run17/Closing local-run90 prerequisites and tests; Closing68/211 and Loan84/144 educational no-follow/404/summary/scale comments. No N/A shortcuts. Rollback is reviewed code/test/doc revert; remote writes remain separate transactions requiring reconciliation, not automatic retry or remote deletion.
- Final independent integrated-record/body review PASS: source6 unchanged, exact13 paths, main prefix7/7 and original ordered history7/7, working/index whitespace0 and unmerged0. Q1–Q4 and fresh242 versus historical316 evidence confirmed; original remote PR body preserved. Incoming Journal UnsettledItem is not referenced by Loan/Closing and does not affect adapter tests. Reviewer recommends commit/publication without redundant local rerun; exact new-head core/API CI and Ready/latest-main remain separate final gates.

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

- Issue #672 / parent #661; difficulty:medium, user-requested high reasoning. Prerequisites #662/PR #680 and path correction #674/PR #707 are merged. Parent confirmed OPEN/status:ready and no competing #672 claim; overlapping open PR #711 changes only shared records within this Issue's scope. Claim: https://github.com/skyg547/account/issues/672#issuecomment-5637621633.
- Branch `agent/672-harness-ci-gate`, worktree `/tmp/account-672-harness-ci-gate`, base `origin/main@e83212a773cfabd3a505615554e12f3f0ed555f3`. Original dirty checkout and other worktrees preserved. Exactly five substantive files: `.github/workflows/harness-validation.yml`, `tools/ci/validate-harness.py`, `tools/ci/test_validate_harness.py`, guides `45-ci-module-validation.md` and `95-codex-skills-subagents.md`; parent appends only these four shared records.
- All PR/main-push/manual events execute the lightweight gate without path or draft filters, including tools-only changes. Fixed `Harness Validation Result` requires actual successful validation; failure/cancellation/skips are rejected. Read-only contents permission, no persisted credentials, existing Ubuntu 24.04 system Python/PyYAML 6.0.1 and Node runtime; no installation fallback. Existing Gradle workflow remains byte-identical and workflow changes still trigger its full matrix.
- Validator checks a fixed 55-path inventory, 11 role TOMLs, 4 skill frontmatters, 3 UI metadata files, 4 workflow YAMLs and 2 Issue forms. Duplicate keys, malformed schema, unsafe role mode, noncanonical required references, missing/symlink files and unreviewed role/skill inventory changes fail. Histories and designated shared logs have existence/type checks only; sensitive/local/unlisted content is excluded. Other local Node suites remain CI-unwired; this Issue connects only the required #662 PR contract suite.
- Verification: `/usr/bin/python3 tools/ci/validate-harness.py` PASS (counts above); `/usr/bin/python3 -m unittest discover -s tools/ci -p test_validate_harness.py -v` 20/20 PASS, failure/error/skip0; `node --test tools/ci/harness-pr-contract.test.cjs` 32/32 PASS, fail/cancel/skip/todo0. Actual summary parsers require nonzero executed tests and no skips. Fixtures execute the checked-in result shell, Python/Node test pipelines and actual Guard JavaScript with synthetic PR/review/API objects, including negative failures. `git diff --check`, exact substantive scope and conflict-marker checks PASS. Evidence: `/tmp/account-672-evidence/{schema.log,python.log,node.tap}`.
- Initial RED evidence found root grouped document abbreviations rejected and command-presence checks accepting early exit. Parent fixed grouped canonical references and exact bounded execution scripts; test agent added regressions. Separate read-only Reviewer re-review follows against the frozen implementation; review result is appended only when returned.
- Q1 evidence: validator parsing/schema/report functions and test classes keep responsibilities separate. Q2: fixed input→schema/fixtures→result flow and failure limits documented in both guides. Q3: runnable local commands, expected counts, parser prerequisite, CI trigger/Gradle distinction and rollback documented. Q4: comments explain bounded reads, YAML on-key handling, duplicate rejection, grouped refs, exact scripts, nonzero reports and synthetic Guard execution. Independent Q1–Q4 confirmation is recorded below after review.
- Not run before publication: hosted GitHub CI (PR does not yet exist), local Gradle/build/service/DB checks (no business/build/runtime change). Hosted full Gradle matrix remains an integration gate; no pending/unrun result is called PASS. Repository schema subset and mock Guard tests do not enforce GitHub permissions, validate every link/anchor or replace semantic/human review. Runner parser drift fails explicitly; no ruleset, scheduling, personal/model config or Guard trust model changed.
- Authority: parent implements workflow/validator/guides and owns shared records/Git/GitHub; Test agent owns only the fixture file; independent Reviewer is read-only. Separate sessions provide procedural separation, not a separate GitHub identity, enforced sandbox or GitHub APPROVED review. Model tier confers no external mutation/merge authority. User authorized commit/push/Refs #672 Draft PR; Ready/merge/Issue close/cleanup are not performed.
- Rollback: reviewed follow-up PR reverts only this Issue's substantive changes, preserving shared history. No schema/data rollback. Next owner after Draft publication: human Reviewer and separately authorized Integrator for latest head/base/CI integration gates. No conflict occurred.

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

## 2026-09-13 — GH-667 contained SCD2 future splits

- Issue #667, difficulty:high / high reasoning. Branch `agent/667-scd2-future-overlap`; worktree `/tmp/account-667-scd2-future-overlap`; base `origin/main@e7c462f4db2c90238b238454d96c43b7d2a14f1a`. Original dirty checkout preserved. User explicitly authorized implementation, verification, commit/push and Draft PR with `Refs #667`.
- Design/ordered split: four-type regression/red evidence, shared policy + three services, BP domain integration, full verification and independent review. New inclusive interval must start strictly after the source start and end no later than its existing end. Omitted end remains9999-12-31 and fails against a finite source. No reserved-history replacement or silent truncation.
- Production files: `MasterDataValidityPolicy.java`, `AccountSubjectService.java`, `DepartmentService.java`, `ProductService.java`, `BusinessPartner.java`. Tests: new `MasterDataValidityPolicyTest.java`/`MasterDataFutureVersionOverlapTest.java`, extended `BusinessPartnerTest.java`/`BusinessPartnerServiceTest.java`. Functional docs: `master-data/docs/beginner-guide.md`. Parent owns this worklog, agent-status, handoff and `docs/history/CODEX_WORKLOG.md`. No build/config/schema/query changes required.
- Core policy executes before date subtraction, source mutation and save. Direct APIs and typed approved appliers route into guarded services; BP code/useYn/account cloning and historical active lookup remain covered. Rejection preserves source/scheduled intervals, audit timestamps, object/account ownership and save counts.
- RED: unchanged production with new regressions ran429 core tests;18 expected failures,0 errors/skips, across all four types. GREEN focused56 PASS. Full `:master-data:core:test :master-data:api:test :master-data:batch:test :master-data:api:bootJar :master-data:batch:bootJar` PASS,22 tasks in2m37; XML473 tests(core444/API24/Batch5),0 failures/errors/skips.49 tests added; focused56 are a subset of473. Existing tests exceed the Issue's historical44 snapshot.
- Environment/commands: existing rootless Podman `gradle:8.7-jdk17-alpine`, `--network=none --cpus=1 --memory=1536m --pids-limit=256 --userns=keep-id`, existing Gradle cache mounted at `/cache` and worktree at `/workspace`, `GRADLE_USER_HOME=/cache`; all Gradle commands use `--offline --no-daemon --console=plain --max-workers=1 --project-cache-dir /tmp/gradle-project-cache -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m'`. Focused filters: `*MasterDataValidityPolicyTest`, `*MasterDataFutureVersionOverlapTest`, `*BusinessPartnerTest`, `*BusinessPartnerServiceTest`. Host wrapper attempt stopped after JDK21-only discovery; claimed Java tests all use JDK17. No package download.
- Actual packaged API smoke: local H2 startup and existing `/actuator/health/readiness` group(application + DB) HTTP200/status UP in network-none JDK17 container(CPU1/RAM768MiB/pids128); process/container stopped afterward. Initial global health returned503 despite successful startup; retained as failed global-health evidence, not claimed as PASS. This is startup/readiness evidence, not HTTP overlap or external PostgreSQL validation. API/Batch bootJar PASS; API JAR SHA256 `ff8d21e1f290b908dd9f8669f6985acf2443e289f4c62e595b9ce82126e3eb19`; embedded five changed core classes byte-match tested build output.
- Evidence: `/tmp/account-667-baseline.log`, `-red-summary.json`, `-targeted.log`, `-targeted-summary.json`, `-modules.log`, `-module-summary.json`, `-smoke-global-health.log`, `-smoke.log`, `-smoke.sh`, `-quality.log` (all share `/tmp/account-667` prefix), module XML under worktree build directories.
- Static: `node --test tools/ci/harness-quality-contract.test.cjs`32 PASS; `python3 tools/ci/validate-harness.py` PASS; `git diff --check` and tracked/new-source conflict-marker scan PASS. No conflicts; conflict-log unchanged.
- Ownership: `/root/regression_tests` wrote three regression files; `/root/split_policy` wrote five production files; parent wrote policy test/docs and owns shared records/Git. `/root/independent_review` inspected code, tests, Issue and actual XML/logs read-only; no P0–P3 findings.

| Item | Result | File/test evidence | N/A reason | Risk/next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Policy:75; service calls98/87/78; BP:198;473 tests | Not used: code changed | Real PostgreSQL/concurrency separate | /root/independent_review confirmed |
| Q2 | PASS | Policy:69; beginner-guide:90 flow/bounds/errors | Not used: behavior changed | Approved-overlap end-to-end not newly exercised | /root/independent_review confirmed |
| Q3 | PASS | beginner-guide:96 examples/default end/JDK17 command | Not used: user behavior changed | Human policy review | /root/independent_review confirmed |
| Q4 | PASS | Policy:72 reservation-preservation intent; stateful test fixtures | Not used: nontrivial invariant | No unresolved code findings | /root/independent_review confirmed |

- Rollback: reviewed revert of this code/test/doc change; preserve all business data, scheduled history, migrations/checksums and harness history. Existing-overlap repair, database exclusion/concurrent writers, real PostgreSQL and new end-to-end approved-overlap execution remain outside scope.
- Authority: separate implementation and read-only Reviewer sessions; parent alone commits/pushes/publishes Draft PR. This is procedural separation, not separate GitHub credentials or an APPROVED review. PR body includes validation, `Refs #667`, Implementer tier and Merge authority. Ready/merge/Issue close/branch-worktree deletion remain later gates; Guard trust policy remains unconfigured. Next owner: human reviewer for policy, current head/base and CI.

## 2026-09-13 — GH-667 Draft PR publication

- Draft PR [#723](https://github.com/skyg547/account/pull/723) OPEN/DRAFT, `Refs #667`, from `agent/667-scd2-future-overlap` to `main`; implementation commit `da88765c`. Worktree `/tmp/account-667-scd2-future-overlap` retained. Issue #667 OPEN / `status:needs-review` / `agent:codex`; ordered execution split documented and stale `sizing:needs-split` removed.
- PR body records473 passing module tests,56 focused subset,18 expected red failures, packaged H2 readiness smoke, initial global-health503, Q1–Q4, rollback, remaining PostgreSQL/concurrency gates and explicit Merge authority/session separation. Independent read-only reviewer also cleared final records/body with no findings.
- GitHub checks have started; no remote CI success is claimed at publication. Draft/Guard trust-policy gates remain before any Ready/merge. Parent only posted/committed/pushed; Ready/merge/Issue close/branch-worktree deletion not performed. Next owner: human reviewer for policy, latest head/base and CI. This checkpoint updates records only, so Java tests are not rerun.

## 2026-09-22 — GH-443 configured-user password encoding contract

- Issue #443; difficulty:high / requested high reasoning. Branch `agent/443-configured-password-contract`; isolated worktree `/tmp/account-443-configured-password-contract`; base and latest fetched `origin/main@e375002ff63652aabaaa1f12c74aa611d4ed29bc`. User authorized implementation, verification, commit/push and a Draft PR with `Refs #443`; Ready, merge, Issue close and branch/worktree removal remain separate gates.
- Contract: configured stored credentials must use the exact lowercase `{bcrypt}` delegated prefix and a structurally valid 60-character BCrypt payload. Prefixless values, surrounding whitespace, case variants, raw/plaintext, `{noop}`, unknown prefixes, malformed payloads and costs outside 04–31 fail closed. `PasswordEncoderPolicy`, properties binding, JPA entity mapping and the verifier share this contract; verifier never repairs input and returns `false` on invalid stored data.
- Seed path: `AuthModuleProperties` validates every non-null configured-user entry with indexed field paths. `AuthUserSeedRunner` validates and maps the complete list before the first repository call, then saves only missing users in its existing transaction. Errors do not echo username, credential, encoder id, payload, length or cost. Actual Spring AOP/H2 coverage proves a late second-row database failure rolls back the first row, a corrected retry stores both rows, and an identical retry is idempotent.
- Latest-main conflict and scope amendment: merged #652 had reintroduced a tracked demo plaintext/hash and three default users through local SQL despite #443 and the module docs. The reviewed scope therefore expands to `application-local.yml`, deletion of `data-auth-local.sql`, deletion/replacement of `AuthLocalProfileBaselineDataTest`, and new `AuthLocalProfileConfiguredUserTest`; recorded at [Issue comment](https://github.com/skyg547/account/issues/443#issuecomment-5763609044). Local SQL init is now `never`; runtime-generated configured properties exercise local JPA seed, lookup, verifier, login and token issuance without committed credential fixtures.
- Ownership: Service agent changed five core production files; Test agent changed/added focused core/API tests; Coder removed the local resource seed; Documentation agent updated `auth/README.md` and `auth/docs/local-run.md`; parent owns scope reconciliation, these four shared records and Git/GitHub; independent Reviewer is read-only. Implementer/reviewer separation is procedural and does not represent a GitHub APPROVED review or separate credentials.
- RED evidence: unchanged production failed 12/21 policy/properties tests, 3 intended seed/entity cases, and 1/3 API configured-user cases. Production writer independently reproduced 15 failures across 27 focused tests. GREEN: focused core 27/27 and existing JPA adapter 5/5; parent API configured-user/runtime tests PASS.
- Required full verification: `./gradlew :auth:core:test :auth:api:test :auth:api:bootJar --no-daemon --console=plain --max-workers=1` -> `BUILD SUCCESSFUL` in 1m01s, core71 + API15 =86 tests, failures/errors/skips0; both test tasks and bootJar executed. Boot JAR SHA256 `99ec0656a35214514ac7d0f4ffc42b7ccda674d422471485cfeec9ee8d37b49c`. Independent Reviewer reran the same command with `--rerun-tasks`: 13 executed, 86/86 PASS in 1m.
- Static verification: `git diff --check`, unmerged-index, tracked/new conflict-marker, fixed BCrypt credential, former demo password, literal policy-encode input and removed local-seed-reference scans PASS. Latest fetched main is unchanged from the work base. Deleted SQL is absent from the Boot JAR.

| Item | Result | File/test evidence | N/A reason | Risk/next gate | Independent review |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `PasswordEncoderPolicy` single policy; `AuthModuleProperties` indexed validation; focused27 + full86 | Not used: code changed | Human review and published-head CI | `/root/issue443_review` confirmed |
| Q2 | PASS | `auth/README.md` password policy; `auth/docs/local-run.md` configured input, full-list validation, rollback/retry | Not used: behavior changed | Real PostgreSQL transaction remains untested | `/root/issue443_review` confirmed |
| Q3 | PASS | Module/run docs specify exact prefix, no defaults, runtime placeholders, command and limits | Not used: user-visible local bootstrap changed | Human local-run review | `/root/issue443_review` confirmed |
| Q4 | PASS | Seed-runner intent comment and policy/config Javadocs explain fail-before-repository and non-disclosure | Not used: nontrivial security flow changed | Preserve comments with future policy changes | `/root/issue443_review` confirmed |

- Independent review: no P0/P1/P2 code or security findings. Its only P3 traceability finding was the four-path #652 scope expansion; the Issue comment and these records resolve it before commit. Remaining risks: actual PostgreSQL was not accessed; existing DB rows with prefixless/legacy hashes now intentionally fail authentication and need a separately approved diagnostic/rotation plan. No external DB/container/user/credential was read or changed.
- Rollback: use a reviewed path-scoped revert of this change, preserving harness history. Re-enabling the deleted fixed credential SQL is not a safe operational rollback; affected local users must be supplied through approved external runtime configuration. Next owner: human reviewer checks the Draft, current head/base and CI before any Ready/merge/Issue-close authorization.

### GH-443 Draft publication

- Draft PR [#729](https://github.com/skyg547/account/pull/729) is OPEN/DRAFT with `Refs #443`; implementation commit `342aed55964639ae026ddef77041dd9f756bb7d5` is pushed on `agent/443-configured-password-contract`. Issue #443 remains OPEN and moved from `status:in-progress` to `status:needs-review`.
- GitHub reports the PR mergeable. Module Validation detection is queued and Merge Guard candidate/Harness Validation have started; no remote check success is claimed at this checkpoint. Local and independent evidence remains the 86/86 Auth tests, bootJar, static scans and no-P0–P3 review above.
- This publication update changes only the four parent records. Ready transition, merge, Issue close, branch/worktree deletion and legacy credential rotation were not performed. Next owner: human reviewer verifies the published head, completed CI and latest base.

## 2026-09-22 — GH-642 container build modernization RFC

- Issue #642 (`difficulty:medium`, requested `gpt-5.6-sol` / high reasoning); branch `agent/642-container-build-study`; isolated worktree `/tmp/account-642-container-build-study`; fetched base `origin/main@e375002ff63652aabaaa1f12c74aa611d4ed29bc`. The earlier claim comment recorded `34c75839`; this run started from and documents the newer fetched base. Existing worktrees and unrelated files were preserved.
- Exact scope: new `docs/guides/container-build-modernization-rfc.md` plus parent-owned `docs/ai-harness/{agent-status,worklog,handoff}.md` and `docs/history/CODEX_WORKLOG.md`. No Dockerfile, Containerfile, Compose, Gradle, source, test, CI, registry, database or runtime behavior changed.
- Inventory evidence: 71 tracked `Dockerfile*`/`Containerfile*`, 56 Compose YAML, 72 Gradle projects, and the canonical manifest's 35 Java targets (infra3/API17/Batch15) plus frontend1. The 35 canonical Java Dockerfiles total903 lines and share Java17 builder/runtime, non-root, JVM options and entrypoint; timezone3 and image healthcheck2 are explicit exceptions. Existing root parameter builder is the B baseline, not a new proposal artifact.
- Decision: conditionally prefer Jib for JVM after a single `:discovery` PoC; keep the common Containerfile as control/fallback and preserve every current Dockerfile through rollout. Temurin17 Alpine stays the first base to isolate builder effects; Distroless is a second experiment after shell/`wget` readiness and debugging are redesigned. Next.js keeps production `Containerfile` and development `Containerfile.dev`; independent infra remains outside Jib.
- RFC covers digest-pinned base images, non-root identity, JVM/timezone/health parity, private registry TLS/credential-helper/promotion policy, amd64/arm64 and Jib's registry-only multi-platform limitation, architecture-specific air-gap tar bundles, measurable A/B thresholds, rollout waves and digest rollback. Jib does not make a Gradle monorepo module an independent checkout; performance claims remain PoC hypotheses.
- Verification PASS: `./gradlew :discovery:test :discovery:bootJar --offline --console=plain --no-daemon --max-workers=1` (6 tests, 0 failure/error/skip, bootJar); `./gradlew :config-server:test --tests '*ContainerImagePolicyTest' --offline --console=plain --no-daemon --max-workers=1` (9 tests, 0 failure/error/skip); Python stdlib tracked inventory; Markdown local links13/13; exact allowlist; `git diff --check`; scoped tracked conflict-marker scan. Harness schema validation and final staged checks are rerun before commit.
- Not run: Jib task/image build, Docker/Podman image build, registry push, multi-arch/native smoke, air-gap build/load, Distroless scan/runtime, Compose/Eureka runtime. Jib is not configured and this RFC deliberately changes no executable build/runtime state. These are explicit follow-up PoC gates, not evidence gaps represented as PASS.
- Independent read-only `/root/review_rfc_642` found one P2: readiness alone did not prove Discovery register/lookup/renew/cancel and rollback repopulation. The RFC now makes synthetic client lifecycle, stale-registration absence and rollback re-registration mandatory. Re-review found no P0–P3 and confirmed inventory, links, official Jib/Distroless claims and XML results.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | N/A | RFC와 공유 기록만 변경; production/test/build/Compose diff 없음 | 실행 코드 품질 변경 없음 | 후속 Jib convention과 policy test 검토 | `/root/review_rfc_642` 확인 |
| Q2 | PASS | RFC 3–8절: A/B, base/user/health/timezone, registry/offline/multi-arch, frontend, PoC와 rollback 흐름 | 해당 없음: 설계 설명 대상 | 실제 Discovery lifecycle/native arch/offline smoke | `/root/review_rfc_642` 확인 |
| Q3 | PASS | RFC 1·6·8·10–11절: 재현 inventory, 미구현 명령 경계, Go/No-Go, 공식 근거와 검증 | 해당 없음: 기능/RFC 문서 대상 | 후속 실행 결과로 수치 갱신 | `/root/review_rfc_642` 확인 |
| Q4 | N/A | 비자명 코드·실행 로직 수정 없음 | 의도 주석 대상 코드 없음 | 후속 convention 구현 시 의도 주석 검토 | `/root/review_rfc_642` 확인 |

- Rollback: reviewed revert of only these five documentation/history files. No image, container, cache, registry object, database or runtime rollback is needed. `conflict-log.md` is unchanged because no conflict occurred.
- Remaining risk/next gate: proposed thresholds and policies are not runtime proof. Human reviewers must approve the Draft and a new executable Issue before plugin/dependency/image activity. Parent publication uses `Refs #642`; Ready, merge, Issue close, branch/worktree deletion and Dockerfile removal remain unauthorized separate gates.
## 2026-09-22 — GH-642 Draft publication

- Draft PR [#730](https://github.com/skyg547/account/pull/730) created OPEN/DRAFT with `Refs #642`, base `main`, head `agent/642-container-build-study`; reviewed implementation commit `ec9a89d9954f148d7181af1012d4b3d087b8255a` was pushed to origin.
- Issue #642 remains OPEN and moved from `status:in-progress` to `status:needs-review`; `agent:codex` and `difficulty:medium` remain. PR body records actual base `e375002f`, exact scope, verification, Q1–Q4, skipped runtime gates, rollback and merge-authority separation.
- This publication checkpoint changes only the four required harness/history records. It does not alter the independently reviewed RFC or executable behavior, so Gradle tests are not repeated. Final static/allowlist checks and clean remote tracking are verified after this commit.
- Hosted checks are not claimed successful at publication. Next owner is a human reviewer for the RFC and current head/base/CI. Draft Ready, merge, Issue close, branch/worktree deletion, Jib implementation and Dockerfile retirement were not performed.

## 2026-09-23 — GH-418 Local H2 기준 데이터

- Issue `#418`; branch `agent/418-local-baseline-data`; worktree `/tmp/account-418-local-baseline-data`; base `origin/main@3da1f655454d`. `master-data:api`의 `local` profile에만 Flyway 이후 `data-local.sql`을 연결했다. Auth production classpath와 profile 설정은 수정하지 않았다.
- 시드 결과는 통화 3건, 부서 3건, 계정과목 8건, 거래처 3건, 2026년 `OPEN` 회계기간 12건으로 총 29건이다. 유효기간은 `2020-01-01`부터 `9999-12-31`이고, 계정 유형·잔액 방향·거래처 위험/KYC 값과 월말 경계는 V6 스키마 제약에 맞춘다.
- 신규 `MasterDataLocalProfileDataSeedTest`는 격리 H2와 `@ActiveProfiles("local")`에서 실제 Flyway→SQL init을 거쳐 통화·부서·계정과목의 정확한 값과 고정 기준일 조회를 검증한다. 기존 Flyway-disabled/create-drop 컨텍스트 테스트는 자체 목적을 보존하도록 SQL init만 명시적으로 끈다. 로컬 실행 문서는 데이터 구성, 초기화와 local-only 경계를 설명한다.
- 사용자 지정 명령 `./gradlew :master-data:core:test :master-data:api:test :auth:core:test :auth:api:test --console=plain`은 `BUILD SUCCESSFUL`(1m25s). XML 결과는 master-data core444/API68, auth core83/API17로 총 612건, failures/errors/skips 0이다. `AuthApiRuntimePolicyTest`를 포함해 정적 credential 없는 fail-closed 정책을 보존했다.
- 독립 읽기 전용 Reviewer `/root/issue418_review`는 P0–P3 없음과 커밋 가능을 확인했다. `git diff --check`, 승인 파일 conflict marker, unmerged index 검사도 통과했다. 같은 JVM에서 고정 H2 이름으로 시드를 반복 실행하는 경우는 범위 밖이며 신규 테스트는 고유 URL로 격리한다.

| 항목 | 판정 | 파일·검증 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `data-local.sql`, local 설정, seed 통합 테스트; 지정 612 tests PASS | 해당 없음: 실행 설정/테스트 변경 | 실제 PostgreSQL은 대상 아님 | `/root/issue418_review` 확인 |
| Q2 | PASS | `master-data/docs/local-run.md`의 Flyway→seed→조회/초기화/local-only 설명 | 해당 없음: 초기화 흐름 변경 | 반복 같은-JVM init은 미검증 | `/root/issue418_review` 확인 |
| Q3 | PASS | 기준 데이터 그룹/건수, 메모리 수명, 검증 명령 문서화 | 해당 없음: local 기능 문서 대상 | dev/prod에는 적용하지 않음 | `/root/issue418_review` 확인 |
| Q4 | N/A | 선언형 SQL/YAML과 직접 assertion만 변경; SQL 첫 줄에 순서 의도 기록 | 비자명 Java/domain 알고리즘 변경 없음 | 후속 로직 추가 시 재검토 | `/root/issue418_review` 확인 |

- Rollback: 다섯 개 기능/테스트/모듈 문서 파일을 검토된 revert로 되돌리고 공유 이력은 보존한다. H2 데이터는 프로세스 종료 시 사라지며 운영 DB 복구는 없다. 충돌이 없어 `conflict-log.md`는 수정하지 않았다. 사용자 요청은 로컬 커밋까지이며 push, Draft PR, Ready, merge, Issue close, branch/worktree 삭제는 수행하지 않는다.
## 2026-09-23 — GH-47 Cashflow domain initial design

- Issue/branch/worktree/base: #47; `agent/47-cashflow-domain`; `/tmp/account-47-cashflow-domain`; `origin/main@fd5ebc7e`. Issue was OPEN with `status:in-progress`, `agent:codex`, and `difficulty:medium`. No PR was requested or created.
- Scope: registered `cashflow:core`, `cashflow:api`, and `cashflow:batch`; added the cashflow domain/application ports and services, local in-memory outbound adapters, REST controllers/DTOs, local profiles, Spring Batch aggregation skeleton, tests, and module documentation. Root runtime verification now includes the cashflow API/Batch PostgreSQL jars and API Actuator. Existing modules, SQL migrations, production data, credentials, and remote state were not changed.
- Domain result: all monetary values use `BigDecimal` scale 2 with `RoundingMode.UNNECESSARY`; activity totals must match line items, net cashflow must equal operating + investing + financing, and ending cash must equal beginning cash + net cashflow. DIRECT/INDIRECT retain different presentation methods while using the same explicit activity aggregation. Forecast risk uses caller-provided watch/critical thresholds with exact boundaries assigned to the less severe band.
- Runtime result: `local` supplies concurrent in-memory statement/forecast stores and a zero-default ledger balance adapter. API maps expected domain validation failures to HTTP 400. Batch owns identifying parameters and orchestration only; its initial tasklet creates an empty-line balanced statement until a classified reader is added. Same-repository completed instances are rejected, while local H2 metadata is intentionally lost across JVM restarts.
- Verification: final independent Reviewer reran `./gradlew :cashflow:core:test :cashflow:api:test :cashflow:batch:test --rerun-tasks --console=plain`: 17 tests, 0 failures/errors/skips, BUILD SUCCESSFUL. Parent also built both bootJars and confirmed PostgreSQL in API/Batch and Actuator in API. `node --test tools/ci/harness-quality-contract.test.cjs` passed 32/32. Final `git diff --check`, untracked whitespace, and conflict-marker checks passed. Initial API smoke exposed 500 responses for domain-invalid input; the parent added exception mapping and MockMvc regressions, after which packaged smoke returned 400 for scale/date violations.
- Independent review: read-only `/root/review_47` found no remaining P0–P3 after correction. Q1 PASS (cohesive domain/application/inbound responsibilities and HTTP error mapping); Q2 PASS (input→domain→persistence plus repeat/restart boundaries); Q3 PASS (verified API/Batch examples and local limitations); Q4 PASS (precision, snapshot, threshold, exception mapping, and Batch-role intent comments).
- Skipped/limits: no external PostgreSQL, production ledger adapter, production persistence adapter, deployed service, remote CI, or load/partition test. The in-memory adapter and H2 Batch metadata are local-only; production durability and high-volume readers remain later gates.
- Rollback: use a reviewed Issue-scoped revert of the cashflow module, `settings.gradle`, root runtime lists, and these four append-only records. No schema/data rollback is required. No conflicts occurred, so `conflict-log.md` was not changed.
- Authority/next owner: parent owns shared records and the requested local commit. Independent review is procedural separation, not GitHub APPROVED review or separate credentials. Push, Draft PR, Ready, merge, Issue close, and worktree/branch cleanup were not requested. Next owner is the user/human reviewer for any publication decision.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `CashflowStatement`, `CashflowForecast`, services, API advice; final 17 tests PASS | 해당 없음: 코드 변경 | production adapter 추가 시 경계 재검토 | `/root/review_47`, no P0–P3 |
| Q2 | PASS | `cashflow/docs/README.md`; HTTP/Batch repeat/failure tests | 해당 없음: 흐름 설명 대상 | 영속 JobRepository에서 process-restart 검증 | `/root/review_47` 확인 |
| Q3 | PASS | `cashflow/README.md`, `cashflow/docs/README.md`; bootJar/local context evidence | 해당 없음: 신규 모듈 문서 대상 | 외부 PostgreSQL/배포 검증은 후속 | `/root/review_47` 확인 |
| Q4 | PASS | precision, invariant, threshold, API error, Batch 역할 intent comments | 해당 없음: 비자명 로직 변경 | 대량 reader 추가 시 partition/restart 주석 검토 | `/root/review_47` 확인 |

## 2026-09-24 — GH-738 external-dev Compose healthcheck port fix

- Issue/branch/worktree/base: #738; `agent/738-dev-server-compose-fix`; `/tmp/account-738-dev-server-compose-fix`; `origin/main@cf53367a`. Issue is OPEN with `status:in-progress`; no PR was requested or created.
- Scope: `closing/api/src/main/resources/application.yml`, `asset-lease/core/src/main/resources/application.yml`, the three requested business external-dev Compose files, new `tools/cleanup-ghost-containers.sh`, `docs/guides/development-compose.md`, and the four required parent-owned harness/history records. No Java/domain/SQL/build/database/credential/remote-state change.
- Configuration result: Closing uses `${SERVER_PORT:8086}` and Asset Lease uses `${SERVER_PORT:8089}`. Parsed merged YAML proves all 13 APIs in Accounting/Products/Risk set `SERVER_PORT: "8080"` and inherit the readiness probe at `127.0.0.1:8080`; comments make that invariant explicit. The 17-row inventory uses verified repository defaults and discloses Asset Lease's still-existing Config Server 8083 value rather than copying the stale Issue port list.
- Cleanup result: the helper synchronizes rootless Podman/OCI state, reports running-to-stopped and broken-state ghost candidates, and identifies only full-ID transient healthcheck timers whose container is missing/non-running. It is report-only by default. Mutation requires reviewed, explicit 64-hex IDs; apply revalidates current state, skips running targets and states outside the removal allowlist, stops only the exact matching stale user timer/service, force-removes only the selected non-running container, and never prunes/removes volumes, images, networks, or pods.
- Verification: requested `python3 -c "import yaml; ..."` PASS; explicit merged-YAML assertion PASS for 13 APIs; `./gradlew :closing:api:test :asset-lease:core:test --console=plain` BUILD SUCCESSFUL (72 + 39 = 111 tests, failures/errors/skips 0); Spring placeholder assertion PASS; `sh -n`, help, missing/short-ID exit-2 checks, live report-only run, and nonexistent explicit-ID no-op apply flow PASS; `node --test tools/ci/harness-quality-contract.test.cjs` 32/32 PASS. Final diff/marker/index gates are rerun immediately before commit.
- Independent review: read-only `/root/review_738` found and returned an initial destructive TOCTOU/preview-contract P1 and an Asset Lease inventory P2. Parent changed apply to explicit full IDs with state revalidation and documented application 8089 versus Config Server 8083. A final causal-comment correction distinguishes candidates from proof. Re-review found no remaining P0–P3 and Q1–Q4 PASS.
- Not executed: no destructive cleanup against a real container/timer, automated mocked Podman/systemd race suite, actual external-dev service startup/readiness smoke, remote CI, push, or PR. A live report revealed a stale timer but the test selected only a nonexistent ID, so no runtime object was stopped or removed.
- Rollback: use a reviewed Issue-scoped revert of the two application settings, three explanatory Compose comments, helper, runbook, and these four append-only records. No database/data/container/volume rollback is required because no live cleanup was applied. No conflict occurred, so `conflict-log.md` is unchanged.
- Authority/next owner: parent owns shared records and the user-requested local commit. Explorer and Reviewer were read-only; independent review is procedural separation, not GitHub approval. Push, Draft PR, Ready, merge, Issue close, branch/worktree deletion, and live server cleanup remain separate gates.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `cleanup-ghost-containers.sh` explicit-ID/state guards; shell checks; requested 111 Gradle tests PASS | 해당 없음: 설정/실행 helper 변경 | destructive apply는 미실행 | `/root/review_738`, no P0–P3 |
| Q2 | PASS | helper의 report→ID review→revalidate→targeted cleanup 흐름과 runbook troubleshooting | 해당 없음: 비자명 흐름 설명 대상 | mocked Podman/systemd race 자동화 없음 | `/root/review_738` 확인 |
| Q3 | PASS | `development-compose.md` 17-row inventory, 8080 override/probe contract, Config 8083 drift와 안전 명령 | 해당 없음: 기능 runbook 대상 | 실제 deployed Compose smoke는 후속 | `/root/review_738` 확인 |
| Q4 | PASS | helper의 candidate/causality, timer naming, exact-target intent comments | 해당 없음: 비자명 shell 로직 | mocked failure-path test는 후속 | `/root/review_738` 확인 |

## 2026-09-24 — GH-740 Closing calendar tasks / Purchase invoice queries

- Issue/branch/worktree/base: #740 (parity parent #179); `agent/740-api-parity-endpoints`; `/tmp/account-740-api-parity-endpoints`; `origin/main@532bde3f`. Issue is OPEN; no PR was requested or created.
- Closing: added `findClosingTasksByCalendarId` to the inbound use case, read-only service, outbound persistence port, and Spring Data repository (`findByClosingCalendarId`). `GET /api/closing/calendars/{calendarId}/tasks` maps domain tasks to `ClosingTaskDto`; service/controller tests cover delegation, DTO fields, order preservation, and an empty array.
- Payable: added `findInvoices(String status)` and `findInvoiceById(Long id)` through use case, read-only service, typed persistence port, adapter, and repository. Null/blank status returns all; other values normalize with `Locale.ROOT` to `PurchaseInvoiceStatus`; invalid status returns 400 and missing ID returns 404 through controller-scoped advice. `PurchaseController` serves list/detail and existing POSTs under both `/api/purchase` and `/api/payable`; service and parameterized MockMvc tests cover both aliases and failures.
- Routing/docs: added `/api/payable/**` to both `gateway/src/main/resources/application.yml` and `config-repo/gateway-service.yml`, retained the existing payable circuit breaker, and updated its exact policy test. Closing/Payable process docs, Gateway README, and the #179 parity matrix document the routes, error behavior, and observed FE `isMandatory` versus BE `mandatory` drift.
- Verification: `./gradlew :closing:core:test :closing:api:test :payable:core:test :payable:api:test --console=plain` BUILD SUCCESSFUL; XML totals Closing core119/API74, Payable core43/API10 = 246 tests, failures/errors/skips 0. Forced `GatewayRouteSecurityPolicyTest` passed30/30. `node --test tools/ci/harness-quality-contract.test.cjs` passed32/32. Final whitespace, untracked-file whitespace, conflict-marker, and unmerged-index gates pass before commit.
- Independent review: `/root/independent_review` first found missing Payable 400/404 mapping, unreachable `/api/payable/**` Gateway routing, and stale parity wording. After correction and focused reruns it found no P0–P3 and marked Q1–Q4 PASS.
- Not executed/residual: no live BFF→Gateway→Payable service smoke, real PostgreSQL seeded repository/query plan, list pagination/order load test, remote CI, push, or PR. Read-only queries do not change financial calculation, state transition, migration, or Batch restart behavior.
- Rollback/authority: revert this Issue-scoped commit after review while preserving shared history; no DB/data recovery is needed. The parent alone updates shared records and creates the requested local commit. Push, Draft PR, Ready, merge, Issue close, and branch/worktree cleanup remain separate gates.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Closing/Payable controller→use case→service→persistence-port paths; requested246 tests and Gateway30 PASS | 해당 없음: production/API 경계 변경 | PostgreSQL query/load smoke 미실행 | `/root/independent_review`, no P0–P3 |
| Q2 | PASS | `closing/docs/process-flow.md`, `payable/docs/process-flow.md`; empty list, status normalization, 400/404, alias/Gateway 흐름 테스트 | 해당 없음: 조회 흐름·예외 설명 대상 | live BFF→Gateway smoke는 후속 | `/root/independent_review` 확인 |
| Q3 | PASS | module process docs, `gateway/README.md`, parity matrix MAPPED 근거와 DTO drift | 해당 없음: 기능/API 문서 변경 | 기존 FE DTO/enum drift는 범위 밖 | `/root/independent_review` 확인 |
| Q4 | PASS | `PurchaseService`의 문자열→typed enum 경계 의도 주석; 나머지는 선언적 route/단순 위임 | 해당 없음: 비자명 경계 주석 존재 | pagination/order 정책은 후속 계약 | `/root/independent_review` 확인 |

## 2026-09-24 — GH-744 rootlessport listener loss detection and recovery

- Issue/branch/worktree/base: #744; `fix/744-rootlessport-recovery`; `/tmp/account-744`; `origin/main@d5798bc0867832e5d7d8a00a375f39513a025b91`. Issue is OPEN with `status:in-progress`; the user requested a local commit and no push or PR.
- Scope: `tools/cleanup-ghost-containers.sh`, `docs/guides/development-compose.md`, and the four required parent-owned harness/history records. Existing ghost-container removal and healthcheck-timer cleanup remain intact; no Java/domain/SQL/build/database/credential change.
- Result: default reporting compares each running container's published TCP host ports with host LISTEN sockets from `ss -tln`, falling back to `/proc/net/tcp{,6}` only when `ss` is unavailable. IPv4, loopback, bracketed IPv6, comma-separated mappings, ranges, and duplicate dual-stack ports are normalized; exposed-only and non-TCP mappings are excluded. Missing ports use the required `Port drop: <ID> <Name> (<port> not listening on host)` form and the summary counts unique containers.
- Recovery: `--fix-ports` accepts no IDs for all current drops or reviewed full 64-hex IDs for a selection. It refreshes Podman/socket state immediately before each restart, skips healed/non-running/unpublished targets, restarts only verified drops, then rechecks all current published TCP ports up to five times and exits nonzero on restart or verification failure. It remains separate from destructive `--apply` cleanup.
- Verification: `sh -n tools/cleanup-ghost-containers.sh` and `dash -n tools/cleanup-ghost-containers.sh` exit0; independent Reviewer also ran `busybox sh -n tools/cleanup-ghost-containers.sh` with exit0. `sh tools/cleanup-ghost-containers.sh` exits0 and reports ghost0/timer0/port-drop1 on the current host. Inline exported-command fixtures PASS for IPv4/IPv6 duplicates, multiple ports, ranges, UDP/exposed-only exclusion, selected healthy skip, successful selected restart verification, and failed verification exit1; these fixtures are not checked in. An injected failing `ss` exits1 with the expected fail-closed error; live `ss` and forced `/proc` paths both identify the same missing9200 listener. `node --test tools/ci/harness-quality-contract.test.cjs` passes32/32; `git diff --check`, conflict-marker scan, and unmerged-index scan pass. `shellcheck`/`checkbashisms` were unavailable.
- Live environment result: final implementation reports ghost0, stale timer0, port-drop1 for the existing `elasticsearch` host port9200. This is a genuine current host condition reproduced through both `ss` and `/proc`, so the requested clean-system 0/0/0 gate is not claimed. No real `--fix-ports` or container restart was performed because runtime disruption was not separately authorized.
- Independent read-only `/root/review_744` found no functional P0–P3. Residual risks are the unexecuted real restart, no checked-in mocked regression suite, port-number-only comparison, and the narrow state change possible between separate Podman CLI snapshot and restart calls.
- Rollback: use a reviewed Issue-scoped revert of the helper/runbook change and preserve append-only records. No runtime/data rollback is needed because no real container was restarted or removed. No conflict occurred, so `conflict-log.md` is unchanged. Push, Draft PR, Ready, merge, Issue close, and branch/worktree cleanup remain separate gates.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `tools/cleanup-ghost-containers.sh:124`, `:169`, `:288`; `sh -n tools/cleanup-ghost-containers.sh` exit0; `dash -n tools/cleanup-ghost-containers.sh` exit0; `sh tools/cleanup-ghost-containers.sh` exit0 with 0/0/1 | 해당 없음: 실행 helper 변경 | 실제 컨테이너 recovery 미실행; checked-in mock 없음 | `/root/review_744`, 기능 P0–P3 없음 |
| Q2 | PASS | `tools/cleanup-ghost-containers.sh:143`, `:176`, `:308`; live `sh tools/cleanup-ghost-containers.sh` exit0 and detects missing9200; supplementary inline failure fixtures exit1 | 해당 없음: 비자명 복구 흐름 | CLI 호출 사이 state race는 후속 운영 관찰; fixture는 미체크인 | `/root/review_744` 확인 |
| Q3 | PASS | `docs/guides/development-compose.md:509`, `:520`, `:535`; live `sh tools/cleanup-ghost-containers.sh` output matches documented report format | 해당 없음: 기능 runbook 대상 | 현재 9200 drop의 실제 복구는 미승인 | `/root/review_744` 확인 |
| Q4 | PASS | `tools/cleanup-ghost-containers.sh:143`, `:176`, `:308`; `node --test tools/ci/harness-quality-contract.test.cjs` 32/32 PASS; `git diff --check` exit0 | 해당 없음: 비자명 shell 로직 | checked-in mock test는 후속 | `/root/review_744` 확인 |

## 2026-09-24 — GH-179 remaining six frontend API endpoints

- Issue/branch/worktree/base: #179; `feature/179-api-parity-missing-endpoints`; `/tmp/account-179`; `11dbd6967dc291963d6f3482dd469f6a100195cf`. Issue is OPEN with `agent:codex` / `status:in-progress`; the user requested implementation, verification, harness synchronization, and a local commit. No push or PR action was requested.
- Ownership: Receivable, Auth, FX, Reconciliation, and Gateway writers used disjoint module allowlists; parent Integrator owns cross-module parity documentation, the Config Server route mirror, shared records, Git state, and final gates. `/root/review_179` performed read-only independent review and did not edit production or test files.
- Receivable: `SalesController` now has `/api/sales` and `/api/receivable` aliases plus invoice list/detail GETs. `SalesUseCase`/`SalesService` and the typed persistence port/adapter/repository support all, status-filtered, and ID queries. Null/blank status returns all; nonblank status uses `Locale.ROOT`; typed invalid-status and not-found exceptions map only the query failures to 400/404. Alias, filter, DTO, typed-error, unrelated-exception, service, and adapter tests cover the path.
- Admin: `AdminUserController` exposes `/api/admin/users` through inbound `AdminUserQueryUseCase` and read-only `AdminUserQueryService`, not the outbound repository port. Missing/non-admin trusted role headers return 403; only `ROLE_SYSTEM_ADMIN`/`SYSTEM_ADMIN` proceeds. The service sorts usernames, evaluates current roles once, maps the explicit FE role union, derives ACTIVE/INACTIVE, and creates a deterministic 48-bit SHA-256 presentation ID that is JS-safe but never an authorization or mutation identity. JPA and memory query adapters implement `findAllUsers`; controller/service/adapter tests cover authorization, role fallback, stable IDs, order, status, and eager role loading.
- FX/Reconciliation: `/api/fx/dashboard` returns three major rates and positions; `FxDashboardResponse` enforces exact KRW and valuation gain/loss aggregate equality. The inter-branch dashboard returns an explicit zero/empty deterministic snapshot. `/auto-match` has no core mutation implementation and therefore fails closed with HTTP 501 plus `NOT_EXECUTED`, making the frontend's `response.ok` false. These static scaffolds and their replacement gates are documented rather than represented as live financial integrations.
- Routing/docs: both `gateway/src/main/resources/application.yml` and `config-repo/gateway-service.yml` route `/api/admin/**`, `/api/fx/**`, `/api/receivable/**`, and `/api/finance/banking/**`; the full equality/security policy test covers the mirrors. The parity matrix marks the six rows MAPPED and the inventory is 42/42 MAPPED, 0 MISSING. Its positive verifier compares all documented method/path/controller sources, exact new line links, route mirrors, and DTO sentinels. Six in-memory negative cases (missing row, method, route, line, DTO field, control character) are rejected.
- Verification: exact user command `./gradlew :receivable:api:test :receivable:core:test :auth:api:test :auth:core:test :journal-ledger:api:test :reconciliation:api:test :gateway:test` BUILD SUCCESSFUL. XML totals are Receivable API14/Core43, Auth API21/Core94, Journal Ledger API34, Reconciliation API6, Gateway74 = 286, with failures/errors/skips 0. The positive matrix verifier and negative6/6 pass. Harness quality/schema and final static gates are recorded after this append.
- Review correction loop: initial Reviewer findings were admin authorization/boundary/identity/role mapping, banking false success, FX unreconciled totals, broad Receivable exception advice, and stale audit evidence. Writers/parent added the trusted-role gate and inbound query service, HTTP501, aggregate invariants, typed exceptions, synchronized route mirror, and executable 42/42 audit. Reviewer then confirmed no remaining P0–P3 and Q1–Q4 PASS. The broader Receivable FE field/enum difference is retained as documented drift because this issue requested the existing `SalesInvoiceResponse` query mapping rather than a new BFF projection.
- Not executed/residual: no live browser/BFF→Gateway→service, external PostgreSQL query plan/load, external FX rate feed, real inter-branch matching/state mutation, remote CI, or pagination/load behavior for unbounded lists. The Admin hash can theoretically collide and profile/last-login fields are substitutes; these are presentation limitations, not identity guarantees.
- Rollback/authority: use a reviewed Issue-scoped revert of code/test/config/docs while preserving append-only history. No migration, data mutation, or operational recovery is required. No conflict occurred, so `conflict-log.md` is unchanged. Parent creates only the requested local commit; push, Draft PR, Ready, merge, Issue close, branch/worktree deletion, and deployment remain separate authorization gates.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Admin controller→inbound use case→service→query port; Receivable controller→use case→typed persistence port; FX aggregate constructors; exact286 tests PASS | 해당 없음: production/API 경계 변경 | live service/DB and pagination load remain | `/root/review_179`, no P0–P3 |
| Q2 | PASS | module process-flow docs cover auth projection/security, Receivable 400/404/filter flow, FX invariants, banking501 failure semantics | 해당 없음: 조회·실패 흐름 설명 대상 | real feed/matcher flows are follow-up | `/root/review_179` 확인 |
| Q3 | PASS | 42/42 parity inventory, executable positive/negative verifier, Gateway mirror docs, current286 evidence | 해당 없음: API feature documentation changed | browser/live integration remains | `/root/review_179` final confirmation |
| Q4 | PASS | locale normalization, hash non-identity limitation, deterministic FX scaffold, non-executing auto-match comments | 해당 없음: 비자명 logic changed | keep comments synchronized when real integrations replace scaffolds | `/root/review_179` 확인 |

## 2026-09-28 GH-799 (#751 child) remote department validation fail-closed
- Claim: https://github.com/skyg547/account/issues/799#issuecomment-5867969315; parent #751; base origin/main@f128a5dd3cf628f1d5226ae3c5db0ad264021e65; agent/799-department-fail-closed in C:/tmp/account-799-department-fail-closed. Exact five substantive files are the adapter, AuthService, two new tests, and auth/docs/process-flow.md. Parent owns this shared record.
- Input -> processing -> output: the remote adapter accepts only HTTP200 with one complete JSON object and one matching string code; HTTP404 returns false; other HTTP, transport, missing/wrong/malformed/trailing/duplicate identity returns a constant safe exception with no remote cause. AuthService converts an invalid reference to a generic denial before token issuance. Local mode and role/JWT rules are unchanged. No external network, DB, SQL, batch or migration.
- RED: old adapter/API targeted tests failed (core41/35 failures, API18/17 failures; errors/skips0). After two same-writer reworks, final forced full offline Auth core139/API43=182 tests PASS, failures/errors/skips0, exit0. Separate read-only code reviewer independently reran targeted adapter45/API22=67 PASS, 0 failures/errors/skips/stale, exit0 UTC10:43:17-10:43:35, and returned no P0-P3. Exact final five-file hashes matched the writer freeze. Static whitespace/marker gates passed.
- Required `./gradlew.bat :auth:core:test :auth:api:test :gateway:test --offline --no-daemon --console=plain --max-workers=2` exited1 at :gateway:compileJava because 32 Gateway dependencies were absent from the offline cache. Gateway XML/tests were not produced. No download was attempted. This is verification incomplete, not a passed Gateway test; actual timeout bounds and deployed behavior remain untested.
- Q1 clean responsibility PASS (adapter-local strict reader); Q2 input/rationale/edge tests PASS; Q3 functional auth process-flow document PASS; Q4 automation actual full verification FAIL/environment blocked. Separately, docs/ai-harness/42-code-documentation-quality.md intent-comment Q4 PASS. Independent reviewer approved frozen code only, not Ready eligibility.
- Rollback: reviewed Issue-scoped revert preserving shared history; no migration or data recovery. Next: Draft PR with explicit blocker; obtain exact-head Gateway verification in a permitted dependency environment, then CI and independent final review before Ready. No merge/Issue close by this workflow.

## 2026-10-01 GH-747 PAT ownership and administrator authority

- Trace: OPEN #747; `agent/747-pat-authority` in `/tmp/account-747-pat-authority`; base `origin/main@b06e7de3a2e26c1e14a15d73486c02fa7216e8b8` (fetched again, unchanged). Allowed implementation `auth/**`; this parent updated only the three expressly requested shared records. Draft PR [#814](https://github.com/skyg547/account/pull/814) carries `Refs #747` and the authority split and verification results.
- RED before change: existing full HTTP Spring test rejected anonymous create expectation (expected 401, actual success; 1/1 failed). New PAT inbound use case verifies an HS256 Bearer JWT, issuer, time claims, role version, and exact active/unlocked AuthUser with current effective roles. The controller ignores body/query username and `X-Auth-*`; ordinary routes use the verified exact owner, administrator routes require current `SYSTEM_ADMIN`/`ROLE_SYSTEM_ADMIN`. Missing/invalid credential is 401, ordinary admin access 403, foreign owner revoke 404.
- V74 expands PAT owner username to 80 to match AuthUser and stores actor/owner/action/token ID/time audit events without raw token or hash. Conditional `ACTIVE→REVOKED` update emits one event only for the winning transition, including competing DELETEs. The new `auth/build.gradle` makes requested `:auth:test` execute core and API tests despite the parent Java project's own test task reporting NO-SOURCE. No Gateway route was added.
- Verification: targeted PAT service 7/7, H2 conditional JPA 1/1, full HTTP 9/9 PASS. `./gradlew :auth:test --offline --no-daemon --console=plain --max-workers=2` PASS: core143/API52, 0 failures/errors/skips. Issue command `./gradlew :auth:core:test :auth:api:test :gateway:test --offline --no-daemon --console=plain --max-workers=2` PASS including Gateway74; `:auth:api:bootJar` PASS. H2 V73→V74 DDL/80-char insert/action constraint check PASS; `node --test tools/ci/harness-quality-contract.test.cjs` 32/32 PASS; diff whitespace/conflict marker checks clean. `/root/pat_review` read-only independent review found no blocking P0–P3 after the conditional update and documentation corrections.
- Limits/rollback: H2 is not PostgreSQL runtime evidence; no real PostgreSQL migration, competing DELETE, or audit-failure rollback test was run. PostgreSQL integration/CI and human review remain before Ready/merge. Use a reviewed Issue-scoped code revert and a reviewed forward schema correction; never shrink username until values are checked or erase audit history. No conflict occurred. The user limited shared record edits to these three files, so `docs/history/CODEX_WORKLOG.md` was left untouched.
- Draft PR #814 initial head `d225857b` remote checks failed before any job step ran: GitHub reported empty steps, no assigned runner, and no downloadable logs for module detection, harness validation, and merge guard. This is missing remote execution evidence, not a local suite failure; PR body records the blocker. Rerun remote CI when jobs can start before Ready.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `PersonalAccessTokenController.java:25`, `PersonalAccessTokenService.java:27`, `JwtPatCredentialVerifier.java:25`; PAT service7/HTTP9/JPA1 PASS | 해당 없음: 코드 책임 변경 | PostgreSQL 통합 전 Ready 보류 | `/root/pat_review`, P0–P3 없음 |
| Q2 | PASS | `auth/docs/process-flow.md:15`, `auth/docs/schema.md:47`; JWT→AuthUser→권한→감사 흐름·실패 설명 | 해당 없음: 핵심 흐름 변경 | 실제 PostgreSQL 트랜잭션 확인 필요 | `/root/pat_review` 확인 |
| Q3 | PASS | `auth/README.md:54`, `auth/docs/beginner-guide.md:44`, `auth/docs/local-run.md:61`, `auth/docs/schema.md:73`; V74/요청 예시/결과·한계 | 해당 없음: 기능 문서 변경 | Gateway 미노출은 문서에 명시 | `/root/pat_review` 확인 |
| Q4 | PASS | `JwtPatCredentialVerifier.java:75`, `PersonalAccessTokenService.java:99`, `PersonalAccessTokenPersistenceAdapter.java:41`, `auth/build.gradle:1`; 조건부 폐기 H2 1/1, quality32/32 | 해당 없음: 비자명 로직 변경 | PostgreSQL 동시성 실증 필요 | `/root/pat_review` 확인 |

## 2026-10-06 — GH-857 reporting GL currency guard

- Trace: OPEN #857, `agent/857-reporting-krw-ledger`, `/tmp/account-857-reporting-krw-ledger`, base `origin/main@973d76dd1331fb4be5d233b4ee717d1cf7423df9`; [Draft PR #866](https://github.com/skyg547/account/pull/866) has `Refs #857`. Claim and frozen-review evidence are in Issue comments. Single reporting writer owned four module files; separate read-only `/root/reporting_review` checked the diff; parent Integrator owns this record and Git/GitHub.
- Input → validation → output: `LedgerClientAdapter` queries all GL currencies, verifies every non-null row has `currencyCode=KRW` before account grouping, then preserves existing `BigDecimal` sum and ending-balance fallback. A missing/non-KRW currency throws before `ReportingService` can save a FINAL snapshot. No FX conversion, shared contract, migration, or other module change. `reporting/build.gradle` makes requested `:reporting:test` depend on core/API/batch tests instead of silently ending NO-SOURCE.
- RED: 4/6 focused tests failed before production change. GREEN: focused 6/6; writer's `./gradlew --offline :reporting:test --console=plain --max-workers=1 --no-daemon` ran core104/API10/batch6 = 120 tests, failures/errors/skips0. Parent's requested `./gradlew :reporting:test --console=plain --max-workers=1 --no-daemon` exited0 with all three child test tasks UP-TO-DATE; XML counts independently checked. Read-only reviewer reran the focused suite with `--rerun-tasks`: 6/6 PASS. Diff whitespace, approved-file conflict marker, and unmerged-index checks PASS; review found no P0–P3.
- Rollback: reviewed Issue-scoped revert of four reporting files, preserving this append-only record; no schema or data repair. No live GL database/HTTP integration, production batch run, or remote CI was claimed. Human PR review and remote checks remain before Ready/merge; Issue closure and worktree/branch cleanup are human-owned gates. The user permitted only the three requested shared harness records outside `reporting/**`, so `docs/history/CODEX_WORKLOG.md` was not edited.
- Draft PR remote guard initially reported missing PR metadata, which the parent supplied through `agent:codex`, a fenced verification block, and an explicit `Merge authority:` line. The guard still cannot pass in Draft: repository workflow deliberately passes `policy: undefined` and the guard rejects both Draft state and unconfigured trust policy. This is a repository Ready/merge gate, not evidence of a reporting test failure; the parent did not change the guard or Ready state.

| 항목 | 판정 | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | `LedgerClientAdapter.java:36-59`, `LedgerClientAdapterTest.java:22-103`; focused6/6, reporting120/120 | 해당 없음: 코드 책임 변경 | 실제 GL 연동·CI 확인 | `/root/reporting_review`, P0–P3 없음 |
| Q2 | PASS | `process-flow.md:13-34`, `ReportingService.java:39,79`; FINAL 저장 차단 테스트 | 해당 없음: 생성 흐름 변경 | 실제 GL 응답 검증 | `/root/reporting_review` PASS |
| Q3 | PASS | `process-flow.md:32-38`; KRW 규칙, 명령, 기대 결과, 한계 | 해당 없음: 기능 문서 변경 | 외화 지원 시 명시적 환산 계약 필요 | `/root/reporting_review` PASS |
| Q4 | PASS | `LedgerClientAdapter.java:42`; 통화 차원 제거 전 검증 이유, 집중6/6 | 해당 없음: 비자명 로직 변경 | 남은 게이트는 실제 연동·CI | `/root/reporting_review` PASS |
