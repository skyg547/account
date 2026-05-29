# Allowance Mart Batch Architecture

`integratedPositionEtlJob`은 IFRS 9 대손충당금 입력 snapshot을 만드는 기준일 배치입니다.

```mermaid
flowchart LR
    PRE[preProcessStep] --> LDQ[ledgerDataQualityStep]
    LDQ --> CDQ[collateralDataQualityStep]
    CDQ --> REC[odsReconcileStep]
    REC --> CDM[cdmLoadStep]
    CDM --> SNAP[allowanceExposureSnapshotStep]
    SNAP --> EVT[cdmEventPublishStep]
```

## 설계 기준

- Job/Step은 오케스트레이션만 담당합니다.
- 원장 검증, 대사, snapshot 생성 규칙은 `mart-core`에 둡니다.
- 기준일 파라미터는 필수이며, 같은 기준일 재실행 시 기존 산출물을 정리한 뒤 다시 생성합니다.
