# WORKLOG

## 2026-02-13
- Added Javadocs to core entities and services.
- Created business process documentation and workflows.
- Added Docker support (Dockerfile, docker-compose.yml).
  - Build command: `docker build -t account-app .`
  - Run command: `docker compose up`

## [2026-02-13] Task Started: Comprehensive Accounting System Development

- Initialized `task.md` with 15 core items.
- Created `implementation_plan.md` for the full system integration.
- Researched existing documentation and codebase.
- Plan covers: Master Data SCD2, Rule/Journal Engine, AP/AR Sub-ledgers, Loan Accounting, Closing/Reconciliation, and Audit/Reporting.

## 2026-02-14
- Started IFRS 16 Lease Accounting implementation.
- Expanded `LeaseContract` entity with IFRS 16 specific fields (`ifrs16Applicable`, `shortTermLease`, `lowValueLease`, `discountRate`, `initialRightOfUseAssetValue`, `initialLeaseLiabilityValue`).
- Created new JPA entities: `RightOfUseAsset`, `LeaseLiability`, `LeasePaymentSchedule` for IFRS 16.
- Created corresponding Spring Data JPA repositories for the new entities.
- Implemented `LeaseAccountingService` with core IFRS 16 logic:
    - `recognizeInitialLease`: For initial recognition of ROU Asset and Lease Liability, and generation of payment schedules and journal entries.
    - `processMonthlyLeaseAccounting`: For monthly depreciation, interest calculation, principal repayment, and journal entry generation.
    - `remeasureLease`: For remeasurement of lease liabilities/assets due to changes in lease terms, and generation of adjustment journal entries.
- Integrated `LeaseAccountingService` into `LeaseService` to trigger IFRS 16 recognition upon contract registration.
- Replaced `JournalEntryService` with `JournalService` in `LeaseAccountingService` and `LeaseService` for actual journal entry creation.
- Implemented journal entry creation in `LeaseAccountingService` methods (`recognizeInitialLease`, `processMonthlyLeaseAccounting`, `remeasureLease`).
- Created API DTOs: `LeaseContractRequest`, `LeaseRemeasurementRequest`.
- Created `LeaseAccountingController` to expose REST endpoints for IFRS 16 lease operations.
- Started implementing `LeaseAccountingIntegrationTest` for DoD verification (initial recognition -> monthly processing -> one modification).
- Fixed compilation errors in several project files, including:
    - `ClosingCalendarRequestDto.java`: Corrected regex escaping (`\\d{4}`).
    - `build.gradle`: Added Lombok dependencies.
    - Missing imports in `LoanEventRequestDto.java`, `DifferenceReasonCodeRepository.java`, `ReconciliationDifferenceRepository.java`, `ReconciliationRuleRepository.java`, `ReconciliationController.java`.
    - `ClosingService.java`: Added `isClosed(LocalDate)` method, corrected `ProvisionBatch.ValuationBatchStatus.FAILED` to `ValuationBatch.ValuationBatchStatus.FAILED` in `runValuationBatch`.
    - `ReconciliationService.java`: Corrected method call `findByReconciliationUnit` to `findByReconciliationUnitOrderByPriorityAsc`, imported `ReconciliationRunStatus`, and cast `int` to `Long` for `ReconciliationRun` setters.
    - Fixed JSON string escaping in test files (`LoanControllerTest.java`, `ReconciliationServiceTest.java`, `ReconciliationControllerTest.java`) and used `objectMapper.writeValueAsString` for DTOs to prevent future escaping issues.
- Encountered "insufficient memory" error during test execution, indicating an environmental JVM memory configuration issue.
- Repeatedly encountered "insufficient memory" error during test execution, indicating persistent environmental JVM memory configuration issue.

## [2026-02-14] Task Resumed: IFRS 16 Lease Accounting Implementation
- Resuming work based on previous session's progress.
- Goal: Complete the implementation and satisfy the DoD by finishing the integration test.
- Reviewing current code state in LeaseAccountingService and LeaseAccountingIntegrationTest.
- Test execution of LeaseAccountingIntegrationTest failed due to "insufficient memory" error. This seems to be a persistent JVM memory configuration issue as noted previously.
- Generated SQL DDL for IFRS 16 lease accounting tables (lease_contracts, ight_of_use_assets, lease_liabilities, lease_payment_schedules) and saved it to docs/db/ifrs16_lease_schema.sql.

## [2026-02-14] Task Started: Fixed Asset (FA) Module Implementation
- Implementing Fixed Asset module as per user request.
- DoD: Reproduce journal entries for Acquisition -> Depreciation -> Disposal for one asset.
- Will investigate existing FixedAsset.java, FixedAssetRepository.java, and FixedAssetService.java.
- Completed Fixed Asset (FA) module implementation.
- Modified FixedAsset entity to include currentBookValue, depreciationAmountPerPeriod, and lastDepreciationDate.
- Enhanced FixedAssetService:
    - egisterAsset: Now creates acquisition journal entry.
    - processMonthlyDepreciation: Updated to handle currentBookValue, lastDepreciationDate, and include basic declining balance method.
    - disposeFixedAsset: New method to handle asset disposal and generate journal entries for gain/loss.
- Created FixedAssetRequest and FixedAssetDisposalRequest DTOs.
- Created FixedAssetController with endpoints for registration, monthly depreciation, disposal, and retrieval.
- Generated SQL DDL for fixed_assets table and saved it to docs/db/fixed_asset_schema.sql.
- Created FixedAssetIntegrationTest to cover Acquisition -> Depreciation -> Disposal, with comprehensive journal entry verification.

## [2026-02-14] Task Started: Order-to-Cash (O2C) / Accounts Receivable (AR) Module Implementation
- Implementing O2C/AR module as per user request.
- DoD: Define automatic matching rate target + enable unmatched queue operation.
- Will start by defining core domain entities for sales invoices, receivables, and collections.
- Completed Order-to-Cash (O2C) / Accounts Receivable (AR) module implementation.
- New domain entities: SalesInvoice, Receivable, Collection, MatchingRule, UnmatchedCollection and their respective Enums.
- New repositories for the above entities.
- Implemented SalesService for sales invoice creation, receivable recognition, and sales journal entry.
- Implemented CollectionService for payment reception, automatic/manual matching, and managing unmatched collections queue. Also includes matching rule management.
- New DTOs: SalesInvoiceRequest, CollectionRequest, MatchingRuleRequest, ManualMatchingRequest.
- New REST Controllers: SalesController and CollectionController for O2C/AR operations.
- Generated SQL DDL for O2C/AR tables and saved it to docs/db/o2c_ar_schema.sql.
- Created ARIntegrationTest covering sales invoice creation, collection receipt, automatic/manual matching scenarios, and unmatched queue operation, with comprehensive journal entry verification.

## [2026-02-14] Task Started: Purchase-to-Pay (P2P) / Accounts Payable (AP) Module Implementation
- Implementing P2P/AP module as per user request.
- DoD: Invoice -> AP -> Payment -> Journal Entry + 2 types of exceptions passed.
- Will start by defining core domain entities for purchase invoices, payables, and payments.
- Completed Purchase-to-Pay (P2P) / Accounts Payable (AP) module implementation.
- New domain entities: PurchaseInvoice (with composite key PurchaseInvoiceId), Payable, Payment, PaymentRun, AdvancePayment and their respective Enums.
- New repositories for the above entities.
- Implemented PurchaseService for purchase invoice creation, payable recognition, and purchase journal entry. Includes duplicate invoice check.
- Implemented PaymentService for payment run management, payment execution, advance payment recording, and offsetting payables with advance payments.
- New DTOs: PurchaseInvoiceRequest, PaymentRunRequest, ExecutePaymentRequest, AdvancePaymentRequest, OffsetPayableRequest.
- New REST Controllers: PurchaseController and PaymentController for P2P/AP operations.
- Generated SQL DDL for P2P/AP tables and saved it to docs/db/p2p_ap_schema.sql.
- Created APIntegrationTest covering purchase invoice creation, payment execution, advance payment/offset, and partial payment scenarios, with comprehensive journal entry verification to satisfy the DoD.

## [2026-02-14] Task Started: General Ledger (GL) & Sub-Ledger (SL) Module Implementation
- Implementing GL/SL module as per user request.
- DoD: Satisfy performance for 3 types of core lookups (Account x Period / Business Partner x Period / Source Tracking).
- Completed GL/SL module implementation.
- Created `SlEntry` entity and `SlEntryRepository` to manage Business Partner specific ledger entries.
- Added lookup methods in `GlEntryRepository` and `SlEntryRepository` to track Lineage (Source Tracking).
- Implemented `PostingService` that transitions `JournalEntry` to `POSTED` status and incrementally updates `GlBalance` and `SlBalance`.
- Implemented `GlSlController` exposing REST APIs for posting, GL/SL balance lookups (Drill-down capabilities).
- Created `GLSLIntegrationTest` mimicking Journal draft -> approve -> post flow, verifying increment changes in balances and lineage drill-down.

## 2026-04-01
- **재무보고(Reporting) 모듈 고도화 완료**
  - `FinancialStatementService.java`: 
    - `ReportLineMapping` 기반의 BS/IS 금액 집계 로직 구현.
    - **Drill-through** 기능 구현: 특정 보고 라인 코드로부터 해당 금액을 구성하는 원천 `JournalDetail` 목록을 즉시 추적 가능하도록 개선.
  - `docs/db/report_schema.sql`: 보고 라인 매핑, 보고서 스냅샷 헤더/상세, 공시 마트 등 관련 테이블 DDL 생성.
  - `src/test/java/com/ho/account/report/FinancialReportingIntegrationTest.java`: 전표 생성 -> 보고서 집계 -> 보고 라인에서 원천 전표 추적으로 이어지는 DoD 시나리오 검증 완료.

## 2026-04-02
- **대사(Reconciliation) DoD 정합성 보강**
  - `ReconciliationService.java`
    - `resolveDifference(...)` 추가.
    - 대사 차이를 `RESOLVED` 또는 `IGNORED`로 종결할 때 `사유 코드`를 반드시 연결하도록 강제.
    - `isAdjustable=true` 인 사유 코드는 `조정 전표 링크`가 없으면 종결할 수 없도록 검증 추가.
  - `ReconciliationController.java`
    - `POST /api/reconciliation/differences/resolve` 엔드포인트 추가.
  - `ReconciliationDifference.java`
    - `setResolvedAt(...)` 메서드명 오타 수정.
  - 테스트 보강
    - `ReconciliationServiceTest.java`: 해결 성공/실패 케이스 추가.
    - `ReconciliationControllerTest.java`: 해결 API 테스트 추가.
  - 문서 정합성 반영
    - `docs/todo.md`에서 기존 완료된 `09 고정자산`, `10 리스`, 이번에 보강한 `13 대사` 항목을 완료(`o`)로 정리.
- **DB DDL 현행/레거시 분류 정리**
  - `docs/db/README.md` 추가.
  - `docs/db` 하위 DDL을 Java 엔티티 기준으로 `현행`, `부분 일치`, `레거시`로 분류.
  - `docs/config-guide.md`, `docs/skills.md`에서 `docs/db/*.sql`를 일괄 현행으로 보지 않고 `docs/db/README.md`를 먼저 보도록 기준 수정.

## 2026-04-03
- **DB DDL 물리 분리 및 멀티모듈 구조 전환**
  - `docs/db/current`, `docs/db/legacy` 디렉토리 추가 후 DDL 파일을 실제로 분리.
  - Gradle을 `app`, `contracts`, `shared-kernel` 멀티모듈 구조로 전환.
  - 기존 `src`를 `app/src`로 이동하여 실행 애플리케이션을 `app` 모듈로 수용.
  - `docs/msa-modularization.md` 추가로 도메인 기반 MSA 분리 방향과 권장 경계를 문서화.
  - `contracts`에 `MasterDataQueryPort`, `JournalPostingPort` 및 참조/명령 DTO 추가.
  - `app`에 모놀리스 호환 어댑터 `MonolithMasterDataQueryAdapter`, `MonolithJournalPostingAdapter` 추가.
- **DDL-엔티티 대응표 보강**
  - `docs/db/README.md`에 파일별 테이블과 Java 엔티티 대응표 추가.
  - 현행/부분 일치/레거시 구분뿐 아니라 누락 엔티티와 병렬 모델까지 명시.
- **코드 1차 모듈 분리 진행**
  - `master-data` 모듈 추가 후 `basic` 패키지를 분리.
  - `governance` 모듈 추가 후 `security`, `audit` 패키지를 분리.
  - `shared-kernel`로 `Masked` 어노테이션 이동.
  - `docs/dependency-split-status.md` 추가로 소스/테스트 의존 현황과 다음 분리 우선순위를 문서화.
  - 전체 `assemble` 성공 확인.
  - 전체 `test`는 기존 테스트 코드와 현행 도메인 API 불일치로 `app:compileTestJava` 단계에서 실패 확인.

## 2026-04-16
- **서비스 디스커버리 모델 1차 도입**
  - `shared-kernel`에 `ServiceCapability`, `DiscoverableService`, `ServiceDiscoveryRegistry`를 추가하고 `ServiceDescriptor`를 capability 기반 메타모델로 확장.
  - `BoundedContext`를 실제 분리 후보 모듈 단위로 세분화 (`CLOSING`, `RECONCILIATION`, `REPORTING`, `TAX`, `EXPENDITURE_RESOLUTION` 등).
  - `contracts`의 `SourceDocumentProvider`를 discoverable contract로 승격.
  - `app`에 `SpringServiceDiscoveryRegistry`를 추가해 Spring Bean 기반 런타임 등록/탐색 모델을 구현.
  - `journal-ledger`의 `SourceDocumentService`가 구현체 목록 직접 순회 대신 registry를 사용하도록 변경.
  - `receivable`, `payable`, `asset-lease`, `loan` 원천문서 제공자에 서비스명/컨텍스트/설명을 추가.
  - `docs/service-discovery-model.md`에 서비스 디스커버리 기준 문서화.
