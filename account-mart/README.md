# Allowance Mart Service

`account-mart`는 IFRS 9 대손충당금 산출에 필요한 원천 데이터를 정제해 `allowance_exposure_snapshots`로 고정하는 입력 마트입니다.

## 모듈

- `mart-core`: ODS 도메인, 데이터 품질 검증, CDM 포지션 변환, allowance snapshot 생성 규칙.
- `mart-batch`: 기준일 단위 ETL, DQ, ODS 대사, snapshot 재생성 Job.
- `mart-api`: CDM/시장데이터/기준정보 조회 API.

## 표준 흐름

```mermaid
flowchart LR
    ODS[ODS / GL] --> DQ[DQ]
    DQ --> CDM[CDM Position]
    CDM --> SNAP[allowance_exposure_snapshots]
    SNAP --> ECL[ecl allowanceEclJob]
```

## 실행

```powershell
./gradlew :account-mart:mart-core:compileJava :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava
./gradlew :account-mart:mart-batch:bootRun --args="--spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --spring.batch.job.enabled=true"
```

스키마는 `account-mart/db/schema-mart.sql`을 기준으로 관리합니다.

`allowance_input_positions`의 쓰기 소유권과 JPA 엔티티는 `account-mart`에 있습니다. 다른 모듈은 이 엔티티를 공유하지 않고 스냅샷/조회 계약을 통해 데이터를 읽습니다.
