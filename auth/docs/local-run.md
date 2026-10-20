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

SSO/OTP도 기본 비활성화입니다. `local`에서만 `auth.sso.enabled=true`와 공급자별 identity를 런타임에 주입하면 설정 기반 SSO를, `auth.otp.enabled=true`와 사용자별 Base32 secret을 주입하면 TOTP를 시험할 수 있습니다. `NORMAL`은 해당 사용자 OTP 설정이나 제출이 없으면 OTP 없이 진행하고, `LDAP`는 항상 OTP가 필요합니다. `SSO`는 공급자 subject가 요청 username 및 등록 사용자와 일치해야 합니다. 테스트용 자격·secret 값은 저장소와 명령줄에 남기지 않습니다. 이 local 어댑터는 운영 OAuth2/OIDC 연동과 인스턴스 간 OTP 재사용 방지 기능을 제공하지 않습니다.

### memory 사용자 저장소로 실행

같은 `local` 프로파일에서 사용자·역할 저장소만 메모리 어댑터로 바꾸려면, 위에서 생성한 임시 보안 입력을 유지한 채 실행 인자로 모드를 지정합니다. `local`의 기본값은 JPA이므로 명시적 실행 인자가 필요합니다.

```powershell
.\gradlew :auth:api:bootRun --args='--spring.profiles.active=local --auth.persistence.mode=memory --auth.login-security.store=memory --server.port=8081' --console=plain --max-workers=1
```

두 모드 모두 전체 Auth 애플리케이션이 기동됩니다. PAT 관리 경로(`POST/GET/DELETE /api/auth/pat` 및 `/api/auth/admin/pat`)는 **JPA 모드에서만** 등록되며, memory 모드에서는 경로가 없어 HTTP 404를 반환합니다. memory 모드에는 PAT 발급·조회·폐기 또는 PAT 감사 저장 기능이 없습니다. PAT를 시험하려면 기본 JPA 모드를 사용합니다. 이 memory 모드는 로컬 데모용이며 재시작 시 사용자·역할 상태가 사라집니다.

## PowerShell H2 API 실행

보안 입력을 생성한 현재 PowerShell에서 실행합니다.

```powershell
.\gradlew :auth:api:bootRun --args='--spring.profiles.active=local --server.port=8081' --console=plain --max-workers=1
```

- API: `http://localhost:8081`
- JDBC: H2 memory database, PostgreSQL compatibility mode
- Config Server/Eureka/Master Data 원격 호출: 비활성화

## 원격 부서 조회 대기 시간

`auth.master-data.enabled=true`로 원격 부서 검증을 사용하는 환경에서 `masterDataRestClient`는 연결 대기와 응답 읽기 대기를 제한합니다. 값을 생략하면 다음 기본값을 사용합니다. 환경변수는 실행 환경의 승인된 설정 주입 경로에서 지정합니다.

| 설정 키 | 환경변수 | 기본값 | 허용 범위 |
| --- | --- | ---: | ---: |
| `auth.master-data.connect-timeout-millis` | `AUTH_MASTER_DATA_CONNECT_TIMEOUT_MILLIS` | 1000 ms | 1~10000 ms |
| `auth.master-data.read-timeout-millis` | `AUTH_MASTER_DATA_READ_TIMEOUT_MILLIS` | 3000 ms | 1~10000 ms |

두 설정 중 하나라도 0, 음수, 10000 ms 초과 또는 숫자가 아닌 값이면 기동 시 설정 필드 경로만 포함한 오류로 실패합니다. 원격 연결 실패나 읽기 시간 초과는 부서 검증 불가로 처리되며 JWT를 발급하지 않습니다. 읽기 제한은 **응답 바이트가 오지 않는 동안의 대기 시간**입니다. 상대 서비스가 바이트를 천천히 계속 보내면 전체 요청 시간은 이 값보다 길어질 수 있습니다.

기본 `local` 프로파일은 `auth.master-data.enabled=false`로 로컬 부서 검증 어댑터를 선택하므로 원격 조회를 호출하지 않습니다. 이 대기 시간 설정은 로컬 어댑터의 승인 동작을 바꾸지 않습니다.

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

테스트 범위에는 local resource 정책, H2/Flyway 컨텍스트, 로그인 실패 JPA commit, API/core 경계와 Boot JAR 패키징이 포함됩니다. `:auth:api:test`의 `EnterpriseSsoOtpHttpIntegrationTest`는 local 설정 기반 SSO/TOTP의 JWT 발급과 실패·잠금을 H2에서 확인합니다. `:auth:core:test`의 `JpaLoginAttemptPostgresConcurrencyTest`는 테스트 프로세스의 임시 PostgreSQL에서 별도 트랜잭션의 동시 최초 삽입, 기존 행 증가, 만료 및 성공 경합을 확인합니다. 이 테스트는 PostgreSQL 실행 파일을 시작할 수 있는 로컬 환경이 필요하며 운영 DB 계정은 사용하지 않습니다.

`AuthJpaModeStartupTest`와 `AuthMemoryModeStartupTest`는 임시 자격증명을 테스트 실행 중 생성해 전체 `AuthApplication` 빈 그래프를 각각 시작하고 PAT HTTP 경로의 존재 여부를 확인합니다. JPA PAT의 실제 생성·조회·폐기 및 감사 동작은 `PersonalAccessTokenHttpIntegrationTest`가 H2에서 검사합니다. H2 검증은 PostgreSQL 검증을 대신하지 않습니다.

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
