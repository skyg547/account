# Dependency Split Status 2026-04-07

## Verified Boundary

- `app` depends on `receivable` and `payable`
- `app` depends on `reporting`
- `SourceDocumentService` now depends on `SourceDocumentProvider` implementations instead of AR/AP repositories
- reporting code now lives outside `app` and depends on `master-data` and `journal-ledger`
- contract adapter implementations now live outside `app`
- drilldown and source-document routing now live in `journal-ledger`, while providers live with their owning domains
- `app` runtime code is reduced to Spring Boot bootstrap; unmatched legacy artifacts were removed from the runtime module
- tests remain in `app` so full integration coverage is preserved during transition

## Remaining Cross-Domain Dependencies

- `app.asset.LeaseService -> app.expenditure.ExpenditureService`
- `app.expenditure.* -> app.asset.*`
- `app.expenditure.* -> app.tax.*`
- `app.income.ArInvoice` and `app.income.ArPayment` -> `app.tax.*`
- `journal-ledger` and other core modules still use `master-data` entities directly

## Why Full `expenditure` Split Was Deferred

- moving the whole package now would introduce reverse dependencies from `payable` into `app`
- the stable cut line is AP core first, then `asset` and `tax`, then expenditure resolution

## Latest Split

- moved `com.ho.account.report.*` from `app` to `reporting`
- moved contract adapter implementations into `master-data`, `journal-ledger`, `closing`, and `expenditure-resolution`
- split source-document providers by ownership: `asset-lease`, `loan`, `receivable`, `payable`
- moved unused `InvoiceMatching` artifacts into `docs/legacy-src`
- `app` is now effectively a bootstrap module
