# auth local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 저장소 루트를 Gradle 프로젝트로 열기

`auth`는 Spring Boot 내장 WAS로 실행되는 API 앱입니다. `local` 프로파일은 Config Server, Eureka, Vault, Redis repository, Master Data 원격 검증 및 PostgreSQL 없이 H2/Flyway/JPA 경계를 확인합니다. 기동에는 JWT secret과 internal API token이 필수이며, 로그인할 사용자도 필요하면 외부 런타임 설정으로 별도 제공해야 합니다.

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

## 선택적 configured user

local SQL 데모 credential과 기본 사용자는 제거되었고 `spring.sql.init.mode=never`입니다. 따라서 사용자 없이도 기동할 수 있지만, 로그인 확인을 하려면 외부 secret 주입 경로에서 최소한 다음 placeholder를 제공합니다.

- `AUTH_USERS_0_USERNAME`
- `AUTH_USERS_0_PASSWORD`
- 필요한 역할·부서 등의 `AUTH_USERS_0_...` 설정

`AUTH_USERS_0_PASSWORD`는 정확한 소문자 `{bcrypt}` 접두사와 구조적으로 유효한 60자 BCrypt 페이로드를 결합한 값이어야 합니다. 접두사 없음, 평문, `{noop}`, unknown 접두사, 대소문자 변형, 공백, malformed 페이로드는 거부됩니다. 자격증명의 실제 값이나 해시를 문서, 명령줄, 쉘 히스토리, 로그에 기록하지 말고 외부의 승인된 생성·보관 절차를 사용합니다.

설정 바인딩, JPA 엔티티 매핑, 로그인 검증은 모두 하나의 `PasswordEncoderPolicy`를 사용합니다. 설정 사용자 목록 전체를 검증하고 엔티티로 매핑한 뒤에만 repository 호출을 시작하므로, 뒤쪽 항목이 잘못되어도 앞쪽 사용자만 일부 저장하지 않습니다. 검증 오류는 안정적인 설정 필드 경로만 제공하고 사용자명·비밀번호·해시 등 입력값을 반복하지 않습니다.

configured-user seed는 `local` 프로필이면서 `auth.persistence.mode=jpa`일 때만 실행됩니다. 이미 존재하는 username은 덮어쓰지 않으며, 여러 사용자를 저장하다 DB 실패가 발생하면 해당 seed 트랜잭션 전체를 rollback합니다. 외부 입력을 수정해 재시작하면 재시도할 수 있고, 동일한 정상 입력을 다시 제공해도 기존 사용자를 변경하지 않습니다.

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
- Flyway V70~V74 활성화
- Hibernate `ddl-auto=validate`
- `spring.sql.init.mode=never`
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
.\gradlew :auth:core:test :auth:api:test :auth:api:bootJar --no-daemon --console=plain --max-workers=1

$jar = Get-ChildItem auth/api/build/libs -Filter '*.jar' |
    Where-Object { $_.Name -notlike '*-plain.jar' } |
    Select-Object -First 1

java -jar $jar.FullName --spring.profiles.active=local --server.port=8081
```

테스트 범위에는 local resource 정책, H2/Flyway 컨텍스트, 로그인 실패 JPA commit, API/core 경계와 Boot JAR 패키징이 포함됩니다. `:auth:core:test`는 테스트 프로세스에서 임시 PostgreSQL 서버를 띄워 서로 다른 트랜잭션의 최초 실패, 기존 행 증가, 만료 및 성공 경합을 확인합니다. 이 테스트는 PostgreSQL 실행 파일을 시작할 수 있는 로컬 환경이 필요하며 운영 DB 계정은 사용하지 않습니다.

## fail-closed 확인

입력을 제거한 뒤 같은 JAR을 실행하면 Auth의 fail-closed 정책 때문에 시작에 실패해야 합니다.

```powershell
Remove-Item Env:AUTH_JWT_SECRET, Env:AUTH_INTERNAL_API_TOKEN -ErrorAction SilentlyContinue
java -jar $jar.FullName --spring.profiles.active=local --server.port=8081
```

local profile은 기본 사용자를 자동 생성하지 않습니다. 로그인 사용자는 저장소 밖의 승인된 configured-user 입력으로 제공하며, 비밀번호 기본값을 커밋하지 않습니다.

## 종료와 정리

`bootRun`과 JAR은 IntelliJ Stop 또는 `Ctrl+C`로 종료합니다. 검증 후 임시 환경변수와 Gradle 데몬을 정리합니다.

```powershell
Remove-Item Env:AUTH_JWT_SECRET, Env:AUTH_INTERNAL_API_TOKEN -ErrorAction SilentlyContinue
.\gradlew --stop
```

실제 개발·운영 credential, 외부 PostgreSQL, 기존 컨테이너는 이 로컬 검증 범위에 포함하지 않습니다.
