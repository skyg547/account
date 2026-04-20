# API Gateway

`gateway` 모듈은 외부 요청의 단일 진입점을 제공하는 Spring Cloud Gateway 런타임이다.

## 목적

- 외부 클라이언트가 백엔드 서비스 주소를 직접 알지 않도록 한다.
- Eureka에 등록된 서비스 ID 기준으로 라우팅한다.
- 공통 CORS, 추적 헤더, 기본 응답 헤더를 한 곳에서 적용한다.

## 현재 범위

- `/api/**` -> `lb://account`
- `/actuator/health`, `/actuator/info` -> `lb://account`
- `X-Request-Id` 생성 및 전달
- 기본 CORS
- 기본 응답 헤더와 요청 로깅

## 실행 순서

1. `./gradlew :discovery:bootRun`
2. `./gradlew :app:bootRun`
3. `./gradlew :gateway:bootRun`

게이트웨이는 기본적으로 `http://localhost:8081`에서 기동한다.

## 참고 문서

- [docs/concept.md](./docs/concept.md)
