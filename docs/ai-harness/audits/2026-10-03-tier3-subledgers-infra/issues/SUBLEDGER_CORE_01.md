# [payable] 다른 공급업체의 선급금으로 매입채무를 상계할 수 있음

- **Issue ID:** SUBLEDGER_CORE_01
- **Priority:** P1
- **Module:** payable
- **Area / category:** core / financial
- **Labels:** module:payable, area:core, type:financial, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:232
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:238
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:243
- payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java:318
- payable/core/src/main/java/com/ho/account/expenditure/domain/AdvancePayment.java:56
- payable/core/src/test/java/com/ho/account/expenditure/application/service/PaymentServiceTest.java:170

## Problem statement
`offsetPayableWithAdvancePayment`는 요청의 두 ID로 채무와 선급금을 각각 조회한 뒤 금액만 확인한다. 두 객체의 `vendorCode`가 같은지 검사하지 않고 양쪽 잔액을 차감한다. 상계 전표도 채무의 공급업체 코드만 사용하므로 선급금 원래 소유자를 전표에 반영하지 않는다.

## Reproduction / evidence
서로 다른 `vendorCode`를 가진 채무 A와 선급금 B의 잔액이 각각 100 이상일 때 `OffsetPayableCommand(A.id, B.id, 100.00)`을 호출하면 238~245행에서 두 객체를 그대로 차감하고 247~250행에서 저장 및 전표 초안을 요청한다. `AdvancePayment.applyOffset`은 소유자 확인 없이 양수·잔액 상한만 검사한다. 현재 상계 테스트는 양쪽 모두 `V001`인 성공 경로만 검증하며 공급업체 불일치 거부 사례가 없다. 이는 코드 흐름에 따른 정적 재현이며 실제 DB 전표까지의 실행 재현은 수행하지 않았다.

## Financial / architectural impact
A 공급업체의 채무가 B 공급업체의 선급금으로 소멸해 거래처별 보조원장과 선급금 잔액이 틀어진다. GL 상계 전표는 A의 코드로 차변·대변을 모두 기록해 B의 선급금 차감을 추적할 수 없다.

## Proposed solution
허용 파일(정확히 3개): `payable/core/src/main/java/com/ho/account/expenditure/application/service/PaymentService.java`, `payable/core/src/test/java/com/ho/account/expenditure/application/service/PaymentServiceTest.java`, `payable/docs/process-flow.md`. 두 객체의 공급업체 코드 일치를 **첫 상태 변경 전에** 검증하고 불일치 시 업무 예외로 거부한다. 동일 업체의 부분·전액 상계는 유지하고 문서에 소유권 불변식을 명시한다.

## Acceptance criteria
- [ ] 공급업체가 다른 채무와 선급금의 상계 요청은 양쪽 잔액·상태·전표를 변경하지 않고 거부된다.
- [ ] 동일 공급업체의 부분·전액 상계는 기존 금액 및 상태 전이와 GL 금액을 유지한다.
- [ ] 서비스 회귀 테스트가 불일치 거부와 전표 포트 미호출을 검증한다.

## Test gap and verification limits
기존 `PaymentServiceTest`의 상계 사례는 단일 공급업체 성공 경로만 사용한다. 대상 검증 명령: `./gradlew :payable:core:test --tests com.ho.account.expenditure.application.service.PaymentServiceTest --offline --no-daemon --console=plain --max-workers=2`. 실제 PostgreSQL·원격 GL 연동은 별도 검증이 필요하다.
