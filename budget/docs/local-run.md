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

API 기동 전 `AUTH_JWT_SECRET`을 반드시 설정해야 합니다. 저장소 기본 secret은 없으며
값이 빠지면 안전하게 기동 실패합니다. `AUTH_JWT_ISSUER`도 Auth/Gateway와 같아야 하고,
호출에는 유효한 Bearer JWT가 필요합니다. 기본 포트는 Expenditure API와 겹치지 않는 `8096`입니다.
Gateway는 `/api/budgets/**`를 `lb://budget-api`로 전달합니다. Eureka 기반 환경에서는
`BUDGET_DISCOVERY_ENABLED=true`, `BUDGET_EUREKA_ENABLED=true`와 올바른
`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`을 설정해야 합니다. 로컬 단독 실행은 Discovery를
기본적으로 끕니다.

연말마감 Job은 운영 launcher에서 `spring.batch.job.name=budgetYearEndCloseJob`과 식별
파라미터 `fiscalYear=YYYY`를 명시해야 합니다. 설정 기본값은 job 자동 실행을 끕니다.

API/Batch bootJar에는 PostgreSQL JDBC driver가 포함됩니다. 로컬 검증은 H2 PostgreSQL
mode에서 migration·JPA·두 스레드 잠금 동작을 확인하지만 실제 PostgreSQL의 lock timeout,
query plan과 대량 마감 성능을 대체하지 않습니다.
