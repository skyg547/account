# Governance Module Docs

`governance` 모듈은 시스템 감사로그, 마스터 승인, 역할/권한 관리, 보안 사용자 모델을 담당합니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 감사, 승인, 권한 체크 흐름을 설명합니다.
- [schema.md](./schema.md): audit/security 영역의 주요 엔티티와 관계를 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 거버넌스 모듈의 목적을 이해할 수 있도록 쉽게 설명합니다.

## 이 모듈이 하는 일

1. 서비스 호출 결과를 감사로그로 남깁니다.
2. 마스터 데이터 변경 요청을 승인 대상으로 관리합니다.
3. 역할과 기능 권한을 등록하고 조회합니다.
4. 권한 여부를 점검해 SOD 관점의 통제를 보조합니다.
5. 마스킹 규칙으로 민감정보 표시를 제한합니다.

## 핵심 진입점

- `AuditController`
- `AuditService`
- `AuditAspect`
- `MasterApprovalService`
- `TracingService`
- `DataMaskingService`

## 현재 구현 기준에서 먼저 알아둘 점

- `audit` 패키지와 `security` 패키지가 함께 있지만 실제 API는 주로 `audit` 쪽에 있습니다.
- `AuditAspect`는 현재 사용자 정보를 `SecurityContext`에서 읽지 않고 `SYSTEM`으로 기록합니다.
- `MasterApprovalService.approve()`에는 실제 마스터 반영 로직이 아직 `TODO` 상태입니다.
- 역할 모델이 두 세트 공존합니다.
  - `audit.domain.SystemRole` + `Authorization`
  - `security.domain.SecurityRole` + `RoleFuncPermission`
- `DataMaskingService`는 패턴별 문자열 마스킹만 제공합니다.
