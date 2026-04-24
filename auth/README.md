# Auth Service

`auth` 모듈은 사용자 인증과 JWT 발급을 담당합니다.

## 주요 API

- `POST /api/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "1234"
}
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "admin",
  "roles": [
    "ROLE_ADMIN"
  ]
}
```

## 구조 (DDD + Hexagonal)

요청 흐름:

```text
Controller(api.web)
  -> UseCase(port.in)
  -> Application Service(core.application.service)
  -> Output Ports(core.application.port.out)
  -> Infrastructure Adapters(core.infrastructure.*)
```

패키지 역할:

- `com.ho.account.auth.api.dto`: 로그인 요청/응답 DTO
- `com.ho.account.auth.api.web`: 인증 컨트롤러, 예외 응답 처리
- `com.ho.account.auth.core.application.port.in`: 인증 유즈케이스
- `com.ho.account.auth.core.application.port.out`: 사용자 조회/비밀번호 검증/토큰 발급 포트
- `com.ho.account.auth.core.application.service`: 로그인 흐름 제어
- `com.ho.account.auth.core.domain.model`: 인증 도메인 모델
- `com.ho.account.auth.core.infrastructure.persistence`: 설정 기반 사용자 조회 어댑터
- `com.ho.account.auth.core.infrastructure.security`: 비밀번호 검증, JWT 발급 어댑터
- `com.ho.account.auth.core.infrastructure.config`: `auth.*` 설정 바인딩

## 설정

`application.yml` 기본값:

```yaml
auth:
  jwt:
    secret: ${AUTH_JWT_SECRET:kbank-account-system-super-secret-key-1234567890}
    issuer: ${AUTH_JWT_ISSUER:auth-service}
    expiration-seconds: ${AUTH_JWT_EXPIRATION_SECONDS:3600}
  users:
    - username: ${AUTH_DEFAULT_USERNAME:admin}
      password: ${AUTH_DEFAULT_PASSWORD:1234}
      active: true
      locked: false
      roles:
        - ROLE_ADMIN
```

`users[].password`는 평문 또는 Spring Security Delegating Password 형식(`{bcrypt}...`, `{noop}...`)을 지원합니다.

## 실행

```bash
./gradlew :auth:bootRun
```

## 테스트/빌드

```bash
./gradlew :auth:build
```
