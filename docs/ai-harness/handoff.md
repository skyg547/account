## 2026-09-10 — 현재 비동기 Issue 루프 / GH-681 통합 재작업

- 최신 사용자 정정: 구현, PR 검수·병합 대기, 반려 재작업, 종료·정리는 별도 대기열이다. 이전 전역 순차/Draft-only 중단 조건은 폐기하며 아래 기록은 당시 이력이다.
- GH-662 / PR #680: main `5f8103afd4f018723302fd4b15b740b3bcf189c1`에 병합 및 main CI 34381040992 성공. Issue 종료 승인 대기는 기존 account 예약 소유이며 자원을 보존한다. 독립 Issue의 blocker가 아니다.
- GH-681 / PR #682: 원래 `agent/681-harness-quality-contract`, `C:/tmp/account-681-harness-quality-contract`에서 부모가 최신 main 일반 merge로 기록5파일 충돌을 해결한다. 양쪽 이력을 보존하고 실질 정책/테스트5파일은 바꾸지 않는다. 충돌 결정은 conflict-log.md에 기록한다.
- GH-663: 별도 `agent/663-migration-canonical-sslmode`, `C:/tmp/account-663-migration-canonical-sslmode`에서 `/root/migration_663_writer` Astra high가 독립된 Java/테스트2파일을 구현 중이다. 부모 기록은 한 Integrator가 순서대로 갱신한다.
- 예약 account-issue-2의 전역 대기를 제거하고 원래 Issue 재작업/실제 의존성 기준으로 수정했다. ACTIVE·30분·동일 task·기존 품질 블록·최종 merge/close 담당은 보존했고 별도 planner가 정적 검토했다. 실제 미래 예약 실행까지 검증한 것은 아니다.
- 현재 통합본의 두 Node 계약·diff/marker/링크·별도 리뷰·새 head CI를 확인한 뒤 Ready로 인계한다. 최종 병합/close는 account 예약 소유다. 이번 부모 재작업은 merge/close/자원 삭제를 수행하지 않는다.

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
