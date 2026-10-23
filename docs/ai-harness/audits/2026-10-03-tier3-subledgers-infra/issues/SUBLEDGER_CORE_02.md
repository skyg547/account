# [receivable] 다른 고객의 수납으로 매출채권을 수동 반제할 수 있음

- **Issue ID:** SUBLEDGER_CORE_02
- **Priority:** P1
- **Module:** receivable
- **Area / category:** core / financial
- **Labels:** module:receivable, area:core, type:financial, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- receivable/api/src/main/java/com/ho/account/receivable/api/adapter/in/web/CollectionController.java:57
- receivable/core/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:113
- receivable/core/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:122
- receivable/core/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:138
- receivable/core/src/main/java/com/ho/account/receivable/application/service/CollectionService.java:203
- receivable/core/src/main/resources/db/receivable-migration/V1__receivable_baseline.sql:71
- receivable/core/src/test/java/com/ho/account/receivable/application/service/CollectionServiceTest.java:171

## Problem statement
수동 매칭은 `collectionId`, `receivableId`, 금액을 받아 두 잔액만 확인한다. `collection.customerCode`와 `receivable.customerCode`의 일치 여부를 검사하지 않는다. `collection_allocations`의 외래키도 두 행의 존재만 보장해 고객 교차 매칭을 막지 못한다.

## Reproduction / evidence
고객 A의 미배분 수납 100과 고객 B의 미수채권 100을 생성한 뒤 `/api/collections/manual-match`에 두 ID와 `100.00`을 제출하면 117~130행의 조회·잔액 검사를 통과한다. `processMatch`는 양쪽 잔액 및 인보이스 상태를 바꾸고 배분 이력과 GL 초안을 생성한다. GL 라인은 203~214행에서 수납 고객 A의 코드로만 작성된다. 현재 테스트는 동일 고객 `C001`의 정상 매칭만 검사한다. 이는 정적 코드 경로로 입증한 결함이며 실제 HTTP/DB 실행은 검증하지 않았다.

## Financial / architectural impact
고객 A의 현금 입금이 고객 B의 채권을 소멸시켜 거래처별 채권 보조원장과 배분 이력이 어긋난다. GL의 거래처 코드가 A로 기록되는 동안 B의 인보이스는 지급 완료 또는 부분 지급으로 바뀔 수 있다.

## Proposed solution
허용 파일(정확히 3개): `receivable/core/src/main/java/com/ho/account/receivable/application/service/CollectionService.java`, `receivable/core/src/test/java/com/ho/account/receivable/application/service/CollectionServiceTest.java`, `receivable/docs/process-flow.md`. 공통 `processMatch` 진입 시 두 고객 코드의 일치를 **상태 변경 전에** 검증해 수동·자동 경로를 함께 보호한다. 정상 부분 매칭 및 GL 거래처 코드는 유지한다.

## Acceptance criteria
- [ ] 고객이 다른 수납·채권의 수동 매칭은 잔액·상태·배분 이력·전표를 변경하지 않고 거부된다.
- [ ] 같은 고객의 수동·자동 부분 매칭은 기존 잔액 및 인보이스 상태 전이를 유지한다.
- [ ] 회귀 테스트가 교차 고객 거부와 저장·전표 포트 미호출을 검증한다.

## Test gap and verification limits
기존 `CollectionServiceTest.manualMatchCollectionUsesMappedAccounts`는 동일 고객의 성공 사례만 다룬다. 대상 검증 명령: `./gradlew :receivable:core:test --tests com.ho.account.receivable.application.service.CollectionServiceTest --offline --no-daemon --console=plain --max-workers=2`. 실제 PostgreSQL·원격 GL 연동은 별도 검증이 필요하다.
