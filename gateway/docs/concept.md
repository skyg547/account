# Gateway Module Concept

## 1. 모듈 목적

`gateway`는 분리된 서비스 앞단의 단일 진입점입니다. 외부 계약과 내부 서비스 위치를 분리하고, 인증·추적·장애 대응처럼 모든 API에 공통인 기술 흐름을 한 곳에서 적용합니다.

Gateway가 하지 않는 일도 중요합니다.

- 분개 가능 여부, 금액 계산, 마감 상태 전이 같은 도메인 규칙을 판단하지 않습니다.
- 사용자 역할을 생성하거나 변경하지 않습니다.
- 사용자·권한 데이터를 자체 DB에 복제해 원장처럼 관리하지 않습니다.

이 업무는 Auth나 각 업무 모듈의 `core/domain`이 소유합니다. Gateway는 검증 결과를 전달하는 오케스트레이터입니다.

## 2. 현재 라우팅 모델

- `gateway-service`는 포트 `8000`으로 실행되고 Eureka에 등록됩니다.
- `config-repo/gateway-service.yml`이 `lb://auth-service`, `lb://master-data`, `lb://journal-ledger`, `lb://closing-service`, `lb://budget-api`, 레거시 `lb://account` 라우트를 정의합니다.
- `/api/basic/**`와 `/api/master-data/**`는 레거시 `/api/**` catch-all보다 먼저 명시적인 `master-data-api` 라우트와 동일한 circuit breaker를 사용합니다.
- `/api/budgets/**`는 전용 `budget-api` route와 circuit breaker를 거쳐 Budget 서비스로 전달됩니다.
- Auth 외부 공개 라우트는 `/api/auth/login`의 POST 하나입니다.
- `/api/auth/validate-token-version`과 `/api/auth/internal/**`는 서비스 간 통신용이므로 전역 필터가 외부 요청을 차단합니다.
- `/api/**`는 라우트 목록과 무관하게 전역 JWT 필터가 기본 보호합니다.
- OpenAPI 문서 경로와 actuator처럼 `/api` 밖의 경로는 JWT 필터 대상이 아닙니다. 운영에서는 별도 네트워크/관리 포트 정책으로 제한해야 합니다.

레거시 `account-api` catch-all은 이전 기간 호환을 위한 임시 라우트입니다. 전역 필터 덕분에 인증 우회는 막지만, 도메인별 서비스 이전이 끝나면 제거해야 합니다.

## 3. 헥사고날 구조

```text
HTTP request
  -> JwtAuthenticationFilter (inbound adapter/orchestrator)
       -> AccessTokenVerifier (port)
            -> JjwtAccessTokenVerifier (JWT technology adapter)
       -> TokenVersionValidator (port)
            -> AuthTokenVersionValidator (WebClient + Caffeine adapter)
  -> verified headers
  -> route target service
```

### 객체별 책임

- `AuthenticatedPrincipal`은 사용자, 역할, 권한 버전, 부서 코드를 불변 값으로 묶고 HTTP 헤더에 안전한 값인지 검증합니다.
- `JjwtAccessTokenVerifier`는 서명, issuer, 발급/만료 시각, 필수 claim 타입을 확인합니다.
- `AuthTokenVersionValidator`는 Auth 응답을 `VALID`, `REJECTED`, `UNAVAILABLE`로 변환합니다.
- `JwtAuthenticationFilter`는 HTTP 상태와 헤더를 선택하고 체인을 이어갈지만 결정합니다.

이 분리는 필터가 JJWT JSON 구조나 WebClient 응답 기술에 직접 결합되지 않게 합니다.

## 4. 인증과 신뢰 헤더 정책

클라이언트는 다음 헤더를 신뢰 값으로 직접 지정할 수 없습니다.

- `X-Auth-User`
- `X-Auth-Roles`
- `X-Auth-Role-Version`
- `X-Auth-Department`

Gateway는 모든 요청에서 위 헤더를 먼저 제거합니다. 보호 API의 JWT와 roleVersion이 모두 검증된 경우에만 `AuthenticatedPrincipal`에서 새로 생성합니다. 토큰에 부서가 없으면 기존 부서 헤더도 남기지 않습니다.

역할은 쉼표로 연결해 전달하므로 사용자/역할/부서 코드는 제어 문자, 공백, 쉼표가 없는 ASCII 코드여야 합니다. 이는 다운스트림 헤더 파싱이 사용자 입력에 의해 달라지지 않게 하는 경계 규칙입니다.

## 5. roleVersion 결과 모델

역할 변경 승인 후 Auth는 `roleVersion`을 올립니다. 기존 JWT에는 이전 버전이 있으므로 Gateway가 Auth에 현재 스냅샷을 확인합니다.

| 결과 | 의미 | Gateway 응답 |
|---|---|---|
| `VALID` | 현재 계정/역할과 JWT 버전 일치 | 라우팅 계속 |
| `REJECTED` | 역할 변경, 계정 잠금/비활성 등 | 401 |
| `UNAVAILABLE` | Auth timeout, HTTP 장애, 빈/구조 불완전 응답 | 503 |

기존의 단순 boolean은 권한 변경과 시스템 장애를 모두 `false`로 만들었습니다. 현재 모델은 사용자가 재로그인해야 할지, 운영자가 Auth를 복구해야 할지 구분합니다.

## 6. 캐시와 재실행 정합성

- `VALID`만 Caffeine에 캐시합니다.
- 기본 TTL은 30초, 최대 항목은 10,000개입니다.
- `REJECTED`와 `UNAVAILABLE`은 캐시하지 않습니다.
- username은 Auth가 JWT에 발급한 canonical 값을 그대로 사용하며 임의 소문자 변환을 하지 않습니다.

TTL 동안 역할 회수 반영이 늦어질 수 있습니다. 운영 다중 노드에서는 Auth 역할 변경 이벤트를 구독해 해당 사용자 cache key를 즉시 무효화하는 방식이 다음 고도화 대상입니다.

## 7. WebFlux/함수형 흐름

Gateway 이벤트 루프에서 네트워크 호출을 `block()`하면 적은 수의 스레드가 모두 멈출 수 있습니다. 따라서 토큰 버전 검증은 `Mono<TokenVersionValidationResult>`로 이어집니다.

- 정상 값은 `flatMap`으로 다음 필터 체인에 전달합니다.
- 빈 publisher는 `UNAVAILABLE`로 바꿔 조용히 200으로 끝나는 상황을 막습니다.
- 예외도 `UNAVAILABLE`로 바꿔 fail-closed 처리합니다.

## 8. 다음 확장 포인트

- HS256 공유 비밀을 JWKS 기반 비대칭 서명과 키 회전으로 전환.
- Auth 내부 API에 mTLS/서비스 자격 증명 적용.
- 역할 변경 이벤트 기반 다중 노드 cache 즉시 무효화.
- rate limiting과 circuit breaker를 업무 중요도별로 세분화.
- 레거시 `account-api` catch-all 제거.
