# auth local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`auth`는 Spring Boot 내장 WAS로 실행되는 API 앱입니다. H2 단독 실행에서는 Config Server, Eureka, PostgreSQL 없이 Flyway와 JPA 스키마까지 확인합니다.

## IntelliJ H2 단독 실행

1. Gradle JVM을 JDK 17로 설정합니다.
2. Gradle Reload를 실행합니다.
3. `Auth bootRun` Run Configuration을 실행합니다.
4. 내장 WAS 포트 `8081`과 H2 콘솔 `/h2-console`을 확인합니다.

공유 Run Configuration은 다음을 적용합니다.

- Config/Discovery/Vault/Eureka/tracing 비활성화
- H2 PostgreSQL 호환 모드
- `auth.persistence.mode=jpa`
- `auth.login-security.store=jpa`
- Flyway V70~V72 활성화
- Hibernate `ddl-auto=validate`

## PowerShell H2 API 실행

```powershell
.\gradlew :auth:bootRun --args="--spring.profiles.active=local --server.port=8081 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=validate --spring.flyway.enabled=true --spring.h2.console.enabled=true --auth.persistence.mode=jpa --auth.login-security.store=jpa" --console=plain --max-workers=1
```

- API: `http://localhost:8081`
- H2 콘솔: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:auth`
- 사용자 `sa`, 비밀번호 빈 값

## H2 컨텍스트/Flyway smoke

웹 포트 없이 마이그레이션과 Bean/JPA 매핑만 확인합니다.

```powershell
.\gradlew :auth:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=validate --spring.flyway.enabled=true --auth.persistence.mode=jpa --auth.login-security.store=jpa" --console=plain --max-workers=1
```

## PostgreSQL 로컬 smoke

PostgreSQL JDBC 드라이버는 포함되어 있습니다. 실제 로컬 DB와 계정은 사용자가 별도로 준비합니다.

```powershell
$env:AUTH_DB_URL='jdbc:postgresql://localhost:5432/account_auth_local'
$env:AUTH_DB_USER='local_user'
$env:AUTH_DB_PASSWORD='local_password'

.\gradlew :auth:bootRun --args="--spring.profiles.active=local --server.port=8081 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.datasource.url=$env:AUTH_DB_URL --spring.datasource.username=$env:AUTH_DB_USER --spring.datasource.password=$env:AUTH_DB_PASSWORD --spring.jpa.hibernate.ddl-auto=validate --spring.flyway.enabled=true --auth.persistence.mode=jpa --auth.login-security.store=jpa" --console=plain --max-workers=1
```

실제 PostgreSQL 실행은 이번 검증 범위에 포함되지 않았습니다. 운영 전 Flyway DDL, lock 동작, 동시 승인/로그인 실패 부하를 PostgreSQL 환경에서 확인해야 합니다.

## 테스트와 빌드

```powershell
.\gradlew :auth:test :auth:bootJar --console=plain --max-workers=1
```

테스트 범위에는 API/core DTO 경계, 고정 시각 역할 스냅샷, token-version 상태 정책, memory/JPA 역할 메타데이터, approvalTraceId 멱등성이 포함됩니다.

## 로컬 사용자와 내부 토큰

```powershell
$env:AUTH_DEFAULT_USERNAME='admin'
$env:AUTH_DEFAULT_PASSWORD='1234'
$env:AUTH_INTERNAL_API_TOKEN='local-internal-auth-token'
$env:AUTH_JWT_SECRET='modern-account-system-super-secret-key-1234567890'
```

접두사 없는 비밀번호는 레거시/로컬 호환 경로입니다. 운영에서는 `{bcrypt}` 같은 Delegating Password Encoder 형식의 해시를 저장해야 하며, 평문 승격/차단은 코드 `@todo`로 남아 있습니다.

## 종료와 메모리 정리

`bootRun`은 IntelliJ Stop 또는 `Ctrl+C`로 종료하고, 검증 후 Gradle 데몬을 정리합니다.

```powershell
.\gradlew --stop
```
