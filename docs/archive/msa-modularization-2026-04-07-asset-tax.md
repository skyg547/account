# MSA Modularization Update 2026-04-07 Asset Tax

## Completed

- added `asset-lease` module
- added `tax` module
- moved `com.ho.account.asset` main source into `asset-lease`
- moved `com.ho.account.tax` main source into `tax`

## Dependency Inversion Added

- added [LeasePaymentResolutionPort.java](/C:/Users/skyg547/IdeaProjects/account/contracts/src/main/java/com/ho/account/contracts/expenditure/LeasePaymentResolutionPort.java)
- added [LeasePaymentResolutionCommand.java](/C:/Users/skyg547/IdeaProjects/account/contracts/src/main/java/com/ho/account/contracts/expenditure/LeasePaymentResolutionCommand.java)
- transitional adapter moved to module-owned location:
  - [MonolithLeasePaymentResolutionAdapter.java](/C:/Users/skyg547/IdeaProjects/account/expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/MonolithLeasePaymentResolutionAdapter.java)
- lease orchestration service path changed during later refactoring:
  - `LeaseService.java` legacy path removed

## Current Remaining Coupling

- `app.expenditure.ExpenditureService -> asset-lease`
- `app.expenditure.ExpenditureService -> tax`
- `app.expenditure.APPaymentService -> tax`
- `app.expenditure.APInvoiceService -> tax`
- legacy `app.income.ArInvoice` and `app.income.ArPayment` -> tax

## Validation

- `gradlew test`: success on 2026-04-07
