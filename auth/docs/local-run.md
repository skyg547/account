# auth local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 저장소 루트를 Gradle 프로젝트로 열기

`auth`는 Spring Boot 내장 WAS로 실행되는 API 앱입니다. `local` 프로파일은 Config Server, Eureka, Vault, Redis repository, Master Data 원격 검증 및 PostgreSQL 없이 H2/Flyway/JPA 경계를 확인합니다.

## 실행 시점 보안 입력

JWT secret과 internal API token은 저장소 파일에 기본값으로 두지 않습니다. 실행할 때마다 값을 생성하고 현재 프로세스 종료 후 제거합니다.

```powershell
function New-EphemeralAuthValue {
    $bytes = New-Object byte[] 32
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($bytes)
        [Convert]::ToBase64String($bytes)
    } finally {
        $random.Dispose()
    }
}

$env:AUTH_JWT_SECRET = New-EphemeralAuthValue
$env:AUTH_INTERNAL_API_TOKEN = New-EphemeralAuthValue
```

값을 콘솔, Issue, PR, 로그에 출력하거나 파일에 저장하지 않습니다.

## IntelliJ H2 단독 실행

1. Gradle JVM을 JDK 17로 설정합니다.
2. Gradle Reload를 실행합니다.
3. Run Configuration에 `local` 프로파일과 위 두 환경변수를 연결합니다.
4. `Auth bootRun`을 실행합니다.
5. 내장 WAS 포트 `8081`에서 API 기동을 확인합니다.

local profile은 다음을 적용합니다.

- Config/Discovery/Vault/Eureka/tracing 비활성화
- H2 PostgreSQL 호환 모드
- `auth.persistence.mode=jpa`
- `auth.login-security.store=jpa`
- Flyway V70~V73 활성화
- Hibernate `ddl-auto=validate`
- 보안 입력과 데모 사용자 기본값 없음

## PowerShell H2 API 실행

보안 입력을 생성한 현재 PowerShell에서 실행합니다.

```powershell
.\gradlew :auth:api:bootRun --args='--spring.profiles.active=local --server.port=8081' --console=plain --max-workers=1
```

- API: `http://localhost:8081`
- JDBC: H2 memory database, PostgreSQL compatibility mode
- Config Server/Eureka/Master Data 원격 호출: 비활성화

## H2 컨텍스트/Flyway smoke

웹 포트 없이 마이그레이션과 Bean/JPA 매핑만 확인합니다.

```powershell
.\gradlew :auth:api:bootRun --args='--spring.profiles.active=local --spring.main.web-application-type=none' --console=plain --max-workers=1
```

## 테스트와 executable JAR

```powershell
.\gradlew :auth:core:test :auth:api:test :auth:api:bootJar --offline --console=plain --max-workers=1

$jar = Get-ChildItem auth/api/build/libs -Filter '*.jar' |
    Where-Object { $_.Name -notlike '*-plain.jar' } |
    Select-Object -First 1

java -jar $jar.FullName --spring.profiles.active=local --server.port=8081
```

테스트 범위에는 local resource 정책, H2/Flyway 컨텍스트, 로그인 실패 JPA commit, API/core 경계와 Boot JAR 패키징이 포함됩니다.

## fail-closed 확인

입력을 제거한 뒤 같은 JAR을 실행하면 Auth의 fail-closed 정책 때문에 시작에 실패해야 합니다.

```powershell
Remove-Item Env:AUTH_JWT_SECRET, Env:AUTH_INTERNAL_API_TOKEN -ErrorAction SilentlyContinue
java -jar $jar.FullName --spring.profiles.active=local --server.port=8081
```

local profile은 사용자를 자동 생성하지 않습니다. 로그인 사용자는 저장소 밖의 승인된 입력이나 별도 개발 bootstrap 절차로 제공하며, 비밀번호 기본값을 커밋하지 않습니다.

## 종료와 정리

`bootRun`과 JAR은 IntelliJ Stop 또는 `Ctrl+C`로 종료합니다. 검증 후 임시 환경변수와 Gradle 데몬을 정리합니다.

```powershell
Remove-Item Env:AUTH_JWT_SECRET, Env:AUTH_INTERNAL_API_TOKEN -ErrorAction SilentlyContinue
.\gradlew --stop
```

실제 개발·운영 credential, 외부 PostgreSQL, 기존 컨테이너는 이 로컬 검증 범위에 포함하지 않습니다.
