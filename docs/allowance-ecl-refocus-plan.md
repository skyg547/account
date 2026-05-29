# IFRS 9 대손충당금 전환 작업 메모

`account-mart`와 `ecl`의 현재 목표는 결산용 IFRS 9 대손충당금 산출 경로만 유지하는 것이다.

## 유지 범위

- `account-mart`: ODS/GL 검증, CDM 포지션 적재, `allowance_exposure_snapshots` 재생성.
- `ecl`: snapshot 동기화, DQ, Stage/PD, EAD/LGD, 미래전망 weighted ECL, `allowance_summary` 재생성.
- `closing`: `allowance_summary` 기반 전표 생성.

## 표준 실행 흐름

```mermaid
flowchart LR
    MART[integratedPositionEtlJob] --> SNAP[allowance_exposure_snapshots]
    SNAP --> ECL[allowanceEclJob]
    ECL --> SUM[allowance_summary]
    SUM --> CLOSING[closing]
```

## 구현 기준

- Batch 모듈은 Job/Step 흐름, chunk, task executor 설정만 담당한다.
- 산식과 데이터 정합성 규칙은 core 모듈에 둔다.
- 금액/비율 계산은 `BigDecimal`을 사용한다.
- 기준일 재실행은 기존 기준일 결과를 정리한 뒤 다시 생성하는 방식으로 멱등 처리한다.

## 현재 상태

- `allowanceEclJob`은 snapshot sync, DQ, Stage/PD, EAD/LGD, weighted ECL, completion, summary를 순차 실행한다.
- `account-mart`는 CDM 적재 뒤 `allowance_exposure_snapshots`를 생성한다.
- `allowance_summary`는 완료된 weighted ECL 결과와 회계 계정 매핑을 결합해 생성한다.
- 오래된 별도 분석/보고/시뮬레이션 코드는 기본 경로에서 제거했다.
