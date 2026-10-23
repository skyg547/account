# [frontend-bff] 멱등성 헤더를 제거해 내부감사 명령 재시도 보호를 우회한다

- **Issue ID:** INFRA_GOV_03
- **Priority:** P2
- **Module:** frontend-bff
- **Area / category:** infrastructure / bug
- **Labels:** module:frontend-bff, area:infrastructure, type:bug, priority:p2, agent-loop, status:draft, spec-driven

## Code reference
- frontend/src/server/auth/bff.ts:17
- frontend/src/server/auth/bff.ts:335
- frontend/src/server/auth/bff.ts:523
- internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/RcmController.java:43
- internal-audit/core/src/main/java/com/ho/account/internalaudit/core/application/service/IdempotentCommandExecutor.java:90
- frontend/tests/auth-bff-live.test.mjs:349

## Problem statement
BFF의 요청 헤더 allowlist에 `X-Idempotency-Key`가 없다. 브라우저가 내부감사 명령에 키를 보내도 BFF가 Gateway로 프록시할 때 제거된다. 내부감사 Controller는 이 헤더를 선택적으로 받고, 키가 없으면 receipt 잠금·fingerprint 비교·결과 재생 없이 명령을 매번 실행한다. BFF가 생성하는 Bearer 헤더 자체는 세션 쿠키의 JWT를 사용하므로 이 결함은 토큰 위조가 아니라 멱등성 계약의 전파 누락이다.

## Reproduction / evidence
`requestHeaders`는 `FORWARDED_REQUEST_HEADERS`의 여섯 일반 헤더만 복사한 뒤 Bearer 헤더를 만든다. `proxyAuthenticatedRequest`는 이 결과를 모든 보호 API에 사용한다. 따라서 `POST /api/v1/internalaudit/rcms/processes`에 `X-Idempotency-Key: K`를 보낸 같은 출처 클라이언트 요청은 업스트림에서 키가 없는 요청이 된다. `IdempotentCommandExecutor.execute`의 90~97행은 키 부재를 비멱등 분기로 처리한다. 같은 ID를 다시 보내면 영속성 제약으로 실패할 수 있으나 최초 응답 재생·명령 동등성 검사라는 계약은 이미 상실된다.

## Financial / architectural impact
네트워크 타임아웃 뒤 사용자가 같은 명령을 재시도하면 내부감사의 원자적 receipt 보호가 적용되지 않아 최초 결과를 안전하게 재생하지 못한다. 실패 응답, 중복 시도, 감사 이력 해석의 혼란이 생기며 ID 생성 방식에 따라 중복 업무 처리 위험도 있다. 실제 중복 저장이 발생했다고 단정하지 않는다.

## Proposed solution
정확한 3파일 allowlist: `frontend/src/server/auth/bff.ts`, `frontend/tests/auth-bff-live.test.mjs`, `frontend/docs/auth-session-guide.md`. BFF의 보호 API 프록시에서 길이·문자 형식을 검증한 `X-Idempotency-Key`를 한 값으로 전달한다. 브라우저가 보낸 `Authorization` 및 `X-Auth-*` 신원 헤더는 현재처럼 버리고, 키 전달 계약과 재시도 의미를 문서화한다.

## Acceptance criteria
- [ ] 같은 출처의 인증된 쓰기 요청에서 유효한 `X-Idempotency-Key`가 Gateway로 동일하게 전달된다.
- [ ] 중복·과대·부적합 키는 명시적으로 거절되며 브라우저의 `Authorization`·`X-Auth-*`는 여전히 전달되지 않는다.
- [ ] 같은 키·같은 내부감사 명령의 재시도는 최초 결과를 재생하고 다른 payload는 409가 되는 통합 검증을 통과한다.

## Test gap and verification limits
현재 `frontend/tests/auth-bff-live.test.mjs`는 세션 Bearer 교체와 위조 신원 헤더 제거를 검증하지만 멱등성 헤더 전파는 확인하지 않는다. `node --test frontend/tests/auth-bff-live.test.mjs`와 `./gradlew :internal-audit:api:test --offline --no-daemon --console=plain --max-workers=1`에 이어 BFF→Gateway→내부감사 통합 재시도 검증이 필요하다. 이번 감사에서는 서버를 띄우거나 실제 재시도를 실행하지 않았고 정적 코드 흐름으로 판단했다.
