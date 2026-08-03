# Allowance Mart Service

`account-mart`는 IFRS 9 대손충당금 산출에 필요한 원천 데이터를 정제해 `allowance_exposure_snapshots`로 고정하는 입력 마트입니다.

## 모듈

- `mart-core`: ODS 도메인, 데이터 품질 검증, CDM 포지션 변환, allowance snapshot 생성 규칙.
- `mart-batch`: 기준일 단위 ETL, DQ, ODS 대사, snapshot 재생성 Job. `mart-batch`는 Spring Batch 어댑터이고, 실제 업무 판단은 `mart-core` 컴포넌트를 호출합니다.
- `mart-api`: CDM/시장데이터/기준정보 조회 API.

## 표준 흐름

```mermaid
flowchart LR
    ODS[ODS / GL] --> DQ[DQ]
    DQ --> CDM[CDM Position]
    CDM --> SNAP[allowance_exposure_snapshots]
    SNAP --> ECL[ecl allowanceEclJob]
```

담보 DQ는 `ods_coll_mst`의 담보 평가액을 먼저 확인하고, 부동산/아파트 담보이면 `ods_apart_coll_detail`의 지역 코드, KB 시세, 전용면적을 함께 확인합니다. 초보자 관점에서는 "담보가 있다"는 마스터 정보와 "그 담보가 실제 LGD 선행 산출에 쓸 수 있는 상세값을 갖췄는지"를 나눠 검수하는 흐름입니다.

## 실행

프로파일을 생략하면 `local`이 선택되고, API와 Batch가 각각 독립 메모리 H2 DB에 Account Mart
전용 Flyway V1 baseline을 적용한 뒤 JPA schema를 검증한다. Batch Job 자동 실행은 기본적으로
꺼져 있다.

```powershell
.\gradlew.bat :account-mart:mart-api:bootRun --console=plain
.\gradlew.bat :account-mart:mart-batch:bootRun --console=plain
.\gradlew.bat :account-mart:mart-api:bootJar :account-mart:mart-batch:bootJar
java -jar account-mart\mart-api\build\libs\account-mart-api-0.0.1-SNAPSHOT.jar
java -jar account-mart\mart-batch\build\libs\account-mart-batch-0.0.1-SNAPSHOT.jar
```

`dev`/`prod`는 PostgreSQL 전용이며 `DEV_DB_*`/`PROD_DB_*` 환경 변수를 반드시 주입한다.
애플리케이션 Flyway와 Hibernate DDL, SQL init, Batch metadata 생성은 금지되고 release-time
`migration-runner`가 published V1-V5와 forward-only V6를 적용한다. `prod`는
`sslmode=verify-full`을 강제한다.

대표 Job을 명시적으로 실행하는 기존 demo 예시는 다음과 같다.

```powershell
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=true --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false" --console=plain --max-workers=1
```

IntelliJ에서 처음 실행할 때는 루트 [docs/local-development.md](../docs/local-development.md)를 먼저 확인합니다.
`mart-batch`는 CLI 인자에 `spring.batch.job.name`이 있으면 웹 서버를 띄우지 않고 배치 실행 후 종료합니다. 로컬에서 Vault, Eureka, Config Server 없이 단독 실행할 때는 위 예시처럼 `spring.cloud.vault.enabled=false`, `spring.cloud.config.enabled=false`, `eureka.client.enabled=false`를 함께 넣습니다.
H2 demo seed까지 같이 확인하려면 아래처럼 실행합니다.

```powershell
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=true --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false" --console=plain --max-workers=1
```

스키마는 `account-mart/db/schema-mart.sql`과 `mart-api/src/main/resources/db/migration`의 Flyway migration을 기준으로 관리합니다.

`allowance_input_positions`의 쓰기 소유권과 JPA 엔티티는 `account-mart`에 있습니다. 다른 모듈은 이 엔티티를 공유하지 않고 스냅샷/조회 계약을 통해 데이터를 읽습니다.

## 문서

- 문서 인덱스: [docs/README.md](docs/README.md)
- 입문 가이드: [docs/DATA_MART_BEGINNER_GUIDE.md](docs/DATA_MART_BEGINNER_GUIDE.md)
- ETL 인터페이스: [docs/ETL_INTERFACE_SPEC.md](docs/ETL_INTERFACE_SPEC.md)
- 데이터 마트 명세: [docs/DATA_MART_SPEC.md](docs/DATA_MART_SPEC.md)
- Batch 학습 가이드: [docs/BATCH_LEARNING_GUIDE.md](docs/BATCH_LEARNING_GUIDE.md)
