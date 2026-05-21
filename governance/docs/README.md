# Governance Module Docs

`governance` 모듈은 내부회계 통제를 위한 감사로그와 승인 워크플로우를 담당합니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 감사, 승인, 권한 체크 흐름을 설명합니다.
- [schema.md](./schema.md): audit/security 영역의 주요 엔티티와 관계를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 거버넌스 모듈의 목적을 이해할 수 있도록 쉽게 설명합니다.

## 이 모듈이 하는 일

1. 서비스 호출 결과를 감사로그로 남깁니다.
2. 마스터 데이터 변경 요청을 승인 대상으로 관리합니다.
3. SOD 관점의 통제 정책을 적용합니다.
4. 마스킹 규칙으로 민감정보 표시를 제한합니다.

## 핵심 진입점

- `AuditController`
- `AuditService`
- `AuditAspect`
- `MasterApprovalService`
- `TracingService`
- `DataMaskingService`

## Hexagonal DDD 적용 현황

- Inbound:
  - `AuditController` (HTTP, request/response DTO)
  - `AuditAspect` (`@AuditLoggable` AOP)
- Application:
  - `application.port.in`: `AuditLogUseCase`, `AuthorizationUseCase`, `MasterApprovalUseCase`
  - `application.service`: `AuditService`, `MasterApprovalService`
  - `application.port.out`: persistence/actor/master-data 연계 포트
- Infrastructure:
  - `infrastructure.persistence`: JPA adapter
  - `infrastructure.security`: 요청 컨텍스트 기반 사용자/IP 해석
  - `infrastructure.masterdata`: 승인 변경을 `master-data` 유스케이스로 반영

## 내부회계 통제 포인트

- 감사로그 사용자 식별:
  - 우선순위 `X-User-ID` -> `request.getRemoteUser()` -> `request.getUserPrincipal()` -> `SYSTEM`
- SOD:
  - 요청자와 승인자 동일 시 승인 불가
- 승인 후 반영:
  - `MasterApproval` 승인 시 `master-data` 변경요청 생성 -> 승인 -> 적용까지 연결
  - `AUTH_USER_ROLE` 승인 시 Auth 내부 역할 할당 API를 호출해 사용자 역할과 `roleVersion`을 갱신
- 권한:
  - role + function + accessType 조합 중복 부여 차단
- 추적성 조회:
  - `TracingService`는 `AuditLogRepository`가 아니라 `AuditLogPersistencePort`를 통해 감사 로그를 조회

## Master Approval API

- `GET /api/audit/approvals/pending`
- `POST /api/audit/approvals/requests`
- `POST /api/audit/approvals/{approvalId}/approve`
- `POST /api/audit/approvals/{approvalId}/reject`

## Auth Role Approval

- `masterType`: `AUTH_USER_ROLE`
- `requestType`: `CREATE` 또는 `UPDATE`
- `masterKey`: 권장값은 Auth `username`
- `payload` 예시:

```json
{"username":"admin","role":"ACCOUNTING_ADMIN","dataScope":"FIN"}
```

승인 후 Governance는 role 값을 `ROLE_ACCOUNTING_ADMIN` 형태로 정규화해 Auth 내부 API에 전달합니다.

## 현재 구현 기준에서 먼저 알아둘 점

- `audit` 패키지와 `security` 패키지가 함께 있지만, 목표 경계에서 사용자/메뉴/권한(IAM) 책임은 `auth` 모듈입니다.
- `security` 패키지 모델은 이관 대상(legacy)으로 보고 신규 기능은 `governance`가 아닌 `auth`에서 확장해야 합니다.
- `DataMaskingService`는 패턴별 문자열 마스킹만 제공합니다.
