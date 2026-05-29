# 대손충당금 입력 마트 ETL 배치

`mart-batch`는 ODS/GL 데이터를 검증하고 IFRS 9 대손충당금 산출 입력 snapshot을 생성합니다.

## 표준 Job

| 순서 | Step | 의미 |
| ---: | --- | --- |
| 1 | `preProcessStep` | 기준일 기존 CDM/snapshot 정리 |
| 2 | `ledgerDataQualityStep` | 계좌 원장 DQ |
| 3 | `collateralDataQualityStep` | 담보 DQ |
| 4 | `odsReconcileStep` | ODS와 GL 대사 |
| 5 | `cdmLoadStep` | CDM 포지션 적재 |
| 6 | `allowanceExposureSnapshotStep` | ECL 입력 snapshot 재생성 |
| 7 | `cdmEventPublishStep` | downstream 이벤트 발행 |

## 실행

```powershell
./gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --spring.batch.job.enabled=true"
```

Kafka 없이 로컬 검증할 때는 `mart.batch.cdm-event.enabled=false`를 사용합니다.

## 대용량 처리

- DQ와 CDM 적재는 chunk 기반으로 처리합니다.
- CDM 적재 병렬화는 `mart.batch.cdm-load.parallel-enabled=true`로 켤 수 있습니다.
- 배치 설정은 reader/processor/writer 연결과 chunk/task executor 설정만 담당합니다.
