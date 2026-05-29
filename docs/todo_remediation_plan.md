# @todo Remediation Plan

## Search Scope

- Date: 2026-05-29
- Command: `rg -n "@todo|TODO:" . -g "!**/build/**" -g "!**/.gradle/**" -g "!**/node_modules/**" -g "!**/.git/**"`
- Excluded from action list: historical notes in `CODEX_WORKLOG.md`, `docs/WORKLOG.md`, `docs/todo.md`, and generated/build output.
- Initial code TODO count: 40
- Current code TODO count: 22

## Execution Strategy

TODOs are split by coupling level. Each phase should be handled as a separate change set with focused tests.

| Phase | Scope | Item Count | Goal |
| --- | ---: | ---: | --- |
| 1 | Local domain/application cleanup | 6 | Move business rules out of processors, remove hardcoded audit defaults where possible, and keep public contracts stable. |
| 2 | Accounting policy and account mapping | 11 | Replace hardcoded currency/account choices with policy, master data, or rule-engine inputs. |
| 3 | Open-item and matching correctness | 8 | Make payment, collection, and reconciliation matching explicit and idempotent. |
| 4 | Approval and external integration hardening | 6 | Add proper approval, retry, failure, and callback boundaries. |
| 5 | Reconciliation model consolidation | 7 | Unify reconciliation aggregates and typed policies. |
| 6 | ECL summary mapping policy | 2 | Make allowance summary mapping precedence explicit and testable. |

## Phase 1 - Local Domain/Application Cleanup

| ID | Module | File | Current Issue | Fix Direction | Verification |
| --- | --- | --- | --- | --- | --- |
| T01 | account-mart | `account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/mart/processor/IntegratedPositionProcessor.java:94` | Staging is calculated directly in the processor. | Move staging decision to `OdsAccountLedger` domain behavior and call it from the processor. | `.\gradlew :account-mart:mart-core:test --console=plain` |
| T02 | ecl | `ecl/ecl-batch/src/main/java/com/ho/account/ecl/batch/processor/EclProcessor.java:65` | Maturity-year calculation lives in the batch processor. | Move maturity calculation to ECL core domain/service and keep the processor as orchestration glue. | `.\gradlew :ecl:ecl-batch:test --console=plain` |
| T03 | journal-ledger | `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:169` | Header audit actor defaults to `SYSTEM`. | Propagate actor from the command/event data with a controlled fallback. | Journal rule-engine tests or `.\gradlew :journal-ledger:core:test --console=plain` |
| T04 | journal-ledger | `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:199` | Line audit actor defaults to `SYSTEM`. | Inherit the resolved actor from the journal header. | Journal rule-engine tests or `.\gradlew :journal-ledger:core:test --console=plain` |
| T05 | journal-ledger | `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:219` | Currency defaults to `KRW`. | Resolve currency from event data first, then policy fallback. | Journal rule-engine tests or `.\gradlew :journal-ledger:core:test --console=plain` |
| T06 | journal-ledger | `journal-ledger/core/src/main/java/com/ho/account/journalledger/application/service/journal/JournalRuleEngine.java:344` | Missing DSL values return `null`. | Return a typed missing-value result or throw a domain-level validation exception. | Journal rule-engine tests or `.\gradlew :journal-ledger:core:test --console=plain` |

## Phase 2 - Accounting Policy and Account Mapping

| ID | Module | File | Current Issue | Fix Direction | Verification |
| --- | --- | --- | --- | --- | --- |
| T07 | deposit | `deposit/core/src/main/java/com/ho/account/deposit/application/service/DepositService.java:55` | Initial deposit journal creation is not wired. | Introduce/use a journal port and create the initial recognition entry transactionally. | `.\gradlew :deposit:core:test --console=plain` |
| T08 | closing | `closing/batch/src/main/java/com/ho/account/closing/batch/service/FxValuationService.java:49` | Functional/reporting currency is hardcoded. | Resolve currency from company/accounting policy. | `.\gradlew :closing:batch:test --console=plain` |
| T09 | closing | `closing/batch/src/main/java/com/ho/account/closing/batch/service/FxValuationService.java:64` | Book rate is inferred from a fake spread. | Add base amount or book-rate source to the balance snapshot. | `.\gradlew :closing:batch:test --console=plain` |
| T10 | closing | `closing/batch/src/main/java/com/ho/account/closing/batch/service/FxValuationService.java:99` | Journal currency is hardcoded to `KRW`. | Use the valuation policy/reporting currency consistently. | `.\gradlew :closing:batch:test --console=plain` |
| T11 | payable | `payable/src/main/java/com/ho/account/expenditure/application/service/PurchaseService.java:63` | Purchase actor defaults to `SYSTEM`. | Propagate caller/command actor. | `.\gradlew :payable:test --console=plain` |
| T12 | payable | `payable/src/main/java/com/ho/account/expenditure/application/service/PurchaseService.java:111` | AP, expense, and VAT accounts are hardcoded. | Resolve through product/vendor/tax account mapping or journal rules. | `.\gradlew :payable:test --console=plain` |
| T13 | payable | `payable/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:156` | Cash/AP accounts are hardcoded. | Resolve settlement accounts from payment method and vendor policy. | `.\gradlew :payable:test --console=plain` |
| T14 | payable | `payable/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:179` | Advance/cash accounts are hardcoded. | Resolve advance-payment accounts from accounting policy. | `.\gradlew :payable:test --console=plain` |
| T15 | payable | `payable/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:198` | AP/advance offset accounts are hardcoded. | Resolve offset accounts from payable and advance-payment mapping. | `.\gradlew :payable:test --console=plain` |
| T16 | receivable | `receivable/src/main/java/com/ho/account/receivable/application/service/SalesService.java:95` | AR, revenue, and VAT accounts are hardcoded. | Resolve through customer/product/tax account mapping or journal rules. | `.\gradlew :receivable:test --console=plain` |
| T17 | receivable | `receivable/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:137` | Cash/clearing accounts are hardcoded. | Resolve collection accounts from payment channel policy. | `.\gradlew :receivable:test --console=plain` |
| T18 | receivable | `receivable/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:156` | Clearing/AR accounts are hardcoded. | Resolve settlement accounts from receivable and channel policy. | `.\gradlew :receivable:test --console=plain` |

## Phase 3 - Open-Item and Matching Correctness

| ID | Module | File | Current Issue | Fix Direction | Verification |
| --- | --- | --- | --- | --- | --- |
| T19 | payable | `payable/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:94` | Payment execution is mocked in application logic. | Add outbound payment-gateway port with idempotency key, retry, and failure status handling. | `.\gradlew :payable:test --console=plain` |
| T20 | payable | `payable/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:100` | Payable settlement matches by vendor and amount. | Carry `payableId` or open-item key through payment run and settlement. | `.\gradlew :payable:test --console=plain` |
| T21 | receivable | `receivable/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:67` | Collection matching uses exact amount only. | Match by reference, virtual account, customer, due date tolerance, and duplicate controls. | `.\gradlew :receivable:test --console=plain` |
| T22 | receivable | `receivable/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:112` | Partial matching residual split is not persisted explicitly. | Persist residual open item or settlement allocation detail. | `.\gradlew :receivable:test --console=plain` |
| T23 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java:148` | Empty snapshot and adapter failure both become zero. | Return a typed snapshot result that distinguishes no data from read failure. | `.\gradlew :reconciliation:test --console=plain` |
| T24 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:442` | Source snapshot is skeleton data in `criteriaJson`. | Load source snapshot through a dedicated port/adapter. | `.\gradlew :reconciliation:test --console=plain` |
| T25 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:450` | Target snapshot uses fixed debit/status assumptions. | Apply account, currency, status, and side filters from the unit policy. | `.\gradlew :reconciliation:test --console=plain` |
| T26 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:536` | Adjustment journal lacks policy-driven currency, rate, idempotency, and reason. | Build adjustment command from a typed adjustment policy. | `.\gradlew :reconciliation:test --console=plain` |

## Phase 4 - Approval and External Integration Hardening

| ID | Module | File | Current Issue | Fix Direction | Verification |
| --- | --- | --- | --- | --- | --- |
| T27 | closing | `closing/batch/src/main/java/com/ho/account/closing/batch/service/FxValuationService.java:135` | FX valuation journals are auto-approved/posted. | Route through closing adjustment approval or controlled auto-post policy with reversal support. | `.\gradlew :closing:batch:test --console=plain` |
| T28 | closing | `closing/batch/src/main/java/com/ho/account/closing/batch/service/EclProvisionService.java:172` | ECL provision journals are auto-approved/posted. | Route through closing adjustment approval or controlled auto-post policy with reversal support. | `.\gradlew :closing:batch:test --console=plain` |
| T29 | journal-ledger | `journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/kafka/KafkaTransactionListener.java:44` | Kafka listener lacks failure/DLQ handling. | Add retry/DLQ behavior and observable failure logging. | `.\gradlew :journal-ledger:api:test --console=plain` |
| T30 | reporting | `reporting/core/src/main/java/com/ho/account/reporting/infrastructure/filing/LocalRegulatoryFilingGatewayAdapter.java:17` | Filing gateway only generates local receipt IDs. | Add real protocol adapter boundary for auth, submit, retry, rejection callback, and receipt tracking. | `.\gradlew :reporting:core:test --console=plain` |
| T31 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:575` | Runtime fallback creates `GENERIC_MISMATCH`. | Seed/manage reason reference data and fail closed when required reason is missing. | `.\gradlew :reconciliation:test --console=plain` |
| T32 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:359` | Reconciliation run actor defaults to `SYSTEM`. | Propagate caller actor from command/API context. | `.\gradlew :reconciliation:test --console=plain` |

## Phase 5 - Reconciliation Model Consolidation

| ID | Module | File | Current Issue | Fix Direction | Verification |
| --- | --- | --- | --- | --- | --- |
| T33 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java:60` | Two reconciliation aggregate lifecycles coexist. | Choose one lifecycle model and migrate services to that model. | `.\gradlew :reconciliation:test --console=plain` |
| T34 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java:132` | SLA default of 3 days is hidden. | Move SLA to reconciliation policy/master data. | `.\gradlew :reconciliation:test --console=plain` |
| T35 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconManagerService.java:209` | Matching rules are untyped JSON. | Replace with a validated matching-policy object. | `.\gradlew :reconciliation:test --console=plain` |
| T36 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:137` | Unit deletion lacks cleanup/archive policy. | Define soft-delete/archive behavior and dependent-data checks. | `.\gradlew :reconciliation:test --console=plain` |
| T37 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java:258` | Reason code deletion can break historical references. | Prevent deletion when referenced or implement inactive/SCD2 semantics. | `.\gradlew :reconciliation:test --console=plain` |
| T38 | reconciliation | `reconciliation/src/main/java/com/ho/account/reconciliation/domain/ReconciliationAdjustmentPolicy.java:35` | Adjustment account codes are free-form JSON. | Replace with a typed and validated adjustment policy. | `.\gradlew :reconciliation:test --console=plain` |

## Phase 6 - ECL Summary Mapping Policy

| ID | Module | File | Current Issue | Fix Direction | Verification |
| --- | --- | --- | --- | --- | --- |
| T39 | ecl | `ecl/ecl-core/src/main/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/JdbcAllowanceSummaryPersistenceAdapter.java:47` | SQL decides duplicate mapping precedence with `ROW_NUMBER()`. | Extract the precedence rule into named domain policy or document it with focused adapter tests. | `.\gradlew :ecl:ecl-core:test --console=plain` |
| T40 | journal-ledger | `journal-ledger/core/src/main/java/com/ho/account/journalledger/domain/journal/repository/JournalDetailRepository.java:21` | Query mixes hardcoded approval/posting statuses. | Accept `JournalEntryStatus` parameters or a named query policy. | `.\gradlew :journal-ledger:core:test --console=plain` |

## Current Execution Order

1. Phase 1 is complete.
2. Phase 2 is complete. Continue with Phase 3 open-item and matching correctness items T19-T26.
3. Re-run the TODO search after every phase and update this document with completed IDs.

## Progress

| ID | Status | Change | Verification |
| --- | --- | --- | --- |
| T01 | Done | Added `OdsAccountLedger.determineStaging()` and changed `IntegratedPositionProcessor` to delegate IFRS 9 staging to the domain object. | `.\gradlew :account-mart:mart-core:test --console=plain` passed. |
| T02 | Done | Added `CrAccount.resolveMaturityYears()` and changed both batch ECL processing and core allowance calculation to share the domain maturity rule. | `.\gradlew :ecl:ecl-core:test --console=plain` and `.\gradlew :ecl:ecl-batch:test --console=plain` passed. |
| T03 | Done | `JournalRuleEngine` now resolves the entry actor from event data instead of forcing `SYSTEM`. | `.\gradlew :journal-ledger:core:test --console=plain` passed. |
| T04 | Done | Journal lines inherit the resolved entry audit actor. | `.\gradlew :journal-ledger:core:test --console=plain` passed. |
| T05 | Done | Currency resolves from event data or nested company/accounting policy before the default fallback. | `.\gradlew :journal-ledger:core:test --console=plain` passed. |
| T06 | Done | DSL value lookup now uses a typed internal result so required missing values fail clearly and optional values can be skipped deliberately. | `.\gradlew :journal-ledger:core:test --console=plain` passed. |
| T07 | Done | Initial deposit journal posting now uses `JournalPostingPort` and configurable deposit account mapping after the account is saved. | `.\gradlew :deposit:core:test --console=plain` passed. |
| T08 | Done | FX valuation now resolves reporting currency from `ClosingAccountingProperties` instead of forcing `KRW`. | `.\gradlew :closing:batch:test --console=plain` passed. |
| T09 | Done | `GlAccountBalance` now carries `baseEndingBalance`; FX valuation uses it and skips valuation when the base amount is missing instead of inventing a book rate. | `.\gradlew :closing:batch:test --console=plain` passed. |
| T10 | Done | FX valuation journal currency now follows the closing reporting-currency policy. | `.\gradlew :closing:batch:test --console=plain` passed. |
| T11 | Done | `PurchaseService` now requires a caller-provided `createdBy` actor and trims it before persistence/journal posting. | `.\gradlew :payable:test --console=plain` passed. |
| T12 | Done | Purchase recognition accounts now come from `PayableAccountMappingPort` with a configurable adapter instead of service-level fixed codes. | `.\gradlew :payable:test --console=plain` passed. |
| T13 | Done | Payment execution AP/cash accounts now come from `PayableAccountMappingPort`; payment journals inherit the payment-run actor when available. | `.\gradlew :payable:test --console=plain` passed. |
| T14 | Done | Advance-payment advance/cash accounts now come from `PayableAccountMappingPort`. | `.\gradlew :payable:test --console=plain` passed. |
| T15 | Done | AP/advance offset accounts now come from `PayableAccountMappingPort`. | `.\gradlew :payable:test --console=plain` passed. |
| T16 | Done | Sales recognition AR/revenue/VAT accounts now come from `ReceivableAccountMappingPort` with a configurable adapter. | `.\gradlew :receivable:test --console=plain` passed. |
| T17 | Done | Collection recognition cash/clearing accounts now come from `ReceivableAccountMappingPort`. | `.\gradlew :receivable:test --console=plain` passed. |
| T18 | Done | Collection match clearing/AR accounts now come from `ReceivableAccountMappingPort`. | `.\gradlew :receivable:test --console=plain` passed. |
