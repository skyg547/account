# 🛡️ Internal Audit (내부 감사 모듈)

Internal Audit 서비스는 RCM(Risk Control Matrix) 및 설계·운영 평가, 내부통제 감사 이력을 관리하는 헥사고날 아키텍처 기반 모듈입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

1. **RCM (Risk Control Matrix, 리스크 통제 행렬):** 기업 내부 프로세스의 통제 목적, 리스크 및 이에 대응하는 통제 활동(Control Activity)을 관리하는 핵심 명세서입니다.
2. **로컬 런타임:** `local` 프로파일은 H2를 사용하고 Spring Cloud Config, Eureka, Vault, PostgreSQL 없이 기동할 수 있습니다. RCM·평가 API 요청을 처리하려면 별도로 JWT 검증 설정, Auth 연결, 로컬 기능 허가 설정이 필요합니다.
3. **Flyway V60–V62 타깃 스키마 & JPA Validate:** 로컬 H2 인메모리 DB 구동 시 Flyway V60–V62 마이그레이션으로 DB 테이블을 자동 생성하고, JPA `validate`로 스키마-엔티티 매핑 정합성을 엄격히 검증합니다.

---

## 2. 🏛️ 모듈 구조 (Module Architecture)

- `internal-audit:core`: domain, application port/service, JPA outbound adapter를 소유하는 순수 Java 라이브러리(`java-library`)
- `internal-audit:api`: Controller, Spring Boot composition root, HTTP REST API를 제공하는 유일한 실행 모듈(`bootJar`)

> **Note:** 구체적인 Job/Step 계약이 없는 Batch는 실행 모듈로 유지하지 않는 모듈 격리 원칙을 준수합니다.

---

## 3. 🧭 로컬 독립 단독 구동 가이드 (Self-Contained Local H2 Guide)

`local` 프로파일은 Config Server, Eureka, Vault, PostgreSQL 없이 기동할 수 있습니다. 보호된 RCM·평가 API는 모든 프로파일에서 Auth에 연결할 수 있어야 하며 로컬 기능 권한 매트릭스를 설정해야 합니다. 신원 검증 설정이나 Auth 연결이 없으면 요청을 503으로 거절합니다.

### 📌 런타임 제어 평면 비활성화 & 격리 메커니즘
- **H2 In-Memory DB**: PostgreSQL 호환 모드 (`MODE=PostgreSQL`) 기반 H2 DB 적용
- **Flyway V60–V62 Target**: `classpath:db/migration` 내 Flyway V60–V62 마이그레이션 스크립트로 스키마 구축
- **JPA Validate**: Hibernate `ddl-auto: validate`로 런타임 스키마 정합성 검증
- **Control Plane Decoupling**: Spring Cloud Config, Eureka Discovery, Vault, Tracing 비활성화. 감사 API의 Auth roleVersion 조회와 로컬 기능 권한 설정은 별도로 필요

### 📌 PowerShell 검증 및 실행 명령

**1) 테스트 및 빌드 검증:**
```powershell
.\gradlew.bat :internal-audit:core:test :internal-audit:api:test :internal-audit:api:bootJar --console=plain
```

**2) 로컬 `local` 프로파일 bootRun 실행:**
```powershell
.\gradlew.bat :internal-audit:api:bootRun --args="--spring.profiles.active=local" --console=plain
```

**3) 실행 가능 JAR로 직접 실행:**
```powershell
java -jar .\internal-audit\api\build\libs\account-internal-audit-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

- **기본 포트:** `8083`
- **Readiness Probe:** `http://localhost:8083/actuator/health/readiness`
- **Health Check:** `http://localhost:8083/actuator/health`

---

## 4. 🐳 Development / Production PostgreSQL

`dev` 및 `prod` 프로파일은 로컬 H2로 Fallback하지 않으며, 외부 주입된 PostgreSQL DB를 사용합니다.

- **Dev/Prod 정책:** 외부 PostgreSQL URL, 사용자, 비밀번호를 환경변수 또는 Secret으로만 전달받아 동작하며, `ddl-auto=validate`로 스키마 구조를 검증합니다.
- **Docker Compose 단독 렌더링 및 실행:**

```powershell
docker compose -f .\internal-audit\docker-compose.yml config --quiet
docker compose -f .\internal-audit\docker-compose.yml up --build internal-audit-api
```

- **필수 환경변수:** `INTERNAL_AUDIT_DB_URL`, `INTERNAL_AUDIT_DB_USER`, `INTERNAL_AUDIT_DB_PASSWORD`, `AUTH_JWT_SECRET`, `INTERNAL_AUDIT_AUTH_BASE_URL`, `INTERNAL_AUDIT_PERMISSION_GRANTS`. `AUTH_JWT_ISSUER`는 기본값 `auth-service`이며 실제 Auth 발급 설정과 일치해야 합니다.

기존 개발 인프라에 `internal_audit_db`를 준비하고 Eureka에 등록하는 절차는
[Governance 개발 실행 안내](../docs/guides/governance-external-dev.md)를 참고하세요.

## 5. 🔐 RCM·평가 API 인증과 권한

처음 호출할 때는 Auth가 발급한 Bearer JWT를 준비하고, `INTERNAL_AUDIT_PERMISSION_GRANTS`에 필요한 허가를 `ROLE:FUNCTION:ACCESS` 형식으로 설정합니다. 예를 들어 감사인의 RCM 조회 허가는 `ROLE_AUDITOR:INTERNAL_AUDIT.RCM:READ`입니다. JWT 서명은 API가 `AUTH_JWT_SECRET`으로 HS256 검증하고 발급자(`AUTH_JWT_ISSUER`), 유효 기간, `sub`, `roles`, `roleVersion`을 확인합니다. `AUTH_JWT_SECRET`은 32바이트 이상이어야 합니다. Gateway 경유 요청도 API에서 같은 검증을 다시 받습니다.

요청마다 API는 `INTERNAL_AUDIT_AUTH_BASE_URL`의 `POST /api/auth/validate-token-version`으로 현재 roleVersion을 확인합니다. 이어 모듈 설정 `internal-audit.identity.permission-grants`의 로컬 허가 매트릭스와 JWT 역할을 대조합니다. RCM 경로는 `INTERNAL_AUDIT.RCM`, 평가 경로는 `INTERNAL_AUDIT.EVALUATION`을 사용하며 GET은 `READ`, POST는 `WRITE`입니다. 일치하는 허가가 있어야 업무 포트가 호출됩니다. 감사 actor는 검증된 JWT의 `sub`에서 가져오며 요청의 `X-Auth-*` 신원 헤더는 사용하지 않습니다.

| 결과 | HTTP 상태 |
| --- | --- |
| Bearer 토큰 없이 `X-Auth-*` 헤더만 전송, 무효 토큰, 회수된 roleVersion | 401 |
| 유효한 역할이지만 해당 기능·접근 유형의 허가가 없음 | 403 |
| 서명 키·Auth 설정 누락 또는 Auth 검증 실패 | 503 |

로컬 허가 매트릭스는 중앙 Governance의 권한 부여 상태와 자동 동기화되지 않습니다. 배포 시 역할별 허가를 별도로 검토하고 설정을 갱신해야 합니다. 또한 Auth의 현재 버전 검사 API는 **활성 역할 배정이 하나라도 있는지** 확인합니다. 만료된 감사인 역할과 다른 활성 역할이 함께 있으면, 아직 유효한 JWT 안의 감사인 역할이 만료되었는지 이 API만으로 판별할 수 없습니다. 역할별 유효성 계약이 Auth에 추가될 때까지 남는 보안 게이트입니다. 운영에서는 역할 만료 시 roleVersion 갱신과 토큰 회수 절차를 확인해야 합니다.

저장소 루트에서 회귀 검증을 실행합니다.

```bash
./gradlew :internal-audit:api:test :internal-audit:core:test --offline --no-daemon --console=plain --max-workers=1
```

로컬 HTTP 스모크에서는 `:internal-audit:api:bootJar`와 `:gateway:bootJar`를 빌드하고, H2 기반 API를 `127.0.0.1:18083`, Auth 응답 스텁을 `127.0.0.1:18084`, 단일 로컬 라우트로 API에 연결한 Gateway를 `127.0.0.1:18080`에서 실행했습니다. API에는 테스트용 HS256 키와 정확히 일치하는 로컬 허가를 설정했습니다.

| 경로 | 관찰한 결과 |
| --- | --- |
| API 직접 호출: 헤더만 있는 GET/POST | 401 |
| API 직접 호출: 서명된 JWT와 위조 `X-Auth-*` 헤더의 GET/POST | 200; POST의 `ownerId`는 헤더가 아닌 JWT `sub` |
| API 직접 호출: 잘못된 서명 또는 roleVersion 2 | 401 |
| API 직접 호출: 로컬 허가가 없는 ADMIN 역할 | 403 |
| Gateway 경유: 헤더만 있는 GET | 401 |
| Gateway 경유: 유효 Bearer JWT와 위조 헤더의 GET | 200 |
| Gateway 경유: roleVersion 2의 GET | 401 |

이는 실제 Auth·Governance 연결 검증이 아닙니다. Auth는 로컬 스텁이었고 API가 그 스텁의 버전 응답을 요청마다 확인했습니다. Gateway의 자체 버전 검사는 이 로컬 실행에서 비활성화했습니다. 운영 Auth, 중앙 권한 변경 동기화, 실제 배포 네트워크 접근 범위는 별도 검증이 필요합니다.


## 명령 재시도와 키 충돌

여섯 RCM/평가 쓰기 API의 `X-Idempotency-Key`는 모듈 전체에서 유효하다.
동일한 유효 명령은 최초 성공 결과를 재생하고, 다른 actor/action/대상/payload는 409다.
기존 감사 로그만 있는 키도 409이며 자동 승격하지 않는다. 키 없음/공백은 매 요청을 실행·감사한다.
과거 결과를 재생해도 이후 업무 변경을 되돌리지 않는다. 과거 키의 실패를 새 키 일괄 재전송으로 처리하지 않는다.
정확한 fingerprint, snapshot, 오류 및 전환 계약은 [GH-665 계약](docs/issue-665-command-idempotency-plan.md)을 참고한다.

로컬은 V62까지 자동 적용한다. PostgreSQL은 release migration-runner가 V62를 적용한 뒤 새 writer를 시작해야 한다.
기존 writer는 새 키 잠금에 참여하지 않으므로 배포 시 쓰기를 중단하고 모든 writer를 교체해야 한다.
애플리케이션만 이전 버전으로 되돌리면 재시도 보호를 잃는다. 감사 데이터와 적용 migration은 삭제하지 않는다.

검증 명령(격리 DB 준비·기동 절차는 위 계약 참고):
```bash
sh ./gradlew :internal-audit:core:test :internal-audit:api:test :migration-runner:test :internal-audit:api:bootJar --offline --no-daemon --console=plain --max-workers=1
sh ./gradlew :internal-audit:api:postgresTest -DinternalAudit.test.postgresql.url=jdbc:postgresql://127.0.0.1:5432/account_idempotency_test --offline --no-daemon --console=plain --max-workers=1
```
