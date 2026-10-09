# [asset-lease] 사용권자산의 월 상각액 반올림 잔액을 마지막 회차에 상각하지 않음

- **Issue ID:** RECONCILIATION_ASSETS_03
- **Priority:** P2
- **Module:** asset-lease
- **Area / category:** core / financial
- **Labels:** module:asset-lease, area:core, type:financial, priority:p2, agent-loop, status:ready, spec-driven

## Code reference
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:71
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:154
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:169
- asset-lease/core/src/main/java/com/ho/account/asset/domain/RightOfUseAsset.java:95
- asset-lease/core/src/main/java/com/ho/account/asset/domain/RightOfUseAsset.java:100
- asset-lease/core/src/main/java/com/ho/account/asset/domain/LeaseContract.java:128

## Problem statement
최초 인식은 현재가치를 계약 개월 수로 나눠 소수 둘째 자리로 반올림한 동일 월 상각액을 저장한다. 월별 처리와 도메인 depreciate()는 마지막 회차인지 구분하지 않고 그 월액만 차감한다. 반올림 월액 × 회차 수가 최초 가액보다 작으면 마지막 예정 회차가 PAID로 닫힌 뒤에도 사용권자산 장부가액이 남고 ACTIVE 상태가 유지된다.

## Reproduction / evidence
시작일 2026-01-01, 종료일 2026-03-31, 월 리스료 33.39, 연 할인율 1%인 유효 계약은 LeaseContract.calculatePresentValue로 PV 100.00을 만든다. 71~75행은 월 상각액을 100.00 / 3 = 33.33으로 저장한다. 1~3월 각각 95~125행은 33.33씩만 상각하여 세 번째 PAID 스케줄 후 장부가액 0.01, 누적상각액 99.99, 상태 ACTIVE가 남는다. 154~161행은 SCHEDULED 스케줄이 없으면 즉시 반환하므로 이 0.01을 후속 월에도 정리할 수 없다. RightOfUseAssetTest.java:61~77은 월액이 잔액보다 큰 경우만 확인하고 이 반올림 잔액 사례는 없다.

## Financial / architectural impact
리스가 종료되어 모든 예정 지급 회차를 처리해도 사용권자산 0.01이 장부에 계속 남는다. 계약별 잔액은 작아도 계약 수가 많으면 자산·누적상각 및 감가상각비 집계가 체계적으로 어긋나며, FULLY_DEPRECIATED 상태 전이도 누락된다.

## Proposed solution
서비스가 해당 계약의 마지막 미처리 지급 회차를 식별해 도메인에 마지막 회차 정보를 전달하고, 도메인이 마지막 회차에는 남은 장부가액 전액을 안전 상각액으로 사용하도록 한다. 중간 회차 월액과 음수 방지 규칙은 유지한다. 정확한 파일 allowlist (5개):
1. asset-lease/core/src/main/java/com/ho/account/asset/domain/RightOfUseAsset.java
2. asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java
3. asset-lease/core/src/test/java/com/ho/account/asset/domain/RightOfUseAssetTest.java
4. asset-lease/core/src/test/java/com/ho/account/asset/application/service/LeaseEntryServiceTest.java
5. asset-lease/docs/process-flow.md

## Acceptance criteria
- [ ] 위 3개월 사례에서 상각액이 33.33, 33.33, 33.34가 되고 마지막 장부가액은 0.00, 상태는 FULLY_DEPRECIATED다.
- [ ] 중간 회차와 월액이 잔액을 초과하는 기존 안전 상각 동작을 보존하고 재실행 시 추가 상각이 없다.
- [ ] 마지막 회차 판단 및 반올림 잔액 회귀 테스트가 도메인·서비스 양쪽에서 통과한다.

## Test gap and verification limits
기존 테스트는 정상 한 회차와 초과 상각 시 0원 하한을 확인하지만, 반올림된 월액을 전체 계약 기간에 적용한 누적 결과를 확인하지 않는다. 수정 전/후에는 ./gradlew :asset-lease:core:test --offline --no-daemon --console=plain --max-workers=2를 실행한다. 이번 감사의 100.00/33.33/0.01 값은 코드 수식과 Decimal 재계산으로 확인했으며 실제 DB·Kafka 통합 실행은 하지 않았다.
