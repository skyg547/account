# reconciliation process flow

## API 입구

현재 `reconciliation`은 library 모듈이므로 아래 API는 reconciliation 컴포넌트를 포함하는 호스트 Spring Boot 애플리케이션에서 노출될 때 사용할 수 있다.

| 컨트롤러 | 메서드 | 경로 | 역할 |
| --- | --- | --- | --- |
| `ReconciliationController` | `POST` | `/api/reconciliation/units` | 대사 단위를 생성한다. |
| `ReconciliationController` | `GET` | `/api/reconciliation/units` | 대사 단위 목록을 조회한다. |
| `ReconciliationController` | `GET` | `/api/reconciliation/units/{id}` | 대사 단위를 단건 조회한다. |
| `ReconciliationController` | `PUT` | `/api/reconciliation/units/{id}` | 대사 단위를 수정한다. |
| `ReconciliationController` | `DELETE` | `/api/reconciliation/units/{id}` | 대사 단위를 비활성화한다. |
| `ReconciliationController` | `POST` | `/api/reconciliation/rules` | 대사 규칙을 생성한다. |
| `ReconciliationController` | `GET` | `/api/reconciliation/units/{unitId}/rules` | 단위별 규칙을 조회한다. |
| `ReconciliationController` | `PUT` | `/api/reconciliation/rules/{id}` | 대사 규칙을 수정한다. |
| `ReconciliationController` | `DELETE` | `/api/reconciliation/rules/{id}` | 대사 규칙을 삭제한다. 현재 물리 삭제이며 `@todo`로 개선 표시했다. |
| `ReconciliationController` | `POST` | `/api/reconciliation/reason-codes` | 차이 사유 코드를 생성한다. |
| `ReconciliationController` | `GET` | `/api/reconciliation/reason-codes` | 차이 사유 코드 목록을 조회한다. |
| `ReconciliationController` | `GET` | `/api/reconciliation/reason-codes/{id}` | 차이 사유 코드를 단건 조회한다. |
| `ReconciliationController` | `PUT` | `/api/reconciliation/reason-codes/{id}` | 차이 사유 코드를 수정한다. |
| `ReconciliationController` | `DELETE` | `/api/reconciliation/reason-codes/{id}` | 차이 사유 코드를 비활성화한다. |
| `ReconciliationController` | `POST` | `/api/reconciliation/run` | 대사를 실행한다. `X-Audit-User` 헤더를 실행자로 기록한다. |
| `ReconciliationController` | `POST` | `/api/reconciliation/differences/assign` | 차이를 담당자에게 배정한다. |
| `ReconciliationController` | `POST` | `/api/reconciliation/differences/resolve` | 차이를 해결 또는 무시 처리한다. |
| `ReconciliationController` | `GET` | `/api/reconciliation/runs/{runId}/differences` | 실행별 차이 목록을 조회한다. |

## 대사 실행 흐름

```mermaid
flowchart TD
    A[POST /api/reconciliation/run] --> B[ReconciliationController]
    B --> C[ReconciliationService.performReconciliation]
    C --> D[ReconciliationUnit 조회]
    D --> E[ReconciliationRule 우선순위 조회]
    E --> F[ReconciliationRun RUNNING 저장]
    F --> G[ExternalReconSnapshotPort로 SOURCE 집계 조회]
    F --> H[JournalQueryPort로 TARGET 원장 집계 조회]
    G --> I[허용오차 계산]
    H --> I
    I --> J{차이가 허용오차 초과인가}
    J -->|아니오| K[Run SUCCESS]
    J -->|예| L[ReconciliationDifference 생성]
    L --> M{사유코드 adjustable?}
    M -->|예| N[JournalPostingPort로 조정 전표 초안 생성]
    M -->|아니오| O[PENDING 차이 저장]
    N --> O
    O --> K
```

원천 집계는 `RECON_EXTERNAL_STAGE_RECORD`를 기준으로 `unitId`, `stageCode`, `reconciliationDate`, 상품, 통화, 법인 조건을 적용한다. 대상 집계는 `criteriaJson`의 `targetAccountCode`와 `targetSide`를 읽어 원장 상세 집계를 조회한다.

## criteriaJson 주요 키

| 키 | 용도 |
| --- | --- |
| `sourceProductCode` | 원천 스냅샷 상품 필터 |
| `sourceCurrencyCode` | 원천 스냅샷 통화 필터 |
| `legalEntityCode` | 원천 스냅샷 법인 필터 |
| `targetAccountCode` | 대상 원장 계정코드 |
| `targetSide` | 원장 정상 잔액 방향. `DEBIT` 또는 `CREDIT` |
| `adjustmentCurrencyCode` | 조정 전표 통화. 없으면 `KRW` |
| `adjustmentDebitAccountCode` | 조정 전표 차변 계정 |
| `adjustmentCreditAccountCode` | 조정 전표 대변 계정 |

조정 가능한 사유 코드를 사용할 때는 조정 계정 두 개가 반드시 있어야 한다. `ReconciliationAdjustmentPolicy`가 이 정책을 fail-closed로 검증한다.

## 자동 매칭 엔진 흐름

```mermaid
flowchart TD
    A[BankStatement 목록] --> B[입금은 양수, 출금은 음수로 변환]
    C[JournalDetailSummary 목록] --> D[차변은 양수, 대변은 음수로 변환]
    D --> E[금액 기준 TreeMap 인덱스 생성]
    B --> F[허용오차 범위 후보 조회]
    E --> F
    F --> G[전표번호/계좌번호/적요/날짜 조건 확인]
    G --> H{단일 후보 매칭}
    H -->|성공| I[MatchResult matched]
    H -->|실패| J[NO_MATCH_FOUND]
```

`AutomatedMatchingEngine`은 상세 라인 매칭 컴포넌트다. 현재 `performReconciliation`의 요약 대사 흐름과는 별도이며, 은행 명세-전표 상세 매칭 테스트에서 검증된다.

## 차이 배정과 해결 흐름

```mermaid
flowchart TD
    A[POST /differences/assign] --> B[담당자와 SLA 저장]
    B --> C[상태 ASSIGNED]

    D[POST /differences/resolve] --> E[차이 조회]
    E --> F[사유 코드 조회]
    F --> G{RESOLVED 또는 IGNORED인가}
    G -->|아니오| H[예외]
    G -->|예| I{조정 필요 사유인가}
    I -->|예: 전표 ID 없음| J[예외]
    I -->|아니오 또는 전표 ID 있음| K[사유/전표/해결자/해결시각 저장]
```

조정 전표가 필요한 사유 코드는 `adjustmentJournalEntryId`가 있어야 완료할 수 있다. 이미 자동 조정 전표가 만들어진 차이는 기존 전표 ID를 유지한다.
