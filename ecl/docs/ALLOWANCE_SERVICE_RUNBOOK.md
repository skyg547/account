# IFRS 9 대손충당금 단독 서비스 런북

이 문서는 대손충당금 모듈만 먼저 서비스로 띄워 산출을 검증하는 절차를 정리한다.

아키텍처와 설계 흐름도는 [ALLOWANCE_ARCHITECTURE.md](ALLOWANCE_ARCHITECTURE.md)를 함께 본다.

## 결론

대손충당금만 먼저 서비스하려면 `ecl`을 독립 서비스로 띄우고, 입력 snapshot만 외부에서 공급하면 된다. `closing`, `frontend`, `gateway`는 대손충당금 summary 이후 단계이므로 처음 검증에는 필수가 아니다.

## 최소 구성

| 구성 | 필수 | 역할 |
| --- | :---: | --- |
| PostgreSQL `ifrs9_allowance_db` | Y | 산출 입력, 배치 메타데이터, ECL 결과, 회계 summary 저장 |
| `ecl-api` | 권장 | 상태 조회, 수동 실행 API, 조회 API 제공 |
| `ecl-batch` | 권장 | 기준일/모델 버전을 지정해 `allowanceEclJob` 실행 |
| snapshot 공급 | Y | `allowance_exposure_snapshots`에 기준일별 산출 입력 적재 |
| `account-mart` 전체 서비스 | N | snapshot을 외부 ETL로 넣으면 없어도 됨 |
| `closing` | N | `allowance_summary` 검증 후 전표 생성 단계에서 연결 |

운영에 가까운 검증은 `ecl-api`와 `ecl-batch`를 같은 DB에 붙이는 구성이 가장 단순하다. API 하나만 띄워도 배치 Bean은 포함되지만, 현재 API의 수동 실행 엔드포인트는 기준일 파라미터를 받지 않으므로 기준일을 명시하려면 `ecl-batch` CLI 실행을 사용한다.

## 데이터베이스 준비

1. DB를 만든다.

```sql
CREATE DATABASE ifrs9_allowance_db;
CREATE USER allowance_user WITH PASSWORD 'allowance_password';
GRANT ALL PRIVILEGES ON DATABASE ifrs9_allowance_db TO allowance_user;
\c ifrs9_allowance_db
GRANT USAGE, CREATE ON SCHEMA public TO allowance_user;
```

2. ECL 스키마를 반영한다.

```powershell
psql -h localhost -U allowance_user -d ifrs9_allowance_db -f ecl/ecl-api/src/main/resources/db/migration/V1__init_credit_schema.sql
psql -h localhost -U allowance_user -d ifrs9_allowance_db -f ecl/ecl-api/src/main/resources/db/migration/V2__align_credit_runtime_schema.sql
psql -h localhost -U allowance_user -d ifrs9_allowance_db -f ecl/ecl-api/src/main/resources/db/migration/V3__add_allowance_summary.sql
```

3. 단독 서비스에서는 입력 snapshot 테이블도 준비한다.

```powershell
psql -h localhost -U allowance_user -d ifrs9_allowance_db -f account-mart/mart-api/src/main/resources/db/migration/V2__add_allowance_exposure_snapshot.sql
```

`allowance_exposure_snapshots`는 account-mart가 만들던 입력 계약이다. account-mart 없이 대손충당금만 띄울 때는 외부 ETL 또는 임시 SQL로 이 테이블에 데이터를 넣어야 한다.

4. 기준월 partition을 확인한다.

현재 초기 스키마에는 2026년 4월 partition 예시만 있다. 다른 기준월을 산출하려면 실행 전에 partition을 추가한다.

```sql
CREATE TABLE IF NOT EXISTS allowance_ecl_results_2026m05
    PARTITION OF allowance_ecl_results
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
```

## 최소 시드 데이터

처음 서비스 확인만 할 때는 아래처럼 모델 마스터, 계정 매핑, snapshot 1건을 넣고 `baseDate=2026-04-15`로 실행한다.

```sql
INSERT INTO cr_product_masters (product_code, product_name, ccf_rate)
VALUES ('LN-1', '일반대출', 0.5);

INSERT INTO cr_grade_masters (rating_code, pd_value, notch_order)
VALUES ('A', 0.01000000, 1);

INSERT INTO allowance_model_parameters (param_key, param_value)
VALUES
    ('PD_FLOOR', 0.0005),
    ('SECURED_LGD_FLOOR', 0.20),
    ('UNSECURED_LGD_FLOOR', 0.45),
    ('DEFAULT_DISCOUNT_RATE', 0.05);

INSERT INTO cr_lgd_segment_masters (segment_name, customer_type, collateral_type, lgd_value)
VALUES ('기업_무담보', 'CORPORATE', 'UNSECURED', 0.45);

INSERT INTO cr_macro_scenario (scenario_type, apply_year, pd_adjustment_factor, probability_weight)
VALUES
    ('BOOM', 2026, 0.80, 0.20),
    ('BASE', 2026, 1.00, 0.60),
    ('RECESSION', 2026, 1.30, 0.20);

INSERT INTO allowance_account_mappings (
    product_code,
    biz_unit_code,
    currency_code,
    legal_entity_code,
    exposure_account_code,
    allowance_account_code,
    bad_debt_expense_account_code,
    reversal_income_account_code,
    active
) VALUES (
    'LN-1',
    'HO',
    'KRW',
    'HO',
    '110101',
    '110199',
    '550101',
    '450101',
    TRUE
);

INSERT INTO allowance_exposure_snapshots (
    base_date,
    exposure_id,
    source_system,
    source_account_no,
    customer_code,
    customer_type,
    is_sme,
    country_code,
    industry_code,
    product_code,
    product_category,
    legal_entity_code,
    branch_code,
    currency_code,
    outstanding_amount,
    undrawn_amount,
    interest_rate,
    open_date,
    maturity_date,
    delinquent_days,
    staging,
    original_rating,
    current_rating,
    warning_level,
    debt_restructured
) VALUES (
    DATE '2026-04-15',
    'EXP-001',
    'LOCAL',
    'ACC-001',
    'C-001',
    'CORPORATE',
    FALSE,
    'KR',
    'MANUFACTURING',
    'LN-1',
    'LOAN',
    'HO',
    'B001',
    'KRW',
    1000000.00,
    200000.00,
    0.050000,
    DATE '2025-04-15',
    DATE '2027-04-15',
    0,
    'STAGE1',
    'A',
    'A',
    'NORMAL',
    FALSE
);
```

## 빌드

```powershell
.\gradlew :ecl:ecl-api:bootJar :ecl:ecl-batch:bootJar --console=plain
```

## API 서비스 실행

로컬에 Config Server, Eureka, Kafka가 없으면 아래 옵션처럼 꺼두고 시작한다.

```powershell
.\gradlew :ecl:ecl-api:bootRun --args="--server.port=8083 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --eureka.client.enabled=false --spring.kafka.listener.auto-startup=false --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.datasource.url=jdbc:postgresql://localhost:5432/ifrs9_allowance_db --spring.datasource.username=allowance_user --spring.datasource.password=allowance_password --spring.datasource.driver-class-name=org.postgresql.Driver --spring.jpa.hibernate.ddl-auto=none --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
```

상태 조회:

```powershell
curl http://localhost:8083/api/v1/ifrs/allowance/batch/status
```

API에서 즉시 실행:

```powershell
curl -X POST "http://localhost:8083/api/v1/ifrs/allowance/batch/run?jobName=allowanceEclJob"
```

현재 API 실행은 오늘 날짜를 기준일로 사용한다. 특정 기준일, 실행 ID, 모델 버전을 지정하려면 아래 배치 CLI 실행을 사용한다.

## 배치 실행

```powershell
.\gradlew :ecl:ecl-batch:bootRun --args="--spring.batch.job.enabled=true --spring.flyway.enabled=false --spring.cloud.discovery.enabled=false --eureka.client.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.datasource.url=jdbc:postgresql://localhost:5432/ifrs9_allowance_db --spring.datasource.username=allowance_user --spring.datasource.password=allowance_password --spring.datasource.driver-class-name=org.postgresql.Driver --spring.jpa.hibernate.ddl-auto=none --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect job.name=allowanceEclJob baseDate=2026-04-15 runId=LOCAL-20260415 modelVersion=LOCAL-V1"
```

개별 재실행:

```powershell
# snapshot -> cr_customers/cr_accounts 동기화만 실행
.\gradlew :ecl:ecl-batch:bootRun --args="--spring.batch.job.enabled=true --spring.flyway.enabled=false --spring.cloud.discovery.enabled=false --eureka.client.enabled=false --spring.datasource.url=jdbc:postgresql://localhost:5432/ifrs9_allowance_db --spring.datasource.username=allowance_user --spring.datasource.password=allowance_password --spring.datasource.driver-class-name=org.postgresql.Driver --spring.jpa.hibernate.ddl-auto=none job.name=standaloneAllowanceExposureSyncJob baseDate=2026-04-15 runId=SYNC-20260415"

# 완료된 ECL 결과로 summary만 재생성
.\gradlew :ecl:ecl-batch:bootRun --args="--spring.batch.job.enabled=true --spring.flyway.enabled=false --spring.cloud.discovery.enabled=false --eureka.client.enabled=false --spring.datasource.url=jdbc:postgresql://localhost:5432/ifrs9_allowance_db --spring.datasource.username=allowance_user --spring.datasource.password=allowance_password --spring.datasource.driver-class-name=org.postgresql.Driver --spring.jpa.hibernate.ddl-auto=none job.name=standaloneAllowanceSummaryJob baseDate=2026-04-15 runId=LOCAL-20260415 modelVersion=LOCAL-V1"
```

`job.name`, `baseDate`, `runId`, `modelVersion`은 `JobRunner`가 읽는 일반 인자다. `--spring...` 형태의 Spring 설정 인자와 구분한다.

## 검증 SQL

```sql
SELECT status, COUNT(*)
FROM allowance_ecl_results
WHERE base_date = DATE '2026-04-15'
GROUP BY status;

SELECT
    base_date,
    run_id,
    model_version,
    SUM(target_allowance_amount) AS target_allowance_amount,
    SUM(source_exposure_amount) AS source_exposure_amount
FROM allowance_summary
WHERE base_date = DATE '2026-04-15'
GROUP BY base_date, run_id, model_version;
```

정상 기준:

- `allowance_ecl_results.status`가 `COMPLETED`로 생성된다.
- `allowance_summary.target_allowance_amount`가 0보다 크다.
- 같은 기준일로 재실행해도 summary가 기준일 단위로 교체되어 중복 누적되지 않는다.

## 자주 막히는 지점

- `allowance_exposure_snapshots` 테이블이 없으면 snapshot DDL을 먼저 반영한다.
- snapshot이 0건이면 배치는 의미 있는 산출을 만들지 않는다.
- `allowance_account_mappings`가 없으면 summary 생성은 중단된다.
- 기준월 partition이 없으면 `allowance_ecl_results` 저장에서 실패할 수 있다.
- 로컬 인프라 없이 API만 띄울 때는 Config Server, Discovery, Kafka listener를 꺼둔다.

## 다음 개선 후보

- `ecl-api`의 `/run` 엔드포인트가 `baseDate`, `runId`, `modelVersion`을 받도록 확장.
- `allowance_exposure_snapshots` 입력 계약 DDL을 `ecl` 전용 migration 또는 공통 contract migration으로 분리.
- 운영 배포에서는 migration 전용 Job과 배치 실행 Job을 분리.
