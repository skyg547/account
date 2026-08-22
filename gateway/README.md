# 🚪 API Gateway (Spring Cloud Gateway)

`gateway` 모듈은 외부 클라이언트가 여러 회계/자산운용 서비스에 접근할 때 사용하는 단일 진입점입니다.
호텔의 1층 안내데스크처럼 요청을 확인하고, 인증된 신원과 추적 번호를 붙인 뒤 올바른 업무 서비스로 전달합니다.
기본 포트는 **8000**입니다.

---

## 1. 🐣 초보자를 위한 개념 설명

### 프론트엔드가 각 모듈을 직접 호출하면 안 되나요?

백엔드 모듈이 여러 개로 나뉘면 프론트엔드는 각 서비스의 IP, 포트, 장애 상태를 모두 알아야 합니다.
Gateway 하나만 외부에 공개하면 클라이언트는 `http://localhost:8000`만 호출하고, Gateway가 URL과 Eureka 서비스 ID를 보고 뒤쪽 서비스를 선택합니다.

Gateway의 책임은 다음과 같습니다.

- **주소 은닉과 라우팅**: `/api/basic/**`, `/api/master-data/**`, `/api/journals/**` 같은 경로를 해당 서비스로 전달합니다.
- **명시 경로만 공개**: 설정에 선언한 route allowlist만 외부에 노출하며 `/{serviceId}/**` 형태의 자동 경로는 만들지 않습니다. 라우팅에서는 Eureka로 명시적인 `lb://` route의 대상 인스턴스를 선택하고, Gateway 자체의 Eureka 등록과 registry fetch도 유지합니다.
- **JWT 1차 검문**: 서명, issuer, 발급/만료 시각, 사용자, 역할, `roleVersion`을 검사합니다.
- **권한 스냅샷 확인**: Auth의 현재 `roleVersion`과 JWT 값을 비교해 역할 변경 전 토큰을 거절합니다.
- **신뢰 헤더 재생성**: 클라이언트가 보낸 `X-Auth-*`는 삭제하고 검증된 JWT 값으로 다시 만듭니다.
- **Master Data 승인 권한**: 변경요청 API는 재생성된 사용자/역할 헤더를 사용해 관리 역할과 maker-checker actor를 검증합니다.
- **추적 번호 관리**: 안전한 `X-Request-Id`를 요청과 응답에 전달합니다.
- **서킷 브레이커**: 뒤쪽 서비스 장애가 전체 장애로 번지지 않게 503 fallback을 반환합니다.

Gateway는 회계 계산이나 승인 상태 변경을 수행하지 않습니다. 금액·전표·마감 같은 업무 규칙은 각 도메인 `core`에 남고, Gateway는 입구의 기술/보안 흐름만 조정합니다.

---

## 2. 🔐 인증 업무 흐름

모든 `/api/**`는 기본적으로 인증 대상이며, 현재 외부 공개 예외는 `POST /api/auth/login`과 CORS 사전 요청(`OPTIONS`)뿐입니다.

```text
외부 요청
  -> X-Auth-* 신뢰 헤더 삭제
  -> 공개/내부 경로 정책 확인
  -> Bearer JWT 검증
  -> Auth roleVersion 비동기 확인
  -> 검증된 X-Auth-* 헤더 생성
  -> Eureka로 찾은 업무 서비스에 전달
```

외부에서 직접 호출할 수 없는 경로:

- `/api/auth/validate-token-version`
- `/api/auth/internal/**`

이 경로는 Gateway에서 `404 INTERNAL_AUTH_ROUTE_NOT_EXPOSED`로 차단합니다. 다만 서비스 포트를 직접 공개하는 환경에서는 Gateway만으로 내부 API를 보호할 수 없으므로 네트워크 격리와 서비스 간 인증도 필요합니다.

### 응답 상태와 오류 코드

| 상황 | HTTP | `X-Auth-Error` |
|---|---:|---|
| Bearer 토큰 없음/형식 오류 | 401 | `BEARER_TOKEN_REQUIRED` |
| 서명·필수 claim·만료 오류 | 401 | `ACCESS_TOKEN_INVALID` |
| 역할 변경/계정 상태로 Auth가 거절 | 401 | `TOKEN_ROLE_VERSION_REJECTED` |
| Auth timeout·장애·빈/구조 불완전 응답 | 503 | `AUTH_VALIDATION_UNAVAILABLE` |
| 내부 Auth 경로 외부 호출 | 404 | `INTERNAL_AUTH_ROUTE_NOT_EXPOSED` |

401은 다시 로그인해야 하는 인증 결과이고, 503은 Auth 복구 후 재시도해야 하는 운영 장애입니다.

---

## 3. 🧱 헥사고날 경계

- `JwtAuthenticationFilter`: HTTP/WebFlux 인바운드 어댑터이자 전역 인증 흐름 조정자.
- `AccessTokenVerifier`: JWT 라이브러리를 필터에서 분리한 검증 포트.
- `JjwtAccessTokenVerifier`: JJWT 기반 서명/claim 검증 어댑터.
- `AuthenticatedPrincipal`: 뒤쪽 서비스로 전달 가능한 검증 완료 신원 값 객체.
- `TokenVersionValidator`: 현재 권한 스냅샷 확인 포트.
- `AuthTokenVersionValidator`: WebClient/Caffeine 기반 Auth 연동 어댑터.
- `RequestIdFilter`, `ResponseHeaderFilter`, `RequestLoggingFilter`: 공통 기술 필터.

Reactor 흐름에서는 `block()`을 사용하지 않습니다. Auth 검증도 `Mono`로 이어서 Gateway 이벤트 루프를 막지 않습니다.

---

## 4. ⚙️ JWT와 roleVersion 설정

Auth와 Gateway는 같은 서명키와 issuer를 사용해야 합니다.

```yaml
auth:
  jwt:
    secret: ${AUTH_JWT_SECRET:${JWT_SECRET:}}
    public-key: ${AUTH_JWT_PUBLIC_KEY:${JWT_PUBLIC_KEY:}}
    jwks-uri: ${AUTH_JWT_JWKS_URI:${JWT_JWKS_URI:}}
    issuer: ${AUTH_JWT_ISSUER:auth-service}
    allowed-clock-skew-seconds: ${AUTH_JWT_ALLOWED_CLOCK_SKEW_SECONDS:30}
  token-version-validation:
    enabled: ${AUTH_TOKEN_VERSION_VALIDATION_ENABLED:true}
    base-url: ${AUTH_TOKEN_VERSION_VALIDATION_BASE_URL:lb://auth-service}
    cache-ttl-seconds: ${AUTH_TOKEN_VERSION_VALIDATION_CACHE_TTL_SECONDS:30}
    timeout-millis: ${AUTH_TOKEN_VERSION_VALIDATION_TIMEOUT_MILLIS:500}
    maximum-cache-size: ${AUTH_TOKEN_VERSION_VALIDATION_MAXIMUM_CACHE_SIZE:10000}
```

소스코드 내 평문 JWT Secret 기본값은 보안 강화를 위해 완전히 제거되었습니다. 운영 및 개발 환경에서는 `AUTH_JWT_SECRET` (또는 `JWT_SECRET`) 및 RS256 비대칭키 공개키(`AUTH_JWT_PUBLIC_KEY`)를 Secret Manager/Vault/환경변수로 주입해야 합니다.

정상 검증 결과만 짧게 캐시합니다. 거절·장애 결과는 캐시하지 않으므로 계정 복구나 Auth 복구가 불필요하게 지연되지 않습니다.

---

## 5. ▶️ 실행과 빌드

상세 절차는 [docs/local-run.md](./docs/local-run.md)를 따릅니다.

### IntelliJ standalone smoke

1. 루트 프로젝트를 Gradle 프로젝트로 엽니다.
2. Gradle JVM을 JDK 17로 설정합니다.
3. 현재 실행 세션에서 생성한 32바이트 이상 `AUTH_JWT_SECRET`을 Run Configuration 환경변수로 연결합니다.
4. Run Configuration에서 `Gateway standalone bootRun`을 실행합니다.
5. `http://localhost:8000/actuator/health`를 확인합니다.

`local` 프로파일은 Config/Discovery/Eureka/Auth token-version 원격 검증을 끄므로 **기동 확인 전용**이며 실제 라우팅 검증은 하지 않습니다. JWT 검증 키는 프로파일 파일에 저장하지 않고 실행자가 매번 임시 주입합니다.

### 전체 라우팅 실행 순서

1. `Config Server bootRun`
2. `Discovery bootRun`
3. `Auth` 및 필요한 업무 API 서비스
4. `Gateway bootRun`

```powershell
.\gradlew :gateway:bootRun --console=plain
```

### 빌드와 테스트

```powershell
.\gradlew :gateway:test :gateway:bootJar --console=plain --max-workers=1 --no-daemon
```

### Docker

루트에서 실행합니다.

```powershell
docker compose up --build gateway
```

루트 Compose는 서비스 디스커버리와 Spring Cloud LoadBalancer 기반의 `lb://auth-service`를 통해 동적으로 Auth 서비스에 접근합니다.

---

## 6. 📚 문서 읽기 순서

1. [docs/README.md](./docs/README.md)
2. [docs/concept.md](./docs/concept.md)
3. [docs/process-flow.md](./docs/process-flow.md)
4. [docs/local-run.md](./docs/local-run.md)

## 7. 남은 고도화 항목

코드의 `@todo`는 다음 운영 경계를 의도적으로 기록합니다.

- 공유 HS256 키를 JWKS 기반 비대칭 키와 키 회전 계약으로 전환.
- 다중 Gateway 노드의 roleVersion positive cache를 Auth 역할 변경 이벤트로 즉시 무효화.
- Auth 내부 검증 API에 mTLS 또는 서비스 자격 증명 적용.
- 구 모놀리식 `account-api` catch-all 라우트를 도메인별 이전 완료 후 제거.
