# [reporting] 서로 다른 통화의 GL 잔액을 계정코드만으로 합산하여 FINAL 재무제표에 저장

- **Issue ID:** REPORTING_MART_01
- **Priority:** P1
- **Module:** reporting
- **Area / category:** core / financial
- **Labels:** module:reporting, area:core, type:financial, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java:34
- reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java:40
- reporting/core/src/main/java/com/ho/account/reporting/application/service/ReportingService.java:57
- reporting/core/src/test/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapterTest.java:17

## Problem statement
`LedgerClientAdapter`는 GL 조회에 통화 필터를 전달하지 않고, 반환 행의 `currencyCode`를 확인하거나 환산하지 않은 채 `accountCode`별 `endingBalance`를 더한다. `LoadLedgerPort`가 통화 차원을 버린 `Map<String, BigDecimal>`을 반환하므로 이후 보고 라인에서는 잘못 합산된 금액을 구별할 수 없다.

## Reproduction / evidence
동일 계정 `101`에 KRW 1,000과 USD 100인 `LedgerBalanceSummary` 두 건을 반환하면 34–38행의 무통화 조회 후 40–46행의 `groupingBy(accountCode)`가 1,100을 만든다. `ReportingService` 56–79행은 이 값을 보고 라인에 넣고 `FINAL`로 확정·저장한다. 기존 `LedgerClientAdapterTest` 17–34행은 합산만 검사하며 테스트 행에 통화가 없어서 이 경계를 검증하지 않는다. 이 재현은 코드 흐름 분석이며 혼합 통화 실행 테스트는 아직 없다.

## Financial / architectural impact
표시 통화가 확정되지 않은 금액이 재무제표와 비교 스냅샷에 남고, 그 스냅샷을 읽는 공시 주석 마트에도 전파된다. 두 통화의 명목 숫자가 우연히 맞아도 재무 금액으로 해석할 수 없다.

## Proposed solution
원자적 수정 allowlist (3파일):
1. `reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java` — 보고 통화를 KRW로 명시하고 각 GL 행의 통화 누락·비 KRW를 합산 전에 거절한다. 외화 지원 시에는 별도 명시적 환산 계약을 마련한다.
2. `reporting/core/src/test/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapterTest.java` — KRW 합산과 KRW/USD 혼합·통화 누락 거절 회귀 테스트를 추가한다.
3. `reporting/docs/process-flow.md` — 보고 통화와 거절 조건을 설명한다.

## Acceptance criteria
- [ ] KRW 계정 잔액은 기존대로 정확히 합산되며 보고 통화가 명시된다.
- [ ] 외화 또는 통화 누락 행이 있으면 금액을 합산하지 않고 생성이 실패해 `FINAL` 스냅샷을 저장하지 않는다.
- [ ] 동일 계정 KRW/USD 혼합 및 단독 USD 사례가 자동 테스트로 검증된다.

## Test gap and verification limits
기존 `LedgerClientAdapterTest`는 `currencyCode`를 설정하지 않은 모의 행과 계정코드 합산만 검증한다. 재현 입력을 추가한 뒤 `./gradlew --offline :reporting:core:test --tests com.ho.account.reporting.infrastructure.persistence.LedgerClientAdapterTest --console=plain --max-workers=1 --no-daemon`로 검증한다. 감사 시 기존 테스트는 통과했으며 결함 재현 테스트와 실제 GL 연동 검증은 미실행이다.
