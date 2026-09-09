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
