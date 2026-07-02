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
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=true --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false" --console=plain --max-workers=1
```

IntelliJ에서 처음 실행할 때는 루트 [docs/local-development.md](../docs/local-development.md)를 먼저 확인합니다.
`mart-batch`는 CLI 인자에 `spring.batch.job.name`이 있으면 웹 서버를 띄우지 않고 배치 실행 후 종료합니다. 로컬에서 Vault, Eureka, Config Server 없이 단독 실행할 때는 위 예시처럼 `spring.cloud.vault.enabled=false`, `spring.cloud.config.enabled=false`, `eureka.client.enabled=false`를 함께 넣습니다.
H2 demo seed까지 같이 확인하려면 아래처럼 실행합니다.

```powershell
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=true --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false" --console=plain --max-workers=1
```

스키마는 `account-mart/db/schema-mart.sql`을 기준으로 관리합니다.

`allowance_input_positions`의 쓰기 소유권과 JPA 엔티티는 `account-mart`에 있습니다. 다른 모듈은 이 엔티티를 공유하지 않고 스냅샷/조회 계약을 통해 데이터를 읽습니다.

## 문서

- 문서 인덱스: [docs/README.md](docs/README.md)
- 입문 가이드: [docs/DATA_MART_BEGINNER_GUIDE.md](docs/DATA_MART_BEGINNER_GUIDE.md)
- ETL 인터페이스: [docs/ETL_INTERFACE_SPEC.md](docs/ETL_INTERFACE_SPEC.md)
- 데이터 마트 명세: [docs/DATA_MART_SPEC.md](docs/DATA_MART_SPEC.md)
- Batch 학습 가이드: [docs/BATCH_LEARNING_GUIDE.md](docs/BATCH_LEARNING_GUIDE.md)
