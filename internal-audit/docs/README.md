# Governance Module Docs

`governance` 모듈은 내부회계 통제를 위한 감사로그와 승인 워크플로우를 담당합니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 감사, 승인, 권한 체크 흐름을 설명합니다.
- [schema.md](./schema.md): audit/security 영역의 주요 엔티티와 관계를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 거버넌스 모듈의 목적을 이해할 수 있도록 쉽게 설명합니다.
- [local-run.md](./local-run.md): IntelliJ와 Gradle에서 H2/PostgreSQL로 governance를 실행하고 검증하는 방법입니다.

## 이 모듈이 하는 일

1. 서비스 호출 결과를 감사로그로 남깁니다.
2. 마스터 데이터, 역할, 권한 변경 요청을 승인 대상으로 관리합니다.
3. SOD 관점의 통제 정책을 적용합니다.
4. 마스킹 규칙으로 민감정보 표시를 제한합니다.

## 핵심 진입점

- `GovernanceApplication`: 실제 audit 기능과 승인 반영에 필요한 master-data Bean을 등록하는 Spring Boot 진입점.
- `AuditController`: 감사, 역할, 권한, 승인 HTTP 인바운드 어댑터.
- `AuditService`: 감사 기록과 역할/권한 요청 유즈케이스.
- `MasterApprovalService`: 승인 상태 전이, SOD 검사, 승인 후 ApplyPort 호출.
- `SystemRoleApprovalApplyAdapter`: 승인된 역할/권한 변경을 실제 Governance 저장소에 반영.
- `TracingService`, `DataMaskingService`: 추적 조회와 마스킹 기능.

## Hexagonal DDD 적용 현황

- Inbound:
  - `AuditController` (HTTP, request/response DTO)
  - `AuditAspect` (`@AuditLoggable` AOP)
- Application:
  - `application.port.in`: `AuditLogUseCase`, `AuthorizationUseCase`, `MasterApprovalUseCase`
  - `service`: `AuditService`, `MasterApprovalService`
  - `application.port.out`: persistence/actor/master-data/Auth 연계 포트
- Domain:
  - `AuditLog`, `MasterApproval`, `SystemRole`, `Authorization`, `AuthorizationPolicy`
- Infrastructure:
  - `infrastructure.persistence`: JPA adapter
  - `infrastructure.security`: 요청 컨텍스트 기반 사용자/IP 해석과 역할/권한 승인 반영
  - `infrastructure.masterdata`: 승인 변경을 `master-data` 유스케이스로 반영
  - `infrastructure.auth`: 승인된 Auth 사용자 역할을 내부 API로 반영

`GovernanceApplication`의 패키지는 `com.ho.account.governance`이고 실제 기능은 `com.ho.account.audit` 아래에 있습니다. 실행 클래스가 명시적으로 스캔 범위를 선언하며, 컨텍스트 테스트가 Controller와 핵심 유스케이스 Bean 등록을 검증합니다.

## 내부회계 통제 포인트

- 감사로그 사용자 식별 우선순위: `X-User-ID` -> `request.getRemoteUser()` -> `request.getUserPrincipal()` -> `SYSTEM`
- SOD: 요청자와 승인자가 동일하면 승인할 수 없음
- 승인 대상 미지원: ApplyPort 또는 requestType이 지원되지 않으면 fail-closed 예외
- 권한 부여: role + function + accessType 조합 중복과 정책 충돌 차단
- 권한 회수: DELETE API에서 즉시 삭제하지 않고 승인 요청을 저장한 후 승인 시 실제 삭제
- master-data 반영: `approvalId`를 `sourceReference`로 전달해 재시도를 멱등 처리하고, 시행일이 오늘 이전이면 즉시 적용하며 미래면 Master Data `APPROVED` 상태로 대기
- Auth 역할 반영: `AUTH_USER_ROLE` 승인 후 Auth 내부 API를 호출해 사용자 역할과 `roleVersion` 갱신
- Auth 내부 인증: `GOVERNANCE_AUTH_INTERNAL_TOKEN`과 Auth의 `AUTH_INTERNAL_API_TOKEN`이 일치해야 함
- 추적성 조회: `TracingService`는 Repository가 아니라 `AuditLogPersistencePort`를 사용

## Master Approval API

- `GET /api/audit/approvals/pending`
- `POST /api/audit/approvals/requests`
- `POST /api/audit/approvals/{approvalId}/approve`
- `POST /api/audit/approvals/{approvalId}/reject`
- `DELETE /api/audit/authorizations/{authorizationId}`: 즉시 삭제하지 않고 `202 Accepted`와 승인 접수 정보를 반환

## Auth Role Approval

- `masterType`: `AUTH_USER_ROLE`
- `requestType`: `CREATE` 또는 `UPDATE`
- `masterKey`: 권장값은 Auth `username`
- `payload` 예시:

```json
{"username":"admin","role":"ACCOUNTING_ADMIN","dataScope":"FIN"}
```

승인 후 Governance는 role 값을 `ROLE_ACCOUNTING_ADMIN` 형태로 정규화해 Auth 내부 API에 전달합니다. 내부 API 호출에는 `X-Internal-Auth-Token`이 포함되며 토큰 미설정 또는 불일치 시 Auth가 403으로 거절합니다.

## 현재 구현 기준에서 먼저 알아둘 점

- 목표 경계에서 사용자/메뉴/인증(IAM) 책임은 `auth` 모듈이며, Governance는 감사·승인·SOD 증적을 소유합니다.
- legacy `com.ho.account.security` 모델의 신규 확장은 `governance`가 아니라 `auth`에서 진행해야 합니다.
- `DataMaskingService`는 패턴별 문자열 마스킹만 제공합니다.
- `.run/Governance bootRun.run.xml`은 H2 단독 API 실행용이며 포트 `8083`을 사용합니다.
- 역할 생성/권한 부여 API는 승인 요청을 만들면서 저장 전 preview 도메인을 반환합니다. 승인 접수 DTO로 통일하는 코드 `@todo`가 남아 있습니다.
- Auth 외부 반영은 Governance DB 트랜잭션과 원자적이지 않습니다. `approvalId` 멱등키 기반 outbox/inbox 전환 코드 `@todo`가 남아 있습니다.
- Master Data 연동의 순차 재시도는 `approvalId` 멱등키로 보호되지만, 두 노드의 동시 최초 저장 충돌 복구와 외부 Auth 연동은 outbox/inbox가 여전히 필요합니다.
- PostgreSQL 드라이버는 포함되어 있지만 운영 Flyway DDL과 실제 PostgreSQL 통합 검증은 아직 필요합니다.
