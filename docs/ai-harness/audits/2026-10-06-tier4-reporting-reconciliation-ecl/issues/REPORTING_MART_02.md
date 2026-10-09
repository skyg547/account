# [account-mart] 기준일 외화 환율 누락 시 원금을 KRW 평가금액으로 저장

- **Issue ID:** REPORTING_MART_02
- **Priority:** P1
- **Module:** account-mart
- **Area / category:** core / financial
- **Labels:** module:account-mart, area:core, type:financial, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/mart/processor/IntegratedPositionProcessor.java:52
- account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/mart/processor/IntegratedPositionProcessor.java:109
- account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/mart/processor/IntegratedPositionProcessor.java:121
- account-mart/mart-batch/src/main/java/com/ho/account/mart/batch/job/ods/IntegratedPositionEtlJobConfig.java:210
- account-mart/mart-batch/src/test/java/com/ho/account/mart/batch/job/ods/IntegratedPositionEtlJobTest.java:89

## Problem statement
`convertToKrw`는 외화 환율 조회가 비면 외화 원금에 `setScale(4)`만 적용하여 반환한다. 호출자는 그 결과를 환산된 `marketValue`로 CDM 포지션에 넣는다. 환율이 1:1이라는 근거가 없는데도 정상 값처럼 영속화된다.

## Reproduction / evidence
기준일 USD 10,000 포지션에 `ExchangeRateRepository`가 `Optional.empty()`를 반환하면 115–121행의 `orElseGet`이 10,000.0000을 반환하고, 52·64행은 이를 `marketValue`로 저장할 엔티티에 전달한다. 환율 1,350이 있는 기존 통합 테스트는 13,500,000.0000을 확인하지만 환율이 없는 사례는 없다. 배치의 CDM Step은 예외가 없는 이 경로를 정상 적재한다. 이는 코드에서 확정되는 동작이며 실제 배치 재현 테스트는 아직 없다.

## Financial / architectural impact
환율 데이터가 빠진 날 외화 포지션의 원화 평가액이 과소 또는 과대 계상되어 마트 조회와 이를 사용하는 후속 계산의 입력이 오염된다. 배치 성공 여부만으로 누락을 발견할 수 없다.

## Proposed solution
원자적 수정 allowlist (4파일):
1. `account-mart/mart-core/src/main/java/com/ho/account/mart/core/domain/mart/processor/IntegratedPositionProcessor.java` — KRW 외 통화는 해당 기준일의 유효 환율을 필수로 요구하고 없거나 유효하지 않으면 금액 변환을 실패시킨다. 배치의 일반 `IllegalArgumentException` skip에 흡수되지 않는 실패를 사용한다.
2. `account-mart/mart-core/src/test/java/com/ho/account/mart/core/domain/mart/processor/IntegratedPositionProcessorTest.java` — 환율 존재·누락·무효 값 테스트를 추가한다.
3. `account-mart/mart-batch/src/test/java/com/ho/account/mart/batch/job/ods/IntegratedPositionEtlJobTest.java` — 환율 누락 배치가 `COMPLETED`가 아님을 검증한다.
4. `account-mart/docs/ETL_INTERFACE_SPEC.md` — 외화 환율 필수 조건과 배치 실패 동작을 명시한다.

## Acceptance criteria
- [ ] 기준일 환율이 있는 외화는 `BigDecimal` 환산·반올림 정책대로 저장된다.
- [ ] 외화 환율이 없거나 유효하지 않으면 1:1 평가금액을 만들지 않고 CDM Step/Job이 실패한다.
- [ ] 환율 누락·무효 경계가 core 및 배치 테스트에서 재현된다.

## Test gap and verification limits
기존 `IntegratedPositionProcessorTest`는 KRW 사례만 다루고, `IntegratedPositionEtlJobTest`는 USD 환율을 항상 seed한다. `./gradlew --offline :account-mart:mart-core:test --tests com.ho.account.mart.core.domain.mart.processor.IntegratedPositionProcessorTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest --console=plain --max-workers=1 --no-daemon`로 수정 후 확인한다. 감사 시 기존 core 단위 테스트는 통과했고 배치 통합 테스트 및 누락 재현은 미실행이다.
