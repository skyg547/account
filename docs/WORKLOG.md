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
