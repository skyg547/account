# [asset-lease] 리스 재측정이 계약 조건만 변경하고 리스부채·사용권자산·상환표를 갱신하지 않음

- **Issue ID:** RECONCILIATION_ASSETS_02
- **Priority:** P1
- **Module:** asset-lease
- **Area / category:** core / financial
- **Labels:** module:asset-lease, area:core, type:financial, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:121
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:126
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:130
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:154
- asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java:195
- asset-lease/core/src/main/java/com/ho/account/asset/infrastructure/persistence/LeasePersistenceAdapter.java:56

## Problem statement
remeasureLease는 월 리스료·종료일·할인율을 계약 엔티티에 설정해 저장하고 IFRS16_REMEASUREMENT 이벤트를 보낸 뒤 반환한다. 최초 인식 때만 수행하는 현재가치 계산, 리스부채·사용권자산 조정, 미래 지급 스케줄 재작성은 호출하지 않는다. 이후 월별 회계는 기존 SCHEDULED 스케줄의 이자·원금을 사용하므로 변경된 계약 조건과 장부 잔액이 계속 분리된다.

## Reproduction / evidence
1~12월, 월 리스료 1,200.00, 할인율 6%인 IFRS 16 계약을 등록하면 61~84행이 최초 PV, 사용권자산, 부채 및 12건 스케줄을 만든다. 6월에 월 리스료를 2,400.00으로 재측정하면 126~139행은 계약의 monthlyPayment만 변경하고 정상 반환한다. 154~179행은 재측정 전 생성된 스케줄을 찾아 종전 원금만 부채에서 차감하고, 181~192행 이벤트에도 종전 이자·원금·지급액을 넣는다. LeaseEntryServiceTest.java:80~101은 날짜 역전 거부만 검증하며, 정상 재측정 뒤 잔액·스케줄·상각액 검증은 없다.

## Financial / architectural impact
계약의 현금흐름과 리스부채 현재가치, 미래 이자비용·원금상환, 사용권자산 장부가액 및 향후 상각액이 불일치한다. API는 성공 응답을 주므로 월별 전표 이벤트도 낡은 금액으로 발행되어 리스 회계 명세와 총계정원장 사이에 실질적인 차이를 만들 수 있다.

## Proposed solution
재측정 기준일과 이미 처리된 회차를 검증하고, 남은 현금흐름의 PV 및 조정액을 도메인 계산 정책으로 산출한다. 같은 DB 트랜잭션에서 현재 리스부채·사용권자산과 남은 기간의 상각 기준액을 조정한다. 기존 포트의 회차 조회·저장을 사용해 PAID 회차는 보존하고, 미래 SCHEDULED 회차는 새 금액으로 갱신하며 단축 회차는 CANCELLED로 표시하고 연장 회차를 추가한다. 이벤트에는 기준일과 조정액을 담는다. 현재 Kafka 발행은 DB 트랜잭션과 원자적으로 묶이지 않으므로 그 보장은 별도 작업으로 다룬다. 정확한 파일 allowlist (5개):
1. asset-lease/core/src/main/java/com/ho/account/asset/application/service/LeaseEntryService.java
2. asset-lease/core/src/main/java/com/ho/account/asset/domain/LeaseContract.java
3. asset-lease/core/src/main/java/com/ho/account/asset/domain/RightOfUseAsset.java
4. asset-lease/core/src/test/java/com/ho/account/asset/application/service/LeaseEntryServiceTest.java
5. asset-lease/docs/process-flow.md

## Acceptance criteria
- [ ] 리스료·할인율·종료일 변경 후 현재 리스부채, 사용권자산, 미래 이자·원금·지급액이 재계산 결과와 일치한다.
- [ ] 이미 PAID인 회차와 과거 잔액은 보존되고, 같은 기준일 재시도 또는 처리 완료 기간으로의 소급 재측정은 중복 미래 스케줄을 만들지 않는다.
- [ ] 정상 재측정·연장·단축·장애 롤백 회귀 테스트가 통과하고 다음 월별 이벤트가 새 스케줄 금액을 사용한다.

## Test gap and verification limits
기존 LeaseEntryServiceTest는 정상 등록 PV, 월별 처리, 잘못된 재측정 날짜를 확인하지만 정상 재측정의 금융 상태를 확인하지 않는다. 수정 전/후에는 ./gradlew :asset-lease:core:test --offline --no-daemon --console=plain --max-workers=2를 실행하고, 서비스·도메인 변경이 API·Batch 경로에 미치는 영향을 컴파일로 추가 확인한다. 이번 감사는 코드 경로와 기존 테스트만 확인했으며 실제 Kafka 전송·외부 전표 반영의 원자성은 별도 검증 게이트다.
