# Loan 입문 가이드

대출 회계는 고객에게 돈을 빌려준 계약과 실제 지급, 시간이 지나며 인식할 이자·수수료, 조건 변경 이력을 함께 관리하는 업무입니다.

## 핵심 용어

| 용어 | 코드 | 쉬운 설명 |
| --- | --- | --- |
| 대출 계약 | `Loan` | 약정 원금·금리·만기와 현재 미상환 잔액을 보관합니다. |
| 대출 실행 | `LoanDisbursal` | 실제 지급과 연결 전표를 기록합니다. 현재 모델은 원금 전액 1회 실행입니다. |
| 이연 항목 | `DeferredItem` | 수수료/직접 비용처럼 기간에 나눠 인식할 금액입니다. |
| 이연 유형 | `DeferredItemType` | EIR 현금흐름 정책과 계정 코드 매핑입니다. |
| EIR | `EIRCalculator` | 원금과 포함 대상 수수료·비용을 함께 반영한 유효이자율입니다. |
| EIR 스케줄 | `EIRAmortizationSchedule` | 월별 기초잔액, 이자수익, 원금상환, 이연상각 계획입니다. |
| 이벤트 | `LoanEvent` | 중도상환, 조건 변경, 부도, 회복 같은 사건입니다. |
| 재계산 실행 | `RecalculationRun` | 변경 전후 EIR·만기와 조정 전표 계보입니다. |
| 발생 로그 | `LoanAccrualLog` | 대출/기준일별 `PENDING`, `SUCCESS`, `FAILED` 결과입니다. |

## 상태 전이

```text
계약 생성 → PENDING_DISBURSEMENT
원금 전액 1회 실행 → ACTIVE
부도 → DEFAULTED
회복 → ACTIVE
```

원래 약정 원금(`principalAmount`)과 현재 미상환 잔액(`currentPrincipalBalance`)은 다릅니다. 중도상환은 약정 원금을 덮어쓰지 않고 현재 잔액만 줄입니다.

## EIR 숫자를 읽는 법

- `0.0450`은 연 4.50%입니다.
- `4.50`을 넣으면 450%가 되므로 허용하지 않습니다.
- 계산은 `double` 대신 `BigDecimal`로 수행하며, 수렴하지 않거나 정책 입력이 부족하면 임의 금리로 대체하지 않고 실패합니다.

## 왜 값 참조와 포트를 쓰는가

Loan은 Master Data의 `BusinessPartner`, `Currency`, `AccountSubject` 엔티티를 직접 소유하지 않습니다. 거래처 ID, 통화 코드, 계정 코드만 저장하고 `LoanReferenceDataPort`로 업무일 유효성을 확인합니다.

전표도 같은 방식입니다. `LoanJournalPort`가 요청을 번역하고 Loan에는 `journalEntryId`, `slipNo`만 남깁니다. 따라서 다른 모듈의 엔티티 생명주기가 Loan Aggregate에 전파되지 않습니다.

## Batch 재실행 규칙

- `accrualDate`는 필수입니다. 서버 오늘 날짜를 암묵적으로 쓰지 않습니다.
- `(loan_id, accrual_date)`가 멱등 키입니다.
- `SUCCESS`는 재실행 시 건너뜁니다.
- `FAILED`는 스케줄 금액을 다시 읽고 재시도합니다.
- 한 건이라도 실패하면 chunk 처리 후 Step이 실패하여 운영자가 성공으로 오인하지 않습니다.
- 현재 스케줄은 월별이므로 스케줄 날짜가 아닌 날은 `NOT_DUE`입니다.

## 운영 전에 확인할 것

- 거래처·통화·계정이 업무일에 유효한지 확인합니다.
- 상품별 이연 유형의 `CUSTOMER_FEE_INFLOW`, `ORIGINATION_COST_OUTFLOW`, `EXCLUDED_FROM_EIR` 정책을 확인합니다.
- V33 멱등 인덱스 적용 전에 기존 중복 데이터를 조회하고 정리합니다.
- Loan–Journal 장애 복구는 outbox/inbox가 구현되기 전까지 수동 대사 절차가 필요합니다.
