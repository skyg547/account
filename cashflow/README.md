# Cashflow

`cashflow`는 현금흐름표 집계와 단기 유동성 예측을 담당하는 bounded context입니다.
`core`가 금융 계산과 불변식을 소유하고, `api`와 `batch`는 각각 HTTP 및 배치 실행
파라미터를 core 유즈케이스로 전달합니다.

## 모듈

- `cashflow:core`: Aggregate, inbound/outbound port, application service, local memory adapter
- `cashflow:api`: 현금흐름표와 유동성 예측 REST API
- `cashflow:batch`: `cashflowAggregationJob` Job/Step 및 실행 파라미터 검증

## 핵심 규칙

- 모든 금액은 `BigDecimal` scale 2입니다. 세 번째 소수점 이하의 유효 값은 자동 반올림하지 않고 거부합니다.
- line item 금액은 유입을 양수, 유출을 음수로 나타냅니다.
- 직접법/간접법 모두 이미 분류된 line item의 `activity`를 기준으로 합산합니다. 두 방식의 차이는
  영업활동 line item을 만들어 내는 upstream 분류 방식이며 합계 공식은 같습니다.
- `netCashflow = totalOperating + totalInvesting + totalFinancing`이고
  `endingCash = beginningCash + netCashflow`입니다.
- 예측은 `netLiquidity = inflowEstimate - outflowEstimate`로 계산합니다. 결과가 watch 이상이면
  `NORMAL`, watch 미만이면서 critical 이상이면 `WATCH`, critical 미만이면 `CRITICAL`입니다.
  정확히 임계값인 경우에는 덜 심각한 구간에 포함합니다.
- `local` 프로파일은 statement/forecast 저장과 기초현금 조회를 프로세스 메모리에 보관합니다.
  재시작하면 데이터가 사라지며 운영용 persistence adapter는 후속 구현 대상입니다.

## 검증

저장소 루트에서 다음 명령을 실행합니다.

```bash
./gradlew :cashflow:core:test :cashflow:api:test :cashflow:batch:test --console=plain
```

기대 결과는 세 모듈의 단위/컨텍스트 테스트가 모두 통과하는 것입니다. API와 Batch는 별도
인프라 없이 기본 `local` 프로파일로 실행됩니다. 자세한 요청과 배치 파라미터는
[기능 및 로컬 실행 안내](docs/README.md)를 참고하세요.
