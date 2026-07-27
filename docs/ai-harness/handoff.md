# AI Harness Handoff - 2026-07-27 Master Data Review Ready

## Active Goal

모듈을 순차적으로 점검하면서 헥사고날 아키텍처, DDD, 실제 업무 프로세스,
스켈레톤 코드 여부, 데이터 정합성, 메서드 호출 순서, 객체지향/함수형 설계를
검수한다. 구현 가능한 항목은 고도화하고, 즉시 구현할 수 없는 운영 설계는
근거 있는 `@todo`로 남긴다. 기존 초보자용 주석과 업무 설명은 최대한 보존하고
코드 흐름에 맞춰 문서도 최신화한다.

## Current State

- Branch/push target: `agent/asset-lease-split` -> `origin/agent/asset-lease-split`.
- Scope: Master Data SCD2 version/approval/apply boundary, locking, runtime packaging, tests, and docs.
- Review state: focused implementation and verification complete; commit/push requested by the user.
- Other four worktrees are clean and their feature commits are already in `main`; do not merge them individually.

## What Changed

- Added repository-backed SCD2 requested-version policy and rechecks at request/approve/apply boundaries.
- Added immutable fail-closed applier registry and duplicate ownership validation.
- Added pessimistic decision lookup, optimistic request lock version, and V3 migration.
- Added Governance approval source-reference idempotency, conflict detection, applied timestamp lineage, and V5 migration.
- Added API validation, HTTP 409 conflict mapping, runtime dependency/port/readiness alignment, and JDK 17 packaging.
- Preserved V2 checksum, added V4 forward payload type correction, and documented the clean PostgreSQL bootstrap gap.
- Preserved beginner explanations and updated README, local run, process, schema, archive, worklogs, and review prompt.

## Verification

- `:master-data:clean :governance:clean :master-data:test :master-data:bootJar :governance:test :governance:bootJar`: passed.
- Master Data 17 suites/57 tests and Governance 10 suites/25 tests: total 82 with 0 failures, errors, or skips; both bootJars passed.
- `git diff --check`, conflict/placeholder scan, and changed Markdown relative-link validation passed.
- The initial incremental compile failed on stale module class outputs; the clean full compile passed and is the authoritative result.

## Known Risks And TODO

- Live PostgreSQL migration and Docker/Compose image execution were not run.
- Historical V2 declares `CLOB`; a clean PostgreSQL database needs a vendor-specific pre-V2 baseline before production certification.
- Same-key multi-node requests need key/advisory locking; concurrent first source-reference inserts need atomic conflict recovery; bulk apply needs per-request transactions, SKIP LOCKED, and execution history.
- Trusted actor extraction, payload masking/authorization, direct-write governance, and three unsupported typed appliers remain.

## Integration Order

1. Commit and push this verified Master Data pass on `agent/asset-lease-split`.
2. Merge current `origin/main` into the feature branch rather than rebasing published history.
3. Resolve harness/docs conflicts, rerun affected checks, push the merge commit, and open/review a PR to `main`.
4. Do not push directly to `main` and do not remove clean worktrees until PR integration is confirmed.

## Rollback

- Revert the Master Data governance/runtime commit as one unit.
- Keep V3/V4/V5 aligned with the entity fields when reverting; partial migration rollback is unsafe.

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
