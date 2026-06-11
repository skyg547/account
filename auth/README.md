# 🔑 Auth Service (인증 및 권한 모듈)

`auth` 모듈은 사용자의 로그인을 처리하고, 다른 모든 서비스에서 통용되는 '출입증(JWT 토큰)'을 발급하는 센터입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

MSA 시스템에서는 서버가 10개로 쪼개져 있습니다. 사용자가 `master-data`에 접근할 때 로그인하고, `journal-ledger`에 접근할 때 또 로그인하게 할 수는 없습니다.

그래서 사용자는 **딱 한 번 `auth` 모듈에 로그인**합니다.
성공하면 `auth` 모듈은 위조가 불가능한 **JWT(JSON Web Token)**라는 전자 출입증을 만들어 줍니다. 사용자는 이후 모든 요청마다 이 출입증을 보여주고, 다른 서버들은 "아, `auth` 부서에서 도장 찍어준 출입증이구나!" 하고 믿고 통과시켜 줍니다.

---

## 2. 🔄 처리 흐름 및 모듈 경계 (Process Flow)

### 📌 로그인 API
- `POST /api/auth/login`
- 사용자 이름(`username`)과 비밀번호를 받아, 맞으면 JWT 토큰(`roles`, `departmentCode`, `roleVersion`, 승인된 역할 할당 정보)을 반환합니다.
- 역할은 단순 문자열이 아니라 `RoleAssignment` 값 객체로 관리하며, 승인 여부와 유효기간을 통과한 역할만 토큰과 응답에 포함합니다.

### 📌 헥사고날 아키텍처 (DDD)
- **Controller:** 로그인 요청 수신
- **UseCase -> Service:** 로그인 흐름 제어
- **Infrastructure:** JPA 기반 사용자/역할 할당 조회, 설정 기반 초기 사용자 seed, 비밀번호 검증(Bcrypt 등), JWT 발급 어댑터 구현

### 📌 사용자/역할 영속화
- 기본 모드는 `auth.persistence.mode=jpa`입니다.
- `AUTH_PERSISTENCE_MODE=memory`로 설정하면 로컬 데모용 설정 기반 인메모리 사용자 저장소를 사용합니다.
- 운영 기본 테이블은 Flyway `V70__auth_user_role_schema.sql`이 생성합니다.
  - `AUTH_USERS`: 사용자 식별자, 저장 비밀번호, 부서 코드, 활성/잠금 상태, `roleVersion`
  - `AUTH_ROLE_ASSIGNMENTS`: 승인 상태, 유효기간, 데이터 범위를 포함한 역할 할당
- 초기 사용자는 DB에 없을 때만 `auth.users` 설정에서 seed합니다. 비밀번호는 마이그레이션에 하드코딩하지 않고 환경변수/설정값을 따릅니다.

### 📌 토큰 Role Version 검증
- `POST /api/auth/validate-token-version`
- 요청의 `username`, `roleVersion`이 현재 DB의 사용자 `roleVersion`과 정확히 일치할 때만 유효로 판단합니다.
- 역할 변경으로 DB의 `roleVersion`이 증가하면 기존 JWT는 재로그인이 필요합니다.

### 📌 로그인 실패 감사와 임시 잠금
- 로그인 성공/실패는 `LoginAttemptPort`를 통해 감사 이벤트로 기록됩니다.
- 기본 정책은 연속 5회 실패 시 15분 동안 임시 잠금입니다.
- `AUTH_LOGIN_MAX_FAILURES`, `AUTH_LOGIN_LOCK_DURATION_MINUTES`로 정책을 조정할 수 있습니다.
- 기본 어댑터는 단일 인스턴스용 인메모리 구현입니다. 다중 인스턴스 운영에서는 Redis/DB 기반 어댑터로 교체해야 모든 서버가 같은 잠금 상태를 공유합니다.

### 📌 내부 역할 할당 반영 API
- `POST /api/auth/internal/users/{username}/role-assignments`
- Governance 승인 완료 후 호출되는 내부 API입니다.
- 요청 헤더 `X-Internal-Auth-Token`이 `auth.internal-api.token`과 일치해야만 처리합니다.
- 요청 본문은 `roleCodes`, `dataScope`, `validFrom`, `validTo`, `approvedBy`, `approvalTraceId`를 받습니다.
- 기존 역할 할당을 승인된 새 목록으로 교체하고 `roleVersion`을 1 증가시켜 기존 JWT를 만료 대상으로 만듭니다.

### 🚨 모듈 경계 (중요!)
- 사용자 식별과 권한 부여는 `auth`가 담당합니다.
- 하지만 **부서 정보(조직 구조)**는 `auth`가 소유하지 않습니다. 부서는 `master-data` 모듈의 소유입니다.
- `auth`는 사용자의 `departmentCode`만 글자(참조값)로 보관하며, 상세 정보가 필요할 때는 `master-data`(`GET /api/basic/departments/{departmentCode}`)를 호출하여 확인합니다.

---

## 3. 🧭 실행 방법 (Docker & Local)

상세 문서는 [docs/README.md](./docs/README.md)에서 `beginner-guide`, `process-flow`, `schema`, `local-run` 순서로 확인합니다.

**IntelliJ 로컬 실행:**
1. `Config Server bootRun`을 먼저 실행합니다.
2. Eureka 등록까지 확인하려면 `Discovery bootRun`을 실행합니다.
3. `Auth bootRun`을 실행합니다.

**최신 엔터프라이즈 Docker 환경 (권장):**
이 모듈은 멀티스테이지 Dockerfile을 통해 빌드되며, 통합 환경에서 Eureka/Config 의존성을 물고 자동으로 구동됩니다.
```bash
docker-compose up -d auth
```

**PowerShell 로컬 실행:**
```powershell
.\gradlew :auth:bootRun --console=plain
```

**PowerShell 검증:**
```powershell
.\gradlew :auth:test --console=plain --max-workers=1 --no-daemon
```

**내부 API 토큰 설정:**
```powershell
$env:AUTH_INTERNAL_API_TOKEN='local-internal-auth-token'
```
