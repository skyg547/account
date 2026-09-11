# 🛡️ Internal Audit (내부 감사 모듈)

Internal Audit 서비스는 RCM(Risk Control Matrix) 및 설계·운영 평가, 내부통제 감사 이력을 관리하는 헥사고날 아키텍처 기반 모듈입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

1. **RCM (Risk Control Matrix, 리스크 통제 행렬):** 기업 내부 프로세스의 통제 목적, 리스크 및 이에 대응하는 통제 활동(Control Activity)을 관리하는 핵심 명세서입니다.
2. **독립 단독 런타임 (Self-Contained Local Runtime):** 로컬 개발 환경에서 외부 PostgreSQL DB, Spring Cloud Config, Eureka, Vault 등 외부 제어 서버 연결 없이 단독으로 빠르게 실행 가능한 오프라인 격리 환경입니다.
3. **Flyway V60 타깃 스키마 & JPA Validate:** 로컬 H2 인메모리 DB 구동 시 Flyway V60 마이그레이션으로 DB 테이블을 자동 생성하고, JPA `validate`로 스키마-엔티티 매핑 정합성을 엄격히 검증합니다.

---

## 2. 🏛️ 모듈 구조 (Module Architecture)

- `internal-audit:core`: domain, application port/service, JPA outbound adapter를 소유하는 순수 Java 라이브러리(`java-library`)
- `internal-audit:api`: Controller, Spring Boot composition root, HTTP REST API를 제공하는 유일한 실행 모듈(`bootJar`)

> **Note:** 구체적인 Job/Step 계약이 없는 Batch는 실행 모듈로 유지하지 않는 모듈 격리 원칙을 준수합니다.

---

## 3. 🧭 로컬 독립 단독 구동 가이드 (Self-Contained Local H2 Guide)

`local` 프로파일은 외부 환경 변수 주입이나 외부 인프라 자원(Config Server, Eureka, Vault, PostgreSQL) 없이 완전 오프라인 독립 단독 구동을 보장합니다.

### 📌 런타임 제어 평면 비활성화 & 격리 메커니즘
- **H2 In-Memory DB**: PostgreSQL 호환 모드 (`MODE=PostgreSQL`) 기반 H2 DB 적용
- **Flyway V60 Target**: `classpath:db/migration` 내 Flyway V60 마이그레이션 스크립트로 스키마 구축
- **JPA Validate**: Hibernate `ddl-auto: validate`로 런타임 스키마 정합성 검증
- **Control Plane Decoupling**: Spring Cloud Config, Eureka Discovery, Vault, Tracing 완전 비활성화

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

- **필수 환경변수:** `INTERNAL_AUDIT_DB_URL`, `INTERNAL_AUDIT_DB_USER`, `INTERNAL_AUDIT_DB_PASSWORD`

기존 개발 인프라에 `internal_audit_db`를 준비하고 Eureka에 등록하는 절차는
[Governance 개발 실행 안내](../docs/guides/governance-external-dev.md)를 참고하세요.
