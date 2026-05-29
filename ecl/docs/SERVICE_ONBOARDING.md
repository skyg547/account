# 대손충당금(IFRS 9) 서비스 온보딩

## 먼저 볼 문서

- `ecl/docs/BATCH_EXECUTION_FLOW.md`
- `ecl/docs/ALLOWANCE_ARCHITECTURE.md`
- `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md`
- `ecl/docs/ALLOWANCE_DATA_MODEL_SPEC.md`
- `account-mart/mart-batch/docs/MART_BATCH_ARCHITECTURE_GUIDE.md`

## 로컬 빌드

```powershell
./gradlew :account-mart:mart-batch:compileJava :ecl:ecl-batch:compileJava
```

## 표준 실행 순서

1. `account-mart`의 `integratedPositionEtlJob`으로 기준일 snapshot을 생성합니다.
2. `ecl`의 `allowanceEclJob`으로 ECL과 `allowance_summary`를 생성합니다.
3. `closing`은 `allowance_summary`를 조회해 전표를 생성합니다.

## 대손충당금만 먼저 서비스할 때

`account-mart` 전체를 띄우지 않는 경우에도 `allowance_exposure_snapshots` 입력 테이블에 기준일 데이터를 외부 ETL로 넣으면 `ecl-api`와 `ecl-batch`만으로 산출을 검증할 수 있습니다. DB 준비, 최소 시드, 실행 명령은 `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md`에 정리합니다.

## 수정 포인트

- Stage 판정 변경: `StagingService`.
- PD/LGD/EAD 변경: `calculation` package.
- 회계 summary 변경: `AllowanceSummaryService`, `JdbcAllowanceSummaryPersistenceAdapter`.
