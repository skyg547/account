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
