# governance local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`governance`는 Spring Boot 내장 WAS로 실행되는 API 애플리케이션입니다. Config Server와 Eureka를 붙인 통합 실행도 가능하지만, 기능 개발과 컨텍스트 확인은 H2 메모리 DB만으로 단독 실행할 수 있습니다.

## IntelliJ에서 H2 단독 실행

1. IntelliJ의 Gradle JVM을 JDK 17로 설정합니다.
2. Gradle Reload를 실행해 `governance` 의존성을 갱신합니다.
3. 상단 Run Configuration에서 `Governance bootRun`을 선택합니다.
4. 실행 후 `http://localhost:8083`에서 내장 WAS가 열린 것을 확인합니다.
5. H2 콘솔이 필요하면 아래 PowerShell 명령처럼 `spring.h2.console.enabled=true`를 추가합니다.

공유 Run Configuration은 Config, Discovery, Vault, Eureka, tracing을 비활성화하고 H2 스키마를 실행할 때 생성합니다. 따라서 Config Server나 PostgreSQL 없이도 API 컨텍스트가 올라옵니다.

## PowerShell H2 API 실행

```powershell
.\gradlew :governance:bootRun --args="--spring.profiles.active=local --server.port=8083 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.datasource.url=jdbc:h2:mem:governance;MODE=PostgreSQL;DB_CLOSE_DELAY=-1 --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false --spring.h2.console.enabled=true --spring.h2.console.path=/h2-console" --console=plain --max-workers=1
```

- API 기본 포트: `8083`
- H2 콘솔: `http://localhost:8083/h2-console`
- JDBC URL: `jdbc:h2:mem:governance`
- 사용자: `sa`, 비밀번호: 빈 값

메모리 DB이므로 프로세스를 종료하면 데이터가 사라집니다. 개발 중 매번 같은 초기 상태에서 승인 흐름을 확인할 때 사용합니다.

## 컨텍스트 smoke 실행

웹 포트를 열지 않고 실제 audit/master-data Bean과 JPA 매핑만 확인하려면 다음 명령을 사용합니다.

```powershell
.\gradlew :governance:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

## PostgreSQL 로컬 smoke 실행

`governance`에는 PostgreSQL JDBC 드라이버가 포함되어 있습니다. 현재 운영용 Flyway DDL은 완성되지 않았으므로 아래 명령은 **폐기 가능한 로컬 DB**에서만 `create-drop`으로 실행합니다. 운영 DB에는 사용하면 안 됩니다.

```powershell
$env:GOVERNANCE_DB_URL='jdbc:postgresql://localhost:5432/account_governance_local'
$env:GOVERNANCE_DB_USER='local_user'
$env:GOVERNANCE_DB_PASSWORD='local_password'

.\gradlew :governance:bootRun --args="--spring.profiles.active=local --server.port=8083 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.datasource.url=$env:GOVERNANCE_DB_URL --spring.datasource.username=$env:GOVERNANCE_DB_USER --spring.datasource.password=$env:GOVERNANCE_DB_PASSWORD --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

실행 전 PostgreSQL에 `account_governance_local` 데이터베이스와 해당 로컬 사용자 권한을 준비해야 합니다. 실제 운영 전환 전에는 Flyway 마이그레이션과 `ddl-auto=validate` 조합으로 바꿔야 합니다.

## Config Server/Eureka/Auth 통합 실행

1. `Config Server bootRun`을 실행합니다.
2. `Discovery bootRun`을 실행합니다.
3. Auth 역할 반영까지 확인하려면 `Auth bootRun`을 실행합니다.
4. 통합 환경용 DB/내부 토큰을 설정한 뒤 `Governance bootRun`을 실행합니다.

단독 H2 Run Configuration은 통합 기능을 끄도록 구성되어 있으므로, 통합 실행에서는 해당 비활성화 인자를 제거해야 합니다.

## 테스트와 빌드

```powershell
.\gradlew :governance:test :governance:bootJar --console=plain --max-workers=1
```

테스트에는 지원하지 않는 역할/권한 변경의 fail-closed 정책, 권한 회수 승인 요청, 실제 기능 Bean 스캔 검증이 포함됩니다.

## Auth 연동 환경변수

```powershell
$env:GOVERNANCE_AUTH_BASE_URL='http://localhost:8081'
$env:GOVERNANCE_AUTH_INTERNAL_TOKEN='local-internal-auth-token'
```

Auth 쪽 `AUTH_INTERNAL_API_TOKEN`과 같은 값을 사용해야 내부 역할 반영 API가 통과합니다.

## 종료와 메모리 정리

실행 중인 `bootRun`은 IntelliJ Stop 버튼 또는 터미널의 `Ctrl+C`로 종료합니다. 검증이 끝나면 Gradle 데몬도 정리합니다.

```powershell
.\gradlew --stop
```
