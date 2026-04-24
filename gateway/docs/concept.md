# Gateway Module Concept

## 개요

`gateway`는 분리된 서비스들 앞단의 단일 진입점이다. 현재는 `account` 애플리케이션으로 요청을 전달하지만, 이후 도메인별 서비스가 나뉘면 라우트만 확장해 같은 진입점을 유지한다.

## 현재 라우팅 모델

- `gateway-service`가 Eureka에 등록된다.
- 백엔드 `app`은 `account`라는 서비스 ID로 Eureka에 등록된다.
- 게이트웨이는 `lb://account` 라우트를 통해 `/api/**`와 일부 Actuator 엔드포인트를 프록시한다.

## 기본 필터

- CORS 허용 규칙
- `X-Request-Id` 생성 및 전달
- 기본 응답 헤더 추가
- 요청 메서드, 경로, 상태, 지연 시간 로깅
- JWT 서명/issuer 검증 (`auth.jwt.*` 설정 기반)

## 다음 확장 포인트

- 도메인별 서비스 ID 라우트 분리
- 인증/인가 필터 추가
- rate limiting, circuit breaker, fallback 추가
