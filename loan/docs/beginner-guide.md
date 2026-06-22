# Loan 입문 가이드

대출 회계는 "고객에게 돈을 빌려주고, 시간이 지나며 이자와 수수료를 회계 장부에 나누어 반영하는 업무"입니다. 단순히 원금과 이자를 저장하는 것을 넘어, 전표 생성과 재계산 이력을 함께 남겨야 합니다.

## 핵심 용어

| 용어 | 코드 | 쉬운 설명 |
| --- | --- | --- |
| 대출 | `Loan` | 고객에게 빌려준 돈의 계약 조건입니다. |
| 대출 실행 | `LoanDisbursal` | 실제로 돈이 지급된 기록입니다. |
| 이연 항목 | `DeferredItem` | 수수료/비용처럼 한 번에 손익 처리하지 않고 기간에 걸쳐 나눠 인식할 금액입니다. |
| 이연 항목 유형 | `DeferredItemType` | 이연 항목의 계정 매핑과 상각 방법을 정의합니다. |
| EIR | `EIRCalculator` | 수수료와 현금흐름까지 반영한 유효이자율입니다. |
| EIR 상각 스케줄 | `EIRAmortizationSchedule` | 기간별 이자수익, 원금상환, 이연 항목 상각 계획입니다. |
| 대출 이벤트 | `LoanEvent` | 중도상환, 조건 변경, 리스케줄처럼 계약에 영향을 주는 사건입니다. |
| 재계산 실행 | `RecalculationRun` | 이벤트 때문에 EIR과 스케줄을 다시 계산한 이력입니다. |
| 이자 발생 로그 | `LoanAccrualLog` | 일별 이자 발생 전표 성공/실패 기록입니다. |

## 초보자가 봐야 할 업무 흐름

1. 대출 계약을 생성합니다.
2. 대출 실행 요청이 들어오면 대출채권/현금 전표를 만들고 전기합니다.
3. 대출 실행 수수료가 있으면 이연 항목으로 등록하고 초기 전표를 만듭니다.
4. EIR 상각 스케줄을 생성합니다.
5. 매일 Batch가 ACTIVE 대출을 읽고, 해당 일자의 상각 스케줄 엔트리를 찾아 이자 발생 전표를 만듭니다.
6. 중도상환이나 조건 변경이 발생하면 EIR과 스케줄을 다시 계산하고 재계산 이력을 남깁니다.

## 왜 전표 포트가 필요한가

`loan`은 전표를 직접 소유하지 않습니다. 전표 생성/승인/전기는 `journal-ledger`가 담당합니다. 그래서 `LoanService`는 `LoanJournalPort`를 호출하고, 실제 구현인 `LoanJournalAdapter`가 `JournalUseCase`로 번역합니다.

이 구조는 대출 도메인이 journal-ledger 내부 JPA 모델에 묶이지 않게 하려는 헥사고날 경계입니다. 대출 Aggregate에는 전표 엔티티를 직접 연결하지 않고 `journalEntryId`, `slipNo` 같은 값만 저장합니다.

## 코드 위치

| 관심사 | 위치 |
| --- | --- |
| REST API | `loan/api/src/main/java/com/ho/account/loan/web/LoanController.java` |
| 대출 유즈케이스 | `loan/core/src/main/java/com/ho/account/loan/service/LoanService.java` |
| 일일 이자 발생 | `loan/core/src/main/java/com/ho/account/loan/service/InterestAccrualService.java` |
| EIR 계산 | `loan/core/src/main/java/com/ho/account/loan/service/EIRCalculator.java` |
| 출력 포트 | `loan/core/src/main/java/com/ho/account/loan/application/port/out` |
| 전표 어댑터 | `loan/core/src/main/java/com/ho/account/loan/infrastructure/adapter/LoanJournalAdapter.java` |
| 기준정보 어댑터 | `loan/core/src/main/java/com/ho/account/loan/infrastructure/adapter/LoanReferenceDataAdapter.java` |
| 일일 이자 Batch | `loan/batch/src/main/java/com/ho/account/loan/batch/config/LoanInterestAccrualBatchConfig.java` |

## 운영 전에 확인할 것

- `account.loan.accounting.*` 계정 코드 설정이 모두 있어야 합니다.
- master-data에 통화, 거래처, 계정과목이 존재해야 합니다.
- 일일 이자 Batch는 ACTIVE 대출만 읽습니다.
- 일일 이자 전표는 `loan_amortization_schedule_entries`에 해당 `payment_date`가 있을 때만 생성됩니다.
- EIR 계산은 `DeferredItemType.eirCashFlowTreatment` 정책을 사용합니다. 고객이 낸 수수료는 순투자액을 줄이고, 회사가 부담한 직접 비용은 순투자액을 늘리며, 제외 항목은 EIR 계산에서 빼고 봅니다.
