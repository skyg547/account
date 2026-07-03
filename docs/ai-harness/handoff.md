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
