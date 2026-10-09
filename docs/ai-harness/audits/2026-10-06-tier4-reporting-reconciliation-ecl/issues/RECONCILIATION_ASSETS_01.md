# [reconciliation] 반대 차대 전표가 은행 원천 거래와 매칭되어 누락 거래를 성공 처리

- **Issue ID:** RECONCILIATION_ASSETS_01
- **Priority:** P1
- **Module:** reconciliation
- **Area / category:** core / financial
- **Labels:** module:reconciliation, area:core, type:financial, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- reconciliation/core/src/main/java/com/ho/account/reconciliation/service/ReconciliationExecutionService.java:307
- reconciliation/core/src/main/java/com/ho/account/reconciliation/service/ReconciliationExecutionService.java:384
- reconciliation/core/src/main/java/com/ho/account/reconciliation/service/ReconciliationExecutionService.java:255
- reconciliation/core/src/main/java/com/ho/account/reconciliation/infrastructure/adapter/HttpReconciliationJournalAdapter.java:203
- reconciliation/core/src/main/java/com/ho/account/reconciliation/infrastructure/adapter/HttpReconciliationJournalAdapter.java:223

## Problem statement
대상 집계는 criteriaJson의 targetSide를 적용하지만, 건별 대상 조회는 계정 코드만 넘겨 같은 계정의 차변과 대변을 모두 매칭 엔진에 넣는다. 반대 방향 전표가 은행 원천 거래와 1:1 매칭되면 실제 지정 방향의 대상 전표가 부족해도 discrepancyGroups가 비어 차이가 생성되지 않는다. 서비스는 집계·건별 건수의 최대값과 집계 금액을 섞어 SUCCESS 결과를 저장한다.

## Reproduction / evidence
targetSide=DEBIT, targetAccountCode=11000인 단위에서 같은 날짜·계정의 은행 원천 100.00 두 건과 전기된 차변 100.00 한 건, 대변 100.00 한 건을 준비한다. 원천과 두 전표의 매칭 키를 맞추면 건별 조회는 차대 구분 없이 두 전표를 반환하여 두 원천 모두 1:1 매칭하고 discrepancyGroups는 비어 있다. 그러나 집계 메서드는 차변 1건/100.00만 반환한다. 255~268행은 sourceCount=2, targetCount=2(집계 1건과 건별 2건의 최대값), matchedCount=2, unmatchedCount=0으로 요약하고 targetTotalAmount는 집계의 100.00을 사용한다. 270~275행은 실제 지정 방향에 100.00이 부족한데도 SUCCESS를 저장하며 차이 엔티티는 없다. 반대 방향 라인이 건별 목록에 포함되는 동작은 HttpReconciliationJournalFinancialQueryTest.java:63~94에서도 확인된다. 현재 ReconciliationServiceTest에는 이 양방향 건별 사례가 없다.

## Financial / architectural impact
은행 원천 2건 중 차변 대상이 없는 1건이 대변 전표와 잘못 매칭되어 차이 추적에서 사라진다. 실행 결과는 원천 합계 200.00과 대상 합계 100.00이 다른데도 미대사 0건·SUCCESS로 기록될 수 있다. 동일 기준일의 재실행은 SUCCESS를 반환하는 177~185행 가드에 막혀 정정된 건별 판정을 다시 수행하지 않는다.

## Proposed solution
targetSide를 집계와 건별 목록에 동일하게 적용하고, 필터 후 건수·금액이 집계와 다르면 성공 대신 명시적으로 실패시키며, 미대사 건수는 실제 매칭 결과와 일치시키는 원자적 수정. 정확한 파일 allowlist (3개):
1. reconciliation/core/src/main/java/com/ho/account/reconciliation/service/ReconciliationExecutionService.java
2. reconciliation/core/src/test/java/com/ho/account/reconciliation/service/ReconciliationServiceTest.java
3. reconciliation/docs/process-flow.md

## Acceptance criteria
- [ ] targetSide=DEBIT/CREDIT 각각에서 반대 방향 라인이 건별 매칭·차이 목록에 들어오지 않는다.
- [ ] 필터된 건별 건수·금액과 집계가 불일치하면 SUCCESS로 확정하지 않으며, 미대사 건수·금액은 저장된 차이와 모순되지 않는다.
- [ ] 동일 날짜의 양방향 전표 회귀 테스트와 정상 재실행 테스트가 통과한다.

## Test gap and verification limits
현재 HttpReconciliationJournalFinancialQueryTest는 집계의 방향 필터와 양방향 건별 반환을 별도로 확인하지만 서비스가 두 결과를 합칠 때의 불변식은 검사하지 않는다. 수정 전/후에는 ./gradlew :reconciliation:core:test --offline --no-daemon --console=plain --max-workers=2를 실행하고, 수정 후 API·Batch 컴파일까지 확인한다. 이번 감사에서는 기존 테스트 실행과 정적 호출 경로로 원인을 검증했으며 실제 은행 데이터나 원격 Journal 서비스는 사용하지 않았다.
