# Reconciliation Module Docs

`reconciliation` 모듈은 대사 단위 정의, 대사 규칙 관리, 대사 실행, 차이 추적, 차이 해소를 담당합니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 대사 실행과 차이 처리 흐름을 설명합니다.
- [schema.md](./schema.md): 주요 엔티티와 관계, 필드를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 대사 업무를 이해할 수 있도록 쉽게 설명합니다.

## 이 모듈이 하는 일

1. 어떤 대사를 할지 `ReconciliationUnit`으로 정의합니다.
2. 자동 매칭 규칙을 `ReconciliationRule`로 등록합니다.
3. 실행 결과를 `ReconciliationRun`으로 남깁니다.
4. 차이가 나면 `ReconciliationDifference`로 기록합니다.
5. 사유코드, 담당자, SLA, 조정분개를 연결해 해소합니다.

## 핵심 진입점

- `ReconciliationController`
- `ReconciliationService`
- `AutomatedMatchingEngine`
- `ReconManagerService`

## 현재 구현 기준에서 먼저 알아둘 점

- 대사 모델이 두 세트 공존합니다.
  - `ReconciliationUnit`, `ReconciliationRun`, `ReconciliationDifference` 중심의 메인 흐름
  - `ReconUnitDefinition`, `ReconciliationResult`, `ReconStageResult`, `ReconciliationVariance` 중심의 심화 흐름
- `performReconciliation`의 메인 흐름은 더미 금액을 사용하지 않습니다.
  - 원천 값은 `ReconciliationUnit.criteriaJson`의 `sourceAmount`, `sourceCount`에서 읽습니다.
  - 대상 값은 `JournalQueryPort`로 기준일 전표 상세를 조회해 차변 기준 금액을 집계합니다.
  - 활성 `ReconciliationRule`의 금액 허용오차를 우선순위 순서로 적용해 허용 범위 안의 차이는 차이 건으로 만들지 않습니다.
- `AutomatedMatchingEngine`는 `JournalDetailSummary` 기준으로 은행 거래와 전표 라인을 매칭합니다.
  - 기본 `match(...)` 호출은 금액과 회계일자가 정확히 같아야 합니다.
  - `MatchOptions`를 전달하면 금액 허용오차와 일자 허용일수를 적용할 수 있습니다.
  - 복합 옵션을 켜면 전표번호, 전표/라인 적요, 계좌번호를 추가 매칭 조건으로 사용할 수 있습니다.
  - 전표 라인은 부호가 반영된 금액 기준으로 인덱싱하고, 은행 거래 금액의 허용오차 범위에 들어오는 후보만 평가합니다.
  - 한 번 매칭된 전표 라인은 같은 실행 안에서 다시 사용하지 않습니다.
  - 은행 입금은 양수, 출금은 음수로 비교하며 전표 `CREDIT` 라인도 음수로 비교합니다.
- 조정 가능한 사유코드로 자동 조정분개를 만들려면 `criteriaJson`에 계정코드를 명시해야 합니다.
  - `adjustmentDebitAccountCode`
  - `adjustmentCreditAccountCode`
  - 계정코드가 없거나 `criteriaJson`이 잘못된 경우 숨은 기본 계정으로 대체하지 않고 실행을 실패시킵니다.
- 자동 조정분개 생성은 `JournalPostingPort`를 통해 journal-ledger의 전표 생성 경로로 위임합니다.
- 차이에 저장되는 조정분개 링크는 `JournalEntry` 엔티티 연관이 아니라 전표 ID입니다.
- 기본 생성되는 `GENERIC_MISMATCH` 사유코드는 자동 조정분개를 만들지 않습니다.
- `ReconManagerService` 심화 흐름의 4단계 금액 집계는 더미값을 사용하지 않습니다.
  - SOURCE/INTERFACE: `matchingRulesJson`의 `sourceAmount/sourceCount`, `interfaceAmount/interfaceCount`
  - JOURNAL: `JournalQueryPort` 전표 상세 차변 집계
  - LEDGER: `LedgerQueryPort` GL 잔액 집계
  - 선택 설정: `journalAccountCode`, `ledgerAccountCode`, `ledgerCurrencyCode`, `ledgerAmountBasis`
