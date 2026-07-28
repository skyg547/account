# auth docs

`auth` 모듈은 로그인, JWT 발급, 역할 할당, token roleVersion 검증을 담당하는 인증/권한 모듈입니다.

## 읽기 순서

1. [beginner-guide.md](./beginner-guide.md) - 로그인, JWT, 역할 버전, 승인 재시도를 초보자 관점에서 설명합니다.
2. [process-flow.md](./process-flow.md) - API DTO부터 core와 출력 어댑터까지의 흐름을 정리합니다.
3. [schema.md](./schema.md) - 사용자, 역할, 로그인 실패, 승인 멱등 이력 테이블을 정리합니다.
4. [local-run.md](./local-run.md) - IntelliJ/Gradle에서 H2 또는 PostgreSQL로 실행하는 방법입니다.

## 현재 실행 전제

- `auth`는 Spring Boot 내장 WAS로 실행되는 API 앱입니다.
- `.run/Auth bootRun.run.xml`은 Config/Eureka 없이 H2, Flyway, JPA validate로 단독 실행합니다.
- H2와 PostgreSQL JDBC 드라이버가 런타임에 포함됩니다.
- 단위/통합 검증은 `.\gradlew :auth:test :auth:bootJar --console=plain --max-workers=1`로 수행합니다.

## 핵심 코드 입구

- `AuthApplication`: Spring Boot 진입점
- `AuthController`: HTTP DTO를 core command/result로 변환
- `AuthService`: 로그인 검증 순서와 역할 스냅샷 조정
- `AuthUserRoleAssignmentService`: 역할 정규화와 approval trace fingerprint 생성
- `AuthUser`, `RoleAssignment`: 계정 상태와 역할 유효성 도메인 규칙
- `JwtTokenIssuer`: 확정된 역할 스냅샷을 JWT claim으로 발급
- `JpaAuthUserRoleAssignmentAdapter`: 사용자별 lock, 역할 교체, 멱등 이력 저장
- `InMemoryAuthUserRoleAssignmentAdapter`: 로컬 memory 모드의 동일한 멱등 정책

## 현재 남은 코드 TODO

- 접두사 없는 레거시 평문 비밀번호 해시 승격과 운영 fail-closed
- 다중 노드 최초 로그인 실패 기록의 원자적 upsert/lock
- 승인 멱등 이력의 감사 보존/아카이브 정책
