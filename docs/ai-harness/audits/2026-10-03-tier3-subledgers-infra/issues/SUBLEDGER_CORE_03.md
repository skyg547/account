# [payable] 동시 지급 런이 동일 채무의 지급 후보를 중복 생성할 수 있음

- **Issue ID:** SUBLEDGER_CORE_03
- **Priority:** P1
- **Module:** payable
- **Area / category:** core / bug
- **Labels:** module:payable, area:core, type:bug, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:71
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:88
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:135
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:144
- payable/core/src/main/java/com/ho/account/expenditure/adapter/out/persistence/PayablePersistenceAdapter.java:33
- payable/core/src/main/java/com/ho/account/expenditure/repository/PayableRepository.java:14
- payable/core/src/main/resources/db/payable-migration/V1__payable_baseline.sql:68
- payable/core/src/test/java/com/ho/account/expenditure/application/service/PaymentServiceTest.java:290

## Problem statement
지급 런의 중복 방어는 기존 런 조회와 채무의 `IN_PAYMENT` 상태 확인에 의존한다. 채무 조회·적격성 확인·상태 저장이 DB 조건부 갱신이나 잠금 없이 별도 단계로 수행된다. 서로 다른 트랜잭션이 같은 `OPEN` 채무를 읽으면 둘 다 지급 후보를 만들 수 있다. `payments` 테이블은 동일 `payable_id`의 미완료 지급 후보 중복을 막는 제약도 없다.

## Reproduction / evidence
동시 요청 T1/T2가 72~83행에서 아직 존재하지 않는 런을 조회하거나 서로 다른 런 키를 사용하고, 88~99행에서 같은 채무를 후보로 읽는다. 각 트랜잭션이 136~142행에서 저장 전 `OPEN` 상태를 확인한 뒤 145~155행에서 `IN_PAYMENT`로 저장하고 별도 Payment를 삽입한다. 영속성 어댑터는 일반 `save`/`findById`만 사용하고 저장소에 잠금·조건부 claim·버전 필드가 없다. 기존 멱등성 테스트는 이미 `PROCESSING`인 런을 순차 조회하는 사례만 검증한다. 이 interleaving은 정적 분석에 의한 동시성 재현 시나리오이며 실제 병렬 DB 테스트는 아직 없다.

## Financial / architectural impact
한 채무에 두 지급 지시가 생긴다. 각 지급은 서로 다른 Payment ID를 멱등 키로 사용하므로 각각 실행되면 외부 송금 중복을 막지 못한다. 첫 성공 뒤 두 번째가 잔액 초과로 로컬 실패하더라도 외부 지급은 이미 발생할 수 있다.

## Proposed solution
허용 파일(정확히 5개): `payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java`, `payable/core/src/main/java/com/ho/account/expenditure/application/port/out/PayablePersistencePort.java`, `payable/core/src/main/java/com/ho/account/expenditure/adapter/out/persistence/PayablePersistenceAdapter.java`, `payable/core/src/main/java/com/ho/account/expenditure/repository/PayableRepository.java`, `payable/core/src/test/java/com/ho/account/expenditure/application/service/PaymentServiceTest.java`. 채무 ID와 허용 상태·양수 잔액을 조건으로 `IN_PAYMENT`로 바꾸는 DB 원자적 claim을 포트에 추가한다. claim이 1행일 때만 Payment를 생성하고 0행이면 건너뛴다. 허용된 단위 테스트는 1행·0행 결과에 따른 서비스 분기를 검증하고, DB 동시성은 아래 별도 완료 게이트에서 확인한다.

## Acceptance criteria
- [ ] 단위 테스트에서 claim 성공 한 번과 실패 한 번의 분기가 Payment 한 건 생성 및 중복 방어로 이어진다.
- [ ] claim 실패 경로는 외부 지급 호출이나 채무 잔액 변경 없이 종료된다.
- [ ] 완료 전 PostgreSQL 동시 요청 검증에서 동일 채무의 Payment가 한 건뿐임을 확인하고, 기존 순차 재호출·다른 채무의 정상 런·실패 후 재시작 흐름도 회귀 검증한다.

## Test gap and verification limits
현재 `PaymentServiceTest.initiatePaymentRunIdempotencyPreventsDuplicatePayments`는 기존 런을 미리 반환하도록 stub한 순차 시나리오다. 대상 단위 검증 명령: `./gradlew :payable:core:test --tests com.ho.account.expenditure.application.service.PaymentServiceTest --offline --no-daemon --console=plain --max-workers=2`. 이 Mockito 테스트는 DB 원자성을 증명하지 못한다. 별도 완료 게이트로 승인된 PostgreSQL 환경에서 같은 채무를 대상으로 두 지급 런 요청을 겹쳐 실행하고 `payments.payable_id`별 건수를 확인해야 한다. 이 통합 검증은 제시된 5파일 구현 allowlist의 자동화 테스트 범위에 포함되지 않으며 현재 감사에서 수행하지 않았다.
