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
