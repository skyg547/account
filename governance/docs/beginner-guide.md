# governance beginner guide

## 한 문장 요약

`governance`는 누가 무엇을 했는지 기록하고, 중요한 변경은 승인받게 하며, 권한 충돌을 막는 통제 모듈입니다.

초보자 관점에서는 회사의 감사팀과 결재 시스템입니다. 버튼을 누른 사람, 바뀐 데이터, 승인자, 반려 사유를 남겨 나중에 회계 사고나 운영 장애를 추적할 수 있게 합니다.

## 주요 개념

| 용어 | 설명 |
| --- | --- |
| 감사 로그 | API나 서비스 동작 전후의 사용자, 대상, 결과를 남기는 기록. |
| 승인 요청 | 기준정보, 역할, 권한 같은 민감 변경을 바로 반영하지 않고 승인 대기로 남기는 요청. |
| SOD | 요청자와 승인자가 같으면 안 된다는 직무 분리 원칙. |
| 마스킹 | 주민번호, 계좌번호, 이메일 같은 민감정보 일부를 가리는 처리. |
| Auth 역할 반영 | Governance 승인 후 Auth 내부 API를 호출해 사용자 역할과 `roleVersion`을 갱신하는 흐름. |

## DDD와 헥사고날 관점

- `web`: 감사/승인/권한 API를 제공하는 인바운드 어댑터.
- `service`: 감사 저장, 승인, 권한 정책 확인 유즈케이스.
- `domain`: `AuditLog`, `MasterApproval`, `SystemRole`, `RoleAuthorization` 같은 통제 모델.
- `application.port.out`: 감사 저장, 승인 저장, Auth/master-data 연동 계약.
- `infrastructure`: JPA 저장, 요청 컨텍스트 해석, Auth 내부 API 호출 어댑터.

## 처음 볼 파일

1. `GovernanceApplication`: Spring Boot 실행 진입점.
2. `AuditController`: 감사 로그, 역할, 권한, 승인 API.
3. `MasterApprovalService`: 승인 요청, 승인, 반려 흐름.
4. `RestClientAuthUserRoleAssignmentAdapter`: Governance 승인 결과를 Auth 내부 API로 반영.
5. `RequestAuditActorAdapter`: 요청 헤더와 principal에서 감사 actor를 해석.
