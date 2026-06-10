# Spring Batch & ETL 입문 가이드

`account-mart`의 배치는 원천 데이터를 읽고, 검증하고, IFRS 9 대손충당금 엔진이 읽을 수 있는 snapshot으로 저장합니다.

## ETL

1. Extract: ODS/GL 원천 데이터를 읽습니다.
2. Transform: 필수값, 금액, 통화, 계정 매핑을 검증하고 CDM 포지션으로 변환합니다.
3. Load: `allowance_exposure_snapshots`에 기준일 데이터를 고정합니다.

## Spring Batch 구성

- Job: 기준일 전체 처리 단위입니다.
- Step: DQ, 대사, 적재, snapshot 생성 같은 세부 단계입니다.
- Reader: 원천 데이터를 chunk 단위로 읽습니다.
- Processor: core 규칙을 호출해 데이터를 변환합니다.
- Writer: 변환 결과를 저장합니다.

표준 실행 Job은 `integratedPositionEtlJob`입니다.

## account-mart Job 목록

`IntegratedPositionEtlJobConfig` 기준으로 다음 Job을 실행할 수 있습니다.

| Job | 용도 |
| --- | --- |
| `integratedPositionEtlJob` | 전처리, DQ, 대사, CDM 적재, snapshot 생성, 이벤트 발행 전체 흐름 |
| `preProcessJob` | 기준일 전처리 |
| `ledgerDqJob` | 원장 DQ 검증 |
| `collateralDqJob` | 담보 DQ 검증 |
| `odsReconcileJob` | ODS-GL 대사 |
| `cdmLoadJob` | CDM 포지션 적재 |
| `allowanceExposureSnapshotJob` | ECL 입력 snapshot 생성 |
| `cdmEventPublishJob` | CDM 완료 이벤트 발행 |

## IntelliJ에서 실행

1. `AllowanceMartBatchApplication` 실행 구성을 만듭니다.
2. Active profiles에 `demo`를 입력합니다.
3. Program arguments에 아래 값을 입력합니다.

```text
--spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30
```

CLI 인자에 `spring.batch.job.name`이 있으면 앱은 웹 서버를 띄우지 않고 배치 실행 후 종료합니다.

## 성능 설정

- 기본 chunk size는 `MartBatchExecutionConfig`에서 관리합니다.
- `mart.batch.cdm-load.parallel-enabled=true`로 CDM 로드 Step에 TaskExecutor를 붙일 수 있습니다.
- 병렬 처리를 켤 때는 reader 상태 저장, skip limit, DB connection pool 크기, 기준일 재실행 멱등성을 함께 확인합니다.
