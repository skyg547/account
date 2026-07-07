# 대손충당금 입력 마트 입문 가이드

대손충당금 엔진은 원천 원장을 그대로 사용하지 않습니다. `account-mart`가 원장, 고객, 상품, 담보, 환율 데이터를 정리해 기준일 snapshot을 만들고, `ecl` 모듈이 그 snapshot으로 ECL을 계산합니다.

## 초보자를 위한 비유

`account-mart`는 회계 데이터의 "검수대"입니다. 여러 시스템에서 온 원천 데이터를 바로 계산기에 넣지 않고, 날짜와 필수값을 맞추고, 원장 잔액과 대사한 뒤, ECL 엔진이 읽을 수 있는 표준 상자(`allowance_exposure_snapshots`)에 담습니다.

담보 데이터는 두 번 봅니다. 먼저 `ods_coll_mst`에서 담보번호, 유형, 평가액이 있는지 보고, 부동산/아파트 담보이면 `ods_apart_coll_detail`에서 지역 코드, KB 시세, 전용면적처럼 LGD 계산 전에 필요한 세부값을 봅니다.

## 흐름

1. 원천 데이터를 읽습니다.
2. 필수값과 잔액 대사를 검증합니다.
3. 담보 평가액과 아파트 상세 시세 입력값을 검증합니다.
4. CDM 포지션을 만듭니다.
5. `allowance_exposure_snapshots`를 생성합니다.

## 확인할 코드

- `IntegratedPositionEtlJobConfig`: 표준 ETL Job 구성.
- `CollateralDataQualityItemProcessor`: Spring Batch row를 core 담보 DQ 유즈케이스로 넘기는 어댑터.
- `CollateralDataQualityInspectionService`: 담보 상세를 port로 조회하고 domain processor를 호출하는 core application service.
- `CollateralDataQualityProcessor`: 담보 마스터와 아파트 상세값의 실제 DQ 판단 규칙.
- `IntegratedPositionProcessor`: 원천 계좌를 CDM 포지션으로 변환.
- `AllowanceExposureSnapshotService`: ECL 입력 snapshot 생성.

## 실행해 보는 순서

```powershell
.\gradlew :account-mart:mart-core:test --console=plain
.\gradlew :account-mart:mart-batch:test --console=plain
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30" --console=plain
```

IntelliJ에서는 `AllowanceMartBatchApplication`을 선택하고 Program arguments에 아래 값을 넣으면 같은 흐름을 실행할 수 있습니다.

```text
--spring.profiles.active=demo --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30
```

## 데이터 흐름에서 중요한 점

- batch 모듈은 Job/Step 흐름을 조립합니다.
- Stage 판정, 만기 계산, 금액 검증, 담보 상세 DQ 같은 규칙은 core/application/domain에 둡니다.
- snapshot은 기준일 재실행을 고려해 멱등하게 다시 만들 수 있어야 합니다.
- `allowance_input_positions`는 account-mart가 쓰기 소유권을 갖고, ECL은 snapshot/조회 계약으로 읽습니다.