# 대손충당금(IFRS 9) 배치 실행 가이드

표준 실행 Job은 `allowanceEclJob`입니다. 이 Job은 `account-mart`의 `allowance_exposure_snapshots`를 입력으로 사용하고, 완료된 결과를 `allowance_summary`로 집계합니다.

## 실행

```powershell
./gradlew :ecl:ecl-batch:bootRun --args="--spring.batch.job.enabled=true job.name=allowanceEclJob baseDate=2026-04-30 runId=RUN-20260430 modelVersion=IFRS9_BASE_2026"
```

대손충당금 모듈만 단독으로 띄울 때 필요한 DB 준비와 최소 시드 데이터는 [ALLOWANCE_SERVICE_RUNBOOK.md](ALLOWANCE_SERVICE_RUNBOOK.md)를 따른다.

## 주요 파라미터

| 파라미터 | 필수 | 설명 |
| --- | :---: | --- |
| `baseDate` | Y | 산출 기준일 |
| `modelVersion` | N | 산출 모델 버전 |
| `runId` | N | 실행 식별자. 없으면 Job runner가 생성 |

## 검증 SQL

```sql
SELECT status, COUNT(*)
FROM allowance_ecl_results
WHERE base_date = DATE '2026-04-30'
GROUP BY status;

SELECT base_date, run_id, model_version, SUM(target_allowance_amount)
FROM allowance_summary
WHERE base_date = DATE '2026-04-30'
GROUP BY base_date, run_id, model_version;
```

## 재처리

같은 `baseDate`로 재실행하면 기준일 입력/결과를 정리한 뒤 다시 생성합니다. 계정 매핑이 누락되면 summary 교체를 중단해 잘못된 전표 생성을 막습니다.
