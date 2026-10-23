# [internal-audit] 직접 호출에서 위조한 인증 헤더만으로 감사 명령을 실행한다

- **Issue ID:** INFRA_GOV_02
- **Priority:** P1
- **Module:** internal-audit
- **Area / category:** api / security
- **Labels:** module:internal-audit, area:api, type:security, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/RcmController.java:39
- internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/RcmController.java:134
- internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/EvaluationController.java:128
- internal-audit/docker-compose.yml:15
- internal-audit/api/src/test/java/com/ho/account/internalaudit/api/adapter/in/web/RcmControllerTest.java:78
- gateway/src/main/java/com/ho/account/gateway/filter/JwtAuthenticationFilter.java:113

## Problem statement
RCM·평가 Controller는 `X-Auth-User`와 `X-Auth-Roles` 문자열의 존재와 역할 이름만 검사한다. 요청이 Gateway를 통과했는지, JWT 서명·roleVersion 검증을 받았는지, 해당 기능의 권한 매트릭스에 허용돼 있는지는 검사하지 않는다. 모듈 Compose는 API의 8083 포트를 호스트에 게시하므로 이 경로에서는 Gateway가 수행하는 헤더 삭제·재생성을 건너뛸 수 있다.

## Reproduction / evidence
`RcmControllerTest`는 Bearer 토큰이나 인증 필터 없이 `X-Auth-User: trusted_auditor`, `X-Auth-Roles: ROLE_AUDITOR`만 넣은 생성 요청이 200으로 처리되고 소유자가 헤더 값으로 저장되는 것을 증명한다. 실제 Controller도 동일한 `requireAuthorizedAuditor` 결과를 `AuditActorContext`에 기록한 뒤 명령을 호출한다. Gateway의 헤더 정화는 Gateway를 지난 요청에만 적용된다. 내부감사 코드 경로에는 기능 코드·접근 유형별 `hasPermission` 호출이 없다.

## Financial / architectural impact
8083 포트에 도달할 수 있는 호출자는 감사인·관리자 역할과 감사 actor를 임의로 주장해 RCM, 설계/운영 평가, 결함 기록을 생성하거나 조회할 수 있다. 저장되는 감사로그 actor도 위조된 헤더에서 파생되므로 내부통제 증거의 신원 라인리지가 훼손된다. 단, 외부 네트워크에서 8083이 실제로 접근 가능한지는 배포 방화벽에 따라 달라진다.

## Proposed solution
정확한 5파일 allowlist: `internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/RcmController.java`, `internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/EvaluationController.java`, `internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/InternalAuditIdentityFilter.java`(신규), `internal-audit/api/build.gradle`, `internal-audit/api/src/test/java/com/ho/account/internalaudit/api/adapter/in/web/InternalAuditIdentityIntegrationTest.java`(신규). API 경계에서 검증된 토큰 또는 인증된 서비스 주체를 만들고 Controller는 그 주체에서 actor와 역할을 얻도록 한다. 역할 이름만으로 끝내지 말고 해당 명령/조회 기능의 권한 정책을 명시적으로 집행한다. 직접 헤더만 보낸 요청은 fail-closed 한다.

## Acceptance criteria
- [ ] 직접 8083 호출에서 임의 `X-Auth-*` 헤더만 보낸 생성·조회 요청은 업무 포트를 호출하지 않고 401/403으로 종료된다.
- [ ] 검증된 JWT/서비스 신원만 actor로 감사로그에 기록되고, 허용 역할이라도 해당 기능 권한이 없으면 거절된다.
- [ ] Gateway 경유 정상 감사인 요청과 무효 토큰·회수된 권한·헤더 위조를 각 Controller 테스트로 검증한다.

## Test gap and verification limits
기존 Controller 테스트는 헤더 값만으로 성공하는 동작을 정상 사례로 고정하며, 직접 포트에서 위조 헤더와 무효 토큰을 함께 보내는 보안 회귀가 없다. `./gradlew :internal-audit:api:test :internal-audit:core:test --offline --no-daemon --console=plain --max-workers=1` 및 직접 포트·Gateway 경유 HTTP 검증이 필요하다. 이번 정적 감사는 실제 네트워크 노출 범위나 운영 방화벽을 검증하지 않았다.
