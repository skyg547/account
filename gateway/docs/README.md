# gateway docs

`gateway`는 외부 클라이언트와 내부 회계/자산운용 서비스 사이의 단일 진입점입니다. 이 문서는 단순 기동뿐 아니라 요청이 어느 필터와 포트를 거쳐 업무 서비스로 전달되는지 설명합니다.

## 읽기 순서

1. [concept.md](./concept.md) - Gateway 책임, 헥사고날 경계, 라우팅/보안 정책.
2. [process-flow.md](./process-flow.md) - 로그인부터 보호 API, 권한 변경, 장애까지의 데이터 흐름.
3. [local-run.md](./local-run.md) - IntelliJ, Gradle, Docker 실행과 확인/종료 방법.
4. [../README.md](../README.md) - 모듈 전체 요약과 설정표.

## 현재 실행 전제

- Spring Boot WebFlux + Spring Cloud Gateway 애플리케이션입니다.
- 기본 포트와 Config Server 설정 포트는 모두 `8000`입니다.
- 라우트는 `config-repo/gateway-service.yml`에서 관리합니다.
- Gateway 자체에는 H2/PostgreSQL 데이터베이스가 없습니다. 사용자·권한의 원장은 Auth가 소유합니다.
- `.run/Gateway standalone bootRun.run.xml`은 `local` 프로파일 기반의 외부 의존성 없는 기동 smoke용입니다. JWT 검증 키는 Run Configuration 환경변수로 별도 주입합니다.
- `.run/Gateway bootRun.run.xml`은 Config/Discovery/Auth와 함께 쓰는 전체 라우팅용입니다.
- 보호 API는 Auth의 token-version API를 호출합니다. 단독 smoke에서만 `AUTH_TOKEN_VERSION_VALIDATION_ENABLED=false`를 사용합니다.

## 핵심 코드 입구

| 코드 | 역할 |
|---|---|
| `GatewayApplication` | Spring Boot 시작점 |
| `JwtAuthenticationFilter` | 모든 `/api/**`의 전역 인증/내부 경로 정책 |
| `AccessTokenVerifier` | 토큰 검증 포트 |
| `JjwtAccessTokenVerifier` | JJWT 기술 어댑터 |
| `AuthenticatedPrincipal` | 검증 완료 내부 신원 값 객체 |
| `TokenVersionValidator` | 현재 권한 버전 확인 포트 |
| `AuthTokenVersionValidator` | WebClient/Caffeine Auth 어댑터 |
| `RequestIdFilter` | 요청 추적 번호 정규화 |
| `ResponseHeaderFilter` | 공통 보안/캐시 응답 헤더 |
| `RequestLoggingFilter` | 요청 경로·상태·지연 로그 |
| `FallbackController` | Master Data/Journal Ledger 장애 503 응답 |

## 문서와 코드가 달라졌을 때

1. `gateway/src/main/resources/application.yml`과 `config-repo/gateway-service.yml`을 먼저 확인합니다.
2. `JwtAuthenticationFilterTest`와 `GatewayRouteSecurityPolicyTest`로 공개/보호 경계를 확인합니다.
3. 문서의 포트, 환경변수, 실행 순서를 코드와 함께 수정합니다.
4. `:gateway:test :gateway:bootJar`와 standalone bootRun을 재검증합니다.
