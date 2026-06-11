# reconciliation beginner guide

## 한 문장 요약

`reconciliation`은 서로 맞아야 하는 두 데이터, 예를 들어 은행 입출금 내역과 회계 장부가 같은지 비교하고 차이를 관리한다.

초보자 관점에서는 가계부와 통장 잔액을 맞춰보는 일과 비슷하다.

1. 어떤 대사를 할지 정한다.
2. 원천 데이터와 장부 데이터를 같은 기준일로 가져온다.
3. 금액과 건수가 맞는지 비교한다.
4. 차이가 나면 차이 항목을 만들고 사유를 붙인다.
5. 조정이 필요한 차이는 전표와 연결하고, 사람이 확인해야 할 차이는 담당자와 SLA를 지정한다.

## 주요 업무 용어

| 용어 | 코드 모델 | 설명 |
| --- | --- | --- |
| 대사 단위 | `ReconciliationUnit` | 어떤 대상끼리 비교할지 정하는 설정. 예: 은행계좌 대사, GL-보조원장 대사. |
| 대사 규칙 | `ReconciliationRule` | 허용오차와 매칭 우선순위를 정의한다. |
| 대사 실행 | `ReconciliationRun` | 특정 기준일에 대사를 한 번 실행한 결과 보고서. |
| 대사 차이 | `ReconciliationDifference` | 금액이나 건수가 맞지 않을 때 생기는 불일치 항목. |
| 사유 코드 | `DifferenceReasonCode` | 차이가 왜 생겼는지 분류하는 기준정보. |
| 단계 결과 | `ReconciliationStageResult` | 원천, 인터페이스, 전표, 원장 등 단계별 집계 체크포인트. |

## 현재 구현의 큰 흐름

`ReconciliationService.performReconciliation`은 요약 대사 흐름을 수행한다. 원천 금액은 `ExternalReconSnapshotPort`를 통해 가져오고, 대상 원장 금액은 `JournalQueryPort`로 집계한다. 두 금액 차이가 허용오차를 넘으면 `ReconciliationDifference`를 만든다.

은행 명세와 전표 상세를 1:1로 맞추는 상세 매칭은 `AutomatedMatchingEngine`이 담당한다. 이 컴포넌트는 입출금 부호를 반영한 금액으로 전표 상세를 인덱싱하고, 허용오차 안의 후보만 스캔해 성능을 높인다. 한 번 매칭된 전표 상세는 다시 사용하지 않는다.

## DDD와 헥사고날 관점

`domain`은 대사 단위, 실행, 차이, 사유 코드, 상태를 표현한다. 대사 차이는 담당자 배정, 사유 코드, 조정 전표 ID, 해결 상태를 하나의 감사 가능한 흐름으로 묶는다.

`service`는 유즈케이스 순서를 조율한다. `ReconciliationService`는 외부 스냅샷 조회, 원장 집계, 허용오차 계산, 차이 생성, 조정 전표 생성, 차이 해소를 처리한다.

외부 의존성은 포트로 분리한다. 원천 데이터는 `ExternalReconSnapshotPort`, 원장 조회는 `JournalQueryPort`, 조정 전표 생성은 `JournalPostingPort`로 접근한다. 대사 서비스는 외부 시스템의 저장소나 API 구현을 직접 알지 않는다.

## 처음 볼 때 체크할 파일

1. `ReconciliationController`: 대사 단위, 규칙, 사유 코드, 실행, 차이 처리 API.
2. `ReconciliationService`: 요약 대사 실행과 차이 해소 흐름.
3. `AutomatedMatchingEngine`: 은행 명세와 전표 상세 자동 매칭 알고리즘.
4. `ReconciliationAdjustmentPolicy`: 조정 전표 계정코드 정책 검증.
5. `ExternalReconStageSnapshotAdapter`: 외부 원천 단계 집계 조회 어댑터.
