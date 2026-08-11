# Internal Audit

Internal Audit는 RCM과 설계·운영 평가를 제공하는 헥사고날 모듈입니다.

- `internal-audit:core`: domain, application port/service, JPA outbound adapter를 소유하는 일반 JAR
- `internal-audit:api`: Controller와 Spring Boot composition root를 소유하는 유일한 실행 JAR

구체적인 Job/Step 계약이 없는 Batch는 실행 모듈로 유지하지 않습니다. Auth도 같은 원칙에 따라 API만 실행 대상입니다.

## Local H2

`local` 프로파일은 별도 환경변수 없이 외부 Config Server, Eureka, Vault, PostgreSQL을 모두 사용하지 않습니다. Flyway V60이 격리된 in-memory H2 PostgreSQL compatibility mode에 스키마를 만들고 Hibernate는 `validate`만 수행합니다. 애플리케이션을 종료하면 로컬 데이터도 함께 사라집니다.

```powershell
.\gradlew :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar --console=plain
.\gradlew :internal-audit:api:bootRun --args="--spring.profiles.active=local" --console=plain
java -jar .\internal-audit\api\build\libs\account-internal-audit-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

기본 포트는 `8083`이며 readiness는 `/actuator/health/readiness`입니다.

## Development / Production PostgreSQL

`dev`와 `prod`는 H2로 fallback하지 않습니다. URL, user, password는 환경변수 또는 Compose secret 주입으로만 전달하고, 애플리케이션은 Flyway를 실행하지 않은 채 승인된 release migration 결과를 `ddl-auto=validate`로 확인합니다. `prod` JDBC URL은 `sslmode=verify-full`이어야 합니다.

개발 Compose는 저장소 루트에서 다음처럼 렌더링·실행합니다.

```powershell
docker compose -f .\internal-audit\docker-compose.yml config --quiet
docker compose -f .\internal-audit\docker-compose.yml up --build internal-audit-api
```

필수 변수는 `INTERNAL_AUDIT_DB_URL`, `INTERNAL_AUDIT_DB_USER`, `INTERNAL_AUDIT_DB_PASSWORD`입니다. Compose는 외부 `account-dev-network`를 사용하며 PostgreSQL 자체는 `postgres/compose.self-contained.yml` 또는 승인된 공유 개발 DB 계약에서 제공합니다.
