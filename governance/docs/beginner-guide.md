# Governance Beginner Guide

## 1. 거버넌스 모듈이란 무엇인가요

이 모듈은 "누가 무엇을 했는지 남기고, 누가 무엇을 할 수 있는지 통제하고, 중요한 변경은 승인받게 만드는" 역할을 합니다.

쉽게 말하면:
- 감사로그 = 흔적 남기기
- 통제정책 = 승인/직무분리 기준 적용
- 승인관리 = 중요한 변경을 바로 반영하지 않고 검토받기

경계 정리:
- 사용자/메뉴/권한(IAM) 마스터 관리는 `auth` 모듈 책임
- `governance`는 내부회계 통제(감사/승인/SOD) 책임

## 2. 초보자가 먼저 알아야 할 용어

- 감사로그: 시스템에서 일어난 행동의 기록
- 역할(Role): 사용자에게 묶음으로 주는 권한 단위
- 권한(Authorization): 특정 기능에 대해 읽기/쓰기/삭제/승인 같은 접근 권리
- SOD: 한 사람이 요청과 승인을 동시에 하지 못하게 하는 직무분리 통제
- 마스터 승인: 계정과목, 부서, 거래처 같은 기준정보 변경을 승인 프로세스로 관리하는 것
- 마스킹: 민감정보를 일부 가려서 보여주는 것

## 3. 이 모듈은 왜 중요한가요

회계 시스템은 숫자만 맞으면 끝이 아닙니다. 나중에 물어봤을 때 아래 질문에 답할 수 있어야 합니다.

- 누가 전표를 만들었나
- 누가 기준정보를 바꿨나
- 어떤 권한으로 처리했나
- 승인 없이 처리된 것은 없나

이 질문에 답하게 해주는 기반이 이 모듈입니다.

## 4. 감사로그는 어떻게 남나요

현재 구조에서는 `@AuditLoggable`이 붙은 메서드를 `AuditAspect`가 감쌉니다.

그러면 자동으로:
- 호출 전 입력값
- 호출 후 결과값
- 성공/실패 상태
- IP 주소

를 `AUDIT_LOG` 테이블에 남깁니다.

주의:
- 사용자 ID는 `X-User-ID` -> `remoteUser` -> `principal` -> `SYSTEM` 순으로 기록됩니다.
- 운영 환경에서는 API Gateway/Auth 계층에서 `X-User-ID` 전달 규칙을 고정하는 것이 좋습니다.

## 5. 승인관리는 어떻게 보나요

예를 들어 계정과목을 바꾼다고 가정하면:

1. 요청자가 변경 요청을 올립니다.
2. `MASTER_APPROVAL`에 `PENDING`으로 저장됩니다.
3. 승인자가 검토합니다.
4. 요청자 본인이 승인하려 하면 SOD 위반으로 막힙니다.
5. 승인 또는 반려 처리됩니다.

현재는 승인 시 `master-data` 변경요청 생성/승인/적용까지 연계됩니다.

## 6. 권한은 어떻게 이해하면 되나요

권한은 두 층으로 나눠 이해하면 됩니다.

- IAM 권한(사용자/메뉴/역할/권한 매트릭스) = `auth` 책임
- 내부회계 통제 권한(승인 가능 여부, SOD 위반 방지) = `governance` 책임

현재 `governance` 코드에 남아있는 `security` 패키지 모델은 이관 대상(legacy)으로 보고 신규 확장은 `auth`에서 진행하는 것을 권장합니다.

## 7. 마스킹은 언제 쓰나요

민감정보를 화면이나 로그에 그대로 보여주면 안 될 때 씁니다.

현재 제공 패턴:
- `ACCOUNT`
- `REG_NO`
- `EMAIL`

예:
- 이메일 `abc@example.com` -> `a***@example.com`

## 8. 처음 확인하기 좋은 API

1. `GET /api/audit/logs`
2. `GET /api/audit/logs/user/{userId}`
3. `GET /api/audit/logs/target?entity=...&targetId=...`
4. `POST /api/audit/roles`
5. `POST /api/audit/roles/{roleCode}/authorizations`
6. `GET /api/audit/permissions/check`
7. `GET /api/audit/approvals/pending`
8. `POST /api/audit/approvals/requests`
9. `POST /api/audit/approvals/{approvalId}/approve`
10. `POST /api/audit/approvals/{approvalId}/reject`

## 9. 현재 코드에서 꼭 주의할 점

- `governance` 내부의 security 관련 모델은 경계 정리 대상이라 처음 보면 헷갈릴 수 있습니다.
- IAM 마스터(사용자/메뉴/권한 매트릭스)는 `auth` 중심으로 정리하는 방향을 유지해야 합니다.

## 10. 문서 추천 순서

1. [README.md](./README.md)
2. [process-flow.md](./process-flow.md)
3. [schema.md](./schema.md)

이 순서로 보면 "왜 필요한지 -> 어떻게 동작하는지 -> 어디에 저장되는지"를 빠르게 잡을 수 있습니다.
