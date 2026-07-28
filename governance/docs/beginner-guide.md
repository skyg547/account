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
| Master Data 시행일 | 승인일과 실제 반영일을 분리합니다. 미래 시행일이면 결재는 끝나도 기준정보는 예약 상태로 기다립니다. |

## DDD와 헥사고날 관점

- `web`: 감사/승인/권한 API를 제공하는 인바운드 어댑터.
- `service`: 감사 저장, 승인, 권한 정책 확인 유즈케이스.
- `domain`: `AuditLog`, `MasterApproval`, `SystemRole`, `Authorization` 같은 통제 모델.
- `application.port.out`: 감사 저장, 승인 저장, Auth/master-data 연동 계약.
- `infrastructure`: JPA 저장, 요청 컨텍스트 해석, Auth 내부 API 호출 어댑터.

## 처음 볼 파일

1. `GovernanceApplication`: Spring Boot 실행 진입점.
2. `AuditController`: 감사 로그, 역할, 권한, 승인 API.
3. `MasterApprovalService`: 승인 요청, 승인, 반려 흐름.
4. `RestClientAuthUserRoleAssignmentAdapter`: Governance 승인 결과를 Auth 내부 API로 반영.
5. `MasterDataChangeRequestAdapter`: 승인 ID를 멱등키로 전달하고 시행일 도래 여부에 따라 즉시/예약 반영을 나눕니다.
6. `RequestAuditActorAdapter`: 요청 헤더와 principal에서 감사 actor를 해석.
## 권한 회수도 왜 승인을 받아야 하나요?

권한을 새로 주는 것뿐 아니라 기존 권한을 없애는 행위도 업무 중단과 통제 증적에 영향을 줍니다. 그래서 `DELETE /api/audit/authorizations/{authorizationId}`는 곧바로 행을 삭제하지 않고 `AUTHORIZATION / DELETE` 승인 요청을 만들며 HTTP `202 Accepted`와 승인 접수 정보를 반환합니다.

승인자가 별도 계정으로 승인해야 `SystemRoleApprovalApplyAdapter`가 실제 권한 행을 삭제합니다. 지원하지 않는 역할 수정/삭제 또는 권한 수정 요청은 조용히 넘어가지 않고 예외로 중단됩니다. 이는 결재 상태와 실제 데이터가 서로 달라지는 상황을 막는 fail-closed 원칙입니다.
