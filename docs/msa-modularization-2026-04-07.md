# MSA Modularization Update 2026-04-07

## Summary

- Added Gradle modules: `receivable`, `payable`
- Added Gradle module: `reporting`
- Moved current AR main source code into `receivable`
- Moved current AP core main source code into `payable`
- Moved current reporting domain/repository/service code into `reporting`
- Moved contract adapter implementations into their owning modules
- Moved drilldown and source-document routing into `journal-ledger`
- Split source-document providers into owning domain modules
- Reduced `app` runtime code to bootstrap-only and moved unmatched legacy common artifacts out of runtime
- Replaced direct fixed-asset registration / lease activation calls with `AssetRegistrationPort`
- Moved AP tax-invoice API/service ownership into `tax`
- Replaced direct tax-invoice entity/repository coupling with `TaxInvoiceQueryPort`

## Current Module Ownership

- `receivable`
  - `SalesInvoice`
  - `Receivable`
  - `Collection`
  - matching rules
  - AR services and controllers
- `payable`
  - `PurchaseInvoice`
  - `Payable`
  - `Payment`
  - `PaymentRun`
  - `AdvancePayment`
  - AP services and controllers
- `reporting`
  - financial statement/report snapshot domain
  - reporting repositories
  - reporting services
- `master-data`, `journal-ledger`, `closing`, `expenditure-resolution`
  - own the contract adapter implementations they expose
- `tax`
  - owns AP tax-invoice APIs/services
  - exposes tax-invoice lookup through a contracts adapter
- `asset-lease`, `loan`, `receivable`, `payable`
  - own their `SourceDocumentProvider` implementations

## Compatibility Interfaces

- Added [SourceDocumentProvider.java](/C:/Users/skyg547/IdeaProjects/account/contracts/src/main/java/com/ho/account/contracts/source/SourceDocumentProvider.java)
- Added [TaxInvoiceQueryPort.java](/C:/Users/skyg547/IdeaProjects/account/contracts/src/main/java/com/ho/account/contracts/tax/TaxInvoiceQueryPort.java)
- Replaced repository-coupled source document routing with providers:
  - [AppSourceDocumentProvider.java](/C:/Users/skyg547/IdeaProjects/account/app/src/main/java/com/ho/account/common/service/AppSourceDocumentProvider.java)
  - [ReceivableSourceDocumentProvider.java](/C:/Users/skyg547/IdeaProjects/account/receivable/src/main/java/com/ho/account/income/service/ReceivableSourceDocumentProvider.java)
  - [PayableSourceDocumentProvider.java](/C:/Users/skyg547/IdeaProjects/account/payable/src/main/java/com/ho/account/expenditure/service/PayableSourceDocumentProvider.java)
- Replaced direct tax-invoice lookups with [TaxInvoiceQueryAdapter.java](/C:/Users/skyg547/IdeaProjects/account/tax/src/main/java/com/ho/account/tax/adapter/TaxInvoiceQueryAdapter.java)

## Deferred Areas

- `BudgetService`
  - still depends on surrounding expenditure workflow decisions
- legacy `ArInvoice`, `ArPayment`
  - still depend on `tax`
  - remain in `app`
- app adapters and drilldown/common services
  - reduced to bootstrap-only runtime code

## Next Split Order

1. remove remaining AR legacy `tax` coupling
2. reduce direct `master-data` entity sharing across modules
3. add module-owned tests as `app` integration tests are carved back
