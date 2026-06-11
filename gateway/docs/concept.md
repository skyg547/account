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

## 다음 확장 포인트

- 도메인별 서비스 ID 라우트 분리
- 역할 변경 직후 기존 JWT 차단을 위한 Auth token-version 검증 또는 캐시 연동
- rate limiting, circuit breaker, fallback 정책의 라우트별 세분화

## 로컬 실행

상세 실행 순서는 [local-run.md](./local-run.md)를 참고한다.
