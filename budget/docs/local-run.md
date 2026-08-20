# 로컬 실행과 검증

JDK 17과 저장소 Gradle wrapper를 사용합니다. 로컬 API/Batch는 공유 DB나 비밀정보 대신
PostgreSQL mode의 메모리 H2를 사용하고 Flyway V50으로 새 Budget 테이블을 검증합니다.

```powershell
.\gradlew.bat :budget:core:test :budget:api:test :budget:batch:test :budget:api:bootJar :budget:batch:bootJar :gateway:test --console=plain --max-workers=1 --no-daemon --rerun-tasks
```

API 실행:

```powershell
.\gradlew.bat :budget:api:bootRun --console=plain --no-daemon
```

Batch 기동 확인(자동 Job 실행 없음):

```powershell
.\gradlew.bat :budget:batch:bootRun --console=plain --no-daemon
```

bootJar를 직접 실행할 때도 기본 `local` 프로파일과 메모리 H2, Flyway V50,
JPA `validate`가 동일하게 적용됩니다.

```powershell
$env:AUTH_JWT_SECRET = '<Auth/Gateway와 동일한 32자 이상 개발 전용 키>'
$apiJar = (Get-ChildItem budget\api\build\libs\account-budget-api-*.jar |
    Where-Object Name -NotLike '*-plain.jar' | Select-Object -First 1).FullName
$batchJar = (Get-ChildItem budget\batch\build\libs\account-budget-batch-*.jar |
    Where-Object Name -NotLike '*-plain.jar' | Select-Object -First 1).FullName
java -jar $apiJar
java -jar $batchJar
```

API 기동 전 `AUTH_JWT_SECRET`을 반드시 설정해야 합니다. 저장소 기본 secret은 없으며
값이 빠지면 안전하게 기동 실패합니다. `AUTH_JWT_ISSUER`도 Auth/Gateway와 같아야 하고,
호출에는 유효한 Bearer JWT가 필요합니다. 기본 포트는 Expenditure API와 겹치지 않는 `8096`입니다.
Gateway는 `/api/budgets/**`를 `lb://budget-api`로 전달합니다. Eureka 기반 환경에서는
`BUDGET_DISCOVERY_ENABLED=true`, `BUDGET_EUREKA_ENABLED=true`와 올바른
`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`을 설정해야 합니다. 로컬 단독 실행은 Discovery를
기본적으로 끕니다.

## 개발 Compose

`budget/docker-compose.yml`은 저장소 루트를 build context로 유지하면서
`budget/api/Dockerfile`과 `budget/batch/Dockerfile`로 각 bootJar를 빌드합니다.
DB URL, DB 계정, JWT 키는 트래킹된 파일에 저장하지
않고 실행 환경에서 필수로 주입합니다.

```powershell
$env:BUDGET_DB_URL = 'jdbc:postgresql://postgres-db:5432/budget_dev'
$env:BUDGET_DB_USER = 'budget_dev_app'
$env:BUDGET_DB_PASSWORD = '<ACCOUNT_DB_APP_PASSWORD과 동일한 로컬 전용 값>'
$env:AUTH_JWT_SECRET = '<Auth/Gateway와 동일한 32자 이상 개발 전용 키>'
docker compose -f budget\docker-compose.yml up --build budget-api
docker compose -f budget\docker-compose.yml --profile batch run --rm budget-batch
```

API 호스트 포트는 `BUDGET_API_PORT`(기본 8096)로만 변경합니다. Batch는
포트를 노출하지 않고 `batch` 프로파일을 명시해야 실행 대상이 되며,
스프링 Job 자동 실행은 끌 상태를 유지합니다. 실제 마감은 승인된 launcher가
Job name과 `fiscalYear`를 명시해 실행해야 합니다.

기본 외부 network 이름은 self-contained DB Compose와 같은 `account-dev-network`이며
필요할 때만 `ACCOUNT_NETWORK_NAME`으로 바꿉니다. `budget_dev_owner`는 migration 전용이고
장기 실행 API/Batch에는 `budget_dev_app` runtime role만 주입합니다.
API/Batch 시작 전에 `docs/development-postgresql.md`의 release migration과
`grant-runtime-privileges.sh` gate를 완료해 업무 테이블 권한을 부여하고
`flyway_schema_history*` runtime 권한이 회수된 상태를 확인해야 합니다.

`dev`/`prod`는 외부 PostgreSQL과 release migration runner가 소유하므로 앱 런타임의
Flyway, Hibernate DDL, SQL init, Batch metadata 초기화를 수행하지 않습니다.
`PROD_DB_URL`은 `jdbc:postgresql://...?...sslmode=verify-full` 계약으로 주입해야
하며 사용자/비밀번호도 환경에서만 제공합니다. Config Server는
`SPRING_CONFIG_IMPORT`와 `SPRING_CLOUD_CONFIG_ENABLED=true`를 모두 명시한 컨테이너에서만
opt-in되며, API Eureka 등록도 `BUDGET_DISCOVERY_ENABLED=true` 및
`BUDGET_EUREKA_ENABLED=true`를 명시해야 합니다. Batch는 Eureka에 등록하지 않습니다.

연말마감 Job은 운영 launcher에서 `spring.batch.job.name=budgetYearEndCloseJob`과 식별
파라미터 `fiscalYear=YYYY`를 명시해야 합니다. 설정 기본값은 job 자동 실행을 끕니다.

API/Batch bootJar에는 PostgreSQL JDBC driver가 포함됩니다. 로컬 검증은 H2 PostgreSQL
mode에서 migration·JPA·두 스레드 잠금 동작을 확인하지만 실제 PostgreSQL의 lock timeout,
query plan과 대량 마감 성능을 대체하지 않습니다.
