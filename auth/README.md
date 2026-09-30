# 🔑 Auth Service (인증 및 권한 모듈)

`auth` 모듈은 사용자의 로그인을 처리하고, 다른 모든 서비스에서 통용되는 출입증인 JWT를 발급하는 센터입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

MSA 시스템에서는 서버가 여러 개로 나뉩니다. 사용자가 `master-data`에 접근할 때 로그인하고, `journal-ledger`에 접근할 때 다시 로그인하게 할 수는 없습니다.

그래서 사용자는 **한 번 `auth` 모듈에 로그인**합니다. 성공하면 `auth`는 서명된 **JWT(JSON Web Token)**를 발급합니다. audience는 `AUTH_JWT_AUDIENCE`로 설정하며 기본값은 `account-api`입니다. Gateway는 서명·issuer·시간·역할 버전을 검사하고, Auth 관리자 API와 Master Data 수신 검증기는 audience도 확인합니다. Auth 발급기와 Master Data 검증기의 audience 설정은 같은 값이어야 합니다.

---

## 2. 🔄 처리 흐름 및 모듈 경계 (Process Flow)

### 📌 로그인 API

- `POST /api/auth/login`
- API의 `LoginRequest`는 core `LoginCommand`로 변환됩니다.
- 현재 성공을 허용하는 로그인 유형은 대소문자를 구분하지 않는 명시적 `NORMAL`뿐입니다. `AuthService`는 사용자, 비밀번호, 활성/잠금, 부서, 유효 역할을 순서대로 확인합니다.
- SSO 신뢰 공급자가 연결되어 있지 않으므로 클라이언트가 `SSO`를 지정해도 일반적인 자격 증명 오류로 fail-closed 처리하며 JWT를 발급하지 않습니다. null, 공백, 그 밖의 알 수 없는 로그인 유형도 같은 공개 오류로 거부합니다. 이 요청들은 자격 증명 검증이 시작되지 않으므로 로그인 실패 횟수나 임시 잠금 카운터를 소비하지 않습니다.
- LDAP 로그인은 실제 OTP 검증 공급자가 연결되어 있지 않으므로 모든 OTP 값을 신뢰하지 않고 일반적인 자격 증명 오류로 fail-closed 처리합니다. 저장소에는 고정 OTP나 기본 OTP가 없습니다.
- 로그인 한 시점의 유효 역할을 확정해 같은 목록을 `AuthenticationResult`, JWT `roles`, JWT `roleAssignments`에 사용합니다.
- Controller가 core 결과를 `LoginResponse`로 변환하므로 core는 HTTP DTO를 참조하지 않습니다.

### 📌 헥사고날 아키텍처와 DDD

- **Inbound Adapter:** `AuthController`, request/response DTO, Bean Validation
- **Input Port / Application:** `AuthUseCase`, `AuthUserRoleAssignmentUseCase`, `AuthService`, `AuthUserRoleAssignmentService`
- **Domain:** `AuthUser`, `RoleAssignment`과 유효기간/승인 역할 계산
- **Output Port:** 사용자 조회, 비밀번호 검증, 로그인 실패 저장, 부서 검증, JWT 발급, 역할 교체
- **Transaction Boundary:** 로그인 서비스는 원격 호출까지 감싸는 장기 트랜잭션을 열지 않고 JPA 어댑터가 짧은 읽기/쓰기 경계를 소유
- **Infrastructure Adapter:** JPA/메모리 저장소, master-data RestClient, JWT, 로그인 실패 저장소

### 📌 사용자/역할 영속화

- 기본 모드는 `auth.persistence.mode=jpa`입니다.
- `AUTH_PERSISTENCE_MODE=memory`는 로컬 데모용 인메모리 사용자 저장소입니다.
- JPA configured-user seed는 `local` 프로필과 JPA 영속화 모드가 모두 활성화된 경우에만 실행됩니다. 저장소에 이미 있는 username은 덮어쓰지 않습니다.
- Flyway 마이그레이션:
  - `V70__auth_user_role_schema.sql`: 사용자와 역할 할당
  - `V71__auth_login_attempts.sql`: 공유 로그인 실패/잠금
  - `V72__auth_role_assignment_apply_log.sql`: Governance 승인 반영 멱등 이력
- memory와 JPA 모드 모두 역할의 `dataScope`, `validFrom`, `validTo`를 보존합니다.

### 📌 토큰 Role Version 검증

- `POST /api/auth/validate-token-version`
- 현재 DB의 `roleVersion`과 JWT 값이 같아야 합니다.
- 사용자가 비활성, 관리 잠금 또는 유효 역할 없음 상태이면 버전이 같아도 `valid=false`입니다.
- 역할 변경으로 `roleVersion`이 증가하면 기존 JWT는 재로그인이 필요합니다.

### 📌 로그인 실패 감사와 임시 잠금

- 로그인 성공/실패는 `LoginAttemptPort`로 기록합니다.
- 기본 정책은 연속 5회 실패 시 15분 잠금입니다.
- 단일 인스턴스는 memory, 다중 인스턴스는 `AUTH_LOGIN_SECURITY_STORE=jpa`를 사용합니다.
- 기존 행의 공유 저장은 구현되어 있지만 여러 노드의 최초 실패 동시 insert를 원자화하는 후속 `@todo`가 남아 있습니다.

### 📌 Governance 역할 할당 반영과 멱등성

- `POST /api/auth/internal/users/{username}/role-assignments`
- `X-Internal-Auth-Token`이 일치해야 하며 `approvalTraceId`는 필수입니다.
- 최초 요청은 현재 역할 목록을 교체하고 `roleVersion`을 1 증가시킵니다.
- 같은 trace와 같은 내용의 재시도는 이미 적용된 요청으로 판단해 역할과 버전을 다시 바꾸지 않습니다.
- 같은 trace를 다른 사용자나 역할 내용에 재사용하면 fail-closed 예외로 중단합니다.

### 📌 통합 비밀번호 인코딩 정책 (PasswordEncoderPolicy)

- **BCrypt 구조 및 포맷 검증**:
  - 현재 승인된 저장 형식은 정확한 소문자 `{bcrypt}` 접두사와 구조적으로 유효한 BCrypt 페이로드의 조합뿐입니다. 접두사는 필수이며 대소문자 변형이나 앞뒤 공백을 허용하지 않습니다.
  - BCrypt 페이로드 Regex: `^\$2[aby]\$[0-9]{2}\$[./0-9A-Za-z]{53}$`
  - 지원 버전: `$2a$`, `$2b$`, `$2y$`
  - Cost factor: `04` ~ `31` (04 미만 또는 31 초과 시 거부)
  - 페이로드 길이: 접두사 제외 정확히 60자 (Radix64 문자 집합: `.` `/` `0-9` `A-Z` `a-z`)
- **Fail-Closed 검증 통합**:
  - 설정 바인딩(`AuthModuleProperties`), JPA 엔티티 매핑(`AuthUserJpaEntity`), 로그인 검증(`DelegatingPasswordVerifier`)이 하나의 `PasswordEncoderPolicy`를 공유합니다.
  - 평문(raw), `{noop}`, 미지원 접두사, 접두사 없음, 대소문자 변형, 공백 포함, malformed 페이로드는 유효한 자격으로 저장·인증되지 않습니다.
  - configured-user 목록은 repository를 호출하기 전에 전체 검증·매핑됩니다. 사용자 하나라도 잘못되면 일부 seed를 시작하지 않고, 오류 메시지에 사용자명이나 자격증명 입력값을 반영하지 않습니다.
  - seed 저장의 뒤쪽에서 DB 실패가 발생하면 전체 트랜잭션이 rollback됩니다. 외부 설정을 수정한 뒤 재시작하면 안전하게 재시도할 수 있고, 이미 존재하는 사용자는 변경하지 않습니다.
- **검증 어댑터 격리 (`DelegatingPasswordVerifier`)**:
  - 저장된 비밀번호가 올바른 BCrypt 포맷이 아닌 경우 예외(500)를 발생시키지 않고 안전하게 `false`를 반환하며, 로그나 에러 메시지에 민감한 비밀번호 페이로드를 노출하지 않습니다.
- **테스트 규칙**:
  - 저장소에 정적 비밀번호, 평문, 하드코딩된 해시 문자열을 커밋하지 않으며, 테스트 시 `BCryptPasswordEncoder` 또는 `PasswordEncoderPolicy.encode()`를 통해 런타임에 동적으로 자격 증명을 생성합니다.

### 🚨 모듈 경계

- 사용자 식별, 인증 상태, 역할 할당은 `auth`가 소유합니다.
- 부서 조직 구조는 `master-data`가 소유합니다.
- `auth`는 `departmentCode`만 참조값으로 저장하고 로그인 시 master-data 출력 어댑터로 존재 여부를 확인합니다.

---

## 3. 🧭 실행 방법

상세 문서는 [docs/README.md](./docs/README.md)에서 `beginner-guide`, `process-flow`, `schema`, `local-run` 순서로 확인합니다.

**IntelliJ H2 단독 실행:**
1. Gradle JVM을 JDK 17로 설정하고 Gradle Reload를 실행합니다.
2. 실행 구성에 `local` 프로파일과 JWT/internal-token 환경변수를 필수로 주입합니다. 로그인 사용자가 필요하면 저장소 밖의 승인된 런타임 입력으로 configured user를 추가합니다.
3. `Auth bootRun`을 실행합니다.
4. Config Server/Eureka/PostgreSQL 없이 내장 WAS가 `8081` 포트에서 시작됩니다.
5. Flyway V70~V73 적용 후 Hibernate가 스키마를 검증합니다.

local SQL 데모 credential과 기본 사용자는 없으며 `spring.sql.init.mode=never`입니다. 선택적 configured user는 외부 런타임 입력/placeholder인 `AUTH_USERS_0_USERNAME`, `AUTH_USERS_0_PASSWORD` 등으로 제공하고, 저장소 파일·쉘 히스토리·로그에 값을 남기지 않습니다. `AUTH_USERS_0_PASSWORD`는 외부에서 준비한 정책 준수 인코딩 값이어야 합니다.

저장소에는 local JWT secret, internal token, 데모 사용자 비밀번호 기본값을 두지 않습니다. PowerShell에서는 값을 출력하지 않고 다음처럼 현재 프로세스에만 생성합니다.

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
.\gradlew :auth:api:bootRun --args='--spring.profiles.active=local'

Remove-Item Env:AUTH_JWT_SECRET, Env:AUTH_INTERNAL_API_TOKEN -ErrorAction SilentlyContinue
```

**PowerShell 검증:**

```powershell
.\gradlew :auth:core:test :auth:api:test :auth:api:bootJar --no-daemon --console=plain --max-workers=1
```

**통합 Docker 실행 기록:**

기존 통합 환경에서는 멀티스테이지 Dockerfile과 Eureka/Config 구성을 사용합니다.

```bash
docker-compose up -d auth
```

`auth/docker-compose.yml`은 Redis 접속 정보(`SPRING_DATA_REDIS_HOST=account-redis`, `SPRING_DATA_REDIS_PORT=6379`)를
포함합니다. 빠지면 Spring Boot 기본값(`localhost`)으로 접속을 시도해 `account-redis` 컨테이너를 찾지 못하고
`/actuator/health`가 503을 반환합니다. (관련: [GH-474](https://github.com/skyg547/account/issues/474))

현재 변경의 실제 검증 기준은 H2 단독 Gradle 실행입니다. Docker와 실제 PostgreSQL/Flyway는 별도 통합 환경에서 확인해야 합니다.

상세한 JAR 실행과 fail-closed 확인 절차는 [docs/local-run.md](./docs/local-run.md)를 따릅니다.

## 4. 남은 운영 고도화

- 다중 노드 최초 로그인 실패 insert를 원자적 upsert 또는 DB lock으로 바꿔야 합니다.
- 승인 멱등 이력은 감사 보존기간과 최대 재시도 기간을 고려한 archive/retention 정책이 필요합니다.
