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
