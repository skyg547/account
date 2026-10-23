# [gateway] `/api/audit/**` 거버넌스 요청을 전달할 라우트와 실행 호스트가 없다

- **Issue ID:** INFRA_GOV_01
- **Priority:** P1
- **Module:** gateway
- **Area / category:** infrastructure / bug
- **Labels:** module:gateway, area:infrastructure, type:bug, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- gateway/src/main/resources/application.yml:109
- config-repo/gateway-service.yml:105
- gateway/src/test/java/com/ho/account/gateway/GatewayRouteSecurityPolicyTest.java:148
- shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:25
- internal-audit/api/src/main/java/com/ho/account/internalaudit/api/InternalAuditApiApplication.java:19
- frontend/src/services/adminService.ts:70

## Problem statement
프런트엔드는 `POST /api/audit/approvals/requests`를 호출하고 공유 커널에는 `/api/audit` Controller가 정의되어 있다. 하지만 Gateway의 패키지 설정과 Config Server 설정에는 `/api/v1/internalaudit/**` 라우트만 있으며 `/api/audit/**` 라우트가 없다. 두 경로는 서로 다른 API다. 더구나 내부감사 실행 모듈의 명시적 component scan에는 공유 커널 Controller가 포함되지 않는다. 따라서 기존 내부감사 라우트에 `/api/audit/**` 패턴만 추가해도 해당 Controller에 도달한다는 근거가 없다.

## Reproduction / evidence
`adminService`의 요청은 BFF의 catch-all 프록시를 거쳐 동일한 `/api/audit/approvals/requests` 경로로 Gateway에 도착한다. Gateway의 명시 라우트 목록과 외부 설정에는 이 경로에 맞는 predicate가 없고 discovery locator도 꺼져 있다. 기존 정책 테스트는 `/api/v1/internalaudit/**`만 정확히 일치한다고 단언한다. `AuthApplication`의 기본 스캔 범위 역시 `com.ho.account.auth`로, 공유 커널 `AuditController`를 실행 호스트로 삼는다는 증거가 없다. 현재 코드 기준으로 Gateway 진입 요청은 업무 Controller에 전달되지 않는다.

## Financial / architectural impact
역할·권한 매트릭스 조회, 승인 요청, 감사로그 조회 등 `/api/audit/**` 거버넌스 기능이 Gateway 경유로 실행되지 않는다. 프런트엔드의 승인 요청은 실제 승인 기록 없이 실패하거나 자체 fallback으로 흘러가므로 통제 운영 상태를 신뢰하기 어렵다.

## Proposed solution
정확한 5파일 이하 allowlist: `gateway/src/main/resources/application.yml`, `config-repo/gateway-service.yml`, `gateway/src/test/java/com/ho/account/gateway/GatewayRouteSecurityPolicyTest.java`, `auth/api/src/main/java/com/ho/account/auth/AuthApplication.java`, `auth/api/build.gradle`. 먼저 공유 커널 거버넌스 Controller와 필요한 서비스·영속성 빈을 제공할 소유 런타임을 확정하고 그 호스팅을 명시적으로 구성한다. 그런 다음 두 Gateway 설정에 `/api/audit/**`를 해당 런타임으로 보내는 전용 라우트를 동일하게 추가한다. 내부감사 `/api/v1/internalaudit/**` 라우트와 혼합하지 않는다.

## Acceptance criteria
- [ ] 패키지와 Config Server 설정의 `/api/audit/**` 라우트가 동일하고, 실제로 거버넌스 Controller를 호스팅하는 서비스로 전달된다.
- [ ] 인증된 `POST /api/audit/approvals/requests`와 권한 조회가 Gateway 경유로 해당 Controller에 도달하며, 미인증 요청은 차단된다.
- [ ] 정책 테스트가 두 설정의 경로·대상 일치와 `/api/v1/internalaudit/**` 경로의 보존을 검증한다.

## Test gap and verification limits
기존 `GatewayRouteSecurityPolicyTest`는 내부감사 경로만 검사한다. `./gradlew :gateway:test :auth:api:test --offline --no-daemon --console=plain --max-workers=1`와 BFF→Gateway→확정된 소유 서비스의 인증·비인증 HTTP smoke가 필요하다. 이번 감사는 설정과 코드 흐름의 정적 확인이며 실제 기동 결과는 주장하지 않는다. 소유 런타임의 빈·DB 스키마 연결이 확인되기 전에는 라우트 대상만 임의 지정해서는 수용 조건을 만족하지 않는다.
