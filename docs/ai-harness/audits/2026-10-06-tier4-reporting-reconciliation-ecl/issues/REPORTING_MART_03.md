# [account-mart] GL/SL 대사 불일치에도 통합 배치가 스냅샷과 준비 완료 이벤트까지 진행

- **Issue ID:** REPORTING_MART_03
- **Priority:** P1
- **Module:** account-mart
- **Area / category:** batch / financial
- **Labels:** module:account-mart, area:batch, type:financial, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/ods/audit/service/OdsReconciliationService.java:52
- account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/ods/audit/service/OdsReconciliationService.java:58
- account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/ods/audit/service/OdsReconciliationService.java:75
- account-mart/mart-batch/src/main/java/com/ho/account/mart/batch/tasklet/OdsReconcileTasklet.java:38
- account-mart/mart-batch/src/main/java/com/ho/account/mart/batch/job/ods/IntegratedPositionEtlJobConfig.java:76

## Problem statement
`reconcileGlToSl`은 GL/SL 차이가 허용오차 0.01을 넘으면 경고를 남기고 `MISMATCH` 이력을 저장하지만 실패 결과를 호출자에게 전달하지 않는다. 게다가 SL 요약만 순회하므로 GL에만 존재하는 계정·통화 키는 검사하지 않는다. `OdsReconcileTasklet`은 반환 직후 `FINISHED`를 반환한다.

## Reproduction / evidence
같은 기준일·계정·통화에 SL 100, GL 90을 주면 56–75행이 차이 -10과 `MISMATCH`를 기록한 뒤 정상 반환한다. `OdsReconcileTasklet` 38–41행도 정상 종료하고 `IntegratedPositionEtlJobConfig` 76–79행은 CDM 적재, exposure snapshot 생성, 준비 완료 이벤트 단계로 이동한다. GL 90만 있고 대응 SL 행이 없는 경우에는 52행 루프 자체가 실행되지 않는다. 기존 `IntegratedPositionEtlJobTest` 95–105행은 정상 대사 건수와 스냅샷 건수만 검사한다. 불일치 배치 실행은 아직 재현 테스트로 검증하지 않았다.

## Financial / architectural impact
원천 원장과 GL이 맞지 않는 기준일을 정상 마트 준비 완료로 알릴 수 있다. 후속 ECL 산출이 대사되지 않은 익스포저를 사용하고, GL 전용 키는 감사 이력에서도 빠질 수 있다.

## Proposed solution
원자적 수정 allowlist (4파일):
1. `account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/ods/audit/service/OdsReconciliationService.java` — GL/SL 키의 합집합을 비교하고 차이 건수를 반환한다. 배치 Step 트랜잭션과 별도 `REQUIRES_NEW` 트랜잭션에서 대사 이력을 커밋한 뒤 반환한다.
2. `account-mart/mart-batch/src/main/java/com/ho/account/mart/batch/tasklet/OdsReconcileTasklet.java` — 대사 결과에 불일치가 있으면 서비스 반환 후 예외를 던져 Step을 실패시킨다. 이 순서로 실패 시에도 커밋된 대사 이력을 보존한다.
3. `account-mart/mart-batch/src/test/java/com/ho/account/mart/batch/job/ods/IntegratedPositionEtlJobTest.java` — 불일치 및 GL 전용 키에서 Job 실패, 후속 Step 미실행, 이력 보존을 검증한다.
4. `account-mart/docs/DATA_MART_SPEC.md` — 대사 임계값, 누락 키와 실패 시 재실행 조건을 설명한다.

## Acceptance criteria
- [ ] GL/SL 양방향 계정·통화 키를 비교하여 0.01 초과 차이와 한쪽 전용 키를 판정한다.
- [ ] 대사 불일치 이력은 보존되며 통합 Job은 실패하고 CDM 적재·스냅샷·준비 완료 이벤트를 실행하지 않는다.
- [ ] 정합한 기준일은 기존처럼 진행하고, 불일치 기준일과 GL 전용 키는 자동 테스트에서 재현된다.

## Test gap and verification limits
현재 배치 통합 테스트는 모두 일치하는 seed와 `COMPLETED`만 확인하고 불일치·GL 전용 키·재시작을 검증하지 않는다. 수정 후 `./gradlew --offline :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest --console=plain --max-workers=1 --no-daemon`을 실행한다. 감사 중 이 배치 테스트는 미실행이므로 실패 전파와 이력 보존은 코드 분석에 근거한 결함 및 수용 조건으로 구분한다.
