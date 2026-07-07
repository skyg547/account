# Latest Handoff - 2026-07-07 Asset Lease

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: `asset-lease` depreciation batch calculation/persistence boundary.
- Working tree: local uncommitted account-mart and asset-lease changes exist.

## What Changed

- `FixedAssetDepreciationResult` now carries depreciation amount, resulting accumulated depreciation, resulting book value, and resulting status.
- `FixedAsset.calculateDepreciation()` calculates without mutating the entity; `FixedAsset.depreciate()` still performs the single-asset state transition for API/service paths.
- `DepreciationPipeline` returns result values and avoids JPA entity mutation in batch chunks.
- `AssetJdbcAdapter.updateDepreciationBulk()` writes calculated values and status once through JDBC bulk update.
- `AssetDepreciationBatchConfig` remains orchestration-only and delegates calculation to core pipeline.
- asset-lease docs were updated for the new batch flow.

## Verification Summary

- `.\gradlew :asset-lease:core:test :asset-lease:api:compileJava :asset-lease:batch:compileJava --console=plain --max-workers=1`: passed.

## Known Risks

- Actual large seeded `assetDepreciationJob` execution was not run in this pass.
- `updated_at = NOW()` compatibility with the final production PostgreSQL/Flyway DDL should be checked when Flyway is enabled.

## Rollback

- Before commit: restore `asset-lease` and related `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` changes carefully.
- After commit: revert the asset-lease depreciation boundary commit.

---
# Latest Handoff - 2026-07-07 Account Mart

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: `account-mart` collateral detail DQ/LGD prerequisite flow.
- Working tree: local uncommitted account-mart changes exist.

## What Changed

- `OdsApartCollDetail` now contains LGD required-input validation and no longer carries the previous implementation `@todo`.
- `OdsApartCollDetailRepository` outbound port, `JpaOdsApartCollDetailRepository`, and `OdsApartCollDetailPersistenceAdapter` connect apartment collateral detail through hexagonal boundaries.
- `CollateralDataQualityInspectionService` loads apartment detail through the port and delegates business judgement to `CollateralDataQualityProcessor`.
- `CollateralDataQualityProcessor` now checks collateral master appraisal, real-estate/apartment detail existence, and district/KB market price/exclusive-area input quality.
- `CollateralDataQualityItemProcessor` remains a Spring Batch adapter and delegates to the core application service.
- `DataPopulator` creates apartment collateral details for generated real-estate collateral rows.
- `V5__add_ods_apart_coll_detail.sql` and account-mart docs were updated.

## Verification Summary

- `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1`: passed.
- `rg -n "@todo|TODO|FIXME" account-mart --glob "*.java" --glob "!**/build/**"`: no matches.

## Known Risks

- PostgreSQL Flyway execution was not run in this pass.
- High-volume collateral detail lookup performance and final LGD formula integration require separate integration verification.

## Rollback

- Before commit: restore `account-mart` and related `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` changes carefully.
- After commit: revert the account-mart collateral DQ/LGD commit.

---
# Latest Handoff - 2026-07-03 Loan

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: `loan` journal port boundary, journal reference value storage, Batch JobRegistry local context cleanup.
- Working tree: local uncommitted loan changes exist.

## What Changed

- `InterestAccrualService` now creates accrual journal commands through `LoanJournalPort` instead of journal-ledger `JournalUseCase` and domain objects.
- `LoanAccrualLog`, `LoanEvent`, and `EIRAmortizationSchedule` now store journal ID/slipNo values instead of holding journal-ledger entity references or returning null slip numbers.
- `V32__loan_accrual_journal_reference.sql` adds missing journal reference columns for accrual logs, loan events, and EIR schedules.
- `LoanBatchJobRegistryConfiguration` moves Job registration to `JobRegistrySmartInitializingSingleton`, removing the local Batch JobRegistry early-initialization warning.
- Loan README/docs/local-run/process-flow/schema and IntelliJ `.run` configs now use local/H2 commands with explicit `loan-api`/`loan-batch` app names and Redis repository scanning disabled.

## Verification Summary

- `.\gradlew :loan:core:test --console=plain --max-workers=1 --rerun-tasks`: passed.
- `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`: passed.
- `.\gradlew :loan:api:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-api --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --server.port=0 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain --max-workers=1`: passed.
- `.\gradlew :loan:batch:bootRun --args="--spring.profiles.active=local --spring.application.name=loan-batch --spring.data.redis.repositories.enabled=false --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain --max-workers=1`: passed.
- API/BATCH logs identify as `loan-api` and `loan-batch`; Redis repository scan logs were not reproduced.
- Batch JobRegistry BeanPostProcessor warning was not reproduced.

## Known Risks

- PostgreSQL migration execution and seeded high-volume accrual Job were not verified in this pass.
- Local smoke disables Redis repository scanning; if a future loan feature intentionally adds Redis repositories, that local option must be revisited.

## Rollback

- Before commit: restore `loan`, `.run/Loan API bootRun.run.xml`, `.run/Loan Batch Context.run.xml`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` carefully.
- After commit: revert the loan boundary refactor commit.

---
# Latest Handoff - 2026-07-03 Closing

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope in progress: `closing` core/batch boundary refactor after ECL and Journal Ledger were pushed.
- Working tree: local uncommitted closing changes exist.

## What Changed

- FX valuation and ECL provision business decisions moved from `closing:batch` into `closing:core` application services.
- Core outbound ports now hide FX rate lookup, allowance GL balance lookup, and closing journal creation.
- Batch adapters now map master-data/journal-ledger technology APIs to core ports.
- Batch configs now focus on Job/Step/Reader/Tasklet orchestration and delegate accounting decisions to core.
- Closing docs and beginner comments were updated for the new responsibility boundary.

## Verification Summary

- `.\gradlew :closing:core:test :closing:batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :closing:api:compileJava --console=plain --max-workers=1`: passed.
- `.\gradlew :closing:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false" --console=plain --max-workers=1`: passed.
- closing API/BATCH local logback XML parsing: passed.
- Closing core Spring Batch type search: no matches.
- Closing TODO/mojibake search: no matches.

## Known Risks

- PostgreSQL high-volume FX/ECL closing run is not verified in this pass.
- FX valuation writer currently logs account-level failures and continues; production skip-limit/retry/reporting policy may need a stricter adapter configuration.
- Batch BeanPostProcessor WARN remains during local context boot; it did not block startup.

## Rollback

- Before commit: restore `closing`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/ai-harness`, `docs/module-documentation-sequence.md`, and `GEMINI_REVIEW_PROMPT.md` carefully.
- After commit: revert the closing boundary refactor commit.

---
# Latest Handoff - 2026-07-03 ECL + Journal Ledger

## Current State

- Branch: `agent/asset-lease-split`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Scope ready for review: ECL core pipeline boundary refactor and Journal Ledger balance reaggregation Batch Job refactor.
- Working tree intent: commit and push this combined refactor by explicit user request.

## What Changed

- `ecl-core` owns Stage/PD, EAD/LGD, and forward-looking ECL processing order through application pipelines.
- `ecl-batch` processors now act as Spring Batch adapters and delegate to core pipelines.
- `AllowanceCalculationService` reuses the same ECL core pipelines used by batch.
- `journal-ledger:batch` now has `dailyBalanceReaggregationJob` with a Tasklet adapter and JobParameter date-range resolver.
- `journal-ledger:batch` local H2 datasource/JPA/Batch YAML and Batch test dependency were corrected.
- `journal-ledger` docs, `docs/local-development.md`, IntelliJ run config, and beginner comments were updated.

## Verification Summary

- `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --console=plain --max-workers=1`: passed.
- `.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain --max-workers=1`: passed, Job status `COMPLETED`.
- ECL/Journal Ledger Java TODO and mojibake search returned no matches.
- `git diff --check`: no whitespace errors, CRLF conversion warnings only.

## Known Risks

- PostgreSQL high-volume seeded ECL and Journal Ledger balance reaggregation runs are not verified in this pass.
- Journal Ledger Batch bootRun still logs Spring Cloud/Batch BeanPostProcessor warnings; they did not block Job completion.

## Rollback

- After commit: `git revert <commit>` for the combined ECL/Journal Ledger refactor.
- Before commit: restore the changed `ecl`, `journal-ledger`, `.run`, docs, worklog, and Gemini prompt files carefully, preserving unrelated user changes.

---
# AI Harness Handoff

## Current State

- Branch: `agent/asset-lease-split`
- Base branch: `main`
- Push target: `origin/agent/asset-lease-split`
- Current owner: Codex
- Working tree: ecl core pipeline refactor is implemented and verified locally, but not committed in this continuation.
- Verification: ecl core/batch focused tests passed.

## What Changed

- `ecl-core` now owns Stage/PD, EAD/LGD, and forward-looking ECL processing order through application pipelines:
  - `StagingCalculationPipeline`
  - `EadCrmCalculationPipeline`
  - `ForwardLookingEclCalculationPipeline`
- `ecl-batch` processors are Spring Batch adapters only. They resolve job parameters or receive chunk items, then delegate to core pipelines.
- `AllowanceCalculationService` reuses the same core pipelines so API/manual single-account calculation and batch calculation share the business sequence.
- ecl README/docs and batch config beginner comments now describe the core pipeline / batch adapter boundary.
- ecl pipeline tests were added, and the allowance use-case service test now verifies pipeline call order.

## Verification Summary

- `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1`: passed.
- `rg -n "AllowanceParameterService|PdCalculationService|LgdCalculationService|CcfCalculationService|EadCrmCalculationService|LifetimePdService|ForwardLookingEclService|BigDecimal|resolveMaturityYears|AllowanceEclResult\.builder" ecl\ecl-batch\src\main\java\com\ho\account\ecl\batch\processor`: no matches.
- `rg -n "org\.springframework\.batch|ItemProcessor|StepExecution|JobParameters|StepScope" ecl\ecl-core\src\main\java ecl\ecl-core\build.gradle`: only explanatory comment remains.
- `rg -n "@todo|TODO:" ecl --glob "*.java" --glob "!**/build/**"`: no matches.

## Known Risks

- PostgreSQL high-volume seeded ECL run is not verified in this pass.
- Gradle 9 deprecation warning remains from existing build configuration.
- Current changes are local and uncommitted unless the user asks for commit/push.

## Rollback

- Before commit: use `git restore -- ecl docs/WORKLOG.md CODEX_WORKLOG.md docs/ai-harness docs/module-documentation-sequence.md GEMINI_REVIEW_PROMPT.md` with care for any unrelated user changes.
- After commit: revert the ecl boundary refactor commit.

## Next Recommended Steps

1. Run `git diff --check` before committing.
2. Commit/push the ecl boundary refactor if requested.
3. Continue the sequential module review with `journal-ledger` after ecl.
