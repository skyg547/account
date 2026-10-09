# [ecl] 월말 snapshot에서 빠진 계좌가 다음 평가의 ECL에 계속 포함됨

- **Issue ID:** RISK_ECL_01
- **Priority:** P1
- **Module:** ecl
- **Area / category:** batch / financial
- **Labels:** module:ecl, area:batch, type:financial, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/JdbcAllowanceExposureSyncAdapter.java:120
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/JdbcAllowanceExposureSyncAdapter.java:167
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/CrBatchQueryProvider.java:23
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/pipeline/AllowanceDataQualityService.java:43
- ecl/ecl-batch/src/main/java/com/ho/account/ecl/batch/config/BatchInfrastructureConfig.java:198
- ecl/ecl-batch/src/main/java/com/ho/account/ecl/batch/config/AllowanceStagingBatchConfig.java:141
- ecl/ecl-core/src/test/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/JdbcAllowanceExposureSyncAdapterTest.java:60

## Problem statement
기준일 snapshot 동기화는 해당 날짜의 계좌만 `cr_accounts`에 upsert하고, 이번 snapshot에 없는 기존 활성 계좌를 비활성화하거나 산출 대상에서 제외하지 않는다. 이후 Stage reader는 `is_active = true`와 ID 범위만 검사하며 `baseDate` 또는 현재 snapshot 존재 여부를 검사하지 않는다. 따라서 상환·해지 등으로 이번 월말 snapshot에서 사라진 계좌가 이전 잔액으로 다시 평가된다.

## Reproduction / evidence
예를 들어 4월 snapshot에 `ACC-A`, `ACC-B`가 있고 5월 snapshot에는 `ACC-A`만 있다고 하자. 4월 동기화 후 두 계좌가 활성화된다. 5월 동기화 SQL은 `ACC-A`만 갱신하고 `ACC-B`를 그대로 둔다(`JdbcAllowanceExposureSyncAdapter.java:163`, `:167`). `CrBatchQueryProvider.java:23-30`은 두 활성 계좌를 모두 반환하고, `AllowanceStagingBatchConfig.java:141-145`는 이를 5월 산출 결과로 저장한다. 기존 adapter 테스트는 같은 기준일 재동기화만 확인하며 월 변경 시 누락 계좌 제외를 확인하지 않는다(`JdbcAllowanceExposureSyncAdapterTest.java:60-81`).

## Financial / architectural impact
이번 기준일에 존재하지 않는 익스포저의 ECL과 원금이 `allowance_ecl_results` 및 `allowance_summary`에 포함되어 목표 대손충당금과 closing의 분개 입력을 과대 계상할 수 있다. 결과에 이번 snapshot과의 계좌별 대사가 없어 정상 배치 완료로 보일 수 있다.

## Proposed solution
Stage reader의 QueryDSL 조회에 `baseDate`를 전달하고, 해당 기준일 `allowance_exposure_snapshots.source_account_no`가 존재하는 활성 계좌만 읽도록 `EXISTS` 조건을 추가한다. 기존 `cr_accounts` 상태와 다른 CDM 적재 경로는 변경하지 않는다. **정확한 수정 allowlist (5파일):** `ecl/ecl-core/src/main/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/CrBatchQueryProvider.java`, `ecl/ecl-batch/src/main/java/com/ho/account/ecl/batch/config/BatchInfrastructureConfig.java`, `ecl/ecl-core/src/main/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/jpa/AllowanceExposureSnapshotReadEntity.java` (신규), `ecl/ecl-core/src/main/java/com/ho/account/ecl/core/infrastructure/adapter/persistence/jpa/AllowanceExposureSnapshotReadId.java` (신규), `ecl/ecl-batch/src/test/java/com/ho/account/ecl/batch/AllowanceEclBatchIntegrationTest.java`.

## Acceptance criteria
- [ ] 연속 두 기준일에서 두 번째 snapshot에 없는 계좌는 두 번째 `allowance_ecl_results` 및 `allowance_summary`에 포함되지 않는다.
- [ ] 같은 기준일 재실행은 결과를 중복 적재하지 않으며 해당 snapshot의 DQ 적격 계좌만 Stage 입력으로 읽는다.
- [ ] 다른 CDM 경로의 계좌 상태는 바뀌지 않으며 DQ 부적격 계좌는 기존 규칙대로 Stage 입력에서 제외된다.

## Test gap and verification limits
현재 `JdbcAllowanceExposureSyncAdapterTest`는 같은 기준일의 upsert 멱등성만 검증하고, 월간 snapshot에서 계좌가 빠지는 경우를 다루지 않는다. 감사 중 기존 대상 테스트는 `./gradlew :ecl:ecl-core:test --tests 'com.ho.account.ecl.core.application.service.allowance.AllowanceSummaryServiceTest' --tests 'com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncServiceTest' --tests 'com.ho.account.ecl.core.infrastructure.adapter.persistence.JdbcAllowanceExposureSyncAdapterTest' --tests 'com.ho.account.ecl.core.application.service.calculation.ForwardLookingEclServiceTest' --offline --no-daemon --console=plain --max-workers=1`로 통과했지만 결함 시나리오는 포함하지 않는다. 수정 후 `./gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --offline --no-daemon --console=plain --max-workers=1`로 월간 탈락 및 집계 회귀를 검증한다. 실제 PostgreSQL 대량 snapshot과 closing 연계 실행은 이번 감사에서 수행하지 않았다.
