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
- `performReconciliation`는 아직 실제 데이터 추출 대신 더미 금액 `1000` vs `950`를 사용합니다.
- `AutomatedMatchingEngine`는 금액 일치 + 회계일자 정확히 일치만 봅니다.
- 조정분개 계정도 현재 하드코딩입니다.
  - 차변 `121000`
  - 대변 `999999`
