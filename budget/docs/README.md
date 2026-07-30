# Budget 문서

## 소유권

| 계층 | 책임 |
| --- | --- |
| Domain | 계획·전용·집행 상태와 금액 불변식 |
| Application | idempotency shard → 회계연도 → 계획 ID 잠금 순서, 멱등 재시도, 트랜잭션 조정 |
| Outbound adapter | JPA 엔티티 매핑, 영구 제어/shard 잠금, unique key 저장 |
| API | JWT 서명·역할 검증, Bean Validation, command/response·오류 변환 |
| Batch | Job 파라미터 검증과 core use case 호출 |

## 읽는 순서

1. [beginner-guide.md](beginner-guide.md): 회계/예산 용어
2. [process-flow.md](process-flow.md): 요청이 계층을 통과하는 순서
3. [schema.md](schema.md): 테이블, 잠금, 멱등 키
4. [api-spec.md](api-spec.md): HTTP 계약
5. [local-run.md](local-run.md): 로컬 검증 명령

## 범위

초기 계획 단위는 `YYYYMM + departmentCode + accountCode`입니다. 연간·분기 편성,
carryover, 교차 월 전용, Expenditure 예약/확정/해제 전환은 Enterprise refinement #17에서 현재
호환 데이터의 대사 기준과 함께 확장합니다.
