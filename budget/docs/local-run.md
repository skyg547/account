# 로컬 실행과 검증

`budget` 모듈(API 및 Batch)을 로컬 환경에서 단독 실행하고 검증하는 방법입니다.
JDK 17과 저장소 루트의 Gradle wrapper(`.\gradlew.bat`)를 사용합니다.

로컬 환경(`local` 프로파일)에서는 외부 PostgreSQL, Config Server, Eureka 없이
PostgreSQL 호환 모드의 인메모리 H2와 Flyway V50 마이그레이션으로 동작하며,
Hibernate `ddl-auto=validate`로 엔티티와 스키마 일치성을 검증합니다.

## 보안 및 fail-closed 가드 계약

`budget:api`는 자체 보안 경계(`BudgetApiSecurityConfiguration`)에서 Auth 서비스가 발급한 HS256 JWT를 검증합니다.

- **Fail-closed 보안 가드**: 저장소 파일에 기본 secret을 커밋하지 않으므로, 기동 시 `auth.jwt.secret`(환경 변수 `AUTH_JWT_SECRET`)이 지정되지 않거나 UTF-8 기준 32바이트(256비트) 미만이면 `IllegalStateException`을 던지고 애플리케이션 기동이 안전하게 실패합니다.
- **로컬 실행 시 필수 조치**: 로컬 단독 실행 시에는 최소 32바이트 이상의 임시/테스트용 JWT secret(예: `ephemeral_test_secret_for_local_development_32bytes`)을 CLI 인자나 환경 변수로 반드시 주입해야 합니다.
- **포트 및 라우팅**: 기본 포트는 `8096`입니다(Expenditure Resolution API의 `8095`와 충돌 방지). Gateway는 `/api/budgets/**` 요청을 `lb://budget-api`로 라우팅합니다.

## PowerShell에서 로컬 실행하기

### 1. API 단독 실행 (CLI 인자 방식 - 권장)

`--args`에 `local` 프로파일과 32바이트 이상의 테스트 JWT secret을 함께 전달합니다.

```powershell
.\gradlew.bat :budget:api:bootRun --args="--spring.profiles.active=local --auth.jwt.secret=ephemeral_test_secret_for_local_development_32bytes"
```

### 2. API 단독 실행 (환경 변수 방식)

PowerShell 세션에서 임시 환경 변수를 설정하고 실행합니다.

```powershell
$env:AUTH_JWT_SECRET = "ephemeral_test_secret_for_local_development_32bytes"
.\gradlew.bat :budget:api:bootRun --console=plain
```

### 3. 컨텍스트 및 Flyway 스모크 검증 (웹 서버 기동 없음)

Tomcat 서버를 띄우지 않고 Flyway 마이그레이션과 Spring ApplicationContext 로딩만 검증할 때 사용합니다.

```powershell
.\gradlew.bat :budget:api:bootRun --args="--spring.profiles.active=local --auth.jwt.secret=ephemeral_test_secret_for_local_development_32bytes --spring.main.web-application-type=none" --console=plain
```

### 4. Batch 컨텍스트 기동 확인 (자동 Job 실행 없음)

기본 설정(`spring.batch.job.enabled=false`)에 따라 애플리케이션 기동 시 마감 Job이 자동으로 실행되지 않습니다.

```powershell
.\gradlew.bat :budget:batch:bootRun --console=plain
```

### 5. 연말마감 Batch Job 직접 실행

실제 연말마감 배치를 실행할 때는 `spring.batch.job.enabled=true`, Job 이름 `budgetYearEndCloseJob`, 그리고 대상 회계연도 `fiscalYear=YYYY` 파라미터를 명시합니다.

```powershell
.\gradlew.bat :budget:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=budgetYearEndCloseJob fiscalYear=2026" --console=plain
```

### 6. 테스트 및 Executable Boot JAR 생성/실행

모듈 단위 테스트 및 Boot JAR 빌드:

```powershell
.\gradlew.bat :budget:core:test :budget:api:test :budget:batch:test :budget:api:bootJar :budget:batch:bootJar --console=plain
```

빌드된 Boot JAR 직접 실행:

```powershell
$env:AUTH_JWT_SECRET = "ephemeral_test_secret_for_local_development_32bytes"
$apiJar = (Get-ChildItem budget\api\build\libs\account-budget-api-*.jar |
    Where-Object Name -NotLike '*-plain.jar' | Select-Object -First 1).FullName
$batchJar = (Get-ChildItem budget\batch\build\libs\account-budget-batch-*.jar |
    Where-Object Name -NotLike '*-plain.jar' | Select-Object -First 1).FullName

# API 실행
java -jar $apiJar --spring.profiles.active=local

# Batch 컨텍스트 실행
java -jar $batchJar --spring.profiles.active=local
```

## 개발 Compose (Docker Compose)

`budget/docker-compose.yml`은 저장소 루트 `Containerfile`로 API/Batch bootJar를 각각 빌드합니다. DB URL, DB 계정, JWT 키는 파일에 저장하지 않고 실행 환경에서 필수로 주입합니다.

```powershell
$env:BUDGET_DB_URL = 'jdbc:postgresql://postgres-db:5432/budget_dev'
$env:BUDGET_DB_USER = 'budget_dev_app'
$env:BUDGET_DB_PASSWORD = '<ACCOUNT_DB_APP_PASSWORD과 동일한 로컬 전용 값>'
$env:AUTH_JWT_SECRET = '<Auth/Gateway와 동일한 32자 이상 개발 전용 키>'
docker compose -f budget\docker-compose.yml up --build budget-api
docker compose -f budget\docker-compose.yml --profile batch run --rm budget-batch
```

- API 호스트 포트는 `BUDGET_API_PORT`(기본 `8096`)로 변경할 수 있습니다.
- Batch는 포트를 노출하지 않으며 `batch` 프로파일을 명시해야 실행 대상이 됩니다.
- 기본 외부 네트워크는 `account-dev-network`이며 필요 시 `ACCOUNT_NETWORK_NAME`으로 재정의합니다.
- `dev`/`prod` 환경은 외부 PostgreSQL과 배포 전 release migration runner가 스키마를 관리하므로 앱 런타임의 Flyway/DDL/SQL init을 수행하지 않습니다.

## 로컬 검증 시 자주 보는 실패와 점검 항목

| 증상 / 오류 메시지 | 원인 및 점검 항목 |
| --- | --- |
| `IllegalStateException: auth.jwt.secret must be at least 32 bytes for HS256` | `AUTH_JWT_SECRET` 환경 변수 또는 `--auth.jwt.secret` CLI 인자가 누락되었거나 32바이트 미만인 경우 발생합니다. 32바이트 이상의 시크릿을 지정하세요. |
| `401 Unauthorized` | 요청 헤더에 `Authorization: Bearer <token>`이 없거나, `auth-service` 발급자가 아니거나, 토큰 서명이 맞지 않는 경우입니다. |
| `403 Forbidden` | 역할(Role) 부족 오류입니다. 예산 편성/전용/집행은 `ROLE_BUDGET_MANAGER`, 승인은 `ROLE_BUDGET_APPROVER` (또는 `ROLE_ACCOUNTING_ADMIN`, `ROLE_ADMIN`) 권한이 필요합니다. |
| `409 Conflict (DUPLICATE_TRANSFER_REQUEST / DUPLICATE_EXECUTION)` | 동일한 `requestKey` 또는 동일한 `sourceType+sourceId+sourceLineId`로 이미 처리된 멱등 요청입니다. |
| `400 Bad Request (BUDGET_EXCEEDED)` | 집행액 또는 전출액이 해당 예산 계획의 현재 가용액(`배정 + 전입 - 전출 - 집행`)을 초과한 경우입니다. |
| `400 Bad Request (CLOSED_FISCAL_YEAR)` | 이미 연말마감(`CLOSED`)된 회계연도의 예산 계획을 변경(승인/전용/집행)하려고 시도한 경우입니다. |
| Config Server / Eureka 연결 실패 | 로컬 단독 실행에서는 기본적으로 꺼져 있습니다(`false`). 만약 활성화하려면 `BUDGET_DISCOVERY_ENABLED=true`, `BUDGET_EUREKA_ENABLED=true` 및 유효한 Eureka URL을 설정해야 합니다. |
