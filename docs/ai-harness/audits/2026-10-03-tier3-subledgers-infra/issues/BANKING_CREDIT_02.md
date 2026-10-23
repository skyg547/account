# [loan] 비월별 상환 주기를 받아도 월별 이자·원금 스케줄을 생성한다

- **Issue ID:** BANKING_CREDIT_02
- **Priority:** P1
- **Module:** loan
- **Area / category:** core / financial
- **Labels:** module:loan, area:core, type:financial, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- loan/core/src/main/java/com/ho/account/loan/domain/Loan.java:107
- loan/core/src/main/java/com/ho/account/loan/domain/Loan.java:322
- loan/api/src/main/java/com/ho/account/loan/dto/LoanRequestDto.java:53
- loan/core/src/main/java/com/ho/account/loan/domain/EIRAmortizationSchedule.java:234
- loan/core/src/main/java/com/ho/account/loan/domain/EIRAmortizationSchedule.java:240
- loan/core/src/main/java/com/ho/account/loan/domain/EIRAmortizationSchedule.java:284
- loan/core/src/test/java/com/ho/account/loan/domain/EIRAmortizationScheduleTest.java:33

## Problem statement
대출 계약과 API는 `DAILY`, `WEEKLY`, `BI_WEEKLY`, `QUARTERLY`, `SEMI_ANNUALLY`, `ANNUALLY`를 상환 주기로 받아 저장한다. 그러나 실제 EIR 스케줄 생성기는 `paymentFrequency`를 읽지 않고 항상 한 달 간격의 상환일과 `annualEir / 12` 이자를 사용한다. 비월별 계약도 오류 없이 월별 원금·이자 및 현금흐름 스케줄을 받는다.

## Reproduction / evidence
`LoanRequestDto.paymentFrequency`에 `QUARTERLY`를 넣으면 `Loan.create`는 이를 322행에 저장하며 주기별 제약은 없다. 2026-01-01 실행, 2026-04-01 만기, 원금 1,200, 연 EIR 0.12 계약에서 `EIRAmortizationSchedule.generateMonthly`는 284–291행에 따라 2026-02-01, 2026-03-01, 2026-04-01의 세 회차를 만든다. 240행의 월 이율은 0.01이고 242–256행은 1회차 원금을 400, 이자를 12로 산출한다. 분기 상환 계약인데도 2월·3월 상환이 예정되는 결과다. 현재 `EIRAmortizationScheduleTest`는 `MONTHLY`만 생성한다.

## Financial / architectural impact
고객 계약의 납입일보다 이른 원금·이자 수취가 예정되고, 그 스케줄을 소비하는 이자 발생·약정 상환 Batch의 대상 날짜와 금액이 잘못된다. 마감일별 미수이자·대출채권 및 현금흐름이 실제 계약과 어긋날 수 있다.

## Proposed solution
수정 allowlist(5파일): `loan/core/src/main/java/com/ho/account/loan/domain/Loan.java`, `loan/core/src/main/java/com/ho/account/loan/domain/EIRAmortizationSchedule.java`, `loan/core/src/test/java/com/ho/account/loan/domain/EIRAmortizationScheduleTest.java`, `loan/api/src/test/java/com/ho/account/loan/web/LoanControllerTest.java`, `loan/docs/process-flow.md`. 현재 계산기가 실제 지원하는 `MONTHLY`만 계약 생성과 스케줄 생성 경계에서 허용하고, 나머지 주기는 원금 실행 전에 명확한 검증 오류로 거부한다. 기존 비월별 데이터도 스케줄 생성에서 fail-closed 처리한다. 분기·일별 등은 기간 이율, 단기 마지막 회차, 휴일·day-count 정책이 정해진 별도 기능으로 지원한다.

## Acceptance criteria
- [ ] 신규 비월별 대출 계약은 저장·전표 실행 전에 명확히 거부되고 API는 400을 반환한다.
- [ ] 기존 비월별 계약도 EIR 스케줄을 생성·교체하지 않으며 잘못된 월별 결과를 반환하지 않는다.
- [ ] 월별 계약의 날짜·원금 합계·이자 계산은 유지되고, `QUARTERLY`와 나머지 지원되지 않는 enum 값의 거부 회귀 테스트가 통과한다.

## Test gap and verification limits
`EIRAmortizationScheduleTest`의 생성 테스트는 `MONTHLY`만 사용하며 비월별 입력과 API 계약 경계를 검증하지 않는다. 감사 시 선택된 `loan:core:test` 146건이 통과했지만 이 도메인 테스트는 이번 선택 실행에 포함되지 않았으므로 통과 근거로 쓰지 않는다. 수정 후 검증 명령: `./gradlew :loan:core:test :loan:api:test :loan:batch:test --offline --no-daemon --console=plain`; 실제 고객 계약·외부 Journal·PostgreSQL에서 스케줄/분개 대사는 이 감사에서 실행하지 않았다.
