# 처리 흐름

## 계획 승인

```text
HTTP request
  → Bearer JWT 서명·issuer·역할 검증
  → BudgetController
  → CreateBudgetPlanCommand
  → BudgetManagementUseCase
  → fiscal-year control 잠금/OPEN 확인
  → BudgetPlan domain validation
  → BudgetPlanPersistencePort
  → JPA outbound adapter
```

## 전용 승인

```text
request transfer (requestKey)
  → idempotency shard 잠금
  → source/target 동일 YYYYMM·승인·가용액 확인
  → fiscal-year control 잠금/OPEN 확인
  → BudgetTransfer REQUESTED 저장

approve transfer
  → fiscal-year control 잠금/OPEN 확인
  → source/target ID 오름차순 잠금
  → source.transferTo(target)
  → BudgetTransfer APPROVED
  → 한 트랜잭션으로 세 Aggregate 저장
```

## 집행과 취소

```text
execute (source lineage)
  → idempotency shard 잠금
  → 기존 lineage 조회
  → 같은 내용이면 기존 결과 반환
  → fiscal-year control 잠금/OPEN 확인
  → 계획 잠금 + 집행일 YYYYMM/가용액 확인 + EXECUTED 저장

cancel
  → fiscal-year control → execution → plan 순서로 잠금
  → 집행액 복원 + CANCELLED 저장
```

## Batch

`budgetYearEndCloseJob → fiscalYear 검증 → BudgetYearEndUseCase.closeFiscalYear
→ fiscal-year control CLOSED + 승인 계획 CLOSED`.
Batch에는 금액 계산이나 상태 전이 규칙이 없습니다.
