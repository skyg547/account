# [deposit] 입출금 잔액 변경에 대응하는 분개 outbox가 없다

- **Issue ID:** BANKING_CREDIT_01
- **Priority:** P1
- **Module:** deposit
- **Area / category:** core / financial
- **Labels:** module:deposit, area:core, type:financial, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- deposit/core/src/main/java/com/ho/account/deposit/application/service/DepositService.java:123
- deposit/core/src/main/java/com/ho/account/deposit/application/service/DepositService.java:139
- deposit/core/src/main/java/com/ho/account/deposit/application/service/DepositService.java:186
- deposit/api/src/main/java/com/ho/account/deposit/adapter/in/web/DepositController.java:55
- deposit/core/src/test/java/com/ho/account/deposit/application/service/DepositServiceConcurrencyTest.java:67

## Problem statement
`deposit`와 `withdraw`는 계좌를 읽고 잔액을 바꿔 `save`하는 것으로 종료한다. 계좌 개설의 초기입금에만 적용되는 `postInitialDepositJournalWithOutbox`를 호출하지 않는다. 따라서 정상 HTTP 200 입출금에 대응하는 현금/예금부채 분개 outbox가 없다. 두 메서드에는 서비스 트랜잭션 경계도 없어 잔액과 outbox를 원자적으로 묶을 수 없다.

## Reproduction / evidence
`POST /api/deposits/accounts/{accountNumber}/deposit` 또는 `/withdraw` 요청은 `DepositController`에서 `DepositTransactionUseCase`로 전달되고, `DepositService` 124–146행은 `DepositAccount.deposit/withdraw`와 계좌 `save`만 수행한다. 반면 86–110행의 `openAccount`는 초기입금이 있으면 186–225행의 전표 outbox 경로를 호출한다. 기존 동시성 테스트는 최종 잔액만 확인한다.

## Financial / architectural impact
현금 유입·유출로 바뀐 예금부채 잔액이 총계정원장과 대사되지 않는다. 전표 계보가 없어 중복 요청이나 장애 후 어느 잔액 변경이 분개됐는지 감사·복구할 수 없다. 마감 기간 분개 검증도 이 경로에서는 실행되지 않는다.

## Proposed solution
수정 allowlist(4파일): `deposit/core/src/main/java/com/ho/account/deposit/application/service/DepositService.java`, `deposit/core/src/test/java/com/ho/account/deposit/application/service/DepositServiceTest.java`, `deposit/core/src/test/java/com/ho/account/deposit/application/service/DepositServiceTransactionIntegrationTest.java`(신규), `deposit/README.md`. 기존 outbox·계정 매핑을 사용해 입금은 현금 차변/예금부채 대변, 출금은 역방향 분개를 서로 다른 거래 계보 ID로 저장한다. 낙관 잠금 충돌은 실패한 DB 트랜잭션 밖에서 재시도하고, 잔액·outbox 저장을 하나의 새 트랜잭션으로 묶는다. 외부 전표 발행은 커밋 후 릴레이한다. `deposit_transactions` 영속 이력과 HTTP 요청 멱등 키는 이 수정 범위 밖의 잔여 결함이다.

## Acceptance criteria
- [ ] 입금·출금 성공 시 계좌 잔액과 고유 거래 계보의 균형 잡힌 전표 outbox가 함께 커밋된다.
- [ ] outbox 저장 실패와 낙관 잠금 충돌 시 해당 시도의 잔액 변경이 남지 않으며, 재시도는 새 트랜잭션에서 최신 잔액을 검증한다.
- [ ] 입금·출금 각각의 분개 방향, 실패 원자성, 충돌 재시도를 테스트하고 마감된 회계일자의 전표 거부·복구 경계를 명시한다.

## Test gap and verification limits
`DepositServiceTest`는 계좌 개설의 초기입금 전표만 검증하고, `DepositServiceConcurrencyTest`는 동시 입금의 최종 잔액만 검증한다. 입출금 outbox·분개·마감 거부 테스트가 없다. 감사 시 기존 `deposit:core:test` 93건이 통과했지만 이 결함을 검출하는 단언은 없다. 수정 후 검증 명령: `./gradlew :deposit:core:test :deposit:api:test --offline --no-daemon --console=plain`; PostgreSQL 트랜잭션·Journal 대사 통합 검증도 필요하다. 이 감사에서는 운영 DB와 실제 Journal을 실행하지 않았다.
