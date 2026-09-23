# Gateway 업무/데이터 흐름

## 1. 로그인 요청

```text
Client
  -> POST /api/auth/login
  -> RequestIdFilter: X-Request-Id 확인/발급
  -> JwtAuthenticationFilter: 외부 X-Auth-* 제거, 로그인 공개 경로 확인
  -> RequestRateLimiter: BFF HMAC 검증 후 불투명 사용자별 + BFF peer aggregate bucket 선택
  -> auth-login-api route
  -> Auth: 계정/비밀번호/잠금/역할 확인 후 JWT 발급
  -> Client
```

로그인은 JWT를 발급받기 위한 시작점이므로 기존 JWT를 요구하지 않습니다. 공개 경로라도
클라이언트가 넣은 `X-Auth-User` 같은 내부 신원 헤더는 Auth에 전달하지 않습니다. BFF가 보낸
로그인 사용자 SHA-256 키는 30초 HMAC이 맞을 때만 rate-limit bucket으로 사용하며 Auth에는
전달되지 않습니다. 사용자별 bucket 외에 더 큰 BFF peer aggregate bucket도 함께 소모하므로
사용자명을 계속 바꿔 quota를 무한히 새로 만들 수 없습니다. in-memory 저장소는 최대 10,000개
bucket으로 제한됩니다. 위조·만료·잘못된 경로는 직접 연결 peer bucket으로 안전하게 대체합니다.

## 2. 보호 업무 API 요청

예: `GET /api/basic/account-subjects` 또는 `GET /api/master-data/change-requests/pending`

```text
1. RequestIdFilter
   - 안전한 X-Request-Id면 보존
   - 누락/129자 이상/허용하지 않은 문자면 UUID 재발급

2. JwtAuthenticationFilter
   - 클라이언트 X-Auth-* 모두 삭제
   - Authorization 헤더가 정확히 하나인지 확인
   - Bearer token 추출

3. JjwtAccessTokenVerifier
   - HS256 서명과 issuer 확인
   - iat/exp 필수 및 시간 순서 확인
   - subject, roles, roleVersion 필수 타입 확인
   - 헤더에 안전한 코드인지 확인

4. AuthTokenVersionValidator
   - (username, roleVersion) positive cache 조회
   - miss이면 Auth /api/auth/validate-token-version 호출
   - VALID/REJECTED/UNAVAILABLE로 변환

5. JwtAuthenticationFilter
   - VALID: 검증된 X-Auth-* 생성
   - REJECTED: 401
   - UNAVAILABLE: 503

6. Spring Cloud Gateway route
   - RequestRateLimiter는 JWT 필터가 기록한 내부 principal attribute로 사용자별 bucket 선택
   - 설정에 명시된 route allowlist와 일치하는 외부 경로만 선택
   - 관리자 사용자(`/api/admin/**`)는 Auth, FX(`/api/fx/**`)는 Journal Ledger, 채권 조회(`/api/receivable/**`)는 Receivable, 지점간 금융(`/api/finance/banking/**`)은 Reconciliation route를 선택
   - 그 밖의 기준정보, 전표/원장, 결산, 예산, 내부감사, 입금, 채권, 채무, 세무, 대사, 자산/리스, 보고서, 지출결의, 마트, ECL 경로도 분리된 서비스의 전용 route를 선택
   - Eureka에서 선택된 route의 대상 인스턴스(`lb://...`) 확인
   - Discovery locator는 비활성화되어 `/{serviceId}/**` 형태의 자동 경로는 생성되지 않음
   - 요청 전달
```

뒤쪽 서비스는 Gateway가 만든 헤더를 인증 컨텍스트로 사용할 수 있지만, 서비스가 Gateway를 우회해 직접 노출되지 않도록 네트워크 정책을 함께 적용해야 합니다.

## 3. 역할 변경 뒤 기존 JWT

```text
Governance 승인
  -> Auth 역할 교체
  -> Auth roleVersion 증가 (예: 7 -> 8)
  -> 기존 JWT 요청(roleVersion=7)
  -> Gateway가 Auth 현재 버전 확인
  -> REJECTED
  -> 401 TOKEN_ROLE_VERSION_REJECTED
  -> 사용자는 다시 로그인해 roleVersion=8 JWT 발급
```

정상 결과 캐시 TTL 안에는 이전 `VALID` 결과가 남을 수 있습니다. 현재 기본 TTL은 30초이며, 운영에서는 역할 변경 이벤트 기반 즉시 무효화가 필요합니다.

## 4. Auth 장애

```text
Gateway -> Auth token-version API
             timeout / 5xx / 빈 body / 필수 필드 누락
Gateway <- UNAVAILABLE
Client  <- 503 AUTH_VALIDATION_UNAVAILABLE
```

Auth 장애를 401로 반환하면 사용자가 반복 로그인해도 해결되지 않고 부하만 늘어납니다. 503은 호출자가 잠시 후 재시도하고 운영자가 Auth 상태를 확인해야 한다는 뜻입니다. 경로 전달은 계속하지 않으므로 보안은 fail-closed입니다.

## 5. 내부 Auth 경로 차단

```text
외부 Client -> /api/auth/internal/** 또는 /api/auth/validate-token-version
Gateway     -> 라우트 선택 전에 404 INTERNAL_AUTH_ROUTE_NOT_EXPOSED
```

Config 파일이 실수로 `/api/auth/**`를 다시 공개해도 전역 필터가 한 번 더 차단합니다. 그러나 Auth 서비스의 8084 포트를 외부에 직접 공개하면 이 보호를 우회할 수 있으므로 운영 방화벽, private network, mTLS/서비스 자격 증명이 필요합니다.

## 6. 뒤쪽 서비스 장애와 fallback

Master Data나 Journal Ledger 호출이 timeout 또는 circuit open 상태가 되면 `FallbackController`가 503과 사용자 안내 메시지를 반환합니다. 이 흐름은 인증 성공 뒤에 발생하므로 인증 장애의 503과 다음 값으로 구분할 수 있습니다.

채권 조회, FX 대시보드, 지점간 금융 API는 각각 Receivable, Journal Ledger, Reconciliation의 기존 circuit breaker와 fallback을 공유합니다. 관리자 사용자 조회 route는 Auth/Admin 전용 fallback 구현이 없으므로 도달할 수 없는 fallback URI를 선언하지 않으며, Auth 연결 실패는 Gateway의 일반 upstream 오류 정책을 따릅니다.

- 인증 인프라 장애: `X-Auth-Error=AUTH_VALIDATION_UNAVAILABLE`
- 업무 서비스 circuit breaker: fallback JSON의 `status=503`

## 7. 종료와 재시작

Gateway는 자체 DB 상태를 갖지 않으므로 프로세스를 재시작해도 업무 데이터가 유실되지 않습니다. 다만 메모리 Caffeine cache는 비워져 재시작 직후 Auth 확인 호출이 일시적으로 늘어납니다.
