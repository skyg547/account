# Cashflow 기능 및 로컬 실행 안내

## 처리 흐름

현금흐름표 요청은 Controller가 DTO를 command로 바꾼 뒤 `CashflowStatementService`에 전달합니다.
서비스는 `LedgerCashBalancePort`에서 기초현금을 읽고 Aggregate에 line item을 전달합니다.
Aggregate가 활동별 합계, 순현금흐름, 기말현금을 계산하고 불변식을 검증한 뒤 persistence port가
결과를 저장합니다. 유동성 예측도 동일하게 Controller → service → Aggregate → persistence port
순서로 처리합니다.

로컬 adapter는 동시 요청에 안전한 `ConcurrentHashMap`을 사용하지만 프로세스 재시작 후에는
복원되지 않습니다. `local` 이외의 환경은 실제 원장/DB adapter를 명시적으로 제공해야 하므로
실수로 메모리 저장소를 운영에 사용하는 것을 방지합니다.

## API 예시

API 실행:

```bash
./gradlew :cashflow:api:bootRun --args='--spring.profiles.active=local' --console=plain
```

현금흐름표 생성:

```http
POST /api/v1/cashflow/statements
Content-Type: application/json

{
  "statementId": "CF-2026-09",
  "fiscalYear": 2026,
  "fiscalPeriod": 9,
  "method": "DIRECT",
  "currency": "KRW",
  "generatedAt": "2026-09-23T10:00:00",
  "lineItems": [
    {
      "lineCode": "OP-COLLECTION",
      "category": "CUSTOMER_RECEIPT",
      "activity": "OPERATING",
      "amount": 100000.00,
      "currency": "KRW",
      "description": "고객 매출채권 회수"
    }
  ]
}
```

로컬 `LedgerCashBalancePort`의 미등록 잔액은 `0.00`이므로 위 응답의 기말현금은
`100000.00`입니다. 생성 결과는 `GET /api/v1/cashflow/statements/CF-2026-09`로 조회합니다.

`lineItems` 목록과 목록의 각 원소는 `null`일 수 없습니다. `lineItems: null`,
`lineItems: [null]`, 정상 항목 뒤에 `null`이 있는 요청은 HTTP 400으로 거절되며 해당
`statementId`는 저장되지 않습니다. 빈 목록 `[]`은 유효하며 활동별 합계와 순현금흐름이
`0.00`인 statement를 HTTP 201로 생성합니다. 정상 항목은 계속 허용되며 금액의 소수점
정밀도와 항목 통화·statement 통화 일치 검증이 적용됩니다.

유동성 예측 생성:

```http
POST /api/v1/cashflow/forecasts
Content-Type: application/json

{
  "forecastId": "FC-2026-10-31",
  "forecastDate": "2026-09-23",
  "targetDate": "2026-10-31",
  "inflowEstimate": 900000.00,
  "outflowEstimate": 950000.00,
  "watchThreshold": 0.00,
  "criticalThreshold": -100000.00,
  "notes": "월말 유동성 점검"
}
```

이 예시의 순유동성은 `-50000.00`이며 위험 수준은 `WATCH`입니다. `criticalThreshold`는
`watchThreshold`보다 클 수 없고, 예상 유입/유출은 음수일 수 없습니다.

## Batch skeleton

Batch 컨텍스트만 확인할 때는 다음 명령을 사용합니다. 기본 설정은 Job 자동 실행을 막습니다.

```bash
./gradlew :cashflow:batch:bootRun --args='--spring.profiles.active=local' --console=plain
```

`cashflowAggregationJob`은 다음 식별 파라미터를 요구합니다.

- `statementId`
- `fiscalYear` (1900–9999)
- `fiscalPeriod` (1–12)
- `method` (`DIRECT` 또는 `INDIRECT`)
- `currency` (ISO-4217 코드)
- `generatedAt` (ISO-8601 local date-time)

초기 skeleton은 분류된 line reader가 아직 없으므로 빈 line item 목록으로 균형 잡힌 statement를
생성합니다. 이후 대량 원장 reader/partitioning을 붙일 때도 분류·합계 계산은 batch가 아니라 core에
남겨야 합니다. 같은 JobRepository 수명 안에서는 동일 식별 파라미터로 완료된 Job instance의 재실행을
Spring Batch가 거부합니다. 다만 local 프로파일의 H2 메타데이터는 JVM 종료 시 사라지므로 프로세스를
재시작하면 같은 파라미터도 다시 실행됩니다. 프로세스 재시작을 넘어 중복을 막아야 하는 환경은 영속
JobRepository를 사용하고, 의도한 재집계에는 새 `statementId` 또는 새 식별 파라미터를 사용해야 합니다.
