# Gateway Module Concept

## 개요

`gateway`는 분리된 서비스들 앞단의 단일 진입점이다. 현재는 `account` 애플리케이션으로 요청을 전달하지만, 이후 도메인별 서비스가 나뉘면 라우트만 확장해 같은 진입점을 유지한다.

## 현재 라우팅 모델

- `gateway-service`가 Eureka에 등록된다.
- 백엔드 `app`은 `account`라는 서비스 ID로 Eureka에 등록된다.
- `config-repo/gateway-service.yml`에는 `auth-service`, `master-data`, `journal-ledger`, 구 모놀리식 `account` 라우트가 함께 정의되어 있다.
- 게이트웨이는 `lb://...` 라우트를 사용하므로 Eureka에서 각 서비스 ID를 찾는다.

## 기본 필터

- CORS 허용 규칙
- `X-Request-Id` 생성 및 전달
- 기본 응답 헤더 추가
- 요청 메서드, 경로, 상태, 지연 시간 로깅
- JWT 서명/issuer 검증 (`auth.jwt.*` 설정 기반)
- JWT 검증 후 `X-Auth-User`, `X-Auth-Roles`, `X-Auth-Role-Version`, `X-Auth-Department` 헤더 전달
- Auth의 `/api/auth/validate-token-version` API와 연동해 JWT의 `roleVersion`을 검증

## 다음 확장 포인트

- 도메인별 서비스 ID 라우트 분리
- roleVersion 검증 캐시 TTL/장애 정책의 운영값 튜닝
- rate limiting, circuit breaker, fallback 정책의 라우트별 세분화

## JWT Role Version 검증

역할 변경 승인 후 Auth는 사용자 `roleVersion`을 증가시킵니다. 기존 JWT에는 예전 `roleVersion`이 들어 있으므로, Gateway는 보호 라우트에서 Auth의 token-version 검증 API를 호출해 오래된 토큰을 거절합니다.

- 기본 설정: `AUTH_TOKEN_VERSION_VALIDATION_ENABLED=true`
- 로컬 Auth 주소: `AUTH_TOKEN_VERSION_VALIDATION_BASE_URL=http://localhost:8084`
- 캐시 TTL: `AUTH_TOKEN_VERSION_VALIDATION_CACHE_TTL_SECONDS=30`
- Auth 호출 timeout: `AUTH_TOKEN_VERSION_VALIDATION_TIMEOUT_MILLIS=500`

Auth 호출 실패는 보안 기준상 fail-closed로 처리합니다. Gateway만 단독 smoke 기동할 때는 요청 검증 테스트가 목적이 아니므로 `AUTH_TOKEN_VERSION_VALIDATION_ENABLED=false`로 끌 수 있습니다.

## 로컬 실행

상세 실행 순서는 [local-run.md](./local-run.md)를 참고한다.
