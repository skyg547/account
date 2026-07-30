# Budget 스키마

Flyway `V50__create_budget_control_tables.sql`이 새 Budget bounded context의 다섯 테이블을 소유합니다.
기존 Expenditure `budgets` 테이블과 이름을 분리해 검토되지 않은 dual-write를 막습니다.

## `budget_fiscal_year_controls`

- `0000`부터 `9999`까지 10,000개 행을 migration에서 미리 생성
- `OPEN → CLOSED`, 마감자·마감시각, optimistic `version`
- 생성·승인·전용·집행·취소·마감이 먼저 해당 연도 행을 `PESSIMISTIC_WRITE`로 잠금

## `budget_idempotency_shards`

- `0..255`의 256개 영구 잠금 행
- SHA-256 기반으로 같은 전용 request key/집행 source triple을 같은 shard에 배정
- 결과 업무 행이 아직 없을 때도 첫 동시 요청을 직렬화

## `budget_plans`

- unique `plan_code`
- unique `(year_month, department_code, account_code)`
- allocation, transfer-in/out, executed: `DECIMAL(19,2)`
- `status`, 감사 사용자, 시각, optimistic `version`

## `budget_transfers`

- unique `request_key`
- source/target plan ID와 금액
- `REQUESTED → APPROVED` 감사 정보

## `budget_executions`

- unique `(source_type, source_id, source_line_id)`
- plan ID, 집행일, 금액
- `EXECUTED → CANCELLED` 감사 정보

Application service는 `idempotency shard → fiscal-year control → 낮은 plan ID` 순서로
`PESSIMISTIC_WRITE` 잠금을 획득합니다.
DB unique/check constraint는 애플리케이션 검증이 우회되더라도 중복 lineage와 음수 금액을
마지막 경계에서 차단합니다.
