# gateway docs

`gateway`는 외부 클라이언트가 여러 백엔드 서비스를 한 번에 접근할 수 있게 해 주는 단일 진입점입니다.

## 읽기 순서

1. [concept.md](./concept.md) - Gateway의 역할, 라우팅 모델, 필터 개념.
2. [local-run.md](./local-run.md) - IntelliJ와 Gradle 로컬 실행 방법.

## 현재 실행 전제

- `gateway`는 Spring Boot WebFlux 앱입니다.
- 라우트와 포트는 주로 `config-repo/gateway-service.yml`에서 관리합니다.
- Config Server import는 optional이지만, 실제 라우트 테스트는 Config Server를 먼저 띄우는 편이 안전합니다.
- 로컬 실행 설정은 `.run/Gateway bootRun.run.xml`을 사용합니다.
- 보호 라우트 JWT 검증은 Auth의 token-version API를 호출합니다. Auth를 함께 띄우지 않는 단독 smoke 실행에서는 `AUTH_TOKEN_VERSION_VALIDATION_ENABLED=false`를 설정합니다.

## 핵심 코드 입구

- `GatewayApplication`
- `JwtAuthenticationFilter`
- `RequestIdFilter`
- `ResponseHeaderFilter`
- `FallbackController`
