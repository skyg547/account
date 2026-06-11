# auth docs

`auth` 모듈은 로그인, JWT 발급, 역할 할당, 토큰 roleVersion 검증을 담당하는 인증/권한 기반 모듈입니다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 로그인, JWT, 역할 버전을 초보자 관점에서 설명합니다.
2. [process-flow.md](./process-flow.md) - API, 서비스, Governance 내부 연동 흐름을 정리합니다.
3. [schema.md](./schema.md) - 사용자와 역할 할당 테이블, 설정 키를 정리합니다.
4. [local-run.md](./local-run.md) - IntelliJ와 Gradle로 로컬 실행/검증하는 방법입니다.

## 현재 실행 전제

- `auth`는 Spring Boot 앱입니다.
- Config Server import는 optional입니다.
- 로컬 실행 설정은 `.run/Auth bootRun.run.xml`을 사용합니다.
- 단위 검증은 `.\gradlew :auth:test --console=plain --max-workers=1 --no-daemon`로 수행합니다.

## 핵심 코드 입구

- `AuthApplication`
- `AuthController`
- `AuthService`
- `AuthUserRoleAssignmentService`
- `JwtTokenIssuer`
- `InMemoryLoginAttemptAdapter`
- `JpaAuthUserPersistenceAdapter`
