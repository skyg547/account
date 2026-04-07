# MSA Modularization Update 2026-04-07 Asset Tax

## Completed

- added `asset-lease` module
- added `tax` module
- moved `com.ho.account.asset` main source into `asset-lease`
- moved `com.ho.account.tax` main source into `tax`

## Dependency Inversion Added

- added [LeasePaymentResolutionPort.java](/C:/Users/skyg547/IdeaProjects/account/contracts/src/main/java/com/ho/account/contracts/expenditure/LeasePaymentResolutionPort.java)
- added [LeasePaymentResolutionCommand.java](/C:/Users/skyg547/IdeaProjects/account/contracts/src/main/java/com/ho/account/contracts/expenditure/LeasePaymentResolutionCommand.java)
- added [MonolithLeasePaymentResolutionAdapter.java](/C:/Users/skyg547/IdeaProjects/account/app/src/main/java/com/ho/account/common/adapter/MonolithLeasePaymentResolutionAdapter.java)
- updated [LeaseService.java](/C:/Users/skyg547/IdeaProjects/account/asset-lease/src/main/java/com/ho/account/asset/service/LeaseService.java) to call the port instead of `ExpenditureService`

## Current Remaining Coupling

- `app.expenditure.ExpenditureService -> asset-lease`
- `app.expenditure.ExpenditureService -> tax`
- `app.expenditure.APPaymentService -> tax`
- `app.expenditure.APInvoiceService -> tax`
- legacy `app.income.ArInvoice` and `app.income.ArPayment` -> tax

## Validation

- `gradlew test`: success on 2026-04-07
