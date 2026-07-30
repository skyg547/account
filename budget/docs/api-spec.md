# Budget API

기본 경로는 `/api/budgets`입니다.

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/plans` | 월별 계획 초안 생성 |
| POST | `/plans/{planId}/approval` | 계획 승인 |
| GET | `/plans/available` | `yearMonth`, `departmentCode`, `accountCode` 조회 |
| POST | `/transfers` | 전용 요청 |
| POST | `/transfers/{transferId}/approval` | 전용 승인 |
| POST | `/executions` | source lineage 기반 집행 |
| POST | `/executions/{executionId}/cancellation` | 집행 취소 |

금액 입력은 양수이면서 정수 17자리·소수 2자리 이하여야 합니다. `yearMonth`는 여섯 자리
형식을 먼저 검사하고 core가 실제 월 범위(01–12)를 다시 검증합니다. HTTP DTO는 JPA
엔티티를 직접 노출하지 않습니다.

## 인증과 권한

모든 요청은 Auth와 같은 HS256 secret/issuer로 검증되는 Bearer JWT가 필요합니다. API는
Gateway의 `X-Auth-*` 헤더나 JSON의 `actor`/`requester` 값을 신뢰하지 않고 JWT `sub`를
감사 사용자로 사용합니다.

- 조회: 유효한 JWT
- 계획 생성·전용 요청·집행·취소: `ROLE_BUDGET_MANAGER`, `ROLE_ACCOUNTING_ADMIN`,
  `ROLE_ADMIN` 중 하나
- 계획 승인·전용 승인: `ROLE_BUDGET_APPROVER`, `ROLE_ACCOUNTING_ADMIN`,
  `ROLE_ADMIN` 중 하나

인증 실패는 401, 권한 부족은 403입니다. 존재하지 않는 자원은 404, 멱등키/동시성 충돌은
409, 잔액·상태·회계기간 규칙 위반은 422, 잘못된 transport 입력은 400으로 응답합니다.
